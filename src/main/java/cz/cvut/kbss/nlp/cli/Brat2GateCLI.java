package cz.cvut.kbss.nlp.cli;


import cz.cvut.kbss.nlp.cli.util.CmdLineUtils;
import cz.cvut.kbss.nlp.gate.ExtractAnnotations;
import gate.*;
import gate.annotation.AnnotationSetImpl;
import gate.creole.brat.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.System.out;


/**
 * Other files are not required as they are inferred from input text file name.
 */
public class Brat2GateCLI {

    private static final Logger LOG = LoggerFactory.getLogger(Brat2GateCLI.class);

    @Option(name = "-p", aliases = "--ontology-prefix-file", metaVar = "PREFIXES_FILE", usage = "Ontology containing prefixes")
    private Path prefixesFile;

    @Option(name = "-t", aliases = "--input-text-file", metaVar = "TEXT_FILE", usage = "Input text file", required = true)
    private Path inputTextFile;

    @Option(name = "-b", aliases = "--input-brat-annotation-file", metaVar = "BRAT_ANNOTATION_FILE", usage = "Input brat annotation file")
    private Path inputBratAnnotationFile;

    @Option(name = "-g", aliases = "--output-gate-file", metaVar = "GATE_DOCUMENT_FILE", usage = "Output gate document file")
    private Path outputGateDocumentFile;

    @Option(name = "-d", aliases = "--output-instance-data-file", metaVar = "INSTANCE_DATA_FILE", usage = "Output ontology file with instance data")
    private Path outputInstanceDataFile;


    public static void main(String[] args) throws Exception {

        Brat2GateCLI asArgs = new Brat2GateCLI();

        // ---------- arguments parsing ------------
        CmdLineParser argParser = new CmdLineParser(asArgs);
        CmdLineUtils.parseCommandLine(args, argParser);

        String output = Arrays.stream(args).collect(Collectors.joining(" "));
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

        if (asArgs.prefixesFile != null) {
            LOG.debug("Using provided ontology prefixes file {}.", asArgs.prefixesFile);
        } else {
            asArgs.prefixesFile = asArgs.inputTextFile.
                    resolveSibling("prefixes.ttl");
            LOG.debug("Using inferred ontology prefixes file {}.", asArgs.prefixesFile);
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

        Gate.init();
        new BratDocumentFormat().init();
        FeatureMap params = Factory.newFeatureMap();
        params.put(Document.DOCUMENT_URL_PARAMETER_NAME, asArgs.inputTextFile.toUri().toURL());
        params.put(Document.DOCUMENT_ENCODING_PARAMETER_NAME, "UTF-8");
        params.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, "text/x-brat");
        Document doc = (Document)Factory.createResource("gate.corpora.DocumentImpl", params);
        try (PrintStream ps = new PrintStream(new FileOutputStream(asArgs.outputGateDocumentFile.toFile()))) {
            ps.println(doc.toXml());
            ps.flush();
        }
    }

}
