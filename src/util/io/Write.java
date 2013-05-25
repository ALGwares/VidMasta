package util.io;

import debug.Debug;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import util.Constant;

public class Write {

    public static final int MK_DIR = 0, MK_FILE = 1, RM_FILE = 2, RM_FILE_NOW_AND_ON_EXIT = 3;

    public static void write(String fileName, String contents) throws Exception {
        write(new File(fileName), contents, false);
    }

    public static void write(File file, String contents) throws Exception {
        write(file, contents, false);
    }

    public static void write(String fileName, String contents, boolean append) throws Exception {
        write(new File(fileName), contents, append);
    }

    public static void write(File file, String contents, boolean append) throws Exception {
        Writer bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), Constant.UTF8));
            bw.write(contents);
        } finally {
            CleanUp.close(bw);
        }
    }

    public static void write(File fileIn, File fileOut) throws Exception {
        InputStream fis = null;
        OutputStream fos = null;
        try {
            write(fis = new FileInputStream(fileIn), fos = new FileOutputStream(fileOut));
        } finally {
            CleanUp.close(fis, fos);
        }
    }

    public static void write(InputStream is, OutputStream os) throws Exception {
        int numBytesRead;
        byte[] bytes = new byte[2048];
        while ((numBytesRead = is.read(bytes)) != -1) {
            os.write(bytes, 0, numBytesRead);
        }
    }

    public static void unzip(String zipFile, String outputDir) throws Exception {
        ZipFile zf = null;
        try {
            zf = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> zipEntries = zf.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry ze = zipEntries.nextElement();
                if (ze.getSize() <= 0) {
                    continue;
                }

                String fileName = ze.getName();
                String[] fileNameParts = fileName.split("/");
                mkFile(fileNameParts, 0, fileNameParts.length - 1, outputDir.substring(0, outputDir.length() - 1));

                InputStream bis = null;
                OutputStream fos = null;
                try {
                    write(bis = new BufferedInputStream(zf.getInputStream(ze)), fos = new FileOutputStream(new File(outputDir + fileName)));
                } finally {
                    CleanUp.close(bis, fos);
                }
            }
        } finally {
            CleanUp.close(zf);
        }
    }

    private static void mkFile(String[] fileNameParts, int currIndex, int lastIndex, String currPath) throws Exception {
        String path = currPath + Constant.FILE_SEPARATOR + fileNameParts[currIndex];
        File file = new File(path);
        if (currIndex < lastIndex) {
            fileOp(file, MK_DIR);
            mkFile(fileNameParts, currIndex + 1, lastIndex, path);
        } else {
            fileOp(file, MK_FILE);
        }
    }

    public static void rmDir(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    rmDir(file);
                } else {
                    fileOp(file, RM_FILE);
                }
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
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return false;
    }

    public static boolean fileOp(String fileName, int operation) {
        return fileOp(new File(fileName), operation);
    }

    private Write() {
    }
}
