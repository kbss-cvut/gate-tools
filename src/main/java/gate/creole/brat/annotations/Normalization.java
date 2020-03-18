package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public class Normalization extends BratAnnotation {

  private String rid, eid;

  private String note;

  protected Normalization(String... data) throws IllegalArgumentException {
    super(data[0]);

    String parts[] = data[1].split("[\\s:]+", 4);

    if(!parts[0].equals("Reference"))
      throw new IllegalArgumentException("Unknown normalization type: " +
        parts[0]);

    type = "Reference";
    
    target = parts[1];
    rid = parts[2];
    eid = parts[3];

    note = data[2];
  }
  
  public String getRID() {
    return rid;
  }
  
  public String getEID() {
    return eid;
  }
  
  public String getText() {
    return note;
  }
  
  @Override
  public void toJSON(JsonGenerator out) throws JsonGenerationException, IOException {

    // ["N7", "Reference", "T2", "homologene", "4504", "trkB"]
    
    out.writeStartArray();
    out.writeString(id);
    out.writeString("Reference");
    out.writeString(target);
    out.writeString(rid);
    out.writeString(eid);
    out.writeString(note);
    out.writeEndArray();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append(id);
    builder.append("\tReference ");
    builder.append(target);
    builder.append(" ");
    builder.append(rid);
    builder.append(":");
    builder.append(eid);
    builder.append("\t");
    builder.append(note);

    return builder.toString();
  }

  public static void main(String args[]) throws Exception {
    JsonFactory jsonF = new JsonFactory();
    JsonGenerator jsonG = jsonF.createGenerator(System.out);
    String example = "N1\tReference T1 Wikipedia:534366\tBarack Obama";
    BratAnnotation bound = BratAnnotation.parse(example);
    System.out.println(example);
    System.out.println(bound);
    bound.toJSON(jsonG);
    jsonG.flush();
  }
}
