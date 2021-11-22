package search.download;

import debug.Debug;
import listener.Video;
import search.util.VideoSearch;
import util.Constant;
import util.Regex;

public class VideoUtil {

  public static String describe(Video video) {
    String seasonAndEpisode = "";
    if (video.isTVShow) {
      if (!video.season.isEmpty() && !video.season.equals(Constant.ANY) && !video.episode.isEmpty() && !video.episode.equals(Constant.ANY)) {
        EpisodeFinder episodeFinder = new EpisodeFinder();
        try {
          episodeFinder.findEpisodes(VideoSearch.url(video), video.season, video.episode);
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
        seasonAndEpisode = episodeFinder.nextEpisodeText.replace(" (", ") (");
      }
      seasonAndEpisode = ' ' + (seasonAndEpisode.isEmpty() ? ("S" + (video.season.isEmpty() || video.season.equals(Constant.ANY) ? "--" : video.season) + "E"
              + (video.episode.isEmpty() || video.episode.equals(Constant.ANY) ? "--" : video.episode)) + ')' : seasonAndEpisode);
    } else {
      seasonAndEpisode = ")";
    }
    return Regex.htmlToPlainText(video.title) + " (" + video.year + seasonAndEpisode;
  }
}
