package cz.cvut.kbss.nlp.gate;

import gate.*;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

public class AnnotationPipelinePrototype {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractAnnotations.class);

    private static final String OUTPUT_FILE_NAME = "out-Lookup.xml";

    private static final String INPUT_FILE_NAME = "input-text.txt";

    private static final String inputApplicationFile = "reliability-pipeline.xgapp";
    private static final String inputApplicationFile1 = "reliability-pipeline1.xgapp";
    private static final String inputApplicationFile2 = "reliability-pipeline2.xgapp";

    private static List<String> annotTypesToWrite;

    private static final String LOOK_UP = "Lookup";

    public static void main(String[] args) throws GateException, IOException {
        executePipelineDA42POH();
    }

    public static void executePipelineExample() throws GateException, IOException {
        Gate.init();
        executePipeline(AnnotationPipelinePrototype.class.getClassLoader().getResource(INPUT_FILE_NAME), OUTPUT_FILE_NAME);
    }




    public static void executePipelineDA42POH() throws GateException, IOException {
        Gate.init();
        String outputDir = "c:\\Users\\user\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\annotations\\BlueSky\\";
        String input2 = "c:\\Users\\user\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\reliability-model\\dev-a-input-documents\\blue-sky\\001-data\\DA42-POH.txt";

//        executePipelineDA42POH(dirStr + "dirDA42-POH--has-component", dirStr + "DA42-POH-aa-rit\\");

        List<File> inputs = new ArrayList<>();
        inputs.add(new File(input2));
        inputs.addAll(Arrays.asList(new File(outputDir + "DA42-POH--has-component\\")
                .listFiles(f -> f.getName().matches("DA42-POH-\\d+.txt"))));

        CorpusController application = (CorpusController) PersistenceManager.loadObjectFromUrl(
                AnnotationPipelinePrototype.class.getClassLoader().getResource(inputApplicationFile));
        executePipelineDA42POH(inputs, outputDir + "DA42-POH-aa-rit\\", application);

        application = (CorpusController) PersistenceManager.loadObjectFromUrl(
                AnnotationPipelinePrototype.class.getClassLoader().getResource(inputApplicationFile2));
        executePipelineDA42POH(inputs,  outputDir + "DA42-POH-aa-ann\\", application);

    }

    public static void executePipelineDA42POH(Collection<File> inputs, String outDirStr, CorpusController application) throws GateException, IOException {

//        File inDir = new File(inDirStr);
        long t = System.currentTimeMillis();

        for (File f : inputs) {
            File out = new File(outDirStr, f.getName().replace(".txt", ".xml"));
            // execute pipeline
            executePipeline(f.toURI().toURL(), out.toString(), application);
        }
    }

    public static void executePipeline(URL url, String outputPath) throws GateException, IOException {
        executePipeline(url, outputPath, null);
    }

    public static void executePipeline(URL url, String outputPath, CorpusController application) throws GateException, IOException {
//        Gate.init();
        Document doc = Factory.newDocument(url);
        Document output = executeApplication(doc, application);
        getLookupAnnotations(output);
        annotTypesToWrite = new ArrayList<>();
//        annotTypesToWrite.add(LOOK_UP); // use empty list to select all the annotation types
//        annotTypesToWrite.add(LOOK_UP);
        createOutputDocument(doc, outputPath);
    }

    private static Document executeApplication(Document doc) throws ResourceInstantiationException, ExecutionException, IOException, PersistenceException {
        return executeApplication(doc, null);
    }
    private static Document executeApplication(Document doc, CorpusController application) throws ResourceInstantiationException, ExecutionException, IOException, PersistenceException {
        Corpus corpus = Factory.newCorpus("corpus");
        corpus.add(doc);
        if(application == null)
            application = (CorpusController) PersistenceManager.loadObjectFromUrl(AnnotationPipelinePrototype.class.getClassLoader().getResource(inputApplicationFile));
        application.setCorpus(corpus);
        application.execute();
        return doc;
    }

    private static void getLookupAnnotations(Document doc) {
        AnnotationSet annotations = doc.getAnnotations();
        annotations.stream().filter(a -> a.getType().equals(LOOK_UP)).forEach(System.out::println);
    }

    private static void createOutputDocument(Document doc, String outputPath) throws IOException {
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
            // Generate XML with the specified annotations, in this case, Lookup annotations
            docXMLString = doc.toXml(annotationsToWrite);
        }
        // Generate XML with all annotations. Ignored in the current main implementation
        else {
            docXMLString = doc.toXml();
        }
        // Write output to file in home directory
        File outputFile = new File(outputPath);
        FileOutputStream fos = new FileOutputStream(outputFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        OutputStreamWriter out;
        out = new OutputStreamWriter(bos);
        out.write(docXMLString);
        out.close();
        LOG.debug("xml file created with specified annotations");
    }
}
