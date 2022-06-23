package cz.cvut.kbss.nlp.gate;

import cz.cvut.kbss.common.InMemoryURL;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurableGatePipeline {

    public static String NLP_PIPELINE_TEMPLATE = "NLP-pipeline-template.xgapp";
    private static final String PARAM_QUOTE = "%%%";
    private static final String  JAPE_RULE_SECTION = "%%%JAPE_RULE_SECTION%%%";
    private static final String  JAPE_RULE_NAME = "%%%JAPE_RULE_NAME%%%";
    private static final String  JAPE_RULE_URI = "%%%JAPE_RULE_URI%%%";

    private static final String ONTOLOGY_URI = "%%%ONTOLOGY_URI%%%";
    private static final String ONTOLOGY_NAME = "%%%ONTOLOGY_NAME%%%";
    private static final String MAPPING_URI = "%%%MAPPING_URI%%%";


    protected static final String JAPE_RULE_TEMPLATE_RESOURCE_URI = "jape-rule-template.xml";
    protected static final String JAPE_RULE_TEMPLATE;

    static {
        try {
            JAPE_RULE_TEMPLATE = loadResource(JAPE_RULE_TEMPLATE_RESOURCE_URI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getNlpPipelineTemplate() throws IOException {
        return loadResource(File.pathSeparator + NLP_PIPELINE_TEMPLATE);
    }
    public static URL getNlpPipelineTemplateURL(){
        return getResourceURL(File.pathSeparator + NLP_PIPELINE_TEMPLATE);
    }

    public static String loadResource(String resourcePath) throws IOException {
        return IOUtils.toString(getResourceURL(resourcePath));
    }

    public static URL getResourceURL(String resourcePath){
        return ConfigurableGatePipeline.class.getClassLoader()
                .getResource(resourcePath);
    }

    /**
     * The generated url wont work with GATE
     * @param gatePipelineTemplate a template xgapp file, this should not be a resource file
     * @param japeRulePaths a collection of file paths to japeRules 
     * @param ontologyPath the path to the ontology used in the pipeline
     * @param mappingPath a path to a text file containing the ontology IRI to file mappings.    
     * @return
     */
    @Deprecated
    public static URL createPipelineConfigurationURLUsingInMemoryUrl(URL gatePipelineTemplate, Collection<String> japeRulePaths, String ontologyPath, String mappingPath){
        String pipelineInstance = instantiatePipeline(gatePipelineTemplate, japeRulePaths, ontologyPath, mappingPath);
        String tempURI = "pipeline-" + System.currentTimeMillis();
        return InMemoryURL.getInstance().build(tempURI, pipelineInstance);
    }

    public static File createPipelineConfigurationFile(String japeRuleRoot, String ontologyPath, String mappingPath, String output){
        return createPipelineConfigurationFile(getNlpPipelineTemplateURL(), japeRuleRoot, ontologyPath, mappingPath, output);
    }

    public static File createPipelineConfigurationFile(URL gatePipelineTemplate, String japeRuleRoot, String ontologyPath, String mappingPath, String output){
        List<String> japeFilePaths = listJapeFiles(japeRuleRoot);
        return createPipelineConfigurationFile(gatePipelineTemplate, japeFilePaths, ontologyPath, mappingPath, output);
    }

    public static List<String> listJapeFiles(String dir){
        return Arrays.asList(new File(dir)
                .listFiles(f -> f.getName().endsWith("jape"))).stream()
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
    }

    /**
     * Generate a pipeline configuration given the template and its arguments
     * @param gatePipelineTemplate a template xgapp file, this should not be a resource file
     * @param japeRulePaths a collection of file paths to japeRules
     * @param ontologyPath the path to the ontology used in the pipeline
     * @param mappingPath a path to a text file containing the ontology IRI to file mappings.
     * @return
     */
    public static File createPipelineConfigurationFile(URL gatePipelineTemplate, Collection<String> japeRulePaths, String ontologyPath, String mappingPath, String output){
        String pipelineInstance = instantiatePipeline(gatePipelineTemplate, japeRulePaths, ontologyPath, mappingPath);
        String temp = output;
        try {
            try(Writer w = new FileWriter(temp)) {
                IOUtils.write(pipelineInstance, w);
                w.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new File(temp);
    }

    public static String instantiatePipeline(URL gatePipelineTemplate, Collection<String> japeRulePaths, String ontologyPath, String mappingPath){
        // load template
        String pipelineTemplate = null;

        try {
            pipelineTemplate = IOUtils.toString(gatePipelineTemplate, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // instantiate the template
        String pipelineInstance = pipelineTemplate
                .replaceAll(ONTOLOGY_NAME, new File(ontologyPath).getName())
                .replaceAll(ONTOLOGY_URI, new File(ontologyPath).toURI().toString())
                .replaceAll(MAPPING_URI, new File(mappingPath).toURI().toString())


                .replaceAll(JAPE_RULE_SECTION,
                        japeRulePaths.stream()
                        .map(p -> instantiateJapeRuleXmlElement(p))
                        .collect(Collectors.joining("\n")));


        return pipelineInstance;
    }
    
    protected static String instantiateJapeRuleXmlElement(String path){
        File f = new File(path);
        String japeRuleName = f.getName();
        int i = japeRuleName.lastIndexOf('.');
        if(i > 0) 
            japeRuleName = japeRuleName.substring(0, i);
        String japeRuleURI = f.toURI().toString();
        return instantiateJapeRuleXmlElement(japeRuleName, japeRuleURI);
    }

    protected static String instantiateJapeRuleXmlElement(String japeRuleName, String japeRuleUri){
        return JAPE_RULE_TEMPLATE
                .replaceAll(JAPE_RULE_NAME, japeRuleName)
                .replaceAll(JAPE_RULE_URI, japeRuleUri);
    }


}
