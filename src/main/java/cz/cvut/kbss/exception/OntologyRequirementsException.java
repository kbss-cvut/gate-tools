package cz.cvut.kbss.exception;

/**
 * Exception thrown when there is unsatisfied requirement within processed ontology.
 **/
public class OntologyRequirementsException extends RuntimeException {

    public OntologyRequirementsException(String message, Throwable cause) {
        super(message, cause);
    }

    public OntologyRequirementsException(String message) {
        super(message);
    }
}

