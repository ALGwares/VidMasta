package search.download;

import debug.Debug;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
  String nextEpisodeText = Str.str("unknown"), prevEpisodeText = nextEpisodeText, prevSeasonNum, prevEpisodeNum;

  EpisodeFinder(GuiListener guiListener, int row, Video video) {
    this.guiListener = guiListener;
    this.row = row;
    this.video = video;
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
      nextEpisodeText = Str.str("summaryConnectionProblem");
      prevEpisodeText = nextEpisodeText;
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    } finally {
      if (!isCancelled()) {
        synchronized (SummaryFinder.LOCK) {
          guiListener.removeSummaryElement(guiListener.getSummaryElement(Constant.EPISODE_LOADING_HTML_ID));
          String summary = showEpisode(guiListener.getSummaryElement(Constant.TV_PREV_EPISODE_HTML_ID), prevEpisodeText, Constant.TV_PREV_EPISODE_HTML_ID,
                  showEpisode(guiListener.getSummaryElement(Constant.TV_NEXT_EPISODE_HTML_ID), nextEpisodeText, Constant.TV_NEXT_EPISODE_HTML_ID,
                          updateSummary ? video.summary : null));
          if (summary != null) {
            summary = Regex.replaceFirst(summary, "\\<img id\\=\"" + Constant.EPISODE_LOADING_HTML_ID + "\"[^\\>]++\\>", "");
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
  }

  private String showEpisode(Element element, String text, String id, String summary) {
    guiListener.insertAfterSummaryElement(element, text);
    String label;
    return summary == null ? null : ((label = Regex.firstMatch(summary, VideoSearch.summaryTagRegex(id))).isEmpty() ? summary : Regex.replaceFirst(summary,
            Pattern.quote(label), label + text));
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

  private void setEpisodeText(String season, String episode, String airdate, Function<String, SimpleDateFormat> getDateFormat, boolean isNextEpisode) {
    SimpleDateFormat airdateFormat = getDateFormat.apply(airdate);
    String airdatePattern, seasonNumber, episodeNumber, episodeText = "S" + (seasonNumber = String.format(Constant.TV_EPISODE_FORMAT, Integer.valueOf(season)))
            + "E" + (episodeNumber = String.format(Constant.TV_EPISODE_FORMAT, Integer.valueOf(episode))) + " (" + Str.str("airdate") + ' '
            + (airdateFormat == null ? Str.str("unknown") : VideoSearch.dateToString(airdateFormat, Regex.replaceAll(airdate, 535), Regex.firstMatch(
                    airdatePattern = airdateFormat.toPattern(), 863).isEmpty() ? null : !Regex.firstMatch(airdatePattern, 864).isEmpty())) + ')';
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
    List<Entry<String, SimpleDateFormat>> dateFormats = Arrays.stream(Regex.split(862, Constant.SEPARATOR2)).map(str -> Regex.split(str,
            Constant.SEPARATOR1)).map(strs -> new SimpleImmutableEntry<>(strs[0], new SimpleDateFormat(strs[1], Locale.ENGLISH))).collect(Collectors.toList());
    Function<String, SimpleDateFormat> getDateFormat = date -> dateFormats.stream().filter(dateFormat -> Regex.isMatch(date, dateFormat.getKey())).map(
            Entry::getValue).findFirst().orElse(null);
    if (findSeasonAndEpisode) {
      setEpisodeText(seasonToFind, episodeToFind, "", getDateFormat, true);
    }
    Calendar currDate = Calendar.getInstance();
    currDate.set(Calendar.HOUR_OF_DAY, 0);
    currDate.set(Calendar.MINUTE, 0);
    currDate.set(Calendar.SECOND, 0);
    currDate.set(Calendar.MILLISECOND, 0);

    String source = Connection.getSourceCode(url, DomainType.VIDEO_INFO, false, Constant.MS_2DAYS);
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
              DomainType.VIDEO_INFO, false, Constant.MS_2DAYS), Str.get(524) + season + Str.get(525), Str.get(526)), true);
      select.accept(currEpisodes, episodeToFind);
      for (int i = 0, j = currEpisodes.size() - 1; i <= j; i++) {
        String episode = currEpisodes.get(i), airdate = Regex.replaceAll(Regex.match(source, Str.get(528) + season + Str.get(529) + episode + Str.get(530),
                Str.get(531)), Str.get(532), Str.get(533));
        SimpleDateFormat airdateFormat = getDateFormat.apply(airdate);
        Calendar currAirdate;
        if (airdateFormat == null) {
          airdate = "";
          currAirdate = null;
        } else {
          (currAirdate = Calendar.getInstance()).setTime(airdateFormat.parse(Regex.replaceAll(airdate, 535)));
          String airdatePattern = airdateFormat.toPattern();
          if (Regex.firstMatch(airdatePattern, 863).isEmpty()) {
            currAirdate.set(Calendar.MONTH, currAirdate.getActualMaximum(Calendar.MONTH));
          }
          if (Regex.firstMatch(airdatePattern, 864).isEmpty()) {
            currAirdate.set(Calendar.DAY_OF_MONTH, currAirdate.getActualMaximum(Calendar.DAY_OF_MONTH));
          }
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
      setEpisodeText(nextEpisode.season, nextEpisode.episode, nextEpisode.airdate, getDateFormat, true);
      prevEpisodeText = nextEpisodeText;
    } else if (numEpisodes > 1) {
      Episode nextEpisode = episodes.get(numEpisodes - 1), prevEpisode = episodes.get(numEpisodes - 2);
      setEpisodeText(nextEpisode.season, nextEpisode.episode, nextEpisode.airdate, getDateFormat, true);
      setEpisodeText(prevEpisode.season, prevEpisode.episode, prevEpisode.airdate, getDateFormat, false);
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
