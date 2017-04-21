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
import java.io.Writer;
import java.util.zip.CRC32;

public class IO {

    public static void read(InputStream is) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, Constant.UTF8));
            String line;
            while ((line = br.readLine()) != null) {
                Debug.println(line);
            }
        } finally {
            close(br);
        }
    }

    public static String read(File file) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Constant.UTF8));
            StringBuilder result = new StringBuilder(4096);
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append(Constant.NEWLINE);
            }
            return result.toString().trim();
        } finally {
            close(br);
        }
    }

    public static void write(File file, String contents) throws Exception {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Constant.UTF8));
            writer.write(contents);
            writer.flush();
        } finally {
            close(writer);
        }
    }

    public static void write(File fileIn, File fileOut) throws Exception {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(new FileInputStream(fileIn));
            os = new BufferedOutputStream(new FileOutputStream(fileOut, true));
            int numBytesRead;
            byte[] bytes = new byte[2048];
            while ((numBytesRead = is.read(bytes)) != -1) {
                os.write(bytes, 0, numBytesRead);
            }
            os.flush();
        } finally {
            close(is, os);
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

    private IO() {
    }
}
