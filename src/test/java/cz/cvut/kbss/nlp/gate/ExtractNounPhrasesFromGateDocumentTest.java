package cz.cvut.kbss.nlp.gate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ExtractNounPhrasesFromGateDocumentTest {

    @Test
    public void filterStopwordsFiltersOutStopwords() {
        String ss = "the hydraulic";
        String s = ExtractNounPhrasesFromGateDocument.filterStopwords(ss);
        Assert.assertEquals(s, "hydraulic");
    }

    @Test
    public void filterStopwordsFiltersOutExtraSpaces() {
        String ss = "hydraulic     pump";
        String s = ExtractNounPhrasesFromGateDocument.filterStopwords(ss);
        Assert.assertEquals(s, "hydraulic pump");
    }

    @Test
    public void splitSplitsStringToArray() {
        String s = "the hydraulic";

        List<String> expectation = new ArrayList<>();
        expectation.add("the");
        expectation.add("hydraulic");
        List<String> result  = ExtractNounPhrasesFromGateDocument.split(s);
        Assert.assertEquals(result, expectation);
    }
}