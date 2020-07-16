package gate.creole.brat;

import gate.*;
import gate.corpora.MimeType;
import gate.corpora.TextualDocumentFormat;
import gate.creole.ResourceInstantiationException;
import gate.creole.brat.annotations.BratAnnotation;
import gate.creole.brat.annotations.Note;
import gate.creole.brat.annotations.TextBound;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;
import gate.relations.RelationSet;
import gate.util.DocumentFormatException;
import gate.util.FeatureBearer;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@CreoleResource(name = "brat Document Format", isPrivate = true, autoinstances = {@AutoInstance(hidden = true)})
public class OntoDocumentFormat extends TextualDocumentFormat {

    private static final long serialVersionUID = -1710274568830645130L;
    private OntologyHelper ontologyHelper;

    public OntoDocumentFormat(OntologyHelper ontologyHelper) {
        this.ontologyHelper = ontologyHelper;
    }

    public static void merge(Document doc, String annotationSetName, Annotations brat, OntologyHelper ontologyHelper) throws DocumentFormatException {
        merge(doc.getAnnotations(annotationSetName), brat, ontologyHelper);
    }

    public static void merge(AnnotationSet annots, Annotations brat, OntologyHelper ontologyHelper) throws DocumentFormatException {

        try {
            Map<String, Integer> brat2GATE = new HashMap<String, Integer>();

            RelationSet relations = annots.getRelations();

            List<BratAnnotation> unprocessed = brat.get();

            while (unprocessed.size() > 0) {
                int left = unprocessed.size();

                Iterator<BratAnnotation> it = unprocessed.iterator();
                while (it.hasNext()) {
                    BratAnnotation annotation = it.next();

                    if (process(annotation, brat2GATE, annots, relations, ontologyHelper)) it.remove();
                }

                if (left == unprocessed.size())
                    throw new DocumentFormatException("invalid file");
            }
        } catch (Exception ioe) {
            throw new DocumentFormatException(ioe);
        }
    }

    private static boolean process(BratAnnotation annotation,
                                   Map<String, Integer> brat2GATE,
                                   AnnotationSet original, RelationSet relations,
                                   OntologyHelper ontologyHelper)
            throws Exception {
        switch (annotation.getID().charAt(0)) {
            case 'T':
                // return new TextBound(data);
                TextBound tb = (TextBound) annotation;

                FeatureMap features = Factory.newFeatureMap();
                features.put("inst", ontologyHelper.createIndividual(tb).toString());
                features.put("class", ontologyHelper.getClass(tb).toString());
                features.put("ontology", ontologyHelper.getSchemeOntologyIri());
                brat2GATE.put(
                        tb.getID(),
                        original.add((long) tb.getStartOffset(), (long) tb.getEndOffset(), "Mention", features));

                return true;
            case 'R':
                gate.creole.brat.annotations.Relation r =
                        (gate.creole.brat.annotations.Relation) annotation;

                if (!brat2GATE.containsKey(r.getArgumentID(0)) ||
                        !brat2GATE.containsKey(r.getArgumentID(1))) return false;

                ontologyHelper.createObjectPropertyAssertion(r);
                return true;
            case '*':
                return true;
            case 'E':
                return true;
            case 'A':
            case 'M':
                return true;
            case 'N':
                return true;
            case '#':
                Note note = (Note) annotation;
                ontologyHelper.annotateResource(note);
                return true;
        }

        throw new Exception("Invalid Line:" + annotation);
    }

    private static FeatureBearer getFeatureBearer(String id,
                                                  Map<String, Integer> brat2GATE,
                                                  AnnotationSet annotations,
                                                  RelationSet relations)
            throws Exception {
        if (!brat2GATE.containsKey(id)) return null;

        if (id.charAt(0) == 'T') {
            return annotations.get(brat2GATE.get(id));
        }

        return relations.get(brat2GATE.get(id));
    }

    @Override
    public Boolean supportsRepositioning() {
        return false;
    }

    @Override
    public Resource init() throws ResourceInstantiationException {

        // create the MIME type object
        MimeType mime = new MimeType("text", "plain");

        // Register the class handler for this mime type
        mimeString2ClassHandlerMap.put(mime.getType() + "/" + mime.getSubtype(),
                this);

        // Register the mime type with mine string
        mimeString2mimeTypeMap.put(mime.getType() + "/" + mime.getSubtype(), mime);

        // Set the mimeType for this language resource
        setMimeType(mime);

        return this;
    }

    @Override
    public void cleanup() {
        super.cleanup();

        MimeType mime = getMimeType();

        mimeString2ClassHandlerMap.remove(mime.getType() + "/" + mime.getSubtype());
        mimeString2mimeTypeMap.remove(mime.getType() + "/" + mime.getSubtype());
    }

    @Override
    public void unpackMarkup(final Document doc) throws DocumentFormatException {
        super.unpackMarkup(doc);

        if (doc.getSourceUrl() == null) return;

        URL annURL = null;

        try {
            annURL =
                    new URL(doc.getSourceUrl().toString()
                            .substring(0, doc.getSourceUrl().toString().lastIndexOf(".")) +
                            ".ann");
        } catch (MalformedURLException e) {
            // I don't think this should be possible but you never know....
            throw new DocumentFormatException(e);
        }

        AnnotationSet original = doc.getAnnotations();

        // removes the paragraph annotations added by the text/plain mimetype
        original.clear();

        BufferedReader in = null;
        try {
            merge(original, new Annotations(annURL), ontologyHelper);
        } catch (Exception ioe) {
            throw new DocumentFormatException(ioe);
        } finally {
            if (in != null) IOUtils.closeQuietly(in);
        }
    }
}
