package util;

import debug.Debug;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WindowsUtil {

    public static void addMicrosoftRegistryEntry(String path, String type, String name, String val) {
        try {
            (new ProcessBuilder("reg", "add", "HKCU\\Software\\Microsoft\\" + path, "/v", name, "/t", "REG_" + type, "/d", val, "/f")).start().waitFor();
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    public static boolean canRunProgramsAsAdmin() {
        try {
            String adminPermissionsTesterProgram = Constant.PROGRAM_DIR + "adminPermissionsTester" + Constant.EXE;
            addMicrosoftRegistryEntry("Windows NT\\CurrentVersion\\AppCompatFlags\\Layers", "SZ", adminPermissionsTesterProgram, "RUNASADMIN");
            return (new ProcessBuilder(adminPermissionsTesterProgram)).start().waitFor() == 0;
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            return false;
        }
    }

    public static boolean isProcessRunning(String process) {
        BufferedReader br = null;
        try {
            Process tasklist = (new ProcessBuilder("tasklist")).start();
            br = new BufferedReader(new InputStreamReader(tasklist.getInputStream(), Constant.UTF8));
            String line, processName = process + Constant.EXE;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(processName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        } finally {
            IO.close(br);
        }
        return false;
    }

    private WindowsUtil() {
    }
}
