package cz.cvut.kbss.nlp.gate;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
                toURL("src/main/resources/NLP-pipeline-template.xgapp"),
                japeRulePaths,
                "src/main/resources/ontology/ontology.owl",
                "src/main/resources/ontology/mappings.txt"
        );

        return templateInstance;
    }
    @Ignore
    @Test
    public void testExamplePipeLine(){

    }

    protected URL toURL(String path){
        try {
            return new File(path).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testXGAPPTemplate(){
        List<String> japeRulePaths = Arrays.asList(
                        new File("src/main/resources/jape-rules")
                                .listFiles(f -> f.getName().endsWith("jape"))
                ).stream()
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
        String templateInstance = ConfigurableGatePipeline.instantiatePipeline(
                toURL("src/main/resources/NLP-pipeline-template.xgapp"),
                japeRulePaths,
                "src/main/resources/ontology/ontology.owl",
                "src/main/resources/ontology/mappings.txt"
        );


    }

    @Ignore
    @Test
    public void testExperiment() throws IOException {
        // %%%ONTOLOGY_NAME%%% -> ontology.owl
        // %%%MAPPING_URI%%% -> $relpath$/ontology/mappings.txt
        // %%%ONTOLOGY_URI%%% -> $relpath$/ontology/reliability-components.owl
        // %%%JAPE_RULE_SECTION%%% -> /jape-rules/*.jape

        List<String> japeRulePaths = Arrays.asList(
                new File("src/main/resources/jape-rules")
                        .listFiles(f -> f.getName().endsWith("jape"))
        ).stream()
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
        String templateInstance = ConfigurableGatePipeline.instantiatePipeline(
                toURL("src/main/resources/NLP-pipeline-template.xgapp"),
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