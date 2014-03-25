package torrent;

import java.io.File;
import main.Str;
import util.Constant;

public class Torrent implements Comparable<Torrent> {

    public final String ID, MAGNET_LINK, NAME, EXTENSIONS;
    public final File FILE;
    public final boolean IS_SAFE;
    public final int NUM_SOURCES, SIZE_IN_GIB;

    public Torrent(String id, String magnetLink, String name, File file, String extensions, boolean isSafe, int numSources, int sizeInGiB) {
        ID = id;
        MAGNET_LINK = magnetLink;
        NAME = name;
        FILE = file;
        EXTENSIONS = extensions;
        IS_SAFE = isSafe;
        NUM_SOURCES = numSources;
        SIZE_IN_GIB = sizeInGiB;
    }

    public String saveName(boolean fileName) {
        return (fileName ? Str.toFileName(NAME) : NAME) + (IS_SAFE ? "" : "_unsafe") + (SIZE_IN_GIB > 0 ? "_" + SIZE_IN_GIB + "GB" : "") + "_"
                + MAGNET_LINK.hashCode() + EXTENSIONS + Constant.TORRENT;
    }

    @Override
    public String toString() {
        return "{'" + ID + "', '" + NAME + "', " + FILE + ", " + (IS_SAFE ? "safe" : "unsafe") + ", sources=" + NUM_SOURCES + ", size=" + SIZE_IN_GIB + "GiB, '"
                + EXTENSIONS + "', '" + MAGNET_LINK + "'}";
    }

    @Override
    public int compareTo(Torrent torrent) {
        if (IS_SAFE && !torrent.IS_SAFE) {
            return -1;
        } else if (!IS_SAFE && torrent.IS_SAFE) {
            return 1;
        } else if (NUM_SOURCES > torrent.NUM_SOURCES) {
            return -1;
        } else if (NUM_SOURCES < torrent.NUM_SOURCES) {
            return 1;
        } else if (SIZE_IN_GIB < torrent.SIZE_IN_GIB) {
            return -1;
        } else if (SIZE_IN_GIB > torrent.SIZE_IN_GIB) {
            return 1;
        } else if (FILE != null && torrent.FILE == null) {
            return -1;
        } else if (FILE == null && torrent.FILE != null) {
            return 1;
        } else if (FILE == null && torrent.FILE == null) {
            return MAGNET_LINK.compareTo(torrent.MAGNET_LINK);
        }
        return FILE.compareTo(torrent.FILE);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        Torrent torrent;
        return obj instanceof Torrent ? IS_SAFE == (torrent = (Torrent) obj).IS_SAFE && NUM_SOURCES == torrent.NUM_SOURCES && SIZE_IN_GIB == torrent.SIZE_IN_GIB
                && ((FILE == null && torrent.FILE == null) || (FILE != null && FILE.equals(torrent.FILE))) && MAGNET_LINK.equals(torrent.MAGNET_LINK) : false;
    }

    @Override
    public int hashCode() {
        int hash = 7 * 31 + (IS_SAFE ? 1 : 0);
        hash = hash * 31 + NUM_SOURCES;
        hash = hash * 31 + SIZE_IN_GIB;
        hash = hash * 31 + (FILE == null ? 0 : FILE.hashCode());
        return hash * 31 + (MAGNET_LINK == null ? 0 : MAGNET_LINK.hashCode());
    }
}
