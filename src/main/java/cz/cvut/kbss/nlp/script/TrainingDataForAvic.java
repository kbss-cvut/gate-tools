package cz.cvut.kbss.nlp.script;

import cz.cvut.kbss.nlp.gate.ExtractAnnotations;
import cz.cvut.kbss.nlp.gate.io.AsConll;
import gate.Document;
import gate.util.GateException;

import java.io.*;

public class TrainingDataForAvic {

    protected AsConll proc = new AsConll();
    protected String filePrefix = "DA42-POH";

    protected OutputStream outputStream;

    public void process(String root, String output){
        String[] dirs = {"DA42-POH--has-component", "DA42-POH--has-failure-mode", "DA42-POH--has-function"};
        try(OutputStream os = new FileOutputStream(output)) {
            outputStream = os;
            PrintStream ps = new PrintStream(os);
            ps.println("Sentence #,Word,POS,Tag,");
            for (String dir : dirs) {
                processDir(new File(root, dir));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void processDir(File dir){
        for(File f : dir.listFiles(this::isInputFile)){
            processFile(f);
        }
    }

    protected void processFile(File file){
        processFile(file, outputStream);
    }

    public boolean isInputFile(File f){
        String p = f.getParentFile().getName();
        String n = f.getName();
        return n.startsWith(filePrefix) && n.endsWith(".xml");
    }

    public void processFile(File file, OutputStream os){
        int sentenceOffset = 0;
        try {
            Document d = ExtractAnnotations.readDocument(file.getAbsolutePath());
//            proc.listOverlappingAnnotations(d, file.getParentFile().getName() + "-" + file.getName(), System.out);
            sentenceOffset = proc.asConllTable(d, os, sentenceOffset);
        } catch (GateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    public static void main(String[] args) {
        String inputRoot = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\annotations\\BlueSky\\";
        String output = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\semantic-reliability\\annotations\\BlueSky\\DA42-POH-conll-train-debug.csv";
        new TrainingDataForAvic().process(inputRoot, output);
    }
}
