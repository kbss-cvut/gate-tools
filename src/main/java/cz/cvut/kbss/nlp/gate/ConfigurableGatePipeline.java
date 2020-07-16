package cz.cvut.kbss.nlp.gate;

import cz.cvut.kbss.common.InMemoryURL;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurableGatePipeline {

    private static final String PARAM_QUOTE = "%%%";
    private static final String  JAPE_RULE_SECTION = "%%%JAPE_RULE_SECTION%%%";
    private static final String  JAPE_RULE_NAME = "%%%JAPE_RULE_NAME%%%";
    private static final String  JAPE_RULE_URI = "%%%JAPE_RULE_URI%%%";

    private static final String ONTOLOGY_URI = "%%%ONTOLOGY_URI%%%";
    private static final String ONTOLOGY_NAME = "%%%ONTOLOGY_NAME%%%";
    private static final String MAPPING_URI = "%%%MAPPING_URI%%%";

    
    protected static final String JAPE_RULE_TEMPLATE = "<gate.util.persistence.LanguageAnalyserPersistence>\n" +
            "  <runtimeParams class=\"gate.util.persistence.MapPersistence\">\n" +
            "\t<mapType>gate.util.SimpleFeatureMapImpl</mapType>\n" +
            "\t<localMap>\n" +
            "\t  <entry>\n" +
            "\t\t<string>ontology</string>\n" +
            "\t\t<gate.util.persistence.LRPersistence reference=\"../../../../../gate.util.persistence.LanguageAnalyserPersistence[6]/runtimeParams/localMap/entry[3]/gate.util.persistence.LanguageAnalyserPersistence/initParams/localMap/entry[3]/gate.util.persistence.LRPersistence\"/>\n" +
            "\t  </entry>\n" +
            "\t</localMap>\n" +
            "  </runtimeParams>\n" +
            "  <resourceType>gate.creole.Transducer</resourceType>\n" +
            "  <resourceName>%%%JAPE_RULE_NAME%%%</resourceName>\n" +
            "  <initParams class=\"gate.util.persistence.MapPersistence\">\n" +
            "\t<mapType>gate.util.SimpleFeatureMapImpl</mapType>\n" +
            "\t<localMap>\n" +
            "\t  <entry>\n" +
            "\t\t<string>grammarURL</string>\n" +
            "\t\t<gate.util.persistence.PersistenceManager-RRPersistence>\n" +
            "\t\t  <uriString>%%%JAPE_RULE_URI%%%</uriString>\n" +
            "\t\t</gate.util.persistence.PersistenceManager-RRPersistence>\n" +
            "\t  </entry>\n" +
            "\t</localMap>\n" +
            "  </initParams>\n" +
            "  <features class=\"gate.util.persistence.MapPersistence\">\n" +
            "\t<mapType>gate.util.SimpleFeatureMapImpl</mapType>\n" +
            "\t<localMap/>\n" +
            "  </features>\n" +
            "</gate.util.persistence.LanguageAnalyserPersistence>\n"; 



    /**
     * The generated url wont work with GATE
     * @param gatePipelineTemplate a template xgapp file, this should not be a resource file
     * @param japeRulePaths a collection of file paths to japeRules 
     * @param ontologyPath the path to the ontology used in the pipeline
     * @param mappingPath a path to a text file containing the ontology IRI to file mappings.    
     * @return
     */
    @Deprecated
    public static URL createPipelineConfigurationURLUsingInMemoryUrl(String gatePipelineTemplate, Collection<String> japeRulePaths, String ontologyPath, String mappingPath){
        String pipelineInstance = instantiatePipeline(gatePipelineTemplate, japeRulePaths, ontologyPath, mappingPath);
        String tempURI = "pipeline-" + System.currentTimeMillis();
        return InMemoryURL.getInstance().build(tempURI, pipelineInstance);
    }

    /**
     * Generate a pipeline configuration given the template and its arguments
     * @param gatePipelineTemplate a template xgapp file, this should not be a resource file
     * @param japeRulePaths a collection of file paths to japeRules
     * @param ontologyPath the path to the ontology used in the pipeline
     * @param mappingPath a path to a text file containing the ontology IRI to file mappings.
     * @return
     */
    public static File createPipelineConfigurationFile(String gatePipelineTemplate, Collection<String> japeRulePaths, String ontologyPath, String mappingPath, String output){
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

    public static String instantiatePipeline(String gatePipelineTemplate, Collection<String> japeRulePaths, String ontologyPath, String mappingPath){
        // load template
        String pipelineTemplate = null;

        try {
            pipelineTemplate = IOUtils.toString(new File(gatePipelineTemplate).toURI().toURL(), Charset.defaultCharset());
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
