package util;

import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI.SHELLEXECUTEINFO;
import debug.Debug;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static void startPeerBlock() throws Exception {
        File peerBlock = new File(Constant.APP_DIR + Constant.PEER_BLOCK_VERSION);
        if (!peerBlock.exists()) {
            try {
                IO.unzip(Constant.PROGRAM_DIR + Constant.PEER_BLOCK_VERSION + Constant.ZIP, Constant.APP_DIR);
            } catch (Exception e) {
                IO.fileOp(peerBlock, IO.RM_DIR);
                throw e;
            }
        }
        File confVersion = new File(Constant.APP_DIR, Constant.PEER_BLOCK_CONF_VERSION);
        if (!confVersion.exists()) {
            File conf = new File(peerBlock, "peerblock.conf");
            try {
                String confStr = IO.read(conf);
                int startIndex = confStr.indexOf("<Lists>"), endIndex;
                if (startIndex != -1 && (endIndex = confStr.indexOf("</Lists>")) != -1) {
                    BufferedReader br = null;
                    BufferedWriter bw = null;
                    try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(Constant.APP_DIR + Constant.IP_FILTER), Constant.UTF8));
                        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Constant.APP_DIR + Constant.IP_FILTER_P2P), Constant.UTF8));
                        String line;
                        while ((line = br.readLine()) != null) {
                            bw.append("ipRange:").append(line);
                            bw.newLine();
                        }
                        bw.flush();
                    } finally {
                        IO.close(br, bw);
                    }
                    IO.write(conf, (new StringBuilder(confStr.substring(0, startIndex).trim())).append(Constant.NEWLINE).append("    <Lists>").append(
                            Constant.NEWLINE).append("        <List><File>").append(Constant.APP_DIR).append(Constant.IP_FILTER_P2P).append(
                            "</File><Type>block</Type><Description>level1, level2, level3, edu, ads, spyware, proxy, badpeers, Microsoft, spider, hijacked, dshield, forumspam, webexploit, DROP, The Onion Router, Apple, and Sony Online Entertainment</Description><Enabled>yes</Enabled></List>").append(
                                    Constant.NEWLINE).append("    ").append(confStr.substring(endIndex)).toString());
                }
                IO.fileOp(confVersion, IO.MK_FILE);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
        String peerBlockProgram = Constant.APP_DIR + Constant.PEER_BLOCK_VERSION + Constant.FILE_SEPARATOR + Constant.PEER_BLOCK + Constant.EXE;
        addMicrosoftRegistryEntry("Windows NT\\CurrentVersion\\AppCompatFlags\\Layers", "SZ", peerBlockProgram, "RUNASADMIN");
        runJavaAsAdmin(Arrays.asList(Constant.PROGRAM_DIR + Constant.PEER_BLOCK + Constant.JAR, peerBlockProgram, Constant.APP_TITLE, Constant.APP_DIR
                + Constant.PEER_BLOCK + "Running", Constant.APP_DIR + Constant.PEER_BLOCK + "Exit"));
    }

    private WindowsUtil() {
    }
}
