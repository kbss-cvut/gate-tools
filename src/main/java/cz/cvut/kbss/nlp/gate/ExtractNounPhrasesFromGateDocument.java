package cz.cvut.kbss.nlp.gate;

import gate.*;
import gate.util.InvalidOffsetException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cz.cvut.kbss.nlp.gate.ExtractAnnotations.readDocument;

public class ExtractNounPhrasesFromGateDocument {

    private static List<String> stopwordsList;
    public static String filename = "DA42-POH-40-NPchunker.xml";

    static {
        try {
            stopwordsList = Files.readAllLines(new File(ExtractNounPhrasesFromGateDocument.class.getClassLoader().getResource("stopwords-English.txt").getFile()).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String NOUN_CHUNK = "NounChunk";

    public static void main(String[] args) throws Exception{
        Document d = readDocument(ExtractNounPhrasesFromGateDocument.class.getClassLoader().getResource(filename).getFile());
        extractNounPhrases(d);

    }

    private static void extractNounPhrases(Document d) throws InvalidOffsetException, IOException {
        AnnotationSet as = d.getAnnotations("");
        DocumentContent documentContent = d.getContent();
        List<String> np = new ArrayList<>();
        for (Annotation a : as) {
            if (a.getType().equals(NOUN_CHUNK)) {
                String text = documentContent.getContent(a.getStartNode().getOffset(), a.getEndNode().getOffset()).toString().trim();
                text = filterStopwords(text);
                if (!text.isEmpty() && (text.length() > 1) && text.matches("^[- A-Za-z]+$")) {
                np.add(filterStopwords(text).trim());
                }
            }
        }
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filename.replaceFirst(".xml", "-output.txt")));
        np.stream().distinct().forEach(s -> {
            try {
                fileWriter.write(s);
                fileWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fileWriter.close();

    }

    static String filterStopwords(String s) {
        List<String> list = ExtractNounPhrasesFromGateDocument.split(s.trim().toLowerCase().replaceAll("\n", "").replaceAll("\\s+"," "));
        return list.stream().filter(s1 -> !stopwordsList.contains(s1)).collect(Collectors.joining(" "));
    }

    public static List<String> split(String str){
        return Stream.of(str.split(" "))
                .map (String::new)
                .collect(Collectors.toList());
    }

    public static boolean isStopword(String s){
        return stopwordsList.contains(s.toLowerCase().trim());
    }
}
