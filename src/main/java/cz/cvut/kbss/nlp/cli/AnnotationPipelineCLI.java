package cz.cvut.kbss.nlp.cli;

import cz.cvut.kbss.nlp.cli.util.CmdLineUtils;
import cz.cvut.kbss.nlp.gate.AnnotationPipelinePrototype1;
import cz.cvut.kbss.nlp.gate.ConfigurableGatePipeline;
import cz.cvut.kbss.nlp.gate.ExtractAnnotations;
import cz.cvut.kbss.nlp.gate.Gate2Ontology;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.util.GateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private String inputSchemeOntologyName;

    @Option(name = "-j", aliases = "--jape-rules-dir", metaVar = "JAPE_RULES_DIR",
            usage = "Path to a directory containing jape rules." +
                    "If not specified the jape-rules are  ")
    private Path japeRulesDirectory;

    @Option(name = "-at", aliases = "--annotation-types", metaVar = "ANNOTATION_TYPES",
            usage = "path to a directory containing jape rules." +
                    "If not specified the jape-rules are  ")
    private List<String> annotationTypes;

    @Option(name = "-t", aliases = "--input-text-file", metaVar = "TEXT_FILE",
            usage = "Input text file", required = true)
    private Path inputTextFile;

    @Option(name = "-f", aliases = "--output-file", metaVar = "TEXT_FILE",
            usage = "Path where to save the output")
    private Path outputFile;


    @Option(name = "-ff", aliases = "--file-format", metaVar = "TEXT_FILE",
            usage = "Output file format, either 'gate' or 'ttl'.")
    private OutputFileFormat fileFormat;


    @Option(name = "-ns", aliases = "--name-space", metaVar = "TEXT_FILE",
            usage = "URI namespace used to generate the TTL output. Used only with --file-format ttl.")
    private String namespace;


    enum OutputFileFormat{
        gate,
        ttl;
    }

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
        prepareParameters();
        // Init gate
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

            switch (fileFormat){
                case gate: outputAsGateDocument(output);break;
                case ttl: outputAsTurtle(output);break;
            }

            LOG.trace("tagging input text at \"{}\" with terms in ontology at \"{}\"", inputTextFile, inputSchemeOntologyFile);
        }finally {
            File f = new File(tempPipelineFileName);
            if(f.exists())
                f.delete();
        }
    }

    protected void outputAsGateDocument(Document output) throws IOException {
        AnnotationPipelinePrototype1.createOutputDocument(output, outputFile);
    }

    protected void outputAsTurtle(Document output){
        Gate2Ontology converter = new Gate2Ontology();
        Model m = converter.process(output, namespace);
        ExtractAnnotations.saveModel(m,  outputFile.toFile());
    }

    public void prepareParameters(){
        if(inputSchemeOntologyFile == null){
            inputSchemeOntologyFile = Paths.get(getResourceURI("./ontology/ontology.owl"));
        }

        if(mappingFile == null){
            mappingFile = Paths.get(getResourceURI("./ontology/mappings.txt"));
        }

        if(japeRulesDirectory == null){
            japeRulesDirectory = Paths.get(getResourceURI("./jape-rules"));
        }



        fileFormat = fileFormat == null ? OutputFileFormat.gate : fileFormat;

        if(fileFormat == OutputFileFormat.ttl && StringUtils.isBlank(namespace)){
            namespace = "http://onto.fel.cvut.cz/ontologies/annotations-" + System.currentTimeMillis() + "/";
        }

        if(outputFile == null){
            String name = inputTextFile.toFile().getName();

            String extension = null;
            if(fileFormat == OutputFileFormat.ttl)
                extension = "ttl";
            else if(fileFormat == OutputFileFormat.gate)
                extension = "xml";

            name = name.substring(0, name.lastIndexOf('.')) + extension;
            outputFile = Paths.get(name);
        }
    }

    protected URI getResourceURI(String pathToResource){
        try {
            return getResourceURL(pathToResource).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected URL getResourceURL(String pathToResource){
        return AnnotationPipelineCLI.class.getClassLoader().getResource(pathToResource);
    }
}
