package cz.cvut.kbss.nlp;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class Vocabulary {

    public static String annotationOntologyPrefix = "https://onto.fel.cvut.cz/ontologies/annotations/";

    public static String webAnnotationOntologyNS = "http://www.w3.org/ns/oa#";
    public static String webAnnotationOntologyPrefix = "oa";

    // TODO use http://www.ontologydesignpatterns.org/cp/owl/semiotics.owl#isDenotedBy
    public static Property isDenotedBy = createProperty("isDenotedBy");

    public static Resource dataPositionSelector = createClass(webAnnotationOntologyNS,  "DataPositionSelector");
    public static Property hasSelector = createProperty(webAnnotationOntologyNS,  "hasSelector");
    public static Property start = createProperty(webAnnotationOntologyNS,  "start");
    public static Property end = createProperty(webAnnotationOntologyNS,  "end");
//    oa:start, oa:end hasSelector
    private static Property createProperty(String localName) {
        return ResourceFactory.createProperty(annotationOntologyPrefix + localName);
    }

    private static Property createProperty(String ns, String name){
        return ResourceFactory.createProperty(ns + name);
    }

    private static Resource createClass(String ns, String name){
        return ResourceFactory.createResource(ns + name);
    }



}
