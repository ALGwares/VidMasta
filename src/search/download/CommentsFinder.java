package search.download;

import java.util.Collection;
import javax.swing.SwingWorker;
import listener.DomainType;
import listener.GuiListener;
import str.Str;
import util.Connection;
import util.Constant;
import util.Regex;

public class CommentsFinder extends SwingWorker<Object, Object> {

    private GuiListener guiListener;
    private String link, name;
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
            String commentsStr = Connection.getSourceCode(link, DomainType.DOWNLOAD_LINK_INFO, true, true);
            StringBuilder commentsBuf = new StringBuilder(4096);
            Collection<String> commentsArr = Regex.matches(commentsStr, 151);
            int numComments = commentsArr.size();
            int numFakeComments = 0;
            double fakeCommentsRatio = 0;

            if (numComments != 0) {
                int number = 0;
                for (String comment : commentsArr) {
                    commentsBuf.append(++number).append(". ").append(Regex.htmlToPlainText(Regex.replaceAllRepeatedly(comment, 672))).append(
                            Constant.STD_NEWLINE2);
                    if (!Regex.firstMatch(comment, 153).isEmpty()) {
                        numFakeComments++;
                    }
                }
                fakeCommentsRatio = numFakeComments / (double) numComments;
            }

            if (!isCancelled() && numComments != 0) {
                comments = commentsBuf.toString();
                guiListener.safetyDialogMsg(numFakeComments + "/" + numComments + " (" + Str.percent(fakeCommentsRatio, 1) + ')', link, name);
            }
        } catch (Exception e) {
            if (!isCancelled()) {
                guiListener.error(e);
            }
        }
        guiListener.commentsFinderStopped();
        return null;
    }
}
