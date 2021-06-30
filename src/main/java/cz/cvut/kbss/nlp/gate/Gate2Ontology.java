package cz.cvut.kbss.nlp.gate;

import cz.cvut.kbss.nlp.temit.Vocabulary;
import gate.Annotation;
import gate.Document;
import gate.GateConstants;
import gate.creole.ANNIEConstants;
import gate.creole.FeatureSchema;
import gate.creole.annic.Constants;
import gate.creole.brat.Brat2OntoConfig;
import gate.creole.brat.OntologyHelper;
import gate.gui.FeaturesSchemaEditor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import javax.jws.WebParam;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Gate2Ontology {
    private OntologyHelper ontologyHelper;


    public Model process(Document d, String ns){
//        printAnnotationTypesAndFeatures(d);
        LookupConverter converter = new LookupConverter();
        Model model = ModelFactory.createDefaultModel();
        converter.convert(d, model, ns);
        return model;
    }


    public class LookupConverter{
        public static final String ELEMENT_TYPE_FEATURE_NAME="type";
        public static final String ELEMENT_TYPE_FEATURE_CLASS_VALUE="class";
        public static final String ELEMENT_TYPE_FEATURE_INSTANCE_VALUE="instance";
        public static final String INSTANCE_LOOKUP="type";
        public static final String ELEMENT_URI="URI";
        public static final String PROPERTY_URI="propertyURI";


        // instances
        public static final String a="propertyURI";
        public static final String CLASS_URI="classURI";

        protected Document d;
        protected Resource documentUri;
        protected String ns;


        public Model convert(Document d, String ns){
            Model model = ModelFactory.createDefaultModel();
            convert(d, model, ns);
            return model;
        }

        public void convert(Document d, Model m, String ns){
            this.d = d;
            documentUri = m.createResource(d.getSourceUrl().getRef());
            this.ns = ns;
            d.getAnnotations().stream()
                    .filter(a -> ANNIEConstants.LOOKUP_ANNOTATION_TYPE.equals(a.getType()))
                    .forEach(a -> convert(m, a));
        }

        protected void convert(Model m, Annotation a){
            String elementType = getStringFeature(a, ELEMENT_TYPE_FEATURE_NAME, null);

            switch (elementType){
                case ELEMENT_TYPE_FEATURE_CLASS_VALUE:
                case ELEMENT_TYPE_FEATURE_INSTANCE_VALUE: createReference(m, a);break;
                default: // NOOP
            }
        }

        public void createReference(Model m, Annotation a){
//            String clsUri = getStringFeature(a, CLASS_URI, null);
            String uri = getStringFeature(a, ELEMENT_URI, null);
//            String label = ExtractAnnotations.getAnnotatedText(d, a);
            if(uri != null){
                Resource referenced = m.createResource(uri);
                Resource reference = m.createResource(ns + "reference-" + a.getId());
                Resource target = m.createResource(ns + "reference-target-" + a.getId());
                reference.addProperty(RDF.type, m.createResource(Vocabulary.s_c_navrzeny_vyskyt_termu));
                reference.addProperty(RDF.type, m.createResource(Vocabulary.s_c_souborovy_vyskyt_termu));
                reference.addProperty(m.createProperty(Vocabulary.s_i_je_prirazenim_termu), referenced);
                reference.addProperty(m.createProperty(Vocabulary.s_i_ma_cil), target);
                target.addProperty(RDF.type, m.createResource(Vocabulary.s_i_ma_cil_souboroveho_vyskytu));
                target.addProperty(m.createProperty(Vocabulary.s_i_ma_zdroj), documentUri);
            }
        }




    }

    protected String getStringFeature(Annotation a, String featureName, String defaultValue){
        return Optional.ofNullable(a.getFeatures().get(featureName)).map(Objects::toString).orElse(defaultValue);
    }

    public static void printAnnotationTypesAndFeatures(Document d){
        System.out.println(d.toXml());
        d.getAnnotations().stream().collect(Collectors.groupingBy(a -> a.getType()))
                .entrySet().stream()
                .map(e -> String.format("%s {\n%s\n}",
                        e.getKey(),
                        e.getValue().stream()
                                .flatMap(a -> a.getFeatures().keySet().stream()
                                        .map(f -> f.toString()))
                                        .collect(Collectors.groupingBy(s -> s, Collectors.counting())).entrySet().stream()
                                .map(es -> String.format("\t%s : %d", es.getKey(), es.getValue()))
                                .collect(Collectors.joining("\n"))
                        )
                ).forEach(s -> System.out.println(s));//collect(Collectors.toList());
    }
}
