package cz.cvut.kbss.nlp.gate;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cvut.kbss.nlp.gate.ExtractAnnotations.ORIGINAL_MARKUP;
import static cz.cvut.kbss.nlp.gate.ExtractAnnotations.readDocument;

public class ExtractTermsFromGateDocument {
    public static final String CC = "CC"; // coordinating conjunction: ‘and’, ‘but’, ‘nor’, ‘or’, ‘yet’, plus, minus, less, times (multiplication), over (division). Also ‘for’ (because) and ‘so’ (i.e., ‘so that’).
    public static final String CD = "CD"; // cardinal number
    public static final String DT = "DT"; // determiner: Articles including ‘a’, ‘an’, ‘every’, ‘no’, ‘the’, ‘another’, ‘any’, ‘some’, ‘those’.
    public static final String EX = "EX"; // existential ‘there’: Unstressed ‘there’ that triggers inversion of the in?ected verb and the logical subject; ‘There was a party in progress’.
    public static final String FW = "FW"; // foreign word
    public static final String IN = "IN"; // preposition or subordinating conjunction
    public static final String JJ = "JJ"; // adjective: Hyphenated compounds that are used as modi?ers; happy-go-lucky.
    public static final String JJR = "JJR"; // adjective - comparative: Adjectives with the comparative ending ‘-er’ and a comparative meaning. Sometimes ‘more’ and ‘less’.
    public static final String JJS = "JJS"; // adjective - superlative: Adjectives with the superlative ending ‘-est’ (and ‘worst’). Sometimes ‘most’ and ‘least’.
    public static final String JJSS = "JJSS"; // -unknown-, but probably a variant of JJS
    public static final String _LRB_ = "-LRB-"; // -unknown-
    public static final String LS = "LS"; // list item marker: Numbers and letters used as identi?ers of items in a list.
    public static final String MD = "MD"; // modal: All verbs that don’t take an ‘-s’ ending in the third person singular present: ‘can’, ‘could’, ‘dare’, ‘may’, ‘might’, ‘must’, ‘ought’, ‘shall’, ‘should’, ‘will’, ‘would’.
    public static final String NN = "NN"; // noun - singular or mass
    public static final String NNP = "NNP"; // proper noun - singular: All words in names usually are capitalized but titles might not be.
    public static final String NNPS = "NNPS"; // proper noun - plural: All words in names usually are capitalized but titles might not be.
    public static final String NNS = "NNS"; // noun - plural
    public static final String NP = "NP"; // proper noun - singular
    public static final String NPS = "NPS"; // proper noun - plural
    public static final String PDT = "PDT"; // predeterminer: Determiner like elements preceding an article or possessive pronoun; ‘all/PDT his marbles’, ‘quite/PDT a mess’.
    public static final String POS = "POS"; // possessive ending: Nouns ending in ‘’s’ or ‘’’.
    public static final String PP = "PP"; // personal pronoun
    public static final String PRPR$ = "PRPR$"; // unknown-, but probably possessive pronoun
    public static final String PRP = "PRP"; // unknown-, but probably possessive pronoun
    public static final String PRP$ = "PRP$"; // unknown, but probably possessive pronoun,such as ‘my’, ‘your’, ‘his’, ‘his’, ‘its’, ‘one’s’, ‘our’, and ‘their’.
    public static final String RB = "RB"; // adverb: most words ending in ‘-ly’. Also ‘quite’, ‘too’, ‘very’, ‘enough’, ‘indeed’, ‘not’, ‘-n’t’, and ‘never’.
    public static final String RBR = "RBR"; // adverb - comparative: adverbs ending with ‘-er’ with a comparative meaning.
    public static final String RBS = "RBS"; // adverb - superlative
    public static final String RP = "RP"; // particle: Mostly monosyllabic words that also double as directional adverbs.
    public static final String STAART = "STAART"; // start state marker (used internally)
    public static final String SYM = "SYM"; // symbol: technical symbols or expressions that aren’t English words.
    public static final String TO = "TO"; // literal “to”
    public static final String UH = "UH"; // interjection: Such as ‘my’, ‘oh’, ‘please’, ‘uh’, ‘well’, ‘yes’.
    public static final String VBD = "VBD"; // verb - past tense: includes conditional form of the verb ‘to be’; ‘If I were/VBD rich...’.
    public static final String VBG = "VBG"; // verb - gerund or present participle
    public static final String VBN = "VBN"; // verb - past participle
    public static final String VBP = "VBP"; // verb - non-3rd person singular present
    public static final String VB = "VB"; // verb - base form: subsumes imperatives, in?nitives and subjunctives.
    public static final String VBZ = "VBZ"; // verb - 3rd person singular present
    public static final String WDT = "WDT"; // ‘wh’-determiner
    public static final String WP$ = "WP$"; // possessive ‘wh’-pronoun: includes ‘whose’
    public static final String WP = "WP"; // ‘wh’-pronoun: includes ‘what’, ‘who’, and ‘whom’.
    public static final String WRB = "WRB"; // ‘wh’-adverb: includes ‘how’, ‘where’, ‘why’. Includes ‘when’ when used in a temporal sense.

    public static final String TOKEN = "Token";
    public static final String STRING_F = "string";
    public static final String CATEGORY_F = "category";


    public static void extractTerms(Document d) {

        AnnotationSet as = d.getAnnotations("");
        List<String> cats = Arrays.asList(NN, NNP, NNPS, NNS);
        List<String> tokens = new ArrayList<>(as.size()/2);
        for (Annotation a : as) {
            if (a.getType().equals(TOKEN)) {
                String str = Objects.toString(a.getFeatures().get(STRING_F));
                String cat = Objects.toString(a.getFeatures().get(CATEGORY_F));
                if (cats.stream().anyMatch(c -> c.equals(cat))) {
                    System.out.println(String.format("%010d\t%s\t%s", a.getId(), str, cat));
                    tokens.add(str);
                }
            }
        }

        Map<String, Long> map = tokens.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        System.out.println(String.format("term frequency table, number of distinct terms os %d", map.size()));
        map.entrySet()
                .stream().sorted(Comparator.comparing(e -> e.getValue()))
                .forEach(e -> System.out.println(String.format("%s\t%d", e.getKey(), e.getValue())));
    }

    public static void main(String[] args) throws Exception{
        String root = "c:\\Users\\user\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\reliability-model\\dev-a-input-documents\\ontologies\\003-ata-aircraft-parts\\lexicon\\";
        String output = "";

        Document d = readDocument(root+ "ata-labales-and-descriptions.xml");
        extractTerms(d);

    }
}
