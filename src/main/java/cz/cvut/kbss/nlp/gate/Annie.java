package cz.cvut.kbss.nlp.gate;

import gate.*;
import gate.creole.ANNIEConstants;
import gate.creole.annic.Constants;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;
import jdk.nashorn.internal.parser.TokenType;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Annie implements Closeable {
    public static final String ANNIE_CONFIG = "annie-application-state-01.xgapp";


    protected CorpusController application;


    public Annie(String annotationPipeline) throws GateException, IOException {
        application = (CorpusController) PersistenceManager.loadObjectFromFile(new File(annotationPipeline));

    }

    public Annie() throws GateException, IOException {
        URL url = Annie.class.getResource("/" + ANNIE_CONFIG);
        if (!Gate.isInitialised()) {
            Gate.init();
        }
        application = (CorpusController) PersistenceManager.loadObjectFromUrl(url);
    }

    public Document annotate(String file) throws GateException {
        Document doc = ExtractAnnotations.readDocument(file);
        return annotate(doc);
    }

    public Document annotate(Document doc) throws GateException {
        Corpus corpus = Factory.newCorpus("corpus");
        corpus.add(doc);
        application.setCorpus(corpus);
        application.execute();
        application.setCorpus(null);// clean the document
        return doc;
    }

    @Override
    public void close() throws IOException {
        application.cleanup();
    }

    //    public Document test(){
//        Constants.ANNOTATION_SETS_NAMES_TO_EXCLUDE
//    }

    public static void main(String[] args) throws GateException, IOException {
//        ANNIEConstants.
        String file = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\annotations\\BlueSky\\DA42-POH--has-component\\DA42-POH-38.xml";
//        String applicationCfg = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\papers\\2020-07-reliability\\reliability-model\\dev-a-input-documents\\avic\\001-data\\annie-application-state-01.xgapp";
        Document doc = new Annie().annotate(file);
        System.out.println(doc.toXml());
    }
}
