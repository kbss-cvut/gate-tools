package gate.creole.brat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class AnnotationConfig {

  public Set<String> entities = new HashSet<String>();

  public Set<String> events = new HashSet<String>();

  public Set<String> relations = new HashSet<String>();

  public Set<String> attributes = new HashSet<String>();

  public AnnotationConfig(URL url) throws Exception {
    try (BufferedReader in =
        new BufferedReader(new InputStreamReader(url.openStream()))) {

      Set<String> active = null;

      String line;
      while((line = in.readLine()) != null) {
        if(line.trim().length() == 0) continue;

        line = line.trim();

        // skip comments
        if(line.charAt(0) == '#') continue;

        // skip macro definitions
        if(line.charAt(0) == '<') continue;

        if(line.charAt(0) == '[') {
          if(line.equals("[entities]")) {
            active = entities;
          } else if(line.equals("[events]")) {
            active = events;
          } else if(line.equals("[relations]")) {
            active = relations;
          } else if(line.equals("[attributes]")) {
            active = attributes;
          } else {
            active = null;
          }
        } else if(active != null) {
          active.add(line.split("\\s+", 2)[0]);
        }
      }
    }
  }

  @Override
  public String toString() {
    return entities.toString() + events.toString() + relations.toString()
        + attributes.toString();
  }
}
