package debug;

import java.util.Collection;

public class Debug {

    public static final boolean DEBUG = false;

    public static void print(String str) {
        if (DEBUG) {
            System.out.print(str);
        }
    }

    public static void println(String str) {
        if (DEBUG) {
            System.out.println(str);
        }
    }

    public static void println(Collection<?> collection) {
        if (DEBUG) {
            System.out.println(collection);
        }
    }

    public static void print(Exception e) {
        if (DEBUG) {
            e.printStackTrace();
        }
    }

    private Debug() {
    }
}
