package java.lang;

/**
 * Thrown to indicate that the application has attempted to convert a string to one of the numeric types,
 * but that the string does not have the appropriate format.
 */
public class NumberFormatException extends IllegalArgumentException{
    private static final long serialVersionUID = 9070730590112500498L;

    public NumberFormatException() {
        super();
    }

    public NumberFormatException(String s) {
        super(s);
    }

    static NumberFormatException forInputString(String s) {
        return new NumberFormatException("For input string: \"" + s + "\"");
    }

}
