package gui;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import debug.Debug;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.text.NumberFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultRowSorter;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTable.PrintMode;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.RootPaneContainer;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import listener.ContentType;
import listener.DomainType;
import listener.FormattedNum;
import listener.GuiListener;
import listener.PlaylistItem;
import listener.Video;
import listener.VideoStrExportListener;
import listener.WorkerListener;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.Regex;
import util.ThrowableUtil;
import util.WindowsUtil;
import util.Worker;

public class GUI extends JFrame implements GuiListener {

  private static final long serialVersionUID = 1L;
  private static final Preferences preferences = Preferences.userNodeForPackage(GUI.class);

  private WorkerListener workerListener;
  private boolean isRegularSearcher = true, cancelTVSelection, isAltSearch, isSubtitleMatch1, isTVShowSubtitle, forcePlay;
  private MenuElement popularSearchMenuElement;
  private final AtomicBoolean isPlaylistRestored = new AtomicBoolean(), playlistShown = new AtomicBoolean();
  private final Set<String> bannedTitles = new ConcurrentSkipListSet<String>();
  private final Set<Long> bannedDownloadIDs = new CopyOnWriteArraySet<Long>();
  private FileFilter torrentFileFilter, proxyListFileFilter, subtitleFileFilter;
  String proxyImportFile, proxyExportFile, torrentDir, subtitleDir, playlistDir;
  DefaultListModel blacklistListModel, whitelistListModel;
  private DefaultListModel removeProxiesListModel;
  private HTMLDocument summaryEditorPaneDocument;
  int imageCol, titleCol, yearCol, ratingCol, idCol, currTitleCol, oldTitleCol, summaryCol, imageLinkCol, isTVShowCol, isTVShowAndMovieCol,
          seasonCol, episodeCol;
  int playlistNameCol, playlistSizeCol, playlistProgressCol, playlistItemCol;
  String randomPort;
  private String subtitleFormat;
  private Video subtitleVideo;
  private VideoStrExportListener subtitleStrExportListener;
  private final Set<Integer> trailerEpisodes = new HashSet<Integer>(4), downloadLinkEpisodes = new HashSet<Integer>(4),
          subtitleEpisodes = new HashSet<Integer>(4);
  private Icon loadingIcon, notLoadingIcon, warningIcon, playIcon, stopIcon, banIcon, unbanIcon;
  JList popupList;
  JTextComponent popupTextComponent;
  SyncTable resultsSyncTable, playlistSyncTable;
  private AbstractPopupListener textComponentPopupListener;
  private final ActionListener htmlCopyListener = new HTMLCopyListener(), tableCopyListener = new TableCopyListener(),
          playlistTableCopyListener = new PlaylistTableCopyListener();
  private final Object msgDialogLock = new Object(), optionDialogLock = new Object();
  private final Lock playlistRestorationLock = new ReentrantLock();
  private final Settings settings = new Settings();
  private final Map<String, Icon> posters = new ConcurrentHashMap<String, Icon>(100);
  final BlockingQueue<String> posterImagePaths = new LinkedBlockingQueue<String>();
  private Thread posterCacher;
  private JTextComponent startDateTextComponent, endDateTextComponent;
  private TrayIcon trayIcon;
  boolean usePeerBlock;
  private volatile Worker timedMsgThread;
  final Object timedMsgLock = new Object();
  private final FindControl findControl, playlistFindControl;
  JDialog dummyDialog = new JDialog();
  JMenuItem dummyMenuItem = new JMenuItem(), peerBlockMenuItem, playDefaultAppMenuItem;
  JComboBox dummyComboBox = new JComboBox();
  ButtonGroup trailerPlayerButtonGroup2;

  public GUI(WorkerListener workerListener) throws Exception {
    this.workerListener = workerListener;

    initComponents();
    startDateTextComponent = (JTextComponent) startDateChooser.getDateEditor().getUiComponent();
    endDateTextComponent = (JTextComponent) endDateChooser.getDateEditor().getUiComponent();
    UI.addUndoRedoSupport(titleTextField, findTextField, playlistFindTextField, startDateTextComponent, endDateTextComponent, profileNameChangeTextField,
            addProxiesTextArea, customExtensionTextField, portTextField, authenticationUsernameTextField, authenticationPasswordField);
    updateToggleButtons(true);
    initFileNameExtensionFilters();

    JOptionPane tempOptionPane = new JOptionPane();
    Color fgColor = tempOptionPane.getForeground(), bgColor = tempOptionPane.getBackground();
    timedMsgLabel.setForeground(fgColor);
    timedMsgLabel.setBackground(bgColor);
    timedMsgLabel.setFont(tempOptionPane.getFont());

    resultsSyncTable = new SyncTable(resultsTable);
    JScrollBar resultsScrollBar = resultsScrollPane.getVerticalScrollBar();
    int increment = 30;
    resultsScrollBar.setUnitIncrement(increment);
    resultsScrollBar.setBlockIncrement(increment);
    playlistSyncTable = new SyncTable(playlistTable);
    JScrollBar playlistScrollBar = playlistScrollPane.getVerticalScrollBar();
    playlistScrollBar.setUnitIncrement(increment);
    playlistScrollBar.setBlockIncrement(increment);

    for (JCalendar calendar : new JCalendar[]{startDateChooser.getJCalendar(), endDateChooser.getJCalendar()}) {
      calendar.setTodayButtonVisible(true);
      calendar.setNullDateButtonVisible(true);
    }

    ActionListener enterKeyListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        searchButtonActionPerformed(evt);
      }
    };
    KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
    for (JComponent component : new JComponent[]{titleTextField, genreList, typeComboBox, ratingComboBox, startDateTextComponent, endDateTextComponent}) {
      component.registerKeyboardAction(enterKeyListener, "Enter", enterKey, JComponent.WHEN_FOCUSED);
    }

    statusBarTextField.setBackground(bgColor = getBackground());
    statusBarTextField.setForeground(fgColor = getForeground());
    statusBarTextField.setSelectionColor(bgColor);
    statusBarTextField.setSelectedTextColor(fgColor);

    UI.addPopupMenu(tablePopupMenu, resultsSyncTable, true);
    UI.addPopupMenu(playlistTablePopupMenu, playlistSyncTable, false);

    UI.addMouseListener(textComponentPopupListener = new AbstractPopupListener() {
      @Override
      protected void showPopup(MouseEvent evt) {
        if (!evt.isPopupTrigger() || !(popupTextComponent = (JTextComponent) evt.getSource()).isEnabled()) {
          return;
        }

        popupTextComponent.getCaret().setSelectionVisible(true);
        if (popupTextComponent instanceof JTextFieldDateEditor) {
          popupTextComponent.removeFocusListener((FocusListener) popupTextComponent);
        }
        show(textComponentPopupMenu, evt);
      }
    }, titleTextField, findTextField, playlistFindTextField, addProxiesTextArea, profileNameChangeTextField, customExtensionTextField, portTextField,
            commentsTextPane, msgEditorPane, faqEditorPane, aboutEditorPane, summaryEditorPane, authenticationUsernameTextField, authenticationPasswordField,
            startDateTextComponent, endDateTextComponent);

    UI.addMouseListener(new AbstractPopupListener() {
      @Override
      protected void showPopup(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
          popupList = (JList) evt.getSource();
          show(listPopupMenu, evt);
        }
      }
    }, removeProxiesList, whitelistedList, blacklistedList);

    UI.addMouseListener(new AbstractPopupListener() {
      @Override
      protected void showPopup(MouseEvent evt) {
        if (evt.isPopupTrigger() && connectionIssueButton.isEnabled()) {
          show(connectionIssueButtonPopupMenu, evt);
        }
      }
    }, connectionIssueButton);

    summaryEditorPaneDocument = (HTMLDocument) summaryEditorPane.getDocument();

    TableColumnModel colModel = resultsTable.getColumnModel();
    imageCol = colModel.getColumnIndex(Constant.IMAGE_COL);
    titleCol = colModel.getColumnIndex(Str.str("GUI.resultsTable.columnModel.title1"));
    yearCol = colModel.getColumnIndex(Str.str("GUI.resultsTable.columnModel.title2"));
    ratingCol = colModel.getColumnIndex(Str.str("GUI.resultsTable.columnModel.title3"));
    idCol = colModel.getColumnIndex(Constant.ID_COL);
    currTitleCol = colModel.getColumnIndex(Constant.CURR_TITLE_COL);
    oldTitleCol = colModel.getColumnIndex(Constant.OLD_TITLE_COL);
    summaryCol = colModel.getColumnIndex(Constant.SUMMARY_COL);
    imageLinkCol = colModel.getColumnIndex(Constant.IMAGE_LINK_COL);
    isTVShowCol = colModel.getColumnIndex(Constant.IS_TV_SHOW_COL);
    isTVShowAndMovieCol = colModel.getColumnIndex(Constant.IS_TV_SHOW_AND_MOVIE_COL);
    seasonCol = colModel.getColumnIndex(Constant.SEASON_COL);
    episodeCol = colModel.getColumnIndex(Constant.EPISODE_COL);
    // Must remove rightmost column first
    for (String col : new String[]{Constant.EPISODE_COL, Constant.SEASON_COL, Constant.IS_TV_SHOW_AND_MOVIE_COL, Constant.IS_TV_SHOW_COL,
      Constant.IMAGE_LINK_COL, Constant.SUMMARY_COL, Constant.OLD_TITLE_COL, Constant.CURR_TITLE_COL, Constant.ID_COL}) {
      resultsTable.removeColumn(resultsTable.getColumn(col));
    }

    TableColumnModel playlistColModel = playlistTable.getColumnModel();
    playlistNameCol = playlistColModel.getColumnIndex(Str.str("GUI.playlistTable.columnModel.title0"));
    playlistSizeCol = playlistColModel.getColumnIndex(Str.str("GUI.playlistTable.columnModel.title1"));
    playlistProgressCol = playlistColModel.getColumnIndex(Str.str("GUI.playlistTable.columnModel.title2"));
    playlistItemCol = playlistColModel.getColumnIndex(Constant.PLAYLIST_ITEM_COL);
    playlistTable.removeColumn(playlistTable.getColumn(Constant.PLAYLIST_ITEM_COL));

    colModel.getColumn(imageCol).setCellRenderer(new DefaultTableCellRenderer() {
      private static final long serialVersionUID = 1L;
      private final JLabel label = new JLabel();

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String imagePath = (String) value;
        if (imagePath == null || imagePath.startsWith(Constant.NO_IMAGE)) {
          imagePath = Constant.PROGRAM_DIR + "noPoster.jpg";
        }
        label.setIcon(getPoster(imagePath));
        return label;
      }
    });

    colModel.getColumn(ratingCol).setCellRenderer(new DefaultTableCellRenderer() {
      private static final long serialVersionUID = 1L;
      private Map<Long, Entry<Long, String>> numRatingsCache;

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JComponent component = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (numRatingsCache == null) {
          numRatingsCache = new HashMap<>(100);
          table.addMouseMotionListener(new MouseMotionAdapter() {
            private Object prevId;

            @Override
            public void mouseMoved(MouseEvent evt) {
              int currRow = table.rowAtPoint(evt.getPoint()), currModelRow;
              Object currId;
              if (currRow != -1 && !(currId = table.getModel().getValueAt(currModelRow = table.convertRowIndexToModel(currRow), idCol)).equals(prevId)) {
                prevId = currId;
                String numRatings;
                File sourceCode;
                Long sourceCodeId;
                if (Constant.NO_RATING.equals(table.getModel().getValueAt(currModelRow, ratingCol))) {
                  numRatings = Str.str("total", 0);
                } else if ((sourceCode = new File(Constant.CACHE_DIR + Str.hashPath(sourceCodeId = Str.hashCode(String.format(Str.get(781), currId)))
                        + Constant.HTML)).exists()) {
                  String tempNumRatings = "";
                  try {
                    Entry<Long, String> numRatingsCacheVal = numRatingsCache.get(sourceCodeId);
                    if (numRatingsCacheVal == null || sourceCode.lastModified() != numRatingsCacheVal.getKey()) {
                      numRatingsCache.put(sourceCodeId, numRatingsCacheVal = new SimpleImmutableEntry<>(sourceCode.lastModified(), Regex.match(Regex.firstMatch(
                              IO.read(sourceCode), 791), 792)));
                    }
                    tempNumRatings = numRatingsCacheVal.getValue();
                  } catch (Exception e) {
                    if (Debug.DEBUG) {
                      Debug.print(e);
                    }
                  }
                  numRatings = (tempNumRatings.isEmpty() ? Str.str("total2") : Str.str("total", tempNumRatings));
                } else {
                  numRatings = Str.str("total3");
                  prevId = null;
                }
                component.setToolTipText(numRatings);
              }
            }
          });
        }
        return component;
      }
    });

    playlistColModel.getColumn(playlistProgressCol).setCellRenderer(new DefaultTableCellRenderer() {
      private static final long serialVersionUID = 1L;
      JProgressBar progressBar;

      {
        (progressBar = new JProgressBar()).setStringPainted(true);
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        FormattedNum val = (FormattedNum) value;
        progressBar.setValue((int) (val.val().doubleValue() * 100));
        progressBar.setString(val.toString());
        return progressBar;
      }
    });

    resultsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent evt) {
        resultsTableValueChanged(evt);
      }
    });

    playlistTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent evt) {
        playlistTableValueChanged(evt);
      }
    });

    blacklistedList.setModel(blacklistListModel = new DefaultListModel());
    whitelistedList.setModel(whitelistListModel = new DefaultListModel());
    removeProxiesList.setModel(removeProxiesListModel = new DefaultListModel());

    UI.registerCutCopyPasteKeyboardActions(summaryEditorPane, htmlCopyListener);
    UI.registerCutCopyPasteKeyboardActions(resultsTable, tableCopyListener);
    UI.registerCutCopyPasteKeyboardActions(playlistTable, playlistTableCopyListener);

    DefaultRowSorter<TableModel, Integer> rowSorter = UI.setRowSorter(resultsSyncTable, yearCol, ratingCol);
    rowSorter.setComparator(titleCol, new AbstractColumnComparator<String>() {
      @Override
      protected String convert(String title) {
        return Regex.htmlToPlainText(UI.innerHTML(title, Constant.TITLE_INDENT_LEN)).toLowerCase(Locale.ENGLISH);
      }
    });
    rowSorter.setComparator(yearCol, new AbstractColumnComparator<Short>() {
      @Override
      protected Short convert(String year) {
        return Short.valueOf(UI.innerHTML(year));
      }
    });
    rowSorter.setComparator(ratingCol, new AbstractColumnComparator<Float>() {
      @Override
      protected Float convert(String rating) throws Exception {
        String tempRating = UI.innerHTML(rating);
        return tempRating.equals(Constant.NO_RATING) ? 0.0f : Str.getNumFormat().parse(tempRating).floatValue();
      }
    });

    DefaultRowSorter<TableModel, Integer> playlistRowSorter = UI.setRowSorter(playlistSyncTable, playlistSizeCol, playlistProgressCol);
    playlistRowSorter.setComparator(playlistNameCol, new Comparator<String>() {
      @Override
      public int compare(String name1, String name2) {
        return name1.compareToIgnoreCase(name2);
      }
    });
    playlistRowSorter.setComparator(playlistSizeCol, new Comparator<FormattedNum>() {
      @Override
      public int compare(FormattedNum size1, FormattedNum size2) {
        return Long.valueOf(size1.val().longValue()).compareTo(size2.val().longValue());
      }
    });
    playlistRowSorter.setComparator(playlistProgressCol, new Comparator<FormattedNum>() {
      @Override
      public int compare(FormattedNum progress1, FormattedNum progress2) {
        return Double.valueOf(progress1.val().doubleValue()).compareTo(progress2.val().doubleValue());
      }
    });

    (findControl = new FindControl(findTextField)).addDataSource(resultsSyncTable.tableModel, currTitleCol);
    (playlistFindControl = new FindControl(playlistFindTextField)).addDataSource(playlistSyncTable.tableModel, playlistNameCol);

    UI.add(trailerPlayerButtonGroup, trailerMediaPlayerRadioButtonMenuItem, trailerMediaPlayer1080RadioButtonMenuItem,
            trailerMediaPlayer720RadioButtonMenuItem, trailerMediaPlayer480RadioButtonMenuItem, trailerMediaPlayer360RadioButtonMenuItem,
            trailerMediaPlayer240RadioButtonMenuItem, trailerWebBrowserPlayerRadioButtonMenuItem);
    UI.add(downloadQualityButtonGroup, downloadAnyQualityRadioButtonMenuItem, downloadHighQualityRadioButtonMenuItem, downloadDVDQualityRadioButtonMenuItem,
            download720HDQualityRadioButtonMenuItem, download1080HDRadioButtonMenuItem);
    UI.add(downloaderButtonGroup, playlistDownloaderRadioButtonMenuItem, playlistDownloaderRadioButtonMenuItem /* Backward compatibility */,
            playlistDownloaderRadioButtonMenuItem /* Backward compatibility */, defaultApplicationDownloaderRadioButtonMenuItem,
            noDownloaderRadioButtonMenuItem);

    UI.setIcon(popularPopupMenuButton, "more");
    loadingIcon = UI.icon("loading.gif");
    notLoadingIcon = UI.icon("notLoading.gif");
    for (JLabel label : new JLabel[]{loadingLabel, proxyLoadingLabel, tvSubtitleLoadingLabel, movieSubtitleLoadingLabel}) {
      label.setIcon(notLoadingIcon);
    }
    warningIcon = UI.icon("warning.png");
    UI.setIcon(trashCanButton, "trashCan");
    playIcon = UI.icon("play.png");
    stopIcon = UI.icon("stop.png");
    banIcon = UI.icon("ban.png");
    unbanIcon = UI.icon("unban.png");
    UI.setIcon(playlistOpenButton, "open");
    UI.setIcon(playlistReloadGroupButton, "reload");
    play(null);
    UI.setIcon(playlistMoveUpButton, "up");
    UI.setIcon(playlistMoveDownButton, "down");
    UI.setIcon(playlistRemoveButton, "remove");
    UI.setIcon(whitelistedToBlacklistedButton, "rightArrow");
    UI.setIcon(blacklistedToWhitelistedButton, "leftArrow");

    File proxies = new File(Constant.APP_DIR + Constant.PROXIES);
    if (proxies.exists()) {
      for (String proxy : Regex.split(IO.read(proxies), Constant.NEWLINE)) {
        String newProxy = proxy.trim();
        if (!newProxy.isEmpty()) {
          proxyComboBox.addItem(newProxy);
        }
      }
    }

    for (int i = 0; i < 10; i++) {
      String profile = "GUI.profile" + i + "MenuItem.text";
      profileComboBox.addItem(i == 0 ? Str.str(profile) : preferences.get(profile, Str.str(profile)));
      updateProfileGUIitems(i);
    }
    profileComboBox.setSelectedIndex(0);

    faqEditorPane.setText(Regex.replaceFirst(IO.read(Constant.PROGRAM_DIR + "FAQ" + Constant.HTML), "<br><br><br>", Str.get(555) + "<br><br><br>"));

    UI.addAutoCompleteSupport(titleTextField, Arrays.asList(Regex.split(IO.read(Constant.PROGRAM_DIR + "autoCompleteTitles" + Constant.TXT),
            Constant.NEWLINE)));

    UI.initCountComboBoxes(414, 502, regularResultsPerSearchComboBox);
    UI.initCountComboBoxes(412, 413, popularMoviesResultsPerSearchComboBox, popularTVShowsResultsPerSearchComboBox);

    AbstractButton[] languageButtons = languageButtons();
    UI.add(languageButtonGroup, languageButtons);
    for (final AbstractButton languageButton : languageButtons) {
      languageButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
          changeLocale(languageButton.getName());
        }
      });
    }

    String escapeKeyWindowClosingActionMapKey = "VK_ESCAPE:WINDOW_CLOSING", enterKeyWindowClosingActionMapKey = "VK_ENTER:WINDOW_CLOSING";
    KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
    Image icon = Toolkit.getDefaultToolkit().getImage(Constant.PROGRAM_DIR + "icon16x16.png");
    timedMsgDialog.setIconImage(icon);

    for (final Window window : windows()) {
      window.setIconImage(icon);
      if (window == this) {
        continue;
      }
      Action windowClosingAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        }
      };
      JComponent root = ((RootPaneContainer) window).getRootPane();
      InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      ActionMap actionMap = root.getActionMap();
      inputMap.put(escapeKey, escapeKeyWindowClosingActionMapKey);
      actionMap.put(escapeKeyWindowClosingActionMapKey, windowClosingAction);
      inputMap.put(enterKey, enterKeyWindowClosingActionMapKey);
      actionMap.put(enterKeyWindowClosingActionMapKey, windowClosingAction);
    }

    try {
      trayIcon = UI.addMinimizeToTraySupport(this, iconifyMenuItem);
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }

    if (Constant.WINDOWS_XP_AND_HIGHER) {
      peerBlockMenuItem = peerBlockNotificationCheckBoxMenuItem;
      playDefaultAppMenuItem = playlistPlayWithDefaultAppCheckBoxMenuItem;
      trailerPlayerButtonGroup2 = trailerPlayerButtonGroup;
    } else {
      peerBlockMenuItem = new JMenuItem();
      peerBlockNotificationCheckBoxMenuItem.setSelected(false);
      usePeerBlock = false;
      playDefaultAppMenuItem = new JMenuItem();
      playlistPlayWithDefaultAppCheckBoxMenuItem.setSelected(true);
      trailerPlayerButtonGroup2 = new ButtonGroup();
      UI.select(trailerPlayerButtonGroup, 6);
      for (JComponent component : new JComponent[]{peerBlockNotificationCheckBoxMenuItem, playlistPlayWithDefaultAppCheckBoxMenuItem, searchMenuSeparator8,
        trailerPlayerMenu}) {
        component.setEnabled(false);
        component.setVisible(false);
      }
    }
    settings.loadSettings(Constant.APP_DIR + Constant.USER_SETTINGS);
    playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(null);

    File idsFile = new File(Constant.APP_DIR, Constant.BANNED_TITLES);
    String ids;
    if (idsFile.exists() && !(ids = IO.read(idsFile)).isEmpty()) {
      Collections.addAll(bannedTitles, ids.split(Constant.NEWLINE));
    }
    if (!(ids = preferences.get("bannedDownloadIDs", "").trim()).isEmpty()) { // Backward compatibility
      for (String id : ids.split(Constant.STD_NEWLINE)) {
        bannedDownloadIDs.add(Long.valueOf(id));
      }
      preferences.remove("bannedDownloadIDs");
    }
    if ((idsFile = new File(Constant.APP_DIR, Constant.BANNED_DOWNLOAD_IDS)).exists() && !(ids = IO.read(idsFile)).isEmpty()) {
      for (String id : ids.split(Constant.NEWLINE)) {
        bannedDownloadIDs.add(Long.valueOf(id));
      }
    }
  }

  private void updateToggleButtons(boolean init) {
    UI.updateToggleButton(searchButton, "GUI.searchButton.text", init);
    UI.updateToggleButton(readSummaryButton, "GUI.readSummaryButton.text", init);
    UI.updateToggleButton(watchTrailerButton, "GUI.watchTrailerButton.text", init);
    UI.updateToggleButton(downloadLink1Button, "GUI.downloadLink1Button.text", init);
    UI.updateToggleButton(downloadLink2Button, "GUI.downloadLink2Button.text", init);
    UI.updateToggleButton(movieSubtitleDownloadMatch1Button, "GUI.movieSubtitleDownloadMatch1Button.text", init);
    UI.updateToggleButton(movieSubtitleDownloadMatch2Button, "GUI.movieSubtitleDownloadMatch2Button.text", init);
    UI.updateToggleButton(tvSubtitleDownloadMatch1Button, "GUI.tvSubtitleDownloadMatch1Button.text", init);
    UI.updateToggleButton(tvSubtitleDownloadMatch2Button, "GUI.tvSubtitleDownloadMatch2Button.text", init);
  }

  public void resizeContent() {
    String stop = Str.str(Constant.STOP_KEY), readSummary = readSummaryButton.getName(), watchTrailer = watchTrailerButton.getName(), downloadLink1
            = downloadLink1Button.getName(), downloadLink2 = downloadLink2Button.getName();
    UI.resize(AbstractComponent.newInstance(searchButton), stop, searchButton.getName());
    UI.resize(AbstractComponent.newInstance(readSummaryButton), watchTrailer, downloadLink1, downloadLink2, stop, readSummary);
    UI.resize(AbstractComponent.newInstance(watchTrailerButton), readSummary, downloadLink1, downloadLink2, stop, watchTrailer);
    UI.resize(AbstractComponent.newInstance(downloadLink1Button), readSummary, watchTrailer, downloadLink2, stop, downloadLink1);
    UI.resize(AbstractComponent.newInstance(downloadLink2Button), stop, downloadLink2);
    UI.resize(AbstractComponent.newInstance(searchProgressTextField), ' ' + Str.str("results", 11111, Str.percent(1, 0)) + ' ',
            searchProgressTextField.getText());
    resizeExitBackupModeButton();
    for (AbstractButton button : new AbstractButton[]{movieSubtitleDownloadMatch1Button, movieSubtitleDownloadMatch2Button, tvSubtitleDownloadMatch1Button,
      tvSubtitleDownloadMatch2Button}) {
      UI.resize(AbstractComponent.newInstance(button), stop, button.getName());
    }
  }

  private void resizeExitBackupModeButton() {
    String text = exitBackupModeButton.getText();
    if (text == null || text.isEmpty()) {
      exitBackupModeButton.setMinimumSize(new Dimension(0, 0));
      exitBackupModeButton.setMaximumSize(new Dimension(0, 0));
    } else {
      exitBackupModeButton.setMinimumSize(null);
      exitBackupModeButton.setMaximumSize(null);
    }
  }

  public void setInitialFocus() {
    titleTextField.requestFocusInWindow();
  }

  public void startPosterCacher() {
    posterCacher = new Thread() {
      @Override
      public void run() {
        try {
          while (true) {
            getPoster(posterImagePaths.take());
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.println("poster cacher stopped: " + e);
          }
        }
      }
    };
    posterCacher.setPriority(Thread.MIN_PRIORITY);
    posterCacher.start();
  }

  public void stopPosterCacher() {
    if (posterCacher != null) {
      posterCacher.interrupt();
    }
  }

  Icon getPoster(String imagePath) {
    return posters.computeIfAbsent(imagePath, imgPath -> new ImageIcon((new ImageIcon(imgPath)).getImage().getScaledInstance(60, -1, Image.SCALE_SMOOTH)));
  }

  private void initFileNameExtensionFilters() {
    torrentFileFilter = new FileNameExtensionFilter(Str.str("torrents") + " (*.torrent)", "torrent");
    proxyListFileFilter = new FileNameExtensionFilter(Str.str("proxyList") + " (*" + Constant.TXT + ")", "txt");
    subtitleFileFilter = new FileNameExtensionFilter(Str.str("subtitle2") + " (" + Str.get(451) + ")", Regex.split(452, ","));
  }

  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    faqFrame = new JFrame() {
      private static final long serialVersionUID = 1L;

      @Override
      public void setMaximizedBounds(Rectangle bounds) {
        super.setMaximizedBounds(bounds == null ? null : UI.getUsableScreenBounds(faqFrame));
      }
    };
    faqScrollPane = new JScrollPane();
    faqEditorPane = new JEditorPane();
    aboutDialog = new JDialog();
    aboutScrollPane = new JScrollPane();
    aboutEditorPane = new JEditorPane();
    timeoutDialog = new JDialog();
    timeoutLabel = new JLabel();
    timeoutComboBox = new JComboBox();
    timeoutButton = new JButton();
    tvDialog = new JDialog();
    tvSeasonComboBox = new JComboBox();
    tvEpisodeComboBox = new JComboBox();
    tvSubmitButton = new JButton();
    tvSelectionLabel = new JLabel();
    seasonLabel = new JLabel();
    episodeLabel = new JLabel();
    tvCancelButton = new JButton();
    resultsPerSearchDialog = new JDialog();
    regularResultsPerSearchLabel = new JLabel();
    regularResultsPerSearchComboBox = new JComboBox();
    resultsPerSearchButton = new JButton();
    popularMoviesResultsPerSearchLabel = new JLabel();
    popularMoviesResultsPerSearchComboBox = new JComboBox();
    popularTVShowsResultsPerSearchLabel = new JLabel();
    popularTVShowsResultsPerSearchComboBox = new JComboBox();
    downloadSizeDialog = new JDialog();
    downloadSizeLabel = new JLabel();
    maxDownloadSizeComboBox = new JComboBox();
    downloadSizeToLabel = new JLabel();
    minDownloadSizeComboBox = new JComboBox();
    downloadSizeButton = new JButton();
    downloadSizeIgnoreCheckBox = new JCheckBox();
    extensionsDialog = new JDialog();
    blacklistedScrollPane = new JScrollPane();
    blacklistedList = new JList();
    blacklistedLabel = new JLabel();
    whitelistedScrollPane = new JScrollPane();
    whitelistedList = new JList();
    whitelistedToBlacklistedButton = new JButton();
    blacklistedToWhitelistedButton = new JButton();
    whitelistLabel = new JLabel();
    fileExtensionsLabel = new JLabel();
    extensionsButton = new JButton();
    customExtensionTextField = new JTextField();
    trashCanButton = new JButton();
    languageCountryDialog = new JDialog();
    countryLabel = new JLabel();
    languageLabel = new JLabel();
    languageCountryOkButton = new JButton();
    languageScrollPane = new JScrollPane();
    languageList = new JList();
    countryScrollPane = new JScrollPane();
    countryList = new JList();
    languageCountryWarningTextArea = new JTextArea();
    tablePopupMenu = new JPopupMenu();
    readSummaryMenuItem = new JMenuItem();
    hearSummaryMenuItem = new JMenuItem();
    watchTrailerMenuItem = new JMenuItem();
    downloadLink1MenuItem = new JMenuItem();
    downloadLink2MenuItem = new JMenuItem();
    tablePopupMenuSeparator1 = new Separator();
    copyMenu = new JMenu();
    copySelectionMenuItem = new JMenuItem();
    copyFullTitleAndYearMenuItem = new JMenuItem();
    copyPosterImageMenuItem = new JMenuItem();
    copyMenuSeparator1 = new Separator();
    copyDownloadLink1MenuItem = new JMenuItem();
    copyDownloadLink2MenuItem = new JMenuItem();
    copySummaryLinkMenuItem = new JMenuItem();
    copyTrailerLinkMenuItem = new JMenuItem();
    copySubtitleLinkMenuItem = new JMenuItem();
    emailMenu = new JMenu();
    emailDownloadLink1MenuItem = new JMenuItem();
    emailDownloadLink2MenuItem = new JMenuItem();
    emailSummaryLinkMenuItem = new JMenuItem();
    emailTrailerLinkMenuItem = new JMenuItem();
    emailMenuSeparator1 = new Separator();
    emailEverythingMenuItem = new JMenuItem();
    tablePopupMenuSeparator2 = new Separator();
    watchOnDeviceMenuItem = new JMenuItem();
    tablePopupMenuSeparator3 = new Separator();
    findSubtitleMenuItem = new JMenuItem();
    tablePopupMenuSeparator4 = new Separator();
    banTitleMenu = new JMenu();
    popularPopupMenu = new JPopupMenu();
    popularMoviesMenuItem = new JMenuItem();
    popularNewHQMoviesMenuItem = new JMenuItem();
    popularTVShowsMenuItem = new JMenuItem();
    popularNewHQTVShowsMenuItem = new JMenuItem();
    textComponentPopupMenu = new JPopupMenu();
    textComponentCutMenuItem = new JMenuItem();
    textComponentCopyMenuItem = new JMenuItem();
    textComponentPasteMenuItem = new JMenuItem();
    textComponentPasteSearchMenuItem = new JMenuItem();
    textComponentDeleteMenuItem = new JMenuItem();
    textComponentPopupMenuSeparator1 = new Separator();
    textComponentSelectAllMenuItem = new JMenuItem();
    proxyFileChooser = new JFileChooser();
    playlistFileChooser = new JFileChooser();
    proxyDialog = new JDialog();
    proxyAddButton = new JButton();
    proxyRemoveButton = new JButton();
    proxyComboBox = new JComboBox();
    proxyDownloadLinkInfoCheckBox = new JCheckBox();
    proxyUseForLabel = new JLabel();
    proxyVideoInfoCheckBox = new JCheckBox();
    proxySearchEnginesCheckBox = new JCheckBox();
    proxyOKButton = new JButton();
    proxyDownloadButton = new JButton();
    proxyTrailersCheckBox = new JCheckBox();
    proxyLoadingLabel = new JLabel();
    proxyImportButton = new JButton();
    proxyExportButton = new JButton();
    proxyUpdatesCheckBox = new JCheckBox();
    proxySubtitlesCheckBox = new JCheckBox();
    addProxiesDialog = new JDialog();
    addProxiesLabel = new JLabel();
    addProxiesScrollPane = new JScrollPane();
    addProxiesTextArea = new JTextArea();
    addProxiesCancelButton = new JButton();
    addProxiesAddButton = new JButton();
    removeProxiesDialog = new JDialog();
    removeProxiesLabel = new JLabel();
    removeProxiesScrollPane = new JScrollPane();
    removeProxiesList = new JList();
    removeProxiesRemoveButton = new JButton();
    removeProxiesCancelButton = new JButton();
    msgDialog = new JDialog();
    msgOKButton = new JButton();
    msgScrollPane = new JScrollPane();
    msgEditorPane = new JEditorPane();
    profileDialog = new JDialog();
    profileSetButton = new JButton();
    profileClearButton = new JButton();
    profileUseButton = new JButton();
    profileComboBox = new JComboBox();
    profileRenameButton = new JButton();
    profileOKButton = new JButton();
    profileNameChangeDialog = new JDialog();
    profileNameChangeTextField = new JTextField();
    profileNameChangeLabel = new JLabel();
    profileNameChangeOKButton = new JButton();
    profileNameChangeCancelButton = new JButton();
    commentsDialog = new JDialog();
    commentsScrollPane = new JScrollPane();
    commentsTextPane = new JTextPane();
    downloaderButtonGroup = new ButtonGroup();
    portDialog = new JDialog();
    portLabel = new JLabel();
    portTextField = new JTextField();
    portRandomizeCheckBox = new JCheckBox();
    portOkButton = new JButton();
    tvSubtitleDialog = new JDialog();
    tvSubtitleLanguageLabel = new JLabel();
    tvSubtitleLanguageComboBox = new JComboBox();
    tvSubtitleFormatLabel = new JLabel();
    tvSubtitleFormatComboBox = new JComboBox();
    tvSubtitleSeasonLabel = new JLabel();
    tvSubtitleSeasonComboBox = new JComboBox();
    tvSubtitleEpisodeLabel = new JLabel();
    tvSubtitleEpisodeComboBox = new JComboBox();
    tvSubtitleDownloadMatch1Button = new JButton();
    tvSubtitleDownloadMatch2Button = new JButton();
    tvSubtitleLoadingLabel = new JLabel();
    movieSubtitleDialog = new JDialog();
    movieSubtitleLanguageLabel = new JLabel();
    movieSubtitleLanguageComboBox = new JComboBox();
    movieSubtitleFormatLabel = new JLabel();
    movieSubtitleFormatComboBox = new JComboBox();
    movieSubtitleDownloadMatch1Button = new JButton();
    movieSubtitleDownloadMatch2Button = new JButton();
    movieSubtitleLoadingLabel = new JLabel();
    timedMsgDialog = new JDialog();
    timedMsgLabel = new JLabel();
    torrentFileChooser = new JFileChooser();
    subtitleFileChooser = new JFileChooser();
    authenticationPanel = new JPanel();
    authenticationMessageLabel = new JLabel();
    authenticationUsernameLabel = new JLabel();
    authenticationUsernameTextField = new JTextField();
    authenticationPasswordLabel = new JLabel();
    authenticationPasswordField = new JPasswordField();
    listPopupMenu = new JPopupMenu();
    listCutMenuItem = new JMenuItem();
    listCopyMenuItem = new JMenuItem();
    listDeleteMenuItem = new JMenuItem();
    listPopupMenuSeparator1 = new Separator();
    listSelectAllMenuItem = new JMenuItem();
    connectionIssueButtonPopupMenu = new JPopupMenu();
    hideMenuItem = new JMenuItem();
    playlistTablePopupMenu = new JPopupMenu();
    playlistPlayMenuItem = new JMenuItem();
    playlistOpenMenuItem = new JMenuItem();
    playlistMoveUpMenuItem = new JMenuItem();
    playlistMoveDownMenuItem = new JMenuItem();
    playlistTablePopupMenuSeparator1 = new Separator();
    playlistCopyMenu = new JMenu();
    playlistCopySelectionMenuItem = new JMenuItem();
    playlistCopySeparator = new Separator();
    playlistCopyDownloadLinkMenuItem = new JMenuItem();
    playlistTablePopupMenuSeparator2 = new Separator();
    playlistRemoveMenuItem = new JMenuItem();
    playlistReloadGroupMenuItem = new JMenuItem();
    playlistTablePopupMenuSeparator3 = new Separator();
    playlistBanGroupMenuItem = new JMenuItem();
    languageButtonGroup = new ButtonGroup();
    trailerPlayerButtonGroup = new ButtonGroup();
    downloadQualityButtonGroup = new ButtonGroup();
    resultsPanel = new JPanel();
    resultsScrollPane = new JScrollPane();
    resultsTable = new JTable();
    readSummaryButton = new JButton();
    watchTrailerButton = new JButton();
    downloadLink1Button = new JButton();
    downloadLink2Button = new JButton();
    exitBackupModeButton = new JButton();
    loadMoreResultsButton = new JButton();
    summaryScrollPane = new JScrollPane();
    summaryEditorPane = new JEditorPane();
    findTextField = new JTextField();
    playlistPanel = new JPanel();
    playlistScrollPane = new JScrollPane();
    playlistTable = new JTable();
    playlistPlayButton = new JButton();
    playlistFindTextField = new JTextField();
    playlistOpenButton = new JButton();
    playlistMoveUpButton = new JButton();
    playlistMoveDownButton = new JButton();
    playlistRemoveButton = new JButton();
    playlistReloadGroupButton = new JButton();
    playlistBanGroupButton = new JButton();
    autoConfirmCheckBoxMenuItem = new JCheckBoxMenuItem();
    titleTextField = new JTextField();
    titleLabel = new JLabel();
    releasedLabel = new JLabel();
    genreLabel = new JLabel();
    ratingComboBox = new JComboBox();
    ratingLabel = new JLabel();
    searchButton = new JButton();
    genreScrollPane = new JScrollPane();
    genreList = new JList();
    typeLabel = new JLabel();
    typeComboBox = new JComboBox();
    releasedToLabel = new JLabel();
    popularPopupMenuButton = new JButton();
    loadingLabel = new JLabel();
    statusBarTextField = new JTextField();
    searchProgressTextField = new JTextField();
    connectionIssueButton = new JButton();
    startDateChooser = new DateChooser(true);
    endDateChooser = new DateChooser(false);
    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultsPanel, playlistPanel);
    menuBar = new JMenuBar();
    fileMenu = new JMenu();
    profileMenu = new JMenu();
    profile0MenuItem = new JMenuItem();
    profile1MenuItem = new JMenuItem();
    profile2MenuItem = new JMenuItem();
    profile3MenuItem = new JMenuItem();
    profile4MenuItem = new JMenuItem();
    profile5MenuItem = new JMenuItem();
    profile6MenuItem = new JMenuItem();
    profile7MenuItem = new JMenuItem();
    profile8MenuItem = new JMenuItem();
    profile9MenuItem = new JMenuItem();
    profileMenuSeparator1 = new Separator();
    editProfilesMenuItem = new JMenuItem();
    fileMenuSeparator1 = new Separator();
    printMenuItem = new JMenuItem();
    fileMenuSeparator2 = new Separator();
    iconifyMenuItem = new JMenuItem();
    exitMenuItem = new JMenuItem();
    editMenu = new JMenu();
    cutMenuItem = new JMenuItem();
    copyMenuItem = new JMenuItem();
    pasteMenuItem = new JMenuItem();
    deleteMenuItem = new JMenuItem();
    editMenuSeparator1 = new Separator();
    selectAllMenuItem = new JMenuItem();
    editMenuSeparator2 = new Separator();
    findMenuItem = new JMenuItem();
    viewMenu = new JMenu();
    languageMenu = new JMenu();
    englishRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    spanishRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    frenchRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    italianRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    dutchRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    portugueseRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    turkishRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    viewMenuSeparator1 = new Separator();
    resetWindowMenuItem = new JMenuItem();
    searchMenu = new JMenu();
    searchBanTitleMenu = new JMenu();
    searchBanTitleEnableCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    searchMenuSeparator1 = new Separator();
    resultsPerSearchMenuItem = new JMenuItem();
    searchMenuSeparator2 = new Separator();
    timeoutMenuItem = new JMenuItem();
    searchMenuSeparator3 = new Separator();
    proxyMenuItem = new JMenuItem();
    searchMenuSeparator4 = new Separator();
    languageCountryMenuItem = new JMenuItem();
    searchMenuSeparator5 = new Separator();
    feedCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    searchMenuSeparator6 = new Separator();
    browserNotificationCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    searchMenuSeparator7 = new Separator();
    emailWithDefaultAppCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    searchMenuSeparator8 = new Separator();
    trailerPlayerMenu = new JMenu();
    trailerMediaPlayerRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    trailerMediaPlayer1080RadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    trailerMediaPlayer720RadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    trailerMediaPlayer480RadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    trailerMediaPlayer360RadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    trailerMediaPlayer240RadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    trailerWebBrowserPlayerRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    playlistMenu = new JMenu();
    playlistMenuItem = new JMenuItem();
    playlistMenuSeparator1 = new Separator();
    playlistSaveFolderMenuItem = new JMenuItem();
    playlistAutoOpenCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    playlistPlayWithDefaultAppCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    playlistShowNonVideoItemsCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    downloadMenu = new JMenu();
    downloadQualityMenu = new JMenu();
    downloadAnyQualityRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    downloadHighQualityRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    downloadDVDQualityRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    download720HDQualityRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    download1080HDRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    downloadMenuSeparator1 = new Separator();
    downloadSizeMenuItem = new JMenuItem();
    downloadMenuSeparator2 = new Separator();
    fileExtensionsMenuItem = new JMenuItem();
    downloadMenuSeparator3 = new Separator();
    safetyCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    peerBlockNotificationCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    downloadMenuSeparator4 = new Separator();
    portMenuItem = new JMenuItem();
    downloadMenuSeparator5 = new Separator();
    downloaderMenu = new JMenu();
    playlistDownloaderRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    defaultApplicationDownloaderRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    noDownloaderRadioButtonMenuItem = UI.newJRadioButtonMenuItem();
    helpMenu = new JMenu();
    faqMenuItem = new JMenuItem();
    helpMenuSeparator1 = new Separator();
    updateMenuItem = new JMenuItem();
    updateCheckBoxMenuItem = UI.newJCheckBoxMenuItem();
    helpMenuSeparator2 = new Separator();
    aboutMenuItem = new JMenuItem();

    ResourceBundle bundle = ResourceBundle.getBundle("i18n/Bundle"); // NOI18N
    faqFrame.setTitle(bundle.getString("GUI.faqFrame.title")); // NOI18N
    faqFrame.setAlwaysOnTop(true);
    faqFrame.setIconImage(null);

    faqScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    faqScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    faqEditorPane.setEditable(false);
    faqEditorPane.setContentType("text/html"); // NOI18N
    faqEditorPane.setText(null);
    UI.addHyperlinkListener(faqEditorPane, new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent evt) {
        try {
          UI.hyperlinkHandler(evt);
        } catch (Exception e) {
          showException(e);
        }
      }
    });
    faqScrollPane.setViewportView(faqEditorPane);

    GroupLayout faqFrameLayout = new GroupLayout(faqFrame.getContentPane());
    faqFrame.getContentPane().setLayout(faqFrameLayout);
    faqFrameLayout.setHorizontalGroup(faqFrameLayout.createParallelGroup(Alignment.LEADING)
      .addComponent(faqScrollPane, GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
    );
    faqFrameLayout.setVerticalGroup(faqFrameLayout.createParallelGroup(Alignment.LEADING)
      .addComponent(faqScrollPane, GroupLayout.DEFAULT_SIZE, 557, Short.MAX_VALUE)
    );

    aboutDialog.setTitle(bundle.getString("GUI.aboutDialog.title")); // NOI18N
    aboutDialog.setAlwaysOnTop(true);

    aboutScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    aboutScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

    aboutEditorPane.setEditable(false);
    aboutEditorPane.setContentType("text/html"); // NOI18N
    aboutEditorPane.setText(UI.about());
    aboutScrollPane.setViewportView(aboutEditorPane);

    GroupLayout aboutDialogLayout = new GroupLayout(aboutDialog.getContentPane());
    aboutDialog.getContentPane().setLayout(aboutDialogLayout);
    aboutDialogLayout.setHorizontalGroup(aboutDialogLayout.createParallelGroup(Alignment.LEADING)
      .addComponent(aboutScrollPane)
    );
    aboutDialogLayout.setVerticalGroup(aboutDialogLayout.createParallelGroup(Alignment.LEADING)
      .addComponent(aboutScrollPane, GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
    );

    timeoutDialog.setTitle(bundle.getString("GUI.timeoutDialog.title")); // NOI18N
    timeoutDialog.setAlwaysOnTop(true);
    timeoutDialog.setModal(true);

    timeoutLabel.setText(bundle.getString("GUI.timeoutLabel.text")); // NOI18N
    timeoutLabel.setToolTipText(bundle.getString("GUI.timeoutLabel.toolTipText")); // NOI18N

    timeoutComboBox.setModel(new DefaultComboBoxModel(UI.items(5, 180, 5, false, null, null)));

    timeoutButton.setText(bundle.getString("GUI.timeoutButton.text")); // NOI18N
    timeoutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        timeoutButtonActionPerformed(evt);
      }
    });

    GroupLayout timeoutDialogLayout = new GroupLayout(timeoutDialog.getContentPane());
    timeoutDialog.getContentPane().setLayout(timeoutDialogLayout);
    timeoutDialogLayout.setHorizontalGroup(timeoutDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(timeoutDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(timeoutLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(timeoutComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGap(18, 18, 18)
        .addComponent(timeoutButton)
        .addContainerGap())
    );
    timeoutDialogLayout.setVerticalGroup(timeoutDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(timeoutDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(timeoutDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(timeoutLabel)
          .addComponent(timeoutComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(timeoutButton))
        .addContainerGap())
    );

    tvDialog.setTitle(bundle.getString("GUI.tvDialog.title")); // NOI18N
    tvDialog.setAlwaysOnTop(true);
    tvDialog.setModal(true);

    UI.init(tvSeasonComboBox, UI.items(1, 100, 1, true, Str.str("any"), null));
    tvSeasonComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvSeasonComboBoxActionPerformed(evt);
      }
    });

    UI.init(tvEpisodeComboBox, UI.items(0, 300, 1, true, Str.str("any"), null));
    tvEpisodeComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvEpisodeComboBoxActionPerformed(evt);
      }
    });
    updateTVComboBoxes();

    tvSubmitButton.setText(bundle.getString("GUI.tvSubmitButton.text")); // NOI18N
    tvSubmitButton.setToolTipText(bundle.getString("GUI.tvSubmitButton.toolTipText")); // NOI18N
    tvSubmitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvSubmitButtonActionPerformed(evt);
      }
    });

    tvSelectionLabel.setText(bundle.getString("GUI.tvSelectionLabel.text")); // NOI18N

    seasonLabel.setText(bundle.getString("GUI.seasonLabel.text")); // NOI18N
    seasonLabel.setToolTipText(bundle.getString("GUI.seasonLabel.toolTipText")); // NOI18N

    episodeLabel.setText(bundle.getString("GUI.episodeLabel.text")); // NOI18N
    episodeLabel.setToolTipText(bundle.getString("GUI.episodeLabel.toolTipText")); // NOI18N

    tvCancelButton.setText(bundle.getString("GUI.tvCancelButton.text")); // NOI18N
    tvCancelButton.setToolTipText(bundle.getString("GUI.tvCancelButton.toolTipText")); // NOI18N
    tvCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvCancelButtonActionPerformed(evt);
      }
    });

    GroupLayout tvDialogLayout = new GroupLayout(tvDialog.getContentPane());
    tvDialog.getContentPane().setLayout(tvDialogLayout);
    tvDialogLayout.setHorizontalGroup(tvDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(tvDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(tvDialogLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(tvDialogLayout.createSequentialGroup()
            .addComponent(seasonLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tvSeasonComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(episodeLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tvEpisodeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(tvSubmitButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tvCancelButton))
          .addComponent(tvSelectionLabel))
        .addContainerGap())
    );

    tvDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {tvCancelButton, tvSubmitButton});

    tvDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {tvEpisodeComboBox, tvSeasonComboBox});

    tvDialogLayout.setVerticalGroup(tvDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(tvDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(tvSelectionLabel)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(tvDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(seasonLabel)
          .addComponent(tvSeasonComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(episodeLabel)
          .addComponent(tvEpisodeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(tvSubmitButton)
          .addComponent(tvCancelButton))
        .addContainerGap())
    );

    tvDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {tvCancelButton, tvSubmitButton});

    tvDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {tvEpisodeComboBox, tvSeasonComboBox});

    resultsPerSearchDialog.setTitle(bundle.getString("GUI.resultsPerSearchDialog.title")); // NOI18N
    resultsPerSearchDialog.setAlwaysOnTop(true);
    resultsPerSearchDialog.setModal(true);

    regularResultsPerSearchLabel.setText(bundle.getString("GUI.regularResultsPerSearchLabel.text")); // NOI18N
    regularResultsPerSearchLabel.setToolTipText(bundle.getString("GUI.regularResultsPerSearchLabel.toolTipText")); // NOI18N

    resultsPerSearchButton.setText(bundle.getString("GUI.resultsPerSearchButton.text")); // NOI18N
    resultsPerSearchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        resultsPerSearchButtonActionPerformed(evt);
      }
    });

    popularMoviesResultsPerSearchLabel.setText(bundle.getString("GUI.popularMoviesResultsPerSearchLabel.text")); // NOI18N
    popularMoviesResultsPerSearchLabel.setToolTipText(bundle.getString("GUI.popularMoviesResultsPerSearchLabel.toolTipText")); // NOI18N

    popularTVShowsResultsPerSearchLabel.setText(bundle.getString("GUI.popularTVShowsResultsPerSearchLabel.text")); // NOI18N
    popularTVShowsResultsPerSearchLabel.setToolTipText(bundle.getString("GUI.popularTVShowsResultsPerSearchLabel.toolTipText")); // NOI18N

    GroupLayout resultsPerSearchDialogLayout = new GroupLayout(resultsPerSearchDialog.getContentPane());
    resultsPerSearchDialog.getContentPane().setLayout(resultsPerSearchDialogLayout);
    resultsPerSearchDialogLayout.setHorizontalGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(resultsPerSearchDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(resultsPerSearchDialogLayout.createSequentialGroup()
            .addComponent(regularResultsPerSearchLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(regularResultsPerSearchComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGroup(resultsPerSearchDialogLayout.createSequentialGroup()
            .addGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.LEADING, false)
              .addComponent(popularTVShowsResultsPerSearchLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(popularMoviesResultsPerSearchLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.LEADING)
              .addComponent(popularTVShowsResultsPerSearchComboBox, GroupLayout.PREFERRED_SIZE, 44, GroupLayout.PREFERRED_SIZE)
              .addComponent(popularMoviesResultsPerSearchComboBox, GroupLayout.PREFERRED_SIZE, 44, GroupLayout.PREFERRED_SIZE)))
          .addComponent(resultsPerSearchButton))
        .addContainerGap(20, Short.MAX_VALUE))
    );

    resultsPerSearchDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {popularMoviesResultsPerSearchComboBox, popularTVShowsResultsPerSearchComboBox, regularResultsPerSearchComboBox});

    resultsPerSearchDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {popularMoviesResultsPerSearchLabel, popularTVShowsResultsPerSearchLabel, regularResultsPerSearchLabel});

    resultsPerSearchDialogLayout.setVerticalGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(resultsPerSearchDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(regularResultsPerSearchLabel)
          .addComponent(regularResultsPerSearchComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(popularMoviesResultsPerSearchLabel)
          .addComponent(popularMoviesResultsPerSearchComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(resultsPerSearchDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(popularTVShowsResultsPerSearchLabel)
          .addComponent(popularTVShowsResultsPerSearchComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addGap(18, 18, 18)
        .addComponent(resultsPerSearchButton)
        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    resultsPerSearchDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {popularMoviesResultsPerSearchComboBox, popularTVShowsResultsPerSearchComboBox, regularResultsPerSearchComboBox});

    resultsPerSearchDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {popularMoviesResultsPerSearchLabel, popularTVShowsResultsPerSearchLabel, regularResultsPerSearchLabel});

    downloadSizeDialog.setTitle(bundle.getString("GUI.downloadSizeDialog.title")); // NOI18N
    downloadSizeDialog.setAlwaysOnTop(true);
    downloadSizeDialog.setModal(true);

    downloadSizeLabel.setText(bundle.getString("GUI.downloadSizeLabel.text")); // NOI18N
    downloadSizeLabel.setToolTipText(bundle.getString("GUI.downloadSizeLabel.toolTipText")); // NOI18N

    UI.init(maxDownloadSizeComboBox, UI.items(1, 100, 1, false, null, Str.str("infinity")));
    maxDownloadSizeComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        maxDownloadSizeComboBoxActionPerformed(evt);
      }
    });

    downloadSizeToLabel.setText(bundle.getString("GUI.downloadSizeToLabel.text")); // NOI18N

    minDownloadSizeComboBox.setModel(new DefaultComboBoxModel(UI.items(0, 100, 1, false, null, null)));
    minDownloadSizeComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        minDownloadSizeComboBoxActionPerformed(evt);
      }
    });

    downloadSizeButton.setText(bundle.getString("GUI.downloadSizeButton.text")); // NOI18N
    downloadSizeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadSizeButtonActionPerformed(evt);
      }
    });

    downloadSizeIgnoreCheckBox.setSelected(true);
    downloadSizeIgnoreCheckBox.setText(bundle.getString("GUI.downloadSizeIgnoreCheckBox.text")); // NOI18N
    downloadSizeIgnoreCheckBox.setBorder(null);
    downloadSizeIgnoreCheckBox.setFocusPainted(false);
    downloadSizeIgnoreCheckBox.setMargin(new Insets(2, 0, 2, 2));

    GroupLayout downloadSizeDialogLayout = new GroupLayout(downloadSizeDialog.getContentPane());
    downloadSizeDialog.getContentPane().setLayout(downloadSizeDialogLayout);
    downloadSizeDialogLayout.setHorizontalGroup(downloadSizeDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(downloadSizeDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(downloadSizeDialogLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(downloadSizeIgnoreCheckBox)
          .addGroup(downloadSizeDialogLayout.createSequentialGroup()
            .addComponent(downloadSizeLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(minDownloadSizeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(downloadSizeToLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(maxDownloadSizeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(downloadSizeButton)))
        .addContainerGap())
    );

    downloadSizeDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {maxDownloadSizeComboBox, minDownloadSizeComboBox});

    downloadSizeDialogLayout.setVerticalGroup(downloadSizeDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(downloadSizeDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(downloadSizeDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(downloadSizeLabel)
          .addComponent(minDownloadSizeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(downloadSizeToLabel)
          .addComponent(maxDownloadSizeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(downloadSizeButton))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(downloadSizeIgnoreCheckBox)
        .addContainerGap())
    );

    downloadSizeDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {maxDownloadSizeComboBox, minDownloadSizeComboBox});

    extensionsDialog.setTitle(bundle.getString("GUI.extensionsDialog.title")); // NOI18N
    extensionsDialog.setAlwaysOnTop(true);
    extensionsDialog.setModal(true);

    blacklistedScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    blacklistedList.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        blacklistedListKeyPressed(evt);
      }
    });
    blacklistedScrollPane.setViewportView(blacklistedList);

    blacklistedLabel.setText(bundle.getString("GUI.blacklistedLabel.text")); // NOI18N

    whitelistedScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    whitelistedList.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        whitelistedListKeyPressed(evt);
      }
    });
    whitelistedScrollPane.setViewportView(whitelistedList);

    whitelistedToBlacklistedButton.setText(null);
    whitelistedToBlacklistedButton.setToolTipText(bundle.getString("GUI.whitelistedToBlacklistedButton.toolTipText")); // NOI18N
    whitelistedToBlacklistedButton.setMargin(new Insets(0, 0, 0, 0));
    whitelistedToBlacklistedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        whitelistedToBlacklistedButtonActionPerformed(evt);
      }
    });

    blacklistedToWhitelistedButton.setText(null);
    blacklistedToWhitelistedButton.setToolTipText(bundle.getString("GUI.blacklistedToWhitelistedButton.toolTipText")); // NOI18N
    blacklistedToWhitelistedButton.setMargin(new Insets(0, 0, 0, 0));
    blacklistedToWhitelistedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        blacklistedToWhitelistedButtonActionPerformed(evt);
      }
    });

    whitelistLabel.setText(bundle.getString("GUI.whitelistLabel.text")); // NOI18N

    fileExtensionsLabel.setText(bundle.getString("GUI.fileExtensionsLabel.text")); // NOI18N

    extensionsButton.setText(bundle.getString("GUI.extensionsButton.text")); // NOI18N
    extensionsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        extensionsButtonActionPerformed(evt);
      }
    });

    customExtensionTextField.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        customExtensionTextFieldKeyPressed(evt);
      }
    });

    trashCanButton.setText(null);
    trashCanButton.setMargin(new Insets(0, 0, 0, 0));
    trashCanButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trashCanButtonActionPerformed(evt);
      }
    });

    GroupLayout extensionsDialogLayout = new GroupLayout(extensionsDialog.getContentPane());
    extensionsDialog.getContentPane().setLayout(extensionsDialogLayout);
    extensionsDialogLayout.setHorizontalGroup(extensionsDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(extensionsDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(extensionsDialogLayout.createSequentialGroup()
            .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.LEADING)
              .addComponent(whitelistedScrollPane, 0, 0, Short.MAX_VALUE)
              .addComponent(whitelistLabel))
            .addGap(18, 18, 18)
            .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.CENTER)
              .addComponent(customExtensionTextField, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE)
              .addComponent(whitelistedToBlacklistedButton)
              .addComponent(blacklistedToWhitelistedButton)
              .addComponent(extensionsButton))
            .addGap(18, 18, 18)
            .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.TRAILING)
              .addComponent(blacklistedLabel, Alignment.LEADING)
              .addComponent(blacklistedScrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
              .addComponent(trashCanButton)))
          .addComponent(fileExtensionsLabel))
        .addContainerGap())
    );

    extensionsDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {blacklistedToWhitelistedButton, customExtensionTextField, extensionsButton, whitelistedToBlacklistedButton});

    extensionsDialogLayout.setVerticalGroup(extensionsDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(extensionsDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(fileExtensionsLabel)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(whitelistLabel)
          .addComponent(blacklistedLabel))
        .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(extensionsDialogLayout.createSequentialGroup()
            .addGap(113, 113, 113)
            .addComponent(customExtensionTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(whitelistedToBlacklistedButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(blacklistedToWhitelistedButton)
            .addGap(165, 165, 165)
            .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.BASELINE)
              .addComponent(extensionsButton, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
              .addComponent(trashCanButton)))
          .addGroup(extensionsDialogLayout.createParallelGroup(Alignment.TRAILING)
            .addComponent(blacklistedScrollPane, GroupLayout.PREFERRED_SIZE, 340, GroupLayout.PREFERRED_SIZE)
            .addComponent(whitelistedScrollPane, GroupLayout.PREFERRED_SIZE, 340, GroupLayout.PREFERRED_SIZE)))
        .addGap(18, 18, 18))
    );

    extensionsDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {blacklistedToWhitelistedButton, extensionsButton, whitelistedToBlacklistedButton});

    languageCountryDialog.setTitle(bundle.getString("GUI.languageCountryDialog.title")); // NOI18N
    languageCountryDialog.setAlwaysOnTop(true);
    languageCountryDialog.setModal(true);

    countryLabel.setText(bundle.getString("GUI.countryLabel.text")); // NOI18N

    languageLabel.setText(bundle.getString("GUI.languageLabel.text")); // NOI18N

    languageCountryOkButton.setText(bundle.getString("GUI.languageCountryOkButton.text")); // NOI18N
    languageCountryOkButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        languageCountryOkButtonActionPerformed(evt);
      }
    });

    UI.init(languageList, Regex.languages.keySet().toArray(Constant.EMPTY_STRS));
    languageList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent evt) {
        languageListValueChanged(evt);
      }
    });
    languageScrollPane.setViewportView(languageList);

    UI.init(countryList, Regex.countries.keySet().toArray(Constant.EMPTY_STRS));
    countryList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent evt) {
        countryListValueChanged(evt);
      }
    });
    countryScrollPane.setViewportView(countryList);

    languageCountryWarningTextArea.setEditable(false);
    languageCountryWarningTextArea.setLineWrap(true);
    languageCountryWarningTextArea.setText(bundle.getString("GUI.languageCountryWarningTextArea.text")); // NOI18N
    languageCountryWarningTextArea.setWrapStyleWord(true);
    languageCountryWarningTextArea.setMinimumSize(new Dimension(300, 0));
    languageCountryWarningTextArea.setOpaque(false);

    GroupLayout languageCountryDialogLayout = new GroupLayout(languageCountryDialog.getContentPane());
    languageCountryDialog.getContentPane().setLayout(languageCountryDialogLayout);
    languageCountryDialogLayout.setHorizontalGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(languageCountryDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(languageCountryOkButton, Alignment.CENTER)
          .addComponent(languageCountryWarningTextArea, Alignment.CENTER, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addGroup(Alignment.CENTER, languageCountryDialogLayout.createSequentialGroup()
            .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
              .addComponent(languageScrollPane, GroupLayout.PREFERRED_SIZE, 145, GroupLayout.PREFERRED_SIZE)
              .addComponent(languageLabel))
            .addGap(18, 18, 18)
            .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
              .addComponent(countryLabel)
              .addComponent(countryScrollPane, GroupLayout.PREFERRED_SIZE, 137, GroupLayout.PREFERRED_SIZE))))
        .addContainerGap())
    );

    languageCountryDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {countryScrollPane, languageScrollPane});

    languageCountryDialogLayout.setVerticalGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(languageCountryDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(languageCountryWarningTextArea, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(countryLabel, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
          .addComponent(languageLabel))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(countryScrollPane, GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
          .addComponent(languageScrollPane, GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE))
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addComponent(languageCountryOkButton, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
        .addContainerGap())
    );

    readSummaryMenuItem.setText(bundle.getString("GUI.readSummaryMenuItem.text")); // NOI18N
    readSummaryMenuItem.setEnabled(false);
    readSummaryMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        readSummaryMenuItemActionPerformed(evt);
      }
    });
    tablePopupMenu.add(readSummaryMenuItem);

    hearSummaryMenuItem.setText(bundle.getString("GUI.hearSummaryMenuItem.text")); // NOI18N
    hearSummaryMenuItem.setEnabled(false);
    hearSummaryMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        hearSummaryMenuItemActionPerformed(evt);
      }
    });
    tablePopupMenu.add(hearSummaryMenuItem);

    watchTrailerMenuItem.setText(bundle.getString("GUI.watchTrailerMenuItem.text")); // NOI18N
    watchTrailerMenuItem.setEnabled(false);
    watchTrailerMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        watchTrailerMenuItemActionPerformed(evt);
      }
    });
    tablePopupMenu.add(watchTrailerMenuItem);

    downloadLink1MenuItem.setText(bundle.getString("GUI.downloadLink1MenuItem.text")); // NOI18N
    downloadLink1MenuItem.setEnabled(false);
    downloadLink1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadLink1MenuItemActionPerformed(evt);
      }
    });
    tablePopupMenu.add(downloadLink1MenuItem);

    downloadLink2MenuItem.setText(bundle.getString("GUI.downloadLink2MenuItem.text")); // NOI18N
    downloadLink2MenuItem.setEnabled(false);
    downloadLink2MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadLink2MenuItemActionPerformed(evt);
      }
    });
    tablePopupMenu.add(downloadLink2MenuItem);
    tablePopupMenu.add(tablePopupMenuSeparator1);

    copyMenu.setText(bundle.getString("GUI.copyMenu.text")); // NOI18N

    copySelectionMenuItem.setText(bundle.getString("GUI.copySelectionMenuItem.text")); // NOI18N
    copySelectionMenuItem.setEnabled(false);
    copySelectionMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copySelectionMenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copySelectionMenuItem);

    copyFullTitleAndYearMenuItem.setText(bundle.getString("GUI.copyFullTitleAndYearMenuItem.text")); // NOI18N
    copyFullTitleAndYearMenuItem.setToolTipText(bundle.getString("GUI.copyFullTitleAndYearMenuItem.toolTipText")); // NOI18N
    copyFullTitleAndYearMenuItem.setEnabled(false);
    copyFullTitleAndYearMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copyFullTitleAndYearMenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copyFullTitleAndYearMenuItem);

    copyPosterImageMenuItem.setText(bundle.getString("GUI.copyPosterImageMenuItem.text")); // NOI18N
    copyPosterImageMenuItem.setEnabled(false);
    copyPosterImageMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copyPosterImageMenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copyPosterImageMenuItem);
    copyMenu.add(copyMenuSeparator1);

    copyDownloadLink1MenuItem.setText(bundle.getString("GUI.copyDownloadLink1MenuItem.text")); // NOI18N
    copyDownloadLink1MenuItem.setEnabled(false);
    copyDownloadLink1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copyDownloadLink1MenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copyDownloadLink1MenuItem);

    copyDownloadLink2MenuItem.setText(bundle.getString("GUI.copyDownloadLink2MenuItem.text")); // NOI18N
    copyDownloadLink2MenuItem.setEnabled(false);
    copyDownloadLink2MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copyDownloadLink2MenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copyDownloadLink2MenuItem);

    copySummaryLinkMenuItem.setText(bundle.getString("GUI.copySummaryLinkMenuItem.text")); // NOI18N
    copySummaryLinkMenuItem.setEnabled(false);
    copySummaryLinkMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copySummaryLinkMenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copySummaryLinkMenuItem);

    copyTrailerLinkMenuItem.setText(bundle.getString("GUI.copyTrailerLinkMenuItem.text")); // NOI18N
    copyTrailerLinkMenuItem.setEnabled(false);
    copyTrailerLinkMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copyTrailerLinkMenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copyTrailerLinkMenuItem);

    copySubtitleLinkMenuItem.setText(bundle.getString("GUI.copySubtitleLinkMenuItem.text")); // NOI18N
    copySubtitleLinkMenuItem.setEnabled(false);
    copySubtitleLinkMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copySubtitleLinkMenuItemActionPerformed(evt);
      }
    });
    copyMenu.add(copySubtitleLinkMenuItem);

    tablePopupMenu.add(copyMenu);

    emailMenu.setText(bundle.getString("GUI.emailMenu.text")); // NOI18N

    emailDownloadLink1MenuItem.setText(bundle.getString("GUI.emailDownloadLink1MenuItem.text")); // NOI18N
    emailDownloadLink1MenuItem.setEnabled(false);
    emailDownloadLink1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        emailDownloadLink1MenuItemActionPerformed(evt);
      }
    });
    emailMenu.add(emailDownloadLink1MenuItem);

    emailDownloadLink2MenuItem.setText(bundle.getString("GUI.emailDownloadLink2MenuItem.text")); // NOI18N
    emailDownloadLink2MenuItem.setEnabled(false);
    emailDownloadLink2MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        emailDownloadLink2MenuItemActionPerformed(evt);
      }
    });
    emailMenu.add(emailDownloadLink2MenuItem);

    emailSummaryLinkMenuItem.setText(bundle.getString("GUI.emailSummaryLinkMenuItem.text")); // NOI18N
    emailSummaryLinkMenuItem.setEnabled(false);
    emailSummaryLinkMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        emailSummaryLinkMenuItemActionPerformed(evt);
      }
    });
    emailMenu.add(emailSummaryLinkMenuItem);

    emailTrailerLinkMenuItem.setText(bundle.getString("GUI.emailTrailerLinkMenuItem.text")); // NOI18N
    emailTrailerLinkMenuItem.setEnabled(false);
    emailTrailerLinkMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        emailTrailerLinkMenuItemActionPerformed(evt);
      }
    });
    emailMenu.add(emailTrailerLinkMenuItem);
    emailMenu.add(emailMenuSeparator1);

    emailEverythingMenuItem.setText(bundle.getString("GUI.emailEverythingMenuItem.text")); // NOI18N
    emailEverythingMenuItem.setEnabled(false);
    emailEverythingMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        emailEverythingMenuItemActionPerformed(evt);
      }
    });
    emailMenu.add(emailEverythingMenuItem);

    tablePopupMenu.add(emailMenu);
    tablePopupMenu.add(tablePopupMenuSeparator2);

    watchOnDeviceMenuItem.setText(bundle.getString("GUI.watchOnDeviceMenuItem.text")); // NOI18N
    watchOnDeviceMenuItem.setToolTipText(bundle.getString("GUI.watchOnDeviceMenuItem.toolTipText")); // NOI18N
    watchOnDeviceMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        watchOnDeviceMenuItemActionPerformed(evt);
      }
    });
    tablePopupMenu.add(watchOnDeviceMenuItem);
    tablePopupMenu.add(tablePopupMenuSeparator3);

    findSubtitleMenuItem.setText(bundle.getString("GUI.findSubtitleMenuItem.text")); // NOI18N
    findSubtitleMenuItem.setEnabled(false);
    findSubtitleMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        findSubtitleMenuItemActionPerformed(evt);
      }
    });
    tablePopupMenu.add(findSubtitleMenuItem);
    tablePopupMenu.add(tablePopupMenuSeparator4);

    banTitleMenu.setText(bundle.getString("GUI.banTitleMenu.text")); // NOI18N
    banTitleMenu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent evt) {
      }
      public void menuDeselected(MenuEvent evt) {
      }
      public void menuSelected(MenuEvent evt) {
        banTitleMenuMenuSelected(evt);
      }
    });
    tablePopupMenu.add(banTitleMenu);

    popularMoviesMenuItem.setText(bundle.getString("GUI.popularMoviesMenuItem.text")); // NOI18N
    popularMoviesMenuItem.setToolTipText(bundle.getString("GUI.popularMoviesMenuItem.toolTipText")); // NOI18N
    popularMoviesMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        popularMoviesMenuItemActionPerformed(evt);
      }
    });
    popularPopupMenu.add(popularMoviesMenuItem);

    popularNewHQMoviesMenuItem.setText(bundle.getString("GUI.popularNewHQMoviesMenuItem.text")); // NOI18N
    popularNewHQMoviesMenuItem.setToolTipText(bundle.getString("GUI.popularNewHQMoviesMenuItem.toolTipText")); // NOI18N
    popularNewHQMoviesMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        popularNewHQMoviesMenuItemActionPerformed(evt);
      }
    });
    popularPopupMenu.add(popularNewHQMoviesMenuItem);

    popularTVShowsMenuItem.setText(bundle.getString("GUI.popularTVShowsMenuItem.text")); // NOI18N
    popularTVShowsMenuItem.setToolTipText(bundle.getString("GUI.popularTVShowsMenuItem.toolTipText")); // NOI18N
    popularTVShowsMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        popularTVShowsMenuItemActionPerformed(evt);
      }
    });
    popularPopupMenu.add(popularTVShowsMenuItem);

    popularNewHQTVShowsMenuItem.setText(bundle.getString("GUI.popularNewHQTVShowsMenuItem.text")); // NOI18N
    popularNewHQTVShowsMenuItem.setToolTipText(bundle.getString("GUI.popularNewHQTVShowsMenuItem.toolTipText")); // NOI18N
    popularNewHQTVShowsMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        popularNewHQTVShowsMenuItemActionPerformed(evt);
      }
    });
    popularPopupMenu.add(popularNewHQTVShowsMenuItem);

    textComponentPopupMenu.addPopupMenuListener(new PopupMenuListener() {
      public void popupMenuCanceled(PopupMenuEvent evt) {
      }
      public void popupMenuWillBecomeInvisible(PopupMenuEvent evt) {
        textComponentPopupMenuPopupMenuWillBecomeInvisible(evt);
      }
      public void popupMenuWillBecomeVisible(PopupMenuEvent evt) {
        textComponentPopupMenuPopupMenuWillBecomeVisible(evt);
      }
    });

    textComponentCutMenuItem.setText(bundle.getString("GUI.textComponentCutMenuItem.text")); // NOI18N
    textComponentCutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        textComponentCutMenuItemActionPerformed(evt);
      }
    });
    textComponentPopupMenu.add(textComponentCutMenuItem);

    textComponentCopyMenuItem.setText(bundle.getString("GUI.textComponentCopyMenuItem.text")); // NOI18N
    textComponentCopyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        textComponentCopyMenuItemActionPerformed(evt);
      }
    });
    textComponentPopupMenu.add(textComponentCopyMenuItem);

    textComponentPasteMenuItem.setText(bundle.getString("GUI.textComponentPasteMenuItem.text")); // NOI18N
    textComponentPasteMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        textComponentPasteMenuItemActionPerformed(evt);
      }
    });
    textComponentPopupMenu.add(textComponentPasteMenuItem);

    textComponentPasteSearchMenuItem.setText(bundle.getString("GUI.textComponentPasteSearchMenuItem.text")); // NOI18N
    textComponentPasteSearchMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        textComponentPasteSearchMenuItemActionPerformed(evt);
      }
    });
    textComponentPopupMenu.add(textComponentPasteSearchMenuItem);

    textComponentDeleteMenuItem.setText(bundle.getString("GUI.textComponentDeleteMenuItem.text")); // NOI18N
    textComponentDeleteMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        textComponentDeleteMenuItemActionPerformed(evt);
      }
    });
    textComponentPopupMenu.add(textComponentDeleteMenuItem);
    textComponentPopupMenu.add(textComponentPopupMenuSeparator1);

    textComponentSelectAllMenuItem.setText(bundle.getString("GUI.textComponentSelectAllMenuItem.text")); // NOI18N
    textComponentSelectAllMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        textComponentSelectAllMenuItemActionPerformed(evt);
      }
    });
    textComponentPopupMenu.add(textComponentSelectAllMenuItem);

    proxyFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    proxyFileChooser.setCurrentDirectory(null);

    playlistFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    playlistFileChooser.setCurrentDirectory(null);
    playlistFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    proxyDialog.setTitle(bundle.getString("GUI.proxyDialog.title")); // NOI18N
    proxyDialog.setAlwaysOnTop(true);
    proxyDialog.setModal(true);

    proxyAddButton.setText(bundle.getString("GUI.proxyAddButton.text")); // NOI18N
    proxyAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        proxyAddButtonActionPerformed(evt);
      }
    });

    proxyRemoveButton.setText(bundle.getString("GUI.proxyRemoveButton.text")); // NOI18N
    proxyRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        proxyRemoveButtonActionPerformed(evt);
      }
    });

    UI.init(proxyComboBox, Str.str("noProxy"));

    proxyDownloadLinkInfoCheckBox.setText(bundle.getString("GUI.proxyDownloadLinkInfoCheckBox.text")); // NOI18N
    proxyDownloadLinkInfoCheckBox.setToolTipText(Str.str("forExample", Str.get(728)));

    proxyUseForLabel.setText(bundle.getString("GUI.proxyUseForLabel.text")); // NOI18N

    proxyVideoInfoCheckBox.setText(bundle.getString("GUI.proxyVideoInfoCheckBox.text")); // NOI18N
    proxyVideoInfoCheckBox.setToolTipText(Str.str("forExample", Str.get(578)));

    proxySearchEnginesCheckBox.setText(bundle.getString("GUI.proxySearchEnginesCheckBox.text")); // NOI18N
    proxySearchEnginesCheckBox.setToolTipText(Str.str("forExample", Str.get(579)));

    proxyOKButton.setText(bundle.getString("GUI.proxyOKButton.text")); // NOI18N
    proxyOKButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        proxyOKButtonActionPerformed(evt);
      }
    });

    proxyDownloadButton.setText(bundle.getString("GUI.proxyDownloadButton.text")); // NOI18N
    proxyDownloadButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        proxyDownloadButtonActionPerformed(evt);
      }
    });

    proxyTrailersCheckBox.setText(bundle.getString("GUI.proxyTrailersCheckBox.text")); // NOI18N
    proxyTrailersCheckBox.setToolTipText(Str.str("forExample", Str.get(580)));

    proxyLoadingLabel.setText(null);

    proxyImportButton.setText(bundle.getString("GUI.proxyImportButton.text")); // NOI18N
    proxyImportButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        proxyImportButtonActionPerformed(evt);
      }
    });

    proxyExportButton.setText(bundle.getString("GUI.proxyExportButton.text")); // NOI18N
    proxyExportButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        proxyExportButtonActionPerformed(evt);
      }
    });

    proxyUpdatesCheckBox.setText(bundle.getString("GUI.proxyUpdatesCheckBox.text")); // NOI18N
    proxyUpdatesCheckBox.setToolTipText(Str.str("forExample", Str.get(582)));

    proxySubtitlesCheckBox.setText(bundle.getString("GUI.proxySubtitlesCheckBox.text")); // NOI18N
    proxySubtitlesCheckBox.setToolTipText(Str.str("forExample", Str.get(583)));

    GroupLayout proxyDialogLayout = new GroupLayout(proxyDialog.getContentPane());
    proxyDialog.getContentPane().setLayout(proxyDialogLayout);
    proxyDialogLayout.setHorizontalGroup(proxyDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(proxyDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(proxyDialogLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(proxyComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addGroup(proxyDialogLayout.createSequentialGroup()
            .addComponent(proxyUseForLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyDownloadLinkInfoCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyVideoInfoCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxySearchEnginesCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyTrailersCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyUpdatesCheckBox)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxySubtitlesCheckBox)
            .addGap(0, 125, Short.MAX_VALUE))
          .addGroup(proxyDialogLayout.createSequentialGroup()
            .addComponent(proxyAddButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyRemoveButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyDownloadButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyImportButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyExportButton)
            .addGap(18, 18, Short.MAX_VALUE)
            .addComponent(proxyLoadingLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(proxyOKButton)))
        .addContainerGap())
    );
    proxyDialogLayout.setVerticalGroup(proxyDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(proxyDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(proxyComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(proxyDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(proxyUseForLabel)
          .addComponent(proxyDownloadLinkInfoCheckBox)
          .addComponent(proxyVideoInfoCheckBox)
          .addComponent(proxySearchEnginesCheckBox)
          .addComponent(proxyTrailersCheckBox)
          .addComponent(proxyUpdatesCheckBox)
          .addComponent(proxySubtitlesCheckBox))
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(proxyDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(proxyAddButton)
          .addComponent(proxyRemoveButton)
          .addComponent(proxyDownloadButton)
          .addComponent(proxyImportButton)
          .addComponent(proxyExportButton)
          .addComponent(proxyLoadingLabel)
          .addComponent(proxyOKButton))
        .addContainerGap())
    );

    proxyDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {proxyAddButton, proxyDownloadButton, proxyExportButton, proxyImportButton, proxyOKButton, proxyRemoveButton});

    proxyDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {proxyDownloadLinkInfoCheckBox, proxySearchEnginesCheckBox, proxySubtitlesCheckBox, proxyTrailersCheckBox, proxyUpdatesCheckBox, proxyVideoInfoCheckBox});

    addProxiesDialog.setTitle(bundle.getString("GUI.addProxiesDialog.title")); // NOI18N
    addProxiesDialog.setAlwaysOnTop(true);
    addProxiesDialog.setModal(true);
    addProxiesDialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        addProxiesDialogWindowClosing(evt);
      }
    });

    addProxiesLabel.setText(bundle.getString("GUI.addProxiesLabel.text")); // NOI18N

    addProxiesTextArea.setColumns(20);
    addProxiesTextArea.setRows(5);
    addProxiesScrollPane.setViewportView(addProxiesTextArea);

    addProxiesCancelButton.setText(bundle.getString("GUI.addProxiesCancelButton.text")); // NOI18N
    addProxiesCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        addProxiesCancelButtonActionPerformed(evt);
      }
    });

    addProxiesAddButton.setText(bundle.getString("GUI.addProxiesAddButton.text")); // NOI18N
    addProxiesAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        addProxiesAddButtonActionPerformed(evt);
      }
    });

    GroupLayout addProxiesDialogLayout = new GroupLayout(addProxiesDialog.getContentPane());
    addProxiesDialog.getContentPane().setLayout(addProxiesDialogLayout);
    addProxiesDialogLayout.setHorizontalGroup(addProxiesDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(addProxiesDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(addProxiesDialogLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(addProxiesLabel)
          .addGroup(Alignment.CENTER, addProxiesDialogLayout.createSequentialGroup()
            .addComponent(addProxiesAddButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(addProxiesCancelButton))
          .addComponent(addProxiesScrollPane, Alignment.CENTER, GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE))
        .addContainerGap())
    );

    addProxiesDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {addProxiesAddButton, addProxiesCancelButton});

    addProxiesDialogLayout.setVerticalGroup(addProxiesDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(addProxiesDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(addProxiesLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(addProxiesScrollPane, GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(addProxiesDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(addProxiesCancelButton)
          .addComponent(addProxiesAddButton))
        .addContainerGap())
    );

    addProxiesDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {addProxiesAddButton, addProxiesCancelButton});

    removeProxiesDialog.setTitle(bundle.getString("GUI.removeProxiesDialog.title")); // NOI18N
    removeProxiesDialog.setAlwaysOnTop(true);
    removeProxiesDialog.setModal(true);
    removeProxiesDialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        removeProxiesDialogWindowClosing(evt);
      }
    });

    removeProxiesLabel.setText(bundle.getString("GUI.removeProxiesLabel.text")); // NOI18N

    removeProxiesList.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        removeProxiesListKeyPressed(evt);
      }
    });
    removeProxiesScrollPane.setViewportView(removeProxiesList);

    removeProxiesRemoveButton.setText(bundle.getString("GUI.removeProxiesRemoveButton.text")); // NOI18N
    removeProxiesRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        removeProxiesRemoveButtonActionPerformed(evt);
      }
    });

    removeProxiesCancelButton.setText(bundle.getString("GUI.removeProxiesCancelButton.text")); // NOI18N
    removeProxiesCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        removeProxiesCancelButtonActionPerformed(evt);
      }
    });

    GroupLayout removeProxiesDialogLayout = new GroupLayout(removeProxiesDialog.getContentPane());
    removeProxiesDialog.getContentPane().setLayout(removeProxiesDialogLayout);
    removeProxiesDialogLayout.setHorizontalGroup(removeProxiesDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(removeProxiesDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(removeProxiesDialogLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(Alignment.CENTER, removeProxiesDialogLayout.createSequentialGroup()
            .addComponent(removeProxiesRemoveButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(removeProxiesCancelButton))
          .addComponent(removeProxiesScrollPane, Alignment.CENTER, GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
          .addComponent(removeProxiesLabel))
        .addContainerGap())
    );

    removeProxiesDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {removeProxiesCancelButton, removeProxiesRemoveButton});

    removeProxiesDialogLayout.setVerticalGroup(removeProxiesDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(removeProxiesDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(removeProxiesLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(removeProxiesScrollPane, GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(removeProxiesDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(removeProxiesRemoveButton)
          .addComponent(removeProxiesCancelButton))
        .addContainerGap())
    );

    removeProxiesDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {removeProxiesCancelButton, removeProxiesRemoveButton});

    msgDialog.setTitle(Constant.APP_TITLE);
    msgDialog.setAlwaysOnTop(true);
    msgDialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        msgDialogWindowClosing(evt);
      }
    });

    msgOKButton.setText(bundle.getString("GUI.msgOKButton.text")); // NOI18N
    msgOKButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        msgOKButtonActionPerformed(evt);
      }
    });

    msgScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    msgEditorPane.setEditable(false);
    msgEditorPane.setContentType("text/html"); // NOI18N
    msgEditorPane.setFont(new Font("Verdana", 0, 12)); // NOI18N
    msgEditorPane.setText(Constant.BLANK_HTML_PAGE);
    msgScrollPane.setViewportView(msgEditorPane);

    GroupLayout msgDialogLayout = new GroupLayout(msgDialog.getContentPane());
    msgDialog.getContentPane().setLayout(msgDialogLayout);
    msgDialogLayout.setHorizontalGroup(msgDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(msgDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(msgDialogLayout.createParallelGroup(Alignment.CENTER)
          .addComponent(msgOKButton)
          .addComponent(msgScrollPane, GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE))
        .addContainerGap())
    );
    msgDialogLayout.setVerticalGroup(msgDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(Alignment.TRAILING, msgDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(msgScrollPane, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addComponent(msgOKButton)
        .addContainerGap())
    );

    profileDialog.setTitle(bundle.getString("GUI.profileDialog.title")); // NOI18N
    profileDialog.setAlwaysOnTop(true);
    profileDialog.setModal(true);

    profileSetButton.setText(bundle.getString("GUI.profileSetButton.text")); // NOI18N
    profileSetButton.setEnabled(false);
    profileSetButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileSetButtonActionPerformed(evt);
      }
    });

    profileClearButton.setText(bundle.getString("GUI.profileClearButton.text")); // NOI18N
    profileClearButton.setEnabled(false);
    profileClearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileClearButtonActionPerformed(evt);
      }
    });

    profileUseButton.setText(bundle.getString("GUI.profileUseButton.text")); // NOI18N
    profileUseButton.setEnabled(false);
    profileUseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileUseButtonActionPerformed(evt);
      }
    });

    profileComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileComboBoxActionPerformed(evt);
      }
    });

    profileRenameButton.setText(bundle.getString("GUI.profileRenameButton.text")); // NOI18N
    profileRenameButton.setEnabled(false);
    profileRenameButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileRenameButtonActionPerformed(evt);
      }
    });

    profileOKButton.setText(bundle.getString("GUI.profileOKButton.text")); // NOI18N
    profileOKButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileOKButtonActionPerformed(evt);
      }
    });

    GroupLayout profileDialogLayout = new GroupLayout(profileDialog.getContentPane());
    profileDialog.getContentPane().setLayout(profileDialogLayout);
    profileDialogLayout.setHorizontalGroup(profileDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(profileDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(profileComboBox, 0, 218, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(profileRenameButton)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(profileSetButton)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(profileClearButton)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(profileUseButton)
        .addGap(18, 18, 18)
        .addComponent(profileOKButton)
        .addContainerGap())
    );
    profileDialogLayout.setVerticalGroup(profileDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(profileDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(profileDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(profileComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(profileRenameButton)
          .addComponent(profileSetButton)
          .addComponent(profileClearButton)
          .addComponent(profileUseButton)
          .addComponent(profileOKButton))
        .addContainerGap())
    );

    profileDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {profileClearButton, profileSetButton, profileUseButton});

    profileNameChangeDialog.setTitle(bundle.getString("GUI.profileNameChangeDialog.title")); // NOI18N
    profileNameChangeDialog.setAlwaysOnTop(true);
    profileNameChangeDialog.setModal(true);
    profileNameChangeDialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        profileNameChangeDialogWindowClosing(evt);
      }
    });

    profileNameChangeLabel.setText(bundle.getString("GUI.profileNameChangeLabel.text")); // NOI18N

    profileNameChangeOKButton.setText(bundle.getString("GUI.profileNameChangeOKButton.text")); // NOI18N
    profileNameChangeOKButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileNameChangeOKButtonActionPerformed(evt);
      }
    });

    profileNameChangeCancelButton.setText(bundle.getString("GUI.profileNameChangeCancelButton.text")); // NOI18N
    profileNameChangeCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profileNameChangeCancelButtonActionPerformed(evt);
      }
    });

    GroupLayout profileNameChangeDialogLayout = new GroupLayout(profileNameChangeDialog.getContentPane());
    profileNameChangeDialog.getContentPane().setLayout(profileNameChangeDialogLayout);
    profileNameChangeDialogLayout.setHorizontalGroup(profileNameChangeDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(profileNameChangeDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(profileNameChangeLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(profileNameChangeTextField, GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(profileNameChangeOKButton)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(profileNameChangeCancelButton)
        .addContainerGap())
    );
    profileNameChangeDialogLayout.setVerticalGroup(profileNameChangeDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(profileNameChangeDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(profileNameChangeDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(profileNameChangeLabel)
          .addComponent(profileNameChangeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(profileNameChangeCancelButton)
          .addComponent(profileNameChangeOKButton))
        .addContainerGap())
    );

    commentsDialog.setTitle(bundle.getString("GUI.commentsDialog.title")); // NOI18N
    commentsDialog.setAlwaysOnTop(true);

    commentsTextPane.setEditable(false);
    commentsTextPane.setFont(new Font("Verdana", 0, 12)); // NOI18N
    commentsTextPane.setMargin(new Insets(8, 8, 8, 8));
    commentsScrollPane.setViewportView(commentsTextPane);

    GroupLayout commentsDialogLayout = new GroupLayout(commentsDialog.getContentPane());
    commentsDialog.getContentPane().setLayout(commentsDialogLayout);
    commentsDialogLayout.setHorizontalGroup(commentsDialogLayout.createParallelGroup(Alignment.LEADING)
      .addComponent(commentsScrollPane, GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
    );
    commentsDialogLayout.setVerticalGroup(commentsDialogLayout.createParallelGroup(Alignment.LEADING)
      .addComponent(commentsScrollPane, GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
    );

    portDialog.setTitle(bundle.getString("GUI.portDialog.title")); // NOI18N
    portDialog.setAlwaysOnTop(true);
    portDialog.setModal(true);
    portDialog.addComponentListener(new ComponentAdapter() {
      public void componentHidden(ComponentEvent evt) {
        portDialogComponentHidden(evt);
      }
    });

    portLabel.setText(bundle.getString("GUI.portLabel.text")); // NOI18N
    portLabel.setToolTipText(bundle.getString("GUI.portLabel.toolTipText")); // NOI18N

    portTextField.setToolTipText(bundle.getString("GUI.portTextField.toolTipText")); // NOI18N

    portRandomizeCheckBox.setSelected(true);
    portRandomizeCheckBox.setText(bundle.getString("GUI.portRandomizeCheckBox.text")); // NOI18N
    portRandomizeCheckBox.setToolTipText(bundle.getString("GUI.portRandomizeCheckBox.toolTipText")); // NOI18N
    portRandomizeCheckBox.setBorder(null);
    portRandomizeCheckBox.setFocusPainted(false);
    portRandomizeCheckBox.setMargin(new Insets(2, 0, 2, 2));

    portOkButton.setText(bundle.getString("GUI.portOkButton.text")); // NOI18N
    portOkButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        portOkButtonActionPerformed(evt);
      }
    });

    GroupLayout portDialogLayout = new GroupLayout(portDialog.getContentPane());
    portDialog.getContentPane().setLayout(portDialogLayout);
    portDialogLayout.setHorizontalGroup(portDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(portDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(portLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(portTextField, GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(portRandomizeCheckBox)
        .addGap(18, 18, 18)
        .addComponent(portOkButton)
        .addContainerGap())
    );
    portDialogLayout.setVerticalGroup(portDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(portDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(portDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(portLabel)
          .addComponent(portTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(portRandomizeCheckBox)
          .addComponent(portOkButton))
        .addContainerGap())
    );

    tvSubtitleDialog.setTitle(bundle.getString("GUI.tvSubtitleDialog.title")); // NOI18N
    tvSubtitleDialog.setAlwaysOnTop(true);

    tvSubtitleLanguageLabel.setText(bundle.getString("GUI.tvSubtitleLanguageLabel.text")); // NOI18N

    String[] subtitleLanguages = Regex.subtitleLanguages.keySet().toArray(Constant.EMPTY_STRS);
    UI.init(tvSubtitleLanguageComboBox, subtitleLanguages);
    tvSubtitleLanguageComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvSubtitleLanguageComboBoxActionPerformed(evt);
      }
    });

    tvSubtitleFormatLabel.setText(bundle.getString("GUI.tvSubtitleFormatLabel.text")); // NOI18N

    String[] subtitleFormats = {Str.str("any"), Constant.HQ, Constant.DVD, Constant.HD720, Constant.HD1080};
    UI.init(tvSubtitleFormatComboBox, subtitleFormats);
    tvSubtitleFormatComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvSubtitleFormatComboBoxActionPerformed(evt);
      }
    });

    tvSubtitleSeasonLabel.setText(bundle.getString("GUI.tvSubtitleSeasonLabel.text")); // NOI18N
    tvSubtitleSeasonLabel.setToolTipText(bundle.getString("GUI.tvSubtitleSeasonLabel.toolTipText")); // NOI18N

    tvSubtitleSeasonComboBox.setModel(new DefaultComboBoxModel(UI.items(1, 100, 1, true, null, null)));

    tvSubtitleEpisodeLabel.setText(bundle.getString("GUI.tvSubtitleEpisodeLabel.text")); // NOI18N
    tvSubtitleEpisodeLabel.setToolTipText(bundle.getString("GUI.tvSubtitleEpisodeLabel.toolTipText")); // NOI18N

    tvSubtitleEpisodeComboBox.setModel(new DefaultComboBoxModel(UI.items(1, 300, 1, true, null, null)));

    tvSubtitleDownloadMatch1Button.setText(bundle.getString("GUI.tvSubtitleDownloadMatch1Button.text")); // NOI18N
    tvSubtitleDownloadMatch1Button.setToolTipText(bundle.getString("GUI.tvSubtitleDownloadMatch1Button.toolTipText")); // NOI18N
    tvSubtitleDownloadMatch1Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvSubtitleDownloadMatch1ButtonActionPerformed(evt);
      }
    });

    tvSubtitleDownloadMatch2Button.setText(bundle.getString("GUI.tvSubtitleDownloadMatch2Button.text")); // NOI18N
    tvSubtitleDownloadMatch2Button.setToolTipText(bundle.getString("GUI.tvSubtitleDownloadMatch2Button.toolTipText")); // NOI18N
    tvSubtitleDownloadMatch2Button.setMargin(new Insets(0, 2, 0, 2));
    tvSubtitleDownloadMatch2Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tvSubtitleDownloadMatch2ButtonActionPerformed(evt);
      }
    });

    tvSubtitleLoadingLabel.setText(null);

    GroupLayout tvSubtitleDialogLayout = new GroupLayout(tvSubtitleDialog.getContentPane());
    tvSubtitleDialog.getContentPane().setLayout(tvSubtitleDialogLayout);
    tvSubtitleDialogLayout.setHorizontalGroup(tvSubtitleDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(Alignment.TRAILING, tvSubtitleDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(tvSubtitleDialogLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(tvSubtitleDialogLayout.createSequentialGroup()
            .addComponent(tvSubtitleLanguageLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tvSubtitleLanguageComboBox, 0, 126, Short.MAX_VALUE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(tvSubtitleFormatLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tvSubtitleFormatComboBox, 0, 96, Short.MAX_VALUE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(tvSubtitleSeasonLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tvSubtitleSeasonComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(tvSubtitleEpisodeLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(tvSubtitleEpisodeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGroup(tvSubtitleDialogLayout.createSequentialGroup()
            .addComponent(tvSubtitleDownloadMatch1Button)
            .addGap(1, 1, 1)
            .addComponent(tvSubtitleDownloadMatch2Button)
            .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(tvSubtitleLoadingLabel)))
        .addContainerGap())
    );

    tvSubtitleDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {tvSubtitleEpisodeComboBox, tvSubtitleSeasonComboBox});

    tvSubtitleDialogLayout.setVerticalGroup(tvSubtitleDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(tvSubtitleDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(tvSubtitleDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(tvSubtitleLanguageLabel)
          .addComponent(tvSubtitleLanguageComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(tvSubtitleSeasonLabel)
          .addComponent(tvSubtitleSeasonComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(tvSubtitleEpisodeLabel)
          .addComponent(tvSubtitleEpisodeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(tvSubtitleFormatLabel)
          .addComponent(tvSubtitleFormatComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(tvSubtitleDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(tvSubtitleDownloadMatch1Button)
          .addComponent(tvSubtitleDownloadMatch2Button)
          .addComponent(tvSubtitleLoadingLabel))
        .addContainerGap())
    );

    tvSubtitleDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {tvSubtitleEpisodeComboBox, tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox});

    tvSubtitleDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {tvSubtitleDownloadMatch1Button, tvSubtitleDownloadMatch2Button});

    movieSubtitleDialog.setTitle(bundle.getString("GUI.movieSubtitleDialog.title")); // NOI18N
    movieSubtitleDialog.setAlwaysOnTop(true);

    movieSubtitleLanguageLabel.setText(bundle.getString("GUI.movieSubtitleLanguageLabel.text")); // NOI18N

    UI.init(movieSubtitleLanguageComboBox, subtitleLanguages);
    movieSubtitleLanguageComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        movieSubtitleLanguageComboBoxActionPerformed(evt);
      }
    });

    movieSubtitleFormatLabel.setText(bundle.getString("GUI.movieSubtitleFormatLabel.text")); // NOI18N

    UI.init(movieSubtitleFormatComboBox, subtitleFormats);
    movieSubtitleFormatComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        movieSubtitleFormatComboBoxActionPerformed(evt);
      }
    });

    movieSubtitleDownloadMatch1Button.setText(bundle.getString("GUI.movieSubtitleDownloadMatch1Button.text")); // NOI18N
    movieSubtitleDownloadMatch1Button.setToolTipText(bundle.getString("GUI.movieSubtitleDownloadMatch1Button.toolTipText")); // NOI18N
    movieSubtitleDownloadMatch1Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        movieSubtitleDownloadMatch1ButtonActionPerformed(evt);
      }
    });

    movieSubtitleDownloadMatch2Button.setText(bundle.getString("GUI.movieSubtitleDownloadMatch2Button.text")); // NOI18N
    movieSubtitleDownloadMatch2Button.setToolTipText(bundle.getString("GUI.movieSubtitleDownloadMatch2Button.toolTipText")); // NOI18N
    movieSubtitleDownloadMatch2Button.setMargin(new Insets(0, 2, 0, 2));
    movieSubtitleDownloadMatch2Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        movieSubtitleDownloadMatch2ButtonActionPerformed(evt);
      }
    });

    movieSubtitleLoadingLabel.setText(null);

    GroupLayout movieSubtitleDialogLayout = new GroupLayout(movieSubtitleDialog.getContentPane());
    movieSubtitleDialog.getContentPane().setLayout(movieSubtitleDialogLayout);
    movieSubtitleDialogLayout.setHorizontalGroup(movieSubtitleDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(movieSubtitleDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(movieSubtitleDialogLayout.createParallelGroup(Alignment.TRAILING)
          .addGroup(Alignment.LEADING, movieSubtitleDialogLayout.createSequentialGroup()
            .addComponent(movieSubtitleLanguageLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(movieSubtitleLanguageComboBox, 0, 126, Short.MAX_VALUE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(movieSubtitleFormatLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(movieSubtitleFormatComboBox, 0, 96, Short.MAX_VALUE))
          .addGroup(movieSubtitleDialogLayout.createSequentialGroup()
            .addComponent(movieSubtitleDownloadMatch1Button)
            .addGap(1, 1, 1)
            .addComponent(movieSubtitleDownloadMatch2Button)
            .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(movieSubtitleLoadingLabel)))
        .addContainerGap())
    );
    movieSubtitleDialogLayout.setVerticalGroup(movieSubtitleDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(Alignment.TRAILING, movieSubtitleDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(movieSubtitleDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(movieSubtitleLanguageLabel)
          .addComponent(movieSubtitleLanguageComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(movieSubtitleFormatLabel)
          .addComponent(movieSubtitleFormatComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(movieSubtitleDialogLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(movieSubtitleDownloadMatch1Button)
          .addComponent(movieSubtitleDownloadMatch2Button)
          .addComponent(movieSubtitleLoadingLabel))
        .addContainerGap())
    );

    movieSubtitleDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {movieSubtitleDownloadMatch1Button, movieSubtitleDownloadMatch2Button});

    movieSubtitleDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {movieSubtitleFormatComboBox, movieSubtitleLanguageComboBox});

    timedMsgDialog.setTitle(Constant.APP_TITLE);
    timedMsgDialog.setAlwaysOnTop(true);
    timedMsgDialog.setResizable(false);

    timedMsgLabel.setHorizontalAlignment(SwingConstants.CENTER);

    GroupLayout timedMsgDialogLayout = new GroupLayout(timedMsgDialog.getContentPane());
    timedMsgDialog.getContentPane().setLayout(timedMsgDialogLayout);
    timedMsgDialogLayout.setHorizontalGroup(timedMsgDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(timedMsgDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(timedMsgLabel, GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)
        .addContainerGap())
    );
    timedMsgDialogLayout.setVerticalGroup(timedMsgDialogLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(timedMsgDialogLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(timedMsgLabel, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
        .addContainerGap())
    );

    torrentFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    torrentFileChooser.setCurrentDirectory(null);

    subtitleFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    subtitleFileChooser.setCurrentDirectory(null);

    authenticationMessageLabel.setText(bundle.getString("GUI.authenticationMessageLabel.text")); // NOI18N

    authenticationUsernameLabel.setText(bundle.getString("GUI.authenticationUsernameLabel.text")); // NOI18N

    authenticationUsernameTextField.setText(null);
    authenticationUsernameTextField.addAncestorListener(new AncestorListener() {
      public void ancestorAdded(AncestorEvent evt) {
        authenticationUsernameTextFieldAncestorAdded(evt);
      }
      public void ancestorMoved(AncestorEvent evt) {
      }
      public void ancestorRemoved(AncestorEvent evt) {
      }
    });

    authenticationPasswordLabel.setText(bundle.getString("GUI.authenticationPasswordLabel.text")); // NOI18N

    authenticationPasswordField.setText(null);
    authenticationPasswordField.setEchoChar('\u2022');
    authenticationPasswordField.addAncestorListener(new AncestorListener() {
      public void ancestorAdded(AncestorEvent evt) {
        authenticationPasswordFieldAncestorAdded(evt);
      }
      public void ancestorMoved(AncestorEvent evt) {
      }
      public void ancestorRemoved(AncestorEvent evt) {
      }
    });

    GroupLayout authenticationPanelLayout = new GroupLayout(authenticationPanel);
    authenticationPanel.setLayout(authenticationPanelLayout);
    authenticationPanelLayout.setHorizontalGroup(authenticationPanelLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(authenticationPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(authenticationPanelLayout.createParallelGroup(Alignment.TRAILING)
          .addComponent(authenticationMessageLabel, GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
          .addGroup(Alignment.LEADING, authenticationPanelLayout.createSequentialGroup()
            .addGroup(authenticationPanelLayout.createParallelGroup(Alignment.LEADING)
              .addComponent(authenticationPasswordLabel)
              .addComponent(authenticationUsernameLabel))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(authenticationPanelLayout.createParallelGroup(Alignment.LEADING)
              .addComponent(authenticationUsernameTextField)
              .addComponent(authenticationPasswordField))))
        .addGap(10, 10, 10))
    );

    authenticationPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {authenticationPasswordLabel, authenticationUsernameLabel});

    authenticationPanelLayout.setVerticalGroup(authenticationPanelLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(authenticationPanelLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(authenticationMessageLabel)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(authenticationPanelLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(authenticationUsernameLabel)
          .addComponent(authenticationUsernameTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(authenticationPanelLayout.createParallelGroup(Alignment.BASELINE)
          .addComponent(authenticationPasswordLabel)
          .addComponent(authenticationPasswordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addContainerGap())
    );

    authenticationPanelLayout.linkSize(SwingConstants.VERTICAL, new Component[] {authenticationPasswordField, authenticationUsernameTextField});

    listPopupMenu.addPopupMenuListener(new PopupMenuListener() {
      public void popupMenuCanceled(PopupMenuEvent evt) {
      }
      public void popupMenuWillBecomeInvisible(PopupMenuEvent evt) {
      }
      public void popupMenuWillBecomeVisible(PopupMenuEvent evt) {
        listPopupMenuPopupMenuWillBecomeVisible(evt);
      }
    });

    listCutMenuItem.setText(bundle.getString("GUI.listCutMenuItem.text")); // NOI18N
    listCutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        listCutMenuItemActionPerformed(evt);
      }
    });
    listPopupMenu.add(listCutMenuItem);

    listCopyMenuItem.setText(bundle.getString("GUI.listCopyMenuItem.text")); // NOI18N
    listCopyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        listCopyMenuItemActionPerformed(evt);
      }
    });
    listPopupMenu.add(listCopyMenuItem);

    listDeleteMenuItem.setText(bundle.getString("GUI.listDeleteMenuItem.text")); // NOI18N
    listDeleteMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        listDeleteMenuItemActionPerformed(evt);
      }
    });
    listPopupMenu.add(listDeleteMenuItem);
    listPopupMenu.add(listPopupMenuSeparator1);

    listSelectAllMenuItem.setText(bundle.getString("GUI.listSelectAllMenuItem.text")); // NOI18N
    listSelectAllMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        listSelectAllMenuItemActionPerformed(evt);
      }
    });
    listPopupMenu.add(listSelectAllMenuItem);

    hideMenuItem.setText(bundle.getString("GUI.hideMenuItem.text")); // NOI18N
    hideMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        hideMenuItemActionPerformed(evt);
      }
    });
    connectionIssueButtonPopupMenu.add(hideMenuItem);

    playlistPlayMenuItem.setToolTipText(bundle.getString("GUI.playlistPlayMenuItem.toolTipText")); // NOI18N
    playlistPlayMenuItem.setEnabled(false);
    playlistPlayMenuItem.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent evt) {
        playlistPlayMenuItemMousePressed(evt);
      }
    });
    playlistPlayMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistPlayMenuItemActionPerformed(evt);
      }
    });
    playlistTablePopupMenu.add(playlistPlayMenuItem);

    playlistOpenMenuItem.setText(bundle.getString("GUI.playlistOpenMenuItem.text")); // NOI18N
    playlistOpenMenuItem.setEnabled(false);
    playlistOpenMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistOpenMenuItemActionPerformed(evt);
      }
    });
    playlistTablePopupMenu.add(playlistOpenMenuItem);

    playlistMoveUpMenuItem.setText(bundle.getString("GUI.playlistMoveUpMenuItem.text")); // NOI18N
    playlistMoveUpMenuItem.setEnabled(false);
    playlistMoveUpMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistMoveUpMenuItemActionPerformed(evt);
      }
    });
    playlistTablePopupMenu.add(playlistMoveUpMenuItem);

    playlistMoveDownMenuItem.setText(bundle.getString("GUI.playlistMoveDownMenuItem.text")); // NOI18N
    playlistMoveDownMenuItem.setEnabled(false);
    playlistMoveDownMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistMoveDownMenuItemActionPerformed(evt);
      }
    });
    playlistTablePopupMenu.add(playlistMoveDownMenuItem);
    playlistTablePopupMenu.add(playlistTablePopupMenuSeparator1);

    playlistCopyMenu.setText(bundle.getString("GUI.copyMenu.text")); // NOI18N

    playlistCopySelectionMenuItem.setText(bundle.getString("GUI.copySelectionMenuItem.text")); // NOI18N
    playlistCopySelectionMenuItem.setEnabled(false);
    playlistCopySelectionMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistCopySelectionMenuItemActionPerformed(evt);
      }
    });
    playlistCopyMenu.add(playlistCopySelectionMenuItem);
    playlistCopyMenu.add(playlistCopySeparator);

    playlistCopyDownloadLinkMenuItem.setText(bundle.getString("GUI.copyDownloadLink1MenuItem.text")); // NOI18N
    playlistCopyDownloadLinkMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistCopyDownloadLinkMenuItemActionPerformed(evt);
      }
    });
    playlistCopyMenu.add(playlistCopyDownloadLinkMenuItem);

    playlistTablePopupMenu.add(playlistCopyMenu);
    playlistTablePopupMenu.add(playlistTablePopupMenuSeparator2);

    playlistRemoveMenuItem.setText(bundle.getString("GUI.playlistRemoveMenuItem.text")); // NOI18N
    playlistRemoveMenuItem.setEnabled(false);
    playlistRemoveMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistRemoveMenuItemActionPerformed(evt);
      }
    });
    playlistTablePopupMenu.add(playlistRemoveMenuItem);

    playlistReloadGroupMenuItem.setText(bundle.getString("GUI.playlistReloadGroupMenuItem.text")); // NOI18N
    playlistReloadGroupMenuItem.setEnabled(false);
    playlistReloadGroupMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistReloadGroupMenuItemActionPerformed(evt);
      }
    });
    playlistTablePopupMenu.add(playlistReloadGroupMenuItem);
    playlistTablePopupMenu.add(playlistTablePopupMenuSeparator3);

    playlistBanGroupMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistBanGroupMenuItemActionPerformed(evt);
      }
    });
    playlistTablePopupMenu.add(playlistBanGroupMenuItem);

    resultsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    resultsScrollPane.setAutoscrolls(true);

    resultsTable.setAutoCreateRowSorter(true);
    resultsTable.setModel(new DefaultTableModel(
      new Object [][] {

      },
      new String [] {
        "", "Title", "Year", "Rating", "", "", "", "", "", "", "", "", ""
      }
    ) {
      Class[] types = new Class [] {
        Object.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class
      };
      boolean[] canEdit = new boolean [] {
        false, false, false, false, false, false, false, false, false, false, false, false, false
      };

      public Class getColumnClass(int columnIndex) {
        return types [columnIndex];
      }

      public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit [columnIndex];
      }
    });
    resultsTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    resultsTable.setFillsViewportHeight(true);
    resultsTable.setMaximumSize(new Dimension(32767, 32767));
    resultsTable.setMinimumSize(new Dimension(24, 24));
    resultsTable.setName("Search Results"); // NOI18N
    resultsTable.setOpaque(false);
    resultsTable.setPreferredSize(null);
    resultsTable.setRowHeight(90);
    resultsTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent evt) {
        resultsTableMouseClicked(evt);
      }
    });
    resultsTable.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        resultsTableKeyPressed(evt);
      }
    });
    resultsScrollPane.setViewportView(resultsTable);
    if (resultsTable.getColumnModel().getColumnCount() > 0) {
      resultsTable.getColumnModel().getColumn(0).setMinWidth(61);
      resultsTable.getColumnModel().getColumn(0).setPreferredWidth(61);
      resultsTable.getColumnModel().getColumn(0).setMaxWidth(61);
      resultsTable.getColumnModel().getColumn(0).setHeaderValue(Constant.IMAGE_COL);
      resultsTable.getColumnModel().getColumn(1).setPreferredWidth(798);
      resultsTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("GUI.resultsTable.columnModel.title1")); // NOI18N
      resultsTable.getColumnModel().getColumn(2).setMinWidth(65);
      resultsTable.getColumnModel().getColumn(2).setPreferredWidth(65);
      resultsTable.getColumnModel().getColumn(2).setMaxWidth(100);
      resultsTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("GUI.resultsTable.columnModel.title2")); // NOI18N
      resultsTable.getColumnModel().getColumn(3).setMinWidth(65);
      resultsTable.getColumnModel().getColumn(3).setPreferredWidth(65);
      resultsTable.getColumnModel().getColumn(3).setMaxWidth(100);
      resultsTable.getColumnModel().getColumn(3).setHeaderValue(bundle.getString("GUI.resultsTable.columnModel.title3")); // NOI18N
      resultsTable.getColumnModel().getColumn(4).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(4).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(4).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(4).setHeaderValue(Constant.ID_COL);
      resultsTable.getColumnModel().getColumn(5).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(5).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(5).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(5).setHeaderValue(Constant.CURR_TITLE_COL);
      resultsTable.getColumnModel().getColumn(6).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(6).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(6).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(6).setHeaderValue(Constant.OLD_TITLE_COL);
      resultsTable.getColumnModel().getColumn(7).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(7).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(7).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(7).setHeaderValue(Constant.SUMMARY_COL);
      resultsTable.getColumnModel().getColumn(8).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(8).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(8).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(8).setHeaderValue(Constant.IMAGE_LINK_COL);
      resultsTable.getColumnModel().getColumn(9).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(9).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(9).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(9).setHeaderValue(Constant.IS_TV_SHOW_COL);
      resultsTable.getColumnModel().getColumn(10).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(10).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(10).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(10).setHeaderValue(Constant.IS_TV_SHOW_AND_MOVIE_COL);
      resultsTable.getColumnModel().getColumn(11).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(11).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(11).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(11).setHeaderValue(Constant.SEASON_COL);
      resultsTable.getColumnModel().getColumn(12).setMinWidth(0);
      resultsTable.getColumnModel().getColumn(12).setPreferredWidth(0);
      resultsTable.getColumnModel().getColumn(12).setMaxWidth(0);
      resultsTable.getColumnModel().getColumn(12).setHeaderValue(Constant.EPISODE_COL);
    }

    readSummaryButton.setText(bundle.getString("GUI.readSummaryButton.text")); // NOI18N
    readSummaryButton.setToolTipText(bundle.getString("GUI.readSummaryButton.toolTipText")); // NOI18N
    readSummaryButton.setEnabled(false);
    readSummaryButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        readSummaryButtonActionPerformed(evt);
      }
    });

    watchTrailerButton.setText(bundle.getString("GUI.watchTrailerButton.text")); // NOI18N
    watchTrailerButton.setToolTipText(bundle.getString("GUI.watchTrailerButton.toolTipText")); // NOI18N
    watchTrailerButton.setEnabled(false);
    watchTrailerButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        watchTrailerButtonActionPerformed(evt);
      }
    });

    downloadLink1Button.setText(bundle.getString("GUI.downloadLink1Button.text")); // NOI18N
    downloadLink1Button.setToolTipText(bundle.getString("GUI.downloadLink1Button.toolTipText")); // NOI18N
    downloadLink1Button.setEnabled(false);
    downloadLink1Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadLink1ButtonActionPerformed(evt);
      }
    });

    downloadLink2Button.setText(bundle.getString("GUI.downloadLink2Button.text")); // NOI18N
    downloadLink2Button.setToolTipText(bundle.getString("GUI.downloadLink2Button.toolTipText")); // NOI18N
    downloadLink2Button.setEnabled(false);
    downloadLink2Button.setMargin(new Insets(0, 2, 0, 2));
    downloadLink2Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadLink2ButtonActionPerformed(evt);
      }
    });

    exitBackupModeButton.setBorderPainted(false);
    exitBackupModeButton.setEnabled(false);
    exitBackupModeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        exitBackupModeButtonActionPerformed(evt);
      }
    });

    loadMoreResultsButton.setText(bundle.getString("GUI.loadMoreResultsButton.text")); // NOI18N
    loadMoreResultsButton.setToolTipText(bundle.getString("GUI.loadMoreResultsButton.toolTipText")); // NOI18N
    loadMoreResultsButton.setEnabled(false);
    loadMoreResultsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        loadMoreResultsButtonActionPerformed(evt);
      }
    });

    summaryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    summaryEditorPane.setEditable(false);
    summaryEditorPane.setContentType("text/html"); // NOI18N
    summaryScrollPane.setViewportView(summaryEditorPane);

    findTextField.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        findTextFieldKeyPressed(evt);
      }
    });

    GroupLayout resultsPanelLayout = new GroupLayout(resultsPanel);
    resultsPanel.setLayout(resultsPanelLayout);
    resultsPanelLayout.setHorizontalGroup(resultsPanelLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(Alignment.TRAILING, resultsPanelLayout.createSequentialGroup()
        .addGap(0, 0, 0)
        .addGroup(resultsPanelLayout.createParallelGroup(Alignment.TRAILING)
          .addComponent(resultsScrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 693, Short.MAX_VALUE)
          .addGroup(resultsPanelLayout.createSequentialGroup()
            .addComponent(readSummaryButton)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(watchTrailerButton)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(downloadLink1Button)
            .addGap(1, 1, 1)
            .addComponent(downloadLink2Button)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(exitBackupModeButton)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(resultsPanelLayout.createParallelGroup(Alignment.LEADING, false)
          .addComponent(summaryScrollPane, GroupLayout.PREFERRED_SIZE, 615, GroupLayout.PREFERRED_SIZE)
          .addGroup(Alignment.TRAILING, resultsPanelLayout.createSequentialGroup()
            .addComponent(findTextField)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(loadMoreResultsButton)))
        .addGap(0, 0, 0))
    );

    resultsPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {downloadLink1Button, readSummaryButton, watchTrailerButton});

    resultsPanelLayout.setVerticalGroup(resultsPanelLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(resultsPanelLayout.createSequentialGroup()
        .addGap(0, 0, 0)
        .addGroup(resultsPanelLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(summaryScrollPane)
          .addComponent(resultsScrollPane, GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE))
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(resultsPanelLayout.createParallelGroup(Alignment.TRAILING)
          .addGroup(resultsPanelLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(exitBackupModeButton, Alignment.TRAILING)
            .addGroup(resultsPanelLayout.createParallelGroup(Alignment.BASELINE)
              .addComponent(readSummaryButton)
              .addComponent(watchTrailerButton)
              .addComponent(downloadLink1Button)
              .addComponent(downloadLink2Button)))
          .addGroup(resultsPanelLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(loadMoreResultsButton)
            .addComponent(findTextField, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE))))
    );

    resultsPanelLayout.linkSize(SwingConstants.VERTICAL, new Component[] {downloadLink1Button, downloadLink2Button, exitBackupModeButton, loadMoreResultsButton, readSummaryButton, watchTrailerButton});

    playlistScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    playlistScrollPane.setAutoscrolls(true);

    playlistTable.setAutoCreateRowSorter(true);
    playlistTable.setModel(new DefaultTableModel(
      new Object [][] {

      },
      new String [] {
        "Name", "Size", "Progress", ""
      }
    ) {
      Class[] types = new Class [] {
        String.class, Object.class, Object.class, Object.class
      };
      boolean[] canEdit = new boolean [] {
        false, false, false, false
      };

      public Class getColumnClass(int columnIndex) {
        return types [columnIndex];
      }

      public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit [columnIndex];
      }
    });
    playlistTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    playlistTable.setFillsViewportHeight(true);
    playlistTable.setMaximumSize(new Dimension(32767, 32767));
    playlistTable.setMinimumSize(new Dimension(24, 24));
    playlistTable.setName("Playlist"); // NOI18N
    playlistTable.setOpaque(false);
    playlistTable.setPreferredSize(null);
    playlistTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent evt) {
        playlistTableMouseClicked(evt);
      }
    });
    playlistTable.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        playlistTableKeyPressed(evt);
      }
    });
    playlistScrollPane.setViewportView(playlistTable);
    if (playlistTable.getColumnModel().getColumnCount() > 0) {
      playlistTable.getColumnModel().getColumn(0).setPreferredWidth(761);
      playlistTable.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("GUI.playlistTable.columnModel.title0")); // NOI18N
      playlistTable.getColumnModel().getColumn(1).setMinWidth(70);
      playlistTable.getColumnModel().getColumn(1).setPreferredWidth(70);
      playlistTable.getColumnModel().getColumn(1).setMaxWidth(105);
      playlistTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("GUI.playlistTable.columnModel.title1")); // NOI18N
      playlistTable.getColumnModel().getColumn(2).setMinWidth(158);
      playlistTable.getColumnModel().getColumn(2).setPreferredWidth(158);
      playlistTable.getColumnModel().getColumn(2).setMaxWidth(193);
      playlistTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("GUI.playlistTable.columnModel.title2")); // NOI18N
      playlistTable.getColumnModel().getColumn(3).setMinWidth(0);
      playlistTable.getColumnModel().getColumn(3).setPreferredWidth(0);
      playlistTable.getColumnModel().getColumn(3).setMaxWidth(0);
      playlistTable.getColumnModel().getColumn(3).setHeaderValue(Constant.PLAYLIST_ITEM_COL);
    }

    playlistPlayButton.setText(null);
    playlistPlayButton.setToolTipText(bundle.getString("GUI.playlistPlayButton.toolTipText")); // NOI18N
    playlistPlayButton.setEnabled(false);
    playlistPlayButton.setMargin(new Insets(0, 0, 0, 0));
    playlistPlayButton.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent evt) {
        playlistPlayButtonMousePressed(evt);
      }
    });
    playlistPlayButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistPlayButtonActionPerformed(evt);
      }
    });

    playlistFindTextField.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        playlistFindTextFieldKeyPressed(evt);
      }
    });

    playlistOpenButton.setText(null);
    playlistOpenButton.setToolTipText(bundle.getString("GUI.playlistOpenButton.toolTipText")); // NOI18N
    playlistOpenButton.setEnabled(false);
    playlistOpenButton.setMargin(new Insets(0, 0, 0, 0));
    playlistOpenButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistOpenButtonActionPerformed(evt);
      }
    });

    playlistMoveUpButton.setText(null);
    playlistMoveUpButton.setToolTipText(bundle.getString("GUI.playlistMoveUpButton.toolTipText")); // NOI18N
    playlistMoveUpButton.setEnabled(false);
    playlistMoveUpButton.setMargin(new Insets(0, 0, 0, 0));
    playlistMoveUpButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistMoveUpButtonActionPerformed(evt);
      }
    });

    playlistMoveDownButton.setText(null);
    playlistMoveDownButton.setToolTipText(bundle.getString("GUI.playlistMoveDownButton.toolTipText")); // NOI18N
    playlistMoveDownButton.setEnabled(false);
    playlistMoveDownButton.setMargin(new Insets(0, 0, 0, 0));
    playlistMoveDownButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistMoveDownButtonActionPerformed(evt);
      }
    });

    playlistRemoveButton.setText(null);
    playlistRemoveButton.setToolTipText(bundle.getString("GUI.playlistRemoveButton.toolTipText")); // NOI18N
    playlistRemoveButton.setEnabled(false);
    playlistRemoveButton.setMargin(new Insets(0, 0, 0, 0));
    playlistRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistRemoveButtonActionPerformed(evt);
      }
    });

    playlistReloadGroupButton.setText(null);
    playlistReloadGroupButton.setToolTipText(bundle.getString("GUI.playlistReloadGroupButton.toolTipText")); // NOI18N
    playlistReloadGroupButton.setEnabled(false);
    playlistReloadGroupButton.setMargin(new Insets(0, 0, 0, 0));
    playlistReloadGroupButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistReloadGroupButtonActionPerformed(evt);
      }
    });

    playlistBanGroupButton.setText(null);
    playlistBanGroupButton.setToolTipText(bundle.getString("GUI.playlistBanGroupButton.toolTipText")); // NOI18N
    playlistBanGroupButton.setEnabled(false);
    playlistBanGroupButton.setMargin(new Insets(0, 0, 0, 0));
    playlistBanGroupButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistBanGroupButtonActionPerformed(evt);
      }
    });

    GroupLayout playlistPanelLayout = new GroupLayout(playlistPanel);
    playlistPanel.setLayout(playlistPanelLayout);
    playlistPanelLayout.setHorizontalGroup(playlistPanelLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(playlistPanelLayout.createSequentialGroup()
        .addGap(0, 0, 0)
        .addGroup(playlistPanelLayout.createParallelGroup(Alignment.LEADING)
          .addComponent(playlistScrollPane, GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE)
          .addGroup(playlistPanelLayout.createSequentialGroup()
            .addComponent(playlistPlayButton)
            .addGap(18, 18, 18)
            .addComponent(playlistFindTextField)
            .addGap(18, 18, 18)
            .addComponent(playlistOpenButton)
            .addGap(18, 18, 18)
            .addComponent(playlistMoveUpButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(playlistMoveDownButton)
            .addGap(18, 18, 18)
            .addComponent(playlistRemoveButton)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(playlistReloadGroupButton)
            .addGap(18, 18, 18)
            .addComponent(playlistBanGroupButton)))
        .addGap(0, 0, 0))
    );
    playlistPanelLayout.setVerticalGroup(playlistPanelLayout.createParallelGroup(Alignment.LEADING)
      .addGroup(playlistPanelLayout.createSequentialGroup()
        .addGap(0, 0, 0)
        .addComponent(playlistScrollPane, GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addGroup(playlistPanelLayout.createParallelGroup(Alignment.LEADING)
          .addGroup(playlistPanelLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(playlistFindTextField, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
            .addComponent(playlistOpenButton))
          .addGroup(playlistPanelLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(playlistPlayButton)
            .addComponent(playlistMoveUpButton)
            .addComponent(playlistMoveDownButton)
            .addComponent(playlistRemoveButton)
            .addComponent(playlistReloadGroupButton)
            .addComponent(playlistBanGroupButton)))
        .addGap(0, 0, 0))
    );

    playlistPanelLayout.linkSize(SwingConstants.VERTICAL, new Component[] {playlistFindTextField, playlistMoveDownButton, playlistMoveUpButton, playlistOpenButton, playlistPlayButton, playlistRemoveButton});

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setTitle(Constant.APP_TITLE);
    setMinimumSize(null);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        formWindowClosing(evt);
      }
    });

    titleTextField.setText(null);

    titleLabel.setLabelFor(titleTextField);
    titleLabel.setText(bundle.getString("GUI.titleLabel.text")); // NOI18N
    titleLabel.setToolTipText(bundle.getString("GUI.titleLabel.toolTipText")); // NOI18N

    releasedLabel.setLabelFor(startDateChooser);
    releasedLabel.setText(bundle.getString("GUI.releasedLabel.text")); // NOI18N
    releasedLabel.setToolTipText(bundle.getString("GUI.releasedLabel.toolTipText")); // NOI18N

    genreLabel.setLabelFor(genreList);
    genreLabel.setText(bundle.getString("GUI.genreLabel.text")); // NOI18N
    genreLabel.setToolTipText(bundle.getString("GUI.genreLabel.toolTipText")); // NOI18N

    ratingComboBox.setMaximumRowCount(11);
    List<String> ratings = new ArrayList<String>(100);
    ratings.add(Str.str("any"));
    Collections.addAll(ratings, Regex.split(360, Constant.SEPARATOR1));
    UI.init(ratingComboBox, ratings.toArray(Constant.EMPTY_STRS));

    ratingLabel.setLabelFor(ratingComboBox);
    ratingLabel.setText(bundle.getString("GUI.ratingLabel.text")); // NOI18N
    ratingLabel.setToolTipText(bundle.getString("GUI.ratingLabel.toolTipText")); // NOI18N

    searchButton.setText(bundle.getString("GUI.searchButton.text")); // NOI18N
    searchButton.setToolTipText(bundle.getString("GUI.searchButton.toolTipText")); // NOI18N
    searchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        searchButtonActionPerformed(evt);
      }
    });

    List<String> genres = new ArrayList<String>(32);
    genres.add(Str.str("any"));
    Collections.addAll(genres, Regex.split(865, Constant.SEPARATOR1));
    UI.init(genreList, genres.toArray(Constant.EMPTY_STRS));
    genreList.setSelectedValue(Str.str("any"), true);
    genreList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent evt) {
        genreListValueChanged(evt);
      }
    });
    genreScrollPane.setViewportView(genreList);

    typeLabel.setLabelFor(typeComboBox);
    typeLabel.setText(bundle.getString("GUI.typeLabel.text")); // NOI18N
    typeLabel.setToolTipText(bundle.getString("GUI.typeLabel.toolTipText")); // NOI18N

    UI.init(typeComboBox, Str.strs("GUI.typeComboBox.model"));

    releasedToLabel.setLabelFor(endDateChooser);
    releasedToLabel.setText(bundle.getString("GUI.releasedToLabel.text")); // NOI18N

    popularPopupMenuButton.setText(null);
    popularPopupMenuButton.setMargin(new Insets(0, 0, 0, 0));
    popularPopupMenuButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        popularPopupMenuButtonActionPerformed(evt);
      }
    });

    loadingLabel.setText(null);

    statusBarTextField.setEditable(false);
    statusBarTextField.setFont(new Font("Verdana", 0, 10)); // NOI18N
    statusBarTextField.setText(null);
    statusBarTextField.setBorder(BorderFactory.createEtchedBorder());

    searchProgressTextField.setEditable(false);
    searchProgressTextField.setFont(new Font("Verdana", 0, 10)); // NOI18N
    searchProgressTextField.setHorizontalAlignment(JTextField.RIGHT);
    searchProgressTextField.setText(null);
    searchProgressTextField.setBorder(BorderFactory.createEtchedBorder());
    searchProgressUpdate(0, 0);

    connectionIssueButton.setText(null);
    connectionIssueButton.setBorder(BorderFactory.createEtchedBorder());
    connectionIssueButton.setEnabled(false);
    connectionIssueButton.setMargin(new Insets(0, 0, 0, 0));
    connectionIssueButton.setMaximumSize(new Dimension(18, 18));
    connectionIssueButton.setPreferredSize(new Dimension(0, 0));
    connectionIssueButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        connectionIssueButtonActionPerformed(evt);
      }
    });

    splitPane.setDividerLocation(Integer.MAX_VALUE);
    splitPane.setDividerSize(10);
    splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    splitPane.setResizeWeight(0.6893004115226338);
    splitPane.setContinuousLayout(true);
    Container divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
    List<Component> dividerComponents = new ArrayList<Component>(3);
    dividerComponents.add(divider);
    Collections.addAll(dividerComponents, divider.getComponents());
    if (dividerComponents.size() == 3) {
      dividerComponents.remove(2);
    }
    for (Component dividerComponent : dividerComponents) {
      dividerComponent.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent evt) {
          if (playlistShown.compareAndSet(false, true)) {
            playlistMenuItemActionPerformed(null);
          }
        }
      });
    }
    playlistPanel.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent evt) {
        if (!playlistShown.get()) {
          splitPane.setDividerLocation(Integer.MAX_VALUE);
        }
      }
    });

    fileMenu.setText(bundle.getString("GUI.fileMenu.text")); // NOI18N

    profileMenu.setText(bundle.getString("GUI.profileMenu.text")); // NOI18N
    profileMenu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent evt) {
      }
      public void menuDeselected(MenuEvent evt) {
      }
      public void menuSelected(MenuEvent evt) {
        profileMenuMenuSelected(evt);
      }
    });

    profile0MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
    profile0MenuItem.setText(bundle.getString("GUI.profile0MenuItem.text")); // NOI18N
    profile0MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_MASK), "doClick");
    profile0MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile0MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile0MenuItem);

    profile1MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK));
    profile1MenuItem.setText(bundle.getString("GUI.profile1MenuItem.text")); // NOI18N
    profile1MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1, InputEvent.CTRL_MASK), "doClick");
    profile1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile1MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile1MenuItem);

    profile2MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK));
    profile2MenuItem.setText(bundle.getString("GUI.profile2MenuItem.text")); // NOI18N
    profile2MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, InputEvent.CTRL_MASK), "doClick");
    profile2MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile2MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile2MenuItem);

    profile3MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_DOWN_MASK));
    profile3MenuItem.setText(bundle.getString("GUI.profile3MenuItem.text")); // NOI18N
    profile3MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD3, InputEvent.CTRL_MASK), "doClick");
    profile3MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile3MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile3MenuItem);

    profile4MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_DOWN_MASK));
    profile4MenuItem.setText(bundle.getString("GUI.profile4MenuItem.text")); // NOI18N
    profile4MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, InputEvent.CTRL_MASK), "doClick");
    profile4MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile4MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile4MenuItem);

    profile5MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.CTRL_DOWN_MASK));
    profile5MenuItem.setText(bundle.getString("GUI.profile5MenuItem.text")); // NOI18N
    profile5MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD5, InputEvent.CTRL_MASK), "doClick");
    profile5MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile5MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile5MenuItem);

    profile6MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.CTRL_DOWN_MASK));
    profile6MenuItem.setText(bundle.getString("GUI.profile6MenuItem.text")); // NOI18N
    profile6MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD6, InputEvent.CTRL_MASK), "doClick");
    profile6MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile6MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile6MenuItem);

    profile7MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_DOWN_MASK));
    profile7MenuItem.setText(bundle.getString("GUI.profile7MenuItem.text")); // NOI18N
    profile7MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD7, InputEvent.CTRL_MASK), "doClick");
    profile7MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile7MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile7MenuItem);

    profile8MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.CTRL_DOWN_MASK));
    profile8MenuItem.setText(bundle.getString("GUI.profile8MenuItem.text")); // NOI18N
    profile8MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD8, InputEvent.CTRL_MASK), "doClick");
    profile8MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile8MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile8MenuItem);

    profile9MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.CTRL_DOWN_MASK));
    profile9MenuItem.setText(bundle.getString("GUI.profile9MenuItem.text")); // NOI18N
    profile9MenuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD9, InputEvent.CTRL_MASK), "doClick");
    profile9MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        profile9MenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(profile9MenuItem);
    profileMenu.add(profileMenuSeparator1);

    editProfilesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    editProfilesMenuItem.setText(bundle.getString("GUI.editProfilesMenuItem.text")); // NOI18N
    editProfilesMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        editProfilesMenuItemActionPerformed(evt);
      }
    });
    profileMenu.add(editProfilesMenuItem);

    fileMenu.add(profileMenu);
    fileMenu.add(fileMenuSeparator1);

    printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
    printMenuItem.setText(bundle.getString("GUI.printMenuItem.text")); // NOI18N
    printMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        printMenuItemActionPerformed(evt);
      }
    });
    fileMenu.add(printMenuItem);
    fileMenu.add(fileMenuSeparator2);

    iconifyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    iconifyMenuItem.setText(bundle.getString("GUI.iconifyMenuItem.text")); // NOI18N
    fileMenu.add(iconifyMenuItem);

    exitMenuItem.setText(bundle.getString("GUI.exitMenuItem.text")); // NOI18N
    exitMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        exitMenuItemActionPerformed(evt);
      }
    });
    fileMenu.add(exitMenuItem);

    menuBar.add(fileMenu);

    editMenu.setText(bundle.getString("GUI.editMenu.text")); // NOI18N
    editMenu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent evt) {
      }
      public void menuDeselected(MenuEvent evt) {
      }
      public void menuSelected(MenuEvent evt) {
        editMenuMenuSelected(evt);
      }
    });

    cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
    cutMenuItem.setText(bundle.getString("GUI.cutMenuItem.text")); // NOI18N
    cutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        cutMenuItemActionPerformed(evt);
      }
    });
    editMenu.add(cutMenuItem);

    copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
    copyMenuItem.setText(bundle.getString("GUI.copyMenuItem.text")); // NOI18N
    copyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        copyMenuItemActionPerformed(evt);
      }
    });
    editMenu.add(copyMenuItem);

    pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
    pasteMenuItem.setText(bundle.getString("GUI.pasteMenuItem.text")); // NOI18N
    pasteMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        pasteMenuItemActionPerformed(evt);
      }
    });
    editMenu.add(pasteMenuItem);

    deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
    deleteMenuItem.setText(bundle.getString("GUI.deleteMenuItem.text")); // NOI18N
    deleteMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        deleteMenuItemActionPerformed(evt);
      }
    });
    editMenu.add(deleteMenuItem);
    editMenu.add(editMenuSeparator1);

    selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
    selectAllMenuItem.setText(bundle.getString("GUI.selectAllMenuItem.text")); // NOI18N
    selectAllMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        selectAllMenuItemActionPerformed(evt);
      }
    });
    editMenu.add(selectAllMenuItem);
    editMenu.add(editMenuSeparator2);

    findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
    findMenuItem.setText(bundle.getString("GUI.findMenuItem.text")); // NOI18N
    findMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        findMenuItemActionPerformed(evt);
      }
    });
    editMenu.add(findMenuItem);

    menuBar.add(editMenu);

    viewMenu.setText(bundle.getString("GUI.viewMenu.text")); // NOI18N

    languageMenu.setText(bundle.getString("GUI.languageMenu.text")); // NOI18N

    englishRadioButtonMenuItem.setText("English");
    englishRadioButtonMenuItem.setName("en_US"); // NOI18N
    languageMenu.add(englishRadioButtonMenuItem);

    spanishRadioButtonMenuItem.setText("espa\u00f1ol - Spanish");
    spanishRadioButtonMenuItem.setName("es_ES"); // NOI18N
    languageMenu.add(spanishRadioButtonMenuItem);

    frenchRadioButtonMenuItem.setText("fran\u00e7ais - French");
    frenchRadioButtonMenuItem.setName("fr_FR"); // NOI18N
    languageMenu.add(frenchRadioButtonMenuItem);

    italianRadioButtonMenuItem.setText("italiano - Italian");
    italianRadioButtonMenuItem.setName("it_IT"); // NOI18N
    languageMenu.add(italianRadioButtonMenuItem);

    dutchRadioButtonMenuItem.setText("Nederlands - Dutch");
    dutchRadioButtonMenuItem.setName("nl_NL"); // NOI18N
    languageMenu.add(dutchRadioButtonMenuItem);

    portugueseRadioButtonMenuItem.setText("portugu\u00eas - Portuguese");
    portugueseRadioButtonMenuItem.setName("pt_PT"); // NOI18N
    languageMenu.add(portugueseRadioButtonMenuItem);

    turkishRadioButtonMenuItem.setText("T\u00fcrk\u00e7e - Turkish");
    turkishRadioButtonMenuItem.setName("tr_TR"); // NOI18N
    languageMenu.add(turkishRadioButtonMenuItem);

    viewMenu.add(languageMenu);
    viewMenu.add(viewMenuSeparator1);

    resetWindowMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
    resetWindowMenuItem.setText(bundle.getString("GUI.resetWindowMenuItem.text")); // NOI18N
    resetWindowMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        resetWindowMenuItemActionPerformed(evt);
      }
    });
    viewMenu.add(resetWindowMenuItem);

    menuBar.add(viewMenu);

    searchMenu.setText(bundle.getString("GUI.searchMenu.text")); // NOI18N

    searchBanTitleMenu.setText(bundle.getString("GUI.searchBanTitleMenu.text")); // NOI18N
    searchBanTitleMenu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent evt) {
      }
      public void menuDeselected(MenuEvent evt) {
      }
      public void menuSelected(MenuEvent evt) {
        searchBanTitleMenuMenuSelected(evt);
      }
    });
    searchMenu.add(searchBanTitleMenu);

    searchBanTitleEnableCheckBoxMenuItem.setSelected(true);
    searchBanTitleEnableCheckBoxMenuItem.setText(bundle.getString("GUI.searchBanTitleEnableCheckBoxMenuItem.text")); // NOI18N
    searchMenu.add(searchBanTitleEnableCheckBoxMenuItem);
    searchMenu.add(searchMenuSeparator1);

    resultsPerSearchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
    resultsPerSearchMenuItem.setText(bundle.getString("GUI.resultsPerSearchMenuItem.text")); // NOI18N
    resultsPerSearchMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        resultsPerSearchMenuItemActionPerformed(evt);
      }
    });
    searchMenu.add(resultsPerSearchMenuItem);
    searchMenu.add(searchMenuSeparator2);

    timeoutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
    timeoutMenuItem.setText(bundle.getString("GUI.timeoutMenuItem.text")); // NOI18N
    timeoutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        timeoutMenuItemActionPerformed(evt);
      }
    });
    searchMenu.add(timeoutMenuItem);
    searchMenu.add(searchMenuSeparator3);

    proxyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
    proxyMenuItem.setText(bundle.getString("GUI.proxyMenuItem.text")); // NOI18N
    proxyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        proxyMenuItemActionPerformed(evt);
      }
    });
    searchMenu.add(proxyMenuItem);
    searchMenu.add(searchMenuSeparator4);

    languageCountryMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
    languageCountryMenuItem.setText(bundle.getString("GUI.languageCountryMenuItem.text")); // NOI18N
    languageCountryMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        languageCountryMenuItemActionPerformed(evt);
      }
    });
    searchMenu.add(languageCountryMenuItem);
    searchMenu.add(searchMenuSeparator5);

    feedCheckBoxMenuItem.setText(bundle.getString("GUI.feedCheckBoxMenuItem.text")); // NOI18N
    feedCheckBoxMenuItem.setToolTipText(bundle.getString("GUI.feedCheckBoxMenuItem.toolTipText")); // NOI18N
    searchMenu.add(feedCheckBoxMenuItem);
    searchMenu.add(searchMenuSeparator6);

    browserNotificationCheckBoxMenuItem.setSelected(true);
    browserNotificationCheckBoxMenuItem.setText(bundle.getString("GUI.browserNotificationCheckBoxMenuItem.text")); // NOI18N
    searchMenu.add(browserNotificationCheckBoxMenuItem);
    searchMenu.add(searchMenuSeparator7);

    emailWithDefaultAppCheckBoxMenuItem.setSelected(true);
    emailWithDefaultAppCheckBoxMenuItem.setText(bundle.getString("GUI.emailWithDefaultAppCheckBoxMenuItem.text")); // NOI18N
    searchMenu.add(emailWithDefaultAppCheckBoxMenuItem);
    searchMenu.add(searchMenuSeparator8);

    trailerPlayerMenu.setText(bundle.getString("GUI.trailerPlayerMenu.text")); // NOI18N

    trailerMediaPlayerRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
    trailerMediaPlayerRadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayerRadioButtonMenuItem.text")); // NOI18N
    trailerMediaPlayerRadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trailerPlayerRadioButtonMenuItemActionPerformed(evt);
      }
    });
    trailerPlayerMenu.add(trailerMediaPlayerRadioButtonMenuItem);

    trailerMediaPlayer1080RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));
    trailerMediaPlayer1080RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer1080RadioButtonMenuItem.text")); // NOI18N
    trailerMediaPlayer1080RadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trailerPlayerRadioButtonMenuItemActionPerformed(evt);
      }
    });
    trailerPlayerMenu.add(trailerMediaPlayer1080RadioButtonMenuItem);

    trailerMediaPlayer720RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
    trailerMediaPlayer720RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer720RadioButtonMenuItem.text")); // NOI18N
    trailerMediaPlayer720RadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trailerPlayerRadioButtonMenuItemActionPerformed(evt);
      }
    });
    trailerPlayerMenu.add(trailerMediaPlayer720RadioButtonMenuItem);

    trailerMediaPlayer480RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
    trailerMediaPlayer480RadioButtonMenuItem.setSelected(true);
    trailerMediaPlayer480RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer480RadioButtonMenuItem.text")); // NOI18N
    trailerMediaPlayer480RadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trailerPlayerRadioButtonMenuItemActionPerformed(evt);
      }
    });
    trailerPlayerMenu.add(trailerMediaPlayer480RadioButtonMenuItem);

    trailerMediaPlayer360RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK));
    trailerMediaPlayer360RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer360RadioButtonMenuItem.text")); // NOI18N
    trailerMediaPlayer360RadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trailerPlayerRadioButtonMenuItemActionPerformed(evt);
      }
    });
    trailerPlayerMenu.add(trailerMediaPlayer360RadioButtonMenuItem);

    trailerMediaPlayer240RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK));
    trailerMediaPlayer240RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer240RadioButtonMenuItem.text")); // NOI18N
    trailerMediaPlayer240RadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trailerPlayerRadioButtonMenuItemActionPerformed(evt);
      }
    });
    trailerPlayerMenu.add(trailerMediaPlayer240RadioButtonMenuItem);

    trailerWebBrowserPlayerRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
    trailerWebBrowserPlayerRadioButtonMenuItem.setText(bundle.getString("GUI.trailerWebBrowserPlayerRadioButtonMenuItem.text")); // NOI18N
    trailerWebBrowserPlayerRadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        trailerPlayerRadioButtonMenuItemActionPerformed(evt);
      }
    });
    trailerPlayerMenu.add(trailerWebBrowserPlayerRadioButtonMenuItem);

    searchMenu.add(trailerPlayerMenu);

    menuBar.add(searchMenu);

    playlistMenu.setText(bundle.getString("GUI.playlistMenu.text")); // NOI18N

    playlistMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
    playlistMenuItem.setText(bundle.getString("GUI.playlistMenuItem.text")); // NOI18N
    playlistMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistMenuItemActionPerformed(evt);
      }
    });
    playlistMenu.add(playlistMenuItem);
    playlistMenu.add(playlistMenuSeparator1);

    playlistSaveFolderMenuItem.setText(bundle.getString("GUI.playlistSaveFolderMenuItem.text")); // NOI18N
    playlistSaveFolderMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistSaveFolderMenuItemActionPerformed(evt);
      }
    });
    playlistMenu.add(playlistSaveFolderMenuItem);

    playlistAutoOpenCheckBoxMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
    playlistAutoOpenCheckBoxMenuItem.setSelected(true);
    playlistAutoOpenCheckBoxMenuItem.setText(bundle.getString("GUI.playlistAutoOpenCheckBoxMenuItem.text")); // NOI18N
    playlistMenu.add(playlistAutoOpenCheckBoxMenuItem);

    playlistPlayWithDefaultAppCheckBoxMenuItem.setText(bundle.getString("GUI.playlistPlayWithDefaultAppCheckBoxMenuItem.text")); // NOI18N
    playlistMenu.add(playlistPlayWithDefaultAppCheckBoxMenuItem);

    playlistShowNonVideoItemsCheckBoxMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
    playlistShowNonVideoItemsCheckBoxMenuItem.setText(bundle.getString("GUI.playlistShowNonVideoItemsCheckBoxMenuItem.text")); // NOI18N
    playlistShowNonVideoItemsCheckBoxMenuItem.setToolTipText(bundle.getString("GUI.playlistShowNonVideoItemsCheckBoxMenuItem.toolTipText")); // NOI18N
    playlistShowNonVideoItemsCheckBoxMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(evt);
      }
    });
    playlistMenu.add(playlistShowNonVideoItemsCheckBoxMenuItem);

    menuBar.add(playlistMenu);

    downloadMenu.setText(bundle.getString("GUI.downloadMenu.text")); // NOI18N

    downloadQualityMenu.setText(bundle.getString("GUI.downloadQualityMenu.text")); // NOI18N

    downloadAnyQualityRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    downloadAnyQualityRadioButtonMenuItem.setSelected(true);
    downloadAnyQualityRadioButtonMenuItem.setText(Str.str("any"));
    downloadAnyQualityRadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadQualityButtonMenuItemActionPerformed(evt);
      }
    });
    downloadQualityMenu.add(downloadAnyQualityRadioButtonMenuItem);

    downloadHighQualityRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    downloadHighQualityRadioButtonMenuItem.setText(Constant.HQ);
    downloadHighQualityRadioButtonMenuItem.setToolTipText(bundle.getString("GUI.downloadHighQualityRadioButtonMenuItem.toolTipText")); // NOI18N
    downloadHighQualityRadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadQualityButtonMenuItemActionPerformed(evt);
      }
    });
    downloadQualityMenu.add(downloadHighQualityRadioButtonMenuItem);

    downloadDVDQualityRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    downloadDVDQualityRadioButtonMenuItem.setText(Constant.DVD);
    downloadDVDQualityRadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadQualityButtonMenuItemActionPerformed(evt);
      }
    });
    downloadQualityMenu.add(downloadDVDQualityRadioButtonMenuItem);

    download720HDQualityRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    download720HDQualityRadioButtonMenuItem.setText(Constant.HD720);
    download720HDQualityRadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadQualityButtonMenuItemActionPerformed(evt);
      }
    });
    downloadQualityMenu.add(download720HDQualityRadioButtonMenuItem);

    download1080HDRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));
    download1080HDRadioButtonMenuItem.setText(Constant.HD1080);
    download1080HDRadioButtonMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadQualityButtonMenuItemActionPerformed(evt);
      }
    });
    downloadQualityMenu.add(download1080HDRadioButtonMenuItem);

    downloadMenu.add(downloadQualityMenu);
    downloadMenu.add(downloadMenuSeparator1);

    downloadSizeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
    downloadSizeMenuItem.setText(bundle.getString("GUI.downloadSizeMenuItem.text")); // NOI18N
    downloadSizeMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        downloadSizeMenuItemActionPerformed(evt);
      }
    });
    downloadMenu.add(downloadSizeMenuItem);
    downloadMenu.add(downloadMenuSeparator2);

    fileExtensionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
    fileExtensionsMenuItem.setText(bundle.getString("GUI.fileExtensionsMenuItem.text")); // NOI18N
    fileExtensionsMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        fileExtensionsMenuItemActionPerformed(evt);
      }
    });
    downloadMenu.add(fileExtensionsMenuItem);
    downloadMenu.add(downloadMenuSeparator3);

    safetyCheckBoxMenuItem.setSelected(true);
    safetyCheckBoxMenuItem.setText(bundle.getString("GUI.safetyCheckBoxMenuItem.text")); // NOI18N
    safetyCheckBoxMenuItem.setToolTipText(bundle.getString("GUI.safetyCheckBoxMenuItem.toolTipText")); // NOI18N
    downloadMenu.add(safetyCheckBoxMenuItem);

    peerBlockNotificationCheckBoxMenuItem.setSelected(true);
    peerBlockNotificationCheckBoxMenuItem.setText(bundle.getString("GUI.peerBlockNotificationCheckBoxMenuItem.text")); // NOI18N
    downloadMenu.add(peerBlockNotificationCheckBoxMenuItem);
    downloadMenu.add(downloadMenuSeparator4);

    portMenuItem.setText(bundle.getString("GUI.portMenuItem.text")); // NOI18N
    portMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        portMenuItemActionPerformed(evt);
      }
    });
    downloadMenu.add(portMenuItem);
    downloadMenu.add(downloadMenuSeparator5);

    downloaderMenu.setText(bundle.getString("GUI.downloaderMenu.text")); // NOI18N

    playlistDownloaderRadioButtonMenuItem.setSelected(true);
    playlistDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.playlistDownloaderRadioButtonMenuItem.text")); // NOI18N
    downloaderMenu.add(playlistDownloaderRadioButtonMenuItem);

    defaultApplicationDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.defaultApplicationDownloaderRadioButtonMenuItem.text")); // NOI18N
    downloaderMenu.add(defaultApplicationDownloaderRadioButtonMenuItem);

    noDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.noDownloaderRadioButtonMenuItem.text")); // NOI18N
    downloaderMenu.add(noDownloaderRadioButtonMenuItem);

    downloadMenu.add(downloaderMenu);

    menuBar.add(downloadMenu);

    helpMenu.setText(bundle.getString("GUI.helpMenu.text")); // NOI18N

    faqMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
    faqMenuItem.setText(bundle.getString("GUI.faqMenuItem.text")); // NOI18N
    faqMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        faqMenuItemActionPerformed(evt);
      }
    });
    helpMenu.add(faqMenuItem);
    helpMenu.add(helpMenuSeparator1);

    updateMenuItem.setText(bundle.getString("GUI.updateMenuItem.text")); // NOI18N
    updateMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        updateMenuItemActionPerformed(evt);
      }
    });
    helpMenu.add(updateMenuItem);

    updateCheckBoxMenuItem.setSelected(true);
    updateCheckBoxMenuItem.setText(bundle.getString("GUI.updateCheckBoxMenuItem.text")); // NOI18N
    helpMenu.add(updateCheckBoxMenuItem);
    helpMenu.add(helpMenuSeparator2);

    aboutMenuItem.setText(bundle.getString("GUI.aboutMenuItem.text")); // NOI18N
    aboutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        aboutMenuItemActionPerformed(evt);
      }
    });
    helpMenu.add(aboutMenuItem);

    menuBar.add(helpMenu);

    setJMenuBar(menuBar);

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
      .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
          .addComponent(splitPane, Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
              .addGroup(layout.createSequentialGroup()
                .addComponent(titleLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(titleTextField))
              .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(typeLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(typeComboBox, 0, 74, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(ratingLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(ratingComboBox, 0, 75, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(releasedLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(startDateChooser, GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                .addGap(7, 7, 7)
                .addComponent(releasedToLabel)
                .addGap(6, 6, 6)
                .addComponent(endDateChooser, GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)))
            .addGap(18, 18, 18)
            .addComponent(genreLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(genreScrollPane, GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
              .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(searchButton)
                .addGap(1, 1, 1)
                .addComponent(popularPopupMenuButton))
              .addComponent(loadingLabel, Alignment.TRAILING))))
        .addContainerGap())
      .addGroup(layout.createSequentialGroup()
        .addComponent(statusBarTextField)
        .addGap(0, 0, 0)
        .addComponent(connectionIssueButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addGap(0, 0, 0)
        .addComponent(searchProgressTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
    );
    layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
              .addComponent(titleLabel)
              .addComponent(titleTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
              .addComponent(genreLabel))
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(Alignment.TRAILING)
              .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(typeComboBox)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                  .addComponent(typeLabel)
                  .addComponent(ratingLabel)
                  .addComponent(ratingComboBox)
                  .addComponent(releasedLabel))
                .addComponent(startDateChooser, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(endDateChooser, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
              .addComponent(releasedToLabel)))
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
              .addComponent(searchButton)
              .addComponent(popularPopupMenuButton))
            .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(loadingLabel))
          .addComponent(genreScrollPane, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.UNRELATED)
        .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 615, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
          .addComponent(statusBarTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(searchProgressTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .addComponent(connectionIssueButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
    );

    layout.linkSize(SwingConstants.VERTICAL, new Component[] {endDateChooser, ratingComboBox, startDateChooser, titleTextField, typeComboBox});

    layout.linkSize(SwingConstants.VERTICAL, new Component[] {genreLabel, ratingLabel, releasedLabel});

    layout.linkSize(SwingConstants.VERTICAL, new Component[] {connectionIssueButton, searchProgressTextField, statusBarTextField});

    layout.linkSize(SwingConstants.VERTICAL, new Component[] {popularPopupMenuButton, searchButton});

    setSize(new Dimension(1160, 773));
    setLocationRelativeTo(null);
  }// </editor-fold>//GEN-END:initComponents

  public void saveUserSettings() {
    try {
      settings.saveSettings(Constant.APP_DIR + Constant.USER_SETTINGS);
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      try {
        IO.write(Constant.APP_DIR + Constant.USER_SETTINGS, IO.read(Constant.PROGRAM_DIR + Constant.DEFAULT_SETTINGS));
      } catch (Exception e2) {
        if (Debug.DEBUG) {
          Debug.print(e2);
        }
      }
    }

    for (Entry<String, Iterable<?>> bannedEntry : new Entry[]{new SimpleImmutableEntry<String, Iterable<?>>(Constant.BANNED_TITLES, bannedTitles),
      new SimpleImmutableEntry<String, Iterable<?>>(Constant.BANNED_DOWNLOAD_IDS, bannedDownloadIDs)}) {
      StringBuilder ids = new StringBuilder(96);
      for (Object id : bannedEntry.getValue()) {
        ids.append(id).append(Constant.NEWLINE);
      }
      try {
        IO.write(Constant.APP_DIR + bannedEntry.getKey(), ids.toString().trim());
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
  }

  public void savePlaylist() {
    if (!isPlaylistRestored.get()) {
      return;
    }

    StringBuilder playlist = new StringBuilder(1024);
    synchronized (playlistSyncTable.lock) {
      for (int row = 0, numRows = playlistSyncTable.tableModel.getRowCount(); row < numRows; row++) {
        PlaylistItem playlistItem = (PlaylistItem) playlistSyncTable.tableModel.getValueAt(row, playlistItemCol);
        playlist.append((String) playlistSyncTable.tableModel.getValueAt(row, playlistNameCol)).append(Constant.NEWLINE)
                .append(((FormattedNum) playlistSyncTable.tableModel.getValueAt(row, playlistSizeCol)).val().longValue()).append(Constant.NEWLINE)
                .append(((FormattedNum) playlistSyncTable.tableModel.getValueAt(row, playlistProgressCol)).val().doubleValue()).append(Constant.NEWLINE)
                .append(playlistItem.groupID()).append(Constant.NEWLINE).append(playlistItem.uri()).append(Constant.NEWLINE)
                .append(playlistItem.groupFile()).append(Constant.NEWLINE).append(playlistItem.groupIndex()).append(Constant.NEWLINE)
                .append(playlistItem.name()).append(Constant.NEWLINE);
      }
    }

    try {
      IO.write(Constant.APP_DIR + Constant.PLAYLIST, playlist.toString().trim());
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  private void restorePlaylist(boolean initPlaylist) {
    if (playlistRestorationLock.tryLock()) {
      try {
        if (isPlaylistRestored.get()) {
          return;
        }
        File playlist = new File(Constant.APP_DIR + Constant.PLAYLIST);
        boolean isFirstVersion = !playlist.exists();
        if (isFirstVersion) { // Backward compatibility
          playlist = new File(Constant.APP_DIR + "playlist" + Constant.TXT);
        }
        String[] playlistEntries;

        try {
          if ((isFirstVersion && !playlist.exists()) || (playlistEntries = Regex.split(IO.read(playlist), Constant.NEWLINE)).length % 8 != 0) {
            return;
          }

          if (initPlaylist) {
            workerListener.initPlaylist();
          }
          for (int i = 0; i < playlistEntries.length; i += 8) {
            String groupFileName = playlistEntries[i + 5];
            if (groupFileName.equals(Constant.NULL)) {
              workerListener.stream(playlistEntries[i + 4], playlistEntries[i]);
              continue;
            }
            File groupFile = new File(groupFileName);
            if (groupFile.exists()) {
              newPlaylistItem(makePlaylistRow(playlistEntries[i], workerListener.playlistItemSize(Long.parseLong(playlistEntries[i + 1])),
                      workerListener.playlistItemProgress(Double.parseDouble(playlistEntries[i + 2])), workerListener.playlistItem(playlistEntries[i
                      + 3], playlistEntries[i + 4], groupFile, Integer.parseInt(playlistEntries[i + 6]), playlistEntries[i + 7],
                      isFirstVersion)), -1);
            }
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        } finally {
          isPlaylistRestored.set(true);
          if (isFirstVersion) {
            IO.fileOp(playlist, IO.RM_FILE);
          }
        }
      } finally {
        playlistRestorationLock.unlock();
      }
    }
  }

    void searchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
      if (UI.stop(searchButton, new Runnable() {
        @Override
        public void run() {
          workerListener.searchStopped(isRegularSearcher);
        }
      })) {
        return;
      }

      findControl.hide(true);

      int numResultsPerSearch = Integer.parseInt((String) regularResultsPerSearchComboBox.getSelectedItem());
      Object type = typeComboBox.getSelectedItem();
      Boolean isTVShow = (Constant.ANY.equals(type) ? null : Constant.TV_SHOW.equals(type));
      Calendar startDate = ((DateChooser) startDateChooser).refreshDate(), endDate = ((DateChooser) endDateChooser).refreshDate();
      String title = titleTextField.getText().trim(), minRating = (String) ratingComboBox.getSelectedItem();
      String[] genres = UI.selectAnyIfNoSelectionAndCopy(genreList), languages = UI.selectAnyIfNoSelectionAndCopy(languageList), countries
              = UI.selectAnyIfNoSelectionAndCopy(countryList);

      isRegularSearcher = true;
      workerListener.regularSearchStarted(numResultsPerSearch, isTVShow, startDate, endDate, title, genres, languages, countries, minRating);
    }//GEN-LAST:event_searchButtonActionPerformed

    void loadMoreResultsButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_loadMoreResultsButtonActionPerformed
      workerListener.loadMoreSearchResults(isRegularSearcher);
    }//GEN-LAST:event_loadMoreResultsButtonActionPerformed

    void faqMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_faqMenuItemActionPerformed
      faqEditorPane.setSelectionStart(0);
      faqEditorPane.setSelectionEnd(0);
      UI.show((Window) faqFrame);
    }//GEN-LAST:event_faqMenuItemActionPerformed

    void aboutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
      aboutEditorPane.setSelectionStart(0);
      aboutEditorPane.setSelectionEnd(0);
      UI.show(aboutDialog);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    void exitMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
      System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    void printMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_printMenuItemActionPerformed
      if (resultsSyncTable.getRowCount() == 0) {
        showMsg(Str.str("noPrintResults"), Constant.INFO_MSG);
        return;
      }
      printMenuItem.setEnabled(false);
      printMenuItem.setText(Str.str("printing"));
      (new Worker() {
        @Override
        protected void doWork() {
          Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR), defaultCursor = Cursor.getDefaultCursor();
          rootPane.setCursor(waitCursor);
          resultsSyncTable.table.setCursor(waitCursor);
          Iterable<Window> windows = UI.hideNonModalDialogs();
          try {
            resultsSyncTable.table.print(PrintMode.FIT_WIDTH);
          } catch (Exception e) {
            showException(e);
          }
          for (Window window : windows) {
            window.setVisible(true);
          }
          rootPane.setCursor(defaultCursor);
          resultsSyncTable.table.setCursor(defaultCursor);
          printMenuItem.setText(Str.str("GUI.printMenuItem.text"));
          printMenuItem.setEnabled(true);
        }
      }).execute();
    }//GEN-LAST:event_printMenuItemActionPerformed

    void cutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cutMenuItemActionPerformed
      JTextField textField;
      if (titleTextField.getCaret().isSelectionVisible() && titleTextField.getSelectedText() != null) {
        textField = titleTextField;
      } else if (findTextField.getCaret().isSelectionVisible() && findTextField.getSelectedText() != null) {
        textField = findTextField;
      } else {
        return;
      }

      TransferHandler th = textField.getTransferHandler();
      if (th != null) {
        th.exportToClipboard(textField, Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.MOVE);
      }
    }//GEN-LAST:event_cutMenuItemActionPerformed

    void copyMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
      JTextField textField;
      if (titleTextField.getCaret().isSelectionVisible() && titleTextField.getSelectedText() != null) {
        textField = titleTextField;
      } else if (findTextField.getCaret().isSelectionVisible() && findTextField.getSelectedText() != null) {
        textField = findTextField;
      } else if (resultsSyncTable.getSelectedRowCount() != 0) {
        tableCopyListener.actionPerformed(new ActionEvent(resultsSyncTable, 0, Constant.COPY));
        return;
      } else {
        return;
      }

      TransferHandler th = textField.getTransferHandler();
      if (th != null) {
        th.exportToClipboard(textField, Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.COPY);
      }
    }//GEN-LAST:event_copyMenuItemActionPerformed

    void pasteMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
      if (UI.isClipboardEmpty()) {
        return;
      }

      JTextField textField;
      if (titleTextField.getCaret().isSelectionVisible()) {
        textField = titleTextField;
      } else if (findTextField.getCaret().isSelectionVisible()) {
        textField = findTextField;
      } else {
        return;
      }

      TransferHandler th = textField.getTransferHandler();
      if (th != null) {
        th.importData(textField, Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null));
      }
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    void resetWindowMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_resetWindowMenuItemActionPerformed
      Dimension dimension = new Dimension(1022, 680);
      if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH && !UI.isMaxSize(this, dimension)) {
        setExtendedState(Frame.NORMAL);
      }
      setSize(dimension);
      UI.centerOnScreen(this);
    }//GEN-LAST:event_resetWindowMenuItemActionPerformed

    void timeoutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_timeoutMenuItemActionPerformed
      resultsToBackground2();
      UI.show(timeoutDialog);
    }//GEN-LAST:event_timeoutMenuItemActionPerformed

    void tvSubmitButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubmitButtonActionPerformed
      cancelTVSelection = false;
      tvDialog.setVisible(false);
    }//GEN-LAST:event_tvSubmitButtonActionPerformed

    void tvEpisodeComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvEpisodeComboBoxActionPerformed
      updateTVComboBoxes();
    }//GEN-LAST:event_tvEpisodeComboBoxActionPerformed

    void tvSeasonComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSeasonComboBoxActionPerformed
      updateTVComboBoxes();
    }//GEN-LAST:event_tvSeasonComboBoxActionPerformed

    void tvCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvCancelButtonActionPerformed
      tvDialog.setVisible(false);
    }//GEN-LAST:event_tvCancelButtonActionPerformed

    void resultsPerSearchMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_resultsPerSearchMenuItemActionPerformed
      resultsToBackground2();
      UI.show(resultsPerSearchDialog);
    }//GEN-LAST:event_resultsPerSearchMenuItemActionPerformed

    void genreListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_genreListValueChanged
      UI.updateList(genreList);
    }//GEN-LAST:event_genreListValueChanged

  public void doPopularVideosSearch(boolean isTVShow, boolean isFeed, boolean isStartUp, MenuElement menuElement) {
    if (isFeed && isStartUp && !feedCheckBoxMenuItem.isSelected()) {
      return;
    }

    findControl.hide(true);
    isRegularSearcher = false;
    int numResultsPerSearch = Integer.parseInt((String) (isTVShow ? popularTVShowsResultsPerSearchComboBox
            : popularMoviesResultsPerSearchComboBox).getSelectedItem());
    String[] languages = UI.selectAnyIfNoSelectionAndCopy(languageList), countries = UI.selectAnyIfNoSelectionAndCopy(countryList);
    workerListener.popularSearchStarted(numResultsPerSearch, isTVShow, languages, countries, isFeed, !isStartUp);
    popularSearchMenuElement = menuElement;
  }

    void downloadSizeMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadSizeMenuItemActionPerformed
      resultsToBackground2();
      updateDownloadSizeComboBoxes();
      UI.show(downloadSizeDialog);
    }//GEN-LAST:event_downloadSizeMenuItemActionPerformed

    void maxDownloadSizeComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_maxDownloadSizeComboBoxActionPerformed
      updateDownloadSizeComboBoxes();
    }//GEN-LAST:event_maxDownloadSizeComboBoxActionPerformed

    void popularPopupMenuButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularPopupMenuButtonActionPerformed
      popularPopupMenu.show(searchButton, 0, searchButton.getHeight() + 1);
      if (popularSearchMenuElement != null) {
        MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{popularPopupMenu, popularSearchMenuElement});
      }
    }//GEN-LAST:event_popularPopupMenuButtonActionPerformed

    void fileExtensionsMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_fileExtensionsMenuItemActionPerformed
      resultsToBackground2();
      customExtensionTextField.requestFocusInWindow();
      UI.show(extensionsDialog);
    }//GEN-LAST:event_fileExtensionsMenuItemActionPerformed

    void whitelistedToBlacklistedButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_whitelistedToBlacklistedButtonActionPerformed
      fileExtensionsButtonAction(whitelistedList, whitelistListModel, blacklistListModel);
    }//GEN-LAST:event_whitelistedToBlacklistedButtonActionPerformed

    void blacklistedToWhitelistedButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_blacklistedToWhitelistedButtonActionPerformed
      fileExtensionsButtonAction(blacklistedList, blacklistListModel, whitelistListModel);
    }//GEN-LAST:event_blacklistedToWhitelistedButtonActionPerformed

  private void fileExtensionsButtonAction(JList fromList, DefaultListModel fromListModel, DefaultListModel toListModel) {
    String customExt = customExtensionTextField.getText().trim();
    int len = customExt.length();
    if (len > 0) {
      customExtensionTextField.setText("");
      if (Regex.isMatch(customExt, "\\.?+(\\p{Alpha}|\\d|-){1,20}+")) {
        if (customExt.charAt(0) != '.') {
          customExt = '.' + customExt;
        }
        int extIndex = UI.indexOf(fromListModel, customExt);
        if (extIndex != -1) {
          toListModel.addElement(fromListModel.remove(extIndex));
        } else if (UI.indexOf(toListModel, customExt) == -1) {
          toListModel.addElement(customExt);
        }
      } else {
        showMsg(Str.str("extensionFormat"), Constant.ERROR_MSG);
      }
    }

    for (Object extension : fromList.getSelectedValues()) {
      fromListModel.removeElement(extension);
      toListModel.addElement(extension);
    }

    UI.sort(toListModel);
  }

    void minDownloadSizeComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_minDownloadSizeComboBoxActionPerformed
      updateDownloadSizeComboBoxes();
    }//GEN-LAST:event_minDownloadSizeComboBoxActionPerformed

    void timeoutButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_timeoutButtonActionPerformed
      timeoutDialog.setVisible(false);
    }//GEN-LAST:event_timeoutButtonActionPerformed

    void resultsPerSearchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_resultsPerSearchButtonActionPerformed
      resultsPerSearchDialog.setVisible(false);
    }//GEN-LAST:event_resultsPerSearchButtonActionPerformed

    void downloadSizeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadSizeButtonActionPerformed
      downloadSizeDialog.setVisible(false);
    }//GEN-LAST:event_downloadSizeButtonActionPerformed

    void extensionsButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_extensionsButtonActionPerformed
      extensionsDialog.setVisible(false);
    }//GEN-LAST:event_extensionsButtonActionPerformed

    void editProfilesMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_editProfilesMenuItemActionPerformed
      resultsToBackground2();
      UI.show(profileDialog);
    }//GEN-LAST:event_editProfilesMenuItemActionPerformed

  void resultsTableValueChanged(ListSelectionEvent evt) {
    if (!evt.getSource().equals(resultsSyncTable.getSelectionModel())) {
      return;
    }

    Boolean enable = Boolean.FALSE;
    UI.enable(enable, findSubtitleMenuItem, emailEverythingMenuItem, copySubtitleLinkMenuItem);
    boolean isTorrentSearchDone = workerListener.isTorrentSearchDone(), isTrailerSearchDone = workerListener.isTrailerSearchDone(),
            isSummarySearchDone = workerListener.isSummarySearchDone();
    enable(isTorrentSearchDone ? enable : null, enable, null, null);
    enable(isTrailerSearchDone ? enable : null, enable, null, ContentType.TRAILER);
    UI.enable(enable, emailSummaryLinkMenuItem, copySummaryLinkMenuItem, copyPosterImageMenuItem, copySelectionMenuItem);
    enable(isSummarySearchDone ? enable : null, enable, null, ContentType.SUMMARY);

    if (evt.getFirstIndex() < 0 || resultsSyncTable.getSelectedRows().length != 1) {
      return;
    }

    enable = Boolean.TRUE;
    findSubtitleMenuItem.setEnabled(enable);
    if (isTorrentSearchDone && isTrailerSearchDone && isSummarySearchDone) {
      emailEverythingMenuItem.setEnabled(enable);
    }
    copySubtitleLinkMenuItem.setEnabled(enable);
    enable(isTorrentSearchDone ? enable : null, isTorrentSearchDone, null, null);
    enable(isTrailerSearchDone ? enable : null, isTrailerSearchDone, null, ContentType.TRAILER);
    UI.enable(enable, emailSummaryLinkMenuItem, copySummaryLinkMenuItem, copyPosterImageMenuItem);
    enable(isSummarySearchDone ? enable : null, isSummarySearchDone, null, ContentType.SUMMARY);
    copySelectionMenuItem.setEnabled(enable);
  }

  void playlistTableValueChanged(ListSelectionEvent evt) {
    if (!evt.getSource().equals(playlistSyncTable.getSelectionModel())) {
      return;
    }

    Component[] components = {playlistRemoveButton, playlistRemoveMenuItem, playlistCopySelectionMenuItem, playlistMoveDownButton, playlistMoveDownMenuItem,
      playlistMoveUpButton, playlistMoveUpMenuItem};
    UI.enable(false, components);
    int[] selectedRows;
    if (evt.getFirstIndex() >= 0 && (selectedRows = playlistSyncTable.getSelectedRows()).length >= 1) {
      UI.enable(true, selectedRows.length == 1 ? (UI.getUnfilteredRowCount(playlistSyncTable) == 1 ? new Component[]{playlistRemoveButton,
        playlistRemoveMenuItem, playlistCopySelectionMenuItem} : components) : new Component[]{playlistRemoveButton, playlistRemoveMenuItem,
        playlistCopySelectionMenuItem});
    }
    refreshPlaylistControls();
  }

    void updateMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_updateMenuItemActionPerformed
      workerListener.updateStarted(false);
    }//GEN-LAST:event_updateMenuItemActionPerformed

  private SelectedTableRow selectedRow() {
    findControl.hide(false);
    SelectedTableRow row = new SelectedTableRow();
    JViewport viewport = (JViewport) resultsSyncTable.table.getParent();
    viewport.scrollRectToVisible(rectangle(viewport, resultsSyncTable.getCellRect(row.viewVal, 0, true)));
    return row;
  }

  private Rectangle rectangle(JViewport viewport, Rectangle cellRect) {
    Point viewPosition = viewport.getViewPosition();
    cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);
    return cellRect;
  }

    void readSummaryButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_readSummaryButtonActionPerformed
      if (!UI.stop(readSummaryButton, new Runnable() {
        @Override
        public void run() {
          workerListener.summarySearchStopped();
        }
      })) {
        readSummaryActionPerformed(selectedRow(), evt != null && evt.getSource() == hearSummaryMenuItem, null);
      }
    }//GEN-LAST:event_readSummaryButtonActionPerformed

  void readSummaryActionPerformed(SelectedTableRow row, boolean read, VideoStrExportListener strExportListener) {
    workerListener.summarySearchStarted(row.val, row.video, read, strExportListener);
  }

    void watchTrailerButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchTrailerButtonActionPerformed
      if (!UI.stop(watchTrailerButton, new Runnable() {
        @Override
        public void run() {
          workerListener.trailerSearchStopped();
        }
      })) {
        watchTrailerActionPerformed(selectedRow(), null);
      }
    }//GEN-LAST:event_watchTrailerButtonActionPerformed

  private void watchTrailerActionPerformed(SelectedTableRow row, VideoStrExportListener strExportListener) {
    if (row.video.isTVShow) {
      downloadLinkEpisodes.add(row.val);
      subtitleEpisodes.add(row.val);
      if (!trailerEpisodes.add(row.val)) {
        row.video.season = "";
        row.video.episode = "";
      }
    }
    workerListener.trailerSearchStarted(row.val, row.video, strExportListener);
  }

  private void downloadLinkActionPerformed(ContentType downloadContentType, SelectedTableRow row, VideoStrExportListener strExportListener) {
    if (row.video.isTVShow && !downloadLinkEpisodes.add(row.val)) {
      row.video.season = "";
      row.video.episode = "";
    }
    workerListener.torrentSearchStarted(Connection.downloadLinkInfoFail() ? ContentType.DOWNLOAD3 : downloadContentType, row.val, row.video,
            strExportListener);
  }

    void downloadLink1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink1ButtonActionPerformed
      if (!UI.stop(downloadLink1Button, new Runnable() {
        @Override
        public void run() {
          workerListener.torrentSearchStopped();
        }
      })) {
        downloadLinkActionPerformed(ContentType.DOWNLOAD1, selectedRow(), null);
      }
    }//GEN-LAST:event_downloadLink1ButtonActionPerformed

    void downloadLink2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink2ButtonActionPerformed
      if (!UI.stop(downloadLink2Button, new Runnable() {
        @Override
        public void run() {
          workerListener.torrentSearchStopped();
        }
      })) {
        downloadLinkActionPerformed(ContentType.DOWNLOAD2, selectedRow(), null);
      }
    }//GEN-LAST:event_downloadLink2ButtonActionPerformed

    void languageCountryMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_languageCountryMenuItemActionPerformed
      resultsToBackground2();
      UI.show(languageCountryDialog);
    }//GEN-LAST:event_languageCountryMenuItemActionPerformed

    void languageListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_languageListValueChanged
      UI.updateList(languageList);
    }//GEN-LAST:event_languageListValueChanged

    void countryListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_countryListValueChanged
      UI.updateList(countryList);
    }//GEN-LAST:event_countryListValueChanged

    void trashCanButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_trashCanButtonActionPerformed
      Object[] whitelistValues = whitelistedList.getSelectedValues(), blacklistValues = blacklistedList.getSelectedValues();
      for (Object whitelistValue : whitelistValues) {
        whitelistListModel.removeElement(whitelistValue);
      }
      for (Object blacklistValue : blacklistValues) {
        blacklistListModel.removeElement(blacklistValue);
      }
    }//GEN-LAST:event_trashCanButtonActionPerformed

    void readSummaryMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_readSummaryMenuItemActionPerformed
      readSummaryButtonActionPerformed(evt);
    }//GEN-LAST:event_readSummaryMenuItemActionPerformed

    void watchTrailerMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchTrailerMenuItemActionPerformed
      watchTrailerButtonActionPerformed(evt);
    }//GEN-LAST:event_watchTrailerMenuItemActionPerformed

    void downloadLink1MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink1MenuItemActionPerformed
      downloadLink1ButtonActionPerformed(evt);
    }//GEN-LAST:event_downloadLink1MenuItemActionPerformed

    void downloadLink2MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink2MenuItemActionPerformed
      downloadLink2ButtonActionPerformed(evt);
    }//GEN-LAST:event_downloadLink2MenuItemActionPerformed

    void copySelectionMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copySelectionMenuItemActionPerformed
      if (resultsSyncTable.getSelectedRowCount() != 0) {
        tableCopyListener.actionPerformed(new ActionEvent(resultsSyncTable, 0, Constant.COPY));
      }
    }//GEN-LAST:event_copySelectionMenuItemActionPerformed

    void textComponentCutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_textComponentCutMenuItemActionPerformed
      popupTextFieldTransfer(TransferHandler.MOVE);
    }//GEN-LAST:event_textComponentCutMenuItemActionPerformed

    void textComponentCopyMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_textComponentCopyMenuItemActionPerformed
      if (popupTextComponent == summaryEditorPane) {
        htmlCopyListener.actionPerformed(new ActionEvent(summaryEditorPane, 0, Constant.COPY));
      } else {
        popupTextFieldTransfer(TransferHandler.COPY);
      }
    }//GEN-LAST:event_textComponentCopyMenuItemActionPerformed

  private void popupTextFieldTransfer(int type) {
    TransferHandler th;
    if (popupTextComponent != null && popupTextComponent.getCaret().isSelectionVisible() && popupTextComponent.getSelectedText() != null && (th
            = popupTextComponent.getTransferHandler()) != null) {
      th.exportToClipboard(popupTextComponent, Toolkit.getDefaultToolkit().getSystemClipboard(), type);
    }
  }

    void textComponentPasteMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_textComponentPasteMenuItemActionPerformed
      TransferHandler th;
      if (!UI.isClipboardEmpty() && popupTextComponent != null && popupTextComponent.getCaret().isSelectionVisible() && (th
              = popupTextComponent.getTransferHandler()) != null) {
        th.importData(popupTextComponent, Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null));
      }
    }//GEN-LAST:event_textComponentPasteMenuItemActionPerformed

    void textComponentPasteSearchMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_textComponentPasteSearchMenuItemActionPerformed
      if (UI.isClipboardEmpty() || popupTextComponent == null || !popupTextComponent.getCaret().isSelectionVisible()) {
        return;
      }

      TransferHandler th = popupTextComponent.getTransferHandler();
      if (th != null) {
        th.importData(popupTextComponent, Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null));
      }
      if (popupTextComponent == findTextField) {
        findTextFieldKeyPressed(null);
      } else if (popupTextComponent == playlistFindTextField) {
        playlistFindTextFieldKeyPressed(null);
      } else if (popupTextComponent == titleTextField || popupTextComponent == startDateTextComponent || popupTextComponent == endDateTextComponent) {
        searchButtonActionPerformed(null);
      }
    }//GEN-LAST:event_textComponentPasteSearchMenuItemActionPerformed

    void textComponentDeleteMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_textComponentDeleteMenuItemActionPerformed
      if (popupTextComponent != null && popupTextComponent.getCaret().isSelectionVisible() && popupTextComponent.getSelectedText() != null) {
        UI.textComponentDelete(popupTextComponent);
      }
    }//GEN-LAST:event_textComponentDeleteMenuItemActionPerformed

    void deleteMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_deleteMenuItemActionPerformed
      if (titleTextField.getCaret().isSelectionVisible() && titleTextField.getSelectedText() != null) {
        UI.textComponentDelete(titleTextField);
      } else if (findTextField.getCaret().isSelectionVisible() && findTextField.getSelectedText() != null) {
        UI.textComponentDelete(findTextField);
      }
    }//GEN-LAST:event_deleteMenuItemActionPerformed

    void editMenuMenuSelected(MenuEvent evt) {//GEN-FIRST:event_editMenuMenuSelected
      boolean isTextFieldTextSelected = (titleTextField.getCaret().isSelectionVisible() && titleTextField.getSelectedText() != null);
      if (isTextFieldTextSelected) {
        if (!cutMenuItem.isEnabled()) {
          cutMenuItem.setEnabled(true);
        }
        if (!deleteMenuItem.isEnabled()) {
          deleteMenuItem.setEnabled(true);
        }
      } else {
        if (cutMenuItem.isEnabled()) {
          cutMenuItem.setEnabled(false);
        }
        if (deleteMenuItem.isEnabled()) {
          deleteMenuItem.setEnabled(false);
        }
      }

      if (isTextFieldTextSelected || resultsSyncTable.getSelectedRowCount() != 0) {
        if (!copyMenuItem.isEnabled()) {
          copyMenuItem.setEnabled(true);
        }
      } else if (copyMenuItem.isEnabled()) {
        copyMenuItem.setEnabled(false);
      }

      if (!UI.isClipboardEmpty() && titleTextField.getCaret().isSelectionVisible()) {
        if (!pasteMenuItem.isEnabled()) {
          pasteMenuItem.setEnabled(true);
        }
      } else if (pasteMenuItem.isEnabled()) {
        pasteMenuItem.setEnabled(false);
      }

      if ((titleTextField.getCaret().isSelectionVisible() && !titleTextField.getText().isEmpty()) || resultsSyncTable.getRowCount() != 0) {
        if (!selectAllMenuItem.isEnabled()) {
          selectAllMenuItem.setEnabled(true);
        }
      } else if (selectAllMenuItem.isEnabled()) {
        selectAllMenuItem.setEnabled(false);
      }
    }//GEN-LAST:event_editMenuMenuSelected

    void selectAllMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_selectAllMenuItemActionPerformed
      if (titleTextField.getCaret().isSelectionVisible() && !titleTextField.getText().isEmpty()) {
        titleTextField.selectAll();
      } else if (findTextField.getCaret().isSelectionVisible() && !findTextField.getText().isEmpty()) {
        findTextField.selectAll();
      } else if (resultsSyncTable.getRowCount() != 0) {
        resultsSyncTable.selectAll();
      }
    }//GEN-LAST:event_selectAllMenuItemActionPerformed

    void textComponentSelectAllMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_textComponentSelectAllMenuItemActionPerformed
      if (popupTextComponent != null && popupTextComponent.getCaret().isSelectionVisible() && !popupTextComponent.getText().isEmpty()) {
        popupTextComponent.selectAll();
      }
    }//GEN-LAST:event_textComponentSelectAllMenuItemActionPerformed

    void textComponentPopupMenuPopupMenuWillBecomeVisible(PopupMenuEvent evt) {//GEN-FIRST:event_textComponentPopupMenuPopupMenuWillBecomeVisible
      boolean isEditable = popupTextComponent.isEditable(), isReadable = !(popupTextComponent instanceof JPasswordField);
      textComponentCutMenuItem.setVisible(isEditable && isReadable);
      textComponentCopyMenuItem.setVisible(isReadable);
      textComponentPasteMenuItem.setVisible(isEditable);
      textComponentDeleteMenuItem.setVisible(isEditable);
      textComponentPasteSearchMenuItem.setVisible(popupTextComponent == findTextField || popupTextComponent == playlistFindTextField
              || popupTextComponent == titleTextField || popupTextComponent == startDateTextComponent || popupTextComponent == endDateTextComponent);

      if (popupTextComponent.getCaret().isSelectionVisible() && popupTextComponent.getSelectedText() != null) {
        if (!textComponentCutMenuItem.isEnabled()) {
          textComponentCutMenuItem.setEnabled(true);
        }
        if (!textComponentCopyMenuItem.isEnabled()) {
          textComponentCopyMenuItem.setEnabled(true);
        }
        if (!textComponentDeleteMenuItem.isEnabled()) {
          textComponentDeleteMenuItem.setEnabled(true);
        }
      } else {
        if (textComponentCutMenuItem.isEnabled()) {
          textComponentCutMenuItem.setEnabled(false);
        }
        if (textComponentCopyMenuItem.isEnabled()) {
          textComponentCopyMenuItem.setEnabled(false);
        }
        if (textComponentDeleteMenuItem.isEnabled()) {
          textComponentDeleteMenuItem.setEnabled(false);
        }
      }

      if (!UI.isClipboardEmpty() && popupTextComponent.getCaret().isSelectionVisible()) {
        if (!textComponentPasteMenuItem.isEnabled()) {
          textComponentPasteMenuItem.setEnabled(true);
        }
        if (!textComponentPasteSearchMenuItem.isEnabled()) {
          textComponentPasteSearchMenuItem.setEnabled(true);
        }
      } else {
        if (textComponentPasteMenuItem.isEnabled()) {
          textComponentPasteMenuItem.setEnabled(false);
        }
        if (textComponentPasteSearchMenuItem.isEnabled()) {
          textComponentPasteSearchMenuItem.setEnabled(false);
        }
      }

      if (popupTextComponent.getCaret().isSelectionVisible() && !popupTextComponent.getText().isEmpty()) {
        if (!textComponentSelectAllMenuItem.isEnabled()) {
          textComponentSelectAllMenuItem.setEnabled(true);
        }
      } else if (textComponentSelectAllMenuItem.isEnabled()) {
        textComponentSelectAllMenuItem.setEnabled(false);
      }
    }//GEN-LAST:event_textComponentPopupMenuPopupMenuWillBecomeVisible

    void proxyMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyMenuItemActionPerformed
      resultsToBackground2();
      UI.show(proxyDialog);
    }//GEN-LAST:event_proxyMenuItemActionPerformed

    void proxyOKButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyOKButtonActionPerformed
      proxyDialog.setVisible(false);
    }//GEN-LAST:event_proxyOKButtonActionPerformed

    void proxyDownloadButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyDownloadButtonActionPerformed
      workerListener.proxyListDownloadStarted();
    }//GEN-LAST:event_proxyDownloadButtonActionPerformed

  private void enableProxyButtons(boolean enable) {
    UI.enable(enable, proxyExportButton, proxyImportButton, proxyDownloadButton, proxyRemoveButton, proxyAddButton);
  }

    void proxyAddButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyAddButtonActionPerformed
      enableProxyButtons(false);
      UI.hide(proxyDialog);
      addProxiesTextArea.setText("");
      UI.show(addProxiesDialog);
    }//GEN-LAST:event_proxyAddButtonActionPerformed

    void addProxiesCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_addProxiesCancelButtonActionPerformed
      restoreProxyDialog(true);
    }//GEN-LAST:event_addProxiesCancelButtonActionPerformed

    void addProxiesAddButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_addProxiesAddButtonActionPerformed
      addProxies(addProxiesTextArea.getText(), true);
    }//GEN-LAST:event_addProxiesAddButtonActionPerformed

    void addProxiesDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_addProxiesDialogWindowClosing
      restoreProxyDialog(true);
    }//GEN-LAST:event_addProxiesDialogWindowClosing

  private int exportProxies(String type) {
    enableProxyButtons(false);

    int numProxies = proxyComboBox.getItemCount();
    if (numProxies == 1) {
      showMsg(Str.str("noProxiesTo" + type), Constant.INFO_MSG);
      enableProxyButtons(true);
      return numProxies;
    }

    UI.hide(proxyDialog);
    return numProxies;
  }

    void proxyRemoveButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyRemoveButtonActionPerformed
      int numProxies = exportProxies("Remove");
      if (numProxies == 1) {
        return;
      }

      removeProxiesListModel.clear();
      for (int i = 1; i < numProxies; i++) {
        removeProxiesListModel.addElement(proxyComboBox.getItemAt(i));
      }

      UI.show(removeProxiesDialog);
    }//GEN-LAST:event_proxyRemoveButtonActionPerformed

  private void restoreProxyDialog(boolean addButtonPressed) {
    if (addButtonPressed) {
      UI.hide(addProxiesDialog);
      addProxiesAddButton.setEnabled(true);
    } else {
      UI.hide(removeProxiesDialog);
      removeProxiesRemoveButton.setEnabled(true);
    }
    enableProxyButtons(true);
    UI.show(proxyDialog);
  }

    void removeProxiesRemoveButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_removeProxiesRemoveButtonActionPerformed
      removeProxiesRemoveButton.setEnabled(false);

      Object[] selectedProxies = removeProxiesList.getSelectedValues();
      if (selectedProxies.length == 0) {
        showMsg(Str.str("noProxiesRemoved"), Constant.INFO_MSG);
        restoreProxyDialog(false);
        return;
      }

      for (Object proxy : selectedProxies) {
        proxyComboBox.removeItem(proxy);
      }

      showMsg(selectedProxies.length == 1 ? Str.str("proxyRemoved") : Str.str("proxiesRemoved", selectedProxies.length), Constant.INFO_MSG);

      StringBuilder proxiesBuf = new StringBuilder(2048);
      int numProxies = proxyComboBox.getItemCount();
      for (int i = 1; i < numProxies; i++) {
        proxiesBuf.append(proxyComboBox.getItemAt(i)).append(Constant.NEWLINE);
      }

      try {
        IO.write(Constant.APP_DIR + Constant.PROXIES, proxiesBuf.toString().trim());
      } catch (Exception e) {
        showException(e);
      }

      restoreProxyDialog(false);
    }//GEN-LAST:event_removeProxiesRemoveButtonActionPerformed

    void removeProxiesCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_removeProxiesCancelButtonActionPerformed
      restoreProxyDialog(false);
    }//GEN-LAST:event_removeProxiesCancelButtonActionPerformed

    void removeProxiesDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_removeProxiesDialogWindowClosing
      restoreProxyDialog(false);
    }//GEN-LAST:event_removeProxiesDialogWindowClosing

  private void noAddedProxies(boolean addButtonPressed) {
    showMsg(Str.str("noProxiesAdded"), Constant.INFO_MSG);
    if (addButtonPressed) {
      restoreProxyDialog(true);
    }
  }

  private void addProxies(String input, boolean addButtonPressed) {
    if (addButtonPressed) {
      addProxiesAddButton.setEnabled(false);
    }

    String proxies = input.trim();
    if (proxies.isEmpty()) {
      noAddedProxies(addButtonPressed);
      return;
    }

    int numProxies = proxyComboBox.getItemCount();
    Collection<String> oldProxies = new ArrayList<String>(numProxies);
    for (int i = 1; i < numProxies; i++) {
      oldProxies.add((String) proxyComboBox.getItemAt(i));
    }

    String[] proxyList = Regex.split(proxies, Constant.STD_NEWLINE);
    Collection<String> validProxies = new ArrayList<String>(proxyList.length);
    for (String proxy : proxyList) {
      String newProxy = proxy.trim();
      if (newProxy.isEmpty()) {
        continue;
      }

      if ((newProxy = Connection.getProxy(newProxy)) == null) {
        showMsg(Str.str("invalidProxy", proxy), Constant.ERROR_MSG);
        if (addButtonPressed) {
          addProxiesAddButton.setEnabled(true);
        }
        return;
      }

      if (!validProxies.contains(newProxy) && !oldProxies.contains(newProxy)) {
        validProxies.add(newProxy);
      }
    }

    if (validProxies.isEmpty()) {
      noAddedProxies(addButtonPressed);
      return;
    }

    proxyComboBox.removeAllItems();
    proxyComboBox.addItem(Constant.NO_PROXY);
    StringBuilder proxiesBuf = new StringBuilder(2048);
    for (String proxy : validProxies) {
      proxyComboBox.addItem(proxy);
      proxiesBuf.append(proxy).append(Constant.NEWLINE);
    }
    for (String proxy : oldProxies) {
      proxyComboBox.addItem(proxy);
      proxiesBuf.append(proxy).append(Constant.NEWLINE);
    }
    proxyComboBox.setSelectedItem(Constant.NO_PROXY);

    int numNewProxies = validProxies.size();
    showMsg(numNewProxies == 1 ? Str.str("proxyAdded") : Str.str("proxiesAdded", numNewProxies), Constant.INFO_MSG);

    try {
      IO.write(Constant.APP_DIR + Constant.PROXIES, proxiesBuf.toString().trim());
    } catch (Exception e) {
      showException(e);
    }

    if (addButtonPressed) {
      restoreProxyDialog(true);
    }
  }

    void proxyImportButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyImportButtonActionPerformed
      enableProxyButtons(false);
      UI.hide(proxyDialog);

      proxyFileChooser.setFileFilter(proxyListFileFilter);
      proxyFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
      if (!proxyImportFile.isEmpty()) {
        proxyFileChooser.setSelectedFile(new File(proxyImportFile));
      }
      UI.nonModalDialogsToBackground(proxyFileChooser);
      if (proxyFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
          File proxyFile = proxyFileChooser.getSelectedFile();
          proxyImportFile = proxyFile.getPath();
          addProxies(IO.read(proxyFile), false);
        } catch (Exception e) {
          showException(e);
        }
      }

      enableProxyButtons(true);
      UI.show(proxyDialog);
    }//GEN-LAST:event_proxyImportButtonActionPerformed

    void proxyExportButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyExportButtonActionPerformed
      int numProxies = exportProxies("Export");
      if (numProxies == 1) {
        return;
      }

      proxyFileChooser.setFileFilter(proxyListFileFilter);
      proxyFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
      proxyFileChooser.setSelectedFile(new File(proxyExportFile.isEmpty() ? Constant.PROXIES : proxyExportFile));
      UI.nonModalDialogsToBackground(proxyFileChooser);
      if (proxyFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
          Collection<String> proxies = new ArrayList<String>(numProxies - 1);
          StringBuilder proxiesBuf = new StringBuilder(2048);
          for (int i = 1; i < numProxies; i++) {
            String proxy = Connection.getProxy((String) proxyComboBox.getItemAt(i));
            if (proxy != null && !proxies.contains(proxy)) {
              proxies.add(proxy);
              proxiesBuf.append(proxy).append(Constant.NEWLINE);
            }
          }

          File proxyFile = proxyFileChooser.getSelectedFile();
          proxyExportFile = proxyFile.getPath();
          IO.write(proxyFile, proxiesBuf.toString().trim());
          numProxies = proxies.size();
          showMsg(numProxies == 1 ? Str.str("proxyExported") : Str.str("proxiesExported", numProxies), Constant.INFO_MSG);
        } catch (Exception e) {
          showException(e);
        }
      }

      enableProxyButtons(true);
      UI.show(proxyDialog);
    }//GEN-LAST:event_proxyExportButtonActionPerformed

    void msgOKButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_msgOKButtonActionPerformed
      msgEditorPane.setText(Constant.BLANK_HTML_PAGE);
      msgDialog.setVisible(false);
    }//GEN-LAST:event_msgOKButtonActionPerformed

    void msgDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_msgDialogWindowClosing
      msgEditorPane.setText(Constant.BLANK_HTML_PAGE);
    }//GEN-LAST:event_msgDialogWindowClosing

    void profileUseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileUseButtonActionPerformed
      profileDialog.setVisible(false);
      loadProfile();
    }//GEN-LAST:event_profileUseButtonActionPerformed

  private void loadProfile() {
    if (profileUseButton.isEnabled()) {
      int profile = profileComboBox.getSelectedIndex();
      int[] rows1 = resultsSyncTable.getSelectedRows(), rows2 = playlistSyncTable.getSelectedRows();
      settings.loadSettings(profile == 0 ? Constant.PROGRAM_DIR + Constant.DEFAULT_SETTINGS : Constant.APP_DIR + Constant.PROFILE + profile + Constant.TXT);
      playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(null);
      timedMsg(Str.str("settingsRestored", profileComboBox.getItemAt(profile)));
      Arrays.stream(rows1).forEach(row -> resultsSyncTable.addRowSelectionInterval(row, row));
      Arrays.stream(rows2).forEach(row -> playlistSyncTable.addRowSelectionInterval(row, row));
    } else {
      showMsg(Str.str("setProfileBeforeUse"), Constant.ERROR_MSG);
    }
  }

  @Override
  public void setMaximizedBounds(Rectangle bounds) {
    super.setMaximizedBounds(bounds == null ? null : UI.getUsableScreenBounds(this));
  }

    void profileComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileComboBoxActionPerformed
      updateProfileGUIitems(profileComboBox.getSelectedIndex());
    }//GEN-LAST:event_profileComboBoxActionPerformed

  private void updateProfileGUIitems(int profile) {
    if (profile == -1) {
      return;
    }
    if (profile == 0) {
      profileUseButton.setEnabled(true);
      UI.enable(false, profileClearButton, profileSetButton, profileRenameButton);
    } else if (new File(Constant.APP_DIR + Constant.PROFILE + profile + Constant.TXT).exists()) {
      UI.enable(true, profileUseButton, profileClearButton, profileSetButton, profileRenameButton);
      enableProfileMenuItem(profile, true);
    } else {
      UI.enable(false, profileUseButton, profileClearButton);
      UI.enable(true, profileSetButton, profileRenameButton);
      enableProfileMenuItem(profile, false);
    }
  }

  private void enableProfileMenuItem(int profile, boolean enable) {
    if (profile == 1) {
      profile1MenuItem.setEnabled(enable);
    } else if (profile == 2) {
      profile2MenuItem.setEnabled(enable);
    } else if (profile == 3) {
      profile3MenuItem.setEnabled(enable);
    } else if (profile == 4) {
      profile4MenuItem.setEnabled(enable);
    } else if (profile == 5) {
      profile5MenuItem.setEnabled(enable);
    } else if (profile == 6) {
      profile6MenuItem.setEnabled(enable);
    } else if (profile == 7) {
      profile7MenuItem.setEnabled(enable);
    } else if (profile == 8) {
      profile8MenuItem.setEnabled(enable);
    } else if (profile == 9) {
      profile9MenuItem.setEnabled(enable);
    }
  }

    void profile0MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile0MenuItemActionPerformed
      profileComboBox.setSelectedIndex(0);
      loadProfile();
    }//GEN-LAST:event_profile0MenuItemActionPerformed

    void profile1MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile1MenuItemActionPerformed
      profileComboBox.setSelectedIndex(1);
      loadProfile();
    }//GEN-LAST:event_profile1MenuItemActionPerformed

    void profile2MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile2MenuItemActionPerformed
      profileComboBox.setSelectedIndex(2);
      loadProfile();
    }//GEN-LAST:event_profile2MenuItemActionPerformed

    void profile3MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile3MenuItemActionPerformed
      profileComboBox.setSelectedIndex(3);
      loadProfile();
    }//GEN-LAST:event_profile3MenuItemActionPerformed

    void profile4MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile4MenuItemActionPerformed
      profileComboBox.setSelectedIndex(4);
      loadProfile();
    }//GEN-LAST:event_profile4MenuItemActionPerformed

    void profile5MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile5MenuItemActionPerformed
      profileComboBox.setSelectedIndex(5);
      loadProfile();
    }//GEN-LAST:event_profile5MenuItemActionPerformed

    void profile6MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile6MenuItemActionPerformed
      profileComboBox.setSelectedIndex(6);
      loadProfile();
    }//GEN-LAST:event_profile6MenuItemActionPerformed

    void profile7MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile7MenuItemActionPerformed
      profileComboBox.setSelectedIndex(7);
      loadProfile();
    }//GEN-LAST:event_profile7MenuItemActionPerformed

    void profile8MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile8MenuItemActionPerformed
      profileComboBox.setSelectedIndex(8);
      loadProfile();
    }//GEN-LAST:event_profile8MenuItemActionPerformed

    void profile9MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profile9MenuItemActionPerformed
      profileComboBox.setSelectedIndex(9);
      loadProfile();
    }//GEN-LAST:event_profile9MenuItemActionPerformed

    void profileSetButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileSetButtonActionPerformed
      int profile = profileComboBox.getSelectedIndex();
      try {
        settings.saveSettings(Constant.APP_DIR + Constant.PROFILE + profile + Constant.TXT);
        updateProfileGUIitems(profile);
        showMsg(Str.str("profileSet"), Constant.INFO_MSG);
      } catch (Exception e) {
        showException(e);
      }
    }//GEN-LAST:event_profileSetButtonActionPerformed

    void profileMenuMenuSelected(MenuEvent evt) {//GEN-FIRST:event_profileMenuMenuSelected
      profile0MenuItem.setText((String) profileComboBox.getItemAt(0));
      profile1MenuItem.setText((String) profileComboBox.getItemAt(1));
      profile2MenuItem.setText((String) profileComboBox.getItemAt(2));
      profile3MenuItem.setText((String) profileComboBox.getItemAt(3));
      profile4MenuItem.setText((String) profileComboBox.getItemAt(4));
      profile5MenuItem.setText((String) profileComboBox.getItemAt(5));
      profile6MenuItem.setText((String) profileComboBox.getItemAt(6));
      profile7MenuItem.setText((String) profileComboBox.getItemAt(7));
      profile8MenuItem.setText((String) profileComboBox.getItemAt(8));
      profile9MenuItem.setText((String) profileComboBox.getItemAt(9));
    }//GEN-LAST:event_profileMenuMenuSelected

    void profileRenameButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileRenameButtonActionPerformed
      UI.hide(profileDialog);
      profileNameChangeTextField.setText("");
      profileNameChangeTextField.requestFocusInWindow();
      UI.show(profileNameChangeDialog);
    }//GEN-LAST:event_profileRenameButtonActionPerformed

    void profileNameChangeOKButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileNameChangeOKButtonActionPerformed
      String profileName = profileNameChangeTextField.getText().trim();
      if (!Regex.isMatch(profileName, "(\\p{javaLowerCase}|\\p{javaUpperCase}|\\d|-| ){1,20}+")) {
        showMsg(Str.str("profileNameFormat"), Constant.ERROR_MSG);
        return;
      }
      if (((DefaultComboBoxModel) profileComboBox.getModel()).getIndexOf(profileName) != -1) {
        showMsg(Str.str("profileNameDuplicatePart1") + ' ' + Str.str("profileNameDuplicatePart2"), Constant.ERROR_MSG);
        return;
      }

      int profile = profileComboBox.getSelectedIndex();
      profileComboBox.removeItemAt(profile);
      profileComboBox.insertItemAt(profileName, profile);
      profileComboBox.setSelectedIndex(profile);
      preferences.put("GUI.profile" + profile + "MenuItem.text", profileName);

      profileNameChangeDialogWindowClosing(null);
    }//GEN-LAST:event_profileNameChangeOKButtonActionPerformed

    void profileNameChangeCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileNameChangeCancelButtonActionPerformed
      profileNameChangeDialogWindowClosing(null);
    }//GEN-LAST:event_profileNameChangeCancelButtonActionPerformed

    void profileNameChangeDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_profileNameChangeDialogWindowClosing
      UI.hide(profileNameChangeDialog);
      UI.show(profileDialog);
    }//GEN-LAST:event_profileNameChangeDialogWindowClosing

    void profileClearButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileClearButtonActionPerformed
      int profile = profileComboBox.getSelectedIndex();
      IO.fileOp(Constant.APP_DIR + Constant.PROFILE + profile + Constant.TXT, IO.RM_FILE);
      updateProfileGUIitems(profile);
    }//GEN-LAST:event_profileClearButtonActionPerformed

    void profileOKButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileOKButtonActionPerformed
      profileDialog.setVisible(false);
    }//GEN-LAST:event_profileOKButtonActionPerformed

    void connectionIssueButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_connectionIssueButtonActionPerformed
      connectionIssueButton.setIcon(null);
      connectionIssueButton.setPreferredSize(new Dimension(0, 0));
      connectionIssueButton.setEnabled(false);
      if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
        msgDialogWindowClosing(null);
      } else {
        UI.show(msgDialog);
      }
    }//GEN-LAST:event_connectionIssueButtonActionPerformed

    void findSubtitleMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_findSubtitleMenuItemActionPerformed
      findSubtitleActionPerformed(selectedRow(), null);
    }//GEN-LAST:event_findSubtitleMenuItemActionPerformed

  void findSubtitleActionPerformed(SelectedTableRow row, VideoStrExportListener strExportListener) {
    subtitleVideo = new Video(row.video.id, Regex.clean(row.video.title), row.video.year, row.video.isTVShow, row.video.isTVShowAndMovie);
    subtitleStrExportListener = strExportListener;
    isTVShowSubtitle = row.video.isTVShow;

    if (subtitleFormat != null) {
      movieSubtitleFormatComboBox.setSelectedItem(subtitleFormat);
      tvSubtitleFormatComboBox.setSelectedItem(subtitleFormat);
      subtitleFormat = null;
    }

    if (isTVShowSubtitle) {
      if (subtitleEpisodes.add(row.val)) {
        if (!row.video.season.isEmpty()) {
          tvSubtitleSeasonComboBox.setSelectedItem(row.video.season);
          tvSubtitleEpisodeComboBox.setSelectedItem(row.video.episode);
          subtitleEpisodes.remove(-1);
        } else if (subtitleEpisodes.contains(-1)) {
          tvSubtitleSeasonComboBox.setSelectedItem(tvSeasonComboBox.getSelectedItem());
          tvSubtitleEpisodeComboBox.setSelectedItem(tvEpisodeComboBox.getSelectedItem());
          subtitleEpisodes.remove(-1);
        }
      } else if (subtitleEpisodes.contains(-1)) {
        tvSubtitleSeasonComboBox.setSelectedItem(tvSeasonComboBox.getSelectedItem());
        tvSubtitleEpisodeComboBox.setSelectedItem(tvEpisodeComboBox.getSelectedItem());
        subtitleEpisodes.remove(-1);
      }
      tvSubtitleDownloadMatch1Button.requestFocusInWindow();
      UI.show(tvSubtitleDialog);
    } else {
      movieSubtitleDownloadMatch1Button.requestFocusInWindow();
      UI.show(movieSubtitleDialog);
    }
  }

    private void resultsTableMouseClicked(MouseEvent evt) {//GEN-FIRST:event_resultsTableMouseClicked
      if (evt.getClickCount() == 2 && resultsSyncTable.getSelectedRows().length == 1) {
        workerListener.summarySearchStopped();
        readSummaryActionPerformed(selectedRow(), false, null);
      }
    }//GEN-LAST:event_resultsTableMouseClicked

    private void portMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_portMenuItemActionPerformed
      resultsToBackground2();
      UI.show(portDialog);
    }//GEN-LAST:event_portMenuItemActionPerformed

    private void portOkButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_portOkButtonActionPerformed
      portDialog.setVisible(false);
    }//GEN-LAST:event_portOkButtonActionPerformed

  private void startSubtitleSearch(JComboBox format, JComboBox language, JComboBox season, JComboBox episode, boolean firstMatch) {
    subtitleVideo.season = (season == null ? "" : (String) season.getSelectedItem());
    subtitleVideo.episode = (episode == null ? "" : (String) episode.getSelectedItem());
    workerListener.subtitleSearchStarted((String) format.getSelectedItem(), Regex.subtitleLanguages.get((String) language.getSelectedItem()), subtitleVideo,
            isSubtitleMatch1 = firstMatch, subtitleStrExportListener);
  }

    private void tvSubtitleDownloadMatch1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubtitleDownloadMatch1ButtonActionPerformed
      if (!UI.stop(tvSubtitleDownloadMatch1Button, new Runnable() {
        @Override
        public void run() {
          workerListener.subtitleSearchStopped();
        }
      })) {
        startSubtitleSearch(tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox, tvSubtitleEpisodeComboBox, true);
      }
    }//GEN-LAST:event_tvSubtitleDownloadMatch1ButtonActionPerformed

    private void movieSubtitleDownloadMatch1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleDownloadMatch1ButtonActionPerformed
      if (!UI.stop(movieSubtitleDownloadMatch1Button, new Runnable() {
        @Override
        public void run() {
          workerListener.subtitleSearchStopped();
        }
      })) {
        startSubtitleSearch(movieSubtitleFormatComboBox, movieSubtitleLanguageComboBox, null, null, true);
      }
    }//GEN-LAST:event_movieSubtitleDownloadMatch1ButtonActionPerformed

    private void findMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_findMenuItemActionPerformed
      for (Component component : new Component[]{playlistTable, playlistPlayButton, playlistFindTextField, playlistOpenButton, playlistMoveUpButton,
        playlistMoveDownButton, playlistRemoveButton, playlistReloadGroupButton, playlistBanGroupButton}) {
        if (component.isFocusOwner()) {
          playlistFindControl.show();
          return;
        }
      }
      findControl.show();
    }//GEN-LAST:event_findMenuItemActionPerformed

    private void findTextFieldKeyPressed(KeyEvent evt) {//GEN-FIRST:event_findTextFieldKeyPressed
      findControl.find(evt, new Runnable() {
        @Override
        public void run() {
          readSummaryButtonActionPerformed(null);
        }
      }, resultsSyncTable);
    }//GEN-LAST:event_findTextFieldKeyPressed

    private void resultsTableKeyPressed(KeyEvent evt) {//GEN-FIRST:event_resultsTableKeyPressed
      if (evt.getKeyCode() == KeyEvent.VK_ENTER && resultsSyncTable.getSelectedRows().length == 1) {
        workerListener.summarySearchStopped();
        readSummaryActionPerformed(selectedRow(), false, null);
      }
    }//GEN-LAST:event_resultsTableKeyPressed

    private void authenticationUsernameTextFieldAncestorAdded(AncestorEvent evt) {//GEN-FIRST:event_authenticationUsernameTextFieldAncestorAdded
      if (authenticationUsernameTextField.getText().isEmpty()) {
        authenticationUsernameTextField.requestFocusInWindow();
      }
    }//GEN-LAST:event_authenticationUsernameTextFieldAncestorAdded

    private void authenticationPasswordFieldAncestorAdded(AncestorEvent evt) {//GEN-FIRST:event_authenticationPasswordFieldAncestorAdded
      if (!authenticationUsernameTextField.getText().isEmpty()) {
        authenticationPasswordField.requestFocusInWindow();
      }
    }//GEN-LAST:event_authenticationPasswordFieldAncestorAdded

    private void formWindowClosing(WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
      setVisible(false);
      if (trayIcon != null && UI.trayIcon(this) == null && isPlaylistActive()) {
        try {
          SystemTray.getSystemTray().add(trayIcon);
          String title = trayIcon.getToolTip(), msg = null;
          int index = title.indexOf('\n');
          if (index != -1) {
            msg = title.substring(index + 1);
            title = title.substring(0, index);
          }
          trayIcon.displayMessage(title, msg, MessageType.INFO);
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          System.exit(0);
        }
      } else {
        System.exit(0);
      }
    }//GEN-LAST:event_formWindowClosing

    private void textComponentPopupMenuPopupMenuWillBecomeInvisible(PopupMenuEvent evt) {//GEN-FIRST:event_textComponentPopupMenuPopupMenuWillBecomeInvisible
      if (popupTextComponent instanceof JTextFieldDateEditor) {
        popupTextComponent.removeFocusListener((FocusListener) popupTextComponent);
        popupTextComponent.addFocusListener((FocusListener) popupTextComponent);
      }
    }//GEN-LAST:event_textComponentPopupMenuPopupMenuWillBecomeInvisible

    private void customExtensionTextFieldKeyPressed(KeyEvent evt) {//GEN-FIRST:event_customExtensionTextFieldKeyPressed
      String customExt;
      int key = evt.getKeyCode();
      if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_KP_RIGHT) {
        whitelistedToBlacklistedButtonActionPerformed(null);
      } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_KP_LEFT) {
        blacklistedToWhitelistedButtonActionPerformed(null);
      } else if (!(customExt = customExtensionTextField.getText()).startsWith(".") && key != KeyEvent.VK_PERIOD && key != KeyEvent.VK_DECIMAL && key
              != KeyEvent.VK_ALT && key != KeyEvent.VK_ALT_GRAPH && key != KeyEvent.VK_CONTROL && key != KeyEvent.VK_META && key != KeyEvent.VK_SHIFT && key
              != KeyEvent.VK_BACK_SPACE && key != KeyEvent.VK_ENTER && key != KeyEvent.VK_DELETE && key != KeyEvent.VK_ESCAPE && !evt.isActionKey()
              && !evt.isAltDown() && !evt.isAltGraphDown() && !evt.isControlDown() && !evt.isMetaDown()) {
        customExtensionTextField.setText('.' + customExt);
      }
    }//GEN-LAST:event_customExtensionTextFieldKeyPressed

    private void listCutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_listCutMenuItemActionPerformed
      popupListCopy();
      popupListDelete();
    }//GEN-LAST:event_listCutMenuItemActionPerformed

    private void listCopyMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_listCopyMenuItemActionPerformed
      popupListCopy();
    }//GEN-LAST:event_listCopyMenuItemActionPerformed

  private void popupListCopy() {
    TransferHandler th;
    if (popupList != null && popupList.getSelectedIndices().length != 0 && (th = popupList.getTransferHandler()) != null) {
      th.exportToClipboard(popupList, Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.COPY);
    }
  }

  private void popupListDelete() {
    if (popupList == removeProxiesList) {
      removeProxiesRemoveButton.requestFocusInWindow();
      removeProxiesRemoveButtonActionPerformed(null);
    } else if (popupList == whitelistedList || popupList == blacklistedList) {
      trashCanButton.requestFocusInWindow();
      trashCanButtonActionPerformed(null);
    }
  }

    private void listDeleteMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_listDeleteMenuItemActionPerformed
      popupListDelete();
    }//GEN-LAST:event_listDeleteMenuItemActionPerformed

    private void listSelectAllMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_listSelectAllMenuItemActionPerformed
      int size;
      if (popupList != null && (size = popupList.getModel().getSize()) > 0) {
        popupList.setSelectionInterval(0, size - 1);
      }
    }//GEN-LAST:event_listSelectAllMenuItemActionPerformed

    private void listPopupMenuPopupMenuWillBecomeVisible(PopupMenuEvent evt) {//GEN-FIRST:event_listPopupMenuPopupMenuWillBecomeVisible
      if (popupList.getSelectedIndex() == -1) {
        if (listCutMenuItem.isEnabled()) {
          listCutMenuItem.setEnabled(false);
        }
        if (listCopyMenuItem.isEnabled()) {
          listCopyMenuItem.setEnabled(false);
        }
        if (listDeleteMenuItem.isEnabled()) {
          listDeleteMenuItem.setEnabled(false);
        }
      } else {
        if (!listCutMenuItem.isEnabled()) {
          listCutMenuItem.setEnabled(true);
        }
        if (!listCopyMenuItem.isEnabled()) {
          listCopyMenuItem.setEnabled(true);
        }
        if (!listDeleteMenuItem.isEnabled()) {
          listDeleteMenuItem.setEnabled(true);
        }
      }

      if (popupList.getModel().getSize() > 0) {
        if (!listSelectAllMenuItem.isEnabled()) {
          listSelectAllMenuItem.setEnabled(true);
        }
      } else if (listSelectAllMenuItem.isEnabled()) {
        listSelectAllMenuItem.setEnabled(false);
      }
    }//GEN-LAST:event_listPopupMenuPopupMenuWillBecomeVisible

    private void tvSubtitleFormatComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubtitleFormatComboBoxActionPerformed
      movieSubtitleFormatComboBox.setSelectedItem(tvSubtitleFormatComboBox.getSelectedItem());
    }//GEN-LAST:event_tvSubtitleFormatComboBoxActionPerformed

    private void movieSubtitleFormatComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleFormatComboBoxActionPerformed
      tvSubtitleFormatComboBox.setSelectedItem(movieSubtitleFormatComboBox.getSelectedItem());
    }//GEN-LAST:event_movieSubtitleFormatComboBoxActionPerformed

    private void movieSubtitleLanguageComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleLanguageComboBoxActionPerformed
      tvSubtitleLanguageComboBox.setSelectedItem(movieSubtitleLanguageComboBox.getSelectedItem());
    }//GEN-LAST:event_movieSubtitleLanguageComboBoxActionPerformed

    private void tvSubtitleLanguageComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubtitleLanguageComboBoxActionPerformed
      movieSubtitleLanguageComboBox.setSelectedItem(tvSubtitleLanguageComboBox.getSelectedItem());
    }//GEN-LAST:event_tvSubtitleLanguageComboBoxActionPerformed

    private void tvSubtitleDownloadMatch2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubtitleDownloadMatch2ButtonActionPerformed
      if (!UI.stop(tvSubtitleDownloadMatch2Button, new Runnable() {
        @Override
        public void run() {
          workerListener.subtitleSearchStopped();
        }
      })) {
        startSubtitleSearch(tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox, tvSubtitleEpisodeComboBox, false);
      }
    }//GEN-LAST:event_tvSubtitleDownloadMatch2ButtonActionPerformed

    private void movieSubtitleDownloadMatch2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleDownloadMatch2ButtonActionPerformed
      if (!UI.stop(movieSubtitleDownloadMatch2Button, new Runnable() {
        @Override
        public void run() {
          workerListener.subtitleSearchStopped();
        }
      })) {
        startSubtitleSearch(movieSubtitleFormatComboBox, movieSubtitleLanguageComboBox, null, null, false);
      }
    }//GEN-LAST:event_movieSubtitleDownloadMatch2ButtonActionPerformed

    private void whitelistedListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_whitelistedListKeyPressed
      int key = evt.getKeyCode();
      if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_KP_RIGHT) {
        whitelistedToBlacklistedButtonActionPerformed(null);
      } else if (key == KeyEvent.VK_DELETE) {
        trashCanButtonActionPerformed(null);
      }
    }//GEN-LAST:event_whitelistedListKeyPressed

    private void blacklistedListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_blacklistedListKeyPressed
      int key = evt.getKeyCode();
      if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_KP_LEFT) {
        blacklistedToWhitelistedButtonActionPerformed(null);
      } else if (key == KeyEvent.VK_DELETE) {
        trashCanButtonActionPerformed(null);
      }
    }//GEN-LAST:event_blacklistedListKeyPressed

    private void hideMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_hideMenuItemActionPerformed
      connectionIssueButtonActionPerformed(new ActionEvent(connectionIssueButton, 0, "", ActionEvent.CTRL_MASK));
    }//GEN-LAST:event_hideMenuItemActionPerformed

    private void copyFullTitleAndYearMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyFullTitleAndYearMenuItemActionPerformed
      SelectedTableRow row = selectedRow();
      readSummaryActionPerformed(row, false, row.strExportListener(false));
    }//GEN-LAST:event_copyFullTitleAndYearMenuItemActionPerformed

  private void trailerLinkExportActionPerformed(boolean exportToEmail) {
    SelectedTableRow row = selectedRow();
    watchTrailerActionPerformed(row, row.strExportListener(exportToEmail));
  }

    private void copyTrailerLinkMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyTrailerLinkMenuItemActionPerformed
      trailerLinkExportActionPerformed(false);
    }//GEN-LAST:event_copyTrailerLinkMenuItemActionPerformed

    private void emailTrailerLinkMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailTrailerLinkMenuItemActionPerformed
      trailerLinkExportActionPerformed(true);
    }//GEN-LAST:event_emailTrailerLinkMenuItemActionPerformed

  private void summaryLinkExportActionPerformed(boolean exportToEmail) {
    SelectedTableRow row = selectedRow();
    exportSummaryLink(row, row.strExportListener(exportToEmail));
  }
    private void copySummaryLinkMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copySummaryLinkMenuItemActionPerformed
      summaryLinkExportActionPerformed(false);
    }//GEN-LAST:event_copySummaryLinkMenuItemActionPerformed

    private void emailSummaryLinkMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailSummaryLinkMenuItemActionPerformed
      summaryLinkExportActionPerformed(true);
    }//GEN-LAST:event_emailSummaryLinkMenuItemActionPerformed

  private void downloadLinkExportActionPerformed(ContentType downloadContentType, boolean exportToEmail) {
    SelectedTableRow row = selectedRow();
    downloadLinkActionPerformed(downloadContentType, row, row.strExportListener(exportToEmail));
  }

    private void copyDownloadLink1MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyDownloadLink1MenuItemActionPerformed
      downloadLinkExportActionPerformed(ContentType.DOWNLOAD1, false);
    }//GEN-LAST:event_copyDownloadLink1MenuItemActionPerformed

    private void emailDownloadLink1MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailDownloadLink1MenuItemActionPerformed
      downloadLinkExportActionPerformed(ContentType.DOWNLOAD1, true);
    }//GEN-LAST:event_emailDownloadLink1MenuItemActionPerformed

    private void copyDownloadLink2MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyDownloadLink2MenuItemActionPerformed
      downloadLinkExportActionPerformed(ContentType.DOWNLOAD2, false);
    }//GEN-LAST:event_copyDownloadLink2MenuItemActionPerformed

    private void emailDownloadLink2MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailDownloadLink2MenuItemActionPerformed
      downloadLinkExportActionPerformed(ContentType.DOWNLOAD2, true);
    }//GEN-LAST:event_emailDownloadLink2MenuItemActionPerformed

    private void copySubtitleLinkMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copySubtitleLinkMenuItemActionPerformed
      SelectedTableRow row = selectedRow();
      findSubtitleActionPerformed(row, row.strExportListener(false));
    }//GEN-LAST:event_copySubtitleLinkMenuItemActionPerformed

    private void emailEverythingMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailEverythingMenuItemActionPerformed
      SelectedTableRow row = selectedRow();
      VideoStrExportListener strExportListener = row.strExportListener(true, true, 6);
      readSummaryActionPerformed(row, false, strExportListener);
      exportPosterImage(row, strExportListener);
      downloadLinkActionPerformed(ContentType.DOWNLOAD1, row, strExportListener);
      watchTrailerActionPerformed(row, strExportListener);
      exportSummaryLink(row, strExportListener);
    }//GEN-LAST:event_emailEverythingMenuItemActionPerformed

    private void copyPosterImageMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyPosterImageMenuItemActionPerformed
      SelectedTableRow row = selectedRow();
      exportPosterImage(row, row.strExportListener(false));
    }//GEN-LAST:event_copyPosterImageMenuItemActionPerformed

    private void removeProxiesListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_removeProxiesListKeyPressed
      if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
        removeProxiesRemoveButtonActionPerformed(null);
      }
    }//GEN-LAST:event_removeProxiesListKeyPressed

    private void portDialogComponentHidden(ComponentEvent evt) {//GEN-FIRST:event_portDialogComponentHidden
      String port = portTextField.getText().trim();
      portTextField.setText(port);
      int portNum;

      if (port.isEmpty()) {
        portRandomizeCheckBox.setSelected(true);
      } else if ((portNum = portNum(port)) == -1) {
        if (isConfirmed(Str.str("invalidPortPart1", port) + ' ' + Str.str("invalidPortPart2") + ' ' + Str.str("invalidPortPart3") + ' ' + Str.str(
                "invalidPortPart4"))) {
          resultsToBackground2();
          UI.show(portDialog);
        } else {
          portTextField.setText(null);
          portRandomizeCheckBox.setSelected(true);
        }
      } else {
        workerListener.portChanged(portNum);
      }
    }//GEN-LAST:event_portDialogComponentHidden

    private void playlistMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistMenuItemActionPerformed
      playlistShown.set(true);
      splitPane.setDividerLocation(splitPane.getResizeWeight());
      playlistSyncTable.requestFocusInWindow();
      if (!isPlaylistRestored.get()) {
        (new Worker() {
          @Override
          protected void doWork() {
            restorePlaylist(true);
          }
        }).execute();
      }
    }//GEN-LAST:event_playlistMenuItemActionPerformed

  private PlaylistItem selectedPlaylistItem() {
    synchronized (playlistSyncTable.lock) {
      int[] rows = playlistSyncTable.table.getSelectedRows();
      if (rows.length != 1) {
        return null;
      }
      return (PlaylistItem) playlistSyncTable.tableModel.getValueAt(playlistSyncTable.table.convertRowIndexToModel(rows[0]), playlistItemCol);
    }
  }

  PlaylistItem[] selectedPlaylistItems() {
    synchronized (playlistSyncTable.lock) {
      int[] rows = playlistSyncTable.table.getSelectedRows();
      if (rows.length < 1) {
        return null;
      }

      Arrays.sort(rows);
      PlaylistItem[] playlistItems = new PlaylistItem[rows.length];
      for (int i = 0; i < rows.length; i++) {
        playlistItems[i] = (PlaylistItem) playlistSyncTable.tableModel.getValueAt(playlistSyncTable.table.convertRowIndexToModel(rows[i]),
                playlistItemCol);
      }
      return playlistItems;
    }
  }

    private void playlistPlayButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistPlayButtonActionPerformed
      playlistFindControl.hide(false);
      boolean force = forcePlay;
      forcePlay = false;
      PlaylistItem[] playlistItems = selectedPlaylistItems();
      if (playlistItems == null) {
        return;
      }

      boolean play = false;
      for (int i = 0; i < playlistItems.length; i++) {
        if (i == 0) {
          play = playlistItems[i].canPlay();
        }
        if (play) {
          if (playlistItems[i].canPlay()) {
            playlistItems[i].play(i == 0 && force);
          }
        } else if (playlistItems[i].isStoppable()) {
          playlistItems[i].stop();
        }
      }
    }//GEN-LAST:event_playlistPlayButtonActionPerformed

    private void playlistPlayMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistPlayMenuItemActionPerformed
      playlistPlayButtonActionPerformed(null);
    }//GEN-LAST:event_playlistPlayMenuItemActionPerformed

    private void playlistSaveFolderMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistSaveFolderMenuItemActionPerformed
      playlistFileChooser.setSelectedFile(new File(playlistDir));
      if (save(playlistFileChooser)) {
        playlistDir = playlistFileChooser.getSelectedFile().getPath();
      }
    }//GEN-LAST:event_playlistSaveFolderMenuItemActionPerformed

    private void playlistMoveUpButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistMoveUpButtonActionPerformed
      playlistMove(true);
    }//GEN-LAST:event_playlistMoveUpButtonActionPerformed

  private void playlistMove(boolean up) {
    playlistFindControl.hide(true);
    synchronized (playlistSyncTable.lock) {
      int numRows = UI.getUnfilteredRowCount(playlistSyncTable.table);
      int[] rows;
      if (numRows > 1 && (rows = playlistSyncTable.table.getSelectedRows()).length == 1) {
        playlistSyncTable.table.getRowSorter().setSortKeys(null);
        int row = playlistSyncTable.table.getSelectedRow();
        if (row == rows[0] && row != (up ? 0 : numRows - 1)) {
          int modelRow = playlistSyncTable.table.convertRowIndexToModel(row);
          int newViewRow = (up ? --row : ++row);
          playlistSyncTable.tableModel.moveRow(modelRow, modelRow, playlistSyncTable.table.convertRowIndexToModel(newViewRow));
          playlistSyncTable.table.getRowSorter().allRowsChanged();
          playlistSyncTable.table.changeSelection(newViewRow, 0, false, false);
        }
      }
    }
  }

    private void playlistMoveUpMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistMoveUpMenuItemActionPerformed
      playlistMoveUpButtonActionPerformed(null);
    }//GEN-LAST:event_playlistMoveUpMenuItemActionPerformed

    private void playlistMoveDownButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistMoveDownButtonActionPerformed
      playlistMove(false);
    }//GEN-LAST:event_playlistMoveDownButtonActionPerformed

    private void playlistMoveDownMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistMoveDownMenuItemActionPerformed
      playlistMoveDownButtonActionPerformed(null);
    }//GEN-LAST:event_playlistMoveDownMenuItemActionPerformed

    private void playlistRemoveButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistRemoveButtonActionPerformed
      playlistFindControl.hide(true);
      synchronized (playlistSyncTable.lock) {
        int[] viewRows = playlistSyncTable.table.getSelectedRows();
        if (viewRows.length == 0) {
          return;
        }

        int[] modelRows = new int[viewRows.length];
        for (int i = 0; i < viewRows.length; i++) {
          modelRows[i] = playlistSyncTable.table.convertRowIndexToModel(viewRows[i]);
        }
        Arrays.sort(modelRows);

        for (int i = modelRows.length - 1; i > -1; i--) {
          ((PlaylistItem) playlistSyncTable.tableModel.getValueAt(modelRows[i], playlistItemCol)).stop();
        }
        List<?> rows = playlistSyncTable.tableModel.getDataVector();
        for (int i = modelRows.length - 1; i > -1; i--) {
          rows.remove(modelRows[i]);
        }
        playlistSyncTable.tableModel.fireTableDataChanged();

        selectFirstRow(viewRows);
      }
    }//GEN-LAST:event_playlistRemoveButtonActionPerformed

  private void selectFirstRow(int[] viewRows) {
    Arrays.sort(viewRows);
    int viewRow = viewRows[0];
    while (viewRow >= UI.getUnfilteredRowCount(playlistSyncTable.table)) {
      viewRow--;
    }
    if (viewRow != -1) {
      playlistSyncTable.table.changeSelection(viewRow, 0, false, false);
    }
  }

    private void playlistRemoveMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistRemoveMenuItemActionPerformed
      playlistRemoveButtonActionPerformed(null);
    }//GEN-LAST:event_playlistRemoveMenuItemActionPerformed

    private void playlistCopySelectionMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistCopySelectionMenuItemActionPerformed
      if (playlistSyncTable.getSelectedRowCount() != 0) {
        playlistTableCopyListener.actionPerformed(new ActionEvent(playlistSyncTable, 0, Constant.COPY));
      }
    }//GEN-LAST:event_playlistCopySelectionMenuItemActionPerformed

    private void playlistOpenMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistOpenMenuItemActionPerformed
      playlistOpenButtonActionPerformed(null);
    }//GEN-LAST:event_playlistOpenMenuItemActionPerformed

  private boolean isPlaylistActive() {
    boolean isPlaylistActive = false;
    synchronized (playlistSyncTable.lock) {
      for (int row = playlistSyncTable.tableModel.getRowCount() - 1; row > -1; row--) {
        if (((PlaylistItem) playlistSyncTable.tableModel.getValueAt(row, playlistItemCol)).isActive()) {
          isPlaylistActive = true;
          break;
        }
      }
    }
    return isPlaylistActive;
  }

    private void playlistPlayButtonMousePressed(MouseEvent evt) {//GEN-FIRST:event_playlistPlayButtonMousePressed
      if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
        forcePlay = true;
      }
    }//GEN-LAST:event_playlistPlayButtonMousePressed

    private void playlistPlayMenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_playlistPlayMenuItemMousePressed
      playlistPlayButtonMousePressed(evt);
    }//GEN-LAST:event_playlistPlayMenuItemMousePressed

    private void playlistTableMouseClicked(MouseEvent evt) {//GEN-FIRST:event_playlistTableMouseClicked
      if (evt.getClickCount() == 2) {
        playlistPlayButtonMousePressed(evt);
        playlistPlayButtonActionPerformed(null);
      }
      playlistTableValueChanged(new ListSelectionEvent(playlistSyncTable.getSelectionModel(), playlistSyncTable.getSelectedRow(), -1, false));
    }//GEN-LAST:event_playlistTableMouseClicked

  @SuppressWarnings("unchecked")
    private void playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed
    synchronized (playlistSyncTable.lock) {
      ((DefaultRowSorter<TableModel, Integer>) playlistSyncTable.table.getRowSorter()).setRowFilter(playlistShowNonVideoItemsCheckBoxMenuItem.isSelected()
              ? null : new RowFilter<TableModel, Integer>() {
        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> row) {
          return !Regex.isMatch(row.getStringValue(playlistNameCol), 683);
        }
      });
    }
    }//GEN-LAST:event_playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed

    private void languageCountryOkButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_languageCountryOkButtonActionPerformed
      languageCountryDialog.setVisible(false);
    }//GEN-LAST:event_languageCountryOkButtonActionPerformed

    private void watchOnDeviceMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchOnDeviceMenuItemActionPerformed
      findControl.hide(false);
      try {
        Connection.browse(Str.get(Constant.WINDOWS ? 718 : (Constant.MAC ? 719 : 720)));
      } catch (Exception e) {
        showException(e);
      }
    }//GEN-LAST:event_watchOnDeviceMenuItemActionPerformed

    private void exitBackupModeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_exitBackupModeButtonActionPerformed
      exitBackupModeButton.setText(null);
      exitBackupModeButton.setBorderPainted(false);
      exitBackupModeButton.setEnabled(false);
      resizeExitBackupModeButton();

      Connection.unfailDownloadLinkInfo();
      if (!isAltSearch) {
        return;
      }

      isAltSearch = false;
      if (!downloadLink2Button.isEnabled() && workerListener.isTorrentSearchDone() && resultsSyncTable.getSelectedRows().length == 1) {
        UI.enable(true, downloadLink2Button, downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem);
      }
    }//GEN-LAST:event_exitBackupModeButtonActionPerformed

    private void playlistReloadGroupMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistReloadGroupMenuItemActionPerformed
      playlistReloadGroupButtonActionPerformed(null);
    }//GEN-LAST:event_playlistReloadGroupMenuItemActionPerformed

    private void playlistFindTextFieldKeyPressed(KeyEvent evt) {//GEN-FIRST:event_playlistFindTextFieldKeyPressed
      playlistFindControl.find(evt, new Runnable() {
        @Override
        public void run() {
          playlistPlayButtonActionPerformed(null);
        }
      }, playlistSyncTable);
    }//GEN-LAST:event_playlistFindTextFieldKeyPressed

    private void playlistBanGroupMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistBanGroupMenuItemActionPerformed
      playlistBanGroupButtonActionPerformed(null);
    }//GEN-LAST:event_playlistBanGroupMenuItemActionPerformed

    private void downloadQualityButtonMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadQualityButtonMenuItemActionPerformed
      subtitleFormat = getFormat();
      if (evt.getModifiers() == (ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK)) {
        timedMsg(evt.getActionCommand());
      }
    }//GEN-LAST:event_downloadQualityButtonMenuItemActionPerformed

    private void playlistOpenButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistOpenButtonActionPerformed
      playlistFindControl.hide(false);
      PlaylistItem playlistItem = selectedPlaylistItem();
      if (playlistItem != null) {
        playlistItem.open();
      }
    }//GEN-LAST:event_playlistOpenButtonActionPerformed

    private void playlistReloadGroupButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistReloadGroupButtonActionPerformed
      playlistFindControl.hide(true);
      PlaylistItem playlistItem = selectedPlaylistItem();
      if (playlistItem != null) {
        workerListener.reloadGroup(playlistItem);
      }
    }//GEN-LAST:event_playlistReloadGroupButtonActionPerformed

    private void playlistBanGroupButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistBanGroupButtonActionPerformed
      playlistFindControl.hide(false);
      PlaylistItem playlistItem = selectedPlaylistItem();
      if (playlistItem == null) {
        return;
      }

      Long downloadID = playlistItem.groupDownloadID();
      Icon icon = banIcon;
      String textKey = "banGroup";
      if (bannedDownloadIDs.add(downloadID)) {
        icon = unbanIcon;
        textKey = "un" + textKey;
      } else {
        bannedDownloadIDs.remove(downloadID);
      }
      playlistBanGroupButton.setIcon(icon);
      playlistBanGroupMenuItem.setText(Str.str(textKey));
    }//GEN-LAST:event_playlistBanGroupButtonActionPerformed

    private void popularTVShowsMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularTVShowsMenuItemActionPerformed
      doPopularVideosSearch(true, false, false, popularTVShowsMenuItem);
    }//GEN-LAST:event_popularTVShowsMenuItemActionPerformed

    private void popularNewHQMoviesMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularNewHQMoviesMenuItemActionPerformed
      doPopularVideosSearch(false, true, false, popularNewHQMoviesMenuItem);
    }//GEN-LAST:event_popularNewHQMoviesMenuItemActionPerformed

    private void popularNewHQTVShowsMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularNewHQTVShowsMenuItemActionPerformed
      doPopularVideosSearch(true, true, false, popularNewHQTVShowsMenuItem);
    }//GEN-LAST:event_popularNewHQTVShowsMenuItemActionPerformed

    private void playlistTableKeyPressed(KeyEvent evt) {//GEN-FIRST:event_playlistTableKeyPressed
      KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(evt);
      if (keyStroke.equals(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))) {
        playlistRemoveButtonActionPerformed(null);
      } else if (keyStroke.equals(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))) {
        playlistPlayButtonActionPerformed(null);
      }
    }//GEN-LAST:event_playlistTableKeyPressed

    private void hearSummaryMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_hearSummaryMenuItemActionPerformed
      readSummaryButtonActionPerformed(evt);
    }//GEN-LAST:event_hearSummaryMenuItemActionPerformed

    private void banTitleMenuMenuSelected(MenuEvent evt) {//GEN-FIRST:event_banTitleMenuMenuSelected
      Container menu = (Container) evt.getSource();
      menu.removeAll();
      List<String> titles = new ArrayList<String>(100);

      synchronized (resultsSyncTable.lock) {
        int row = resultsSyncTable.table.getSelectedRow();
        if (row != -1) {
          int modelRow = resultsSyncTable.table.convertRowIndexToModel(row);
          String title = bannedTitle((String) resultsSyncTable.tableModel.getValueAt(modelRow, idCol), (String) resultsSyncTable.tableModel.getValueAt(
                  modelRow, currTitleCol), (String) resultsSyncTable.tableModel.getValueAt(modelRow, yearCol));
          if (!bannedTitles.contains(title)) {
            titles.add(title);
          }
        }
      }

      boolean unbannedTitle = !titles.isEmpty();
      titles.addAll(bannedTitles);
      int numTitles = titles.size();

      if (numTitles == 0) {
        JMenuItem menuItem = new JMenuItem(Str.str("empty"));
        menuItem.setEnabled(false);
        menu.add(menuItem);
      } else {
        boolean enableBan = searchBanTitleEnableCheckBoxMenuItem.isSelected();
        if (unbannedTitle) {
          final String title = titles.get(0);
          final AbstractButton banTitleButton = UI.newJCheckBoxMenuItem(title);
          banTitleButton.setEnabled(enableBan);
          banTitleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
              if (banTitleButton.isSelected()) {
                bannedTitles.add(title);
              } else {
                bannedTitles.remove(title);
              }
            }
          });
          menu.add(banTitleButton);
          if (numTitles > 1) {
            menu.add(new Separator());
          }
          titles.remove(0);
        }
        if (!titles.isEmpty()) {
          JList list = new JList(titles.toArray(Constant.EMPTY_STRS));
          list.setEnabled(enableBan);
          list.setCellRenderer(new ListCellRenderer() {
            private static final long serialVersionUID = 1L;
            private final AbstractButton button = new JCheckBoxMenuItem(null, true);

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
              button.setComponentOrientation(list.getComponentOrientation());
              button.setForeground(isSelected ? Color.LIGHT_GRAY : list.getForeground());
              button.setText((String) value);
              button.setEnabled(list.isEnabled());
              button.setFont(list.getFont());
              button.setSelected(!isSelected);
              return button;
            }
          });
          final ListSelectionModel listSelectionModel = list.getSelectionModel();
          final ListModel listModel = list.getModel();
          listSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
              for (int i = 0, size = listModel.getSize(); i < size; i++) {
                String title = (String) listModel.getElementAt(i);
                if (listSelectionModel.isSelectedIndex(i)) {
                  bannedTitles.remove(title);
                } else {
                  bannedTitles.add(title);
                }
              }
            }
          });
          JScrollPane scrollPane = new JScrollPane(list);
          scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
          scrollPane.setBorder(null);
          ((Container) menu.add(new JMenu(" "))).add(scrollPane);
        }
      }
    }//GEN-LAST:event_banTitleMenuMenuSelected

  private static String bannedTitle(String id, String title, String year) {
    return Regex.htmlToPlainText(title) + " (" + UI.innerHTML(year) + ") (" + id + ')';
  }

    private void searchBanTitleMenuMenuSelected(MenuEvent evt) {//GEN-FIRST:event_searchBanTitleMenuMenuSelected
      banTitleMenuMenuSelected(evt);
    }//GEN-LAST:event_searchBanTitleMenuMenuSelected

    private void playlistCopyDownloadLinkMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistCopyDownloadLinkMenuItemActionPerformed
      PlaylistItem playlistItem = selectedPlaylistItem();
      if (playlistItem != null) {
        UI.exportToClipboard(playlistItem.link());
      }
    }//GEN-LAST:event_playlistCopyDownloadLinkMenuItemActionPerformed

    private void popularMoviesMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularMoviesMenuItemActionPerformed
      doPopularVideosSearch(false, false, false, popularMoviesMenuItem);
    }//GEN-LAST:event_popularMoviesMenuItemActionPerformed

  private void trailerPlayerRadioButtonMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_trailerPlayerRadioButtonMenuItemActionPerformed
    if (evt.getModifiers() == ActionEvent.CTRL_MASK) {
      timedMsg(evt.getActionCommand());
    }
  }//GEN-LAST:event_trailerPlayerRadioButtonMenuItemActionPerformed

  private void exportSummaryLink(SelectedTableRow row, VideoStrExportListener strExportListener) {
    strExportListener.export(ContentType.TITLE, String.format(Str.get(781), row.video.id), false, this);
  }

  private void exportPosterImage(SelectedTableRow row, VideoStrExportListener strExportListener) {
    strExportListener.export(ContentType.IMAGE, summary(null, row.video.imagePath.startsWith(Constant.NO_IMAGE) ? Constant.PROGRAM_DIR + "noPosterBig.jpg"
            : row.video.imagePath), false, this);
  }

  private void updateDownloadSizeComboBoxes() {
    String max = (String) maxDownloadSizeComboBox.getSelectedItem();
    if (!max.equals(Constant.INFINITY) && Integer.parseInt((String) minDownloadSizeComboBox.getSelectedItem()) >= Integer.parseInt(max)) {
      maxDownloadSizeComboBox.setSelectedItem(Constant.INFINITY);
    }
  }

  private void updateTVComboBoxes() {
    boolean allSeasons = tvSeasonComboBox.getSelectedItem().equals(Constant.ANY), allEpisodes = tvEpisodeComboBox.getSelectedItem().equals(Constant.ANY);

    if (allSeasons && !allEpisodes) {
      if (tvEpisodeComboBox.isEnabled()) {
        tvEpisodeComboBox.setSelectedItem(Constant.ANY);
        tvEpisodeComboBox.setEnabled(false);
      }
    } else if ((!allSeasons && allEpisodes) || (!allSeasons && !allEpisodes)) {
      if (!tvEpisodeComboBox.isEnabled()) {
        tvEpisodeComboBox.setEnabled(true);
      }
    } else if (tvEpisodeComboBox.isEnabled()) {
      tvEpisodeComboBox.setEnabled(false);
    }

    subtitleEpisodes.add(-1);
  }

  int showOptionDialog(Object msg, String title, int type, Boolean confirm) {
    Window alwaysOnTopFocus = resultsToBackground();
    int result = UI.showOptionDialog(this, summaryScrollPane, msg, title, type, confirm);
    resultsToForeground(alwaysOnTopFocus);
    return result;
  }

  void showException(Exception e) {
    showException(e, false);
  }

  private void showException(Exception e, Boolean confirm) {
    if (Debug.DEBUG) {
      Debug.print(e);
    }
    if (e.getClass().equals(ConnectionException.class)) {
      return;
    }

    showMsg(ThrowableUtil.toString(e), Constant.ERROR_MSG, confirm);
    IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
  }

  private void showOptionalMsg(final String msg, final JMenuItem menuItem) {
    synchronized (optionDialogLock) {
      UI.run(true, new Runnable() {
        @Override
        public void run() {
          showOptionDialog(UI.container(msg, null, menuItem, textComponentPopupListener), Constant.APP_TITLE, Constant.INFO_MSG, null);
        }
      });
    }
  }

  private void showMsg(String msg, int msgType) {
    showMsg(msg, msgType, false);
  }

  private int showMsg(final String msg, final int msgType, final Boolean confirm) {
    synchronized (optionDialogLock) {
      return UI.run(new Callable<Integer>() {
        @Override
        public Integer call() {
          return showOptionDialog(UI.container(msg, null, null, textComponentPopupListener), Constant.APP_TITLE, msgType, confirm);
        }
      });
    }
  }

  private void showConnectionException(Exception e) {
    if (!e.getClass().equals(ConnectionException.class)) {
      return;
    }
    String msg = e.getMessage();
    if (msg == null || msg.isEmpty()) {
      return;
    }
    synchronized (msgDialogLock) {
      String text = Regex.replaceAll(msgEditorPane.getText(), "\\s++", " ");
      if (text.contains(msg)) {
        return;
      }

      text = Regex.match(text, "<html> *+<head> *+</head> *+<body marginwidth=\"10\">", "<br> *+</body> *+</html>");
      if (!text.isEmpty()) {
        text += "<br><hr>";
      }

      msgEditorPane.setText("<html><head></head><body marginwidth=\"10\">" + text + "<p>" + msg + "</p><br></body></html>");
      msgEditorPane.setSelectionStart(0);
      msgEditorPane.setSelectionEnd(0);
      if (!msgDialog.isVisible()) {
        if (connectionIssueButton.isEnabled()) {
          blinkConnectionIssueButton(false);
        } else {
          connectionIssueButton.setEnabled(true);
          connectionIssueButton.setPreferredSize(new Dimension(18, 18));
          connectionIssueButton.setIcon(warningIcon);
          blinkConnectionIssueButton(true);
        }
      }
    }
  }

  private void blinkConnectionIssueButton(final boolean delay) {
    (new Worker() {
      @Override
      public void doWork() {
        if (delay) {
          try {
            Thread.sleep(250);
          } catch (InterruptedException e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
          }
        }
        if (!msgDialog.isVisible()) {
          connectionIssueButton.setIcon(null);
          try {
            Thread.sleep(250);
          } catch (InterruptedException e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
          }
          if (!msgDialog.isVisible()) {
            connectionIssueButton.setIcon(warningIcon);
          }
        }
      }
    }).execute();
  }

  private Window[] windows() {
    return new Window[]{this, dummyDialog /* Backward compatibility */, msgDialog, dummyDialog /* Backward compatibility */, faqFrame, aboutDialog,
      timeoutDialog, tvDialog, resultsPerSearchDialog, downloadSizeDialog, extensionsDialog, languageCountryDialog, dummyDialog /* Backward compatibility */,
      proxyDialog, addProxiesDialog, removeProxiesDialog, profileDialog, profileNameChangeDialog, commentsDialog, portDialog, tvSubtitleDialog,
      movieSubtitleDialog};
  }

  private AbstractButton[] languageButtons() {
    return new AbstractButton[]{englishRadioButtonMenuItem, spanishRadioButtonMenuItem, frenchRadioButtonMenuItem, italianRadioButtonMenuItem,
      dutchRadioButtonMenuItem, portugueseRadioButtonMenuItem, turkishRadioButtonMenuItem};
  }

  private class Settings {

    private static final String EMPTY_LIST = " empty";
    private static final String EMPTY_PATH = "EMPTY";

    Settings() {
    }

    void loadSettings(String settingsFile) {
      try {
        String[] settings = Regex.split(IO.read(settingsFile), Constant.NEWLINE);
        boolean updateSettings = (settings.length < Constant.SETTINGS_LEN);
        if (updateSettings) {
          String[] defaultSettings = Regex.split(IO.read(Constant.PROGRAM_DIR + Constant.DEFAULT_SETTINGS), Constant.NEWLINE);
          System.arraycopy(settings, 0, defaultSettings, 0, settings.length);
          settings = defaultSettings;
        }
        int i = -1;
        changeLocale(settings[Constant.SETTINGS_LEN - 8]);
        i += restoreComboBoxes(settings, i, comboBoxSet1());
        i += restoreButtons(settings, i, buttonSet1());
        ++i; // Backward compatibility
        i += restoreButtons(settings, i, buttonSet2());
        i += restoreComboBoxes(settings, i, proxyComboBox);
        portTextField.setText(settings[++i].equals(Constant.NULL) ? null : settings[i]);
        i += restoreButtons(settings, i, portRandomizeCheckBox);
        if (portRandomizeCheckBox.isSelected()) {
          if (randomPort != null) {
            portTextField.setText(randomPort);
          } else {
            randomPort = String.valueOf(setRandomPort());
          }
        }
        ++i; // Backward compatibility
        ++i; // Backward compatibility

        restoreSize(GUI.this, settings[++i]);
        i += restoreWindows(settings, i, windows());

        restoreList("whitelist", settings[++i], whitelistListModel);
        restoreList("blacklist", settings[++i], blacklistListModel);
        restoreList("languageList", settings[++i], languageList);
        restoreList("countryList", settings[++i], countryList);

        i += restoreButtons(settings, i, dummyMenuItem /* Backward compatibility */, feedCheckBoxMenuItem);

        proxyImportFile = getPath(settings, ++i);
        proxyExportFile = getPath(settings, ++i);
        torrentDir = getPath(settings, ++i);
        subtitleDir = getPath(settings, ++i);

        i += restoreComboBoxes(settings, i, dummyComboBox); // Backward compatibility
        i += restoreComboBoxes(settings, i, dummyComboBox); // Backward compatibility
        i += restoreButtons(settings, i, emailWithDefaultAppCheckBoxMenuItem);
        ++i; // Backward compatibility

        playlistDir = getPath(settings, ++i);
        usePeerBlock = Boolean.parseBoolean(settings[++i]);
        i += restoreButtons(settings, i, playlistAutoOpenCheckBoxMenuItem, dummyMenuItem /* Backward compatibility */,
                playlistShowNonVideoItemsCheckBoxMenuItem, playDefaultAppMenuItem);
        i += restoreColumnWidths(settings, i, resultsTable, yearCol, ratingCol);
        ++i; // language
        ++i; // Backward compatibility

        UI.select(downloaderButtonGroup, Integer.parseInt(settings[++i]));
        UI.select(trailerPlayerButtonGroup2, Integer.parseInt(settings[++i]));
        UI.select(downloadQualityButtonGroup, Integer.parseInt(settings[++i]));
        subtitleFormat = getFormat();
        splitPane.setResizeWeight(Double.parseDouble(settings[++i]));
        if (playlistShown.get()) {
          if (splitPane.getDividerLocation() + splitPane.getDividerSize() < splitPane.getHeight()) {
            splitPane.setDividerLocation(splitPane.getResizeWeight());
          } else {
            splitPane.setLastDividerLocation((int) ((splitPane.getHeight() - splitPane.getDividerSize()) * splitPane.getResizeWeight()));
          }
        }
        restoreButtons(settings, i, autoConfirmCheckBoxMenuItem, searchBanTitleEnableCheckBoxMenuItem);

        if (!updateSettings) {
          return;
        }

        StringBuilder newSettings = new StringBuilder(1024);
        for (String setting : settings) {
          newSettings.append(setting).append(Constant.NEWLINE);
        }
        IO.write(settingsFile, newSettings.toString().trim());
      } catch (Exception e) {
        showException(e);
      }
    }

    public void saveSettings(String fileName) throws Exception {
      StringBuilder settings = new StringBuilder(1024);
      saveComboBoxes(settings, comboBoxSet1());
      saveButtons(settings, buttonSet1());
      settings.append(usePeerBlock).append(Constant.NEWLINE); // Backward compatibility
      saveButtons(settings, buttonSet2());
      saveComboBoxes(settings, proxyComboBox);
      String port = portTextField.getText().trim();
      settings.append(port.isEmpty() ? Constant.NULL : port).append(Constant.NEWLINE);
      saveButtons(settings, portRandomizeCheckBox);
      settings.append(false).append(Constant.NEWLINE); // Backward compatibility
      saveButtons(settings, dummyMenuItem); // Backward compatibility

      settings.append(saveSize(GUI.this));
      for (Window window : windows()) {
        settings.append(savePosition(window));
      }

      saveList(settings, "whitelist", whitelistListModel.toArray());
      saveList(settings, "blacklist", blacklistListModel.toArray());
      saveList(settings, "languageList", languageList.getSelectedValues());
      saveList(settings, "countryList", countryList.getSelectedValues());
      saveButtons(settings, dummyMenuItem /* Backward compatibility */, feedCheckBoxMenuItem);
      savePaths(settings, proxyImportFile, proxyExportFile, torrentDir, subtitleDir);
      saveComboBoxes(settings, dummyComboBox); // Backward compatibility
      saveComboBoxes(settings, dummyComboBox); // Backward compatibility
      saveButtons(settings, emailWithDefaultAppCheckBoxMenuItem);
      settings.append(savePosition(dummyDialog));
      savePaths(settings, playlistDir);
      settings.append(usePeerBlock).append(Constant.NEWLINE);
      saveButtons(settings, playlistAutoOpenCheckBoxMenuItem, dummyMenuItem /* Backward compatibility */, playlistShowNonVideoItemsCheckBoxMenuItem,
              playDefaultAppMenuItem);
      saveColumnWidths(settings, resultsTable, yearCol, ratingCol);

      String language = null;
      for (AbstractButton languageButton : languageButtons()) {
        if (languageButton.isSelected()) {
          language = languageButton.getName();
          break;
        }
      }
      settings.append(language).append(Constant.NEWLINE);
      settings.append(0).append(Constant.NEWLINE); // Backward compatibility
      settings.append(UI.selectedIndex(downloaderButtonGroup)).append(Constant.NEWLINE);
      settings.append(UI.selectedIndex(trailerPlayerButtonGroup2)).append(Constant.NEWLINE);
      settings.append(UI.selectedIndex(downloadQualityButtonGroup)).append(Constant.NEWLINE);
      settings.append(playlistShown.get() ? (splitPane.getDividerLocation() / (double) (splitPane.getHeight() - splitPane.getDividerSize()))
              : splitPane.getResizeWeight()).append(Constant.NEWLINE);
      saveButtons(settings, autoConfirmCheckBoxMenuItem, searchBanTitleEnableCheckBoxMenuItem);

      IO.write(fileName, settings.toString().trim());
    }

    private JComboBox[] comboBoxSet1() {
      return new JComboBox[]{regularResultsPerSearchComboBox, popularMoviesResultsPerSearchComboBox, popularTVShowsResultsPerSearchComboBox,
        minDownloadSizeComboBox, maxDownloadSizeComboBox, timeoutComboBox, tvSubtitleLanguageComboBox, movieSubtitleLanguageComboBox};
    }

    private AbstractButton[] buttonSet1() {
      return new AbstractButton[]{downloadSizeIgnoreCheckBox, safetyCheckBoxMenuItem, peerBlockMenuItem};
    }

    private AbstractButton[] buttonSet2() {
      return new AbstractButton[]{dummyMenuItem /* Backward compatibility */, updateCheckBoxMenuItem, dummyMenuItem /* Backward compatibility */,
        proxyDownloadLinkInfoCheckBox, proxyVideoInfoCheckBox, proxySearchEnginesCheckBox, proxyTrailersCheckBox,
        dummyMenuItem /* Backward compatibility */, proxyUpdatesCheckBox, proxySubtitlesCheckBox, browserNotificationCheckBoxMenuItem};
    }

    private void saveComboBoxes(StringBuilder settings, JComboBox... comboBoxes) {
      for (JComboBox comboBox : comboBoxes) {
        settings.append(comboBox.getSelectedItem()).append(Constant.NEWLINE);
      }
    }

    private int restoreComboBoxes(String[] settings, int settingsIndex, JComboBox... comboBoxes) {
      for (int i = 0, j = settingsIndex + 1; i < comboBoxes.length; i++) {
        comboBoxes[i].setSelectedItem(settings[j + i]);
      }
      return comboBoxes.length;
    }

    private void saveButtons(StringBuilder settings, AbstractButton... buttons) {
      for (AbstractButton button : buttons) {
        settings.append(Boolean.toString(button.isSelected())).append(Constant.NEWLINE);
      }
    }

    private int restoreButtons(String[] settings, int settingsIndex, AbstractButton... buttons) {
      for (int i = 0, j = settingsIndex + 1; i < buttons.length; i++) {
        buttons[i].setSelected(Boolean.parseBoolean(settings[j + i]));
      }
      return buttons.length;
    }

    private StringBuilder saveList(StringBuilder settings, String name, Object[] items) {
      if (items.length == 0) {
        settings.append(name).append(EMPTY_LIST);
      } else {
        for (int i = 0; i < items.length; i++) {
          settings.append(items[i]);
          if (i < items.length - 1) {
            settings.append(':');
          }
        }
      }
      return settings.append(Constant.NEWLINE);
    }

    private void restoreList(String name, String str, DefaultListModel listModel) {
      listModel.clear();
      if (!str.equals(name + EMPTY_LIST)) {
        for (String currStr : Regex.split(str, ":")) {
          listModel.addElement(currStr);
        }
      }
    }

    private void restoreList(String name, String str, JList list) {
      if (!str.equals(name + EMPTY_LIST)) {
        UI.select(list, Regex.split(str, ":"));
      }
    }

    private String saveSize(Window window) {
      Dimension size = window.getSize();
      return size.width + "x" + size.height + Constant.NEWLINE;
    }

    private void restoreSize(Window window, String size) {
      String[] dimensions = Regex.split(size, "x");
      Dimension dimension = new Dimension(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]));
      if (window instanceof Frame && (((Frame) window).getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH && !UI.isMaxSize(window,
              dimension)) {
        ((Frame) window).setExtendedState(Frame.NORMAL);
      }
      window.setSize(dimension);
    }

    private String savePosition(Window window) {
      Point location = window.getLocation();
      return location.x + "," + location.y + Constant.NEWLINE;
    }

    private void restorePosition(Window window, String location) {
      if (location.equals("center")) {
        UI.centerOnScreen(window);
      } else if (location.equals(Constant.NULL)) {
        window.setLocationRelativeTo(GUI.this);
      } else {
        String[] xy = Regex.split(location, ",");
        Point point = new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
        if (UI.isOnScreen(point)) {
          window.setLocation(point);
        } else {
          UI.centerOnScreen(window);
        }
      }
    }

    private int restoreWindows(String[] settings, int settingsIndex, Window... windows) {
      for (int i = 0, j = settingsIndex + 1; i < windows.length; i++) {
        if (windows[i] != GUI.this) {
          windows[i].pack();
        }
        restorePosition(windows[i], settings[j + i]);
      }
      return windows.length;
    }

    private void savePaths(StringBuilder settings, String... paths) {
      for (String path : paths) {
        settings.append(path.isEmpty() ? EMPTY_PATH : path.replace(Constant.FILE_SEPARATOR, Constant.SEPARATOR3)).append(Constant.NEWLINE);
      }
    }

    private String getPath(String[] settings, int settingsIndex) {
      return settings[settingsIndex].equals(EMPTY_PATH) ? "" : settings[settingsIndex].replace(Constant.SEPARATOR3, Constant.FILE_SEPARATOR);
    }

    private void saveColumnWidths(StringBuilder settings, JTable table, int... columns) {
      TableColumnModel colModel = table.getColumnModel();
      for (int column : columns) {
        settings.append(colModel.getColumn(column).getWidth()).append(Constant.NEWLINE);
      }
    }

    private int restoreColumnWidths(String[] settings, int settingsIndex, JTable table, int... columns) {
      TableColumnModel colModel = table.getColumnModel();
      for (int i = 0, j = settingsIndex + 1; i < columns.length; i++) {
        colModel.getColumn(columns[i]).setPreferredWidth(Integer.parseInt(settings[j + i]));
      }
      return columns.length;
    }
  }

  private class HTMLCopyListener implements ActionListener {

    HTMLCopyListener() {
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      String text;
      if ((evt.getActionCommand().equals(Constant.COPY) || evt.getActionCommand().equals(Constant.CUT)) && (text = summaryEditorPane.getSelectedText())
              != null) {
        UI.exportToClipboard(Regex.replaceAll(text.replace("\u200B", ""), "\\s{2,}+", Constant.NEWLINE2).trim());
      }
    }
  }

  private class TableCopyListener implements ActionListener {

    TableCopyListener() {
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      int[] selectedRows, selectedCols;
      if ((!evt.getActionCommand().equals(Constant.COPY) && !evt.getActionCommand().equals(Constant.CUT))
              || (selectedRows = resultsSyncTable.getSelectedRows()).length == 0 || (selectedCols = resultsSyncTable.getSelectedColumns()).length == 0) {
        return;
      }

      List<Integer> modelCols = new ArrayList<Integer>(selectedCols.length);
      for (int i = 0; i < selectedCols.length; i++) {
        int col = resultsSyncTable.convertColumnIndexToModel(selectedCols[i]);
        if (col == imageCol || col == titleCol) {
          col = currTitleCol;
        }
        if (!modelCols.contains(col)) {
          modelCols.add(col);
        }
      }

      StringBuilder str = new StringBuilder(2048), str2 = new StringBuilder(64);
      for (int i = 0; i < selectedRows.length; i++) {
        str2.setLength(0);
        int modelRow = resultsSyncTable.convertRowIndexToModel(selectedRows[i]);
        for (int modelCol : modelCols) {
          String val = (String) resultsSyncTable.getModelValueAt(modelRow, modelCol);
          str2.append(modelCol == currTitleCol ? Regex.htmlToPlainText(val) : UI.innerHTML(val)).append('\t');
        }
        str.append(str2.toString().trim()).append(Constant.NEWLINE);
      }
      UI.exportToClipboard(str.toString().trim());
    }
  }

  private class PlaylistTableCopyListener implements ActionListener {

    PlaylistTableCopyListener() {
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      if ((!evt.getActionCommand().equals(Constant.COPY) && !evt.getActionCommand().equals(Constant.CUT))) {
        return;
      }

      StringBuilder str;
      synchronized (playlistSyncTable.lock) {
        int[] selectedRows = playlistSyncTable.table.getSelectedRows(), selectedCols;
        if (selectedRows.length == 0 || (selectedCols = playlistSyncTable.table.getSelectedColumns()).length == 0) {
          return;
        }

        str = new StringBuilder(2048);
        StringBuilder str2 = new StringBuilder(64);
        for (int i = 0; i < selectedRows.length; i++) {
          str2.setLength(0);
          for (int j = 0; j < selectedCols.length; j++) {
            str2.append(playlistSyncTable.table.getValueAt(selectedRows[i], selectedCols[j])).append('\t');
          }
          str.append(str2.toString().trim()).append(Constant.NEWLINE);
        }
      }
      UI.exportToClipboard(str.toString().trim());
    }
  }

  private class SelectedTableRow {

    final int viewVal, val;
    final Video video;

    SelectedTableRow() {
      viewVal = resultsSyncTable.getSelectedRow();
      val = resultsSyncTable.convertRowIndexToModel(viewVal);
      video = new Video((String) resultsSyncTable.getModelValueAt(val, idCol), (String) resultsSyncTable.getModelValueAt(val, currTitleCol),
              UI.innerHTML((String) resultsSyncTable.getModelValueAt(val, yearCol)), resultsSyncTable.getModelValueAt(val, isTVShowCol).equals("1"),
              resultsSyncTable.getModelValueAt(val, isTVShowAndMovieCol).equals("1"));
      video.oldTitle = (String) resultsSyncTable.getModelValueAt(val, oldTitleCol);
      video.imagePath = (String) resultsSyncTable.getModelValueAt(val, imageCol);
      video.summary = (String) resultsSyncTable.getModelValueAt(val, summaryCol);
      video.imageLink = (String) resultsSyncTable.getModelValueAt(val, imageLinkCol);
      video.season = (String) resultsSyncTable.getModelValueAt(val, seasonCol);
      video.episode = (String) resultsSyncTable.getModelValueAt(val, episodeCol);
    }

    VideoStrExportListener strExportListener(boolean exportToEmail) {
      VideoStrExportListener strExportListener = strExportListener(exportToEmail, false, exportToEmail ? 3 : 1);
      if (exportToEmail) {
        readSummaryActionPerformed(this, false, strExportListener);
        exportPosterImage(this, strExportListener);
      }
      return strExportListener;
    }

    VideoStrExportListener strExportListener(boolean exportToEmail, boolean exportSecondaryContent, int numStrsToExport) {
      return new VideoStrExporter(video.title, video.year, video.isTVShow, exportToEmail, exportSecondaryContent, numStrsToExport);
    }
  }

  @Override
  public void loading(boolean isLoading) {
    if (isLoading) {
      if (loadingLabel.getIcon().equals(notLoadingIcon)) {
        loadingLabel.setIcon(loadingIcon);
      }
    } else if (workerListener.areWorkersDone()) {
      loadingLabel.setIcon(notLoadingIcon);
    }
  }

  @Override
  public void error(Exception e) {
    showConnectionException(e);
    showException(e, null);
  }

  @Override
  public void enable(Boolean enablePrimary, Boolean enableSecondary, Boolean startPrimary, ContentType contentType) {
    AbstractButton[] primaryButtons1;
    AbstractButton primaryButtons2 = null;
    Boolean enablePrimary1 = enablePrimary, enablePrimary2 = null, enableSecondary2 = null;
    Component[] secondaryComponents1, secondaryComponents2 = null;
    if (contentType == ContentType.SUMMARY) {
      primaryButtons1 = new AbstractButton[]{readSummaryButton};
      secondaryComponents1 = new Component[]{readSummaryMenuItem, hearSummaryMenuItem, copyFullTitleAndYearMenuItem};
    } else if (contentType == ContentType.TRAILER) {
      primaryButtons1 = new AbstractButton[]{watchTrailerButton};
      secondaryComponents1 = new Component[]{watchTrailerMenuItem, emailTrailerLinkMenuItem, copyTrailerLinkMenuItem};
    } else {
      if (contentType == ContentType.DOWNLOAD1 || contentType == ContentType.DOWNLOAD3) {
        primaryButtons1 = new AbstractButton[]{downloadLink1Button};
        secondaryComponents1 = new Component[]{downloadLink1MenuItem, emailDownloadLink1MenuItem, copyDownloadLink1MenuItem};
        secondaryComponents2 = new Component[]{downloadLink2Button, downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem};
      } else if (contentType == ContentType.DOWNLOAD2) {
        primaryButtons1 = new AbstractButton[]{downloadLink2Button};
        enablePrimary1 = (isAltSearch ? Boolean.FALSE : enablePrimary);
        secondaryComponents1 = new Component[]{downloadLink1Button, downloadLink1MenuItem, emailDownloadLink1MenuItem, copyDownloadLink1MenuItem};
        secondaryComponents2 = new Component[]{downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem};
      } else {
        primaryButtons1 = new AbstractButton[]{downloadLink1Button};
        primaryButtons2 = downloadLink2Button;
        enablePrimary2 = (isAltSearch ? Boolean.FALSE : enablePrimary);
        secondaryComponents1 = new Component[]{downloadLink1MenuItem, emailDownloadLink1MenuItem, copyDownloadLink1MenuItem};
        secondaryComponents2 = new Component[]{downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem};
      }
      enableSecondary2 = (isAltSearch ? Boolean.FALSE : enableSecondary);
    }
    UI.enable(primaryButtons1, enablePrimary1, startPrimary, primaryButtons2, enablePrimary2, secondaryComponents1, enableSecondary, secondaryComponents2,
            enableSecondary2);
  }

  @Override
  public void enable(ContentType contentType) {
    Boolean enable = (resultsSyncTable.getSelectedRows().length == 1);
    enable(enable, enable, Boolean.TRUE, contentType);
  }

  @Override
  public void altVideoDownloadStarted() {
    if (!isAltSearch) {
      showConnectionException(new ConnectionException(Str.str("connectionProblem", Connection.getShortUrl(Connection.downloadLinkInfoFailUrl(), false))
              + " <font color=\"red\">" + Str.str("switchingToBackupMode") + "</font>"));
      exitBackupModeButton.setEnabled(true);
      exitBackupModeButton.setBorderPainted(true);
      exitBackupModeButton.setText(Str.str("GUI.exitBackupModeButton.text2"));
      resizeExitBackupModeButton();
      isAltSearch = true;
    }
  }

  @Override
  public void msg(String msg, int msgType) {
    showMsg(msg, msgType, null);
  }

  @Override
  public void timedMsg(final String msg) {
    timedMsg(msg, -1);
  }

  @Override
  public void timedMsg(final String msg, long millis) {
    if (timedMsgThread != null) {
      timedMsgThread.cancel(true);
    }
    (timedMsgThread = new Worker() {
      @Override
      public void doWork() {
        synchronized (timedMsgLock) {
          timedMsgLabel.setText(msg);
          timedMsgDialog.pack();
          timedMsgDialog.setLocationRelativeTo(GUI.this);
          UI.show(timedMsgDialog);
          try {
            Thread.sleep(millis < 0 ? Regex.split(msg, " ").length * 400L : millis);
            timedMsgDialog.setVisible(false);
          } catch (InterruptedException e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
          }
        }
      }
    }).execute();
  }

  Window resultsToBackground() {
    Window alwaysOnTopFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    resultsToBackground2();
    if (alwaysOnTopFocus != null) {
      if (alwaysOnTopFocus.isAlwaysOnTop()) {
        alwaysOnTopFocus.setAlwaysOnTop(false);
      } else {
        return null;
      }
    }
    return alwaysOnTopFocus;
  }

  private void resultsToBackground2() {
    JScrollBar resultsScrollBar = resultsScrollPane.getVerticalScrollBar();
    JScrollBar resultsScrollBarCopy = new JScrollBar(resultsScrollBar.getOrientation(), resultsScrollBar.getValue(),
            resultsScrollBar.getVisibleAmount(), resultsScrollBar.getMinimum(), resultsScrollBar.getMaximum());
    resultsScrollBarCopy.setUnitIncrement(resultsScrollBar.getUnitIncrement());
    resultsScrollBarCopy.setBlockIncrement(resultsScrollBar.getBlockIncrement());
    resultsScrollPane.setVerticalScrollBar(resultsScrollBarCopy); // Stop scrolling
  }

  void resultsToForeground(Window alwaysOnTopFocus) {
    if (alwaysOnTopFocus != null) {
      alwaysOnTopFocus.setAlwaysOnTop(true);
    }
  }

  @Override
  public boolean canProceedWithUnsafeDownload(final String name, final int numFakeComments, final int numComments, final String link, final String comments) {
    synchronized (optionDialogLock) {
      return UI.run(new Callable<Boolean>() {
        @Override
        public Boolean call() {
          final boolean autoConfirm = autoConfirmCheckBoxMenuItem.isSelected();
          Container container = UI.container("<html><head><title></title></head><body>" + System.getProperty("htmlFont2") + Str.str(
                  "linkSafetyWarningPart1", name) + (numComments == 0 ? "" : " " + Str.htmlLinkStr("linkSafetyWarningPart2", link, numFakeComments + "/"
                                  + numComments + " (" + Str.percent(numFakeComments / (double) numComments, 1) + ')')) + "<br><br>" + Str.str(
                  "linkSafetyWarningPart3") + (autoConfirm ? ' ' + UIManager.getString("OptionPane.noButtonText") + '.' : "")
                  + "</font></body></html>", autoConfirmCheckBoxMenuItem, null, textComponentPopupListener);
          UI.addHyperlinkListener((JEditorPane) ((JScrollPane) container.getComponent(0)).getViewport().getView(), new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent evt) {
              if (evt.getEventType().equals(EventType.ACTIVATED)) {
                commentsTextPane.setText(comments);
                commentsTextPane.setSelectionStart(0);
                commentsTextPane.setSelectionEnd(0);
                if (autoConfirm) {
                  commentsDialog.setModal(false);
                  UI.show(commentsDialog);
                } else {
                  commentsDialog.setModal(true);
                  Window alwaysOnTopFocus = resultsToBackground();
                  UI.show(commentsDialog);
                  resultsToForeground(alwaysOnTopFocus);
                }
              }
            }
          });
          if (autoConfirm) {
            showOptionDialog(container, Constant.APP_TITLE, JOptionPane.WARNING_MESSAGE, null);
            return true;
          } else {
            return showOptionDialog(container, Constant.APP_TITLE, JOptionPane.YES_NO_OPTION, true) == JOptionPane.NO_OPTION;
          }
        }
      });
    }
  }

  @Override
  public String summary(String summary, String imagePath) {
    String posterCol = "";
    if (imagePath != null) {
      String imageSize = "";
      String posterFilePath = Constant.TEMP_DIR + (new File(imagePath)).getName();
      File posterFile = new File(posterFilePath);
      if (!posterFile.exists()) {
        try {
          RenderedImage posterImage = UI.image(new ImageIcon((new ImageIcon(imagePath)).getImage().getScaledInstance(Integer.parseInt(Str.get(495)), -1,
                  Image.SCALE_SMOOTH)));
          ImageIO.write(posterImage, "png", posterFile);
          if (summary == null) {
            return posterFilePath;
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          IO.fileOp(posterFile, IO.RM_FILE);
          posterFilePath = imagePath;
          if (summary == null) {
            return posterFilePath;
          }
          imageSize = " width=\"" + Str.get(495) + "\" height=\"" + Str.get(496) + "\"";
        }
      }
      if (summary == null) {
        return posterFilePath;
      }
      posterCol = "<td align=\"left\" valign=\"top\"><img src=\"file:///" + Regex.replaceAll(posterFilePath, 237) + '"' + imageSize + "></td>";
    }
    summaryEditorPane.setText(Regex.replaceFirst(summary, Pattern.quote("<!--poster-->"), posterCol));
    summaryEditorPane.setSelectionStart(0);
    summaryEditorPane.setSelectionEnd(0);
    return null;
  }

  @Override
  public Element getSummaryElement(String id) {
    return UI.run(() -> summaryEditorPaneDocument.getElement(id));
  }

  @Override
  public void insertAfterSummaryElement(Element element, String text) {
    UI.run(false, () -> {
      try {
        summaryEditorPaneDocument.insertAfterEnd(element, System.getProperty("htmlFont1") + text + "</font>");
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    });
  }

  @Override
  public void browserNotification(DomainType domainType) {
    if (!browserNotificationCheckBoxMenuItem.isSelected()) {
      return;
    }

    JCheckBox proxyCheckBox;
    String type;
    if (domainType == DomainType.DOWNLOAD_LINK_INFO) {
      proxyCheckBox = proxyDownloadLinkInfoCheckBox;
      type = "Download";
    } else if (domainType == DomainType.VIDEO_INFO) {
      proxyCheckBox = proxyVideoInfoCheckBox;
      type = "Summary";
    } else if (domainType == DomainType.TRAILER) {
      proxyCheckBox = proxyTrailersCheckBox;
      type = "Trailer";
    } else {
      return;
    }

    String proxy = (proxyCheckBox.isSelected() ? Connection.getProxy(getSelectedProxy()) : Constant.NO_PROXY);
    String[] proxyParts;
    showOptionalMsg(Str.str("browse" + type + "Link") + (proxy == null || proxy.equals(Constant.NO_PROXY) ? "" : ' ' + Str.str("setWebBrowserProxy",
            (proxyParts = Regex.split(proxy, 256))[0], proxyParts[1])), browserNotificationCheckBoxMenuItem);
  }

  @Override
  public void startPeerBlock() {
    if (playlistDownloaderRadioButtonMenuItem.isSelected()) {
      return;
    }
    boolean canShowPeerBlock = peerBlockNotificationCheckBoxMenuItem.isSelected();
    if (!Constant.WINDOWS_XP_AND_HIGHER || (!usePeerBlock && !canShowPeerBlock) || (new File(Constant.APP_DIR + Constant.PEER_BLOCK + "Running")).exists()) {
      return;
    }
    if ((new File(Constant.APP_DIR + Constant.PEER_BLOCK + "Exit")).exists() || WindowsUtil.isProcessRunning(Constant.PEER_BLOCK)) {
      usePeerBlock = false;
      peerBlockNotificationCheckBoxMenuItem.setSelected(false);
      IO.fileOp(Constant.APP_DIR + Constant.PEER_BLOCK + "Exit", IO.RM_FILE_NOW_AND_ON_EXIT);
      return;
    }
    if (canShowPeerBlock && showConfirm(Str.str("startPeerblock"), peerBlockNotificationCheckBoxMenuItem) != JOptionPane.YES_OPTION) {
      usePeerBlock = false;
      return;
    }

    usePeerBlock = true;

    try {
      WindowsUtil.startPeerBlock();
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      showMsg(Str.str("peerblockStartError") + ' ' + ThrowableUtil.toString(e), Constant.ERROR_MSG);
    }
  }

  private String save(final JFileChooser fileChooser, final FileFilter fileFilter, final String selectedFilePath, File file) throws Exception {
    final AtomicReference<File> selectedFileRef = new AtomicReference<File>();
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setSelectedFile(new File(selectedFilePath));
        if (save(fileChooser)) {
          selectedFileRef.set(fileChooser.getSelectedFile());
        }
      }
    });
    File selectedFile = selectedFileRef.get();
    if (selectedFile != null) {
      IO.write(file, selectedFile);
      return IO.parentDir(selectedFile);
    }
    return null;
  }

  private boolean save(JFileChooser fileChooser) {
    Window alwaysOnTopFocus = resultsToBackground();
    UI.nonModalDialogsToBackground(fileChooser);
    boolean result = (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION);
    resultsToForeground(alwaysOnTopFocus);
    return result;
  }

  @Override
  public void saveTorrent(File torrentFile) throws Exception {
    String saveDir = save(torrentFileChooser, torrentFileFilter, torrentDir + torrentFile.getName(), torrentFile);
    if (saveDir != null) {
      torrentDir = saveDir;
    }
  }

  @Override
  public void saveSubtitle(String saveFileName, File subtitleFile) throws Exception {
    String saveDir = save(subtitleFileChooser, subtitleFileFilter, subtitleDir + saveFileName, subtitleFile);
    if (saveDir != null) {
      subtitleDir = saveDir;
    }
  }

  @Override
  public boolean tvChoices(final String season, final String episode, boolean enableEpisode) {
    Window alwaysOnTopFocus = resultsToBackground();
    tvSeasonComboBox.setSelectedItem(season);
    if (enableEpisode) {
      tvEpisodeComboBox.setSelectedItem(episode);
    } else {
      final Object selectedEpisode = tvEpisodeComboBox.getSelectedItem();
      final ActionListener tvSeasonComboBoxActionListener = tvSeasonComboBox.getActionListeners()[0];
      tvSeasonComboBox.removeActionListener(tvSeasonComboBoxActionListener);
      final ActionListener tvEpisodeComboBoxActionListener = tvEpisodeComboBox.getActionListeners()[0];
      tvEpisodeComboBox.removeActionListener(tvEpisodeComboBoxActionListener);
      tvEpisodeComboBox.setSelectedItem(Constant.ANY);
      tvEpisodeComboBox.setEnabled(false);
      tvDialog.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentHidden(ComponentEvent evt) {
          if (season.equals(tvSeasonComboBox.getSelectedItem())) {
            tvEpisodeComboBox.setSelectedItem(episode.isEmpty() ? selectedEpisode : episode);
          }
          updateTVComboBoxes();
          tvSeasonComboBox.addActionListener(tvSeasonComboBoxActionListener);
          tvEpisodeComboBox.addActionListener(tvEpisodeComboBoxActionListener);
          tvDialog.removeComponentListener(this);
        }
      });
    }
    cancelTVSelection = true;
    UI.show(tvDialog);
    resultsToForeground(alwaysOnTopFocus);
    return cancelTVSelection;
  }

  @Override
  public String getTitle(int row, String titleID) {
    return (String) resultsSyncTable.getModelValueAt(row, titleCol, idCol, titleID);
  }

  @Override
  public void setTitle(final String title, final int row, final String titleID) {
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.setModelValueAt(title, row, titleCol, idCol, titleID);
      }
    });
  }

  @Override
  public void setSummary(final String summary, final int row, final String titleID) {
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.setModelValueAt(summary, row, summaryCol, idCol, titleID);
      }
    });
  }

  @Override
  public String getSeason(int row, String titleID) {
    return (String) resultsSyncTable.getModelValueAt(row, seasonCol, idCol, titleID);
  }

  @Override
  public void setSeason(final String season, final int row, final String titleID) {
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.setModelValueAt(season, row, seasonCol, idCol, titleID);
      }
    });
  }

  @Override
  public void setEpisode(final String episode, final int row, final String titleID) {
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.setModelValueAt(episode, row, episodeCol, idCol, titleID);
      }
    });
  }

  @Override
  public void setImageLink(final String imageLink, final int row, final String titleID) {
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.setModelValueAt(imageLink, row, imageLinkCol, idCol, titleID);
      }
    });
  }

  @Override
  public void setImagePath(final String imagePath, final int row, final String titleID) {
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.setModelValueAt(imagePath, row, imageCol, idCol, titleID);
      }
    });
  }

  @Override
  public String getSeason() {
    return (String) tvSeasonComboBox.getSelectedItem();
  }

  @Override
  public String getEpisode() {
    return (String) tvEpisodeComboBox.getSelectedItem();
  }

  @Override
  public void searchStarted() {
    loading(true);
    UI.enable(new AbstractButton[]{searchButton}, false, new Component[]{loadMoreResultsButton, popularNewHQTVShowsMenuItem, popularTVShowsMenuItem,
      popularNewHQMoviesMenuItem, popularMoviesMenuItem}, false);
    resultsSyncTable.requestFocusInWindow();
  }

  @Override
  public void newSearch(boolean isTVShow) {
    exitBackupModeButton.setText(null);
    exitBackupModeButton.setBorderPainted(false);
    exitBackupModeButton.setEnabled(false);
    resizeExitBackupModeButton();
    UI.run(true, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.setRowCount(0);
      }
    });
    isAltSearch = false;
    searchProgressUpdate(0, 0);

    Connection.unfailDownloadLinkInfo();
    posters.clear();
    if (isTVShow) {
      trailerEpisodes.clear();
      downloadLinkEpisodes.clear();
      subtitleEpisodes.clear();
    }
  }

  @Override
  public void searchStopped() {
    try {
      for (int i = 0; i < 25; i++) {
        if (resultsSyncTable.getRowCount() > 0 && resultsSyncTable.getSelectedRow() == -1) {
          UI.run(false, new Runnable() {
            @Override
            public void run() {
              JViewport viewport = (JViewport) resultsSyncTable.table.getParent();
              if ((new Rectangle(viewport.getExtentSize())).intersects(rectangle(viewport, resultsSyncTable.getCellRect(0, 0, true)))) {
                resultsSyncTable.setRowSelectionInterval(0, 0);
              }
            }
          });
          break;
        }
        Thread.sleep(10);
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }

    UI.enable(new AbstractButton[]{searchButton}, true, new Component[]{popularNewHQTVShowsMenuItem, popularTVShowsMenuItem, popularNewHQMoviesMenuItem,
      popularMoviesMenuItem}, true);

    if (popularPopupMenu.isVisible() && popularSearchMenuElement != null) {
      popularPopupMenu.setVisible(false);
      popularPopupMenu.setVisible(true);
      MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{popularPopupMenu, popularSearchMenuElement});
    }

    if (titleTextField.isEnabled() && resultsSyncTable.getRowCount() == 0) {
      titleTextField.requestFocusInWindow();
    }
  }

  @Override
  public void moreResults(boolean areMoreResults) {
    loadMoreResultsButton.setEnabled(areMoreResults);
  }

  @Override
  public void newResult(final Object[] result) {
    UI.run(false, new Runnable() {
      @Override
      public void run() {
        resultsSyncTable.addRow(result);
      }
    });
    posterImagePaths.add((String) result[imageCol]);
  }

  @Override
  public void searchProgressUpdate(int numResults, double progress) {
    searchProgressTextField.putClientProperty("numResults", numResults);
    searchProgressTextField.putClientProperty("progress", progress);
    searchProgressTextField.setText(' ' + Str.str("result" + (numResults == 1 ? "" : "s"), "000", Str.percent(progress, 0)).replaceFirst("000", String.valueOf(
            numResults)) + ' ');
  }

  @Override
  public boolean newPlaylistItems(final List<Object[]> items, final int insertRow, final int primaryItemIndex) {
    return UI.run(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        boolean isPrimaryItemNew = true;
        synchronized (playlistSyncTable.lock) {
          for (int i = 0, numItems = items.size(), currInsertRow = insertRow, prevInsertRow; i < numItems; i++) {
            if ((prevInsertRow = newPlaylistItemHelper(items.get(i), currInsertRow)) >= 0) {
              currInsertRow = prevInsertRow + 1;
            } else if (i == primaryItemIndex) {
              isPrimaryItemNew = false;
            }
          }
        }
        return isPrimaryItemNew;
      }
    });
  }

  @Override
  public int newPlaylistItem(final Object[] item, final int insertRow) {
    return UI.run(new Callable<Integer>() {
      @Override
      public Integer call() {
        synchronized (playlistSyncTable.lock) {
          return newPlaylistItemHelper(item, insertRow);
        }
      }
    });
  }

  private int newPlaylistItemHelper(final Object[] item, final int insertRow) {
    int numRows = playlistSyncTable.tableModel.getRowCount();
    for (int row = numRows - 1; row > -1; row--) {
      if (playlistSyncTable.tableModel.getValueAt(row, playlistItemCol).equals(item[playlistItemCol])) {
        if (!item[playlistNameCol].equals(playlistSyncTable.tableModel.getValueAt(row, playlistNameCol))) {
          playlistSyncTable.tableModel.setValueAt(item[playlistNameCol], row, playlistNameCol);
        }
        return -1;
      }
    }
    if (insertRow >= 0 && insertRow < numRows) {
      try {
        playlistSyncTable.tableModel.insertRow(insertRow, item);
        return insertRow;
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
    playlistSyncTable.tableModel.addRow(item);
    return numRows;
  }

  @Override
  public boolean isBanned(String id, String title, String year) {
    return searchBanTitleEnableCheckBoxMenuItem.isSelected() && bannedTitles.contains(bannedTitle(id, title, year));
  }

  @Override
  public boolean unbanDownload(Long downloadID, String downloadName) {
    if (bannedDownloadIDs.contains(downloadID)) {
      if (showConfirm(Str.str("banDownloadConfirm", downloadName), null) == JOptionPane.YES_OPTION) {
        return false;
      }
      bannedDownloadIDs.remove(downloadID);
      return true;
    }
    return true;
  }

  @Override
  public int removePlaylistItem(final PlaylistItem playlistItem) {
    return UI.run(() -> {
      synchronized (playlistSyncTable.lock) {
        int row = playlistSyncTable.tableModel.getRowCount() - 1;
        for (; row > -1; row--) {
          if (playlistSyncTable.tableModel.getValueAt(row, playlistItemCol).equals(playlistItem)) {
            int[] viewRows = playlistSyncTable.table.getSelectedRows();
            playlistSyncTable.tableModel.removeRow(row);
            if (viewRows.length != 0) {
              selectFirstRow(viewRows);
            }
            return row;
          }
        }
        return row;
      }
    });
  }

  @Override
  public void setPlaylistItemProgress(final FormattedNum progress, final PlaylistItem playlistItem, final boolean updateValOnly) {
    UI.run(false, new Runnable() {
      @Override
      public void run() {
        synchronized (playlistSyncTable.lock) {
          for (int row = playlistSyncTable.tableModel.getRowCount() - 1; row > -1; row--) {
            if (playlistSyncTable.tableModel.getValueAt(row, playlistItemCol).equals(playlistItem)) {
              FormattedNum oldProgress = (FormattedNum) playlistSyncTable.tableModel.getValueAt(row, playlistProgressCol);
              if (updateValOnly) {
                Number newProgressVal = progress.val();
                if (!newProgressVal.equals(oldProgress.val())) {
                  playlistSyncTable.tableModel.setValueAt(oldProgress.copy(newProgressVal), row, playlistProgressCol);
                }
              } else if (!progress.equals(oldProgress)) {
                playlistSyncTable.tableModel.setValueAt(progress, row, playlistProgressCol);
                if (trayIcon != null && playlistItem.isActive()) {
                  trayIcon.setToolTip(Regex.split(trayIcon.getToolTip(), "\n")[0] + "\n" + playlistSyncTable.tableModel.getValueAt(row,
                          playlistProgressCol) + "\n" + playlistSyncTable.tableModel.getValueAt(row, playlistNameCol));
                }
              }
              return;
            }
          }
        }
      }
    });
  }

  @Override
  public void showPlaylist() {
    if (playlistShown.compareAndSet(false, true)) {
      UI.run(false, new Runnable() {
        @Override
        public void run() {
          splitPane.setDividerLocation(splitPane.getResizeWeight());
        }
      });
    }
    restorePlaylist(false);
  }

  @Override
  public boolean selectPlaylistItem(final PlaylistItem playlistItem) {
    boolean selected = UI.run(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        synchronized (playlistSyncTable.lock) {
          for (int row = playlistSyncTable.tableModel.getRowCount() - 1; row > -1; row--) {
            if (playlistSyncTable.tableModel.getValueAt(row, playlistItemCol).equals(playlistItem)) {
              int viewRow = playlistSyncTable.table.convertRowIndexToView(row);
              if (viewRow != -1) {
                boolean disable = playlistSyncTable.table.getSelectedRowCount() != 0;
                playlistSyncTable.table.setRowSelectionInterval(viewRow, viewRow);
                JViewport viewport = (JViewport) playlistSyncTable.table.getParent();
                viewport.scrollRectToVisible(rectangle(viewport, playlistSyncTable.table.getCellRect(viewRow, 0, true)));
                if (disable) {
                  playlistTableValueChanged(new ListSelectionEvent(playlistSyncTable.table.getSelectionModel(), -1, -1, false));
                }
                return true;
              }
              break;
            }
          }
          return false;
        }
      }
    });
    refreshPlaylistControls();
    return selected;
  }

  @Override
  public String getPlaylistSaveDir() {
    return playlistDir;
  }

  @Override
  public void playlistError(final String msg) {
    synchronized (optionDialogLock) {
      UI.run(true, new Runnable() {
        @Override
        public void run() {
          showOptionDialog(UI.container(msg, null, null, textComponentPopupListener), Constant.APP_TITLE, Constant.ERROR_MSG, null);
        }
      });
    }
  }

  private void play(PlaylistItem[] playlistItems) {
    if (playlistItems == null || playlistItems.length > 1) {
      playlistBanGroupButton.setIcon(banIcon);
      playlistBanGroupMenuItem.setText(Str.str("banGroup"));
      UI.enable(false, playlistBanGroupButton, playlistBanGroupMenuItem, playlistReloadGroupButton, playlistReloadGroupMenuItem, playlistOpenButton,
              playlistOpenMenuItem, playlistCopyDownloadLinkMenuItem);
      if (playlistItems == null) {
        playlistPlayButton.setIcon(playIcon);
        playlistPlayMenuItem.setText(Str.str("play"));
        UI.enable(false, playlistPlayButton, playlistPlayMenuItem);
        return;
      }
    }
    PlaylistItem playlistItem = playlistItems[0];
    if (playlistItems.length == 1) {
      boolean isBanned = bannedDownloadIDs.contains(playlistItem.groupDownloadID()), canBan = playlistItem.canBan();
      playlistBanGroupButton.setIcon(isBanned ? unbanIcon : banIcon);
      playlistBanGroupButton.setEnabled(canBan);
      playlistBanGroupMenuItem.setText(Str.str((isBanned ? "un" : "") + "banGroup"));
      playlistBanGroupMenuItem.setEnabled(canBan);
      playlistCopyDownloadLinkMenuItem.setEnabled(canBan);
      UI.enable(playlistItem.canOpen(), playlistReloadGroupButton, playlistReloadGroupMenuItem, playlistOpenButton, playlistOpenMenuItem);
    }
    boolean play = playlistItem.canPlay(), active = playlistItem.isActive();
    playlistPlayButton.setIcon(play ? playIcon : stopIcon);
    playlistPlayMenuItem.setText(Str.str(play ? "play" : Constant.STOP_KEY));
    UI.enable((play && !active) || (!play && active && playlistItem.isStoppable()), playlistPlayButton, playlistPlayMenuItem);
  }

  @Override
  public void refreshPlaylistControls() {
    UI.run(false, new Runnable() {
      @Override
      public void run() {
        play(selectedPlaylistItems());
      }
    });
  }

  @Override
  public void setPlaylistPlayHint(Long numBlockedIps) {
    String numBlockedIpsMsg = (numBlockedIps != null && numBlockedIps > 0 ? ' ' + Str.str("ipFiltering", Str.getNumFormat("#,###").format(numBlockedIps)) : "");
    playlistPlayButton.setToolTipText(Str.str("GUI.playlistPlayButton.toolTipText") + numBlockedIpsMsg);
    playlistPlayButton.putClientProperty("numBlockedIps", numBlockedIps);
    playlistPlayButton.putClientProperty("numBlockedIpsMsg", numBlockedIpsMsg);
    playlistPlayMenuItem.setToolTipText(Str.str("GUI.playlistPlayMenuItem.toolTipText") + numBlockedIpsMsg);
  }

  @Override
  public String getNumBlockedIpsMsg() {
    return Objects.toString(playlistPlayButton.getClientProperty("numBlockedIpsMsg"), "");
  }

  private int showConfirm(final String msg, final JMenuItem menuItem) {
    synchronized (optionDialogLock) {
      return UI.run(new Callable<Integer>() {
        @Override
        public Integer call() {
          if (autoConfirmCheckBoxMenuItem.isSelected()) {
            showOptionDialog(UI.container(msg + ' ' + UIManager.getString("OptionPane.yesButtonText") + '.', autoConfirmCheckBoxMenuItem, menuItem,
                    textComponentPopupListener), Constant.APP_TITLE, Constant.INFO_MSG, null);
            return JOptionPane.YES_OPTION;
          } else {
            return showOptionDialog(UI.container(msg, autoConfirmCheckBoxMenuItem, menuItem, textComponentPopupListener), Constant.APP_TITLE,
                    JOptionPane.YES_NO_OPTION, true);
          }
        }
      });
    }
  }

  @Override
  public boolean isConfirmed(String msg) {
    return showMsg(msg, JOptionPane.YES_NO_OPTION, true) == JOptionPane.YES_OPTION;
  }

  @Override
  public boolean isAuthorizationConfirmed(final String msg) {
    synchronized (optionDialogLock) {
      return UI.run(new Callable<Boolean>() {
        @Override
        public Boolean call() {
          authenticationMessageLabel.setText(msg);
          return showOptionDialog(authenticationPanel, Str.str("authenticationRequired"), JOptionPane.OK_CANCEL_OPTION, true) == JOptionPane.OK_OPTION;
        }
      });
    }
  }

  @Override
  public String getAuthorizationUsername() {
    return authenticationUsernameTextField.getText();
  }

  @Override
  public char[] getAuthorizationPassword() {
    char[] password = authenticationPasswordField.getPassword();
    authenticationPasswordField.setText(null);
    return password;
  }

  @Override
  public void proxyListDownloadStarted() {
    proxyLoadingLabel.setIcon(loadingIcon);
    enableProxyButtons(false);
  }

  @Override
  public void proxyListDownloadStopped() {
    enableProxyButtons(true);
    proxyLoadingLabel.setIcon(notLoadingIcon);
  }

  @Override
  public void newProxies(Iterable<String> proxies) {
    proxyComboBox.removeAllItems();
    proxyComboBox.addItem(Constant.NO_PROXY);
    for (String proxy : proxies) {
      proxyComboBox.addItem(proxy);
    }
    proxyComboBox.setSelectedItem(Constant.NO_PROXY);
  }

  @Override
  public int getTimeout() {
    return Integer.parseInt((String) timeoutComboBox.getSelectedItem());
  }

  @Override
  public int getDownloadLinkTimeout() {
    return noDownloaderRadioButtonMenuItem.isSelected() || !blacklistListModel.isEmpty() ? 45 : 0;
  }

  @Override
  public void setStatusBar(String msg) {
    if (!statusBarTextField.getText().equals(msg)) {
      statusBarTextField.setText(msg);
    }
  }

  @Override
  public void clearStatusBar() {
    if (!statusBarTextField.getText().isEmpty()) {
      statusBarTextField.setText(null);
    }
  }

  @Override
  public String getSelectedProxy() {
    return (String) proxyComboBox.getSelectedItem();
  }

  @Override
  public boolean canProxyDownloadLinkInfo() {
    return proxyDownloadLinkInfoCheckBox.isSelected();
  }

  @Override
  public boolean canProxyVideoInfo() {
    return proxyVideoInfoCheckBox.isSelected();
  }

  @Override
  public boolean canProxySearchEngines() {
    return proxySearchEnginesCheckBox.isSelected();
  }

  @Override
  public boolean canProxyTrailers() {
    return proxyTrailersCheckBox.isSelected();
  }

  @Override
  public boolean canProxyUpdates() {
    return proxyUpdatesCheckBox.isSelected();
  }

  @Override
  public boolean canProxySubtitles() {
    return proxySubtitlesCheckBox.isSelected();
  }

  @Override
  public boolean canAutoOpenPlaylistItem() {
    return playlistAutoOpenCheckBoxMenuItem.isSelected();
  }

  @Override
  public int getTrailerPlayer() {
    return UI.selectedIndex(trailerPlayerButtonGroup);
  }

  @Override
  public String getFormat() {
    for (AbstractButton button : new AbstractButton[]{downloadHighQualityRadioButtonMenuItem, downloadDVDQualityRadioButtonMenuItem,
      download720HDQualityRadioButtonMenuItem, download1080HDRadioButtonMenuItem}) {
      if (button.isSelected()) {
        return button.getText();
      }
    }
    return Constant.ANY;
  }

  @Override
  public String getMinDownloadSize() {
    return (String) minDownloadSizeComboBox.getSelectedItem();
  }

  @Override
  public String getMaxDownloadSize() {
    return (String) maxDownloadSizeComboBox.getSelectedItem();
  }

  @Override
  public boolean canDownloadWithPlaylist() {
    return playlistDownloaderRadioButtonMenuItem.isSelected();
  }

  @Override
  public String[] getWhitelistedFileExts() {
    return UI.copy(whitelistListModel.toArray());
  }

  @Override
  public String[] getBlacklistedFileExts() {
    return UI.copy(blacklistListModel.toArray());
  }

  @Override
  public boolean canShowSafetyWarning() {
    return safetyCheckBoxMenuItem.isSelected();
  }

  @Override
  public boolean canDownloadWithDefaultApp() {
    return defaultApplicationDownloaderRadioButtonMenuItem.isSelected();
  }

  @Override
  public boolean canEmailWithDefaultApp() {
    return emailWithDefaultAppCheckBoxMenuItem.isSelected();
  }

  @Override
  public boolean canPlayWithDefaultApp() {
    return playlistPlayWithDefaultAppCheckBoxMenuItem.isSelected();
  }

  @Override
  public boolean canIgnoreDownloadSize() {
    return downloadSizeIgnoreCheckBox.isSelected();
  }

  @Override
  public Object[] makeRow(String titleID, String imagePath, String title, String currTitle, String oldTitle, String year, String rating, String summary,
          String imageLink, boolean isTVShow, boolean isTVShowAndMovie, String season, String episode) {
    String[] row = new String[13];
    row[idCol] = titleID;
    row[imageCol] = imagePath;
    row[titleCol] = title;
    row[currTitleCol] = currTitle;
    row[oldTitleCol] = oldTitle;
    row[yearCol] = year;
    row[ratingCol] = rating;
    row[summaryCol] = summary;
    row[imageLinkCol] = imageLink;
    row[isTVShowCol] = (isTVShow ? "1" : "");
    row[isTVShowAndMovieCol] = (isTVShowAndMovie ? "1" : "");
    row[seasonCol] = season;
    row[episodeCol] = episode;
    return row;
  }

  @Override
  public Object[] makePlaylistRow(String name, FormattedNum size, FormattedNum progress, PlaylistItem playlistItem) {
    Object[] row = new Object[4];
    row[playlistNameCol] = name;
    row[playlistSizeCol] = size;
    row[playlistProgressCol] = progress;
    row[playlistItemCol] = playlistItem;
    return row;
  }

  @Override
  public void updateStarted() {
    updateMenuItem.setEnabled(false);
    updateMenuItem.setText(Str.str("GUI.updateMenuItem.text2"));
  }

  @Override
  public void updateStopped() {
    updateMenuItem.setText(Str.str("GUI.updateMenuItem.text"));
    updateMenuItem.setEnabled(true);
  }

  @Override
  public void updateMsg(final String msg) {
    synchronized (optionDialogLock) {
      UI.run(true, new Runnable() {
        @Override
        public void run() {
          Container container = UI.container(msg, null, null, textComponentPopupListener);
          UI.addHyperlinkListener((JEditorPane) ((JScrollPane) container.getComponent(0)).getViewport().getView(), new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent evt) {
              try {
                UI.hyperlinkHandler(evt);
              } catch (Exception e) {
                if (Debug.DEBUG) {
                  Debug.print(e);
                }
                showMsg(ThrowableUtil.toString(e), Constant.ERROR_MSG);
                IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
              }
            }
          });
          showOptionDialog(container, Constant.APP_TITLE, Constant.INFO_MSG, null);
        }
      });
    }
  }

  @Override
  public boolean canUpdate() {
    return updateCheckBoxMenuItem.isSelected();
  }

  @Override
  public void subtitleSearchStarted() {
    if (isTVShowSubtitle) {
      tvSubtitleLoadingLabel.setIcon(loadingIcon);

      Component secondaryComponent;
      AbstractButton primaryButton;
      if (isSubtitleMatch1) {
        secondaryComponent = tvSubtitleDownloadMatch2Button;
        primaryButton = tvSubtitleDownloadMatch1Button;
      } else {
        secondaryComponent = tvSubtitleDownloadMatch1Button;
        primaryButton = tvSubtitleDownloadMatch2Button;
      }
      UI.enable(new AbstractButton[]{primaryButton}, false, new Component[]{secondaryComponent}, false);
    } else {
      movieSubtitleLoadingLabel.setIcon(loadingIcon);

      Component secondaryComponent;
      AbstractButton primaryButton;
      if (isSubtitleMatch1) {
        secondaryComponent = movieSubtitleDownloadMatch2Button;
        primaryButton = movieSubtitleDownloadMatch1Button;
      } else {
        secondaryComponent = movieSubtitleDownloadMatch1Button;
        primaryButton = movieSubtitleDownloadMatch2Button;
      }
      UI.enable(new AbstractButton[]{primaryButton}, false, new Component[]{secondaryComponent}, false);
    }
  }

  @Override
  public void subtitleSearchStopped() {
    if (isTVShowSubtitle) {
      tvSubtitleDialog.setVisible(false);
      UI.enable(new AbstractButton[]{isSubtitleMatch1 ? tvSubtitleDownloadMatch1Button : tvSubtitleDownloadMatch2Button}, true, new Component[]{
        tvSubtitleDownloadMatch2Button, tvSubtitleDownloadMatch1Button}, true);
      tvSubtitleLoadingLabel.setIcon(notLoadingIcon);
    } else {
      movieSubtitleDialog.setVisible(false);
      UI.enable(new AbstractButton[]{isSubtitleMatch1 ? movieSubtitleDownloadMatch1Button : movieSubtitleDownloadMatch2Button}, true, new Component[]{
        movieSubtitleDownloadMatch2Button, movieSubtitleDownloadMatch1Button}, true);
      movieSubtitleLoadingLabel.setIcon(notLoadingIcon);
    }
  }

  @Override
  public void summarySearchStarted(Video video) {
    workerListener.summarySearchStarted(0, video, false, null);
  }

  private static int portNum(String port) {
    int portNum;
    return Regex.isMatch(port, "\\d{1,5}+") && (portNum = Integer.parseInt(port)) <= 65535 ? portNum : -1;
  }

  @Override
  public boolean canRandomizePort() {
    return portRandomizeCheckBox.isSelected();
  }

  @Override
  public int setRandomPort() {
    int portNum = (new Random()).nextInt(16373) + 49161;
    portTextField.setText(String.valueOf(portNum));
    return portNum;
  }

  @Override
  public void setPort(int port) {
    portTextField.setText(String.valueOf(port));
  }

  @Override
  public int getPort() {
    String port = portTextField.getText().trim();
    int portNum;
    return port.isEmpty() || (portNum = portNum(port)) == -1 ? setRandomPort() : portNum;
  }

  @Override
  public String wideSpace() {
    return UI.displayableStr(new JProgressBar(), "\u2004", " ");
  }

  @Override
  public String invisibleSeparator() {
    return UI.displayableStr(playlistSyncTable.table, "\u200b\u200b\u200b", "\u0009\u0009\u0009");
  }

  void changeLocale(String locale) {
    try {
      changeLocale(locale == null || locale.equals(Constant.NULL) ? null : new Locale(locale.substring(0, 2), locale.substring(3)));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  private void changeLocale(Locale locale) {
    NumberFormat prevNumFormat = Str.getNumFormat();
    workerListener.changeLocale(locale);

    languageButtonGroup.clearSelection();
    for (AbstractButton languageButton : languageButtons()) {
      if (Str.locale().toString().equals(languageButton.getName())) {
        languageButtonGroup.setSelected(languageButton.getModel(), true);
        break;
      }
    }

    UI.update(typeComboBox, false, Str.strs("GUI.typeComboBox.model"));

    NumberFormat ratingFormat = Str.getNumFormat(Constant.RATING_FORMAT);
    String[] modelRatings = ((Renderer) ratingComboBox.getRenderer()).model(), viewRatings = new String[modelRatings.length];
    viewRatings[0] = Str.str("any");
    for (int i = 1; i < modelRatings.length; i++) {
      viewRatings[i] = ratingFormat.format(Double.parseDouble(modelRatings[i]));
    }
    UI.update(ratingComboBox, false, viewRatings);

    UI.update(genreList, true, Str.strs("GUI.genreList.model"));
    UI.update(tvSeasonComboBox, false, UI.items(1, 100, 1, true, Str.str("any"), null));
    UI.update(tvEpisodeComboBox, false, UI.items(0, 300, 1, true, Str.str("any"), null));
    updateTVComboBoxes();
    UI.update(languageList, true, Str.strs("GUI.languageList.model"));
    UI.update(countryList, true, Str.strs("GUI.countryList.model"));
    String[] subtitleLanguages = Str.strs("GUI.subtitleLanguageComboBox.model");
    UI.update(movieSubtitleLanguageComboBox, true, subtitleLanguages);
    UI.update(tvSubtitleLanguageComboBox, true, subtitleLanguages);
    String[] subtitleFormats = {Str.str("any"), Constant.HQ, Constant.DVD, Constant.HD720, Constant.HD1080};
    UI.update(movieSubtitleFormatComboBox, false, subtitleFormats);
    UI.update(tvSubtitleFormatComboBox, false, subtitleFormats);
    UI.update(maxDownloadSizeComboBox, false, UI.items(1, 100, 1, false, null, Str.str("infinity")));

    Object proxy = proxyComboBox.getSelectedItem();
    ((Renderer) proxyComboBox.getRenderer()).setView(false, Str.str("noProxy"));
    proxyComboBox.setModel(proxyComboBox.getModel());
    proxyComboBox.setSelectedItem(proxy);

    aboutDialog.setTitle(Str.str("GUI.aboutDialog.title"));
    aboutMenuItem.setText(Str.str("GUI.aboutMenuItem.text"));
    addProxiesAddButton.setText(Str.str("GUI.addProxiesAddButton.text"));
    addProxiesCancelButton.setText(Str.str("GUI.addProxiesCancelButton.text"));
    addProxiesDialog.setTitle(Str.str("GUI.addProxiesDialog.title"));
    addProxiesLabel.setText(Str.str("GUI.addProxiesLabel.text"));
    authenticationMessageLabel.setText(Str.str("GUI.authenticationMessageLabel.text"));
    authenticationPasswordLabel.setText(Str.str("GUI.authenticationPasswordLabel.text"));
    authenticationUsernameLabel.setText(Str.str("GUI.authenticationUsernameLabel.text"));
    banTitleMenu.setText(Str.str("GUI.banTitleMenu.text"));
    blacklistedLabel.setText(Str.str("GUI.blacklistedLabel.text"));
    blacklistedToWhitelistedButton.setToolTipText(Str.str("GUI.blacklistedToWhitelistedButton.toolTipText"));
    browserNotificationCheckBoxMenuItem.setText(Str.str("GUI.browserNotificationCheckBoxMenuItem.text"));
    commentsDialog.setTitle(Str.str("GUI.commentsDialog.title"));
    copyDownloadLink1MenuItem.setText(Str.str("GUI.copyDownloadLink1MenuItem.text"));
    copyDownloadLink2MenuItem.setText(Str.str("GUI.copyDownloadLink2MenuItem.text"));
    copyFullTitleAndYearMenuItem.setText(Str.str("GUI.copyFullTitleAndYearMenuItem.text"));
    copyFullTitleAndYearMenuItem.setToolTipText(Str.str("GUI.copyFullTitleAndYearMenuItem.toolTipText"));
    copyMenu.setText(Str.str("GUI.copyMenu.text"));
    copyMenuItem.setText(Str.str("GUI.copyMenuItem.text"));
    copyPosterImageMenuItem.setText(Str.str("GUI.copyPosterImageMenuItem.text"));
    copySelectionMenuItem.setText(Str.str("GUI.copySelectionMenuItem.text"));
    copySubtitleLinkMenuItem.setText(Str.str("GUI.copySubtitleLinkMenuItem.text"));
    copySummaryLinkMenuItem.setText(Str.str("GUI.copySummaryLinkMenuItem.text"));
    copyTrailerLinkMenuItem.setText(Str.str("GUI.copyTrailerLinkMenuItem.text"));
    countryLabel.setText(Str.str("GUI.countryLabel.text"));
    cutMenuItem.setText(Str.str("GUI.cutMenuItem.text"));
    defaultApplicationDownloaderRadioButtonMenuItem.setText(Str.str("GUI.defaultApplicationDownloaderRadioButtonMenuItem.text"));
    deleteMenuItem.setText(Str.str("GUI.deleteMenuItem.text"));
    downloadAnyQualityRadioButtonMenuItem.setText(Str.str("any"));
    downloadHighQualityRadioButtonMenuItem.setToolTipText(Str.str("GUI.downloadHighQualityRadioButtonMenuItem.toolTipText"));
    downloadLink1Button.setToolTipText(Str.str("GUI.downloadLink1Button.toolTipText"));
    downloadLink1MenuItem.setText(Str.str("GUI.downloadLink1MenuItem.text"));
    downloadLink2Button.setToolTipText(Str.str("GUI.downloadLink2Button.toolTipText"));
    downloadLink2MenuItem.setText(Str.str("GUI.downloadLink2MenuItem.text"));
    downloadMenu.setText(Str.str("GUI.downloadMenu.text"));
    downloadQualityMenu.setText(Str.str("GUI.downloadQualityMenu.text"));
    downloadSizeButton.setText(Str.str("GUI.downloadSizeButton.text"));
    downloadSizeDialog.setTitle(Str.str("GUI.downloadSizeDialog.title"));
    downloadSizeIgnoreCheckBox.setText(Str.str("GUI.downloadSizeIgnoreCheckBox.text"));
    downloadSizeLabel.setText(Str.str("GUI.downloadSizeLabel.text"));
    downloadSizeLabel.setToolTipText(Str.str("GUI.downloadSizeLabel.toolTipText"));
    downloadSizeMenuItem.setText(Str.str("GUI.downloadSizeMenuItem.text"));
    downloadSizeToLabel.setText(Str.str("GUI.downloadSizeToLabel.text"));
    downloaderMenu.setText(Str.str("GUI.downloaderMenu.text"));
    editMenu.setText(Str.str("GUI.editMenu.text"));
    editProfilesMenuItem.setText(Str.str("GUI.editProfilesMenuItem.text"));
    emailDownloadLink1MenuItem.setText(Str.str("GUI.emailDownloadLink1MenuItem.text"));
    emailDownloadLink2MenuItem.setText(Str.str("GUI.emailDownloadLink2MenuItem.text"));
    emailEverythingMenuItem.setText(Str.str("GUI.emailEverythingMenuItem.text"));
    emailMenu.setText(Str.str("GUI.emailMenu.text"));
    emailSummaryLinkMenuItem.setText(Str.str("GUI.emailSummaryLinkMenuItem.text"));
    emailTrailerLinkMenuItem.setText(Str.str("GUI.emailTrailerLinkMenuItem.text"));
    emailWithDefaultAppCheckBoxMenuItem.setText(Str.str("GUI.emailWithDefaultAppCheckBoxMenuItem.text"));
    episodeLabel.setText(Str.str("GUI.episodeLabel.text"));
    episodeLabel.setToolTipText(Str.str("GUI.episodeLabel.toolTipText"));
    exitMenuItem.setText(Str.str("GUI.exitMenuItem.text"));
    extensionsButton.setText(Str.str("GUI.extensionsButton.text"));
    extensionsDialog.setTitle(Str.str("GUI.extensionsDialog.title"));
    faqFrame.setTitle(Str.str("GUI.faqFrame.title"));
    faqMenuItem.setText(Str.str("GUI.faqMenuItem.text"));
    feedCheckBoxMenuItem.setText(Str.str("GUI.feedCheckBoxMenuItem.text"));
    feedCheckBoxMenuItem.setToolTipText(Str.str("GUI.feedCheckBoxMenuItem.toolTipText"));
    fileExtensionsLabel.setText(Str.str("GUI.fileExtensionsLabel.text"));
    fileExtensionsMenuItem.setText(Str.str("GUI.fileExtensionsMenuItem.text"));
    fileMenu.setText(Str.str("GUI.fileMenu.text"));
    findMenuItem.setText(Str.str("GUI.findMenuItem.text"));
    findSubtitleMenuItem.setText(Str.str("GUI.findSubtitleMenuItem.text"));
    genreLabel.setText(Str.str("GUI.genreLabel.text"));
    genreLabel.setToolTipText(Str.str("GUI.genreLabel.toolTipText"));
    helpMenu.setText(Str.str("GUI.helpMenu.text"));
    hideMenuItem.setText(Str.str("GUI.hideMenuItem.text"));
    iconifyMenuItem.setText(Str.str("GUI.iconifyMenuItem.text"));
    languageCountryDialog.setTitle(Str.str("GUI.languageCountryDialog.title"));
    languageCountryMenuItem.setText(Str.str("GUI.languageCountryMenuItem.text"));
    languageCountryOkButton.setText(Str.str("GUI.languageCountryOkButton.text"));
    languageCountryWarningTextArea.setText(Str.str("GUI.languageCountryWarningTextArea.text"));
    languageLabel.setText(Str.str("GUI.languageLabel.text"));
    languageMenu.setText(Str.str("GUI.languageMenu.text"));
    listCopyMenuItem.setText(Str.str("GUI.listCopyMenuItem.text"));
    listCutMenuItem.setText(Str.str("GUI.listCutMenuItem.text"));
    listDeleteMenuItem.setText(Str.str("GUI.listDeleteMenuItem.text"));
    listSelectAllMenuItem.setText(Str.str("GUI.listSelectAllMenuItem.text"));
    loadMoreResultsButton.setText(Str.str("GUI.loadMoreResultsButton.text"));
    loadMoreResultsButton.setToolTipText(Str.str("GUI.loadMoreResultsButton.toolTipText"));
    movieSubtitleDialog.setTitle(Str.str("GUI.movieSubtitleDialog.title"));
    movieSubtitleDownloadMatch1Button.setToolTipText(Str.str("GUI.movieSubtitleDownloadMatch1Button.toolTipText"));
    movieSubtitleDownloadMatch2Button.setToolTipText(Str.str("GUI.movieSubtitleDownloadMatch2Button.toolTipText"));
    movieSubtitleFormatLabel.setText(Str.str("GUI.movieSubtitleFormatLabel.text"));
    movieSubtitleLanguageLabel.setText(Str.str("GUI.movieSubtitleLanguageLabel.text"));
    msgOKButton.setText(Str.str("GUI.msgOKButton.text"));
    noDownloaderRadioButtonMenuItem.setText(Str.str("GUI.noDownloaderRadioButtonMenuItem.text"));
    pasteMenuItem.setText(Str.str("GUI.pasteMenuItem.text"));
    peerBlockNotificationCheckBoxMenuItem.setText(Str.str("GUI.peerBlockNotificationCheckBoxMenuItem.text"));
    playlistAutoOpenCheckBoxMenuItem.setText(Str.str("GUI.playlistAutoOpenCheckBoxMenuItem.text"));
    playlistBanGroupButton.setToolTipText(Str.str("GUI.playlistBanGroupButton.toolTipText"));
    playlistCopyDownloadLinkMenuItem.setText(Str.str("GUI.copyDownloadLink1MenuItem.text"));
    playlistCopyMenu.setText(Str.str("GUI.copyMenu.text"));
    playlistCopySelectionMenuItem.setText(Str.str("GUI.copySelectionMenuItem.text"));
    playlistDownloaderRadioButtonMenuItem.setText(Str.str("GUI.playlistDownloaderRadioButtonMenuItem.text"));
    playlistMenu.setText(Str.str("GUI.playlistMenu.text"));
    playlistMenuItem.setText(Str.str("GUI.playlistMenuItem.text"));
    playlistMoveDownButton.setToolTipText(Str.str("GUI.playlistMoveDownButton.toolTipText"));
    playlistMoveDownMenuItem.setText(Str.str("GUI.playlistMoveDownMenuItem.text"));
    playlistMoveUpButton.setToolTipText(Str.str("GUI.playlistMoveUpButton.toolTipText"));
    playlistMoveUpMenuItem.setText(Str.str("GUI.playlistMoveUpMenuItem.text"));
    playlistOpenButton.setToolTipText(Str.str("GUI.playlistOpenButton.toolTipText"));
    playlistOpenMenuItem.setText(Str.str("GUI.playlistOpenMenuItem.text"));
    setPlaylistPlayHint((Long) playlistPlayButton.getClientProperty("numBlockedIps"));
    playlistPlayWithDefaultAppCheckBoxMenuItem.setText(Str.str("GUI.playlistPlayWithDefaultAppCheckBoxMenuItem.text"));
    playlistReloadGroupButton.setToolTipText(Str.str("GUI.playlistReloadGroupButton.toolTipText"));
    playlistReloadGroupMenuItem.setText(Str.str("GUI.playlistReloadGroupMenuItem.text"));
    playlistRemoveButton.setToolTipText(Str.str("GUI.playlistRemoveButton.toolTipText"));
    playlistRemoveMenuItem.setText(Str.str("GUI.playlistRemoveMenuItem.text"));
    playlistSaveFolderMenuItem.setText(Str.str("GUI.playlistSaveFolderMenuItem.text"));
    playlistShowNonVideoItemsCheckBoxMenuItem.setText(Str.str("GUI.playlistShowNonVideoItemsCheckBoxMenuItem.text"));
    playlistShowNonVideoItemsCheckBoxMenuItem.setToolTipText(Str.str("GUI.playlistShowNonVideoItemsCheckBoxMenuItem.toolTipText"));
    popularMoviesMenuItem.setText(Str.str("GUI.popularMoviesMenuItem.text"));
    popularMoviesMenuItem.setToolTipText(Str.str("GUI.popularMoviesMenuItem.toolTipText"));
    popularMoviesResultsPerSearchLabel.setText(Str.str("GUI.popularMoviesResultsPerSearchLabel.text"));
    popularMoviesResultsPerSearchLabel.setToolTipText(Str.str("GUI.popularMoviesResultsPerSearchLabel.toolTipText"));
    popularNewHQMoviesMenuItem.setText(Str.str("GUI.popularNewHQMoviesMenuItem.text"));
    popularNewHQMoviesMenuItem.setToolTipText(Str.str("GUI.popularNewHQMoviesMenuItem.toolTipText"));
    popularNewHQTVShowsMenuItem.setText(Str.str("GUI.popularNewHQTVShowsMenuItem.text"));
    popularNewHQTVShowsMenuItem.setToolTipText(Str.str("GUI.popularNewHQTVShowsMenuItem.toolTipText"));
    popularTVShowsMenuItem.setText(Str.str("GUI.popularTVShowsMenuItem.text"));
    popularTVShowsMenuItem.setToolTipText(Str.str("GUI.popularTVShowsMenuItem.toolTipText"));
    popularTVShowsResultsPerSearchLabel.setText(Str.str("GUI.popularTVShowsResultsPerSearchLabel.text"));
    popularTVShowsResultsPerSearchLabel.setToolTipText(Str.str("GUI.popularTVShowsResultsPerSearchLabel.toolTipText"));
    portDialog.setTitle(Str.str("GUI.portDialog.title"));
    portLabel.setText(Str.str("GUI.portLabel.text"));
    portLabel.setToolTipText(Str.str("GUI.portLabel.toolTipText"));
    portMenuItem.setText(Str.str("GUI.portMenuItem.text"));
    portOkButton.setText(Str.str("GUI.portOkButton.text"));
    portRandomizeCheckBox.setText(Str.str("GUI.portRandomizeCheckBox.text"));
    portRandomizeCheckBox.setToolTipText(Str.str("GUI.portRandomizeCheckBox.toolTipText"));
    portTextField.setToolTipText(Str.str("GUI.portTextField.toolTipText"));
    printMenuItem.setText(Str.str("GUI.printMenuItem.text"));
    profileClearButton.setText(Str.str("GUI.profileClearButton.text"));
    profileDialog.setTitle(Str.str("GUI.profileDialog.title"));
    profileMenu.setText(Str.str("GUI.profileMenu.text"));
    profileNameChangeCancelButton.setText(Str.str("GUI.profileNameChangeCancelButton.text"));
    profileNameChangeDialog.setTitle(Str.str("GUI.profileNameChangeDialog.title"));
    profileNameChangeLabel.setText(Str.str("GUI.profileNameChangeLabel.text"));
    profileNameChangeOKButton.setText(Str.str("GUI.profileNameChangeOKButton.text"));
    profileOKButton.setText(Str.str("GUI.profileOKButton.text"));
    profileRenameButton.setText(Str.str("GUI.profileRenameButton.text"));
    profileSetButton.setText(Str.str("GUI.profileSetButton.text"));
    profileUseButton.setText(Str.str("GUI.profileUseButton.text"));
    proxyAddButton.setText(Str.str("GUI.proxyAddButton.text"));
    proxyDialog.setTitle(Str.str("GUI.proxyDialog.title"));
    proxyDownloadButton.setText(Str.str("GUI.proxyDownloadButton.text"));
    proxyDownloadLinkInfoCheckBox.setText(Str.str("GUI.proxyDownloadLinkInfoCheckBox.text"));
    proxyExportButton.setText(Str.str("GUI.proxyExportButton.text"));
    proxyImportButton.setText(Str.str("GUI.proxyImportButton.text"));
    proxyMenuItem.setText(Str.str("GUI.proxyMenuItem.text"));
    proxyOKButton.setText(Str.str("GUI.proxyOKButton.text"));
    proxyRemoveButton.setText(Str.str("GUI.proxyRemoveButton.text"));
    proxySearchEnginesCheckBox.setText(Str.str("GUI.proxySearchEnginesCheckBox.text"));
    proxySubtitlesCheckBox.setText(Str.str("GUI.proxySubtitlesCheckBox.text"));
    proxyTrailersCheckBox.setText(Str.str("GUI.proxyTrailersCheckBox.text"));
    proxyUpdatesCheckBox.setText(Str.str("GUI.proxyUpdatesCheckBox.text"));
    proxyUseForLabel.setText(Str.str("GUI.proxyUseForLabel.text"));
    proxyVideoInfoCheckBox.setText(Str.str("GUI.proxyVideoInfoCheckBox.text"));
    ratingLabel.setText(Str.str("GUI.ratingLabel.text"));
    ratingLabel.setToolTipText(Str.str("GUI.ratingLabel.toolTipText"));
    readSummaryButton.setToolTipText(Str.str("GUI.readSummaryButton.toolTipText"));
    readSummaryMenuItem.setText(Str.str("GUI.readSummaryMenuItem.text"));
    hearSummaryMenuItem.setText(Str.str("GUI.hearSummaryMenuItem.text"));
    regularResultsPerSearchLabel.setText(Str.str("GUI.regularResultsPerSearchLabel.text"));
    regularResultsPerSearchLabel.setToolTipText(Str.str("GUI.regularResultsPerSearchLabel.toolTipText"));
    releasedLabel.setText(Str.str("GUI.releasedLabel.text"));
    releasedLabel.setToolTipText(Str.str("GUI.releasedLabel.toolTipText"));
    releasedToLabel.setText(Str.str("GUI.releasedToLabel.text"));
    removeProxiesCancelButton.setText(Str.str("GUI.removeProxiesCancelButton.text"));
    removeProxiesDialog.setTitle(Str.str("GUI.removeProxiesDialog.title"));
    removeProxiesLabel.setText(Str.str("GUI.removeProxiesLabel.text"));
    removeProxiesRemoveButton.setText(Str.str("GUI.removeProxiesRemoveButton.text"));
    resetWindowMenuItem.setText(Str.str("GUI.resetWindowMenuItem.text"));
    resultsPerSearchButton.setText(Str.str("GUI.resultsPerSearchButton.text"));
    resultsPerSearchDialog.setTitle(Str.str("GUI.resultsPerSearchDialog.title"));
    resultsPerSearchMenuItem.setText(Str.str("GUI.resultsPerSearchMenuItem.text"));
    safetyCheckBoxMenuItem.setText(Str.str("GUI.safetyCheckBoxMenuItem.text"));
    safetyCheckBoxMenuItem.setToolTipText(Str.str("GUI.safetyCheckBoxMenuItem.toolTipText"));
    searchBanTitleMenu.setText(Str.str("GUI.searchBanTitleMenu.text"));
    searchBanTitleEnableCheckBoxMenuItem.setText(Str.str("GUI.searchBanTitleEnableCheckBoxMenuItem.text"));
    searchButton.setToolTipText(Str.str("GUI.searchButton.toolTipText"));
    searchMenu.setText(Str.str("GUI.searchMenu.text"));
    seasonLabel.setText(Str.str("GUI.seasonLabel.text"));
    seasonLabel.setToolTipText(Str.str("GUI.seasonLabel.toolTipText"));
    selectAllMenuItem.setText(Str.str("GUI.selectAllMenuItem.text"));
    textComponentCopyMenuItem.setText(Str.str("GUI.textComponentCopyMenuItem.text"));
    textComponentCutMenuItem.setText(Str.str("GUI.textComponentCutMenuItem.text"));
    textComponentDeleteMenuItem.setText(Str.str("GUI.textComponentDeleteMenuItem.text"));
    textComponentPasteMenuItem.setText(Str.str("GUI.textComponentPasteMenuItem.text"));
    textComponentPasteSearchMenuItem.setText(Str.str("GUI.textComponentPasteSearchMenuItem.text"));
    textComponentSelectAllMenuItem.setText(Str.str("GUI.textComponentSelectAllMenuItem.text"));
    timeoutButton.setText(Str.str("GUI.timeoutButton.text"));
    timeoutDialog.setTitle(Str.str("GUI.timeoutDialog.title"));
    timeoutLabel.setText(Str.str("GUI.timeoutLabel.text"));
    timeoutLabel.setToolTipText(Str.str("GUI.timeoutLabel.toolTipText"));
    timeoutMenuItem.setText(Str.str("GUI.timeoutMenuItem.text"));
    titleLabel.setText(Str.str("GUI.titleLabel.text"));
    titleLabel.setToolTipText(Str.str("GUI.titleLabel.toolTipText"));
    trailerMediaPlayer1080RadioButtonMenuItem.setText(Str.str("GUI.trailerMediaPlayer1080RadioButtonMenuItem.text"));
    trailerMediaPlayer240RadioButtonMenuItem.setText(Str.str("GUI.trailerMediaPlayer240RadioButtonMenuItem.text"));
    trailerMediaPlayer360RadioButtonMenuItem.setText(Str.str("GUI.trailerMediaPlayer360RadioButtonMenuItem.text"));
    trailerMediaPlayer480RadioButtonMenuItem.setText(Str.str("GUI.trailerMediaPlayer480RadioButtonMenuItem.text"));
    trailerMediaPlayer720RadioButtonMenuItem.setText(Str.str("GUI.trailerMediaPlayer720RadioButtonMenuItem.text"));
    trailerMediaPlayerRadioButtonMenuItem.setText(Str.str("GUI.trailerMediaPlayerRadioButtonMenuItem.text"));
    trailerPlayerMenu.setText(Str.str("GUI.trailerPlayerMenu.text"));
    trailerWebBrowserPlayerRadioButtonMenuItem.setText(Str.str("GUI.trailerWebBrowserPlayerRadioButtonMenuItem.text"));
    tvCancelButton.setText(Str.str("GUI.tvCancelButton.text"));
    tvCancelButton.setToolTipText(Str.str("GUI.tvCancelButton.toolTipText"));
    tvDialog.setTitle(Str.str("GUI.tvDialog.title"));
    tvSelectionLabel.setText(Str.str("GUI.tvSelectionLabel.text"));
    tvSubmitButton.setText(Str.str("GUI.tvSubmitButton.text"));
    tvSubmitButton.setToolTipText(Str.str("GUI.tvSubmitButton.toolTipText"));
    tvSubtitleDialog.setTitle(Str.str("GUI.tvSubtitleDialog.title"));
    tvSubtitleDownloadMatch1Button.setToolTipText(Str.str("GUI.tvSubtitleDownloadMatch1Button.toolTipText"));
    tvSubtitleDownloadMatch2Button.setToolTipText(Str.str("GUI.tvSubtitleDownloadMatch2Button.toolTipText"));
    tvSubtitleEpisodeLabel.setText(Str.str("GUI.tvSubtitleEpisodeLabel.text"));
    tvSubtitleEpisodeLabel.setToolTipText(Str.str("GUI.tvSubtitleEpisodeLabel.toolTipText"));
    tvSubtitleFormatLabel.setText(Str.str("GUI.tvSubtitleFormatLabel.text"));
    tvSubtitleLanguageLabel.setText(Str.str("GUI.tvSubtitleLanguageLabel.text"));
    tvSubtitleSeasonLabel.setText(Str.str("GUI.tvSubtitleSeasonLabel.text"));
    tvSubtitleSeasonLabel.setToolTipText(Str.str("GUI.tvSubtitleSeasonLabel.toolTipText"));
    typeLabel.setText(Str.str("GUI.typeLabel.text"));
    typeLabel.setToolTipText(Str.str("GUI.typeLabel.toolTipText"));
    updateCheckBoxMenuItem.setText(Str.str("GUI.updateCheckBoxMenuItem.text"));
    updateMenuItem.setText(Str.str("GUI.updateMenuItem.text"));
    viewMenu.setText(Str.str("GUI.viewMenu.text"));
    watchOnDeviceMenuItem.setText(Str.str("GUI.watchOnDeviceMenuItem.text"));
    watchOnDeviceMenuItem.setToolTipText(Str.str("GUI.watchOnDeviceMenuItem.toolTipText"));
    watchTrailerButton.setToolTipText(Str.str("GUI.watchTrailerButton.toolTipText"));
    watchTrailerMenuItem.setText(Str.str("GUI.watchTrailerMenuItem.text"));
    whitelistLabel.setText(Str.str("GUI.whitelistLabel.text"));
    whitelistedToBlacklistedButton.setToolTipText(Str.str("GUI.whitelistedToBlacklistedButton.toolTipText"));

    updateToggleButtons(false);

    proxyDownloadLinkInfoCheckBox.setToolTipText(Str.str("forExample", Str.get(728)));
    proxyVideoInfoCheckBox.setToolTipText(Str.str("forExample", Str.get(578)));
    proxySearchEnginesCheckBox.setToolTipText(Str.str("forExample", Str.get(579)));
    proxyTrailersCheckBox.setToolTipText(Str.str("forExample", Str.get(580)));
    proxyUpdatesCheckBox.setToolTipText(Str.str("forExample", Str.get(582)));
    proxySubtitlesCheckBox.setToolTipText(Str.str("forExample", Str.get(583)));
    if (exitBackupModeButton.isEnabled()) {
      exitBackupModeButton.setText(Str.str("GUI.exitBackupModeButton.text2"));
    }

    startDateChooser.setLocale(Str.locale());
    endDateChooser.setLocale(Str.locale());

    TableColumnModel colModel = resultsTable.getColumnModel();
    colModel.getColumn(resultsTable.convertColumnIndexToView(titleCol)).setHeaderValue(Str.str("GUI.resultsTable.columnModel.title1"));
    colModel.getColumn(resultsTable.convertColumnIndexToView(yearCol)).setHeaderValue(Str.str("GUI.resultsTable.columnModel.title2"));
    colModel.getColumn(resultsTable.convertColumnIndexToView(ratingCol)).setHeaderValue(Str.str("GUI.resultsTable.columnModel.title3"));
    resultsTable.getTableHeader().repaint();
    colModel = playlistTable.getColumnModel();
    colModel.getColumn(playlistTable.convertColumnIndexToView(playlistNameCol)).setHeaderValue(Str.str("GUI.playlistTable.columnModel.title0"));
    colModel.getColumn(playlistTable.convertColumnIndexToView(playlistSizeCol)).setHeaderValue(Str.str("GUI.playlistTable.columnModel.title1"));
    colModel.getColumn(playlistTable.convertColumnIndexToView(playlistProgressCol)).setHeaderValue(Str.str("GUI.playlistTable.columnModel.title2"));
    playlistTable.getTableHeader().repaint();

    synchronized (resultsSyncTable.lock) {
      @SuppressWarnings("unchecked")
      Collection<? extends List> rows = resultsSyncTable.tableModel.getDataVector();
      if (!rows.isEmpty()) {
        try {
          for (List<Object> row : rows) {
            String rating = UI.innerHTML((String) row.get(ratingCol));
            row.set(ratingCol, rating.equals(Constant.NO_RATING) ? rating : ratingFormat.format(prevNumFormat.parse(rating).doubleValue()));
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
        resultsSyncTable.tableModel.fireTableDataChanged();
      }
    }

    synchronized (playlistSyncTable.lock) {
      @SuppressWarnings("unchecked")
      Collection<? extends List> rows = playlistSyncTable.tableModel.getDataVector();
      if (!rows.isEmpty()) {
        try {
          for (List<Object> row : rows) {
            row.set(playlistSizeCol, workerListener.playlistItemSize(((FormattedNum) row.get(playlistSizeCol)).val().longValue()));
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
        playlistSyncTable.tableModel.fireTableDataChanged();
      }
    }

    searchProgressUpdate((Integer) searchProgressTextField.getClientProperty("numResults"), (Double) searchProgressTextField.getClientProperty("progress"));

    try {
      if (trayIcon != null) {
        UI.updateTrayIconLabels(trayIcon, this);
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }

    String[] names = new String[10];
    for (int i = 0; i < names.length; i++) {
      String profile = "GUI.profile" + i + "MenuItem.text";
      names[i] = (i == 0 ? Str.str(profile) : preferences.get(profile, Str.str(profile)));
    }

    int profileIndex = profileComboBox.getSelectedIndex();
    profileComboBox.removeAllItems();
    for (int i = 0; i < names.length; i++) {
      profileComboBox.addItem(names[i]);
      updateProfileGUIitems(i);
    }
    profileComboBox.setSelectedIndex(profileIndex == -1 ? 0 : profileIndex);

    initFileNameExtensionFilters();

    aboutEditorPane.setText(UI.about());

    UIManager.put("OptionPane.yesButtonText", Str.str("GUI.optionPane.yesButton.text"));
    UIManager.put("OptionPane.noButtonText", Str.str("GUI.optionPane.noButton.text"));
    UIManager.put("OptionPane.okButtonText", Str.str("GUI.optionPane.okButton.text"));
    UIManager.put("OptionPane.cancelButtonText", Str.str("GUI.optionPane.cancelButton.text"));

    resizeContent();
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  JDialog aboutDialog;
  JEditorPane aboutEditorPane;
  JMenuItem aboutMenuItem;
  JScrollPane aboutScrollPane;
  JButton addProxiesAddButton;
  JButton addProxiesCancelButton;
  JDialog addProxiesDialog;
  JLabel addProxiesLabel;
  JScrollPane addProxiesScrollPane;
  JTextArea addProxiesTextArea;
  JLabel authenticationMessageLabel;
  JPanel authenticationPanel;
  JPasswordField authenticationPasswordField;
  JLabel authenticationPasswordLabel;
  JLabel authenticationUsernameLabel;
  JTextField authenticationUsernameTextField;
  JCheckBoxMenuItem autoConfirmCheckBoxMenuItem;
  JMenu banTitleMenu;
  JLabel blacklistedLabel;
  JList blacklistedList;
  JScrollPane blacklistedScrollPane;
  JButton blacklistedToWhitelistedButton;
  JCheckBoxMenuItem browserNotificationCheckBoxMenuItem;
  JDialog commentsDialog;
  JScrollPane commentsScrollPane;
  JTextPane commentsTextPane;
  JButton connectionIssueButton;
  JPopupMenu connectionIssueButtonPopupMenu;
  JMenuItem copyDownloadLink1MenuItem;
  JMenuItem copyDownloadLink2MenuItem;
  JMenuItem copyFullTitleAndYearMenuItem;
  JMenu copyMenu;
  JMenuItem copyMenuItem;
  Separator copyMenuSeparator1;
  JMenuItem copyPosterImageMenuItem;
  JMenuItem copySelectionMenuItem;
  JMenuItem copySubtitleLinkMenuItem;
  JMenuItem copySummaryLinkMenuItem;
  JMenuItem copyTrailerLinkMenuItem;
  JLabel countryLabel;
  JList countryList;
  JScrollPane countryScrollPane;
  JTextField customExtensionTextField;
  JMenuItem cutMenuItem;
  JRadioButtonMenuItem defaultApplicationDownloaderRadioButtonMenuItem;
  JMenuItem deleteMenuItem;
  JRadioButtonMenuItem download1080HDRadioButtonMenuItem;
  JRadioButtonMenuItem download720HDQualityRadioButtonMenuItem;
  JRadioButtonMenuItem downloadAnyQualityRadioButtonMenuItem;
  JRadioButtonMenuItem downloadDVDQualityRadioButtonMenuItem;
  JRadioButtonMenuItem downloadHighQualityRadioButtonMenuItem;
  JButton downloadLink1Button;
  JMenuItem downloadLink1MenuItem;
  JButton downloadLink2Button;
  JMenuItem downloadLink2MenuItem;
  JMenu downloadMenu;
  Separator downloadMenuSeparator1;
  Separator downloadMenuSeparator2;
  Separator downloadMenuSeparator3;
  Separator downloadMenuSeparator4;
  Separator downloadMenuSeparator5;
  ButtonGroup downloadQualityButtonGroup;
  JMenu downloadQualityMenu;
  JButton downloadSizeButton;
  JDialog downloadSizeDialog;
  JCheckBox downloadSizeIgnoreCheckBox;
  JLabel downloadSizeLabel;
  JMenuItem downloadSizeMenuItem;
  JLabel downloadSizeToLabel;
  ButtonGroup downloaderButtonGroup;
  JMenu downloaderMenu;
  JRadioButtonMenuItem dutchRadioButtonMenuItem;
  JMenu editMenu;
  Separator editMenuSeparator1;
  Separator editMenuSeparator2;
  JMenuItem editProfilesMenuItem;
  JMenuItem emailDownloadLink1MenuItem;
  JMenuItem emailDownloadLink2MenuItem;
  JMenuItem emailEverythingMenuItem;
  JMenu emailMenu;
  Separator emailMenuSeparator1;
  JMenuItem emailSummaryLinkMenuItem;
  JMenuItem emailTrailerLinkMenuItem;
  JCheckBoxMenuItem emailWithDefaultAppCheckBoxMenuItem;
  JDateChooser endDateChooser;
  JRadioButtonMenuItem englishRadioButtonMenuItem;
  JLabel episodeLabel;
  JButton exitBackupModeButton;
  JMenuItem exitMenuItem;
  JButton extensionsButton;
  JDialog extensionsDialog;
  JEditorPane faqEditorPane;
  JFrame faqFrame;
  JMenuItem faqMenuItem;
  JScrollPane faqScrollPane;
  JCheckBoxMenuItem feedCheckBoxMenuItem;
  JLabel fileExtensionsLabel;
  JMenuItem fileExtensionsMenuItem;
  JMenu fileMenu;
  Separator fileMenuSeparator1;
  Separator fileMenuSeparator2;
  JMenuItem findMenuItem;
  JMenuItem findSubtitleMenuItem;
  JTextField findTextField;
  JRadioButtonMenuItem frenchRadioButtonMenuItem;
  JLabel genreLabel;
  JList genreList;
  JScrollPane genreScrollPane;
  JMenuItem hearSummaryMenuItem;
  JMenu helpMenu;
  Separator helpMenuSeparator1;
  Separator helpMenuSeparator2;
  JMenuItem hideMenuItem;
  JMenuItem iconifyMenuItem;
  JRadioButtonMenuItem italianRadioButtonMenuItem;
  ButtonGroup languageButtonGroup;
  JDialog languageCountryDialog;
  JMenuItem languageCountryMenuItem;
  JButton languageCountryOkButton;
  JTextArea languageCountryWarningTextArea;
  JLabel languageLabel;
  JList languageList;
  JMenu languageMenu;
  JScrollPane languageScrollPane;
  JMenuItem listCopyMenuItem;
  JMenuItem listCutMenuItem;
  JMenuItem listDeleteMenuItem;
  JPopupMenu listPopupMenu;
  Separator listPopupMenuSeparator1;
  JMenuItem listSelectAllMenuItem;
  JButton loadMoreResultsButton;
  JLabel loadingLabel;
  JComboBox maxDownloadSizeComboBox;
  JMenuBar menuBar;
  JComboBox minDownloadSizeComboBox;
  JDialog movieSubtitleDialog;
  JButton movieSubtitleDownloadMatch1Button;
  JButton movieSubtitleDownloadMatch2Button;
  JComboBox movieSubtitleFormatComboBox;
  JLabel movieSubtitleFormatLabel;
  JComboBox movieSubtitleLanguageComboBox;
  JLabel movieSubtitleLanguageLabel;
  JLabel movieSubtitleLoadingLabel;
  JDialog msgDialog;
  JEditorPane msgEditorPane;
  JButton msgOKButton;
  JScrollPane msgScrollPane;
  JRadioButtonMenuItem noDownloaderRadioButtonMenuItem;
  JMenuItem pasteMenuItem;
  JCheckBoxMenuItem peerBlockNotificationCheckBoxMenuItem;
  JCheckBoxMenuItem playlistAutoOpenCheckBoxMenuItem;
  JButton playlistBanGroupButton;
  JMenuItem playlistBanGroupMenuItem;
  JMenuItem playlistCopyDownloadLinkMenuItem;
  JMenu playlistCopyMenu;
  JMenuItem playlistCopySelectionMenuItem;
  Separator playlistCopySeparator;
  JRadioButtonMenuItem playlistDownloaderRadioButtonMenuItem;
  JFileChooser playlistFileChooser;
  JTextField playlistFindTextField;
  JMenu playlistMenu;
  JMenuItem playlistMenuItem;
  Separator playlistMenuSeparator1;
  JButton playlistMoveDownButton;
  JMenuItem playlistMoveDownMenuItem;
  JButton playlistMoveUpButton;
  JMenuItem playlistMoveUpMenuItem;
  JButton playlistOpenButton;
  JMenuItem playlistOpenMenuItem;
  JPanel playlistPanel;
  JButton playlistPlayButton;
  JMenuItem playlistPlayMenuItem;
  JCheckBoxMenuItem playlistPlayWithDefaultAppCheckBoxMenuItem;
  JButton playlistReloadGroupButton;
  JMenuItem playlistReloadGroupMenuItem;
  JButton playlistRemoveButton;
  JMenuItem playlistRemoveMenuItem;
  JMenuItem playlistSaveFolderMenuItem;
  JScrollPane playlistScrollPane;
  JCheckBoxMenuItem playlistShowNonVideoItemsCheckBoxMenuItem;
  JTable playlistTable;
  JPopupMenu playlistTablePopupMenu;
  Separator playlistTablePopupMenuSeparator1;
  Separator playlistTablePopupMenuSeparator2;
  Separator playlistTablePopupMenuSeparator3;
  JMenuItem popularMoviesMenuItem;
  JComboBox popularMoviesResultsPerSearchComboBox;
  JLabel popularMoviesResultsPerSearchLabel;
  JMenuItem popularNewHQMoviesMenuItem;
  JMenuItem popularNewHQTVShowsMenuItem;
  JPopupMenu popularPopupMenu;
  JButton popularPopupMenuButton;
  JMenuItem popularTVShowsMenuItem;
  JComboBox popularTVShowsResultsPerSearchComboBox;
  JLabel popularTVShowsResultsPerSearchLabel;
  JDialog portDialog;
  JLabel portLabel;
  JMenuItem portMenuItem;
  JButton portOkButton;
  JCheckBox portRandomizeCheckBox;
  JTextField portTextField;
  JRadioButtonMenuItem portugueseRadioButtonMenuItem;
  JMenuItem printMenuItem;
  JMenuItem profile0MenuItem;
  JMenuItem profile1MenuItem;
  JMenuItem profile2MenuItem;
  JMenuItem profile3MenuItem;
  JMenuItem profile4MenuItem;
  JMenuItem profile5MenuItem;
  JMenuItem profile6MenuItem;
  JMenuItem profile7MenuItem;
  JMenuItem profile8MenuItem;
  JMenuItem profile9MenuItem;
  JButton profileClearButton;
  JComboBox profileComboBox;
  JDialog profileDialog;
  JMenu profileMenu;
  Separator profileMenuSeparator1;
  JButton profileNameChangeCancelButton;
  JDialog profileNameChangeDialog;
  JLabel profileNameChangeLabel;
  JButton profileNameChangeOKButton;
  JTextField profileNameChangeTextField;
  JButton profileOKButton;
  JButton profileRenameButton;
  JButton profileSetButton;
  JButton profileUseButton;
  JButton proxyAddButton;
  JComboBox proxyComboBox;
  JDialog proxyDialog;
  JButton proxyDownloadButton;
  JCheckBox proxyDownloadLinkInfoCheckBox;
  JButton proxyExportButton;
  JFileChooser proxyFileChooser;
  JButton proxyImportButton;
  JLabel proxyLoadingLabel;
  JMenuItem proxyMenuItem;
  JButton proxyOKButton;
  JButton proxyRemoveButton;
  JCheckBox proxySearchEnginesCheckBox;
  JCheckBox proxySubtitlesCheckBox;
  JCheckBox proxyTrailersCheckBox;
  JCheckBox proxyUpdatesCheckBox;
  JLabel proxyUseForLabel;
  JCheckBox proxyVideoInfoCheckBox;
  JComboBox ratingComboBox;
  JLabel ratingLabel;
  JButton readSummaryButton;
  JMenuItem readSummaryMenuItem;
  JComboBox regularResultsPerSearchComboBox;
  JLabel regularResultsPerSearchLabel;
  JLabel releasedLabel;
  JLabel releasedToLabel;
  JButton removeProxiesCancelButton;
  JDialog removeProxiesDialog;
  JLabel removeProxiesLabel;
  JList removeProxiesList;
  JButton removeProxiesRemoveButton;
  JScrollPane removeProxiesScrollPane;
  JMenuItem resetWindowMenuItem;
  JPanel resultsPanel;
  JButton resultsPerSearchButton;
  JDialog resultsPerSearchDialog;
  JMenuItem resultsPerSearchMenuItem;
  JScrollPane resultsScrollPane;
  JTable resultsTable;
  JCheckBoxMenuItem safetyCheckBoxMenuItem;
  JCheckBoxMenuItem searchBanTitleEnableCheckBoxMenuItem;
  JMenu searchBanTitleMenu;
  JButton searchButton;
  JMenu searchMenu;
  Separator searchMenuSeparator1;
  Separator searchMenuSeparator2;
  Separator searchMenuSeparator3;
  Separator searchMenuSeparator4;
  Separator searchMenuSeparator5;
  Separator searchMenuSeparator6;
  Separator searchMenuSeparator7;
  Separator searchMenuSeparator8;
  JTextField searchProgressTextField;
  JLabel seasonLabel;
  JMenuItem selectAllMenuItem;
  JRadioButtonMenuItem spanishRadioButtonMenuItem;
  JSplitPane splitPane;
  JDateChooser startDateChooser;
  JTextField statusBarTextField;
  JFileChooser subtitleFileChooser;
  JEditorPane summaryEditorPane;
  JScrollPane summaryScrollPane;
  JPopupMenu tablePopupMenu;
  Separator tablePopupMenuSeparator1;
  Separator tablePopupMenuSeparator2;
  Separator tablePopupMenuSeparator3;
  Separator tablePopupMenuSeparator4;
  JMenuItem textComponentCopyMenuItem;
  JMenuItem textComponentCutMenuItem;
  JMenuItem textComponentDeleteMenuItem;
  JMenuItem textComponentPasteMenuItem;
  JMenuItem textComponentPasteSearchMenuItem;
  JPopupMenu textComponentPopupMenu;
  Separator textComponentPopupMenuSeparator1;
  JMenuItem textComponentSelectAllMenuItem;
  JDialog timedMsgDialog;
  JLabel timedMsgLabel;
  JButton timeoutButton;
  JComboBox timeoutComboBox;
  JDialog timeoutDialog;
  JLabel timeoutLabel;
  JMenuItem timeoutMenuItem;
  JLabel titleLabel;
  JTextField titleTextField;
  JFileChooser torrentFileChooser;
  JRadioButtonMenuItem trailerMediaPlayer1080RadioButtonMenuItem;
  JRadioButtonMenuItem trailerMediaPlayer240RadioButtonMenuItem;
  JRadioButtonMenuItem trailerMediaPlayer360RadioButtonMenuItem;
  JRadioButtonMenuItem trailerMediaPlayer480RadioButtonMenuItem;
  JRadioButtonMenuItem trailerMediaPlayer720RadioButtonMenuItem;
  JRadioButtonMenuItem trailerMediaPlayerRadioButtonMenuItem;
  ButtonGroup trailerPlayerButtonGroup;
  JMenu trailerPlayerMenu;
  JRadioButtonMenuItem trailerWebBrowserPlayerRadioButtonMenuItem;
  JButton trashCanButton;
  JRadioButtonMenuItem turkishRadioButtonMenuItem;
  JButton tvCancelButton;
  JDialog tvDialog;
  JComboBox tvEpisodeComboBox;
  JComboBox tvSeasonComboBox;
  JLabel tvSelectionLabel;
  JButton tvSubmitButton;
  JDialog tvSubtitleDialog;
  JButton tvSubtitleDownloadMatch1Button;
  JButton tvSubtitleDownloadMatch2Button;
  JComboBox tvSubtitleEpisodeComboBox;
  JLabel tvSubtitleEpisodeLabel;
  JComboBox tvSubtitleFormatComboBox;
  JLabel tvSubtitleFormatLabel;
  JComboBox tvSubtitleLanguageComboBox;
  JLabel tvSubtitleLanguageLabel;
  JLabel tvSubtitleLoadingLabel;
  JComboBox tvSubtitleSeasonComboBox;
  JLabel tvSubtitleSeasonLabel;
  JComboBox typeComboBox;
  JLabel typeLabel;
  JCheckBoxMenuItem updateCheckBoxMenuItem;
  JMenuItem updateMenuItem;
  JMenu viewMenu;
  Separator viewMenuSeparator1;
  JMenuItem watchOnDeviceMenuItem;
  JButton watchTrailerButton;
  JMenuItem watchTrailerMenuItem;
  JLabel whitelistLabel;
  JList whitelistedList;
  JScrollPane whitelistedScrollPane;
  JButton whitelistedToBlacklistedButton;
  // End of variables declaration//GEN-END:variables
}
