package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;


public class Equivalence extends BratAnnotation {

  private String[] ids;

  protected Equivalence(String... data) throws IllegalArgumentException {
    super(data[0]);

    type = "Equiv";
    
    String parts[] = data[1].split("\\s+");

    if(!parts[0].equals("Equiv"))
      throw new IllegalArgumentException("Invalid equivalence type: " + parts[0]);

    ids = new String[parts.length-1];
    System.arraycopy(parts, 1, ids, 0, ids.length);
  }
  
  public String[] getIDs() {
    return ids;
  }

  @Override
  public void toJSON(JsonGenerator out) throws JsonGenerationException, IOException {
    out.writeStartArray();
    out.writeString(id);
    out.writeString("Equiv");
    for (int i = 0 ; i < ids.length; ++i) {
      out.writeString(ids[i]);
    }
    out.writeEndArray();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append(id);
    builder.append("\tEquiv");

    for(int i = 0; i < ids.length; ++i) {
      builder.append(" ");
      builder.append(ids[i]);
    }

    return builder.toString();
  }

  public static void main(String args[]) throws Exception {
    JsonFactory jsonF = new JsonFactory();
    JsonGenerator jsonG = jsonF.createGenerator(System.out);
    String example = "*\tEquiv T1 T2 T3";
    BratAnnotation bound = BratAnnotation.parse(example);
    System.out.println(example);
    System.out.println(bound);
    bound.toJSON(jsonG);
    jsonG.flush();
  }
}
