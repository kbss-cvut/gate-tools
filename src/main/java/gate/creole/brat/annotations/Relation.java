package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public class Relation extends BratAnnotation {

    private String[] roles, ids;

    protected Relation(String... data) throws IllegalArgumentException {
        super(data[0]);

        String parts[] = data[1].split("[\\s:]+");

        if (parts.length != 5)
            throw new IllegalArgumentException(
                    "A relation must have a type and two role:id pairs: " + data[1]);

        type = parts[0];

        roles = new String[]{parts[1], parts[3]};
        ids = new String[]{parts[2], parts[4]};
    }

    public static void main(String args[]) throws Exception {
        JsonFactory jsonF = new JsonFactory();
        JsonGenerator jsonG = jsonF.createGenerator(System.out);
        String example = "R1\tOrigin Arg1:T3 Arg2:T4";
        BratAnnotation bound = parse(example);
        System.out.println(example);
        System.out.println(bound);
        bound.toJSON(jsonG);
        jsonG.flush();
    }

    @Override
    public void toJSON(JsonGenerator out) throws JsonGenerationException, IOException {


        // [ "R1", "Anaphora", [ [ "Anaphor", "T2" ], [ "Entity", "T1" ] ] ]

        out.writeStartArray();
        out.writeString(id);
        out.writeString(type);
        out.writeStartArray();
        for (int i = 0; i < roles.length; ++i) {
            out.writeStartArray();
            out.writeString(roles[i]);
            out.writeString(ids[i]);
            out.writeEndArray();
        }
        out.writeEndArray();
        out.writeEndArray();
    }

    public String getArgumentLabel(int i) {
        return roles[i];
    }

    public String getArgumentID(int i) {
        return ids[i];
    }

    public int getArgumentCount() {
        return 2;
    }

    public void reverse() {
        String arg1 = ids[0];
        ids[0] = ids[1];
        ids[1] = arg1;
    }

    public void setArgumentID(int i, String id) {
        ids[i] = id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(id);
        builder.append("\t");
        builder.append(type);

        for (int i = 0; i < roles.length; ++i) {
            builder.append(" ");
            builder.append(roles[i]);
            builder.append(":");
            builder.append(ids[i]);
        }

        return builder.toString();
    }
}
