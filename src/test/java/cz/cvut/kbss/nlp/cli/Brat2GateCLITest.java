package cz.cvut.kbss.nlp.cli;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class Brat2GateCLITest {

    @Ignore
    @Test
    public void test_main() throws Exception {
        String inputTestFile = "c:\\Users\\kostobog\\Documents\\skola\\projects\\2019-msmt-inter-excelence\\code\\gate-tools\\testing\\test-brat-data-folder\\DA42-POH-38.txt";
        String bratServerUrl = "https://kbss.felk.cvut.cz/brat";
//        String bratDataHomeDirectory = "/var/www/brat/data";
        String[] args = new String[]{
//            "brat2gate",
            "--input-text-file", inputTestFile,
            "--brat-server-url", bratServerUrl,
//            "--brat-data-home-directory", bratDataHomeDirectory
        };

//        UnifiedCLI.main(args);
        Brat2GateCLI.main(args);
    }
}