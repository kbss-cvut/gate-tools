package gate.creole.brat;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public class Brat2OntoConfig {


    private URL bratServerUrl;
    private Path bratHome = null;
    private Path bratTextFilePath;
    private boolean checkOntologyEntities = true;
    private String bratPrefixSeparator = "_";

    public Brat2OntoConfig() {
    }

    public Brat2OntoConfig(
            URL bratServerUrl,
            Path bratHome, Path bratTextFilePath,
            boolean checkOntologyEntities) {
        this.bratServerUrl = bratServerUrl;
        this.bratHome = bratHome;
        this.bratTextFilePath = bratTextFilePath;
        this.checkOntologyEntities = checkOntologyEntities;
    }

    public URL getBratServerUrl() {
        return bratServerUrl;
    }

    public void setBratServerUrl(URL bratServerUrl) {
        this.bratServerUrl = bratServerUrl;
    }

    public Path getBratHome() {
        return bratHome;
    }

    public void setBratHome(Path bratHome) {
        this.bratHome = bratHome;
    }

    public String getBratPrefixSeparator() {
        return bratPrefixSeparator;
    }

    public void setBratPrefixSeparator(String bratPrefixSeparator) {
        this.bratPrefixSeparator = bratPrefixSeparator;
    }

    public boolean isCheckOntologyEntities() {
        return checkOntologyEntities;
    }

    public void setCheckOntologyEntities(boolean checkOntologyEntities) {
        this.checkOntologyEntities = checkOntologyEntities;
    }

    public boolean isReferenceBratServer() {
        return (bratHome != null) && (bratServerUrl != null);
    }

    public Path getBratTextFilePath() {
        return bratTextFilePath;
    }

    public void setBratTextFilePath(Path bratTextFilePath) {
        this.bratTextFilePath = bratTextFilePath;
    }

    public String getBratDataName() {
        return removeTxtExtension(bratTextFilePath.getFileName().toString());
    }

    public String getBratRelativeDataHome() {
        return bratHome.resolve(bratTextFilePath.resolve(".")).toString();
    }

    public String getBratDataUrl() {
        return bratServerUrl + "/#/" + getBratRelativeDataHome() + getBratDataName();
    }

    public String getBratDataUrl(String focusedBratEntityId) {
        return getBratDataUrl() + "?focus=" + focusedBratEntityId;
    }

    private String removeTxtExtension(String txtFilePath) {
        return txtFilePath.replace("\\.txt$", "");
    }
}
