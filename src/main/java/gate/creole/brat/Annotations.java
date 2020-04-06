package gate.creole.brat;

import gate.Annotation;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.Utils;
import gate.creole.brat.annotations.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class Annotations implements Iterable<BratAnnotation> {

    // we use a LinkedHashMap so that the annotations are returned in order
    private Map<String, BratAnnotation> indexById =
            new LinkedHashMap<String, BratAnnotation>();

    private Map<Class<? extends BratAnnotation>, Set<String>> indexByType =
            new HashMap<Class<? extends BratAnnotation>, Set<String>>();

    private Map<String, Set<String>> indexByTarget =
            new HashMap<String, Set<String>>();

    private Map<Class<? extends BratAnnotation>, Integer> typeOffsets =
            new HashMap<Class<? extends BratAnnotation>, Integer>();

    public Annotations(URL url) throws IOException {
        try (BufferedReader in =
                     new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                add(BratAnnotation.parse(line));
            }
        }
    }

    public Annotations() {
        // nothing to do here
    }

    @SuppressWarnings("unchecked")
    public static Annotations getBratAnnotations(AnnotationSet annots,
                                                 AnnotationConfig config) {
        Annotations annotations = new Annotations();

        StringBuilder builder = new StringBuilder();

        // TODO replace this with the typeOffsets map
        int t = 0;
        int a = 0;
        int l = 0;
        int n = 0;
        int r = 0;
        int e = 0;

        Map<Integer, String> gate2brat = new HashMap<Integer, String>();
        Set<String> types = new HashSet<String>();
        types.addAll(config.entities);
        types.addAll(config.events);
        AnnotationSet entities = annots.get(types);
        for (Annotation entity : entities) {

            String bratID = "T" + (++t);

            builder.setLength(0);
            builder.append(bratID);
            builder.append("\t");
            builder.append(entity.getType());
            builder.append(" ");
            builder.append(entity.getStartNode().getOffset());
            builder.append(" ");
            builder.append(entity.getEndNode().getOffset());
            builder.append("\t");
            if (entity.getFeatures().containsKey("string")) {
                builder.append(entity.getFeatures().get("string"));
            } else {
                builder.append(Utils.cleanStringFor(annots.getDocument(), entity));
            }

            annotations.add(BratAnnotation.parse(builder.toString()));
            gate2brat.put(entity.getId(), bratID);

            a = addAttributes(bratID, a, config.attributes, entity.getFeatures(),
                    annotations);
            n = addNotes(bratID, n, entity.getFeatures(), annotations);

            if (entity.getFeatures().containsKey(Brat.NORMALIZATIONS)) {
                Object obj = entity.getFeatures().get(Brat.NORMALIZATIONS);
                if (obj instanceof Map) {

                    for (Map.Entry<String, String> entry : ((Map<String, String>) obj)
                            .entrySet()) {
                        builder.setLength(0);
                        builder.append("N");
                        builder.append(++l);
                        builder.append("\tReference ");
                        builder.append(bratID);
                        builder.append(" ");
                        builder.append(entry.getKey());
                        builder.append("\t");
                        builder.append(entry.getValue());

                        annotations.add(BratAnnotation.parse(builder.toString()));
                    }
                }
            }
        }

        List<gate.relations.Relation> relations =
                new ArrayList<gate.relations.Relation>();
        relations.addAll(annots.getRelations().getRelations(Brat.EQUIVALENCE));
        relations.addAll(annots.getRelations().getRelations(Brat.RELATION));
        relations.addAll(annots.getRelations().getRelations(Brat.EVENT));

        while (relations.size() > 0) {
            int left = relations.size();

            Iterator<gate.relations.Relation> it = relations.iterator();
            while (it.hasNext()) {

                gate.relations.Relation relation = it.next();

                String[] bratIDs = new String[relation.getMembers().length];
                boolean ready = true;

                for (int i = 0; i < relation.getMembers().length; ++i) {
                    ready = ready && gate2brat.containsKey(relation.getMembers()[i]);
                    if (!ready) break;
                    bratIDs[i] = gate2brat.get(relation.getMembers()[i]);
                }

                if (!ready) continue;

                if (relation.getType().equals(Brat.EQUIVALENCE)) {
                    builder.setLength(0);
                    builder.append("*\tEquiv");
                    for (String id : bratIDs) {
                        builder.append(" ");
                        builder.append(id);
                    }

                    annotations.add(BratAnnotation.parse(builder.toString()));
                } else if (relation.getType().equals(Brat.RELATION)) {
                    String bratID = "R" + (++r);
                    List<String> labels =
                            (List<String>) relation.getFeatures().get(Brat.ARGUMENT_LABELS);
                    builder.setLength(0);
                    builder.append(bratID);
                    builder.append("\t");
                    builder.append(relation.getFeatures().get(Brat.TYPE));
                    for (int i = 0; i < bratIDs.length; ++i) {
                        builder.append(" ");
                        builder.append(labels.get(i));
                        builder.append(":");
                        builder.append(bratIDs[i]);
                    }

                    annotations.add(BratAnnotation.parse(builder.toString()));
                    gate2brat.put(relation.getId(), bratID);

                    a = addAttributes(bratID, a, config.attributes,
                            relation.getFeatures(), annotations);
                    n = addNotes(bratID, n, relation.getFeatures(), annotations);

                } else if (relation.getType().equals(Brat.EVENT)) {
                    String bratID = "E" + (++e);
                    List<String> labels =
                            (List<String>) relation.getFeatures().get(Brat.ARGUMENT_LABELS);
                    builder.setLength(0);
                    builder.append(bratID);
                    builder.append("\t");
                    builder.append(relation.getFeatures().get(Brat.TYPE));
                    builder.append(":");
                    builder.append(bratIDs[0]);
                    for (int i = 1; i < bratIDs.length; ++i) {
                        builder.append(" ");
                        builder.append(labels.get(i - 1));
                        builder.append(":");
                        builder.append(bratIDs[i]);
                    }
                    annotations.add(BratAnnotation.parse(builder.toString()));
                    gate2brat.put(relation.getId(), bratID);

                    a = addAttributes(bratID, a, config.attributes,
                            relation.getFeatures(), annotations);
                    n = addNotes(bratID, n, relation.getFeatures(), annotations);
                }

                it.remove();
            }

            // TODO pick a better exception type
            if (left == relations.size())
                throw new IllegalArgumentException(
                        "invalid document relations: " + relations);
        }

        return annotations;
    }

    private static int addAttributes(String bratID, int a, Set<String> attributes,
                                     FeatureMap features, Annotations annotations) {
        StringBuilder builder = new StringBuilder();
        for (String attribute : attributes) {
            if (features.containsKey(attribute)) {
                Object value = features.get(attribute);
                builder.setLength(0);
                builder.append("A");
                builder.append(++a);
                builder.append("\t");
                builder.append(attribute);
                builder.append(" ");
                builder.append(bratID);
                if (!(value instanceof Boolean)) {
                    builder.append(" ");
                    builder.append(value.toString());
                }
                annotations.add(BratAnnotation.parse(builder.toString()));
            }
        }
        return a;
    }

    private static int addNotes(String bratID, int n, FeatureMap features,
                                Annotations annotations) {
        StringBuilder builder = new StringBuilder();
        if (features.containsKey(Brat.NOTES)) {
            Object obj = features.get(Brat.NOTES);
            if (obj instanceof FeatureMap) {
                for (Map.Entry<Object, Object> note : ((FeatureMap) obj).entrySet()) {
                    builder.setLength(0);
                    builder.append("#");
                    builder.append(++n);
                    builder.append("\t");
                    builder.append(note.getKey());
                    builder.append(" ");
                    builder.append(bratID);
                    builder.append("\t");
                    builder.append(note.getValue());

                    annotations.add(BratAnnotation.parse(builder.toString()));
                }
            }
        }
        return n;
    }

    public static void main(String args[]) {
        Annotations annots = new Annotations();
        annots.add(BratAnnotation.parse("T1\tPerson 0 10\tHello World"));
        annots
                .add(BratAnnotation.parse("#1\tNotes T1\tThe notes we want to store"));
        System.out.println(annots.toString());
        annots.remove("T1");
        System.out.println("----");
        System.out.println(annots.toString());

    }

    public void clear() {
        indexById.clear();
        indexByType.clear();
    }

    public void add(BratAnnotation annotation) {
        indexById.put(annotation.getID(), annotation);

        Set<String> ids = indexByType.get(annotation.getClass());
        if (ids == null) {
            ids = new HashSet<String>();
            indexByType.put(annotation.getClass(), ids);
        }
        ids.add(annotation.getID());

        if (annotation instanceof Relation) {
            // if we delete the annotation at either end of a relation we need to
            // delete the relation as well, this is a special case as everything else
            // uses the target instead
            Relation r = (Relation) annotation;
            for (int i = 0; i < r.getArgumentCount(); ++i) {
                ids = indexByTarget.get(r.getArgumentID(i));
                if (ids == null) {
                    ids = new HashSet<String>();
                    indexByTarget.put(r.getArgumentID(i), ids);
                }
                ids.add(annotation.getID());
            }
        } else {
            ids = indexByTarget.get(annotation.getTarget());
            if (ids == null) {
                ids = new HashSet<String>();
                indexByTarget.put(annotation.getTarget(), ids);
            }
            ids.add(annotation.getID());
        }

        if (annotation instanceof Equivalence) return;

        if (!typeOffsets.containsKey(annotation.getClass())) {
            typeOffsets.put(annotation.getClass(),
                    Integer.parseInt(annotation.getID().substring(1)));
        } else {
            typeOffsets.put(annotation.getClass(),
                    Math.max(typeOffsets.get(annotation.getClass()),
                            Integer.parseInt(annotation.getID().substring(1))));
        }
    }

    public <T extends BratAnnotation> int getNextID(Class<T> type) {
        if (!typeOffsets.containsKey(type)) return 1;

        return typeOffsets.get(type) + 1;
    }

    public BratAnnotation get(String id) {
        return indexById.get(id);
    }

    public List<BratAnnotation> get() {
        return new ArrayList<BratAnnotation>(indexById.values());
    }

    public BratAnnotation remove(String id) {
        BratAnnotation annot = indexById.remove(id);

        if (annot != null) {
            indexByType.get(annot.getClass()).remove(annot.getID());
        }

        Set<String> dependent = indexByTarget.remove(id);
        if (dependent != null) {
            for (String dId : dependent) {
                remove(dId);
            }
        }

        for (Set<String> values : indexByTarget.values()) {
            values.remove(id);
        }

        return annot;
    }

    @SuppressWarnings("unchecked")
    public <T extends BratAnnotation> Set<T> getDependent(String id,
                                                          Class<T> type) {
        Set<T> dependent = new HashSet<T>();

        if (!indexByTarget.containsKey(id)) return dependent;

        if (!indexByType.containsKey(type)) return dependent;

        Set<String> ids = new HashSet<String>(indexByType.get(type));
        ids.retainAll(indexByTarget.get(id));

        for (String i : ids) {
            dependent.add((T) indexById.get(i));
        }

        return dependent;
    }

    public Set<BratAnnotation> getDependent(String id) {
        Set<BratAnnotation> dependent = new HashSet<BratAnnotation>();

        if (!indexByTarget.containsKey(id)) return dependent;

        for (String i : indexByTarget.get(id)) {
            dependent.add(indexById.get(i));
        }

        return dependent;
    }

    public Set<TextBound> getEventTriggers() {
        // TODO make this an index so it is easy to get out
        Set<TextBound> triggers = new HashSet<TextBound>();
        System.out.println(indexByType.get(Event.class));
        for (String id : indexByType.get(Event.class)) {
            Event event = (Event) indexById.get(id);
            triggers.add((TextBound) indexById.get(event.getTarget()));
        }

        return triggers;
    }

    public boolean remove(BratAnnotation annot) {
        return remove(annot.getID()) != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends BratAnnotation> Set<T> get(Class<T> type) {
        Set<T> found = new HashSet<T>();

        Set<String> ids = indexByType.get(type);

        if (ids != null) {
            for (String id : ids) {
                found.add((T) indexById.get(id));
            }
        }

        return found;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (BratAnnotation annotation : indexById.values()) {
            builder.append(annotation);
            builder.append("\n");
        }

        return builder.toString();
    }

    @Override
    public Iterator<BratAnnotation> iterator() {
        return indexById.values().iterator();
    }
}