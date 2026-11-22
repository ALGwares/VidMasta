package search.download;

import debug.Debug;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import listener.GuiListener;
import listener.Video;
import search.util.TorrentSite2;
import torrent.Torrent;
import util.Constant;
import util.Regex;

// Finds torrents on the torrent site that is searched along with The Pirate Bay (TorrentSite2), whose search results are read like The Pirate Bay's
class TorrentFinder2 extends TorrentFinder {

  TorrentFinder2(GuiListener guiListener, Collection<Torrent> torrents, Video video, String seasonAndEpisode, boolean orderByLeechers, boolean magnetLinkOnly,
          boolean ignoreYear, boolean isOldTitle, TorrentSearchState searchState) {
    super(guiListener, Collections.<TorrentFinder>emptyList(), torrents, video, seasonAndEpisode, orderByLeechers, magnetLinkOnly, ignoreYear, isOldTitle,
            false, searchState, null, false, false);
  }

  String query() {
    return Regex.clean(video.title) + (ignoreYear ? "" : ' ' + video.year) + seasonAndEpisode;
  }

  @Override
  public Collection<Torrent> getTorrents(boolean prefetch, boolean generalSearch) throws Exception {
    if (isCancelled()) {
      return null;
    }
    String sourceCode = TorrentSite2.search(query(), video.isTVShow, orderByLeechers, !prefetch, Constant.MS_1HR, this::isCancelled);
    return (prefetch || isCancelled() ? null : getTorrents(TorrentSite2.class.getSimpleName(), sourceCode));
  }

  @Override
  int maxNumAttempts() {
    return 1; // Another attempt would search the same way
  }

  // Not shown, because this site's results only add to The Pirate Bay's, whose errors are shown. vidmasta-tester checks this site daily.
  @Override
  void error(Exception e) {
    if (Debug.DEBUG) {
      Debug.print(e);
    }
  }

  @Override
  String torrentID(String videoStr) {
    return TorrentSite2.torrentID(videoStr);
  }

  @Override
  String infoHash(String videoStr) throws Exception {
    try {
      return TorrentSite2.infoHash(videoStr, true, this::isCancelled);
    } catch (InterruptedException | CancellationException e) {
      throw e;
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      return null; // The other torrents found might still be usable
    }
  }
}
