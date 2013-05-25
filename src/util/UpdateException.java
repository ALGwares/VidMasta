package util;

public class UpdateException extends Exception {

    private static final long serialVersionUID = 1L;

    public UpdateException(String msg) {
        super(msg);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
