package util;

import i18n.I18nStr;

public class ExceptionUtil {

    public static String toString(Exception e) {
        String msg = e.getMessage();

        if (e instanceof IndexOutOfBoundsException && msg != null) {
            String[] msgParts = msg.split(":");
            if (msgParts.length > 0) {
                String index = msgParts[msgParts.length - 1].trim();
                if (index.matches("\\d++")) {
                    return I18nStr.str("indexOutOfBounds", index);
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

    public static Exception cause(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof Exception) {
            return (Exception) cause;
        }
        return new Exception(cause);
    }

    public static RuntimeException unwrap(Exception e) {
        Exception cause = cause(e);
        return new RuntimeException(cause.getMessage(), cause);
    }

    private ExceptionUtil() {
    }
}
