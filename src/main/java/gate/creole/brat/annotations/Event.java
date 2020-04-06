package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;


public class Event extends BratAnnotation {

    private String[] roles, ids;

    protected Event(String... data) {
        super(data[0]);

        String parts[] = data[1].split("[\\s:]+");

        type = parts[0];
        target = parts[1];

        roles = new String[(parts.length / 2) - 1];
        ids = new String[roles.length];

        for (int i = 0; i < roles.length; ++i) {
            roles[i] = parts[(i * 2) + 2];
            ids[i] = parts[(i * 2) + 3];
        }
    }

    public static void main(String args[]) throws Exception {
        JsonFactory jsonF = new JsonFactory();
        JsonGenerator jsonG = jsonF.createGenerator(System.out);
        String example = "E1\tMERGE-ORG:T2 Org1:T1 Org2:T3";
        BratAnnotation bound = BratAnnotation.parse(example);
        System.out.println(example);
        System.out.println(bound);
        bound.toJSON(jsonG);
        jsonG.flush();
    }

    public String getArgumentLabel(int i) {
        return roles[i];
    }

    public String getArgumentID(int i) {
        return ids[i];
    }

    public int getArgumentCount() {
        return ids.length;
    }

    @Override
    public void toJSON(JsonGenerator out) throws JsonGenerationException, IOException {

    /*StringBuilder builder = new StringBuilder();

    builder.append("[ \"");
    builder.append(id);
    builder.append("\", \"");
    builder.append(target);
    builder.append("\", [ ");

    for(int i = 0; i < roles.length; ++i) {
      builder.append("[ \"");
      builder.append(roles[i]);
      builder.append("\", \"");
      builder.append(ids[i]);
      builder.append("\" ]");
      if(i != roles.length - 1) builder.append(", ");
    }

    builder.append(" ] ]");

    return builder.toString();*/

        out.writeStartArray();
        out.writeString(id);
        out.writeString(target);
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(id);
        builder.append("\t");
        builder.append(type);
        builder.append(":");
        builder.append(target);

        for (int i = 0; i < roles.length; ++i) {
            builder.append(" ");
            builder.append(roles[i]);
            builder.append(":");
            builder.append(ids[i]);
        }

        return builder.toString();
    }
}
