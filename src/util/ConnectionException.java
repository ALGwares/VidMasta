package util;

public class ConnectionException extends Exception {

    private static final long serialVersionUID = 1L;
    public final String URL;

    public ConnectionException() {
        this("");
    }

    public ConnectionException(String msg) {
        this(msg, null, null);
    }

    public ConnectionException(String msg, Throwable cause, String url) {
        super(msg, cause);
        URL = url;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
