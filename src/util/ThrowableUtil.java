package util;

import i18n.I18nStr;

public class ThrowableUtil {

    public static String toString(Throwable t) {
        String msg = t.getMessage();

        if (t instanceof IndexOutOfBoundsException && msg != null) {
            String[] msgParts = msg.split(":");
            if (msgParts.length > 0) {
                String index = msgParts[msgParts.length - 1].trim();
                if (index.matches("\\d++")) {
                    return I18nStr.str("indexOutOfBounds", index);
                }
            }
        }

        if (msg == null || (msg = msg.trim()).isEmpty()) {
            msg = t.toString().trim();
            if (msg.endsWith(":")) {
                msg = msg.substring(0, msg.length() - 1);
            }
        }

        return msg;
    }

    public static Throwable rootCause(Throwable t) {
        Throwable cause = t;
        for (Throwable t2 = t; t2 != null; t2 = t2.getCause()) {
            cause = t2;
        }
        return cause;
    }

    public static Exception cause(Throwable t) {
        Throwable cause = t.getCause();
        if (cause instanceof Exception) {
            return (Exception) cause;
        }
        return new Exception(cause);
    }

    public static RuntimeException unwrap(Throwable t) {
        Exception cause = cause(t);
        return new RuntimeException(cause.getMessage(), cause);
    }

    private ThrowableUtil() {
    }
}
