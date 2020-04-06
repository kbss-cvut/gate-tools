package cz.cvut.kbss.nlp.gate;

import gate.*;
import gate.annotation.AnnotationFactory;
import gate.annotation.AnnotationSetImpl;
import gate.annotation.DefaultAnnotationFactory;
import gate.annotation.NodeImpl;
import gate.corpora.DocumentImpl;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import gate.util.SimpleFeatureMapImpl;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtractAnnotations {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractAnnotations.class);

    public static final String MENTION = "Mention";
    public static final String ORIGINAL_MARKUP = "Original markups";

    public static final String SOURCE_URL = "sourceUrl";
    
    public static final String FEATURE_CLASS = "class";
    public static final String FEATURE_ONTOLOGY = "ontology";
    public static final String FEATURE_INSTANCE = "inst";
    
    static{
        try {
            Gate.init();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    protected static Resource getType(String annotationType){
        // normalize annotation type string
        switch (annotationType){
            case "function": return Reliability.c_function;
            case "component": return Reliability.c_component;
            case "failure-mode": return Reliability.c_failureMode;
            default: return null;
        }
    }

    public static Document readDocument(String filePath) throws GateException {
        File f = new File(filePath);
        DocumentFormat.getSupportedMimeTypes().forEach(m -> System.out.println(m));
        FeatureMap fm = Factory.newFeatureMap();

        fm.put(SOURCE_URL, f.toURI().toString());
        Document d = (Document)Factory
                .createResource(DocumentImpl.class.getCanonicalName(), fm);
        return d;
    }

    public static void writeDocument(Document d, String out) throws FileNotFoundException {
        File f = new File(out);
        String output = d.toXml();
        try(PrintStream ps = new PrintStream(new FileOutputStream(f))){
            ps.println(output);
            ps.flush();
        }
    }

    public static String getAnnotatedText(Document d, Annotation a){
        try {
            return d.getContent().getContent(a.getStartNode().getOffset() , a.getEndNode().getOffset()).toString();
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<Integer, Integer> findTextIn(String text, String textContext, Document doc){
        return findTextIn(text, textContext, doc, 0L, doc.getContent().size());
    }

    public static String getContent(Document d, Long from, Long to){
        try {
            return d.getContent().getContent(from, to).toString();
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<Integer, Integer> findTextIn(String text, String textContext, Document doc, Long from, Long to ){
        if(from == null)
            from = 0L;
        if(to == null){
            to = doc.getContent().size();
        }
        Map<Integer, Integer> locations = new HashMap<>();
        String c = getContent(doc, from, to);

        // normalize spaces
        String cc = c;
        c = c.replaceAll("\\s+", "");
        Map<Integer,Integer> skippedMap = new HashMap<>();
        int spaceCount = 0;
        int lastSpaceCount = 0;
        for(int i = 0, j = 0; i < cc.length(); i++){
            char c1 = cc.charAt(i);
            if(("" + c1).matches("\\s")){
                spaceCount ++;
            }else {
                char c2 = c.charAt(j);

                // spaceCount == i - j // this should hold!
                if(c1 != c2){
                    LOG.info("chars do not match {} != {} at indices {} and {}, in texts \"\"\"\n{}\"\"\",\"\"\"\n{}\"\"\"", c1, c2, i, j, cc, c);
                }
                if(lastSpaceCount < spaceCount){
                    skippedMap.put(j, spaceCount);
                    lastSpaceCount = spaceCount;
                }
                j ++; // count only non space chars
            }
        }
        int[] keys = skippedMap.keySet().stream().mapToInt(i -> i).sorted().toArray();
        String t = text.replaceAll("\\s+", "");
        String tc= textContext != null ? textContext.replaceAll("\\s+", "") : null;

        long i = 0;
        long tci = 0;
        while(i < c.length()) {
            int s = c.indexOf(t, (int)i);
            if(s < 0)
                break;
            if(tc != null) {
                if (tci + 1 < s && tci >= 0)
                    tci = c.indexOf(tc, (int) i);
                if(!locations.isEmpty() && tci < 0)
                    break; // already found the last one
                if (s < tci || tci < 0) {
                    i += s + 1;
                    continue;// the context matched
                }
            }
            int a = Arrays.binarySearch(keys, s);
            a = a < 0 ? -a -2 : a;
            Integer sWithSpaces = s + (a > 0 ? skippedMap.get(keys[a]) : 0);

//            Integer newLen =
            if(sWithSpaces == null){
                LOG.info("could not find fix with spaces !!!");
                i = s + 1;// try to find the string starting from the next index.
                continue;
            }
            int lenWithSpaces = findTextLengthWithSpacesIn(t, sWithSpaces, cc);
            locations.put(from.intValue() + sWithSpaces, lenWithSpaces);
            // DEBUG ////////////////////////
            String annotatedText = getContent(doc, from + sWithSpaces, from + sWithSpaces + lenWithSpaces);
            if(!annotatedText.replaceAll("\\s+", "").equals(t)  ){
                LOG.info("{} != {}", annotatedText, text);
            }

            // DEBUG ////////////////////////
            i += s + text.length();
        }

        return locations;
    }

    public static int findTextLengthWithSpacesIn(String txtNoSpace, int start, String textToSearch){
        for(int i = start; i < textToSearch.length(); i++){
            int j = i;

            char c1 = textToSearch.charAt(j);
            if(("" + c1).matches("\\s")) // skip leading white spaces
                continue;

            for(int k = 0; j < textToSearch.length(); j ++){
                c1 = textToSearch.charAt(j);
                if(!("" + c1).matches("\\s")){
                    char c2 = txtNoSpace.charAt(k);
                    if(c1 != c2){
                        j = i; // comparison failed
                        break;
                    }
                    k++;
                    if(k >= txtNoSpace.length()){
                        return (j + 1) - i;//  + 1 to account for the character at index j.
                    }
                }
            }
        }
        return txtNoSpace.length();
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

    public static Model extractOATAnnotationsAsClasses(String file) throws Exception {
        Document d = readDocument(file);
        String  mentionType = MENTION;
        AnnotationSet as = d.getAnnotations(ORIGINAL_MARKUP);
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
            Object objType = a.getFeatures().get(FEATURE_CLASS);
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

    /**
     * Create an ontology with instances based on annotations with the class feature classes if the annotation does not
     * have an instance feature. The created instances are of type class and have label the annotated text. Additionally,
     * the created instance is added as an instance feature to the annotation from which it was created.
     *
     * it is added.
     * @param file
     * @return
     * @throws Exception
     */
    public static Model extractOATAnnotationsAsInstances(String file, String documentOutputFile) throws Exception {
        Document d = readDocument(file);
        String  mentionType = MENTION;
        AnnotationSet as = d.getAnnotations(ORIGINAL_MARKUP);
        DocumentContent dc = d.getContent();
        System.out.println(d.getNamedAnnotationSets().size());

        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("rel", Reliability.ns);
        m.setNsPrefix("rdf", RDF.uri);
        m.setNsPrefix("rdfs", RDFS.uri);
        m.setNsPrefix("", DA42_POH.FMEA);

        int instance_index = 1;
        for(Annotation a : as){
            String annotationType = a.getType();
            if(!mentionType.equals(annotationType) || a.getFeatures().get(FEATURE_INSTANCE) != null)
                continue;
            String annotatedText = getAnnotatedText(d, a);
            Object objType = a.getFeatures().get(FEATURE_CLASS);
            if(objType != null) {
                System.out.println(a.getType() + " : " + annotatedText);
                String ontologyClass = objType.toString();
                Resource type = m.createResource(ontologyClass);
                String ontologyClassLocalName = type.getLocalName();

                String newInstance = URIUtils.constructUri(String.format("%05X",instance_index++), ontologyClassLocalName, DA42_POH.FMEA);
                Resource r = m.createResource(newInstance, type)
                        .addLiteral(RDFS.label, annotatedText);
                a.getFeatures().put(FEATURE_INSTANCE, newInstance);
            }
        }
        writeDocument(d, documentOutputFile);
        return m;
    }

    public static void insertInstancesInGATEXml(String instanceOntology, String document ) throws GateException, FileNotFoundException {
        Document d = readDocument(document);
        Model m = RDFDataMgr.loadModel(new File(instanceOntology).toURI().toString());
        AnnotationSet as = d.getAnnotations(ORIGINAL_MARKUP).get(MENTION);
        FeatureMap fm = new SimpleFeatureMapImpl();
        
        Map<String, List<Annotation>> annotationMap = as.stream()
                .filter(a -> a.getFeatures().get(FEATURE_CLASS) != null)
                .collect(Collectors.groupingBy(a -> a.getFeatures().get(FEATURE_CLASS) + ""));
        
        ResIterator iter = m.listSubjectsWithProperty(RDF.type, OWL2.Class);
        while(iter.hasNext()){
            Resource res = iter.nextResource();
            ResIterator instances = m.listSubjectsWithProperty(RDF.type, res);
            Map<String, List<Annotation>> annotationsByText = Optional.ofNullable(annotationMap.get(res.getURI())).map(s -> s.stream()).orElse(Stream.of())
                    .collect(Collectors.groupingBy(a -> getAnnotatedText(d, a).trim()));

            while(instances.hasNext()){
                Resource inst = instances.nextResource();
                String label = Optional.of(inst.getProperty(RDFS.label)).map(s -> s.getObject().toString()).orElse(null);
                if(label == null)
                    continue;

                List<Annotation> anns = Optional.ofNullable(annotationsByText.get(label))
                        .orElse(Collections.EMPTY_LIST);
                Annotation ann = anns.stream().filter(a -> a.getFeatures().get(FEATURE_INSTANCE) == null)
                        .findFirst().orElse(null);

                if(ann != null){
                    ann.getFeatures().put(FEATURE_INSTANCE, inst.getURI());
                }

//                if(ann.size() > 1){
//                    LOG.warn("Found multiple annotation for instance \"{}\", <{}> :\n{}", label, inst.getURI(),
//                            ann.stream().map(a -> a.getId() + "").collect(Collectors.joining("\n")));
//
//                }
                if(ann == null){
                    LOG.warn("Annotation not found for instance \"{}\", <{}> ", label, inst.getURI());
                }
            }
        }
        writeDocument(d, document);
    }

    public static Model extractAnnotations(String file) throws Exception{
        Document d = readDocument(file);
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
        String annotationSetName = ORIGINAL_MARKUP;
        String annotationType = MENTION;
        AnnotationSet as = new AnnotationSetImpl(d, annotationSetName);
        AnnotationFactory af = new DefaultAnnotationFactory();

        NodeImpl n1 = new NodeImpl(0, 11L );
        NodeImpl n2 = new NodeImpl(1, 18L);
        FeatureMap map = new SimpleFeatureMapImpl();
         map.put(FEATURE_CLASS, "http://onto.fe.cvut.cz/ontologies/riliability/function");
         map.put(FEATURE_ONTOLOGY, "http://onto.fe.cvut.cz/ontologies/riliability");
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
        String outputFile = "c:/Users/user/Documents/skola/proje cts/2019-msmt-inter-excelence/code/semantic-reliability/reliability-model/dev-b-input-analysis/001-data_blue-sky_manua-text-annotation/DA42-POH-004.xml";
//        String onto = "c:/Users/user/Documents/skola/projects/2019-msmt-inter-excelence/code/semantic-reliability/reliability-model/dev-b-input-analysis/001-data_blue-sky_manua-text-annotation/DA42-POH.ttl";
        File f = new File(file);


//        Model m = extractAnnotations(file);
//        Model m = extractOATAnnotationsAsClasses(file);
        Model m = extractOATAnnotationsAsInstances(file, outputFile);
        File out = changeExtension(f, "ttl");
        saveModel(m, out);

//        insertInstancesInGATEXml(onto, file);

//        createXMLGateDocumentFromScratchAndSaveIt();
    }
}
