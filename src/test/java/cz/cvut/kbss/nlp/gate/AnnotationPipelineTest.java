package cz.cvut.kbss.nlp.gate;

import gate.AnnotationSet;
import gate.Document;
import gate.Gate;
import gate.util.GateException;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

public class AnnotationPipelineTest {

    protected static AnnotationPipeline sut;
    public static Document execute_URL() throws GateException, IOException {
        String input_file_name = "input-text.txt";
        String s = AnnotationPipeline.inputApplicationFile;
        sut = new AnnotationPipeline();
        URL url = AnnotationPipeline.class.getClassLoader().getResource(input_file_name);
        Document output = sut.execute(url);
        return output;
    }

    @AfterClass
    public static void cleanup(){
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> sut.close()));
    }

    @Test
    public void testExecute_URL() throws GateException, IOException {
        Document output = execute_URL();
        AnnotationSet as = output.getAnnotations();
        System.out.println(as.size());
    }
}