package net.kalbskinder.eyeliner.markup;

/**
 * Thrown when a .eyeliner document is malformed or uses unknown elements.
 */
public class EyelinerParseException extends RuntimeException {

    public EyelinerParseException(String message) {
        super(message);
    }

    public EyelinerParseException(String message, int line, int column) {
        super(message + " (line " + line + ", column " + column + ")");
    }
}
