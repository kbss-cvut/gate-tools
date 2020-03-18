package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public class Note extends BratAnnotation {

  private String note;

  protected Note(String... data) {
    super(data[0]);

    String parts[] = data[1].split("\\s+",2);

    type = parts[0];
    target = parts[1];
    
    note = data[2];
  }
  
  public String getText() {
    return note;
  }
  
  public void setText(String text) {
    this.note = text;
  }

  @Override
  public void toJSON(JsonGenerator out) throws JsonGenerationException, IOException {
    
    out.writeStartArray();
    out.writeString(target);
    out.writeString(type);
    out.writeString(note);
    out.writeEndArray();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append(id);
    builder.append("\t");
    builder.append(type);
    builder.append(" ");
    builder.append(target);
    builder.append("\t");
    builder.append(note);

    return builder.toString();
  }

  public static void main(String args[]) throws Exception {
    JsonFactory jsonF = new JsonFactory();
    JsonGenerator jsonG = jsonF.createGenerator(System.out);
    String example = "#1\tAnnotatorNotes T1\tthis annotation is suspect";
    BratAnnotation bound = BratAnnotation.parse(example);
    System.out.println(example);
    System.out.println(bound);
    bound.toJSON(jsonG);
    jsonG.flush();
  }
}
