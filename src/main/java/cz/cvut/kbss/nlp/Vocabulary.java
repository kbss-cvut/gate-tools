package cz.cvut.kbss.nlp;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class Vocabulary {

    public static String annotationOntologyPrefix = "https://onto.fel.cvut.cz/ontologies/annotations/";

    // TODO use http://www.ontologydesignpatterns.org/cp/owl/semiotics.owl#isDenotedBy
    public static Property isDenotedBy = createProperty("isDenotedBy");


    private static Property createProperty(String localName) {
        return ResourceFactory.createProperty(annotationOntologyPrefix + localName);
    }
}
