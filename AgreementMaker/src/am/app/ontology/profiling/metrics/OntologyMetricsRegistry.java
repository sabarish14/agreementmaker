package am.app.ontology.profiling.metrics;

import am.app.ontology.profiling.metrics.annotation.AnnotationStringLengthMetric;
import am.app.ontology.profiling.metrics.context.ChildrenCountMetric;
import am.app.ontology.profiling.metrics.context.ParentsCountMetric;
import am.app.ontology.profiling.metrics.context.SiblingCountMetric;

/**
 * Keep track of ontology metrics.
 * 
 * @author cosmin
 *
 */
public enum OntologyMetricsRegistry {

	InheritanceCount	("Parents Count", ParentsCountMetric.class ),
	SiblingCount		("Sibling Count", SiblingCountMetric.class ),
	ChildCount			("Child Count", ChildrenCountMetric.class ),
	StringLength		("String Length", AnnotationStringLengthMetric.class ),
	;
	
	private String name;
	private Class<? extends OntologyMetric> className;
	
	/* Constructor */
	OntologyMetricsRegistry( String n, Class<? extends OntologyMetric> matcherClass ) { 
		name = n; className = matcherClass;
	}
	
	/* Getters and Setters */
	public String getMetricName() { return name; }
	public Class<? extends OntologyMetric> getMetricClass() { return className; }
	
	@Override
	public String toString() {
		return name;
	}	
}
