package onlinesearch;

import com.flagstone.transform.Movie;
import com.flagstone.transform.MovieHeader;
import com.flagstone.transform.MovieTag;
import com.flagstone.transform.ShowFrame;
import com.flagstone.transform.sound.SoundStreamBlock;
import com.flagstone.transform.sound.SoundStreamHead;
import gui.AbstractSwingWorker;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;
import listener.GuiListener;
import main.Str;
import onlinesearch.util.SwingWorkerUtil;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.Regex;
import util.io.Write;

public class SummaryReader extends AbstractSwingWorker {

    GuiListener guiListener;
    private String titleID, title, year, summary;
    String swfName;
    Map<Integer, String> movieParts = new ConcurrentHashMap<Integer, String>(8, .75f, 8);
    final AtomicBoolean failure = new AtomicBoolean();

    public SummaryReader(GuiListener guiListener, String titleID, String title, String year, String summary) {
        this.guiListener = guiListener;
        this.titleID = titleID;
        this.title = title;
        this.year = year;
        this.summary = summary;
    }

    @Override
    protected Object doInBackground() {
        guiListener.summaryReadStarted();
        try {
            readSummary();
        } catch (Exception e) {
            if (!isCancelled()) {
                guiListener.connectionError(e);
            }
        }
        guiListener.summaryReadStopped();
        workDone();
        return null;
    }

    public void readSummary() throws Exception {
        Write.fileOp(Constant.TEMP_DIR, Write.MK_DIR);
        swfName = Str.hashCodeStr(titleID);
        String swfSpeech = Constant.TEMP_DIR + swfName + Constant.SWF;
        String swfPage = Constant.TEMP_DIR + swfName + Constant.HTML;
        if ((new File(swfSpeech)).exists() && (new File(swfPage)).exists()) {
            browse(swfPage);
            return;
        }

        String br1 = "<br>", br2 = br1 + "\\s*+" + br1;
        String newSummary = Regex.match(summary, "Storyline:", br2);
        if (newSummary.isEmpty()) {
            newSummary = Regex.match(summary, "Genre:").isEmpty() ? Regex.match(summary, "<font[^>]++>", br2) : Regex.match(summary, br2, br2);
        } else {
            newSummary = Regex.match(newSummary, br1, "\\z");
        }

        summary = newSummary.replaceAll(Str.get(468), Str.get(469)).replaceAll(Str.get(470), Str.get(471));
        for (Entry<String, String> entry : Regex.badStrs.entrySet()) {
            String hexCode = entry.getKey();
            if (hexCode.charAt(0) == '&') {
                summary = summary.replaceAll(hexCode, entry.getValue());
            }
        }
        summary = Str.htmlToPlainText(summary).replaceAll(Str.get(472), Str.get(473)).replaceAll(Str.get(339), Str.get(340)).trim();

        List<String> summaryParts = new ArrayList<String>(8);
        getSummaryParts(summaryParts, summary, Integer.parseInt(Str.get(478)));

        Collection<MoviePartFinder> moviePartFinders = new ArrayList<MoviePartFinder>(8);

        int numSummaryParts = summaryParts.size();
        for (int i = 0; i < numSummaryParts; i++) {
            moviePartFinders.add(new MoviePartFinder(i, Str.get(474) + URLEncoder.encode(summaryParts.get(i), Constant.UTF8)));
        }

        SwingWorkerUtil.execute(this, moviePartFinders);
        if (isCancelled() || failure.get()) {
            return;
        }

        Movie speech = convertMoviesToAudioClip();
        speech.encodeToFile(new File(swfSpeech));

        String page = Str.get(479).replace(Str.get(480), swfSpeech).replace(Str.get(481), Str.cleanWeirdChars(title) + " (" + year + ")"), imagePath;
        page = page.replace(Str.get(482), (new File(imagePath = Constant.CACHE_DIR + Video.imagePath(titleID))).exists() ? imagePath : Constant.PROGRAM_DIR
                + "noPosterBig.jpg");

        Write.write(swfPage, page);
        browse(swfPage);
    }

    private Movie convertMoviesToAudioClip() throws Exception {
        Movie audioClip = new Movie();
        int numMovieParts = movieParts.size();

        for (int i = 0; i < numMovieParts; i++) {
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
            Write.fileOp(moviePart, Write.RM_FILE);
        }
        audioClip.add(audioClip.getObjects().get(0));

        return audioClip;
    }

    private void browse(String swfPage) throws Exception {
        guiListener.browserNotification("summary", "read to you", Connection.VIDEO_INFO);
        Connection.browseFile(swfPage);
    }

    private static void getSummaryParts(Collection<String> summaryParts, String summary, int maxLen) {
        int len = summary.length();
        if (len <= maxLen) {
            summaryParts.add(summary);
            return;
        }

        for (int i = 0, j = maxLen; i < len; i++) {
            if (i == maxLen) {
                summaryParts.add(summary.substring(0, j).trim());
                getSummaryParts(summaryParts, summary.substring(j), maxLen);
                return;
            }
            if (Regex.isMatch(String.valueOf(summary.charAt(i)), Str.get(477))) {
                j = i + 1;
            }
        }
    }

    private class MoviePartFinder extends SwingWorker<Object, Object[]> {

        private Integer partNumber;
        private String url;

        MoviePartFinder(Integer partNumber, String url) {
            this.partNumber = partNumber;
            this.url = url;
        }

        @Override
        protected Object doInBackground() throws Exception {
            try {
                if (failure.get()) {
                    return null;
                }
                String source = Connection.getSourceCode(url, Connection.VIDEO_INFO);

                if (!Regex.match(source, Str.get(485)).isEmpty()) {
                    Connection.removeFromCache(url);
                    throw new ConnectionException(Connection.error(url));
                }

                if (failure.get()) {
                    return null;
                }

                url = Regex.match(source, Str.get(475), Str.get(476));
                String movie = Constant.TEMP_DIR + swfName + "_" + partNumber + Constant.SWF;
                if (failure.get()) {
                    return null;
                }
                Connection.saveData(url, movie, Connection.VIDEO_INFO);
                if (failure.get()) {
                    return null;
                }
                movieParts.put(partNumber, movie);
            } catch (Exception e) {
                failure.set(true);
                if (!isCancelled()) {
                    guiListener.connectionError(e);
                }
            }
            return null;
        }
    }
}
