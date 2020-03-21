package gate.creole.brat;

import cz.cvut.kbss.exception.BratProcessingException;
import cz.cvut.kbss.exception.OntologyRequirementsException;
import cz.cvut.kbss.nlp.Vocabulary;
import gate.creole.brat.annotations.BratAnnotation;
import gate.creole.brat.annotations.Relation;
import gate.creole.brat.annotations.TextBound;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to create ontological entities from brat annotations.
 */
public class OntologyHelper {


    private final Brat2OntoConfig config;
    private final Model schemaOntology;
    private final String LOCAL_NAME_DELIMITER = "/";
    private OntModel dataOntology;
    private Map<String, Resource> bratAnnId2resource = new HashMap<>();

    public OntologyHelper(Brat2OntoConfig config, Model schemaOntology) {
        this.schemaOntology = schemaOntology;
        this.config = config;
        createDataOntologyModel();
    }

    /**
     * Returns Iri of previously created OWLNamedIndividual.
     *
     * @param bratAnnotationId Identifier that was used to create the instance.
     * @return Iri of the instance.
     */
    private Resource getIndividual(String bratAnnotationId) {
        return bratAnnId2resource.get(bratAnnotationId);
    }

    /*
     * Creates OWLNamedIndividual from brat TextBound and asserts its types.
     *
     * @param textBound Annotation representing assertion of an individual.
     * @return
     */
    public Individual createIndividual(TextBound textBound) {
        String iri = getSchemaOntologyResource().toString() +
                LOCAL_NAME_DELIMITER +
                textBound.getText() + "-" + getTypeRelatedLocalAnnotationId(textBound);

        Individual individual = dataOntology.createIndividual(
                iri,
                getClass(textBound)
        );

        individual.addLiteral(Vocabulary.isDenotedBy, textBound.getText());
        individual.addLiteral(RDFS.label, textBound.getText());
        individual.addLiteral(DCTerms.source, config.getBratDataUrl(textBound.getID()));
        bratAnnId2resource.put(textBound.getID(), individual);

        return individual;
    }

    /**
     * Returns Iri of ontology that is specified by provided brat id.
     *
     * @return Iri of an ontology
     */
    public String getSchemeOntologyIri() {
        return getSchemaOntologyResource().getURI();
    }

    /**
     * Returns resource that is used to express type of individual annotated in <code>textBound</code>.
     * It is assumed that such resource is represented as OWLClass within scheme ontology.
     * @param textBound An annotation representing individual of a type.
     * @return Resource representing the type.
     */
    public Resource getClass(TextBound textBound) {
        return getClass(textBound.getType());
    }

    /**
     * Returns Iri of an OWL object property specified by provided brat id.
     *
     * @param relation Annotation of a relation.
     * @return Iri of the class
     */
    public ObjectProperty getObjectProperty(Relation relation) {
        String iri = getFullURI(relation.getType());
        checkEntity(iri, OWL2.ObjectProperty);

        return dataOntology.createObjectProperty(iri);
    }

    private Resource getClass(String bratTextBoundType) {
        String iri = getFullURI(bratTextBoundType);
        checkEntity(iri, OWL2.Class);

        return dataOntology.getResource(iri);
    }

    private void checkEntity(String owlEntityIri, Resource entityType) {
        if (config.isCheckOntologyEntities()) {
            schemaOntology.getResource(owlEntityIri).listProperties(RDF.type)
                    .filterKeep(
                            r -> r.getObject().equals(OWL2.Class)
                    ).nextOptional().orElseThrow(() -> new BratProcessingException(
                    "OWL entity <" + owlEntityIri + "> is not of type " + OWL2.class + ". " +
                            "Make sure you disable this check if it is not feasible for your use case."
            ));
        }
    }

    /**
     * Creates OWL object property assertion from arguments. First argument of relation determines subject,
     * relation type determines predicate and second argument of relation determines object
     * of the triple representing the assertion. It is assumed that individuals representing first and second
     * arguments were already created.
     *
     */
    public void createObjectPropertyAssertion(Relation relation) {

        String firstArgumentId = relation.getArgumentID(0);
        String secondArgumentId = relation.getArgumentID(1);

        ReifiedStatement reifiedSt = dataOntology.createReifiedStatement(
                dataOntology.createStatement(
                        getIndividual(firstArgumentId),
                        getObjectProperty(relation),
                        getIndividual(secondArgumentId)
                )
        );

        reifiedSt.addLiteral(DCTerms.source, config.getBratDataUrl(relation.getID()));
        bratAnnId2resource.put(relation.getID(), reifiedSt);
    }

    private String getFullURI(String bratEntityId) {
        String prefix = bratEntityId.split("_", 2)[0];
        String localName = bratEntityId.split("_", 2)[1];

        String nameSpace = schemaOntology.getNsPrefixMap().get(prefix);

        if (nameSpace == null) {
            throw new RuntimeException("Unable to construct URI from brat entity '" + bratEntityId + "'. " +
                    "Prefix '" + prefix + "' is not defined in configuration file. " +
                    "Loaded prefix mapping is : " + schemaOntology.getNsPrefixMap()
            );
        }
        return nameSpace + localName;
    }

    private void createDataOntologyModel() {

        dataOntology = ModelFactory.createOntologyModel();
        Resource schemaOntologyRes = getSchemaOntologyResource();
        Resource dataOntologyRes = dataOntology.createResource(schemaOntologyRes +
                "/" + config.getBratRelativeDataHome() +
                "/" + config.getBratDataName());
        dataOntology.add(dataOntologyRes, RDF.type, OWL2.Ontology);
        dataOntology.add(dataOntologyRes, OWL2.imports, schemaOntologyRes);
        dataOntology.add(dataOntologyRes, DCTerms.source, config.getBratDataUrl());
    }

    private Resource getSchemaOntologyResource() {
        ResIterator schemaIriIt = schemaOntology.listSubjectsWithProperty(RDF.type, OWL2.Ontology);
        if (!schemaIriIt.hasNext()) {
            throw new OntologyRequirementsException("Missing triple pattern" + getTriplePattern(RDF.type, OWL2.Ontology));
        }
        return schemaIriIt.next();
    }

    private String getTriplePattern(Property p, Resource o) {
        return String.format("(?s, %s, %s)", p, o);
    }

    private String getTypeRelatedLocalAnnotationId(BratAnnotation annotation) {
        return annotation.getID().substring(1);
    }
}
