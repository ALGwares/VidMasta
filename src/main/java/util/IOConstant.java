package util;

import java.io.File;
import java.util.Locale;

public interface IOConstant {

  String APP_TITLE = "VidMasta";
  double APP_VERSION = 29.1;
  String STD_NEWLINE = "\n", STD_NEWLINE2 = STD_NEWLINE + STD_NEWLINE, NEWLINE = System.getProperty("line.separator", STD_NEWLINE), NEWLINE2 = NEWLINE + NEWLINE;
  String FILE_SEPARATOR = File.separator;
  String UTF8 = "UTF-8";
  String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
  boolean WINDOWS = OS_NAME.startsWith("win"), MAC = OS_NAME.startsWith("mac");
}
