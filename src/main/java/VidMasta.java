
import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import main.Main;

// The name of this class (it must be in the default package) is used by Mac's menu bar and Info window.
public class VidMasta {

  public static void main(String[] args) {
    try {
      if (System.getProperty("startFlag", "").contains("true") || ((Supplier<Integer>) () -> {
        try {
          Class<?> versionClass = Class.forName("java.lang.Runtime$Version");
          return (Integer) versionClass.getMethod("feature").invoke(versionClass.getMethod("parse", String.class).invoke(null, System.getProperty(
                  "java.version")));
        } catch (Exception e) { // Ignore
          return 0;
        }
      }).get() < 17) {
        Main.init();
        return;
      }

      String java = System.getProperty("java.home", "java");
      Process process = (new ProcessBuilder(java + (java.equals("java") ? "" : File.separator + "bin" + File.separator + "java"), "-cp", System.getProperty(
              "java.class.path"), "--add-opens", "java.base/sun.security.action=ALL-UNNAMED", "--add-opens", "java.desktop/sun.awt.shell=ALL-UNNAMED",
              "--add-opens", "java.base/java.net=ALL-UNNAMED", "--add-opens", "java.base/java.net=ALL-UNNAMED", "--add-opens",
              "java.base/sun.nio.ch=ALL-UNNAMED", "--add-opens", "java.desktop/sun.swing=ALL-UNNAMED", "--add-opens",
              "java.desktop/javax.swing.plaf.synth=ALL-UNNAMED", "--add-opens", "java.desktop/sun.swing.plaf.synth=ALL-UNNAMED", "--add-opens",
              "java.desktop/sun.swing.table=ALL-UNNAMED", "--add-opens", "java.desktop/javax.swing.plaf.basic=ALL-UNNAMED", "--add-opens",
              "java.base/java.lang=ALL-UNNAMED", "-DstartFlag=\"true\"", VidMasta.class.getName())).redirectErrorStream(true).start();
      if (Debug.DEBUG) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
          String line;
          while ((line = br.readLine()) != null) {
            Debug.println(line);
          }
        }
        process.waitFor();
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      JOptionPane.showMessageDialog(null, "Startup error: " + e, VidMasta.class.getSimpleName(), JOptionPane.ERROR_MESSAGE);
    }
  }

  private VidMasta() {
  }
}
