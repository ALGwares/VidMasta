package debug;

import java.util.Arrays;
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

    public static void print(Exception e) {
        if (DEBUG) {
            e.printStackTrace();
        }
    }

    public static void print(Collection<?> collection) {
        if (DEBUG) {
            System.out.println(Arrays.deepToString(collection.toArray()));
        }
    }

    private Debug() {
    }
}
