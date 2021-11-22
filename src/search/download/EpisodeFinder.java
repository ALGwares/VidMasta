package search.download;

import debug.Debug;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import javax.swing.text.Element;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.util.VideoSearch;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.Regex;
import util.Worker;

public class EpisodeFinder extends Worker {

  private GuiListener guiListener;
  private int row;
  private Video video;
  private Element nextEpisodeElement, prevEpisodeElement;
  String nextEpisodeText = Str.str("unknown"), prevEpisodeText = nextEpisodeText, prevSeasonNum, prevEpisodeNum;

  EpisodeFinder(GuiListener guiListener, int row, Video video) {
    this.guiListener = guiListener;
    this.row = row;
    this.video = video;
    nextEpisodeElement = guiListener.getSummaryElement(Constant.TV_NEXT_EPISODE_HTML_ID);
    prevEpisodeElement = guiListener.getSummaryElement(Constant.TV_PREV_EPISODE_HTML_ID);
  }

  EpisodeFinder() {
  }

  @Override
  protected void doWork() {
    boolean updateSummary = true;
    try {
      findEpisodes(VideoSearch.url(video));
    } catch (ConnectionException e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      updateSummary = false;
      nextEpisodeText = Str.str("episodeConnectionProblem");
      prevEpisodeText = nextEpisodeText;
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    } finally {
      if (!isCancelled()) {
        String summary = showEpisode(prevEpisodeElement, prevEpisodeText, Constant.TV_PREV_EPISODE_HTML_ID, showEpisode(nextEpisodeElement,
                nextEpisodeText, Constant.TV_NEXT_EPISODE_HTML_ID, updateSummary ? video.summary : null));
        if (summary != null) {
          guiListener.setSummary(summary, row, video.id);
          video.summary = summary;
        }
        String season = guiListener.getSeason(row, video.id);
        if (season != null && season.isEmpty() && prevSeasonNum != null && prevEpisodeNum != null) {
          guiListener.setSeason(prevSeasonNum, row, video.id);
          guiListener.setEpisode(prevEpisodeNum, row, video.id);
        }
      }
    }
  }

  private String showEpisode(Element element, String text, String id, String summary) {
    guiListener.insertAfterSummaryElement(element, text);
    String label;
    return summary == null ? null : ((label = Regex.firstMatch(summary, VideoSearch.summaryTagRegex(id))).isEmpty() ? summary : Regex.replaceFirst(summary,
            Pattern.quote(label + Constant.TV_EPISODE_PLACEHOLDER), label + text));
  }

  private static List<String> sortedNumListSet(Collection<String> strs, boolean ascending) {
    List<String> nums = new ArrayList<String>(strs.size());
    for (String str : strs) {
      if (Regex.isMatch(str, "\\d++") && !nums.contains(str)) {
        nums.add(str);
      }
    }
    Comparator<String> comparator = new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        return Integer.valueOf(s1).compareTo(Integer.valueOf(s2));
      }
    };
    Collections.sort(nums, ascending ? comparator : Collections.reverseOrder(comparator));
    return nums;
  }

  private void setEpisodeText(String season, String episode, String airdate, DateFormat[] dateFormats, boolean isNextEpisode) {
    String airdateText;
    if (Regex.isMatch(airdate, 534)) {
      airdateText = VideoSearch.dateToString(dateFormats[0], Regex.replaceAll(airdate, 535), Boolean.parseBoolean(Str.get(558)));
    } else if (Regex.isMatch(airdate, 546)) {
      airdateText = VideoSearch.dateToString(dateFormats[1], Regex.replaceAll(airdate, 535), Boolean.parseBoolean(Str.get(559)));
    } else if (Regex.isMatch(airdate, 745)) {
      airdateText = VideoSearch.dateToString(dateFormats[2], Regex.replaceAll(airdate, 535), null);
    } else {
      airdateText = Str.str("unknown");
    }
    String seasonNumber, episodeNumber, episodeText = "S" + (seasonNumber = String.format(Constant.TV_EPISODE_FORMAT, Integer.valueOf(season))) + "E"
            + (episodeNumber = String.format(Constant.TV_EPISODE_FORMAT, Integer.valueOf(episode))) + " (" + Str.str("airdate") + ' ' + airdateText + ')';
    if (isNextEpisode) {
      nextEpisodeText = episodeText;
    } else {
      prevEpisodeText = episodeText;
      prevSeasonNum = seasonNumber;
      prevEpisodeNum = episodeNumber;
    }
  }

  void findEpisodes(String url) throws Exception {
    findEpisodes(url, "", "");
  }

  void findEpisodes(String url, String seasonToFind, String episodeToFind) throws Exception {
    boolean findSeasonAndEpisode = !seasonToFind.isEmpty() && !seasonToFind.equals(Constant.ANY) && !episodeToFind.isEmpty() && !episodeToFind.equals(
            Constant.ANY);
    DateFormat[] dateFormats = {new SimpleDateFormat(Str.get(538), Locale.ENGLISH), new SimpleDateFormat(Str.get(547), Locale.ENGLISH), new SimpleDateFormat(
      Str.get(746), Locale.ENGLISH)};
    if (findSeasonAndEpisode) {
      setEpisodeText(seasonToFind, episodeToFind, "", dateFormats, true);
    }
    Calendar currDate = Calendar.getInstance();
    currDate.set(Calendar.HOUR_OF_DAY, 0);
    currDate.set(Calendar.MINUTE, 0);
    currDate.set(Calendar.SECOND, 0);
    currDate.set(Calendar.MILLISECOND, 0);

    String source = Connection.getSourceCode(url, DomainType.VIDEO_INFO, false, Constant.MS_1DAY);
    List<String> seasons = Regex.matches(source, 520);
    seasons.add(Regex.match(source, 550));
    seasons = sortedNumListSet(seasons, false);

    BiConsumer<List<String>, String> select = (numStrs, numStr) -> {
      if (findSeasonAndEpisode) {
        int num = Integer.parseInt(numStr);
        ListIterator<String> numStrsIt = numStrs.listIterator();
        while (numStrsIt.hasNext()) {
          if (Integer.parseInt(numStrsIt.next()) != num) {
            numStrsIt.remove();
          }
        }
      }
    };
    select.accept(seasons, seasonToFind);

    if (seasons.isEmpty()) {
      return;
    }
    List<Episode> episodes = new ArrayList<Episode>(3), prevNextEpisodes = new ArrayList<Episode>(2);

    outer:
    for (String season : seasons) {
      episodes.clear();
      List<String> currEpisodes = sortedNumListSet(Regex.matches(source = Connection.getSourceCode(String.format(Str.get(782), url, season),
              DomainType.VIDEO_INFO, false, Constant.MS_1DAY), Str.get(524) + season + Str.get(525), Str.get(526)), true);
      select.accept(currEpisodes, episodeToFind);
      for (int i = 0, j = currEpisodes.size() - 1; i <= j; i++) {
        String episode = currEpisodes.get(i), airdate = Regex.replaceAll(Regex.match(source, Str.get(528) + season + Str.get(529) + episode + Str.get(530),
                Str.get(531)), Str.get(532), Str.get(533));
        Calendar currAirdate;
        if (Regex.isMatch(airdate, 534)) {
          (currAirdate = Calendar.getInstance()).setTime(dateFormats[0].parse(Regex.replaceAll(airdate, 535)));
        } else if (Regex.isMatch(airdate, 546)) {
          (currAirdate = Calendar.getInstance()).setTime(dateFormats[1].parse(Regex.replaceAll(airdate, 535)));
          currAirdate.set(Calendar.DAY_OF_MONTH, currAirdate.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else if (Regex.isMatch(airdate, 745)) {
          (currAirdate = Calendar.getInstance()).setTime(dateFormats[2].parse(Regex.replaceAll(airdate, 535)));
          currAirdate.set(Calendar.MONTH, currAirdate.getActualMaximum(Calendar.MONTH));
          currAirdate.set(Calendar.DAY_OF_MONTH, currAirdate.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {
          airdate = "";
          currAirdate = null;
        }

        Episode currEpisode = new Episode(season, episode, airdate);
        episodes.add(currEpisode);
        if (findSeasonAndEpisode) {
          break outer;
        }
        if (episodes.size() == 3) {
          Episode prevPrevEpisode = episodes.get(0), prevEpisode = episodes.get(1);
          if (prevPrevEpisode.airdate.equals(prevEpisode.airdate) && prevEpisode.airdate.equals(currEpisode.airdate) && !prevPrevEpisode.aired
                  && !prevEpisode.aired) {
            episodes.remove(2);
            prevNextEpisodes.clear();
            prevNextEpisodes.addAll(episodes);
            continue outer;
          }
          episodes.remove(0);
        }

        if (currAirdate == null) {
          if (i == j) {
            prevNextEpisodes.clear();
            prevNextEpisodes.addAll(episodes);
            continue outer;
          }
        } else if (currAirdate.compareTo(currDate) > 0) {
          prevNextEpisodes.clear();
          prevNextEpisodes.addAll(episodes);
          continue outer;
        } else {
          currEpisode.aired = true;
        }
      }
      episodes.addAll(prevNextEpisodes);
      break;
    }

    int numEpisodes = episodes.size();
    if (numEpisodes == 1) {
      Episode nextEpisode = episodes.get(0);
      setEpisodeText(nextEpisode.season, nextEpisode.episode, nextEpisode.airdate, dateFormats, true);
      prevEpisodeText = nextEpisodeText;
    } else if (numEpisodes > 1) {
      Episode nextEpisode = episodes.get(numEpisodes - 1), prevEpisode = episodes.get(numEpisodes - 2);
      setEpisodeText(nextEpisode.season, nextEpisode.episode, nextEpisode.airdate, dateFormats, true);
      setEpisodeText(prevEpisode.season, prevEpisode.episode, prevEpisode.airdate, dateFormats, false);
    }
  }

  private static class Episode {

    String season, episode, airdate;
    boolean aired;

    Episode(String season, String episode, String airdate) {
      this.season = season;
      this.episode = episode;
      this.airdate = airdate;
    }
  }
}
