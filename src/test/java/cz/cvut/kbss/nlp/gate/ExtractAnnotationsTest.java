package cz.cvut.kbss.nlp.gate;

import gate.Document;
import gate.util.GateException;
import org.junit.Assert;
import org.junit.Test;

public class ExtractAnnotationsTest {

//    @Test
    public void testWriteDocument(){
    }

    @Test
    public void testReadDocument() throws GateException {
        String file = "gate-outpu.xml";
        Document doc = ExtractAnnotations.readDocument(file);
        Assert.assertNotNull(doc);
    }
}