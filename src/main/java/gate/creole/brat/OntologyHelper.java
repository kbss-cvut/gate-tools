package gate.creole.brat;

import cz.cvut.kbss.exception.BratProcessingException;
import cz.cvut.kbss.exception.OntologyRequirementsException;
import cz.cvut.kbss.nlp.Vocabulary;
import gate.creole.brat.annotations.BratAnnotation;
import gate.creole.brat.annotations.Note;
import gate.creole.brat.annotations.Relation;
import gate.creole.brat.annotations.TextBound;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
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

    private static Resource getOntologyResource(Model model) {
        ResIterator ontology = model.listSubjectsWithProperty(RDF.type, OWL2.Ontology);
        if (!ontology.hasNext()) {
            throw new OntologyRequirementsException("Missing triple pattern" + getTriplePattern(RDF.type, OWL2.Ontology));
        }
        return ontology.next();
    }

    private static String getTriplePattern(Property p, Resource o) {
        return String.format("(?s, %s, %s)", p, o);
    }

    private static AnonId getStatementId(Statement statement) {
        StringBuilder statementBuff = new StringBuilder();
        statementBuff
                .append("(")
                .append("<").append(statement.getSubject().getURI()).append(">, ")
                .append("<").append(statement.getPredicate().getURI()).append(">, ")
                .append("<").append(statement.getObject().asResource().getURI()).append(">")
                .append(")");
        return AnonId.create(
                DigestUtils.md5Hex(statementBuff.toString())
        );
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
        String iri = getOntologyResource(schemaOntology).toString() +
                LOCAL_NAME_DELIMITER +
                getNormalizedName(textBound.getText()) + "-" + getTypeRelatedLocalAnnotationId(textBound);

        Individual individual = dataOntology.createIndividual(
                iri,
                getClass(textBound)
        );

        individual.addProperty(RDF.type, OWL2.NamedIndividual);
        individual.addLiteral(Vocabulary.isDenotedBy, textBound.getText());
        individual.addLiteral(RDFS.label, textBound.getText());
        if (config.isReferenceBratServer()) {
            individual.addLiteral(DCTerms.source, config.getBratDataUrl(textBound.getID()));
        }
        bratAnnId2resource.put(textBound.getID(), individual);

        return individual;
    }

    /*
     * Annotate RDF resource to which brat Note refers to. If there is a note for the resource,
     * it will become both rdfs:label and rdfs:comment.
     *
     * @param note Annotation representing note related to RDF resource.
     * @return
     */
    public Resource annotateResource(Note note) {
        Resource resource = getIndividual(note.getTarget());
        resource.addLiteral(RDFS.label, note.getText());
        resource.addLiteral(RDFS.comment, note.getText());
        return resource;
    }

    /**
     * Returns Iri of ontology that is specified by provided brat id.
     *
     * @return Iri of an ontology
     */
    public String getSchemeOntologyIri() {
        return getOntologyResource(schemaOntology).getURI();
    }

    /**
     * Returns resource that is used to express type of individual annotated in <code>textBound</code>.
     * It is assumed that such resource is represented as OWLClass within scheme ontology.
     *
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

    public OntModel getDataOntology() {
        return this.dataOntology;
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
     */
    public void createObjectPropertyAssertion(Relation relation) {

        String firstArgumentId = relation.getArgumentID(0);
        String secondArgumentId = relation.getArgumentID(1);

        Statement st = dataOntology.createStatement(
                getIndividual(firstArgumentId),
                getObjectProperty(relation),
                getIndividual(secondArgumentId)
        );

        dataOntology.add(st);
        if (config.isReferenceBratServer()) {
            Resource reifiedSt = addReifiedStatment(st);
            reifiedSt.addLiteral(DCTerms.source, config.getBratDataUrl(relation.getID()));
            bratAnnId2resource.put(relation.getID(), reifiedSt);
        }
    }

    private Resource addReifiedStatment(Statement statement) {

        Resource reifiedStatement = dataOntology.createResource(getStatementId(statement));

        reifiedStatement
                .addProperty(RDF.type, OWL2.Axiom)
                .addProperty(OWL2.annotatedSource, statement.getSubject())
                .addProperty(OWL2.annotatedProperty, statement.getPredicate())
                .addProperty(OWL2.annotatedTarget, statement.getObject());

        return reifiedStatement;
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
        Resource schemaOntologyRes = getOntologyResource(schemaOntology);
        Resource dataOntologyRes = createDataOntologyResource();
        dataOntology.add(dataOntologyRes, RDF.type, OWL2.Ontology);
        dataOntology.add(dataOntologyRes, OWL2.imports, schemaOntologyRes);
        if (config.isReferenceBratServer()) {
            dataOntology.setNsPrefix("dcterms", DCTerms.NS);
            dataOntology.add(DCTerms.source, RDF.type, OWL2.AnnotationProperty);
            dataOntology.add(dataOntologyRes, DCTerms.source, config.getBratDataUrl());
        }
        dataOntology.setNsPrefixes(schemaOntology.getNsPrefixMap());
        dataOntology.setNsPrefix("", getOntologyResource(dataOntology).getURI() + LOCAL_NAME_DELIMITER);

    }

    private Resource createDataOntologyResource() {
        String relativePath =
                (config.getBratRelativeDataHome() == null) ? "" : "/" + config.getBratRelativeDataHome();
        return dataOntology.createResource(
                getOntologyResource(schemaOntology) +
                        relativePath +
                        "/" + config.getBratDataName()
        );
    }

    private String getTypeRelatedLocalAnnotationId(BratAnnotation annotation) {
        return annotation.getID().substring(1);
    }

    private String getNormalizedName(String name) {
        return name.replaceAll("[ \\[\\]{}()_:\\/\\\\;.<>]", "-");
    }
}

