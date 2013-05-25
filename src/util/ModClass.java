package util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.tools.JavaFileObject.Kind;
import util.io.CleanUp;
import util.io.Write;

public class ModClass {

    private String binaryName, resourceName;
    private byte[][] byteReplacements;
    private byte[] bytes;

    public ModClass(String binaryName, byte[]... byteReplacements) throws Exception {
        if (byteReplacements.length % 2 != 0) {
            throw new Exception("A byte replacement must be a pair of byte[] (bytes to replace followed by replacement bytes)");
        }
        this.binaryName = binaryName;
        resourceName = binaryName.replace('.', '/') + Kind.CLASS.extension;
        this.byteReplacements = Arrays.copyOf(byteReplacements, byteReplacements.length);
    }

    public static void mod(String jarFilePath, ModClass... modClasses) throws Exception {
        List<ModClass> modClasses2 = Arrays.asList(modClasses);
        if ((new HashSet<ModClass>(modClasses2)).size() != modClasses2.size()) {
            throw new Exception("There cannot be duplicate classes for modification");
        }

        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineClass.setAccessible(true);

        List<ModClass> modClasses3 = new ArrayList<ModClass>(modClasses2);
        ZipFile zf = null;
        try {
            zf = new ZipFile(jarFilePath);
            Enumeration<? extends ZipEntry> zipEntries = zf.entries();
            while (zipEntries.hasMoreElements() && !modClasses3.isEmpty()) {
                ZipEntry ze = zipEntries.nextElement();
                if (ze.getSize() <= 0) {
                    continue;
                }
                String zipEntryName = ze.getName();
                ListIterator<ModClass> modClassesIt = modClasses3.listIterator();
                while (modClassesIt.hasNext()) {
                    ModClass modClass = modClassesIt.next();
                    if (modClass.resourceName.equals(zipEntryName)) {
                        modClass.mod(zf.getInputStream(ze));
                        modClassesIt.remove();
                        break;
                    }
                }
            }
        } finally {
            CleanUp.close(zf);
        }

        if (!modClasses3.isEmpty()) {
            throw new FileNotFoundException("A class for modification is missing");
        }

        for (ModClass modClass : modClasses2) {
            defineClass.invoke(ClassLoader.getSystemClassLoader(), modClass.binaryName, modClass.bytes, 0, modClass.bytes.length);
        }
    }

    private void mod(InputStream classInputStream) throws Exception {
        InputStream bis = null;
        ByteArrayOutputStream baos = null;
        try {
            Write.write(bis = new BufferedInputStream(classInputStream), baos = new ByteArrayOutputStream());
            bytes = baos.toByteArray();

            for (int i = 0; i < byteReplacements.length; i += 2) {
                byte[] bytesToReplace = byteReplacements[i];
                if (bytes.length < bytesToReplace.length) {
                    throw new Exception("There must be at least " + bytesToReplace.length + " bytes in the original class file to replace");
                }

                int maxLen = (bytes.length - bytesToReplace.length) + 1, indexOffset = bytesToReplace.length;
                boolean areBytesFound = false;

                outer:
                for (int j = 0; j < maxLen; j++) {
                    for (int k = 0; k < bytesToReplace.length; k++) {
                        if (bytesToReplace[k] != bytes[j + k]) {
                            continue outer;
                        }
                    }
                    if (areBytesFound) {
                        throw new Exception("The bytes to replace must be unique");
                    }
                    baos.reset();
                    baos.write(bytes, 0, j);
                    byte[] replacementBytes = byteReplacements[i + 1];
                    baos.write(replacementBytes, 0, replacementBytes.length);
                    int lastByteIndex = j + indexOffset;
                    if (lastByteIndex < bytes.length) {
                        baos.write(bytes, lastByteIndex, bytes.length - lastByteIndex);
                    }
                    bytes = baos.toByteArray();
                    areBytesFound = true;
                }

                if (!areBytesFound) {
                    throw new Exception("Bytes to replace not found");
                }
            }
        } finally {
            CleanUp.close(bis, baos);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModClass)) {
            return false;
        }
        return binaryName.equals(((ModClass) obj).binaryName);
    }

    @Override
    public int hashCode() {
        return 7 * 31 + (binaryName == null ? 0 : binaryName.hashCode());
    }
}