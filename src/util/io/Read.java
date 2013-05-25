package util.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.CRC32;
import util.Constant;

public class Read {

    public static String read(String fileName) throws Exception {
        return read(new File(fileName));
    }

    public static String read(File file) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Constant.UTF8));
        StringBuilder result = new StringBuilder(4096);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append(Constant.NEWLINE);
            }
        } finally {
            CleanUp.close(br);
        }
        return result.toString().trim();
    }

    public static long checksum(File file) throws Exception {
        CRC32 checksum = new CRC32();
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[2048];
            while (fis.read(bytes) != -1) {
                checksum.update(bytes);
            }
        } finally {
            CleanUp.close(fis);
        }
        return checksum.getValue();
    }

    private Read() {
    }
}
