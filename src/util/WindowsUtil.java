package util;

import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI.SHELLEXECUTEINFO;
import debug.Debug;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static void runJavaAsAdmin(List<String> javaArgs) throws Exception {
        List<String> cmd = new ArrayList<String>(2 + javaArgs.size());
        Collections.addAll(cmd, Constant.JAVA, Constant.JAR_OPTION);
        cmd.addAll(javaArgs);
        if (!Constant.WINDOWS_VISTA_AND_HIGHER) {
            (new ProcessBuilder(cmd)).start();
            return;
        }

        String adminPermissionsTesterProgram = Constant.PROGRAM_DIR + "adminPermissionsTester" + Constant.EXE;
        addMicrosoftRegistryEntry("Windows NT\\CurrentVersion\\AppCompatFlags\\Layers", "SZ", adminPermissionsTesterProgram, "RUNASADMIN");
        try {
            if ((new ProcessBuilder(adminPermissionsTesterProgram)).start().waitFor() == 0) {
                (new ProcessBuilder(cmd)).start();
                return;
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }

        SHELLEXECUTEINFO shellExecuteInfo = new SHELLEXECUTEINFO();
        shellExecuteInfo.lpVerb = "runas";
        shellExecuteInfo.lpFile = '"' + cmd.get(0) + "w\"";
        StringBuilder params = new StringBuilder(512);
        for (int i = 1, j = cmd.size(); i < j; i++) {
            params.append(" \"").append(cmd.get(i)).append('"');
        }
        shellExecuteInfo.lpParameters = params.toString().trim();
        if (!Shell32.INSTANCE.ShellExecuteEx(shellExecuteInfo)) {
            throw new Exception(Kernel32Util.getLastErrorMessage());
        }
    }

    public static boolean isProcessRunning(String process) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader((new ProcessBuilder("tasklist")).start().getInputStream(), Constant.UTF8));
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
