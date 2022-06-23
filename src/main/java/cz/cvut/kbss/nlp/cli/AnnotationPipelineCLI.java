package cz.cvut.kbss.nlp.cli;

import cz.cvut.kbss.nlp.cli.util.CmdLineUtils;
import cz.cvut.kbss.nlp.gate.AnnotationPipelinePrototype1;
import cz.cvut.kbss.nlp.gate.ConfigurableGatePipeline;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.util.GateException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AnnotationPipelineCLI {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationPipelineCLI.class);

    @Option(name = "-o", aliases = "--input-schema-file", metaVar = "ONTOLOGY_SCHEMA_FILE",
            usage = "Ontology providing schema according to which annotations are made. " +
                    "The ontology should provide at least mapping of prefixes." +
                    "By default ontology.ttl file is searched within directory of input text file.")
    private Path inputSchemeOntologyFile;

    @Option(name = "-m", aliases = "--input-mapping-file", metaVar = "ONTOLOGY_MAPPING_FILE",
            usage = "File providing mapping ontology IRI to file mapping to properly load onotologies " +
                    "imported by the ontology specified in the --input-schema-file parameter" +
                    "By default mappings.txt file is searched within directory of input text file.")
    private Path mappingFile;



    @Option(name = "-n", aliases = "--ontology-name", metaVar = "ONTOLOGY_NAME",
            usage = "Name of the ontology in the ONTOLOGY_SCHEMA_FILE. ")
    private Path inputSchemeOntologyName;

    @Option(name = "-j", aliases = "--jape-rules-dir", metaVar = "JAPE_RULES_DIR",
            usage = "Path to a directory containing jape rules." +
                    "If not specified the jape-rules are  ")
    private Path japeRulesDirectory;

    @Option(name = "-at", aliases = "--annotation-types", metaVar = "JAPE_RULES_DIR",
            usage = "path to a directory containing jape rules." +
                    "If not specified the jape-rules are  ")
    private List<String> annotationTypes;

    @Option(name = "-t", aliases = "--input-text-file", metaVar = "TEXT_FILE",
            usage = "Input text file", required = true)
    private Path inputTextFile;

    private URL nlpPipelineTemplate = ConfigurableGatePipeline.getNlpPipelineTemplateURL();

    public static void main(String[] args) throws Exception {

        AnnotationPipelineCLI asArgs = new AnnotationPipelineCLI();

        // ---------- arguments parsing ------------
        CmdLineParser argParser = new CmdLineParser(asArgs);
        CmdLineUtils.parseCommandLine(args, argParser);

        asArgs.execute();
    }

    public void execute() throws GateException, IOException {
        LOG.trace("initializing GATE");
//        prepare parameters


        Gate.init();



        Document doc = Factory.newDocument(inputTextFile.toUri().toURL());
        String tempPipelineFileName = "nlp-pipeline-" + System.currentTimeMillis() + ".xgapp";
        try {
            LOG.trace("creating pipeline configuration file \"{}\" based on input parameters ", tempPipelineFileName);

            File pipeLine = ConfigurableGatePipeline.createPipelineConfigurationFile(
                    japeRulesDirectory.toString(),
                    inputSchemeOntologyFile.toString(),
                    mappingFile.toString(),
                    tempPipelineFileName
            );

            LOG.trace("tagging input text at \"{}\" with terms in ontology at \"{}\"", inputTextFile, inputSchemeOntologyFile);
            Document output = AnnotationPipelinePrototype1.executeApplication(doc, pipeLine.toURI().toURL());
            AnnotationPipelinePrototype1.createOutputDocument(output, annotationTypes);

            LOG.trace("tagging input text at \"{}\" with terms in ontology at \"{}\"", inputTextFile, inputSchemeOntologyFile);
        }finally {
            File f = new File(tempPipelineFileName);
            if(f.exists())
                f.delete();
        }
    }

//    public void prepareParameters(){
//        inputSchemeOntologyFile
//    }
}
