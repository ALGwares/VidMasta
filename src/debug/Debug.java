package debug;

import java.util.Collection;

public class Debug {

    public static final boolean DEBUG = true;

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

    public static void print(Throwable t) {
        if (DEBUG) {
            t.printStackTrace();
        }
    }

    private Debug() {
    }
}
