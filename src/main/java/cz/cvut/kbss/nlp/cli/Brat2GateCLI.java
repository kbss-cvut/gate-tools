package cz.cvut.kbss.nlp.cli;


import cz.cvut.kbss.nlp.cli.util.CmdLineUtils;
import gate.*;
import gate.creole.brat.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * Other files are not required as they are inferred from input text file name.
 */
public class Brat2GateCLI {

    private static final Logger LOG = LoggerFactory.getLogger(Brat2GateCLI.class);

    @Option(name = "-o", aliases = "--input-schema-file", metaVar = "ONTOLOGY_SCHEMA_FILE",
            usage = "Ontology providing schema according to which annotations are made. " +
                    "The ontology should provide at least mapping of prefixes." +
                    "In case the file contains OWL ontology, its IRI is used to infer name for output data ontology IRI." +
                    "By default ontology.ttl file is searched within directory of input text file.")
    private Path inputSchemeOntologyFile;

    @Option(name = "-t", aliases = "--input-text-file", metaVar = "TEXT_FILE",
            usage = "Input text file", required = true)
    private Path inputTextFile;

    @Option(name = "-b", aliases = "--input-brat-annotation-file", metaVar = "BRAT_ANNOTATION_FILE",
            usage = "Input brat annotation file")
    private Path inputBratAnnotationFile;

    @Option(name = "-g", aliases = "--output-gate-file", metaVar = "GATE_DOCUMENT_FILE",
            usage = "Output gate document file")
    private Path outputGateDocumentFile;

    @Option(name = "-d", aliases = "--output-instance-data-file", metaVar = "ONTOLOGY_DATA_FILE",
            usage = "Output ontology file with instance data.")
    private Path outputInstanceDataFile;

    @Option(name = "-m", aliases = "--mime-type", metaVar = "MIME_TYPE",
            usage = "Mime type of output gate document file i.e. text/plain (default), text/x-brat")
    private String mimeType = "text/plain";

    @Option(name = "-s", aliases = "--brat-server-url", metaVar = "BRAT_SERVER_URL",
            usage = "Brat server url (e.g. https://cvut.cz/brat)")
    private URL bratServerUrl;

    @Option(name = "-h", aliases = "--brat-data-home-directory", metaVar = "DATA_ROOT_DIRECTORY",
            usage = "Root directory of Brat data collections that is shown when redirected to Brat server url")
    private Path bratHome;

    @Option(name = "-c", aliases = "--check-ontology-entities", metaVar = "DATA_ROOT_DIRECTORY",
            usage = "Check whether ontological entities such as OWL classes and OWL object properties " +
                    "exists within provided schema ontology.")
    private boolean checkOntologyEntities = false;

    public static void main(String[] args) throws Exception {

        Brat2GateCLI asArgs = new Brat2GateCLI();

        // ---------- arguments parsing ------------
        CmdLineParser argParser = new CmdLineParser(asArgs);
        CmdLineUtils.parseCommandLine(args, argParser);

        String output = String.join(" ", args);
        LOG.info("Executing brat2gate cli ... " + output);

        // ----- load input model
        Model inputDataModel = ModelFactory.createDefaultModel();

        // ----- infer working directory and input files
        Path inputTextDirectory = asArgs.inputTextFile.toAbsolutePath().getParent();
        String fileId = asArgs.inputTextFile.getFileName().toString().replaceFirst("\\.txt$", "");

        if (asArgs.inputBratAnnotationFile != null) {
            LOG.debug("Using provided input brat annotation file {}.", asArgs.inputBratAnnotationFile);
        } else {
            asArgs.inputBratAnnotationFile = asArgs.inputTextFile.
                    resolveSibling(fileId + ".ann");
            LOG.debug("Using inferred input brat annotation file {}.", asArgs.inputBratAnnotationFile);
        }

        if (asArgs.inputSchemeOntologyFile != null) {
            LOG.debug("Using provided ontology file {}.", asArgs.inputSchemeOntologyFile);
        } else {
            asArgs.inputSchemeOntologyFile = asArgs.inputTextFile.
                    resolveSibling("ontology.ttl");
            LOG.debug("Using inferred ontology file {}.", asArgs.inputSchemeOntologyFile);
        }

        if (asArgs.outputGateDocumentFile != null) {
            LOG.debug("Using provided output gate document file {}.", asArgs.outputGateDocumentFile);
        } else {
            asArgs.outputGateDocumentFile = asArgs.inputTextFile.
                    resolveSibling(fileId + ".xml");
            LOG.debug("Using inferred output gate document file {}.", asArgs.outputGateDocumentFile);
        }

        if (asArgs.outputInstanceDataFile != null) {
            LOG.debug("Using provided output instance data file {}.", asArgs.outputInstanceDataFile);
        } else {
            asArgs.outputInstanceDataFile = asArgs.inputTextFile.
                    resolveSibling(fileId + ".ttl");
            LOG.debug("Using inferred output instance data file {}.", asArgs.outputInstanceDataFile);
        }

        Brat2OntoConfig brat2OntoConfig = new Brat2OntoConfig(
                asArgs.bratServerUrl,
                asArgs.bratHome,
                asArgs.inputTextFile,
                asArgs.checkOntologyEntities
        );

        initializeDocumentFormats(brat2OntoConfig, getModel(asArgs.inputSchemeOntologyFile));
        saveDocumentFormat(asArgs.inputTextFile, asArgs.outputGateDocumentFile, asArgs.mimeType);
    }

    private static Model getModel(Path configFile) throws IOException {
        LOG.info("Loading ontology from file {} ...", configFile);
        return ModelFactory.createDefaultModel().read(configFile.toUri().toURL().toString());
    }

    private static void saveDocumentFormat(Path inputTextFile, Path outputGateDocumentFile, String mimeType) throws Exception {
        FeatureMap params = Factory.newFeatureMap();
        params.put(Document.DOCUMENT_URL_PARAMETER_NAME, inputTextFile.toUri().toURL());
        params.put(Document.DOCUMENT_ENCODING_PARAMETER_NAME, "UTF-8");
        params.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, mimeType);
        Document doc = (Document)Factory.createResource("gate.corpora.DocumentImpl", params);
        try (PrintStream ps = new PrintStream(new FileOutputStream(outputGateDocumentFile.toFile()))) {
            ps.println(doc.toXml());
            ps.flush();
        }
    }

    private static void initializeDocumentFormats(Brat2OntoConfig config, Model ontology) throws Exception {
        Gate.init();
        new BratDocumentFormat().init();
        OntologyHelper ontologyHelper = new OntologyHelper(
                config, ontology
        );
        new OntoDocumentFormat(ontologyHelper).init();
    }

}
