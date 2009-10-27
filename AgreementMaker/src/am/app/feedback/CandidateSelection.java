package am.app.feedback;

import am.app.feedback.measures.FamilyRelationships;
import am.app.mappingEngine.Alignment;
import am.app.mappingEngine.AlignmentSet;

public class CandidateSelection {

	
	InitialMatchers imStack;  // initial matchers stack
	
	
	/**
	 * Constructor
	 * @param matched - The AlignmentSet of currently matched concepts
	 */
	CandidateSelection( InitialMatchers im ) {
		
		imStack = im;
	}
	
	public AlignmentSet<Alignment> getTopCandidates() {
		
		AlignmentSet<CandidateMapping> candidateSet = new AlignmentSet<CandidateMapping>();
		AlignmentSet<Alignment> topCandidates = new AlignmentSet<Alignment>();
		
		FamilyRelationships fr = new FamilyRelationships(this);
		
		
		
		
		return topCandidates;
		
	}

	public AlignmentSet<ExtendedAlignment> getCurrentAlignments() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
