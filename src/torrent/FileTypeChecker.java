package torrent;

import debug.Debug;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentDeserialiseImpl;
import str.Str;
import util.Constant;

public class FileTypeChecker {

    private String[] whitelistedFileExts, blacklistedFileExts;
    private List<String> fileExts = new ArrayList<String>(4);
    private static final int MAX_EXT_LEN = Integer.parseInt(Str.get(499)), MAX_EXTS_LEN = Integer.parseInt(Str.get(500));

    public FileTypeChecker(String[] whitelistedFileExts, String[] blacklistedFileExts) {
        this.whitelistedFileExts = Arrays.copyOf(whitelistedFileExts, whitelistedFileExts.length);
        this.blacklistedFileExts = Arrays.copyOf(blacklistedFileExts, blacklistedFileExts.length);
    }

    public boolean isValidFileType(File torrent) {
        try {
            return checkFileType(torrent);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return true;
    }

    private boolean checkFileType(File torrent) throws Exception {
        TOTorrentFile[] files = (new TOTorrentDeserialiseImpl(torrent)).getFiles();
        if (files == null) {
            return true;
        }

        for (TOTorrentFile file : files) {
            try {
                String fileName = (new File(file.getRelativePath())).getName();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex == -1) {
                    continue;
                }
                String fileExtension = fileName.substring(dotIndex, fileName.length());
                for (String currFileExtension : blacklistedFileExts) {
                    if (fileExtension.equalsIgnoreCase(currFileExtension)) {
                        if (Debug.DEBUG) {
                            Debug.println("Invalid file extension (" + currFileExtension + ") for torrent file: " + fileName);
                        }
                        return false;
                    }
                }
                for (String currFileExtension : whitelistedFileExts) {
                    if (fileExtension.equalsIgnoreCase(currFileExtension)) {
                        String extension = fileExtension.toLowerCase(Locale.ENGLISH);
                        if (!fileExts.contains(extension)) {
                            fileExts.add(extension);
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }

        return true;
    }

    public String getFileExts() {
        Collections.sort(fileExts);
        StringBuilder extensions = new StringBuilder(MAX_EXTS_LEN);
        for (String extension : fileExts) {
            if (extension.length() > MAX_EXT_LEN) {
                continue;
            }
            extensions.append(extension);
            if (extensions.length() >= MAX_EXTS_LEN) {
                break;
            }
        }
        return extensions.toString();
    }

    public static String getFileExts(File torrent, String[] whitelistedFileExts) {
        FileTypeChecker fileTypeChecker = new FileTypeChecker(whitelistedFileExts, Constant.EMPTY_STRS);
        fileTypeChecker.isValidFileType(torrent);
        return fileTypeChecker.getFileExts();
    }
}
