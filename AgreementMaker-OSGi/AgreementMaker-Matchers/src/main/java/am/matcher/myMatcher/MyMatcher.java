package am.matcher.myMatcher;


import java.util.ArrayList;



import am.Utility;
import am.app.mappingEngine.AbstractMatcher;
import am.app.mappingEngine.Alignment;
import am.app.mappingEngine.DefaultMatcherParameters;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.ReferenceEvaluationData;
import am.app.mappingEngine.referenceAlignment.ReferenceAlignmentMatcher;
import am.app.mappingEngine.referenceAlignment.ReferenceAlignmentParameters;
import am.app.mappingEngine.referenceAlignment.ReferenceEvaluator;
import am.app.mappingEngine.similarityMatrix.SimilarityMatrix;
import am.app.ontology.Node;
import am.app.ontology.Ontology;
import am.app.ontology.ontologyParser.OntoTreeBuilder;
import am.app.ontology.ontologyParser.OntologyDefinition;


public class MyMatcher  extends AbstractMatcher  {
/*
	private static FileOutput writer;*/
	
	private static StopWords stop;

	public MyMatcher(){
		super();
		
		setName("MyMatcher");
		setCategory(MatcherCategory.UNCATEGORIZED);		
	}
	
	/**
	 * 
	 */
	@Override
	protected Mapping alignTwoNodes(Node source, Node target,
			alignType typeOfNodes, SimilarityMatrix matrix) throws Exception 
	{
		
		String sourceName=source.getLocalName();
		//replace the first character with lower case to avoid confusions considering words like "hasAuthor"
		sourceName = Character.toLowerCase(sourceName.charAt(0)) + sourceName.substring(1); 
		String targetName=target.getLocalName();
		targetName = Character.toLowerCase(targetName.charAt(0)) + targetName.substring(1); 
		
		//replace the special characters
		sourceName=sourceName.replace("-", "");
		targetName=targetName.replace("-", "");
		
		//get the similarity score (sim > 0 if similarity score is greater than 0.7)
		double sim=this.getSimilarityScore(sourceName, targetName);
		
		// special case where the majority words are similar yet sim value is not greater than 0.7 (Eg:Conference_document->document)
		if(sim>=0.66 && sim<0.7)
		{
			//if source and target is class , check if their parents are equal
			if(source.isClass())
			{
				//when both source and target has no parents
				if(source.getParentCount()==0&&target.getParentCount()==0)
				{
					sim=0.7;
				}
				
				
				else
				{
					
					//obtain the String array with parent names
					String sparents=(source.getParents().toString());
					sparents=sparents.substring(1,sparents.length()-1);
					String[] sourceparent=sparents.split(",");
					
					String tparents=target.getParents().toString();
					tparents=tparents.substring(1,tparents.length()-1);
					String[] targetparent=tparents.split(",");
					
					// When there is exactly one parent for source and target , check the similarity of two parents
					if(sourceparent.length==1 && targetparent.length==1)
					{
						// strip the numerical from parent names, Eg: 19 User -> User
						if(this.getSimilarityScore(targetparent[0].replaceAll("[0-9]", ""), sourceparent[0].replaceAll("[0-9]", ""))>0.9)
							sim=0.7;
						else
							sim=0;
					}
					//When more than one parent , probability of similarity is less
					else
						sim=0;
					
				}	
					
			}
			
		}
		// If type of source and target is a property, check whether their domain is same.If same assign the similarity score
		
		if (source.isProp())
		{
			double propertySim=0;
			try
			{
				//Get source domain name
				String sourceDomainName=source.getPropertyDomain().getLocalName();
				//Get target domain name
				String targetDomainName=target.getPropertyDomain().getLocalName();
				//If source and target domain name null allocate propertySim as 0
				if (sourceDomainName==null ||targetDomainName==null)
					propertySim=0d;
				
				//If not null, find propertySim value
				else
				{
					propertySim=this.getSimilarityScore(sourceDomainName, targetDomainName);
				}
				
				//If propertySim less than threshold make sim as 0
				if (propertySim<0.8)
				{
					sim=0;
					
					
				}
				
				
				
			}
			catch(NullPointerException e)
			{
				
			}	
		}
		return new Mapping(source, target, sim);				
	}
	
	
	/**
	 * Returns similarity score of source and target 
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	private double getSimilarityScore(String sourceName, String targetName)
	{
		//String similarity class contains edit distance methods and wordnet methods
		StringSimilarity s=new StringSimilarity();
		double sim=0.0d;
		// If the source name matches exactly with target name
		if(sourceName.equalsIgnoreCase(targetName))
			sim=2.0d;
		//If the edit distance is greater than threshold
		else if (s.similarity(sourceName, targetName)>0.8)
			sim=1.5d;
		//If source and target are wordnet match

		
		
		// If the source and target doesn't match try substring match
		else
		{
			sim=this.getSubstringSim(sourceName, targetName);
		}
		return sim;
	}
	
	
	/**
	 * Return the similarity score if the substrings match each other
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	private double getSubstringSim(String sourceName, String targetName)
	{
		StringSimilarity s=new StringSimilarity();
		int count=0;
		double sim=0.0d;
		double score;
		
		
		//Split source into parts 
		ArrayList<String> sparts=this.getParts(sourceName);
		ArrayList<String> tparts=this.getParts(targetName);
		
		for(String t:tparts)
		{
			//count stopwords if only count is greater than 1 
			if (stop.isStopWord(t)&&count<1)
			{
				continue;
			}
			
			for(String sp:sparts)
			{	
				if (stop.isStopWord(sp)&&count<1)
				{
					continue;
				}
				score=s.similarity(sp, t);
				if(score>0.8)
				{
					count++;	
				}
						
			}
		 }
		//find the ratio of matching words
		int totalsize=sparts.size()+tparts.size()-stop.countStopWords(sparts)-stop.countStopWords(tparts);
		if (totalsize>0)
		{
			score=(2*count)/(float)totalsize;
			
			if(score<0.66)
				sim=0;
			else
				sim=score;
			
		}
		return sim;
	}
		
	/**
	 * 	Returns the ArrayList that contains tokenized strings
	 * @param targetName
	 * @return
	 */
	//find substrings
	private ArrayList<String> getParts(String targetName)
	{
		ArrayList<String> tparts=new ArrayList<String>();	
		String[] pt;
		//split String by "_"
		if(targetName.contains("_"))
		{	
			String ts[]=targetName.toLowerCase().split("_");
			for(String x:ts)
			{
				
				tparts.add(x);
			}
		}
		else
			if(!targetName.equals(targetName.toLowerCase()))
			{
				//split string by Uppercase
				pt=targetName.split("(?=\\p{Upper})");	
				for(String x:pt)
				{
					tparts.add(x.toLowerCase());
				}
			}
			else
			{
			
			tparts.add(targetName);
			}
		return tparts;
	}
	
	
	private ArrayList<Double> referenceEvaluation(String pathToReferenceAlignment)
			throws Exception {

	
		// Run the reference alignment matcher to get the list of mappings in
		// the reference alignment file
		ReferenceAlignmentMatcher refMatcher = new ReferenceAlignmentMatcher();


		// these parameters are equivalent to the ones in the graphical
		// interface
		ReferenceAlignmentParameters parameters = new ReferenceAlignmentParameters();
		parameters.fileName = pathToReferenceAlignment;
		parameters.format = ReferenceAlignmentMatcher.OAEI;
		parameters.onlyEquivalence = false;
		parameters.skipClasses = false;
		parameters.skipProperties = false;
		refMatcher.setSourceOntology(this.getSourceOntology());
		refMatcher.setTargetOntology(this.getTargetOntology());

		// When working with sub-superclass relations the cardinality is always
		// ANY to ANY
		if (!parameters.onlyEquivalence) {
			parameters.maxSourceAlign = AbstractMatcher.ANY_INT;
			parameters.maxTargetAlign = AbstractMatcher.ANY_INT;
		}

		refMatcher.setParam(parameters);

		// load the reference alignment
		refMatcher.match();
		
		Alignment<Mapping> referenceSet;
		if (refMatcher.areClassesAligned() && refMatcher.arePropertiesAligned()) {
			referenceSet = refMatcher.getAlignment(); // class + properties
		} else if (refMatcher.areClassesAligned()) {
			referenceSet = refMatcher.getClassAlignmentSet();
		} else if (refMatcher.arePropertiesAligned()) {
			referenceSet = refMatcher.getPropertyAlignmentSet();
		} else {
			// empty set? -- this should not happen
			referenceSet = new Alignment<Mapping>(Ontology.ID_NONE,
					Ontology.ID_NONE);
		}

		// the alignment which we will evaluate
		Alignment<Mapping> myAlignment;

		if (refMatcher.areClassesAligned() && refMatcher.arePropertiesAligned()) {
			myAlignment = getAlignment();
		} else if (refMatcher.areClassesAligned()) {
			myAlignment = getClassAlignmentSet();
		} else if (refMatcher.arePropertiesAligned()) {
			myAlignment = getPropertyAlignmentSet();
		} else {
			myAlignment = new Alignment<Mapping>(Ontology.ID_NONE,
					Ontology.ID_NONE); // empty
		}

		// use the ReferenceEvaluator to actually compute the metrics
		ReferenceEvaluationData rd = ReferenceEvaluator.compare(myAlignment,
				referenceSet);
		
		// optional
		setRefEvaluation(rd);

		// output the report
		StringBuilder report = new StringBuilder();
		report.append("Reference Evaluation Complete\n\n").append(getName())
				.append("\n\n").append(rd.getReport()).append("\n");
		
		
		double precision=rd.getPrecision();
		double recall=rd.getRecall();
		double fmeasure=rd.getFmeasure();
		
		ArrayList<Double> results=new ArrayList<Double>();
		results.add(precision);
		results.add(recall);
		results.add(fmeasure);
		
		return results;

	}

	
	
	public static void main(String[] args) throws Exception {
	
		String ONTOLOGY_BASE_PATH ="conference_dataset/"; // Use your base path
		String[] confs = {"cmt","conference","confOf","edas","ekaw","iasted","sigkdd"};
		
		
		
		MyMatcher mm = new MyMatcher();
		System.setProperty("wordnet.database.dir","wordnet-3.0/dict");
		double precision=0.0d;
		double recall=0.0d;
		double fmeasure=0.0d;
		int size=21;
		
		stop=new StopWords();
		
		for(int i = 0; i < confs.length-1; i++)
		{
			for(int j = i+1; j < confs.length; j++)
			{
				Ontology source = OntoTreeBuilder.loadOWLOntology(ONTOLOGY_BASE_PATH + "/"+confs[i]+".owl");
				Ontology target = OntoTreeBuilder.loadOWLOntology(ONTOLOGY_BASE_PATH + "/"+confs[j]+".owl");
				
				OntologyDefinition def1=new OntologyDefinition(true, source.getURI(), null, null);
				OntologyDefinition def2=new OntologyDefinition(true, target.getURI(), null, null);
		
				def1.largeOntologyMode=false;
				source.setDefinition(def1);
				def2.largeOntologyMode=false;
				target.setDefinition(def2);
				mm.setSourceOntology(source);
				mm.setTargetOntology(target);
		
				DefaultMatcherParameters param = new DefaultMatcherParameters();
		
				//Set your parameters
				param.threshold = 0.0;
				param.maxSourceAlign = 1;
				param.maxTargetAlign = 1;
			//	mm.setName(TARGET_ONTOLOGY);
				mm.setParameters(param);

				try {
					mm.match();			
			
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
			ArrayList<Double> results=	mm.referenceEvaluation(ONTOLOGY_BASE_PATH + confs[i]+"-"+confs[j]+".rdf");
	
			
			precision+=results.get(0);
			recall+=results.get(1);
			fmeasure+=results.get(2);
		
			
			
			}
			
		}

		StringBuilder sb= new StringBuilder();
		
		precision/=size;
		recall/=size;
		fmeasure/=size;
		
		String pPercent = Utility.getOneDecimalPercentFromDouble(precision);
		String rPercent = Utility.getOneDecimalPercentFromDouble(recall);
		String fPercent = Utility.getOneDecimalPercentFromDouble(fmeasure);
		
		
		sb.append("Precision = Correct/Discovered: "+ pPercent+"\n");
		sb.append("Recall = Correct/Reference: "+ rPercent+"\n");
		sb.append("Fmeasure = 2(precision*recall)/(precision+recall): "+ fPercent+"\n");
		
		
		String report=sb.toString();
		System.out.println("Evaulation results:");
		System.out.println(report);

	}
	
 

	
}
