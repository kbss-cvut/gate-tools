package cz.cvut.kbss.nlp.gate;

import gate.*;
import gate.corpora.DocumentXmlUtils;
import gate.corpora.XmlDocumentFormat;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

public class ExtractAnnotations {

    protected static String ns = "http://onto.fel.cvut/ontologies/bluesky/DA42-POH/";

    protected static MessageDigest md5;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    protected static Resource getType(String annotationType){
        // normalize annotation type string
        switch (annotationType){
            case "function": return Vocabulary.c_function;
            case "component": return Vocabulary.c_component;
            case "failure-mode": return Vocabulary.c_failureMode;
            default: return null;
        }
    }

    protected static String normalizedAnnotationType(String annotationType){
        return annotationType.trim().toLowerCase().replaceAll("\\s", "-");
    }

    protected static String constructUri(String annotatedText, String prefix) throws Exception {
        // normalize string
        String norm = annotatedText
                .trim()
                .replaceAll("\\s", "-");
        byte[] bytes = md5.digest(norm.getBytes("UTF-8"));

        norm = DatatypeConverter.printHexBinary(bytes).toUpperCase();
//        try {
//            norm = URLEncoder.encode(norm, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        // TODO: fix this simplified creation of uri's local name
        return String.format("%s%s-%s", ns, prefix, norm);
    }

    public static File changeExtension(File f, String extension){
        String name = f.getName();
        name = name.substring(0,f.getName().lastIndexOf(".")) + "." + extension;
        return new File(f.getParent(), name);
    }

    public static void saveModel(Model m, File f){
        try(OutputStream os = new FileOutputStream(f)){
            m.write(os, "ttl");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Model extractOntologyFromOATAnnoations(Document d) throws Exception {
        String  mentionType = "Mention";
        AnnotationSet as = d.getAnnotations("Original markups");
        DocumentContent dc = d.getContent();
        System.out.println(d.getNamedAnnotationSets().size());

        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("rel", Vocabulary.ns);
        m.setNsPrefix("rdf", RDF.uri);
        m.setNsPrefix("rdfs", RDFS.uri);

        for(Annotation a : as){
            String annotationType = a.getType();
            if(!mentionType.equals(annotationType))
                continue;
            String annotatedText = dc.getContent(a.getStartNode().getOffset() , a.getEndNode().getOffset()).toString().trim();
            Object objType = a.getFeatures().get("class");
            if(objType != null) {
                System.out.println(a.getType() + " : " + annotatedText);
                String ontologyClass = objType.toString();
                Resource type = m.createResource(ontologyClass);
                String ontologyClassLocalName = type.getLocalName();

                String newType = constructUri(annotatedText , ontologyClassLocalName);
                Resource r = m.createResource(newType, RDFS.Class);
                r.addProperty(RDFS.subClassOf, type)
                        .addLiteral(RDFS.label, annotatedText);
            }
        }
        return m;
    }

    public static Model extractAnnotations(Document d) throws Exception{
        AnnotationSet as = d.getAnnotations("Function");
        DocumentContent dc = d.getContent();
        System.out.println(d.getNamedAnnotationSets().size());

        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("rel", Vocabulary.ns);
        m.setNsPrefix("rdf", RDF.uri);
        m.setNsPrefix("rdfs", RDFS.uri);

        for(Annotation a : as){
            String annotationType = a.getType();
            String annotatedText = dc.getContent(a.getStartNode().getOffset() , a.getEndNode().getOffset()).toString().trim();
            System.out.println(a.getType() + " : " + annotatedText);
            String normalizedAnnotationType = normalizedAnnotationType(annotationType);
            Resource type = getType(normalizedAnnotationType);
            String newType = constructUri(annotatedText , normalizedAnnotationType);
            m.createResource(newType, RDFS.Class)
                    .addProperty(RDFS.subClassOf, type)
                    .addLiteral(RDFS.label, annotatedText);
        }
        return m;
    }

    public static void main(String[] args) throws Exception {
        String file = "c:/Users/user/Documents/skola/projects/2019-msmt-inter-excelence/code/semantic-reliability/reliability-model/dev-b-input-analysis/001-data_blue-sky_manua-text-annotation/DA42-POH.xml";

        File f = new File(file);
        Gate.init();

//        XmlDocumentFormat xmld = new XmlDocumentFormat();
////        xmld.
//        DocumentContentImpl dc = new DocumentContentImpl(f.toURI().toURL(), "UTF-8", null, null);
//        System.out.println(dc.size());
//        DocumentImpl d = new DocumentImpl();
//        d.setContent(dc);
        DocumentFormat.getSupportedMimeTypes().forEach(m -> System.out.println(m));
        FeatureMap fm = Factory.newFeatureMap();
//        fm.put("mimeType", "text/xmi+xml");
        fm.put("sourceUrl", f.toURI().toURL());
//        fm.put("encoding", f.toURI().toURL());
        Document d = (Document)Factory
                .createResource("gate.corpora.DocumentImpl", fm);




//        Model m = extractAnnotations(d);
        Model m = extractOntologyFromOATAnnoations(d);

        File out = changeExtension(f, "ttl");
        saveModel(m, out);
    }
}
