package search.download;

import debug.Debug;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;
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

public class EpisodeFinder extends SwingWorker<Object, Object> {

    private GuiListener guiListener;
    private final int ROW;
    private Video video;
    private Element nextEpisodeElement, prevEpisodeElement;
    private String nextEpisodeText = Str.str("unknown"), prevEpisodeText = nextEpisodeText, prevSeasonNum, prevEpisodeNum;

    EpisodeFinder(GuiListener guiListener, int row, Video video) {
        this.guiListener = guiListener;
        ROW = row;
        this.video = video;
        nextEpisodeElement = guiListener.getSummaryElement(Constant.TV_NEXT_EPISODE_HTML_ID);
        prevEpisodeElement = guiListener.getSummaryElement(Constant.TV_PREV_EPISODE_HTML_ID);
    }

    @Override
    protected Object doInBackground() {
        boolean updateSummary = true;
        try {
            findEpisodes();
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
                    guiListener.setSummary(summary, ROW, video.ID);
                    video.summary = summary;
                }
                String season = guiListener.getSeason(ROW, video.ID);
                if (season != null && season.isEmpty() && prevSeasonNum != null && prevEpisodeNum != null) {
                    guiListener.setSeason(prevSeasonNum, ROW, video.ID);
                    guiListener.setEpisode(prevEpisodeNum, ROW, video.ID);
                }
            }
        }
        return null;
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

    private void setEpisodeText(String season, String episode, String airdate, SimpleDateFormat dateFormat, boolean isNextEpisode) {
        String airdateText = airdate;
        if (Regex.isMatch(airdateText, 534)) {
            airdateText = VideoSearch.dateToString(dateFormat, Regex.replaceAll(airdateText, 535), Boolean.parseBoolean(Str.get(558)));
        } else if (Regex.isMatch(airdateText, 546)) {
            airdateText = VideoSearch.dateToString(new SimpleDateFormat(Str.get(547), Locale.ENGLISH), Regex.replaceAll(airdateText, 535), Boolean.parseBoolean(
                    Str.get(559)));
        } else if (airdateText.isEmpty() || Regex.isMatch(airdateText, 537)) {
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

    private void findEpisodes() throws Exception {
        Calendar currDate = Calendar.getInstance();
        currDate.set(Calendar.HOUR_OF_DAY, 0);
        currDate.set(Calendar.MINUTE, 0);
        currDate.set(Calendar.SECOND, 0);
        currDate.set(Calendar.MILLISECOND, 0);
        Date currTime = currDate.getTime();

        String url = VideoSearch.url(video), source = Connection.getSourceCode(url, DomainType.VIDEO_INFO, false);
        List<String> seasons = Regex.matches(source, 520);
        seasons.add(Regex.match(source, 550));
        seasons = sortedNumListSet(seasons, false);
        if (seasons.isEmpty()) {
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(Str.get(538), Locale.ENGLISH);
        List<Episode> episodes = new ArrayList<Episode>(3), prevNextEpisodes = new ArrayList<Episode>(2);

        outer:
        for (String season : seasons) {
            episodes.clear();
            for (String episode : sortedNumListSet(Regex.matches(source = Connection.getSourceCode(url + Str.get(523) + season, DomainType.VIDEO_INFO, false),
                    Str.get(524) + season + Str.get(525), Str.get(526)), true)) {
                Episode currEpisode = new Episode(season, episode, Regex.replaceAll(Regex.match(source, Str.get(528) + season + Str.get(529) + episode
                        + Str.get(530), Str.get(531)), Str.get(532), Str.get(533)));
                episodes.add(currEpisode);
                if (episodes.size() == 3) {
                    Episode prevPrevEpisode = episodes.get(0), prevEpisode = episodes.get(1);
                    if (prevPrevEpisode.airdate.equals(prevEpisode.airdate) && prevEpisode.airdate.equals(currEpisode.airdate)) {
                        episodes.remove(2);
                        prevNextEpisodes.clear();
                        prevNextEpisodes.addAll(episodes);
                        if (prevPrevEpisode.aired || prevEpisode.aired) {
                            break outer;
                        }
                        continue outer;
                    }
                    episodes.remove(0);
                }
                if (!Regex.isMatch(currEpisode.airdate, 534) || dateFormat.parse(Regex.replaceAll(currEpisode.airdate, 535)).compareTo(currTime) > 0) {
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
            setEpisodeText(nextEpisode.season, nextEpisode.episode, nextEpisode.airdate, dateFormat, true);
            prevEpisodeText = nextEpisodeText;
        } else if (numEpisodes > 1) {
            Episode nextEpisode = episodes.get(numEpisodes - 1), prevEpisode = episodes.get(numEpisodes - 2);
            setEpisodeText(nextEpisode.season, nextEpisode.episode, nextEpisode.airdate, dateFormat, true);
            setEpisodeText(prevEpisode.season, prevEpisode.episode, prevEpisode.airdate, dateFormat, false);
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
