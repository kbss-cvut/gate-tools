package cz.cvut.kbss.nlp.gate.io;

import cz.cvut.kbss.nlp.gate.*;
import gate.*;
import gate.creole.ANNIEConstants;
import gate.util.GateException;
import org.apache.commons.compress.utils.Iterators;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsConll implements Closeable {

    public static char[] sentenceDelimiters = {'.', '?', '!'};
    public static String sentenceDelim = "[.?!]";

    public static final String O = "O";
    public static final String B = "B-";
    public static final String I = "I-";


    protected Comparator<Annotation> comparator = Comparator.comparing(a -> a.getStartNode().getOffset());
    protected Comparator<Annotation> overlappingComparator = Comparator
            .comparing((Annotation a) -> a.getStartNode().getOffset())
            .thenComparing(Comparator.comparing((Annotation a) -> a.getEndNode().getOffset()).reversed());
    protected List<String> sentences;
    protected Annie annotate;

    {
        try {
            annotate = new Annie();
        } catch (GateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listOverlappingAnnotations(Document d, String prefix, OutputStream os){
        PrintStream ps = new PrintStream(os);
        List<Annotation> mentions = new ArrayList<>(d.getAnnotations().get(ExtractAnnotations.MENTION));

        mentions.sort(overlappingComparator);
//        ps.println("text, start, end, type");
        for(Annotation a: mentions){
            String text = ExtractAnnotations.getAnnotatedText(d, a);
            String cls = Optional
                    .ofNullable(ExtractAnnotations.getFeature(a, ExtractAnnotations.FEATURE_CLASS))
                    .map(s -> ResourceFactory.createResource(s).getLocalName()).orElse(null);
            long s = a.getStartNode().getOffset();
            long e = a.getEndNode().getOffset();

            ps.println(String.format("%s\t\"%s\"\t%d\t%d\t%s", prefix, text, s, e, cls));
        }
    }

    public void listOverlappingAnnotations(AnnotationSet as){
        List<Annotation> annots = new ArrayList<>(as);
        annots.sort(overlappingComparator);

//        Map<Annotation, List<Annotation>> parentMap = new HashMap<>();
//        for(int i = 0; i < annots.size() - 1; i++){
//            Annotation a1 = annots.get(0);
//            for(int j = i + 1 ; j < annots.size(); j++) {
//                Annotation a2 = annots.get(j);
//                if(isIn(a1, a2)){
//
//                }
//            }
//        }
    }

    protected void fixParents(Annotation parent, Annotation a2, Map<Annotation, List<Annotation>> pm){
        List<Annotation> parents = pm.get(a2);
        if(parents != null){
            for(Annotation p: parents){
//                if(isIn())
            }
        }else{
            parents = new ArrayList<>();
            parents.add(parent);
            pm.put(a2, parents);
        }

    }


    /**
     *
     * @param d
     * @param os
     * @throws Exception
     * @deprecated use {@link #asConllTable(Document, OutputStream, int)} instead.
     */
    @Deprecated
    public void asConll(Document d, OutputStream os) throws Exception {
        PrintStream ps = new PrintStream(os);
        d = annotate.annotate(d);
        String  mentionType = ExtractAnnotations.MENTION;
//        AnnotationSet as = d.getAnnotations(ExtractAnnotations.ORIGINAL_MARKUP);
//        Iterator<Annotation> ai = as.iterator();
//        DocumentContent dc = d.getContent();
//        System.out.println(d.getNamedAnnotationSets().size());
//        String text = d.getContent().toString();
//        int instance_index = 1;
        // write sentences

        List<Annotation> mentions = d.getAnnotations()
                .get(mentionType).stream()
                .filter(a -> a != null)
                .collect(Collectors.toList());

        mentions.sort(overlappingComparator);
        mentions = filterNestedMentions(mentions);
        int mentionIndex = 0;
        // filter nested mentions

//        Iterator<Annotation> miter = mentions.iterator();
        List<Annotation> tokens = new ArrayList<>(d.getAnnotations().get(ANNIEConstants.TOKEN_ANNOTATION_TYPE));
        tokens.sort(comparator);
        List<Annotation> sentences = new ArrayList<>(d.getAnnotations().get(ANNIEConstants.SENTENCE_ANNOTATION_TYPE));
        sentences.sort(comparator);
        int sentenceIndex = 0;

        Annotation mention = (mentionIndex < mentions.size()) ? mentions.get(mentionIndex++) : null;
        boolean inMention = false;
        String mt = null;
        StringBuilder output = new StringBuilder();

        for(Annotation a : tokens){
            output.append(ExtractAnnotations.getContent(d, a));
            output.append(" ");
            if(mention == null){
                output.append(O);
                output.append('\n');
                continue;
            }

            if(isIn(mention, a.getStartNode())){
                if(!inMention){// B
                    //inMention
                    String ontologyClass = ExtractAnnotations.getFeature(mention, ExtractAnnotations.FEATURE_CLASS);
                    if(ontologyClass != null) {
                        Resource type = ResourceFactory.createResource(ontologyClass);
                        mt = type.getLocalName();
                    }
                    output.append(B);
                    inMention = true;
                }else{
                    output.append(I);
                }
                output.append(mt);
            }else{
                output.append(O);
                if(inMention) {
                    inMention = false;
                    mention = (mentionIndex < mentions.size() ) ? mentions.get(mentionIndex++) : null;
                }else{

                }
            }
            output.append('\n');
        }

        ps.print(output.toString());

//        for(long i = 0; i < text.length(); i ++ ){
//            String annotationType = a.getType();
//            if(!mentionType.equals(annotationType) || a.getFeatures().get(ExtractAnnotations.FEATURE_INSTANCE) != null)
//                continue;
//            String annotatedText = ExtractAnnotations.getAnnotatedText(d, a);
//            Object objType = a.getFeatures().get(ExtractAnnotations.FEATURE_CLASS);
//            if(objType != null) {
//                System.out.println(a.getType() + " : " + annotatedText);
//                String ontologyClass = objType.toString();
//                Resource type = ResourceFactory.createResource(ontologyClass);
//                String ontologyClassLocalName = type.getLocalName();
//            }
//        }
    }

    /**
     *
     * @param d
     * @param os
     * @return number of sentences
     * @throws GateException
     */
    public int asConllTable(Document d, OutputStream os, int sentenceOffset) throws GateException {
        PrintStream ps = new PrintStream(os);
        ConllConvereterInput in = new ConllConvereterInput(d, ExtractAnnotations.MENTION);

        Iterator<ConllToken> rawTable = new ConllTableRowStream(in).iterator();

        int sentenceNumber = -1;
        StringBuilder output = new StringBuilder();
        // table constants
        char colSep = ',';
        char rowSep = '\n';


        while(rawTable.hasNext()){
            ConllToken token = rawTable.next();
            // sentence
            if(token.sentenceNumber > sentenceNumber){
                sentenceNumber = token.sentenceNumber;
                output.append("Sentence: " + (sentenceOffset + sentenceNumber));
            }
            output.append(colSep);
            // token text
            if(token.token.contains(",")) {
                output.append('"' + token.token + '"');
                output.append(colSep);
                output.append('"' + token.tags.get(0) + '"');
            } else {
                output.append(token.token);
                output.append(colSep);
                output.append(token.tags.get(0));
            }
            output.append(colSep);
            output.append(token.tags.get(1));
            output.append(rowSep);
        }
        ps.print(output.toString());
        ps.flush();
        return sentenceOffset + sentenceNumber;
    }

    protected Iterator<ConllToken> asConllTable(Document d, List<Annotation> sentences, List<Annotation> tokens, List<Annotation> mentions) {
        return new ConllTableRowStream(d, sentences, tokens, mentions).iterator();
    }

    protected class ConllConvereterInput {
        protected Document d;
        protected List<Annotation> mentions;
        protected List<Annotation> sentences;
        protected List<Annotation> tokens;

        public ConllConvereterInput(Document d, String mentionFeature) throws GateException {
            this.d = annotate.annotate(d);
            mentions = d.getAnnotations()
                    .get(mentionFeature).stream()
                    .filter(a -> a != null)
                    .collect(Collectors.toList());

            mentions.sort(overlappingComparator);
            mentions = filterNestedMentions(mentions); // filter nested mentions
            tokens = new ArrayList<>(d.getAnnotations().get(ANNIEConstants.TOKEN_ANNOTATION_TYPE));
            tokens.sort(comparator);
            sentences = new ArrayList<>(d.getAnnotations().get(ANNIEConstants.SENTENCE_ANNOTATION_TYPE));
            sentences.sort(comparator);
        }
    }

    protected class ConllTableRowStream{

        protected Document d;
        protected Iterator<Annotation> mentions;
        protected ListIterator<Annotation> sentences;
        protected List<Annotation> tokens;
//        private Function<Iterator<Annotation>, Annotation> next = i -> i.hasNext() ? i.next() : null;

        // state fields

        private Annotation mention;     // current mention, annotation that may span several tokens.
        String mt = null;               // the type/class of the last mention, initilized when the mention is
        private Annotation sentence;    // current sentence, sentence annotation cover whole sentences.
        boolean inMention = false;      // whether the last token was in mention

        public ConllTableRowStream(ConllConvereterInput input) {
            this.d = input.d;
            this.mentions = input.mentions.iterator();
            this.sentences = input.sentences.listIterator();
            this.tokens = input.tokens;
        }
        public ConllTableRowStream(Document d, List<Annotation> sentencesList, List<Annotation> tokens, List<Annotation> mentionsList) {
            this.d = d;
            mentions = mentionsList.iterator();
            sentences = sentencesList.listIterator();
            this.tokens = tokens;
        }

        public Iterator<ConllToken> iterator() {
            nextMention();
            nextSentence();

            return mapEach(tokens.iterator(), this::constructRow);
        }

        protected ConllToken constructRow(Annotation token){
            List<String> tags = new ArrayList<String>(4);
            if (!isIn(sentence, token))
                nextSentence();

            tags.add(ExtractAnnotations.getFeature(token, ANNIEConstants.TOKEN_CATEGORY_FEATURE_NAME));  // POS TAG
            tags.add(entityClassTag(token));                                                             // entity class TAG

            return new ConllToken(
                    sentences.nextIndex(),                                                              // sentence number
                    ExtractAnnotations.getContent(d, token),                                            // token
                    tags);
        }

        protected String entityClassTag(Annotation token){
            // case 1: there are no mentions
            if (mention == null) { // no mentions
                return O;
            }
            // case 2: there are mentions
            StringBuilder tag = new StringBuilder();
            if (isIn(mention, token.getStartNode())) {
                if (!inMention) {// B
                    tag.append(B);
                    inMention = true;
                } else {
                    tag.append(I);
                }
                tag.append(mt);
            } else { //
                tag.append(O);
                if (inMention) {
                    inMention = false;
                    nextMention();
                } else {

                }
            }
            return tag.toString();
        }

        protected Annotation next(Iterator<Annotation> i){
            return i.hasNext() ? i.next() : null;
        }

        protected void nextSentence(){
            sentence = next(sentences);
        }

        protected void nextMention(){
            mention = next(mentions);
            mt = Optional.ofNullable(mention)
                    .map(m -> ExtractAnnotations.getFeature(mention, ExtractAnnotations.FEATURE_CLASS))
                    .map(ResourceFactory::createResource)
                    .map(Resource::getLocalName)
                    .orElse(null);
        }
    }

    protected class ConllToken{
        public int sentenceNumber;
        public String token;
        public List<String> tags;

        public ConllToken(int sentenceNumber, String token, List<String> tags) {
            this.sentenceNumber = sentenceNumber;
            this.token = token;
            this.tags = tags;
        }
    }




    public List<Annotation> filterNestedMentions(List<Annotation> in){
        List<Annotation> out = new ArrayList<>();
        int nextNotNestedI = 0;
        for(int i = 0; i < in.size() - 1; i = nextNotNestedI){
            Annotation a1 = in.get(i);
            out.add(a1);
            nextNotNestedI = i + 1;
            for(int j = i + 1; j < in.size(); j ++) {
                Annotation a2 = in.get(j);
                nextNotNestedI = j; // skip to j in the outer cycle,
                if(!isIn(a1,a2)) {
                    break;
                }
            }
        }
        return out;
    }


    public boolean overlap(Annotation a1, Annotation a2){
        return isIn(a1, a2.getStartNode()) || isIn(a2, a1.getStartNode());
    }

    public boolean isIn(Annotation a1, Annotation a2){
        return isIn(a1, a2.getStartNode()) && isInClosed(a1, a2.getEndNode());
    }

    public boolean isIn(Annotation a, Node n){
        return a.getStartNode().getOffset() <= n.getOffset() && n.getOffset() < a.getEndNode().getOffset();
    }

    public boolean isInClosed(Annotation a, Node n){
        return a.getStartNode().getOffset() <= n.getOffset() && n.getOffset() <= a.getEndNode().getOffset();
    }

    @Override
    public void close() throws IOException {
        annotate.close();
    }



    //
////    public List<Token> tokenize(String text){
////        String token = null;
////        boolean extracting token
////        for(long i = 0; i < text.length(); i ++ ){
////            text.charAt(i)
////            if(text.charAt(i))
////        }
////    }
//
//
//    public void tokenize(){
//
//    }
//
//    public Token nextToken(String text, int from){
//        int start = -1;
//        int i;
//        for(i = from; i < text.length(); i ++ ){
//            char c = text.charAt(i);
//            boolean isSpace = Character.isSpaceChar(c);
//            if(start < 0 && isSpace) {
//                continue;
//            }
//            if(start == -1)
//                start = i;
//            else if(start > -1 && isSpace)
//                break; // found a token
//        }
//        if(start > -1) // if we found a toke, handle two cases 1) token inside the text and 2) token at the end of the text.
//            return new Token(start, i, text.substring(start, i));
//
//        return null;
////        return new Token()
//    }
//
//    public void sentences(String text){
//
//    }
//    public static String getSentence(Long offset){
//
//        return null;
//    }
//
//
//    public static class Token{
//        int start;
//        int end;
//        String txt;
//
//        public Token(int start, int end, String txt) {
//            this.start = start;
//            this.end = end;
//            this.txt = txt;
//        }
//    }

//
//    public static class Container{
//        public Container parent;
//        public Annotation container;
//        public List<Container> contained;
//
//        public Container(Annotation container, List<Container> contained) {
//            this.container = container;
//            this.contained = contained;
//        }
//
//        public Container(Annotation container) {
//            this(container, new ArrayList<>());
//        }
//
//    }
//

    public static <I,O> Iterator<O> mapEach(final Iterator<I> in, Function<I,O> mapping){
        return new Iterator<O>() {
            @Override
            public boolean hasNext() {
                return in.hasNext();
            }

            @Override
            public O next() {
                return mapping.apply(in.next());
            }
        };
    }



    public static void main(String[] args) throws Exception {


//        List a = Arrays.asList(2,3,1);
//        a.sort(Comparator.comparing((Integer i) -> i));
//        Stack<Integer> s = new Stack<>();
//        s.addAll(a);
//        while(!s.empty())
//            System.out.println(s.pop());

//        String file = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\annotations\\BlueSky\\DA42-POH--has-component\\DA42-POH-38.xml";
//
//        AsConll c = new AsConll();
//        Document d = ExtractAnnotations.readDocument(file);
////        c.asConll(d, System.out);
//        c.listOverlappingAnnotations(d, "",System.out);
    }
}
