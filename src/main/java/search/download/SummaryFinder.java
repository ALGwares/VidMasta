package search.download;

import debug.Debug;
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

public class SummaryFinder extends Worker {

  public static final Object LOCK = new Object();
  private GuiListener guiListener;
  private int row;
  private Video video;

  SummaryFinder(GuiListener guiListener, int row, Video video) {
    this.guiListener = guiListener;
    this.row = row;
    this.video = video;
  }

  @Override
  protected void doWork() {
    boolean updateSummary = true;
    String summary = "";
    try {
      summary = Regex.match(Connection.getSourceCode(String.format(Str.get(798), video.id), DomainType.VIDEO_INFO, false, video.isTVShow ? Constant.MS_2DAYS
              : Constant.MS_3DAYS), 799);
      summary = Regex.replaceAll(summary, 205).trim();
      summary = Regex.replaceAll(Regex.replaceAll(summary, 245), 247);
    } catch (ConnectionException e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      updateSummary = false;
      summary = Str.str("summaryConnectionProblem");
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      summary = Str.str("unknown");
    } finally {
      if (!isCancelled()) {
        synchronized (LOCK) {
          guiListener.removeSummaryElement(guiListener.getSummaryElement(Constant.STORYLINE_LOADING_HTML_ID));
          Element storyLineElement1 = guiListener.getSummaryElement(Constant.STORYLINE_LINK1_HTML_ID);
          if (storyLineElement1 == null) {
            if (!summary.isEmpty()) {
              String br1 = "\\<br\\>";
              String br2 = br1 + "\\s*+" + br1;
              if (Regex.htmlToPlainText(Regex.firstMatch(video.summary, VideoSearch.summaryTagRegex(Constant.GENRE_HTML_ID)).isEmpty() ? Regex.match(
                      video.summary, "\\<font[^\\>]++\\>", br2) : Regex.match(video.summary, br2, br2)).equalsIgnoreCase(Regex.htmlToPlainText(summary))) {
                summary = "";
              } else {
                guiListener.insertAfterSummaryElement(guiListener.getSummaryElement(Constant.STORYLINE_LINK2_HTML_ID), summary = System.getProperty("htmlFont3")
                        + "<b id=\"" + Constant.STORYLINE_HTML_ID + "\">" + Str.str("storyline") + "</b></font><br>" + summary);
              }
            }
          } else {
            guiListener.insertAfterSummaryElement(storyLineElement1, summary = summary.isEmpty() ? Str.get(159) : summary);
          }
          if (updateSummary) {
            summary = Regex.replaceFirst(Regex.replaceFirst(video.summary, "\\<img id\\=\"" + Constant.STORYLINE_LOADING_HTML_ID + "\"[^\\>]++\\>", ""),
                    "\\<span\\s++id\\s*+\\=\\s*+\"((" + Constant.STORYLINE_LINK1_HTML_ID + ")|(" + Constant.STORYLINE_LINK2_HTML_ID
                    + "))\"\\>[^\\<]++\\</span\\>", summary);
            guiListener.setSummary(summary, row, video.id);
            video.summary = summary;
          }
        }
      }
    }
  }
}
