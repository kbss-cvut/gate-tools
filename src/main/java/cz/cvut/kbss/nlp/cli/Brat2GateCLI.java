package cz.cvut.kbss.nlp.cli;


import cz.cvut.kbss.nlp.cli.util.CmdLineUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * Other files are not required as they are inferred from input text file name.
 */
public class Brat2GateCLI {

    // cat input-data.rdf | sem-pipes execute --instance "<http://url>"
    //                   --config-file "$PATH/config.ttl"
    //                   --input-binding-file "$PATH/input-binding.ttl" --output-binding-file "$PATH/output-binding.ttl"
    //                   --input-file --output-file
    // > output.data.rdf

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


    public static void main(String[] args) throws IOException {

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
//        if (asArgs.isInputDataFromStdIn) {
//            LOG.info("Loading input data from std-in ...");
//            inputDataModel.read(System.in, null, FileUtils.langTurtle);
//        }
//
//        LOG.info("Processing successfully finished.");
//       // outputExecutionContext.getDefaultModel().write(System.out);
//
//
//        // return output data
//        if (asArgs.outputRdfFile != null) {
//            outputExecutionContext.getDefaultModel().write(new FileOutputStream(asArgs.outputRdfFile), FileUtils.langTurtle);
//        } else {
//            outputExecutionContext.getDefaultModel().write(System.out, FileUtils.langTurtle);
//        }
//
//        return;
    }





}
