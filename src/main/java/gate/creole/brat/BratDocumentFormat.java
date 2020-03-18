package gate.creole.brat;

import gate.*;
import gate.corpora.MimeType;
import gate.corpora.TextualDocumentFormat;
import gate.creole.ResourceInstantiationException;
import gate.creole.brat.annotations.*;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;
import gate.relations.Relation;
import gate.relations.RelationSet;
import gate.util.DocumentFormatException;
import gate.util.FeatureBearer;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@CreoleResource(name = "brat Document Format", isPrivate = true, autoinstances = {@AutoInstance(hidden = true)})
public class BratDocumentFormat extends TextualDocumentFormat {

  private static final long serialVersionUID = -1710274568830645130L;

  @Override
  public Boolean supportsRepositioning() {
    return false;
  }

  @Override
  public Resource init() throws ResourceInstantiationException {

    // create the MIME type object
    MimeType mime = new MimeType("text", "x-brat");

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

    if(doc.getSourceUrl() == null) return;

    URL annURL = null;

    try {
      annURL =
        new URL(doc.getSourceUrl().toString()
          .substring(0, doc.getSourceUrl().toString().lastIndexOf(".")) +
          ".ann");
    } catch(MalformedURLException e) {
      // I don't think this should be possible but you never know....
      throw new DocumentFormatException(e);
    }

    AnnotationSet original = doc.getAnnotations("Original markups");

    // removes the paragraph annotations added by the text/plain mimetype
    original.clear();

    BufferedReader in = null;
    try {
      merge(original, new Annotations(annURL));
    } catch(Exception ioe) {
      throw new DocumentFormatException(ioe);
    } finally {
      if(in != null) IOUtils.closeQuietly(in);
    }
  }

  public static void merge(Document doc, String annotationSetName, Annotations brat) throws DocumentFormatException {
    merge(doc.getAnnotations(annotationSetName), brat);
  }
  
  public static void merge(AnnotationSet annots, Annotations brat) throws DocumentFormatException {
    try {
      Map<String,Integer> brat2GATE = new HashMap<String,Integer>();
      
      RelationSet relations = annots.getRelations();
      
      List<BratAnnotation> unprocessed = brat.get();

      while(unprocessed.size() > 0) {
        int left = unprocessed.size();

        Iterator<BratAnnotation> it = unprocessed.iterator();
        while(it.hasNext()) {
          BratAnnotation annotation = it.next();

          if(process(annotation, brat2GATE, annots, relations)) it.remove();
        }

        if(left == unprocessed.size())
          throw new DocumentFormatException("invalid file");
      }
    } catch(Exception ioe) {
      throw new DocumentFormatException(ioe);
    } 
  }
  
  private static boolean process(BratAnnotation annotation,
                                 Map<String, Integer> brat2GATE,
                                 AnnotationSet original, RelationSet relations)
    throws Exception {
    switch(annotation.getID().charAt(0)){
      case 'T':
        // return new TextBound(data);
        TextBound tb = (TextBound)annotation;

        FeatureMap features = Factory.newFeatureMap();
        features.put("string", tb.getText());
        brat2GATE.put(
          tb.getID(),
          original.add((long)tb.getStartOffset(), (long)tb.getEndOffset(),
            tb.getType(), features));

        return true;
      case 'R':
        gate.creole.brat.annotations.Relation r =
          (gate.creole.brat.annotations.Relation)annotation;

        if(!brat2GATE.containsKey(r.getArgumentID(0)) ||
          !brat2GATE.containsKey(r.getArgumentID(1))) return false;

        Relation relation =
          relations.addRelation(Brat.RELATION,
            brat2GATE.get(r.getArgumentID(0)),
            brat2GATE.get(r.getArgumentID(1)));

        List<String> labels = new ArrayList<String>();
        labels.add(r.getArgumentLabel(0));
        labels.add(r.getArgumentLabel(1));

        relation.getFeatures().put(Brat.TYPE, r.getType());
        relation.getFeatures().put(Brat.ARGUMENT_LABELS, labels);

        brat2GATE.put(r.getID(), relation.getId());

        return true;
      case '*':
        Equivalence eq = (Equivalence)annotation;
        String[] eqBrat = eq.getIDs();
        int[] eqGate = new int[eqBrat.length];
        for(int i = 0; i < eqBrat.length; ++i) {
          if(!brat2GATE.containsKey(eqBrat[i])) return false;
          eqGate[i] = brat2GATE.get(eqBrat[i]);
        }

        relations.addRelation(Brat.EQUIVALENCE, eqGate);

        return true;
      case 'E':
        Event e = (Event)annotation;

        int[] ids = new int[e.getArgumentCount() + 1];
        labels = new ArrayList<String>();

        if(!brat2GATE.containsKey(e.getTarget())) return false;

        ids[0] = brat2GATE.get(e.getTarget());
        for(int i = 1; i < ids.length; ++i) {
          String bratID = e.getArgumentID(i - 1);
          if(!brat2GATE.containsKey(bratID)) return false;
          ids[i] = brat2GATE.get(bratID);
          labels.add(e.getArgumentLabel(i - 1));
        }

        Relation event = relations.addRelation(Brat.EVENT, ids);
        event.getFeatures().put(Brat.TYPE, e.getType());
        event.getFeatures().put(Brat.ARGUMENT_LABELS, labels);

        brat2GATE.put(e.getID(), event.getId());

        return true;
      case 'A':
      case 'M':
        Attribute a = (Attribute)annotation;

        FeatureBearer fb =
          getFeatureBearer(a.getTarget(), brat2GATE, original, relations);

        if(fb == null) return false;

        if(a.isBoolean()) {
          fb.getFeatures().put(a.getType(), Boolean.TRUE);
        } else {
          fb.getFeatures().put(a.getType(), a.getValue());
        }

        return true;
      case 'N':
        Normalization n = (Normalization)annotation;

        if(!brat2GATE.containsKey(n.getTarget())) return false;

        Annotation annot = original.get(brat2GATE.get(n.getTarget()));

        @SuppressWarnings("unchecked")
        Map<String,String> normalizations =
          (Map<String,String>)annot.getFeatures().get(Brat.NORMALIZATIONS);
        if(normalizations == null) {
          normalizations = new HashMap<String,String>();
          annot.getFeatures().put(Brat.NORMALIZATIONS, normalizations);
        }

        normalizations.put(n.getRID() + ":" + n.getEID(), n.getText());

        return true;
      case '#':
        Note note = (Note)annotation;

        fb = getFeatureBearer(note.getTarget(), brat2GATE, original, relations);

        if(fb == null) return false;

        FeatureMap fm = fb.getFeatures();

        FeatureMap notes = (FeatureMap)fm.get(Brat.NOTES);
        if(notes == null) {
          notes = Factory.newFeatureMap();
          fm.put(Brat.NOTES, notes);
        }

        notes.put(note.getType(), note.getText());

        return true;
    }

    throw new Exception("Invalid Line:" + annotation);
  }

  private static FeatureBearer getFeatureBearer(String id,
                                                Map<String, Integer> brat2GATE,
                                                AnnotationSet annotations,
                                                RelationSet relations)
    throws Exception {
    if(!brat2GATE.containsKey(id)) return null;

    if(id.charAt(0) == 'T') { return annotations.get(brat2GATE.get(id)); }

    return relations.get(brat2GATE.get(id));
  }
}
