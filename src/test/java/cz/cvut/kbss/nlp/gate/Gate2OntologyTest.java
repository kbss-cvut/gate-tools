package cz.cvut.kbss.nlp.gate;

import gate.Document;
import gate.util.GateException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class Gate2OntologyTest {


    @Test
    public void test_process_Document_created_by_AnnotationPipeline_execute_Document() throws GateException, IOException {
        // load document using the
        Document d = AnnotationPipelineTest.execute_URL();

        Gate2Ontology sut = new Gate2Ontology();
        Model m = sut.process(d, d.getSourceUrl() + "/" + "references/");
        m.write(System.out, Lang.TTL.getName(), null);
    }
}