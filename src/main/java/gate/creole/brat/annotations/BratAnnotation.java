package gate.creole.brat.annotations;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;


public abstract class BratAnnotation {
  
  protected String id, type, target;
  
  protected BratAnnotation(String id) {
    this.id = id;
  }
  
  public String getID() {
    return id;
  }
  
  public String getType() {
    return type;
  }
  
  public String getTarget() {
    return target;
  }
  
  @Override
  public abstract String toString();
  
  public abstract void toJSON(JsonGenerator out) throws JsonGenerationException, IOException;
  
  public static BratAnnotation parse(String annotation) throws IllegalArgumentException {
    String[] data = annotation.split("\t");
    
    switch (annotation.charAt(0)) {
      case 'T':
        return new TextBound(data);
      case 'R':
        return new Relation(data);
      case '*':
        return new Equivalence(data);
      case 'E':
        return new Event(data);
      case 'A':
      case 'M':
        return new Attribute(data);
      case 'N':
        return new Normalization(data);
      case '#':
        return new Note(data);
    }
    
    throw new IllegalArgumentException("Invalid or Unsupported Annotation: " + annotation);
  }
}
