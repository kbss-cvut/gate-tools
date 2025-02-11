package cz.cvut.kbss.nlp.cli;

import org.junit.Test;

public class AnnotationPipelineCLITest {

    @Test
    public void testExecuteWithCorrectArguments() throws Exception {
        String ontology = "src/main/resources/ontology/ontology.owl";
        String mappingsFile = "src/main/resources/ontology/mappings.txt";
        String textFile = "src/test/resources/example-text-to-annotate.txt";
        String output = "example-text-to-annotate--annotated.txt";
//        String  japeRulesRoot = "src/main/resources/jape-rules";
        AnnotationPipelineCLI.main(new String[]{
                "-o", ontology,
                "-m", mappingsFile,
//                "-j", japeRulesRoot,
                "-t", textFile,
                "-f", output,
//                "-ff", "ttl",
//                "-ns", "htt://onto.fel.cvut.cz/ontologies/example-occurrences-of-component-labels-in-text/"
        });
    }
}