package cz.cvut.kbss.nlp.gate;

import gate.Gate;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Ignore
public class ConfigurableGatePipelineTest extends TestCase {

    public String configurePipelineExample1(){
        List<String> japeRulePaths = Arrays.asList(
                new File("src/main/resources/jape-rules")
                        .listFiles(f -> f.getName().endsWith("jape"))
        ).stream()
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
        String templateInstance = ConfigurableGatePipeline.instantiatePipeline(
                "src/main/resources/NLP-pipeline-template.xgapp",
                japeRulePaths,
                "src/main/resources/ontology/ontology.owl",
                "src/main/resources/ontology/mappings.txt"
        );

        return templateInstance;
    }
    @Ignore
    @Test
    public void testExperiment() throws IOException {
        List<String> japeRulePaths = Arrays.asList(
                new File("src/main/resources/jape-rules")
                        .listFiles(f -> f.getName().endsWith("jape"))
        ).stream()
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
        String templateInstance = ConfigurableGatePipeline.instantiatePipeline(
                "src/main/resources/NLP-pipeline-template.xgapp",
                japeRulePaths,
                "src/main/resources/ontology/ontology.owl",
                "src/main/resources/ontology/mappings.txt"
        );
        URL url = ConfigurableGatePipeline.class.getClassLoader().getResource("NLP-pipeline.xgapp");
        String expected = IOUtils.toString(url);
        System.out.println(templateInstance);

        assertEquals(expected, templateInstance);
    }

}