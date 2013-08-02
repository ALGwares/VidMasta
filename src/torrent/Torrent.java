package torrent;

import java.io.File;

public class Torrent implements Comparable<Torrent> {

    public final String id, extensions;
    public final File file;
    public final boolean isSafe;
    public final int numSources, sizeInGiB;

    public Torrent(String id, File file, String extensions, boolean isSafe, int numSources, int sizeInGiB) {
        this.id = id;
        this.file = file;
        this.extensions = extensions;
        this.isSafe = isSafe;
        this.numSources = numSources;
        this.sizeInGiB = sizeInGiB;
    }

    @Override
    public String toString() {
        return "{'" + id + "', " + file + ", " + (isSafe ? "safe" : "unsafe") + ", sources=" + numSources + ", size=" + sizeInGiB + "GiB, '" + extensions + "'}";
    }

    @Override
    public int compareTo(Torrent torrent) {
        if (isSafe && !torrent.isSafe) {
            return -1;
        } else if (!isSafe && torrent.isSafe) {
            return 1;
        } else if (numSources > torrent.numSources) {
            return -1;
        } else if (numSources < torrent.numSources) {
            return 1;
        } else if (sizeInGiB < torrent.sizeInGiB) {
            return -1;
        } else if (sizeInGiB > torrent.sizeInGiB) {
            return 1;
        }
        return file.compareTo(torrent.file);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        Torrent torrent;
        return obj instanceof Torrent ? isSafe == (torrent = (Torrent) obj).isSafe && numSources == torrent.numSources && sizeInGiB == torrent.sizeInGiB
                && file.equals(torrent.file) : false;
    }

    @Override
    public int hashCode() {
        int hash = 7 * 31 + (isSafe ? 1 : 0);
        hash = hash * 31 + numSources;
        hash = hash * 31 + sizeInGiB;
        return hash * 31 + (file == null ? 0 : file.hashCode());
    }
}
