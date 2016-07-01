package debug;

public class Debug {

    public static final boolean DEBUG = true;

    public static void print(Object obj) {
        if (DEBUG) {
            System.out.print(obj);
        }
    }

    public static void println(Object obj) {
        if (DEBUG) {
            System.out.println(obj);
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
