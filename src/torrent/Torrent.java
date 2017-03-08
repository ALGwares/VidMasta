package torrent;

import java.io.File;
import java.net.URLEncoder;
import str.Str;
import util.Constant;
import util.Regex;

public class Torrent implements Comparable<Torrent> {

    public final String ID, MAGNET_LINK, NAME, EXTENSIONS, COMMENTS_LINK;
    public final File FILE;
    public final boolean IS_SAFE;
    public final int NUM_SOURCES, SIZE_IN_GIB;

    public Torrent(String id, String magnetLink, String name, File file, String extensions, String commentsLink, boolean isSafe, int numSources, int sizeInGiB) {
        ID = id;
        MAGNET_LINK = magnetLink;
        NAME = name;
        FILE = file;
        EXTENSIONS = extensions;
        COMMENTS_LINK = commentsLink;
        IS_SAFE = isSafe;
        NUM_SOURCES = numSources;
        SIZE_IN_GIB = sizeInGiB;
    }

    public String name() {
        return Regex.replaceAll(NAME, 679) + (IS_SAFE ? "" : '-' + Str.str("unsafe")) + (SIZE_IN_GIB > 0 ? "-" + SIZE_IN_GIB + Str.str("GB") : "") + "-"
                + (MAGNET_LINK.hashCode() & 0xfffffff) + EXTENSIONS;
    }

    public String fileName() {
        return Regex.toFileName(NAME) + (IS_SAFE ? "" : '-' + Regex.toFileName(Str.str("unsafe"))) + (SIZE_IN_GIB > 0 ? "-" + SIZE_IN_GIB + Regex.toFileName(
                Str.str("GB")) : "") + "-" + (MAGNET_LINK.hashCode() & 0xfffffff) + EXTENSIONS + Constant.TORRENT;
    }

    public String magnetLinkURL() throws Exception {
        return Str.get(624) + URLEncoder.encode(MAGNET_LINK, Constant.UTF8);
    }

    @Override
    public String toString() {
        return "{'" + NAME + "' " + IS_SAFE + " " + NUM_SOURCES + " " + SIZE_IN_GIB + "GB '" + EXTENSIONS + "'\n'" + MAGNET_LINK + "'\n'" + FILE + "' '" + ID
                + "' '" + COMMENTS_LINK + "'}\n";
    }

    @Override
    public int compareTo(Torrent torrent) {
        if (IS_SAFE && !torrent.IS_SAFE) {
            return -1;
        }
        if (!IS_SAFE && torrent.IS_SAFE) {
            return 1;
        }
        if (NUM_SOURCES > torrent.NUM_SOURCES) {
            return -1;
        }
        if (NUM_SOURCES < torrent.NUM_SOURCES) {
            return 1;
        }
        if (SIZE_IN_GIB < torrent.SIZE_IN_GIB) {
            return -1;
        }
        if (SIZE_IN_GIB > torrent.SIZE_IN_GIB) {
            return 1;
        }
        if (FILE != null && torrent.FILE == null) {
            return -1;
        }
        if (FILE == null && torrent.FILE != null) {
            return 1;
        }
        if (FILE == null) {
            return MAGNET_LINK.compareTo(torrent.MAGNET_LINK);
        }
        return FILE.compareTo(torrent.FILE);
    }

    @Override
    public boolean equals(Object obj) {
        Torrent torrent;
        return this == obj || (obj instanceof Torrent && IS_SAFE == (torrent = (Torrent) obj).IS_SAFE && NUM_SOURCES == torrent.NUM_SOURCES
                && SIZE_IN_GIB == torrent.SIZE_IN_GIB && ((FILE == null && torrent.FILE == null) || (FILE != null && FILE.equals(torrent.FILE)))
                && MAGNET_LINK.equals(torrent.MAGNET_LINK));
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
