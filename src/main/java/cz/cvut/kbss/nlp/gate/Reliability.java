package cz.cvut.kbss.nlp.gate;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class Reliability {

    public static String ns = "http://onto.fel.cvut.cz/ontology/reliability/";

    public static String function = "http://onto.fel.cvut.cz/ontology/reliability/function";
    public static String component = "http://onto.fel.cvut.cz/ontology/reliability/component";
    public static String failureMode = "http://onto.fel.cvut.cz/ontology/reliability/failure-mode";

    public static Resource c_function = ResourceFactory.createResource(function);
    public static Resource c_component = ResourceFactory.createResource(component);
    public static Resource c_failureMode = ResourceFactory.createResource(failureMode);

}
