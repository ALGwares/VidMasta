package search.download;

import java.text.DecimalFormat;
import java.util.Collection;
import javax.swing.SwingWorker;
import listener.GuiListener;
import main.Str;
import util.Connection;
import util.Constant;
import util.Regex;

public class CommentsFinder extends SwingWorker<Object, Object> {

    private GuiListener guiListener;
    private String link, name;
    public static final String NO_COMMENTS = "There are no comments." + Constant.STD_NEWLINE + Constant.STD_NEWLINE;
    volatile String comments;

    CommentsFinder(GuiListener guiListener, String link, String name) {
        this.guiListener = guiListener;
        this.link = link;
        this.name = name;
    }

    @Override
    protected Object doInBackground() {
        guiListener.commentsFinderStarted();
        try {
            String commentsStr = Connection.getSourceCode(link, Connection.DOWNLOAD_LINK_INFO, true, true);
            StringBuilder commentsBuf = new StringBuilder(4096);
            Collection<String> commentsArr = Regex.matches(commentsStr, Str.get(151), Str.get(152));
            int numComments = commentsArr.size();
            int numFakeComments = 0;
            String percentage = Str.get(160);

            if (numComments != 0) {
                int number = 0;
                for (String comment : commentsArr) {
                    commentsBuf.append(++number).append(". ").append(Regex.replaceAll(Regex.replaceAll(comment, Str.get(296), Str.get(297)), Str.get(298),
                            Str.get(299))).append(Constant.STD_NEWLINE).append(Constant.STD_NEWLINE);
                    if (!Regex.match(comment, Str.get(153)).isEmpty()) {
                        numFakeComments++;
                    }
                }
                percentage = (new DecimalFormat(Str.get(154))).format((numFakeComments / (double) numComments) * 100);
            }

            if (!isCancelled() && numComments != 0) {
                comments = commentsBuf.toString();
                guiListener.safetyDialogMsg(numFakeComments + Str.get(155) + numComments + Str.get(156) + percentage + Str.get(157), link, name);
            }
        } catch (Exception e) {
            if (!isCancelled()) {
                guiListener.commentsFinderError(e);
            }
        }
        guiListener.commentsFinderStopped();
        return null;
    }
}
