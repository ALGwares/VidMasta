package search;

import com.flagstone.transform.DoAction;
import com.flagstone.transform.Movie;
import com.flagstone.transform.MovieHeader;
import com.flagstone.transform.MovieTag;
import com.flagstone.transform.ShowFrame;
import com.flagstone.transform.action.Action;
import com.flagstone.transform.action.BasicAction;
import com.flagstone.transform.sound.SoundStreamBlock;
import com.flagstone.transform.sound.SoundStreamHead;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.util.VideoSearch;
import str.Str;
import util.AbstractWorker;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.MediaPlayer;
import util.Regex;
import util.Worker;

public class SummaryReader extends Worker {

    GuiListener guiListener;
    private Video video;
    long swfName;
    Map<Integer, String> movieParts = new ConcurrentHashMap<Integer, String>(8, 0.75f, 8);
    final AtomicBoolean failure = new AtomicBoolean();

    public SummaryReader(GuiListener guiListener, Video video) {
        this.guiListener = guiListener;
        this.video = video;
    }

    @Override
    protected void doWork() {
        guiListener.summaryReadStarted();
        try {
            readSummary();
        } catch (Exception e) {
            if (!isCancelled()) {
                guiListener.error(e);
            }
        }
        guiListener.summaryReadStopped();
    }

    public void readSummary() throws Exception {
        IO.fileOp(Constant.TEMP_DIR, IO.MK_DIR);
        File swfSpeech = new File(Constant.TEMP_DIR + (swfName = Str.hashCode(video.ID)) + Constant.SWF);
        if (swfSpeech.exists()) {
            browse(swfSpeech);
            return;
        }

        String br1 = "\\<br\\>", br2 = br1 + "\\s*+" + br1;
        String newSummary = Regex.match(video.summary, VideoSearch.summaryTagRegex(Constant.STORYLINE_HTML_ID), br2);
        if (newSummary.isEmpty()) {
            newSummary = Regex.firstMatch(video.summary, VideoSearch.summaryTagRegex(Constant.GENRE_HTML_ID)).isEmpty() ? Regex.match(video.summary,
                    "\\<font[^\\>]++\\>", br2) : Regex.match(video.summary, br2, br2);
        } else {
            newSummary = Regex.match(newSummary, br1, "\\z");
        }

        List<String> summaryParts = Regex.split(Regex.clean(Regex.replaceAll(Regex.replaceAll(newSummary, 468), 470), false), Str.get(477), Integer.parseInt(
                Str.get(478)));
        Collection<MoviePartFinder> moviePartFinders = new ArrayList<MoviePartFinder>(8);

        for (int i = 0, numSummaryParts = summaryParts.size(); i < numSummaryParts; i++) {
            moviePartFinders.add(new MoviePartFinder(i, Str.get(474) + URLEncoder.encode(summaryParts.get(i), Constant.UTF8)));
        }

        AbstractWorker.executeAndWaitFor(moviePartFinders);
        if (isCancelled() || failure.get()) {
            return;
        }

        convertMoviesToAudioClip().encodeToFile(swfSpeech);

        browse(swfSpeech);
    }

    private Movie convertMoviesToAudioClip() throws Exception {
        Movie audioClip = new Movie();
        for (int i = 0, numMovieParts = movieParts.size(); i < numMovieParts; i++) {
            boolean add = false;
            MovieTag prevTag = null;
            Movie movie = new Movie();
            File moviePart = new File(movieParts.get(i));
            movie.decodeFromFile(moviePart);
            for (MovieTag tag : movie.getObjects()) {
                if (!add) {
                    if (i == 0) {
                        add = true;
                    } else if (tag instanceof SoundStreamHead) {
                        add = true;
                        continue;
                    }
                }
                if (add && (tag instanceof SoundStreamBlock || tag instanceof ShowFrame || tag instanceof MovieHeader || tag instanceof SoundStreamHead)) {
                    if (!(prevTag instanceof ShowFrame) || !(tag instanceof ShowFrame)) {
                        audioClip.add(tag);
                    }
                    prevTag = tag;
                }
            }
            IO.fileOp(moviePart, IO.RM_FILE);
        }
        audioClip.add(new DoAction(Arrays.asList((Action) BasicAction.STOP, BasicAction.END)));
        audioClip.add(ShowFrame.getInstance());
        return audioClip;
    }

    private void browse(File swfSpeech) throws Exception {
        if (MediaPlayer.open(704, swfSpeech, true, true)) {
            return;
        }

        File swfPage = new File(Constant.TEMP_DIR + swfName + Constant.HTML);
        if (!swfPage.exists()) {
            String imagePath;
            IO.write(swfPage, Str.get(479).replace(Str.get(480), swfSpeech.getPath()).replace(Str.get(481), Regex.cleanWeirdChars(video.title) + " (" + video.year
                    + ')').replace(Str.get(482), (new File(imagePath = Constant.CACHE_DIR + VideoSearch.imagePath(video))).exists() ? imagePath
                                    : Constant.PROGRAM_DIR + "noPosterBig.jpg"));
        }

        guiListener.browserNotification(DomainType.VIDEO_INFO);
        IO.browse(swfPage);
    }

    private class MoviePartFinder extends Worker {

        private Integer partNumber;
        private String url;

        MoviePartFinder(Integer partNumber, String url) {
            this.partNumber = partNumber;
            this.url = url;
        }

        @Override
        protected void doWork() throws Exception {
            try {
                if (failure.get()) {
                    return;
                }
                String source = Connection.getSourceCode(url, DomainType.VIDEO_INFO);

                if (!Regex.firstMatch(source, 485).isEmpty()) {
                    Connection.removeFromCache(url);
                    throw new ConnectionException(Connection.serverError(url));
                }

                if (failure.get()) {
                    return;
                }

                url = Regex.match(source, 475);
                String movie = Constant.TEMP_DIR + swfName + "_" + partNumber + Constant.SWF;
                if (failure.get()) {
                    return;
                }
                Connection.saveData(url, movie, DomainType.VIDEO_INFO);
                if (failure.get()) {
                    return;
                }
                movieParts.put(partNumber, movie);
            } catch (Exception e) {
                failure.set(true);
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }
        }
    }
}
