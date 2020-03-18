package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;


public class Attribute extends BratAnnotation {

  private String value;

  protected Attribute(String... data) throws IllegalArgumentException {
    super(data[0]);

    String parts[] = data[1].split("\\s+", 3);

    type = parts[0];
    target = parts[1];

    if(parts.length > 2) value = parts[2];
  }
  
  public boolean isBoolean() {
    return value == null;
  }
  
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public void toJSON(JsonGenerator out) throws JsonGenerationException, IOException {

    //StringBuilder builder = new StringBuilder();

    // ["A4", "Negation", "E10", true]
    out.writeStartArray();
    out.writeString(id);
    out.writeString(type);
    out.writeString(target);
    if (value == null) {
      out.writeBoolean(true);
    }
    else {
      out.writeString(value);
    }
    out.writeEndArray();

    /*builder.append("[ \"");
    builder.append(id);
    builder.append("\", \"");
    builder.append(type);
    builder.append("\", \"");
    builder.append(target);
    builder.append("\", ");
    if (value == null) {
      builder.append("true");
    }
    else {
      builder.append("\"");
      builder.append(value);
      builder.append("\"");
    }
    
    builder.append(" ]");*/

    //return builder.toString();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append(id);
    builder.append("\t");
    builder.append(type);
    builder.append(" ");
    builder.append(target);
    if(value != null) {
      builder.append(" ");
      builder.append(value);
    }

    return builder.toString();
  }

  public static void main(String args[]) throws Exception {
    JsonFactory jsonF = new JsonFactory();
    JsonGenerator jsonG = jsonF.createGenerator(System.out);
    String example = "A1\tNegation E1";
    BratAnnotation bound = parse(example);
    System.out.println(example);
    System.out.println(bound);
    bound.toJSON(jsonG);
    jsonG.flush();

    System.out.println("\n");

    example = "A2\tConfidence E2 L1";
    bound = parse(example);
    System.out.println(example);
    System.out.println(bound);
    bound.toJSON(jsonG);    
    jsonG.flush();
  }
}
