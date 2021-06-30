package cz.cvut.kbss.nlp.gate;

import gate.*;
import gate.creole.*;
import gate.event.CreoleEvent;
import gate.event.CreoleListener;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

public class AnnotationPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractAnnotations.class);

    private static final String OUTPUT_FILE_NAME = "out-Lookup.xml";

    private static final String INPUT_FILE_NAME = "input-text.txt";

            static final String inputApplicationFile = "NLP-pipeline.xgapp";

    private static List<String> annotTypesToWrite;

    private static final String LOOK_UP = "Lookup";

    private static final String COMPONENT = "Component";

    private static final String RELATION = "Relation";

    protected String applicationPipelineResource;
    protected CorpusController application;

    public static void main(String[] args) throws GateException, IOException {
//        Plugin.Maven
        Gate.init();
        String file = INPUT_FILE_NAME;
        String outputFile = OUTPUT_FILE_NAME;
        URL url = AnnotationPipeline.class.getClassLoader().getResource(file);

//        String file = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\annotations\\BlueSky\\DA42-POH--has-component\\DA42-POH-38.txt";
//        String outputFile = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\annotations\\BlueSky\\DA42-POH--has-component\\DA42-POH-38.txt.out.xml";
//        URL url = new File(file).toURI().toURL();

        Document doc = Factory.newDocument(url);
        Document output = executeApplication(doc);
        getLookupAnnotations(output);
        annotTypesToWrite = new ArrayList<>();
        annotTypesToWrite.add(LOOK_UP);
        annotTypesToWrite.add(COMPONENT);
        annotTypesToWrite.add(RELATION);
//        createOutputDocument(doc);
        createOutputDocument(doc, outputFile);
    }

    public AnnotationPipeline() throws GateException, IOException {
        this(inputApplicationFile);
    }

    public AnnotationPipeline(String applicationPipelineResource) throws GateException, IOException {
        this.applicationPipelineResource = applicationPipelineResource;
        if(!Gate.isInitialised()) {
            Gate.init();
        }
        application = (CorpusController) PersistenceManager.loadObjectFromUrl(AnnotationPipeline.class.getClassLoader().getResource(applicationPipelineResource));
    }

    public void close(){
        application.cleanup();
////        CreoleListener l;
////        CreoleRegister r = Gate.getCreoleRegister();
////        CreoleEvent e;
////        SerialAnalyserController c;
//        Thread t = new Thread(() -> application.cleanup());
//        t.run();
//        try {
//            t.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Annotates the input String
     * @param input
     * @return
     */
    public Document executeOnString(String input) throws GateException, IOException{
        Document doc = Factory.newDocument(input);
        return execute(doc);
    }

    /**
     * Annotates the input document found at documentUrl
     * @param documentUrl
     * @return
     */
    public Document execute(URL documentUrl) throws GateException, IOException{
        Document doc = Factory.newDocument(documentUrl);
        return execute(doc);
    }

    /**
     * Annotates the input document document
     * @param doc
     * @return
     * @throws GateException
     * @throws IOException
     */
    public Document execute(Document doc) throws GateException, IOException{
        Corpus corpus = Factory.newCorpus("corpus");
        corpus.add(doc);
        execute(corpus);
        return doc;
    }

    public Corpus execute(Corpus corpus) throws GateException, IOException{
        application.setCorpus(corpus);
        application.execute();
        return corpus;
    }


    private static Document executeApplication(Document doc) throws ResourceInstantiationException, ExecutionException, IOException, PersistenceException {
        Corpus corpus = Factory.newCorpus("corpus");
        corpus.add(doc);
        CorpusController application = (CorpusController) PersistenceManager.loadObjectFromUrl(AnnotationPipeline.class.getClassLoader().getResource(inputApplicationFile));
        application.setCorpus(corpus);
        application.execute();
        return doc;
    }

    private static void getLookupAnnotations(Document doc) {
        AnnotationSet annotations = doc.getAnnotations();
        annotations.stream().filter(a -> a.getType().equals(LOOK_UP)).forEach(System.out::println);
    }

    private static void createOutputDocument(Document doc) throws IOException {
        createOutputDocument(doc, OUTPUT_FILE_NAME);
    }
    private static void createOutputDocument(Document doc, String output) throws IOException {
        String docXMLString = null;
        if(!annotTypesToWrite.isEmpty()) {
            Set<Annotation> annotationsToWrite = new HashSet<>();
            AnnotationSet defaultAnnots = doc.getAnnotations();
            for (String s : annotTypesToWrite) {
                AnnotationSet annotsOfThisType = defaultAnnots.get(s);
                if (annotsOfThisType != null) {
                    annotationsToWrite.addAll(annotsOfThisType);
                }
            }
            // Generate XML with the specified annotations, in this case, Lookup, Component, and Relation annotations
            docXMLString = doc.toXml(annotationsToWrite);
        }
        // Generate XML with all annotations. Ignored in the current main implementation
        else {
            docXMLString = doc.toXml();
        }
        // Write output to file in home directory
        File outputFile = new File(output);
        FileOutputStream fos = new FileOutputStream(outputFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        OutputStreamWriter out;
        out = new OutputStreamWriter(bos);
        out.write(docXMLString);
        out.close();
        LOG.debug("xml file created with specified annotations");
    }
}
