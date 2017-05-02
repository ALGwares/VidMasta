package search;

import java.io.File;
import java.net.URLEncoder;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.util.VideoSearch;
import str.Str;
import util.Connection;
import util.Constant;
import util.IO;
import util.MediaPlayer;
import util.Regex;
import util.Worker;

public class SummaryReader extends Worker {

    GuiListener guiListener;
    private Video video;

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
        File speech = new File(Constant.TEMP_DIR + Regex.toFileName(video.title + '-' + video.year) + "-" + (video.ID.hashCode() & 0xfffffff) + Str.get(768));
        if (speech.exists()) {
            read(speech);
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

        Connection.saveData(String.format(Str.get(767), URLEncoder.encode(Regex.clean(Regex.replaceAll(Regex.replaceAll(newSummary, 468), 470), false),
                Constant.UTF8)), speech.getPath(), DomainType.VIDEO_INFO);
        if (!isCancelled()) {
            read(speech);
        }
    }

    private void read(File speech) throws Exception {
        if (MediaPlayer.open(704, speech, true, true)) {
            return;
        }

        String speechName = speech.getName();
        File speechPage = new File(Constant.TEMP_DIR + speechName.substring(0, speechName.lastIndexOf('.')) + Constant.HTML);
        if (!speechPage.exists()) {
            String imagePath;
            IO.write(speechPage, Str.get(769).replace(Str.get(480), speech.getPath().replace('\\', '/')).replace(Str.get(481), Regex.cleanWeirdChars(video.title)
                    + " (" + video.year + ')').replace(Str.get(482), ((new File(imagePath = Constant.CACHE_DIR + VideoSearch.imagePath(video))).exists()
                                    ? imagePath : Constant.PROGRAM_DIR + "noPosterBig.jpg").replace('\\', '/')));
        }

        guiListener.browserNotification(DomainType.VIDEO_INFO);
        IO.browse(speechPage);
    }
}
