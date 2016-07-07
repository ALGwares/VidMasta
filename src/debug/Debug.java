package debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

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
            Writer writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            System.out.println("\n" + writer);
        }
    }

    private Debug() {
    }
}
