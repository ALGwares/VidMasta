package util;

public class ConnectionException extends Exception {

    private static final long serialVersionUID = 1L;
    public final String url;

    public ConnectionException() {
        this("", null);
    }

    public ConnectionException(String msg) {
        this(msg, null);
    }

    public ConnectionException(String msg, String url) {
        super(msg);
        this.url = url;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
