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
import java.util.*;

public class AnnotationPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractAnnotations.class);

    private static final String OUTPUT_FILE_NAME = "out-Lookup.xml";

    private static final String INPUT_FILE_NAME = "input-text.txt";

    private static final String inputApplicationFile = "reliability-pipeline.xgapp";

    private static List<String> annotTypesToWrite;

    private static final String LOOK_UP = "Lookup";

    public static void main(String[] args) throws GateException, IOException {
        Gate.init();
        Document doc = Factory.newDocument(AnnotationPipeline.class.getClassLoader().getResource(INPUT_FILE_NAME));
        Document output = executeApplication(doc);
        getLookupAnnotations(output);
        annotTypesToWrite = new ArrayList<>();
        annotTypesToWrite.add(LOOK_UP);
        createOutputDocument(doc);
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
        File outputFile = new File(OUTPUT_FILE_NAME);
        FileOutputStream fos = new FileOutputStream(outputFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        OutputStreamWriter out;
        out = new OutputStreamWriter(bos);
        out.write(docXMLString);
        out.close();
        LOG.debug("xml file created with specified annotations");
    }
}
