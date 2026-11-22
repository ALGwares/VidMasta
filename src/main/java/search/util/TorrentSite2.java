package search.util;

import debug.Debug;
import java.io.File;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import listener.DomainType;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.Regex;

// The torrent site that is searched along with The Pirate Bay. Which site it is, and how its pages are read, is set only by Strs (953 to 980 and the mirrors
// in 895), so that the site can be replaced by updating them. Its pages are loaded from the first of its mirrors that works, starting with the one that last
// worked. Its torrents are converted to the JSON of The Pirate Bay's apibay.org, so that the torrents of both sites are read the same way.
public class TorrentSite2 {

  private static final File MIRROR_INDEX = new File(Constant.APP_DIR + Constant.DOWNLOAD_LINK_INFO2_MIRROR_INDEX);
  private static volatile Integer mirrorIndex; // Of the mirror that last worked, read from MIRROR_INDEX when first needed

  // The popular movies or TV shows (from the first page of Str 953 or 954 that isn't an error page), as apibay.org's JSON
  public static String popular(boolean isTVShow, long cacheExpirationMs, Supplier<Boolean> isCancelled) throws Exception {
    return toApibayJson(getSourceCode(Regex.split(isTVShow ? 954 : 953, Constant.SEPARATOR1), true, cacheExpirationMs, isCancelled), isTVShow);
  }

  // The torrents found for the query, as apibay.org's JSON. Each search page of Str 955 is tried in turn until one isn't an error page (such as a sorted
  // search when the site can't sort). A search page is a format of the query (%1$s), the category (%2$s) and the sort order (%3$s).
  public static String search(String query, boolean isTVShow, boolean orderByLeechers, boolean showStatus, long cacheExpirationMs,
          Supplier<Boolean> isCancelled) throws Exception {
    String[] paths = Regex.split(955, Constant.SEPARATOR1);
    for (int i = 0; i < paths.length; i++) {
      paths[i] = String.format(paths[i], URLEncoder.encode(query, Constant.UTF8), Str.get(isTVShow ? 957 : 956), Str.get(orderByLeechers ? 959 : 958));
    }
    return toApibayJson(getSourceCode(paths, showStatus, cacheExpirationMs, isCancelled), isTVShow);
  }

  // The info hash of a torrent of popular() or search(), from the torrent's page if the search result didn't have it
  public static String infoHash(String torrent, boolean showStatus, Supplier<Boolean> isCancelled) throws Exception {
    String infoHash = Regex.match(torrent, 389);
    return (infoHash.isEmpty() ? Regex.firstMatch(getSourceCode(Regex.match(torrent, "\"url\":\"", "\""), 979, showStatus, Constant.MS_3DAYS, isCancelled), 979)
            : infoHash);
  }

  // The ID of a torrent of popular() or search(): its page's path, or its info hash (like The Pirate Bay's torrent IDs) if it has no page
  public static String torrentID(String torrent) {
    String path = Regex.match(torrent, "\"url\":\"", "\"");
    return (path.isEmpty() ? Regex.match(torrent, 389) : path);
  }

  // Loads the first page of the paths that has a list of torrents (or says that there are none), instead of an error page
  private static String getSourceCode(String[] paths, boolean showStatus, long cacheExpirationMs, Supplier<Boolean> isCancelled) throws Exception {
    for (int i = 0;; i++) {
      try {
        return getSourceCode(paths[i], 978, showStatus, cacheExpirationMs, isCancelled);
      } catch (ErrorPageException e) {
        if (i == paths.length - 1) {
          throw e;
        }
        if (Debug.DEBUG) {
          Debug.println(e.getMessage() + " (error page)");
        }
      }
    }
  }

  // Loads the page at the path of a mirror. A page that the regex doesn't find anything in counts as the mirror failing (such as a parked domain or a bot
  // check that wasn't passed), unless it's the site's own error page (Str 980), which the other mirrors would show too.
  private static String getSourceCode(String path, int validPageRegexIndex, boolean showStatus, long cacheExpirationMs, Supplier<Boolean> isCancelled)
          throws Exception {
    String[] mirrors = Regex.split(895, Constant.SEPARATOR1);
    int firstMirrorIndex = mirrorIndex(mirrors.length);
    Exception firstException = null;
    for (int i = 0; i < mirrors.length; i++) {
      int currMirrorIndex = (firstMirrorIndex + i) % mirrors.length;
      String url = mirrors[currMirrorIndex] + path;
      try {
        String sourceCode = Connection.getSourceCode(url, DomainType.DOWNLOAD_LINK_INFO, showStatus, false, cacheExpirationMs);
        checkCancellation(isCancelled);
        boolean isValidPage = !Regex.firstMatch(sourceCode, validPageRegexIndex).isEmpty();
        if (!isValidPage) {
          Connection.removeFromCache(url);
        }
        boolean isErrorPage = (!isValidPage && !Regex.firstMatch(sourceCode, 980).isEmpty());
        if ((isValidPage || isErrorPage) && currMirrorIndex != firstMirrorIndex) { // The mirror works
          setMirrorIndex(currMirrorIndex);
        }
        if (isErrorPage) {
          throw new ErrorPageException(Connection.serverError(url));
        }
        if (!isValidPage) {
          throw new ConnectionException(Connection.serverError(url));
        }
        return sourceCode;
      } catch (InterruptedException | CancellationException | ErrorPageException e) {
        throw e;
      } catch (Exception e) {
        checkCancellation(isCancelled);
        if (Debug.DEBUG) {
          Debug.println("mirror failed for " + url + ": " + e);
        }
        if (firstException == null) {
          firstException = e;
        }
      }
    }
    throw (firstException == null ? new ConnectionException() : firstException);
  }

  // A cancelled request returns an empty page or fails, which isn't a reason to try another mirror
  private static void checkCancellation(Supplier<Boolean> isCancelled) {
    if (isCancelled.get()) {
      throw new CancellationException();
    }
  }

  private static int mirrorIndex(int numMirrors) {
    Integer index = mirrorIndex;
    if (index == null) {
      try {
        index = (MIRROR_INDEX.exists() ? Integer.valueOf(IO.read(MIRROR_INDEX).trim()) : 0);
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        index = 0;
      }
      mirrorIndex = index;
    }
    return (index >= 0 && index < numMirrors ? index : 0);
  }

  private static void setMirrorIndex(int index) {
    mirrorIndex = index;
    try {
      IO.write(MIRROR_INDEX, String.valueOf(index));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  // The torrents in a page of search results, as apibay.org's JSON with the torrent page's path ("url") added. The info hash is empty if the search results
  // don't show it, and the upload year is this year if they don't show the year.
  static String toApibayJson(String sourceCode, boolean isTVShow) {
    StringBuilder json = new StringBuilder(8192);
    int currYear = Calendar.getInstance().get(Calendar.YEAR);
    for (String torrent : Regex.matches(sourceCode, 960)) {
      String path = Regex.firstMatch(torrent, 975), name = Regex.match(torrent, 962), infoHash = Regex.firstMatch(torrent, 979);
      if (name.isEmpty() || (path.isEmpty() && infoHash.isEmpty())) {
        continue;
      }
      String uploadYear = Regex.firstMatch(Regex.match(torrent, 968), 977);
      // Symbols (such as stars and emoji) are removed from the name, which they aren't part of
      json.append(json.length() == 0 ? '[' : ',').append("{\"name\":\"").append(Regex.htmlToPlainText(name).replaceAll("[\\p{So}\\p{Cs}\\p{Co}\\p{Cn}]++", "")
              .replace('"', '\'').replace('\\', '/').replace('{', '(').replace('}', ')').trim()).append("\",\"info_hash\":\"").append(infoHash).append(
              "\",\"leechers\":\"").append(number(Regex.match(
              torrent, 966))).append("\",\"seeders\":\"").append(number(Regex.match(torrent, 964))).append("\",\"size\":\"").append(bytes(Regex.match(
              torrent, 970))).append("\",\"added_year\":\"").append(uploadYear.isEmpty() ? currYear : (uploadYear.length() == 2 ? 2000 : 0) + Integer.parseInt(
              uploadYear)).append("\",\"status\":\"").append(Regex.isMatch(Regex.match(torrent, 972), 974) ? "trusted" : "member").append(
              "\",\"category\":\"").append(Str.get(isTVShow ? 651 : 652)).append("\",\"url\":\"").append(path).append("\"}");
    }
    return (json.length() == 0 ? "[]" : json.append(']').toString());
  }

  private static String number(String number) {
    String digits = number.replaceAll("\\D++", "");
    return (digits.isEmpty() ? "0" : digits);
  }

  // A size such as "1.4 GB" or "1.4 GiB", in bytes (empty if unknown)
  private static String bytes(String size) {
    String num = Regex.firstMatch(size.replace(",", ""), "\\d++(\\.\\d++)?+"), unit = Regex.firstMatch(size, "(?i)[KMGT]?+i?+B(?!\\p{Alpha})");
    if (num.isEmpty() || unit.isEmpty()) {
      return "";
    }
    return String.valueOf((long) (Double.parseDouble(num) * Math.pow(1024, "BKMGT".indexOf(Character.toUpperCase(unit.charAt(0))))));
  }

  // The site's own error page, which its other mirrors would show too
  private static class ErrorPageException extends ConnectionException {

    private static final long serialVersionUID = 1L;

    ErrorPageException(String msg) {
      super(msg);
    }
  }

  private TorrentSite2() {
  }
}
