package util;

import debug.Debug;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.channels.FileLock;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class IO {

    public static final int MK_DIR = 0, RM_FILE = 1, MK_FILE = 2, RM_FILE_NOW_AND_ON_EXIT = 3, RM_DIR = 4;

    public static String read(String fileName) throws Exception {
        return read(new File(fileName));
    }

    public static String read(File file) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), IOConstant.UTF8));
            StringBuilder result = new StringBuilder(4096);
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append(IOConstant.NEWLINE);
            }
            return result.toString().trim();
        } finally {
            close(br);
        }
    }

    public static long checksum(File file) throws Exception {
        CRC32 checksum = new CRC32();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            byte[] bytes = new byte[2048];
            while (is.read(bytes) != -1) {
                checksum.update(bytes);
            }
        } finally {
            close(is);
        }
        return checksum.getValue();
    }

    public static void write(String fileName, String contents) throws Exception {
        write(fileName, contents, false);
    }

    public static void write(File file, String contents) throws Exception {
        write(file, contents, false);
    }

    public static void write(String fileName, String contents, boolean append) throws Exception {
        write(new File(fileName), contents, append);
    }

    public static void write(File file, String contents, boolean append) throws Exception {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), IOConstant.UTF8));
            writer.write(contents);
            writer.flush();
        } finally {
            close(writer);
        }
    }

    public static void write(File file, byte[] contents) throws Exception {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            os.write(contents);
            os.flush();
        } finally {
            close(os);
        }
    }

    public static void write(File fileIn, File fileOut) throws Exception {
        InputStream is = null;
        OutputStream os = null;
        try {
            write(is = new BufferedInputStream(new FileInputStream(fileIn)), os = new BufferedOutputStream(new FileOutputStream(fileOut)));
        } finally {
            close(is, os);
        }
    }

    public static void write(InputStream is, OutputStream os) throws Exception {
        int numBytesRead;
        byte[] bytes = new byte[2048];
        while ((numBytesRead = is.read(bytes)) != -1) {
            os.write(bytes, 0, numBytesRead);
        }
        os.flush();
    }

    public static void consumeErrorStream(HttpURLConnection connection) {
        if (connection == null) {
            return;
        }

        InputStream is = null;
        try {
            if ((is = connection.getErrorStream()) != null) {
                byte[] bytes = new byte[2048];
                while (is.read(bytes) != -1) {
                }
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        } finally {
            close(is);
        }
    }

    public static void write(String fileName, Throwable t) {
        try {
            Writer writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            write(fileName, Calendar.getInstance().getTime() + IOConstant.NEWLINE + writer + IOConstant.NEWLINE, true);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    public static void unzip(String zipFile, String outputDir) throws Exception {
        ZipFile zf = null;
        try {
            zf = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> zipEntries = zf.entries();
            Pattern fileSeparator = Pattern.compile("/");
            while (zipEntries.hasMoreElements()) {
                ZipEntry ze = zipEntries.nextElement();
                if (ze.getSize() <= 0) {
                    continue;
                }

                String fileName = ze.getName();
                String[] fileNameParts = fileSeparator.split(fileName);
                mkFile(fileNameParts, 0, fileNameParts.length - 1, outputDir.substring(0, outputDir.length() - 1));

                InputStream is = null;
                OutputStream os = null;
                try {
                    write(is = new BufferedInputStream(zf.getInputStream(ze)), os = new BufferedOutputStream(new FileOutputStream(outputDir + fileName)));
                } finally {
                    close(is, os);
                }
            }
        } finally {
            close(zf);
        }
    }

    private static void mkFile(String[] fileNameParts, int currIndex, int lastIndex, String currPath) {
        String path = currPath + IOConstant.FILE_SEPARATOR + fileNameParts[currIndex];
        File file = new File(path);
        if (currIndex < lastIndex) {
            fileOp(file, MK_DIR);
            mkFile(fileNameParts, currIndex + 1, lastIndex, path);
        } else {
            fileOp(file, MK_FILE);
        }
    }

    private static boolean rmDir(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        rmDirHelper(dir);
        return !dir.exists();
    }

    private static void rmDirHelper(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                rmDirHelper(file);
            } else {
                fileOp(file, RM_FILE);
            }
        }
        fileOp(dir, RM_FILE);
    }

    public static boolean fileOp(File file, int operation) {
        try {
            if (operation == RM_FILE) {
                return file.delete();
            } else if (operation == MK_DIR) {
                return file.mkdirs();
            } else if (operation == MK_FILE) {
                return file.createNewFile();
            } else if (operation == RM_FILE_NOW_AND_ON_EXIT) {
                if (file.delete()) {
                    return true;
                } else {
                    file.deleteOnExit();
                }
            } else if (operation == RM_DIR) {
                return rmDir(file);
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return false;
    }

    public static boolean fileOp(String path, int operation) {
        return fileOp(new File(path), operation);
    }

    public static void close(ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    public static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                }
            }
        }
    }

    public static void close(HttpURLConnection connection, Closeable... closeables) {
        close(closeables);
        if (connection != null) {
            connection.disconnect();
        }
    }

    public static void release(FileLock fileLock) {
        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    public static String parentDir(String path) {
        return parentDir(new File(path));
    }

    public static String parentDir(File file) {
        return dir(file.getParent());
    }

    public static String dir(String dir) {
        if (dir == null || dir.isEmpty()) {
            return "";
        }
        return dir.endsWith(IOConstant.FILE_SEPARATOR) ? dir : dir + IOConstant.FILE_SEPARATOR;
    }

    public static boolean isFileTooOld(File file, long maxAge) {
        long lastModified = file.lastModified();
        return lastModified != 0L && (System.currentTimeMillis() - lastModified) > maxAge;
    }

    private IO() {
    }
}
