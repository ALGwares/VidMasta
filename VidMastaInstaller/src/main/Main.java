package main;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import util.Constant;
import util.IO;

public class Main {

    public static void main(String[] args) throws Exception {
        innoExe();
        jar(new File(Constant.SAVE_DIR + Constant.INSTALLER + ".jar"), "installerJAR");
        jar(new File(Constant.SAVE_DIR + Constant.AUTO_INSTALLER + ".jar"), "auto-installer");
        launch4jExe(jar(new File(Constant.AUTO_INSTALLER + ".jar"), "auto-installer"), new File(Constant.SAVE_DIR + Constant.AUTO_INSTALLER + ".exe"));
        //launch4jAdminPermissionsTesterExe(new File("C:" + Constant.FILE_SEPARATOR + "Users" + Constant.FILE_SEPARATOR + "Anthony" + Constant.FILE_SEPARATOR
        //        + "Desktop" + Constant.FILE_SEPARATOR + "adminPermissionsTester.exe"));
    }

    private static File jar(File jar, String installerXMLFolderName) throws Exception {
        jar.delete();
        File installerXML = new File(installerXMLFolderName + Constant.FILE_SEPARATOR + "installer.xml");
        run(null, "java", "-jar", "izpack-standalone-compiler.jar", installerXML.getCanonicalPath(), "-o", jar.getCanonicalPath(), "-c", "bzip2", "-b",
                installerXML.getParentFile().getCanonicalPath());
        return jar;
    }

    static void launch4jExe(File jar, File exe) throws Exception {
        File config = new File("launch4j.xml");
        IO.write(config, ""
                + "<launch4jConfig>\n"
                + "  <headerType>gui</headerType>\n"
                + "  <jar>" + jar.getCanonicalPath() + "</jar>\n"
                + "  <outfile>" + exe.getCanonicalPath() + "</outfile>\n"
                + "  <jre>\n"
                + "    <minVersion>" + Constant.MIN_JAVA_VERSION + ".0</minVersion>\n"
                + "  </jre>\n"
                + "</launch4jConfig>");
        String launch4jDir = "C:" + Constant.FILE_SEPARATOR + "Program Files (x86)" + Constant.FILE_SEPARATOR + "Launch4j" + Constant.FILE_SEPARATOR;
        run(new File(launch4jDir), "java", "-jar", launch4jDir + "launch4j.jar", config.getCanonicalPath());
        config.delete();
        jar.delete();
    }

    static void launch4jAdminPermissionsTesterExe(File exe) throws Exception {
        File config = new File("launch4j.xml");
        IO.write(config, ""
                + "<launch4jConfig>\n"
                + "  <headerType>console</headerType>\n"
                + "  <downloadUrl>about:blank</downloadUrl>\n"
                + "  <errTitle>Testing (Ignore)</errTitle>\n"
                + "  <jar>" + (new File("adminPermissionsTester.jar")).getCanonicalPath() + "</jar>\n"
                + "  <outfile>" + exe.getCanonicalPath() + "</outfile>\n"
                + "  <manifest>" + (new File("dummy.manifest")).getCanonicalPath() + "</manifest>\n"
                + "  <jre>\n"
                + "    <minVersion>" + Constant.MIN_JAVA_VERSION + ".0</minVersion>\n"
                + "  </jre>\n"
                + "</launch4jConfig>");
        String launch4jDir = "C:" + Constant.FILE_SEPARATOR + "Program Files (x86)" + Constant.FILE_SEPARATOR + "Launch4j" + Constant.FILE_SEPARATOR;
        run(new File(launch4jDir), "java", "-jar", launch4jDir + "launch4j.jar", config.getCanonicalPath());
        config.delete();
    }

    static void innoExe() throws Exception {
        File jre7Test = new File("jre7Test.jar"), jre = new File("jre-7u80-windows-i586.exe"), config = new File("config.iss");
        String appDir = "{userappdata}" + Constant.FILE_SEPARATOR + Constant.APP_NAME + Constant.FILE_SEPARATOR, appJar = "{app}" + Constant.FILE_SEPARATOR
                + Constant.APP_NAME + ".jar", publisher = "Anthony Gray", readme = "{app}" + Constant.FILE_SEPARATOR + "readme.html", homePage
                = "https://sites.google.com/site/algwares/", vidMastaPage = homePage + "vidmasta", appId = Constant.APP_NAME + " " + Constant.APP_VERSION,
                icon = "{app}" + Constant.FILE_SEPARATOR + "favicon.ico", exeShortcutConfig = Constant.FILE_SEPARATOR + Constant.APP_NAME
                + " (exe)\"; Filename: \"{code:installJava}cmd.exe\"; Parameters: \"/c start \"\"" + Constant.APP_NAME + "\"\" \"\"" + appJar
                + "\"\"\"; IconFilename: \"" + icon + "\"; WorkingDir: \"{app}\"\n", jarShortcutConfig = Constant.FILE_SEPARATOR + Constant.APP_NAME
                + "\"; Filename: \"{code:installJava}" + appJar + "\"; IconFilename: \"" + icon + "\"; WorkingDir: \"{app}\"\n";
        BigDecimal minJreVersion = (Constant.MIN_JAVA_VERSION.subtract(new BigDecimal(".0001")));

        IO.write(config, ""
                + "[Setup]\n"
                + "AppId=" + appId + '\n'
                + "AppName=" + Constant.APP_NAME + '\n'
                + "AppVersion=" + Constant.APP_VERSION + "\n"
                + "AppVerName=" + appId + '\n'
                + "AppPublisher=" + publisher + '\n'
                + "AppPublisherURL=" + vidMastaPage + '\n'
                + "AppSupportURL=" + homePage + "contact\n"
                + "AppUpdatesURL=" + vidMastaPage + '\n'
                + "DefaultDirName={pf}" + Constant.FILE_SEPARATOR + Constant.APP_NAME + '\n'
                + "DefaultGroupName=" + Constant.APP_NAME + '\n'
                + "DisableProgramGroupPage=yes\n"
                + "DisableReadyPage=yes\n"
                + "OutputDir=" + Constant.SAVE_DIR + '\n'
                + "OutputBaseFilename=" + Constant.INSTALLER + '\n'
                + "Compression=lzma2/max\n"
                + "SolidCompression=yes\n"
                + "CloseApplicationsFilter=*.jar,*.exe,*.dll,*.zip\n"
                + "DisableDirPage=no\n"
                + "DisableFinishedPage=yes\n"
                + "ShowLanguageDialog=no\n"
                + "AppComments=" + appId + '\n'
                + "AppContact=" + publisher + '\n'
                + "AppReadmeFile=" + readme + '\n'
                + "UninstallDisplayIcon=" + icon + '\n'
                + "AllowCancelDuringInstall=no\n"
                + '\n'
                + "[Languages]\n"
                + "Name: \"en\"; MessagesFile: \"compiler:Default.isl\"\n"
                + "Name: \"es\"; MessagesFile: \"compiler:Languages" + Constant.FILE_SEPARATOR + "Spanish.isl\"\n"
                + "Name: \"fr\"; MessagesFile: \"compiler:Languages" + Constant.FILE_SEPARATOR + "French.isl\"\n"
                + "Name: \"it\"; MessagesFile: \"compiler:Languages" + Constant.FILE_SEPARATOR + "Italian.isl\"\n"
                + "Name: \"nl\"; MessagesFile: \"compiler:Languages" + Constant.FILE_SEPARATOR + "Dutch.isl\"\n"
                + "Name: \"pt\"; MessagesFile: \"compiler:Languages" + Constant.FILE_SEPARATOR + "Portuguese.isl\"\n"
                + "Name: \"tr\"; MessagesFile: \"compiler:Languages" + Constant.FILE_SEPARATOR + "Turkish.isl\"\n"
                + '\n'
                + "[Files]\n"
                + "Source: \"" + jre7Test.getCanonicalPath() + "\"; DestDir: \"" + appDir + "\"; Flags: ignoreversion deleteafterinstall\n"
                + "Source: \"" + jre.getCanonicalPath() + "\"; DestDir: \"" + appDir + "\"; Flags: ignoreversion deleteafterinstall\n"
                + "Source: \"C:" + Constant.FILE_SEPARATOR + "Users" + Constant.FILE_SEPARATOR + "Anthony" + Constant.FILE_SEPARATOR + "workspace"
                + Constant.FILE_SEPARATOR + "Netbeans" + Constant.FILE_SEPARATOR + Constant.APP_NAME + Constant.FILE_SEPARATOR + Constant.APP_NAME
                + Constant.FILE_SEPARATOR + "*\"; DestDir: \"{app}\"; Flags: ignoreversion recursesubdirs createallsubdirs\n"
                + '\n'
                + "[Icons]\n"
                + "Name: \"{commonprograms}" + Constant.FILE_SEPARATOR + Constant.APP_NAME + jarShortcutConfig
                + "Name: \"{commonprograms}" + Constant.FILE_SEPARATOR + Constant.APP_NAME + exeShortcutConfig
                + "Name: \"{commonprograms}" + Constant.FILE_SEPARATOR + Constant.APP_NAME + Constant.FILE_SEPARATOR + "Readme"
                + "\"; Filename: \"{code:installJava}" + readme + "\"; WorkingDir: \"{app}\"\n"
                + "Name: \"{commondesktop}" + jarShortcutConfig
                + "Name: \"{commondesktop}" + exeShortcutConfig
                + '\n'
                + "[Code]\n"
                + "var\n"
                + "  InstalledJava: Boolean;\n"
                + '\n'
                + "function installJava(Param: String): String;\n"
                + "  var\n"
                + "    JreVersion: String;\n"
                + "    JreVersion32Bit: Extended;\n"
                + "    JreVersion64Bit: Extended;\n"
                + "    ReturnCode: Integer;\n"
                + "  begin\n"
                + "    if not InstalledJava then\n"
                + "    begin\n"
                + "      InstalledJava := True;\n"
                + "      Try\n"
                + "        if RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\\\\JavaSoft\\\\Java Runtime Environment', 'CurrentVersion', JreVersion) then\n"
                + "          begin\n"
                + "            JreVersion32Bit := StrToFloat(JreVersion);\n"
                + "          end;\n"
                + "      Except\n"
                + "      end;\n"
                + '\n'
                + "      Try\n"
                + "        if RegQueryStringValue(HKEY_LOCAL_MACHINE_64, 'SOFTWARE\\\\JavaSoft\\\\Java Runtime Environment', 'CurrentVersion', JreVersion) then\n"
                + "          begin\n"
                + "            JreVersion64Bit := StrToFloat(JreVersion);\n"
                + "          end;\n"
                + "      Except\n"
                + "      end;\n"
                + '\n'
                + "      if (JreVersion32Bit < " + minJreVersion + ") and (JreVersion64Bit < " + minJreVersion + ") then\n"
                + "        begin\n"
                + "          Try\n"
                + "            Exec(ExpandConstant('" + appDir + jre.getName() + "'), '/s', '', SW_SHOW, ewWaitUntilTerminated, ReturnCode);\n"
                + "          Except\n"
                + "          end;\n"
                + "        end\n"
                + "      else\n"
                + "        begin\n"
                + "          Try\n"
                + "            Exec('javaw.exe', '-jar " + jre7Test.getName() + "', ExpandConstant('" + appDir + "'), SW_SHOW, ewWaitUntilTerminated, ReturnCode);\n"
                + "            if ReturnCode = 1371298452 then\n"
                + "              begin\n"
                + "                ReturnCode := 0;\n"
                + "                ShellExec('', '" + jre7Test.getName() + "', '', ExpandConstant('" + appDir + "'), SW_SHOW, ewWaitUntilTerminated, ReturnCode);\n"
                + "              end;\n"
                + "          Except\n"
                + "          end;\n"
                + '\n'
                + "          if ReturnCode <> 1371298452 then\n"
                + "            begin\n"
                + "              Try\n"
                + "                Exec(ExpandConstant('" + appDir + jre.getName() + "'), '/s', '', SW_SHOW, ewWaitUntilTerminated, ReturnCode);\n"
                + "              Except\n"
                + "              end;\n"
                + "            end;\n"
                + "        end;\n"
                + '\n'
                + "      Try\n"
                + "        ShellExec('', 'chmod.jar', '', ExpandConstant('{app}'), SW_SHOW, ewWaitUntilTerminated, ReturnCode);\n"
                + "      Except\n"
                + "      end;\n"
                + "    end;\n"
                + '\n'
                + "    Result := '';\n"
                + "  end;\n"
                + '\n'
                + "[Run]\n"
                + "Filename: \"javaw.exe\"; Parameters: \"-jar \"\"" + appJar + "\"\"\"; WorkingDir: \"{app}\"; Flags: nowait postinstall skipifsilent\n");
        run(null, "C:" + Constant.FILE_SEPARATOR + "Program Files (x86)" + Constant.FILE_SEPARATOR + "Inno Setup 5" + Constant.FILE_SEPARATOR + "ISCC.exe",
                config.getCanonicalPath());
        config.delete();
    }

    private static void run(File workingDir, String... cmd) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        if (workingDir != null) {
            processBuilder.directory(workingDir);
        }
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        IO.read(process.getInputStream());
        if (process.waitFor() != 0) {
            throw new Exception("Command failed: " + Arrays.toString(cmd));
        }
    }
}