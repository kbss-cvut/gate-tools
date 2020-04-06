package cz.cvut.kbss.exception;


/**
 * Exception thrown when there is an error in processing brat annotations.
 **/
public class BratProcessingException extends RuntimeException {

    public BratProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BratProcessingException(String message) {
        super(message);
    }
}
