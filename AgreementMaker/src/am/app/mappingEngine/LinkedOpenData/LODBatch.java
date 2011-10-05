package am.app.mappingEngine.LinkedOpenData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import am.app.mappingEngine.baseSimilarity.advancedSimilarity.AdvancedSimilarityMatcher;
import am.app.mappingEngine.baseSimilarity.advancedSimilarity.AdvancedSimilarityParameters;
import am.app.mappingEngine.hierarchy.HierarchyMatcherModified;
import am.app.ontology.Ontology;
import am.app.ontology.ontologyParser.OntoTreeBuilder;

import com.hp.hpl.jena.util.LocationMapper;

public class LODBatch {
	String report = "";
	
	Logger log;
	
	LocationMapper mapper;
	
	public LODBatch(){
		log = Logger.getLogger(LODBatch.class);
		Logger.getRootLogger().setLevel(Level.DEBUG);	
	}
	
	public void run(){
		//singleRun(LODOntologies.MUSIC_ONTOLOGY, LODOntologies.BBC_PROGRAM, "music-bbc");
		//singleRun(LODOntologies.MUSIC_ONTOLOGY, LODOntologies.DBPEDIA, "music-dbpedia");
		//singleRun(LODOntologies.FOAF, LODOntologies.DBPEDIA, "foaf-dbpedia");
		//singleRun(LODOntologies.GEONAMES, LODOntologies.DBPEDIA, "geonames-dbpedia");
		singleRun(LODOntologies.SIOC, LODOntologies.FOAF, "sioc-foaf");
		//singleRun(LODOntologies.SW_CONFERENCE, LODOntologies.AKT_PORTAL, "swc-akt");
		//singleRun(LODOntologies.SW_CONFERENCE, LODOntologies.DBPEDIA, "swc-dbpedia");
		log.info(report);
	}
	
	public void singleRun(String sourceName, String targetName, String testName){
		long start = System.nanoTime();
		Ontology sourceOntology = null;
		Ontology targetOntology = null;
		OntoTreeBuilder treeBuilder;
				
		log.info("Opening sourceOntology...");
		sourceOntology = OntoTreeBuilder.loadOWLOntology(new File(sourceName).getPath());
		//sourceOntology = LODUtils.openOntology(new File(sourceName).getAbsolutePath());	
		log.info("Done");	
				
		log.info("Opening targetOntology...");
		targetOntology = OntoTreeBuilder.loadOWLOntology(new File(targetName).getPath());
		//targetOntology = LODUtils.openOntology(new File(targetName).getAbsolutePath());
		log.info("Done");
		
		AdvancedSimilarityMatcher asm = new AdvancedSimilarityMatcher();
		asm.setSourceOntology(sourceOntology);
		asm.setTargetOntology(targetOntology);
		
		AdvancedSimilarityParameters asmParam = new AdvancedSimilarityParameters();
		asmParam.useDictionary = true;
		asm.setParam(asmParam);
		
		log.info("ASM matching");				
		try {
			asm.match();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		HierarchyMatcherModified hmm = new HierarchyMatcherModified();
		hmm.setSourceOntology(sourceOntology);
		hmm.setTargetOntology(targetOntology);
		hmm.addInputMatcher(asm);
		
		log.info("HMM matching");
		
		try {
			hmm.match();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			printDocument(testName, hmm.getAlignmentsStrings(true, false), sourceName, targetName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		long end = System.nanoTime();
		long executionTime = (end-start)/1000000;
		report += "Execution times:\t" + asm.getExecutionTime() + "\t" + hmm.getExecutionTime() + "\t" + executionTime + "\n";
		log.info("Total time: " + executionTime);
		
	}
	
	public void printDocument(String name, String alignmentStrings, String source, String target) throws Exception{
		Date d = new Date();
		String toBePrinted = "AGREEMENT DOCUMENT\n\n";
		toBePrinted += "Date: "+d+"\n";
		toBePrinted += "Source Ontology: "+source+"\n";
		toBePrinted += "Target Ontology: "+target+"\n\n";
		toBePrinted += alignmentStrings;
		
		FileOutputStream out = new FileOutputStream("LOD/batch/" + name + ".txt");
	    PrintStream p = new PrintStream( out );
	    String[] lines = toBePrinted.split("\n");
	    for(int i = 0; i < lines.length; i++)
	    	p.println(lines[i]);
	    p.close();
	    out.close();
	}
	
	public static void main(String[] args) {
		LODBatch batch = new LODBatch();
		batch.run();
	}
}
	

