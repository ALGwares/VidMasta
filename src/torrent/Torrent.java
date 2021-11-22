package torrent;

import java.io.File;
import java.net.URLEncoder;
import str.Str;
import util.Constant;
import util.Regex;

public class Torrent implements Comparable<Torrent> {

  public final String id, magnetLink, name, extensions, commentsLink;
  public final File file;
  public final boolean isSafe;
  public final int numSources, sizeInGiB;

  public Torrent(String id, String magnetLink, String name, File file, String extensions, String commentsLink, boolean isSafe, int numSources, int sizeInGiB) {
    this.id = id;
    this.magnetLink = magnetLink;
    this.name = name;
    this.file = file;
    this.extensions = extensions;
    this.commentsLink = commentsLink;
    this.isSafe = isSafe;
    this.numSources = numSources;
    this.sizeInGiB = sizeInGiB;
  }

  public String name() {
    return Regex.replaceAll(name, 679) + (isSafe ? "" : '-' + Str.str("unsafe")) + (sizeInGiB > 0 ? "-" + sizeInGiB + Str.str("GB") : "") + "-"
            + (magnetLink.hashCode() & 0xfffffff) + extensions;
  }

  public String fileName() {
    return Regex.toFileName(name) + (isSafe ? "" : '-' + Regex.toFileName(Str.str("unsafe"))) + (sizeInGiB > 0 ? "-" + sizeInGiB + Regex.toFileName(
            Str.str("GB")) : "") + "-" + (magnetLink.hashCode() & 0xfffffff) + extensions + Constant.TORRENT;
  }

  public String magnetLinkURL() throws Exception {
    return Str.get(624) + URLEncoder.encode(magnetLink, Constant.UTF8);
  }

  @Override
  public String toString() {
    return "{'" + name + "' " + isSafe + " " + numSources + " " + sizeInGiB + "GB '" + extensions + "'\n'" + magnetLink + "'\n'" + file + "' '" + id
            + "' '" + commentsLink + "'}\n";
  }

  @Override
  public int compareTo(Torrent torrent) {
    if (isSafe && !torrent.isSafe) {
      return -1;
    }
    if (!isSafe && torrent.isSafe) {
      return 1;
    }
    if (numSources > torrent.numSources) {
      return -1;
    }
    if (numSources < torrent.numSources) {
      return 1;
    }
    if (sizeInGiB < torrent.sizeInGiB) {
      return -1;
    }
    if (sizeInGiB > torrent.sizeInGiB) {
      return 1;
    }
    if (file != null && torrent.file == null) {
      return -1;
    }
    if (file == null && torrent.file != null) {
      return 1;
    }
    if (file == null) {
      return magnetLink.compareTo(torrent.magnetLink);
    }
    return file.compareTo(torrent.file);
  }

  @Override
  public boolean equals(Object obj) {
    Torrent torrent;
    return this == obj || (obj instanceof Torrent && isSafe == (torrent = (Torrent) obj).isSafe && numSources == torrent.numSources
            && sizeInGiB == torrent.sizeInGiB && ((file == null && torrent.file == null) || (file != null && file.equals(torrent.file)))
            && magnetLink.equals(torrent.magnetLink));
  }

  @Override
  public int hashCode() {
    int hash = 7 * 31 + (isSafe ? 1 : 0);
    hash = hash * 31 + numSources;
    hash = hash * 31 + sizeInGiB;
    hash = hash * 31 + (file == null ? 0 : file.hashCode());
    return hash * 31 + (magnetLink == null ? 0 : magnetLink.hashCode());
  }
}
