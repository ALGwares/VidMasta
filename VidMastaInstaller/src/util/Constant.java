package util;

import java.math.BigDecimal;
import java.util.Locale;

public interface Constant {

    double APP_VERSION = 21.8;
    BigDecimal MIN_JAVA_VERSION = new BigDecimal("1.7");
    String APP_NAME = "VidMasta", INSTALLER = APP_NAME.toLowerCase(Locale.ENGLISH) + "-setup-" + APP_VERSION, AUTO_INSTALLER = "auto-" + INSTALLER;
    String FILE_SEPARATOR = System.getProperty("file.separator", "/");
    String SAVE_DIR = "C:" + FILE_SEPARATOR + "Users" + FILE_SEPARATOR + "Anthony" + FILE_SEPARATOR + "workspace" + FILE_SEPARATOR + "www" + FILE_SEPARATOR;
    String UTF8 = "UTF-8";
}