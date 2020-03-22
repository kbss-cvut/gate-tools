package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;


public class TextBound extends BratAnnotation {

    private int[] offsets;

    private String text;

    protected TextBound(String... data) throws IllegalArgumentException {
        super(data[0]);

        String parts[] = data[1].split("[;\\s]+");

        type = parts[0];

        offsets = new int[parts.length - 1];

        if (offsets.length % 2 != 0)
            throw new IllegalArgumentException(
                    "the number of offsets must be a multiple of two: " + data[1]);

        for (int i = 0; i < offsets.length; ++i) {
            offsets[i] = Integer.parseInt(parts[i + 1]);
        }

        text = data[2];
    }

    public static void main(String args[]) throws Exception {
        JsonFactory jsonF = new JsonFactory();
        JsonGenerator jsonG = jsonF.createGenerator(System.out);
        String example = "T1\tOrganization 0 4\tSony";
        BratAnnotation bound = parse(example);
        System.out.println(example);
        System.out.println(bound);
        bound.toJSON(jsonG);
        jsonG.flush();

        System.out.println();

        example = "T1\tLocation 0 5;16 23\tNorth America";
        bound = parse(example);
        System.out.println(example);
        System.out.println(bound);
        bound.toJSON(jsonG);
        jsonG.flush();
    }

    @Override
    public void toJSON(JsonGenerator out) throws JsonGenerationException, IOException {

        out.writeStartArray();
        out.writeString(id);
        out.writeString(type);
        out.writeStartArray();
        for (int i = 0; i < offsets.length; i = i + 2) {
            out.writeStartArray();
            out.writeNumber(offsets[i]);
            out.writeNumber(offsets[i + 1]);
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
        builder.append(" ");

        for (int i = 0; i < offsets.length; ++i) {
            builder.append(offsets[i]);
            if (i != offsets.length - 1)
                builder.append((i + 1) % 2 == 0 ? ";" : " ");
        }

        builder.append("\t");
        builder.append(text);

        return builder.toString();
    }

    public String getText() {
        return text;
    }

    public int getStartOffset() {
        return offsets[0];
    }

    public int getEndOffset() {
        return offsets[offsets.length - 1];
    }
}
