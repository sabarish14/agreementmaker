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
import am.app.ontology.profiling.manual.ManualOntologyProfiler;


public class MyMatcher  extends AbstractMatcher  {

	private static FileOutput writer;
	Synonyms syn;

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
		sourceName = Character.toLowerCase(sourceName.charAt(0)) + sourceName.substring(1); 
		String targetName=target.getLocalName();
		targetName = Character.toLowerCase(targetName.charAt(0)) + targetName.substring(1); 
		double sim=this.getSimilarityScore(sourceName, targetName);
		if (source.isProp())
		{
			double propertySim=0;
			try
			{
				String sourceDomainName=source.getPropertyDomain().getLocalName();
				String targetDomainName=target.getPropertyDomain().getLocalName();
				if (sourceDomainName==null ||targetDomainName==null)
					propertySim=0d;
				else
				{
					propertySim=this.getSimilarityScore(sourceDomainName, targetDomainName);
				}
					
				if (propertySim<0.6)
					sim=0d;
			}
			catch(NullPointerException e)
			{
				
			}	
		}
		return new Mapping(source, target, sim);				
	}
	/**
	 * 
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	private double getSimilarityScore(String sourceName, String targetName)
	{
	
		syn=new Synonyms();
		syn.synonymList();
		StringSimilarity s=new StringSimilarity();
		double sim=0.0d;
		double score;
		// If the source name matches exactly with target name
		if(sourceName.equalsIgnoreCase(targetName))
			sim=2.0d;
		else if (s.similarity(sourceName, targetName)>0.8)
			sim=1.5d;
		else if (syn.isSynonym(sourceName.toLowerCase(), targetName.toLowerCase()))
			sim=1d;
		
		// If the source and target doesn't match try alternate ways to see whether there is match
		else
		{
			int count=0;
			//Split source into parts 
			ArrayList<String> sparts=this.getParts(sourceName);
			ArrayList<String> tparts=this.getParts(targetName);
			for(String t:tparts)
			{
				for(String sp:sparts)
				{	
						score=s.similarity(sp, t);
						if(score>0.8)
						{
							count++;	
						}
						else
						{ 	//check synonyms
							if (syn.isSynonym(t, sp))
									count++;							
						}				
				}
			 }
			//find the ratio of matching words
			float sizes=(sparts.size()+tparts.size());
			float y=(2*count)/sizes;
			if(y>0.7)
				sim=y;
		}
		return sim;
	}
		
		
	/**
	 * 	
	 * @param targetName
	 * @return
	 */
	private ArrayList<String> getParts(String targetName)
	{
		ArrayList<String> tparts=new ArrayList<String>();	
		String[] pt;
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
		int i,j;
		for (i=0;i<referenceSet.size();i++)
		{
			Boolean flag=false;
			for (j=0;j<myAlignment.size();j++)
			{
				if (referenceSet.get(i).equals(myAlignment.get(j)))
				{
					if (referenceSet.get(i).getEntity1().isProp())
						writer.write(referenceSet.get(i).toString(), myAlignment.get(j).toString(), referenceSet.get(i).getEntity1().getPropertyDomain().getLocalName(),referenceSet.get(i).getEntity2().getPropertyDomain().getLocalName());
					else	
						writer.write(referenceSet.get(i).toString(), myAlignment.get(j).toString(), referenceSet.get(i).getEntity1().getParents().toString(),referenceSet.get(i).getEntity2().getParents().toString());
					flag=true;
					myAlignment.remove(j);
				}
					
			}
			if (!flag)
			{
				if (referenceSet.get(i).getEntity1().isProp())
					writer.write("Unmatched:"+referenceSet.get(i).toString(), "", referenceSet.get(i).getEntity1().getPropertyDomain().getLocalName(),referenceSet.get(i).getEntity2().getPropertyDomain().getLocalName());
				else	
					writer.write("Unmatched:"+referenceSet.get(i).toString(), "", referenceSet.get(i).getEntity1().getParents().toString(),referenceSet.get(i).getEntity2().getParents().toString());
				//writer.write("Unmatched "+referenceSet.get(i).toString(),"",referenceSet.get(i).getEntity1().getParents().toString(),referenceSet.get(i).getEntity2().getParents().toString());

			}
				
		}
		
		for (j=0;j<myAlignment.size();j++)
		{
			if (myAlignment.get(j).getEntity1().isProp())
				try
			{
				writer.write("False positives "+myAlignment.get(j).toString(),"",myAlignment.get(j).getEntity1().getPropertyDomain().getLocalName(),myAlignment.get(j).getEntity2().getPropertyDomain().getLocalName());
			}
			catch(NullPointerException e)
			{
				System.out.print("");
			}
			else
				writer.write("False positives "+myAlignment.get(j).toString(),"",myAlignment.get(j).getEntity1().getParents().toString(),myAlignment.get(j).getEntity2().getParents().toString());
		}
		
		
		System.out.println();

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
		
		//log.info(report);
		
		// use system out if you don't see the log4j output
	//	System.out.println(report);

	}

	
	
	public static void main(String[] args) throws Exception {
	
		String ONTOLOGY_BASE_PATH ="conference_dataset/"; // Use your base path
		String[] confs = {"cmt","conference","confOf","edas","ekaw","iasted","sigkdd"};
		//String[] confs = {"cmt","sigkdd"};
		
		
		MyMatcher mm = new MyMatcher();
		System.setProperty("wordnet.database.dir","wordnet-3.0/dict");
		double precision=0.0d;
		double recall=0.0d;
		double fmeasure=0.0d;
		int size=21;
		ArrayList<Double> fscore=new ArrayList<Double>();
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
				ManualOntologyProfiler mop=new ManualOntologyProfiler(source, target);
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
				
			writer=new FileOutput(confs[i]+"-"+confs[j]+".csv");
			writer.writeHeader();
			ArrayList<Double> results=	mm.referenceEvaluation(ONTOLOGY_BASE_PATH + confs[i]+"-"+confs[j]+".rdf");
			//Add fscore to a list
			fscore.add(results.get(2));
			precision+=results.get(0);
			recall+=results.get(1);
			fmeasure+=results.get(2);
			writer.close();
			
			
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
