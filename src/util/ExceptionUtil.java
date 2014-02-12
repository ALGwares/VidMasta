package util;

import java.util.concurrent.ExecutionException;

public class ExceptionUtil {

    public static String toString(Exception e) {
        String msg = e.getMessage();

        if (e instanceof IndexOutOfBoundsException && msg != null) {
            String[] msgParts = Regex.split(msg, ":");
            if (msgParts.length > 0) {
                String index = msgParts[msgParts.length - 1].trim();
                if (index.matches("\\d++")) {
                    return "Index " + index + " is out of bounds";
                }
            }
        }

        if (msg == null || (msg = msg.trim()).isEmpty()) {
            msg = e.toString().trim();
            if (msg.endsWith(":")) {
                msg = msg.substring(0, msg.length() - 1);
            }
        }

        return msg;
    }

    public static Exception cause(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof Exception) {
            return (Exception) cause;
        }
        return new Exception(cause);
    }

    private ExceptionUtil() {
    }
}
