package util;

public class SearchException extends Exception {

    private static final long serialVersionUID = 1L;

    public SearchException(String msg) {
        super(msg);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
