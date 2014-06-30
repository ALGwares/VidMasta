package util;

public interface IOConstant {

    String STD_NEWLINE = "\n", NEWLINE = System.getProperty("line.separator", STD_NEWLINE);
    String FILE_SEPARATOR = System.getProperty("file.separator", "/");
    String UTF8 = "UTF-8";
}
