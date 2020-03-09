package cz.cvut.kbss.nlp.gate;

import gate.*;
import gate.annotation.AnnotationFactory;
import gate.annotation.AnnotationSetImpl;
import gate.annotation.DefaultAnnotationFactory;
import gate.annotation.NodeImpl;
import gate.util.SimpleFeatureMapImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.*;

public class ExtractAnnotations {


    protected static Resource getType(String annotationType){
        // normalize annotation type string
        switch (annotationType){
            case "function": return Reliability.c_function;
            case "component": return Reliability.c_component;
            case "failure-mode": return Reliability.c_failureMode;
            default: return null;
        }
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

    public static Model extractOATAnnoationsAsClasses(Document d) throws Exception {
        String  mentionType = "Mention";
        AnnotationSet as = d.getAnnotations("Original markups");
        DocumentContent dc = d.getContent();
        System.out.println(d.getNamedAnnotationSets().size());

        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("rel", Reliability.ns);
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

                String newType = URIUtils.constructUri(annotatedText , ontologyClassLocalName, DA42_POH.NS);
                Resource r = m.createResource(newType, RDFS.Class);
                r.addProperty(RDFS.subClassOf, type)
                        .addLiteral(RDFS.label, annotatedText);
            }
        }
        return m;
    }

    public static Model extractOATAnnoationsAsInstances(Document d) throws Exception {
        String  mentionType = "Mention";
        AnnotationSet as = d.getAnnotations("Original markups");
        DocumentContent dc = d.getContent();
        System.out.println(d.getNamedAnnotationSets().size());

        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("rel", Reliability.ns);
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

                String newInstance = URIUtils.constructUri(annotatedText, ontologyClassLocalName, DA42_POH.NS);
                Resource r = m.createResource(newInstance, type)
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
        m.setNsPrefix("rel", Reliability.ns);
        m.setNsPrefix("rdf", RDF.uri);
        m.setNsPrefix("rdfs", RDFS.uri);

        for(Annotation a : as){
            String annotationType = a.getType();
            String annotatedText = dc.getContent(a.getStartNode().getOffset() , a.getEndNode().getOffset()).toString().trim();
            System.out.println(a.getType() + " : " + annotatedText);
            String normalizedAnnotationType = URIUtils.normalizedAnnotationType(annotationType);
            Resource type = getType(normalizedAnnotationType);
            String newType = URIUtils.constructUri(annotatedText , normalizedAnnotationType, DA42_POH.NS);
            m.createResource(newType, RDFS.Class)
                    .addProperty(RDFS.subClassOf, type)
                    .addLiteral(RDFS.label, annotatedText);
        }
        return m;
    }

    public static void createXMLGateDocumentFromScratchAndSaveIt() throws Exception{
        String out = "gate-outpu.xml";
        File f = new File(out);
//        FeatureMap fm = Factory.newFeatureMap();
//        fm.put("mimeType", "application/xml");
//        fm.put("sourceUrl", f.toURI().toURL());
////        fm.put("encoding", f.toURI().toURL());
//        Document d = (Document)Factory
//                .createResource("gate.corpora.DocumentImpl", fm);
        String text = "This is an example text which will be annotated programmatically with the gate api.";
        Document d = Factory.newDocument(text);

        // annotate document
        String annotationSetName = "Original markups";
        String annotationType = "Mention";
        AnnotationSet as = new AnnotationSetImpl(d, annotationSetName);
        AnnotationFactory af = new DefaultAnnotationFactory();

        NodeImpl n1 = new NodeImpl(0, 11L );
        NodeImpl n2 = new NodeImpl(1, 18L);
        FeatureMap map = new SimpleFeatureMapImpl();
         map.put("class", "http://onto.fe.cvut.cz/ontologies/riliability/function");
         map.put("ontology", "http://onto.fe.cvut.cz/ontologies/riliability");
//        d.
        Annotation annotation = af.createAnnotationInSet(as, 0, n1, n2, annotationType, map);

        d.getNamedAnnotationSets().put(as.getName(), as);

        String output = d.toXml();
        try(PrintStream ps = new PrintStream(new FileOutputStream(f))){
            ps.println(output);
            ps.flush();
        }
    }

    public static void main(String[] args) throws Exception {
//        String file = "c:/Users/user/Documents/skola/projects/2019-msmt-inter-excelence/code/semantic-reliability/reliability-model/dev-b-input-analysis/001-data_blue-sky_manua-text-annotation/DA42-POH.xml";
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
//        Model m = extractOATAnnoationsAsClasses(d);
        Model m = extractOATAnnoationsAsInstances(d);
        File out = changeExtension(f, "ttl");
        saveModel(m, out);



//        createXMLGateDocumentFromScratchAndSaveIt();
    }
}
