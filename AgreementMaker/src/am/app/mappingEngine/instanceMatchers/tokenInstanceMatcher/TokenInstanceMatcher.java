package am.app.mappingEngine.instanceMatchers.tokenInstanceMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import simpack.measure.weightingscheme.StringTFIDF;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.wcohen.ss.api.StringWrapper;

import am.app.mappingEngine.DefaultMatcherParameters;
import am.app.mappingEngine.StringUtil.AMStringWrapper;
import am.app.mappingEngine.StringUtil.Normalizer;
import am.app.mappingEngine.StringUtil.NormalizerParameter;
import am.app.mappingEngine.instanceMatchers.BaseInstanceMatcher;
import am.app.mappingEngine.instanceMatchers.KeywordsUtils;
import am.app.mappingEngine.instanceMatchers.Porter;
import am.app.mappingEngine.instanceMatchers.WordNetUtils;
import am.app.mappingEngine.instanceMatchers.labelInstanceMatcher.LabelInstanceMatcher;
import am.app.ontology.instance.Instance;

/**
 * Instance matcher based on bag-of-words comparison.
 * 
 * @author federico
 *
 */
public class TokenInstanceMatcher extends BaseInstanceMatcher{
	private static final long serialVersionUID = 7328940873670935546L;

	//Stemmer
	private Porter stemmer = new Porter();
	//Wordnet utilities (to compute synonyms)
	private WordNetUtils wordNetUtils = new WordNetUtils();

	private Logger log = Logger.getLogger(TokenInstanceMatcher.class);

	//Caching for the source instances.
	private String lastSourceURI = "";
	private List<String> lastSourceProcessed; 

	private boolean useSynonyms = false;

	//Used for retrieving the labels when the objects are URIs
	LabeledDatasource sourceKB;
	LabeledDatasource targetKB;
	
	//Normalizes strings
	private Normalizer normalizer;

	/**
	 * Modality ALL means that we have to take ALL the property values
	 */
	public enum Modality { ALL, ALL_SYNTACTIC, ALL_SEMANTIC, SELECTIVE };

	private Modality modality;
	
	/**
	 * Aggregation function
	 */
	public enum Aggregation { TF_IDF, DICE };
	
	private Aggregation aggregation;
	
	private StringTFIDF tfidf;
	List<StringWrapper> corpus;
	Map<Instance, StringWrapper> documents;
	
	
	/**
	 * List of properties of which we need to gather values
	 */
	public List<String> selectedProperties;

	/**
	 * Default modality is all
	 */
	public TokenInstanceMatcher(){
		initNormalizer();
		modality = Modality.ALL;
		aggregation = Aggregation.TF_IDF;
		initTfIdf();
	}

	private void initTfIdf() {
		corpus = new ArrayList<StringWrapper>();
		documents = new HashMap<Instance, StringWrapper>();
	}

	private void initNormalizer() {
		NormalizerParameter np = new NormalizerParameter();
		np.stem = true;
		np.normalizeDigit = true;
		np.normalizePossessive = true;
		np.normalizeSlashes = true;
		np.removeAllStopWords = true;
		normalizer = new Normalizer(np);
	}

	@Override
	public void setParam(DefaultMatcherParameters param) {
		if(param instanceof TokenInstanceMatcherParameters){
			TokenInstanceMatcherParameters p = (TokenInstanceMatcherParameters) param;
			modality = p.modality;
			selectedProperties = p.selectedProperties;
			aggregation = p.aggregation; 
		}
		
		if(aggregation == Aggregation.TF_IDF){
			initTfIdf();
		}
		
		super.setParam(param);
	}

	
	@Override
	public void passEnd() {
		if(aggregation == Aggregation.TF_IDF && tfidf == null){
			tfidf = new StringTFIDF(corpus);			
		}
	}
	
	@Override
	public double instanceSimilarity(Instance source, Instance target)
			throws Exception {
		double sim = 0.0;

		log.debug("TIM matching: " + source.getUri() + " " + target);		

		//This is the TFIDF second pass
		if(aggregation == Aggregation.TF_IDF && tfidf != null){
			StringWrapper sourceDocument = documents.get(source);	
			StringWrapper targetDocument = documents.get(target);			
			return tfidf.getSimilarity(sourceDocument.unwrap(), targetDocument.unwrap());
		}
				
		List<String> sourceKeywords = new ArrayList<String>();

		//Check the last element cache
		//TODO check URIs when packing entities
		if(lastSourceURI.equals(source.getUri()))
			sourceKeywords = lastSourceProcessed;
		else{
			sourceKeywords = buildKeywordsList(source, sourceKB);
			log.debug("S Preprocess:" + sourceKeywords);
			sourceKeywords = KeywordsUtils.processKeywords(sourceKeywords, normalizer);
			
			if(aggregation == Aggregation.TF_IDF){
				StringBuffer buf = new StringBuffer();
				for (String string : sourceKeywords) {
					buf.append(string).append(" ");
				}
				
				if(tfidf == null){
					StringWrapper sw = new AMStringWrapper(buf.toString()); 
					
					System.out.println("Source: " + sw.unwrap());
					
					corpus.add(sw);
					documents.put(source, sw);
				}
			}
			
			log.debug("S Postprocess:" + sourceKeywords);
		}

		List<String> targetKeywords = buildKeywordsList(target, targetKB);
		
		lastSourceURI = source.getUri();
		lastSourceProcessed = sourceKeywords;

		log.debug("T Preprocess:" + targetKeywords);
		targetKeywords = KeywordsUtils.processKeywords(targetKeywords, normalizer);
		log.debug("T Postprocess:" + targetKeywords);

		if(aggregation == Aggregation.TF_IDF){
			StringBuffer buf = new StringBuffer();
			for (String string : targetKeywords) {
				buf.append(string).append(" ");
			}
			
			if(tfidf == null){
				StringWrapper sw = new AMStringWrapper(buf.toString()); 
				corpus.add(sw);
				documents.put(target, sw);
				
				System.out.println("Target: " + sw.unwrap());
				
			}
		}
		else{
			//All the metrics but TFIDF
			sim = keywordsSimilarity(sourceKeywords, targetKeywords);
		}
			
		log.debug(sim);

		return sim;
	}

	private List<String> buildKeywordsList(Instance instance, LabeledDatasource kb) {
		List<String> values = new ArrayList<String>();	

		//In case modality is ALL or ALL_SYNTACTIC, we have to have to gather all the properties 
		//from the properties map
		if(modality == Modality.ALL || modality == Modality.ALL_SYNTACTIC){
			values.addAll(instance.getAllPropertyValues());
		}	

		//In case modality is ALL or ALL_SEMANTIC, we have to have to gather all the properties 
		//from the list of statements
		if(modality == Modality.ALL || modality == Modality.ALL_SEMANTIC){
			List<Statement> stmts = instance.getStatements();
			for (Statement statement : stmts) {
				String literal = statement.getObject().asLiteral().getString();

				log.debug(literal);
				
				if(literal.startsWith("http://")){
					if(kb == null){
						//TODO Figure out if we need to take the URI fragment when we cannot access to the label
						continue;
					}

					log.debug("Accessing the KB");
					String label = kb.getLabelFromURI(literal);
					if(label != null)
						values.add(label);					
				}
				else{
					int limit = 300;				
					if(literal.length() < limit)					
						values.add(literal);
					else values.add(literal.substring(0, limit - 1));
				}

			}
		}
			

		//Modality SELECTIVE means that we have to take only some of the property values,
		//for which the strings in selectedProperties are matching
		//TODO implement this
		if(modality == Modality.SELECTIVE){
			// TODO manage selective modality						

			//			List<Statement> sourceStmts = source.getStatements();
			//
			//			for (Statement statement : sourceStmts) {
			//				if(statement.getPredicate().getLocalName().toLowerCase().contains("keyword")){
			//					log.debug("Found keywords property: " + statement.getPredicate());
			//					String keywords = statement.getObject().asLiteral().getString();
			//					String[] split = keywords.split("\\s|,");
			//					for (int i = 0; i < split.length; i++) {
			//						split[i].trim();
			//						if(split[i].length() > 0)
			//							sourceKeywords.add(split[i]);
			//					}
			//				}
			//			}
			//			
			//			List<String> types = target.getProperty("type");
			//			List<String> candidateKeywords = new ArrayList<String>();
			//
			//			//Specific for freebase
			//			if(types != null){
			//				types = KeywordsUtils.processKeywords(types);
			//				//sim = keywordsSimilarity(sourceKeywords, types);
			//				log.debug("types: " + types);
			//				candidateKeywords.addAll(types);
			//			}
			//
			//			List<Statement> stmts = target.getStatements();
			//			for (int i = 0; i < stmts.size(); i++) {
			//				if(stmts.get(i).getPredicate().equals(RDFS.comment)){
			//					String comment = stmts.get(i).getObject().asLiteral().getString();
			//					candidateKeywords.add(comment);
			//					log.debug("Comment:" + comment);
			//				}
			//			}
		}

		String label = instance.getSingleValuedProperty("label");

		if(label != null){
			String betweenParentheses = getBetweenParentheses(label);
			log.debug("between parentheses: " + betweenParentheses);
			if(betweenParentheses != null) values.add(betweenParentheses);		
		}

		return values;
	}

	private double keywordsSimilarity(List<String> sourceList, List<String> targetList){
		//Compute score
		double score = 0;
		
		List<String> sourceStemmed = stemList(sourceList);
		List<String> targetStemmed = stemList(targetList);
		
		String source;
		String target;
		for (int i = 0; i < sourceList.size(); i++) {
			source = sourceList.get(i);
			if(source.isEmpty()) continue;
			source = source.toLowerCase();
			for (int j = 0; j < targetList.size(); j++) {
				target = targetList.get(j).toLowerCase();
				if(target.isEmpty()) continue;
				//System.out.println(type + "|" + keyword);
				if(source.equals(target)){
					score++;
				}

				if(useSynonyms){
					if(wordNetUtils.areSynonyms(source, target) ){
						score += 0.5;
						//System.out.println("matched syn: " + source + "|" + target);
					}
					continue;
				}

				try{
					if(sourceStemmed.get(i).equals(targetStemmed.get(j)))
						score += 0.5;
				}
				catch (Exception e) {
					System.err.println("Error when stemming " + source + " with " + target);
				}
			}
		}

		double avg = (Math.min(sourceList.size(), targetList.size()) 
				+ Math.max(sourceList.size(), targetList.size())) / 2;
		double max = Math.max(sourceList.size(), targetList.size());

		if(max == 0) return 0;

		score /= avg;

		return score;
	}

	private List<String> stemList(List<String> list) {
		List<String> stemmed = new ArrayList<String>();
		for (String string : list) {
			try{
				stemmed.add(stemmer.stripAffixes(string));
			}
			catch(Exception e){
				e.printStackTrace();				
			}
		}		
		return stemmed;
	}

	public static String getBetweenParentheses(String label){
		if(label.contains("(")){
			int beg = label.indexOf('(');
			int end = label.indexOf(')');
			return label.substring(beg + 1, end);
		}
		return null; 
	}

	public void setSourceKB(LabeledDatasource sourceKB) {
		this.sourceKB = sourceKB;
	}

	public void setTargetKB(LabeledDatasource targetKB) {
		this.targetKB = targetKB;
	}
	
	public Normalizer getNormalizer() {
		return normalizer;
	}
	
	@Override
	public String getName() {
		return "Token Instance Matcher";
	}
	
	@Override
	public boolean requiresTwoPasses() {
		if(aggregation == Aggregation.TF_IDF){
			requiresTwoPasses = true;
		}
		return super.requiresTwoPasses();
	}
		
	public static void main(String[] args) {
		String s = "1985 Tampa Bay Buccaneers season, UKN, Infobox NFL_season, 1985 Tampa Bay Buccaneers season\n" +
				"The 1985 Tampa Bay Buccaneers season began with the team trying to improve on an\n" +
				"6-10 season. It was the first season for Leeman Bennett as the team's head\n" +
				"coach. Prior to the season they acquired the future hall of fame quarterback\n" +
				"Steve Young. In week 1, Tampa Ba, 1985_Tampa_Bay_Buccaneers_season, 1985, Tampa Stadium, Tampa Stadium, Leeman Bennett, Leeman Bennett, Tampa Bay Buccaneers, 5th NFC Central, 2-14, Did not quailify, NFC Central";
		
		Normalizer n = new TokenInstanceMatcher().getNormalizer();
		System.out.println(n.normalize(s));				
	}
}