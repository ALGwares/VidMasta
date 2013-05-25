package util;

public class ExceptionUtil {

    public static String toString(Exception e) {
        String msg = e.getMessage();

        if (e instanceof IndexOutOfBoundsException && msg != null) {
            String[] msgParts = msg.split(":");
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

    private ExceptionUtil() {
    }
}
