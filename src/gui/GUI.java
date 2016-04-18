package gui;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import debug.Debug;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.TrayIcon;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
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
import javax.swing.JTable;
import javax.swing.JTable.PrintMode;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.RootPaneContainer;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument;
import listener.ContentType;
import listener.DomainType;
import listener.FormattedNum;
import listener.GuiListener;
import listener.PlaylistItem;
import listener.Video;
import listener.VideoStrExportListener;
import listener.WorkerListener;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.ExceptionUtil;
import util.IO;
import util.Regex;
import util.WindowsUtil;

public class GUI extends JFrame implements GuiListener {

    private static final long serialVersionUID = 1L;
    private static final Preferences preferences = Preferences.userNodeForPackage(GUI.class);

    private WorkerListener workerListener;
    private boolean isRegularSearcher = true, proceedWithDownload, cancelTVSelection, isAltSearch, isTVShowSearch, isSubtitleMatch1, isTVShowSubtitle, forcePlay;
    boolean viewedPortBefore;
    private final AtomicBoolean isPlaylistRestored = new AtomicBoolean();
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
    private Icon loadingIcon, notLoadingIcon, noWarningIcon, warningIcon, playIcon, stopIcon;
    JList popupList;
    JTextComponent popupTextComponent;
    SyncTable resultsSyncTable, playlistSyncTable;
    private AbstractPopupListener textComponentPopupListener;
    private final ActionListener htmlCopyListener = new HTMLCopyListener(), tableCopyListener = new TableCopyListener(),
            playlistTableCopyListener = new PlaylistTableCopyListener();
    private final Object msgDialogLock = new Object(), optionDialogLock = new Object(), playlistRestorationLock = new Object();
    private final Settings settings = new Settings();
    private final Map<String, Icon> posters = new ConcurrentHashMap<String, Icon>(100);
    final BlockingQueue<String> posterImagePaths = new LinkedBlockingQueue<String>();
    private Thread posterCacher;
    private JTextFieldDateEditor startDateTextField, endDateTextField;
    private TrayIcon trayIcon, playlistTrayIcon;
    boolean usePeerBlock;
    private volatile Thread timedMsgThread;
    final Object timedMsgLock = new Object();
    private final FindControl findControl, playlistFindControl;
    private SplashScreen splashScreen;
    JDialog dummyDialog = new JDialog();
    JMenuItem dummyMenuItem = new JMenuItem(), dummyMenuItem2 = new JMenuItem(), dummyMenuItem3 = new JMenuItem(), dummyMenuItem4 = new JMenuItem(),
            dummyMenuItem5 = new JMenuItem(), dummyMenuItem6 = new JMenuItem(), peerBlockMenuItem, playDefaultAppMenuItem;
    JComboBox dummyComboBox = new JComboBox();
    ButtonGroup trailerPlayerButtonGroup2;

    public GUI(WorkerListener workerListener, SplashScreen splashScreen) throws Exception {
        this.workerListener = workerListener;
        this.splashScreen = splashScreen;

        splashScreen.progress();

        initComponents();
        updateToggleButtons(true);
        UI.initToggleButton(summaryTextToSpeechButton, "speaker.png");
        initFileNameExtensionFilters();

        dummyComboBox.setEditable(true);

        splashScreen.progress();

        findControl = new FindControl(findTextField);
        playlistFindControl = new FindControl(playlistFindTextField);

        splashScreen.progress();

        JOptionPane tempOptionPane = new JOptionPane();
        Color fgColor = tempOptionPane.getForeground(), bgColor = tempOptionPane.getBackground();
        Font font = tempOptionPane.getFont();
        for (JComponent component : new JComponent[]{optionalMsgTextArea, optionalMsgCheckBox, optionalMsgPanel, timedMsgLabel}) {
            component.setForeground(fgColor);
            component.setBackground(bgColor);
            component.setFont(font);
        }

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
        startDateTextField = (JTextFieldDateEditor) startDateChooser.getDateEditor().getUiComponent();
        endDateTextField = (JTextFieldDateEditor) endDateChooser.getDateEditor().getUiComponent();

        splashScreen.progress();

        ActionListener enterKeyListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        };
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
        for (JComponent component : new JComponent[]{titleTextField, genreList, typeComboBox, ratingComboBox, startDateTextField, endDateTextField}) {
            component.registerKeyboardAction(enterKeyListener, "Enter", enterKey, JComponent.WHEN_FOCUSED);
        }

        splashScreen.progress();

        statusBarTextField.setBackground(bgColor = getBackground());
        statusBarTextField.setForeground(fgColor = getForeground());
        statusBarTextField.setSelectionColor(bgColor);
        statusBarTextField.setSelectedTextColor(fgColor);

        splashScreen.progress();

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
                optionalMsgTextArea, commentsTextPane, msgEditorPane, faqEditorPane, aboutEditorPane, summaryEditorPane, safetyEditorPane,
                authenticationUsernameTextField, authenticationPasswordField, startDateTextField, endDateTextField, activationTextField);

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

        UI.addPopupMenu(popularMoviesButtonPopupMenu, popularMoviesButton);

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

        blacklistedList.setModel(blacklistListModel = new DefaultListModel());
        whitelistedList.setModel(whitelistListModel = new DefaultListModel());
        removeProxiesList.setModel(removeProxiesListModel = new DefaultListModel());

        UI.registerCutCopyPasteKeyboardActions(summaryEditorPane, htmlCopyListener);
        UI.registerCutCopyPasteKeyboardActions(resultsTable, tableCopyListener);
        UI.registerCutCopyPasteKeyboardActions(playlistTable, playlistTableCopyListener);

        splashScreen.progress();

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

        playlistSyncTable.tableModel.addTableModelListener(new TableModelListener() {
            private final Map<String, List<String>> cache = new HashMap<String, List<String>>(100);

            @Override
            public void tableChanged(TableModelEvent evt) {
                playlistFindControl.clearFindables();
                for (Object row : playlistSyncTable.tableModel.getDataVector()) {
                    playlistFindControl.addFindable((String) ((List<?>) row).get(playlistNameCol), cache);
                }
            }
        });

        splashScreen.progress();

        UI.add(trailerPlayerButtonGroup, trailerMediaPlayerRadioButtonMenuItem, trailerMediaPlayer1080RadioButtonMenuItem,
                trailerMediaPlayer720RadioButtonMenuItem, trailerMediaPlayer480RadioButtonMenuItem, trailerMediaPlayer360RadioButtonMenuItem,
                trailerMediaPlayer240RadioButtonMenuItem, trailerWebBrowserPlayerRadioButtonMenuItem);
        UI.add(downloaderButtonGroup, playlistDownloaderRadioButtonMenuItem, webBrowserAppDownloaderRadioButtonMenuItem,
                webBrowserAltAppDownloaderRadioButtonMenuItem, defaultApplicationDownloaderRadioButtonMenuItem, noDownloaderRadioButtonMenuItem);

        loadingIcon = UI.icon("loading.gif");
        notLoadingIcon = UI.icon("notLoading.gif");
        for (JLabel label : new JLabel[]{loadingLabel, safetyLoadingLabel, proxyLoadingLabel, tvSubtitleLoadingLabel, movieSubtitleLoadingLabel,
            summaryLoadingLabel, activationLoadingLabel}) {
            label.setIcon(notLoadingIcon);
        }
        noWarningIcon = UI.icon("noWarning.png");
        warningIcon = UI.icon("warning.png");
        connectionIssueButton.setIcon(noWarningIcon);
        UI.setIcon(trashCanButton, "trashCan");
        playIcon = UI.icon("play.png");
        stopIcon = UI.icon("stop.png");
        play(null);
        UI.setIcon(playlistMoveUpButton, "up");
        UI.setIcon(playlistMoveDownButton, "down");
        UI.setIcon(playlistRemoveButton, "remove");
        UI.setIcon(whitelistedToBlacklistedButton, "rightArrow");
        UI.setIcon(blacklistedToWhitelistedButton, "leftArrow");

        splashScreen.progress();

        File proxies = new File(Constant.APP_DIR + Constant.PROXIES);
        if (proxies.exists()) {
            for (String proxy : Regex.split(IO.read(proxies), Constant.NEWLINE)) {
                String newProxy = proxy.trim();
                if (!newProxy.isEmpty()) {
                    proxyComboBox.addItem(newProxy);
                }
            }
        }

        splashScreen.progress();

        for (int i = 0; i < 10; i++) {
            String profile = "GUI.profile" + i + "MenuItem.text";
            profileComboBox.addItem(i == 0 ? Str.str(profile) : preferences.get(profile, Str.str(profile)));
            updateProfileGUIitems(i);
        }
        profileComboBox.setSelectedIndex(0);

        faqEditorPane.setText(Regex.replaceFirst(IO.read(Constant.PROGRAM_DIR + "FAQ" + Constant.HTML), "<br><br><br>", Str.get(555) + "<br><br><br>"));

        splashScreen.progress();

        AutoCompleteDecorator.decorate(titleTextField, Arrays.asList(Regex.split(IO.read(Constant.PROGRAM_DIR + "autoCompleteTitles" + Constant.TXT),
                Constant.NEWLINE)), false);

        splashScreen.progress();

        UI.initCountComboBoxes(414, 502, regularResultsPerSearchComboBox);
        UI.initCountComboBoxes(412, 413, popularMoviesResultsPerSearchComboBox, popularTVShowsResultsPerSearchComboBox);

        splashScreen.progress();

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

        splashScreen.progress();

        String escapeKeyWindowClosingActionMapKey = "VK_ESCAPE:WINDOW_CLOSING", enterKeyWindowClosingActionMapKey = "VK_ENTER:WINDOW_CLOSING";
        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Image icon = Toolkit.getDefaultToolkit().getImage(Constant.PROGRAM_DIR + "icon16x16.png");

        timedMsgDialog.setIconImage(icon);
        Collection<Window> windows = new ArrayList<Window>(Arrays.asList(windows()));
        Collections.addAll(windows, (Window) playlistFrame, activationDialog);

        for (final Window window : windows) {
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
            if (window != playlistFrame && window != activationDialog) {
                inputMap.put(enterKey, enterKeyWindowClosingActionMapKey);
                actionMap.put(enterKeyWindowClosingActionMapKey, windowClosingAction);
            }
        }

        try {
            trayIcon = UI.addMinimizeToTraySupport(this);
            playlistTrayIcon = UI.addMinimizeToTraySupport(playlistFrame);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }

        splashScreen.progress();

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
            for (JComponent component : new JComponent[]{peerBlockNotificationCheckBoxMenuItem, playlistPlayWithDefaultAppCheckBoxMenuItem, searchMenuSeparator7,
                trailerPlayerMenu}) {
                component.setEnabled(false);
                component.setVisible(false);
            }
        }
        settings.loadSettings(Constant.APP_DIR + Constant.USER_SETTINGS);
        playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(null);

        String downloadIDs = preferences.get(Constant.BANNED_DOWNLOAD_IDS, "").trim();
        if (!downloadIDs.isEmpty()) {
            for (String downloadID : downloadIDs.split(Constant.STD_NEWLINE)) {
                bannedDownloadIDs.add(Long.valueOf(downloadID));
            }
        }

        splashScreen.progress();
    }

    private void updateToggleButtons(boolean init) {
        UI.updateToggleButton(searchButton, "GUI.searchButton.text", init);
        UI.updateToggleButton(popularTVShowsButton, "GUI.popularTVShowsButton.text", init);
        UI.updateToggleButton(popularMoviesButton, "GUI.popularMoviesButton.text", init);
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
        String stop = Str.str(Constant.STOP_KEY), popularMovies = popularMoviesButton.getName(), popularTVShows = popularTVShowsButton.getName(), readSummary
                = readSummaryButton.getName(), watchTrailer = watchTrailerButton.getName(), downloadLink1 = downloadLink1Button.getName(), downloadLink2
                = downloadLink2Button.getName();
        UI.resize(AbstractComponent.newInstance(searchButton), stop, searchButton.getName());
        UI.resize(AbstractComponent.newInstance(popularTVShowsButton), popularMovies, stop, popularTVShows);
        UI.resize(AbstractComponent.newInstance(popularMoviesButton), popularTVShows, stop, popularMovies);
        UI.resize(AbstractComponent.newInstance(readSummaryButton), watchTrailer, downloadLink1, downloadLink2, stop, readSummary);
        UI.resize(AbstractComponent.newInstance(watchTrailerButton), readSummary, downloadLink1, downloadLink2, stop, watchTrailer);
        UI.resize(AbstractComponent.newInstance(downloadLink1Button), readSummary, watchTrailer, downloadLink2, stop, downloadLink1);
        UI.resize(AbstractComponent.newInstance(downloadLink2Button), readSummary, watchTrailer, downloadLink1, stop, downloadLink2);
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
        Icon image = posters.get(imagePath);
        if (image == null) {
            posters.put(imagePath, image = new ImageIcon((new ImageIcon(imagePath)).getImage().getScaledInstance(60, 89, Image.SCALE_SMOOTH))); // Not a concurrency bug
        }
        return image;
    }

    public void showFeed(boolean isStartUp) {
        if (isStartUp && !feedCheckBoxMenuItem.isSelected()) {
            return;
        }

        findControl.hide();
        isTVShowSearch = false;
        isRegularSearcher = false;
        int numResultsPerSearch = Integer.parseInt((String) popularMoviesResultsPerSearchComboBox.getSelectedItem());
        String[] languages = UI.selectAnyIfNoSelectionAndCopy(languageList), countries = UI.selectAnyIfNoSelectionAndCopy(countryList);
        workerListener.popularSearchStarted(numResultsPerSearch, isTVShowSearch, languages, countries, true, !isStartUp);
    }

    private void initFileNameExtensionFilters() {
        torrentFileFilter = new FileNameExtensionFilter(Str.str("torrents") + " (*.torrent)", "torrent");
        proxyListFileFilter = new FileNameExtensionFilter(Str.str("proxyList") + " (*" + Constant.TXT + ")", "txt");
        subtitleFileFilter = new FileNameExtensionFilter(Str.str("subtitle2") + " (" + Str.get(451) + ")", Regex.split(452, ","));
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        safetyDialog = new JDialog();
        yesButton = new JButton();
        noButton = new JButton();
        safetyLoadingLabel = new JLabel();
        safetyScrollPane = new JScrollPane();
        safetyEditorPane = new JEditorPane();
        summaryDialog = new JDialog();
        summaryCloseButton = new JButton();
        summaryScrollPane = new JScrollPane();
        summaryEditorPane = new JEditorPane();
        summaryTextToSpeechButton = new JButton();
        summaryLoadingLabel = new JLabel();
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
        timeoutDownloadLinkLabel = new JLabel();
        timeoutDownloadLinkComboBox = new JComboBox();
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
        optionalMsgPanel = new JPanel();
        optionalMsgCheckBox = new JCheckBox();
        optionalMsgTextArea = new JTextArea();
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
        popularMoviesButtonPopupMenu = new JPopupMenu();
        viewNewHighQualityMoviesMenuItem = new JMenuItem();
        playlistFrame = new JFrame() {
            private static final long serialVersionUID = 1L;

            @Override
            public void setMaximizedBounds(Rectangle bounds) {
                super.setMaximizedBounds(bounds == null ? null : UI.getUsableScreenBounds(playlistFrame));
            }
        };
        playlistScrollPane = new JScrollPane();
        playlistTable = new JTable();
        playlistPlayButton = new JButton();
        playlistFindTextField = new JTextField();
        playlistMoveUpButton = new JButton();
        playlistMoveDownButton = new JButton();
        playlistRemoveButton = new JButton();
        playlistTablePopupMenu = new JPopupMenu();
        playlistPlayMenuItem = new JMenuItem();
        playlistOpenMenuItem = new JMenuItem();
        playlistMoveUpMenuItem = new JMenuItem();
        playlistMoveDownMenuItem = new JMenuItem();
        playlistTablePopupMenuSeparator1 = new Separator();
        playlistCopyMenuItem = new JMenuItem();
        playlistTablePopupMenuSeparator2 = new Separator();
        playlistRemoveMenuItem = new JMenuItem();
        playlistReloadGroupMenuItem = new JMenuItem();
        playlistTablePopupMenuSeparator3 = new Separator();
        playlistBanGroupMenuItem = new JMenuItem();
        activationDialog = new JDialog();
        activationUpgradeButton = new JButton();
        activationUpgradeLabel = new JLabel();
        activationCodeLabel = new JLabel();
        activationTextField = new JTextField();
        activationButton = new JButton();
        activationLoadingLabel = new JLabel();
        languageButtonGroup = new ButtonGroup();
        trailerPlayerButtonGroup = new ButtonGroup();
        titleTextField = new JTextField();
        titleLabel = new JLabel();
        releasedLabel = new JLabel();
        genreLabel = new JLabel();
        ratingComboBox = new JComboBox();
        ratingLabel = new JLabel();
        resultsScrollPane = new JScrollPane();
        resultsTable = new JTable();
        searchButton = new JButton();
        genreScrollPane = new JScrollPane();
        genreList = new JList();
        loadMoreResultsButton = new JButton();
        typeLabel = new JLabel();
        typeComboBox = new JComboBox();
        releasedToLabel = new JLabel();
        hqVideoTypeCheckBox = new JCheckBox();
        dvdCheckBox = new JCheckBox();
        hd720CheckBox = new JCheckBox();
        hd1080CheckBox = new JCheckBox();
        popularMoviesButton = new JButton();
        popularTVShowsButton = new JButton();
        loadingLabel = new JLabel();
        readSummaryButton = new JButton();
        watchTrailerButton = new JButton();
        downloadLink1Button = new JButton();
        downloadLink2Button = new JButton();
        statusBarTextField = new JTextField();
        searchProgressTextField = new JTextField();
        exitBackupModeButton = new JButton();
        connectionIssueButton = new JButton();
        startDateChooser = new DateChooser();
        endDateChooser = new DateChooser();
        findTextField = new JTextField();
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
        englishRadioButtonMenuItem = new JRadioButtonMenuItem();
        spanishRadioButtonMenuItem = new JRadioButtonMenuItem();
        frenchRadioButtonMenuItem = new JRadioButtonMenuItem();
        italianRadioButtonMenuItem = new JRadioButtonMenuItem();
        dutchRadioButtonMenuItem = new JRadioButtonMenuItem();
        portugueseRadioButtonMenuItem = new JRadioButtonMenuItem();
        turkishRadioButtonMenuItem = new JRadioButtonMenuItem();
        viewMenuSeparator1 = new Separator();
        resetWindowMenuItem = new JMenuItem();
        searchMenu = new JMenu();
        resultsPerSearchMenuItem = new JMenuItem();
        searchMenuSeparator1 = new Separator();
        timeoutMenuItem = new JMenuItem();
        searchMenuSeparator2 = new Separator();
        proxyMenuItem = new JMenuItem();
        searchMenuSeparator3 = new Separator();
        languageCountryMenuItem = new JMenuItem();
        searchMenuSeparator4 = new Separator();
        feedCheckBoxMenuItem = new JCheckBoxMenuItem();
        searchMenuSeparator5 = new Separator();
        browserNotificationCheckBoxMenuItem = new JCheckBoxMenuItem();
        searchMenuSeparator6 = new Separator();
        emailWithDefaultAppCheckBoxMenuItem = new JCheckBoxMenuItem();
        searchMenuSeparator7 = new Separator();
        trailerPlayerMenu = new JMenu();
        trailerMediaPlayerRadioButtonMenuItem = new JRadioButtonMenuItem();
        trailerMediaPlayer1080RadioButtonMenuItem = new JRadioButtonMenuItem();
        trailerMediaPlayer720RadioButtonMenuItem = new JRadioButtonMenuItem();
        trailerMediaPlayer480RadioButtonMenuItem = new JRadioButtonMenuItem();
        trailerMediaPlayer360RadioButtonMenuItem = new JRadioButtonMenuItem();
        trailerMediaPlayer240RadioButtonMenuItem = new JRadioButtonMenuItem();
        trailerWebBrowserPlayerRadioButtonMenuItem = new JRadioButtonMenuItem();
        playlistMenu = new JMenu();
        playlistMenuItem = new JMenuItem();
        playlistMenuSeparator1 = new Separator();
        playlistSaveFolderMenuItem = new JMenuItem();
        playlistAutoOpenCheckBoxMenuItem = new JCheckBoxMenuItem();
        playlistPlayWithDefaultAppCheckBoxMenuItem = new JCheckBoxMenuItem();
        playlistShowNonVideoItemsCheckBoxMenuItem = new JCheckBoxMenuItem();
        downloadMenu = new JMenu();
        downloadSizeMenuItem = new JMenuItem();
        downloadMenuSeparator1 = new Separator();
        fileExtensionsMenuItem = new JMenuItem();
        downloadMenuSeparator2 = new Separator();
        safetyCheckBoxMenuItem = new JCheckBoxMenuItem();
        peerBlockNotificationCheckBoxMenuItem = new JCheckBoxMenuItem();
        downloadMenuSeparator3 = new Separator();
        portMenuItem = new JMenuItem();
        downloadMenuSeparator4 = new Separator();
        downloaderMenu = new JMenu();
        playlistDownloaderRadioButtonMenuItem = new JRadioButtonMenuItem();
        webBrowserAppDownloaderRadioButtonMenuItem = new JRadioButtonMenuItem();
        webBrowserAltAppDownloaderRadioButtonMenuItem = new JRadioButtonMenuItem();
        defaultApplicationDownloaderRadioButtonMenuItem = new JRadioButtonMenuItem();
        noDownloaderRadioButtonMenuItem = new JRadioButtonMenuItem();
        helpMenu = new JMenu();
        faqMenuItem = new JMenuItem();
        helpMenuSeparator1 = new Separator();
        updateMenuItem = new JMenuItem();
        updateCheckBoxMenuItem = new JCheckBoxMenuItem();
        helpMenuSeparator2 = new Separator();
        aboutMenuItem = new JMenuItem();
        splashScreen.progress();

        ResourceBundle bundle = ResourceBundle.getBundle("i18n/Bundle"); // NOI18N
        safetyDialog.setTitle(bundle.getString("GUI.safetyDialog.title")); // NOI18N
        safetyDialog.setAlwaysOnTop(true);
        safetyDialog.setIconImage(null);
        safetyDialog.setModalityType(ModalityType.APPLICATION_MODAL);

        yesButton.setText(bundle.getString("GUI.yesButton.text")); // NOI18N
        yesButton.setToolTipText(bundle.getString("GUI.yesButton.toolTipText")); // NOI18N
        yesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                yesButtonActionPerformed(evt);
            }
        });

        noButton.setText(bundle.getString("GUI.noButton.text")); // NOI18N
        noButton.setToolTipText(bundle.getString("GUI.noButton.toolTipText")); // NOI18N
        noButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                noButtonActionPerformed(evt);
            }
        });

        safetyLoadingLabel.setText(null);

        safetyScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        safetyEditorPane.setContentType("text/html"); // NOI18N
        safetyEditorPane.setEditable(false);
        safetyEditorPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent evt) {
                safetyEditorPaneHyperlinkUpdate(evt);
            }
        });
        safetyScrollPane.setViewportView(safetyEditorPane);

        GroupLayout safetyDialogLayout = new GroupLayout(safetyDialog.getContentPane());
        safetyDialog.getContentPane().setLayout(safetyDialogLayout);
        safetyDialogLayout.setHorizontalGroup(safetyDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(safetyDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(safetyDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(safetyDialogLayout.createSequentialGroup()
                        .addComponent(yesButton, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(noButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 194, Short.MAX_VALUE)
                        .addComponent(safetyLoadingLabel))
                    .addComponent(safetyScrollPane, GroupLayout.DEFAULT_SIZE, 404, Short.MAX_VALUE))
                .addContainerGap())
        );

        safetyDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {noButton, yesButton});

        safetyDialogLayout.setVerticalGroup(safetyDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, safetyDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(safetyScrollPane, GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(safetyDialogLayout.createParallelGroup(Alignment.TRAILING)
                    .addGroup(safetyDialogLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(yesButton)
                        .addComponent(noButton))
                    .addComponent(safetyLoadingLabel))
                .addContainerGap())
        );

        safetyDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {noButton, yesButton});

        splashScreen.progress();

        summaryDialog.setTitle(bundle.getString("GUI.summaryDialog.title")); // NOI18N
        summaryDialog.setAlwaysOnTop(true);

        summaryCloseButton.setText(bundle.getString("GUI.summaryCloseButton.text")); // NOI18N
        summaryCloseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                summaryCloseButtonActionPerformed(evt);
            }
        });
        summaryCloseButton.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                summaryCloseButtonKeyPressed(evt);
            }
        });

        summaryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        summaryEditorPane.setEditable(false);
        summaryEditorPane.setContentType("text/html"); // NOI18N
        summaryScrollPane.setViewportView(summaryEditorPane);

        summaryTextToSpeechButton.setText(null);
        summaryTextToSpeechButton.setToolTipText(bundle.getString("GUI.summaryTextToSpeechButton.toolTipText")); // NOI18N
        summaryTextToSpeechButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                summaryTextToSpeechButtonActionPerformed(evt);
            }
        });

        summaryLoadingLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        summaryLoadingLabel.setText(null);

        GroupLayout summaryDialogLayout = new GroupLayout(summaryDialog.getContentPane());
        summaryDialog.getContentPane().setLayout(summaryDialogLayout);
        summaryDialogLayout.setHorizontalGroup(summaryDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(summaryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(summaryDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(summaryScrollPane, GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
                    .addGroup(summaryDialogLayout.createSequentialGroup()
                        .addComponent(summaryTextToSpeechButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 164, Short.MAX_VALUE)
                        .addComponent(summaryCloseButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 164, Short.MAX_VALUE)
                        .addComponent(summaryLoadingLabel)))
                .addContainerGap())
        );

        summaryDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {summaryCloseButton, summaryLoadingLabel, summaryTextToSpeechButton});

        summaryDialogLayout.setVerticalGroup(summaryDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, summaryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(summaryScrollPane, GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(summaryDialogLayout.createParallelGroup(Alignment.TRAILING)
                    .addGroup(summaryDialogLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(summaryCloseButton)
                        .addComponent(summaryTextToSpeechButton))
                    .addComponent(summaryLoadingLabel))
                .addContainerGap())
        );

        summaryDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {summaryCloseButton, summaryLoadingLabel, summaryTextToSpeechButton});

        splashScreen.progress();

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
                faqEditorPaneHyperlinkUpdate(evt);
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

        splashScreen.progress();

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

        splashScreen.progress();

        timeoutDialog.setTitle(bundle.getString("GUI.timeoutDialog.title")); // NOI18N
        timeoutDialog.setAlwaysOnTop(true);
        timeoutDialog.setModal(true);
        timeoutDialog.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);

        timeoutLabel.setText(bundle.getString("GUI.timeoutLabel.text")); // NOI18N
        timeoutLabel.setToolTipText(bundle.getString("GUI.timeoutLabel.toolTipText")); // NOI18N

        timeoutComboBox.setModel(new DefaultComboBoxModel(UI.items(5, 180, 5, false, null, null)));

        timeoutButton.setText(bundle.getString("GUI.timeoutButton.text")); // NOI18N
        timeoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                timeoutButtonActionPerformed(evt);
            }
        });

        timeoutDownloadLinkLabel.setText(bundle.getString("GUI.timeoutDownloadLinkLabel.text")); // NOI18N
        timeoutDownloadLinkLabel.setToolTipText(bundle.getString("GUI.timeoutDownloadLinkLabel.toolTipText")); // NOI18N

        timeoutDownloadLinkComboBox.setModel(new DefaultComboBoxModel(UI.items(0, 180, 5, false, null, null)));

        GroupLayout timeoutDialogLayout = new GroupLayout(timeoutDialog.getContentPane());
        timeoutDialog.getContentPane().setLayout(timeoutDialogLayout);
        timeoutDialogLayout.setHorizontalGroup(timeoutDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(timeoutDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(timeoutDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(timeoutDialogLayout.createSequentialGroup()
                        .addGroup(timeoutDialogLayout.createParallelGroup(Alignment.TRAILING, false)
                            .addComponent(timeoutDownloadLinkLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(timeoutLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(timeoutDialogLayout.createParallelGroup(Alignment.LEADING, false)
                            .addComponent(timeoutComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(timeoutDownloadLinkComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(timeoutButton))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        timeoutDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {timeoutDownloadLinkLabel, timeoutLabel});

        timeoutDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {timeoutComboBox, timeoutDownloadLinkComboBox});

        timeoutDialogLayout.setVerticalGroup(timeoutDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(timeoutDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(timeoutDialogLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(timeoutLabel)
                    .addComponent(timeoutComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(timeoutDialogLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(timeoutDownloadLinkLabel)
                    .addComponent(timeoutDownloadLinkComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(timeoutButton)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        timeoutDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {timeoutDownloadLinkLabel, timeoutLabel});

        timeoutDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {timeoutComboBox, timeoutDownloadLinkComboBox});

        splashScreen.progress();

        tvDialog.setTitle(bundle.getString("GUI.tvDialog.title")); // NOI18N
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

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

        readSummaryMenuItem.setText(bundle.getString("GUI.readSummaryMenuItem.text")); // NOI18N
        readSummaryMenuItem.setEnabled(false);
        readSummaryMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readSummaryMenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(readSummaryMenuItem);

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

        splashScreen.progress();

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

        splashScreen.progress();

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
                        .addGap(0, 0, Short.MAX_VALUE))
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
                        .addPreferredGap(ComponentPlacement.RELATED, 158, Short.MAX_VALUE)
                        .addComponent(proxyOKButton)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(proxyLoadingLabel)))
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

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

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

        splashScreen.progress();

        commentsDialog.setTitle(bundle.getString("GUI.commentsDialog.title")); // NOI18N
        commentsDialog.setModalityType(ModalityType.APPLICATION_MODAL);

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

        splashScreen.progress();

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

        splashScreen.progress();

        optionalMsgPanel.setOpaque(false);

        optionalMsgCheckBox.setText(bundle.getString("GUI.optionalMsgCheckBox.text")); // NOI18N
        optionalMsgCheckBox.setBorder(null);
        optionalMsgCheckBox.setFocusPainted(false);
        optionalMsgCheckBox.setMargin(new Insets(2, 0, 2, 2));
        optionalMsgCheckBox.setOpaque(false);

        optionalMsgTextArea.setEditable(false);
        optionalMsgTextArea.setColumns(20);
        optionalMsgTextArea.setLineWrap(true);
        optionalMsgTextArea.setRows(5);
        optionalMsgTextArea.setWrapStyleWord(true);
        optionalMsgTextArea.setOpaque(false);

        GroupLayout optionalMsgPanelLayout = new GroupLayout(optionalMsgPanel);
        optionalMsgPanel.setLayout(optionalMsgPanelLayout);
        optionalMsgPanelLayout.setHorizontalGroup(optionalMsgPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(optionalMsgPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionalMsgPanelLayout.createParallelGroup(Alignment.LEADING, false)
                    .addComponent(optionalMsgCheckBox)
                    .addComponent(optionalMsgTextArea, GroupLayout.PREFERRED_SIZE, 354, GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        optionalMsgPanelLayout.setVerticalGroup(optionalMsgPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, optionalMsgPanelLayout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(optionalMsgTextArea, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(optionalMsgCheckBox)
                .addContainerGap())
        );

        splashScreen.progress();

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
                        .addComponent(tvSubtitleLanguageComboBox, 0, 164, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(tvSubtitleFormatLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(tvSubtitleFormatComboBox, 0, 164, Short.MAX_VALUE)
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
                        .addPreferredGap(ComponentPlacement.RELATED)
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

        splashScreen.progress();

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
                        .addComponent(movieSubtitleLanguageComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(movieSubtitleFormatLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(movieSubtitleFormatComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(movieSubtitleDialogLayout.createSequentialGroup()
                        .addComponent(movieSubtitleDownloadMatch1Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(movieSubtitleDownloadMatch2Button)
                        .addPreferredGap(ComponentPlacement.RELATED, 95, Short.MAX_VALUE)
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

        splashScreen.progress();

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

        splashScreen.progress();

        torrentFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        torrentFileChooser.setCurrentDirectory(null);

        subtitleFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        subtitleFileChooser.setCurrentDirectory(null);

        authenticationMessageLabel.setText(bundle.getString("GUI.authenticationMessageLabel.text")); // NOI18N

        authenticationUsernameLabel.setText(bundle.getString("GUI.authenticationUsernameLabel.text")); // NOI18N

        authenticationUsernameTextField.setText(null);
        authenticationUsernameTextField.addAncestorListener(new AncestorListener() {
            public void ancestorMoved(AncestorEvent evt) {
            }
            public void ancestorAdded(AncestorEvent evt) {
                authenticationUsernameTextFieldAncestorAdded(evt);
            }
            public void ancestorRemoved(AncestorEvent evt) {
            }
        });

        authenticationPasswordLabel.setText(bundle.getString("GUI.authenticationPasswordLabel.text")); // NOI18N

        authenticationPasswordField.setText(null);
        authenticationPasswordField.setEchoChar('\u2022');
        authenticationPasswordField.addAncestorListener(new AncestorListener() {
            public void ancestorMoved(AncestorEvent evt) {
            }
            public void ancestorAdded(AncestorEvent evt) {
                authenticationPasswordFieldAncestorAdded(evt);
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

        splashScreen.progress();

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

        splashScreen.progress();

        hideMenuItem.setText(bundle.getString("GUI.hideMenuItem.text")); // NOI18N
        hideMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hideMenuItemActionPerformed(evt);
            }
        });
        connectionIssueButtonPopupMenu.add(hideMenuItem);

        splashScreen.progress();

        viewNewHighQualityMoviesMenuItem.setText(bundle.getString("GUI.viewNewHighQualityMoviesMenuItem.text")); // NOI18N
        viewNewHighQualityMoviesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                viewNewHighQualityMoviesMenuItemActionPerformed(evt);
            }
        });
        popularMoviesButtonPopupMenu.add(viewNewHighQualityMoviesMenuItem);

        splashScreen.progress();

        playlistFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        playlistFrame.setTitle(bundle.getString("GUI.playlistFrame.title")); // NOI18N
        playlistFrame.setAlwaysOnTop(true);
        playlistFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                playlistFrameWindowClosing(evt);
            }
        });

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
            playlistTable.getColumnModel().getColumn(0).setPreferredWidth(703);
            playlistTable.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("GUI.playlistTable.columnModel.title0")); // NOI18N
            playlistTable.getColumnModel().getColumn(1).setPreferredWidth(70);
            playlistTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("GUI.playlistTable.columnModel.title1")); // NOI18N
            playlistTable.getColumnModel().getColumn(2).setPreferredWidth(158);
            playlistTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("GUI.playlistTable.columnModel.title2")); // NOI18N
            playlistTable.getColumnModel().getColumn(3).setPreferredWidth(0);
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
        playlistPlayButton.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                playlistPlayButtonKeyPressed(evt);
            }
        });

        playlistFindTextField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                playlistFindTextFieldKeyPressed(evt);
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
        playlistMoveUpButton.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                playlistMoveUpButtonKeyPressed(evt);
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
        playlistMoveDownButton.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                playlistMoveDownButtonKeyPressed(evt);
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
        playlistRemoveButton.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                playlistRemoveButtonKeyPressed(evt);
            }
        });

        GroupLayout playlistFrameLayout = new GroupLayout(playlistFrame.getContentPane());
        playlistFrame.getContentPane().setLayout(playlistFrameLayout);
        playlistFrameLayout.setHorizontalGroup(playlistFrameLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(playlistFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(playlistFrameLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(playlistScrollPane, GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE)
                    .addGroup(playlistFrameLayout.createSequentialGroup()
                        .addComponent(playlistPlayButton)
                        .addGap(18, 18, 18)
                        .addComponent(playlistFindTextField)
                        .addGap(18, 18, 18)
                        .addComponent(playlistMoveUpButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(playlistMoveDownButton)
                        .addGap(18, 18, 18)
                        .addComponent(playlistRemoveButton)))
                .addContainerGap())
        );
        playlistFrameLayout.setVerticalGroup(playlistFrameLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(playlistFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(playlistScrollPane, GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(playlistFrameLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(playlistFindTextField, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
                    .addGroup(playlistFrameLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(playlistPlayButton)
                        .addComponent(playlistMoveUpButton)
                        .addComponent(playlistMoveDownButton)
                        .addComponent(playlistRemoveButton)))
                .addContainerGap())
        );

        playlistFrameLayout.linkSize(SwingConstants.VERTICAL, new Component[] {playlistFindTextField, playlistMoveDownButton, playlistMoveUpButton, playlistPlayButton, playlistRemoveButton});

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

        playlistCopyMenuItem.setText(bundle.getString("GUI.playlistCopyMenuItem.text")); // NOI18N
        playlistCopyMenuItem.setEnabled(false);
        playlistCopyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistCopyMenuItemActionPerformed(evt);
            }
        });
        playlistTablePopupMenu.add(playlistCopyMenuItem);
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

        activationDialog.setTitle(bundle.getString("GUI.activationDialog.title")); // NOI18N
        activationDialog.setAlwaysOnTop(true);
        activationDialog.setIconImage(null);
        activationDialog.setModal(true);
        activationDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                activationDialogWindowClosing(evt);
            }
        });

        activationUpgradeButton.setText(bundle.getString("GUI.activationUpgradeButton.text")); // NOI18N
        activationUpgradeButton.setToolTipText(bundle.getString("GUI.activationUpgradeButton.toolTipText")); // NOI18N
        activationUpgradeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                activationUpgradeButtonActionPerformed(evt);
            }
        });

        activationUpgradeLabel.setText(bundle.getString("GUI.activationUpgradeLabel.text")); // NOI18N

        activationCodeLabel.setText(bundle.getString("GUI.activationCodeLabel.text")); // NOI18N

        activationButton.setText(bundle.getString("GUI.activationButton.text")); // NOI18N
        activationButton.setToolTipText(bundle.getString("GUI.activationButton.toolTipText")); // NOI18N
        activationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                activationButtonActionPerformed(evt);
            }
        });

        activationLoadingLabel.setText(null);

        GroupLayout activationDialogLayout = new GroupLayout(activationDialog.getContentPane());
        activationDialog.getContentPane().setLayout(activationDialogLayout);
        activationDialogLayout.setHorizontalGroup(activationDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(activationDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(activationDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(activationDialogLayout.createSequentialGroup()
                        .addComponent(activationCodeLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(activationTextField))
                    .addGroup(activationDialogLayout.createSequentialGroup()
                        .addComponent(activationUpgradeButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(activationUpgradeLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(activationDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(activationButton)
                    .addComponent(activationLoadingLabel))
                .addContainerGap())
        );

        activationDialogLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {activationCodeLabel, activationUpgradeButton});

        activationDialogLayout.setVerticalGroup(activationDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(activationDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(activationDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(activationDialogLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(activationUpgradeButton)
                        .addComponent(activationUpgradeLabel))
                    .addComponent(activationLoadingLabel, Alignment.TRAILING))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(activationDialogLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(activationTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(activationCodeLabel)
                    .addComponent(activationButton))
                .addContainerGap())
        );

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

        searchButton.setText(bundle.getString("GUI.searchButton.text")); // NOI18N
        searchButton.setToolTipText(bundle.getString("GUI.searchButton.toolTipText")); // NOI18N
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        List<String> genres = new ArrayList<String>(32);
        genres.add(Str.str("any"));
        Collections.addAll(genres, Regex.split(359, Constant.SEPARATOR1));
        UI.init(genreList, genres.toArray(Constant.EMPTY_STRS));
        genreList.setSelectedValue(Str.str("any"), true);
        genreList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                genreListValueChanged(evt);
            }
        });
        genreScrollPane.setViewportView(genreList);

        loadMoreResultsButton.setText(bundle.getString("GUI.loadMoreResultsButton.text")); // NOI18N
        loadMoreResultsButton.setToolTipText(bundle.getString("GUI.loadMoreResultsButton.toolTipText")); // NOI18N
        loadMoreResultsButton.setEnabled(false);
        loadMoreResultsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                loadMoreResultsButtonActionPerformed(evt);
            }
        });

        typeLabel.setLabelFor(typeComboBox);
        typeLabel.setText(bundle.getString("GUI.typeLabel.text")); // NOI18N
        typeLabel.setToolTipText(bundle.getString("GUI.typeLabel.toolTipText")); // NOI18N

        UI.init(typeComboBox, Str.strs("GUI.typeComboBox.model"));

        releasedToLabel.setLabelFor(endDateChooser);
        releasedToLabel.setText(bundle.getString("GUI.releasedToLabel.text")); // NOI18N

        hqVideoTypeCheckBox.setText(Constant.HQ);
        hqVideoTypeCheckBox.setToolTipText(bundle.getString("GUI.hqVideoTypeCheckBox.toolTipText")); // NOI18N
        hqVideoTypeCheckBox.setMargin(new Insets(0, 0, 0, 0));
        hqVideoTypeCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hqVideoTypeCheckBoxActionPerformed(evt);
            }
        });

        dvdCheckBox.setText(Constant.DVD);
        dvdCheckBox.setToolTipText(bundle.getString("GUI.dvdCheckBox.toolTipText")); // NOI18N
        dvdCheckBox.setMargin(new Insets(0, 0, 0, 0));
        dvdCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dvdCheckBoxActionPerformed(evt);
            }
        });

        hd720CheckBox.setText(Constant.HD720);
        hd720CheckBox.setToolTipText(bundle.getString("GUI.hd720CheckBox.toolTipText")); // NOI18N
        hd720CheckBox.setMargin(new Insets(0, 0, 0, 0));
        hd720CheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hd720CheckBoxActionPerformed(evt);
            }
        });

        hd1080CheckBox.setText(Constant.HD1080);
        hd1080CheckBox.setToolTipText(bundle.getString("GUI.hd1080CheckBox.toolTipText")); // NOI18N
        hd1080CheckBox.setMargin(new Insets(0, 0, 0, 0));
        hd1080CheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hd1080CheckBoxActionPerformed(evt);
            }
        });

        popularMoviesButton.setText(bundle.getString("GUI.popularMoviesButton.text")); // NOI18N
        popularMoviesButton.setToolTipText(bundle.getString("GUI.popularMoviesButton.toolTipText")); // NOI18N
        popularMoviesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                popularMoviesButtonActionPerformed(evt);
            }
        });

        popularTVShowsButton.setText(bundle.getString("GUI.popularTVShowsButton.text")); // NOI18N
        popularTVShowsButton.setToolTipText(bundle.getString("GUI.popularTVShowsButton.toolTipText")); // NOI18N
        popularTVShowsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                popularTVShowsButtonActionPerformed(evt);
            }
        });

        loadingLabel.setText(null);

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
        downloadLink2Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadLink2ButtonActionPerformed(evt);
            }
        });

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

        exitBackupModeButton.setBorderPainted(false);
        exitBackupModeButton.setEnabled(false);
        exitBackupModeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                exitBackupModeButtonActionPerformed(evt);
            }
        });

        connectionIssueButton.setText(null);
        connectionIssueButton.setBorderPainted(false);
        connectionIssueButton.setEnabled(false);
        connectionIssueButton.setMargin(new Insets(0, 0, 0, 0));
        connectionIssueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                connectionIssueButtonActionPerformed(evt);
            }
        });

        findTextField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                findTextFieldKeyPressed(evt);
            }
        });

        fileMenu.setText(bundle.getString("GUI.fileMenu.text")); // NOI18N
        splashScreen.progress();

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

        profile0MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_MASK));
        profile0MenuItem.setText(bundle.getString("GUI.profile0MenuItem.text")); // NOI18N
        profile0MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile0MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile0MenuItem);

        profile1MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_MASK));
        profile1MenuItem.setText(bundle.getString("GUI.profile1MenuItem.text")); // NOI18N
        profile1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile1MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile1MenuItem);

        profile2MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_MASK));
        profile2MenuItem.setText(bundle.getString("GUI.profile2MenuItem.text")); // NOI18N
        profile2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile2MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile2MenuItem);

        profile3MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_MASK));
        profile3MenuItem.setText(bundle.getString("GUI.profile3MenuItem.text")); // NOI18N
        profile3MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile3MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile3MenuItem);

        profile4MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_MASK));
        profile4MenuItem.setText(bundle.getString("GUI.profile4MenuItem.text")); // NOI18N
        profile4MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile4MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile4MenuItem);

        profile5MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.CTRL_MASK));
        profile5MenuItem.setText(bundle.getString("GUI.profile5MenuItem.text")); // NOI18N
        profile5MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile5MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile5MenuItem);

        profile6MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.CTRL_MASK));
        profile6MenuItem.setText(bundle.getString("GUI.profile6MenuItem.text")); // NOI18N
        profile6MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile6MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile6MenuItem);

        profile7MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_MASK));
        profile7MenuItem.setText(bundle.getString("GUI.profile7MenuItem.text")); // NOI18N
        profile7MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile7MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile7MenuItem);

        profile8MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.CTRL_MASK));
        profile8MenuItem.setText(bundle.getString("GUI.profile8MenuItem.text")); // NOI18N
        profile8MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile8MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile8MenuItem);

        profile9MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.CTRL_MASK));
        profile9MenuItem.setText(bundle.getString("GUI.profile9MenuItem.text")); // NOI18N
        profile9MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile9MenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(profile9MenuItem);
        profileMenu.add(profileMenuSeparator1);

        editProfilesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK | InputEvent.CTRL_MASK));
        editProfilesMenuItem.setText(bundle.getString("GUI.editProfilesMenuItem.text")); // NOI18N
        editProfilesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                editProfilesMenuItemActionPerformed(evt);
            }
        });
        profileMenu.add(editProfilesMenuItem);

        fileMenu.add(profileMenu);
        fileMenu.add(fileMenuSeparator1);

        printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK));
        printMenuItem.setText(bundle.getString("GUI.printMenuItem.text")); // NOI18N
        printMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                printMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(printMenuItem);
        fileMenu.add(fileMenuSeparator2);

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

        cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
        cutMenuItem.setText(bundle.getString("GUI.cutMenuItem.text")); // NOI18N
        cutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cutMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(cutMenuItem);

        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        copyMenuItem.setText(bundle.getString("GUI.copyMenuItem.text")); // NOI18N
        copyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copyMenuItem);

        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
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

        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        selectAllMenuItem.setText(bundle.getString("GUI.selectAllMenuItem.text")); // NOI18N
        selectAllMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                selectAllMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(selectAllMenuItem);
        editMenu.add(editMenuSeparator2);

        findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
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

        resetWindowMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
        resetWindowMenuItem.setText(bundle.getString("GUI.resetWindowMenuItem.text")); // NOI18N
        resetWindowMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                resetWindowMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(resetWindowMenuItem);

        menuBar.add(viewMenu);

        searchMenu.setText(bundle.getString("GUI.searchMenu.text")); // NOI18N

        resultsPerSearchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
        resultsPerSearchMenuItem.setText(bundle.getString("GUI.resultsPerSearchMenuItem.text")); // NOI18N
        resultsPerSearchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                resultsPerSearchMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(resultsPerSearchMenuItem);
        searchMenu.add(searchMenuSeparator1);

        timeoutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
        timeoutMenuItem.setText(bundle.getString("GUI.timeoutMenuItem.text")); // NOI18N
        timeoutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                timeoutMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(timeoutMenuItem);
        searchMenu.add(searchMenuSeparator2);

        proxyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
        proxyMenuItem.setText(bundle.getString("GUI.proxyMenuItem.text")); // NOI18N
        proxyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(proxyMenuItem);
        searchMenu.add(searchMenuSeparator3);

        languageCountryMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
        languageCountryMenuItem.setText(bundle.getString("GUI.languageCountryMenuItem.text")); // NOI18N
        languageCountryMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                languageCountryMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(languageCountryMenuItem);
        searchMenu.add(searchMenuSeparator4);

        feedCheckBoxMenuItem.setText(bundle.getString("GUI.feedCheckBoxMenuItem.text")); // NOI18N
        feedCheckBoxMenuItem.setToolTipText(bundle.getString("GUI.feedCheckBoxMenuItem.toolTipText")); // NOI18N
        searchMenu.add(feedCheckBoxMenuItem);
        searchMenu.add(searchMenuSeparator5);

        browserNotificationCheckBoxMenuItem.setSelected(true);
        browserNotificationCheckBoxMenuItem.setText(bundle.getString("GUI.browserNotificationCheckBoxMenuItem.text")); // NOI18N
        searchMenu.add(browserNotificationCheckBoxMenuItem);
        searchMenu.add(searchMenuSeparator6);

        emailWithDefaultAppCheckBoxMenuItem.setSelected(true);
        emailWithDefaultAppCheckBoxMenuItem.setText(bundle.getString("GUI.emailWithDefaultAppCheckBoxMenuItem.text")); // NOI18N
        searchMenu.add(emailWithDefaultAppCheckBoxMenuItem);
        searchMenu.add(searchMenuSeparator7);

        trailerPlayerMenu.setText(bundle.getString("GUI.trailerPlayerMenu.text")); // NOI18N

        trailerMediaPlayerRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        trailerMediaPlayerRadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayerRadioButtonMenuItem.text")); // NOI18N
        trailerPlayerMenu.add(trailerMediaPlayerRadioButtonMenuItem);

        trailerMediaPlayer1080RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
        trailerMediaPlayer1080RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer1080RadioButtonMenuItem.text")); // NOI18N
        trailerPlayerMenu.add(trailerMediaPlayer1080RadioButtonMenuItem);

        trailerMediaPlayer720RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
        trailerMediaPlayer720RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer720RadioButtonMenuItem.text")); // NOI18N
        trailerPlayerMenu.add(trailerMediaPlayer720RadioButtonMenuItem);

        trailerMediaPlayer480RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK));
        trailerMediaPlayer480RadioButtonMenuItem.setSelected(true);
        trailerMediaPlayer480RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer480RadioButtonMenuItem.text")); // NOI18N
        trailerPlayerMenu.add(trailerMediaPlayer480RadioButtonMenuItem);

        trailerMediaPlayer360RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_MASK));
        trailerMediaPlayer360RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer360RadioButtonMenuItem.text")); // NOI18N
        trailerPlayerMenu.add(trailerMediaPlayer360RadioButtonMenuItem);

        trailerMediaPlayer240RadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_MASK));
        trailerMediaPlayer240RadioButtonMenuItem.setText(bundle.getString("GUI.trailerMediaPlayer240RadioButtonMenuItem.text")); // NOI18N
        trailerPlayerMenu.add(trailerMediaPlayer240RadioButtonMenuItem);

        trailerWebBrowserPlayerRadioButtonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
        trailerWebBrowserPlayerRadioButtonMenuItem.setText(bundle.getString("GUI.trailerWebBrowserPlayerRadioButtonMenuItem.text")); // NOI18N
        trailerPlayerMenu.add(trailerWebBrowserPlayerRadioButtonMenuItem);

        searchMenu.add(trailerPlayerMenu);

        menuBar.add(searchMenu);

        playlistMenu.setText(bundle.getString("GUI.playlistMenu.text")); // NOI18N

        playlistMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
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

        playlistAutoOpenCheckBoxMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        playlistAutoOpenCheckBoxMenuItem.setSelected(true);
        playlistAutoOpenCheckBoxMenuItem.setText(bundle.getString("GUI.playlistAutoOpenCheckBoxMenuItem.text")); // NOI18N
        playlistMenu.add(playlistAutoOpenCheckBoxMenuItem);

        playlistPlayWithDefaultAppCheckBoxMenuItem.setText(bundle.getString("GUI.playlistPlayWithDefaultAppCheckBoxMenuItem.text")); // NOI18N
        playlistMenu.add(playlistPlayWithDefaultAppCheckBoxMenuItem);

        playlistShowNonVideoItemsCheckBoxMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
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

        downloadSizeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        downloadSizeMenuItem.setText(bundle.getString("GUI.downloadSizeMenuItem.text")); // NOI18N
        downloadSizeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadSizeMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(downloadSizeMenuItem);
        downloadMenu.add(downloadMenuSeparator1);

        fileExtensionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
        fileExtensionsMenuItem.setText(bundle.getString("GUI.fileExtensionsMenuItem.text")); // NOI18N
        fileExtensionsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fileExtensionsMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(fileExtensionsMenuItem);
        downloadMenu.add(downloadMenuSeparator2);

        safetyCheckBoxMenuItem.setSelected(true);
        safetyCheckBoxMenuItem.setText(bundle.getString("GUI.safetyCheckBoxMenuItem.text")); // NOI18N
        safetyCheckBoxMenuItem.setToolTipText(bundle.getString("GUI.safetyCheckBoxMenuItem.toolTipText")); // NOI18N
        downloadMenu.add(safetyCheckBoxMenuItem);

        peerBlockNotificationCheckBoxMenuItem.setSelected(true);
        peerBlockNotificationCheckBoxMenuItem.setText(bundle.getString("GUI.peerBlockNotificationCheckBoxMenuItem.text")); // NOI18N
        downloadMenu.add(peerBlockNotificationCheckBoxMenuItem);
        downloadMenu.add(downloadMenuSeparator3);

        portMenuItem.setText(bundle.getString("GUI.portMenuItem.text")); // NOI18N
        portMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                portMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(portMenuItem);
        downloadMenu.add(downloadMenuSeparator4);

        downloaderMenu.setText(bundle.getString("GUI.downloaderMenu.text")); // NOI18N

        playlistDownloaderRadioButtonMenuItem.setSelected(true);
        playlistDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.playlistDownloaderRadioButtonMenuItem.text")); // NOI18N
        downloaderMenu.add(playlistDownloaderRadioButtonMenuItem);

        webBrowserAppDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.webBrowserAppDownloaderRadioButtonMenuItem.text")); // NOI18N
        downloaderMenu.add(webBrowserAppDownloaderRadioButtonMenuItem);

        webBrowserAltAppDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.webBrowserAltAppDownloaderRadioButtonMenuItem.text")); // NOI18N
        downloaderMenu.add(webBrowserAltAppDownloaderRadioButtonMenuItem);

        defaultApplicationDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.defaultApplicationDownloaderRadioButtonMenuItem.text")); // NOI18N
        downloaderMenu.add(defaultApplicationDownloaderRadioButtonMenuItem);

        noDownloaderRadioButtonMenuItem.setText(bundle.getString("GUI.noDownloaderRadioButtonMenuItem.text")); // NOI18N
        downloaderMenu.add(noDownloaderRadioButtonMenuItem);

        downloadMenu.add(downloaderMenu);

        menuBar.add(downloadMenu);

        helpMenu.setText(bundle.getString("GUI.helpMenu.text")); // NOI18N

        faqMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
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
        splashScreen.progress();

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(resultsScrollPane, Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(readSummaryButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(watchTrailerButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(downloadLink1Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(downloadLink2Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(exitBackupModeButton)
                        .addGap(2, 2, 2)
                        .addComponent(hqVideoTypeCheckBox)
                        .addGap(0, 0, 0)
                        .addComponent(dvdCheckBox)
                        .addGap(0, 0, 0)
                        .addComponent(hd720CheckBox)
                        .addGap(0, 0, 0)
                        .addComponent(hd1080CheckBox)
                        .addGap(0, 162, Short.MAX_VALUE)
                        .addComponent(loadMoreResultsButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(popularMoviesButton)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addComponent(popularTVShowsButton)
                                .addGap(18, 18, 18)
                                .addComponent(findTextField))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(titleLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(titleTextField))
                            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(typeLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(typeComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(ratingLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(ratingComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(releasedLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(startDateChooser, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(7, 7, 7)
                                .addComponent(releasedToLabel)
                                .addGap(6, 6, 6)
                                .addComponent(endDateChooser, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addComponent(genreLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(connectionIssueButton))
                            .addComponent(genreScrollPane))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(searchButton, Alignment.TRAILING)
                            .addComponent(loadingLabel, Alignment.TRAILING))))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addComponent(statusBarTextField)
                .addGap(0, 0, 0)
                .addComponent(searchProgressTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {downloadLink1Button, downloadLink2Button, readSummaryButton, watchTrailerButton});

        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(loadingLabel)
                    .addComponent(connectionIssueButton)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(popularMoviesButton)
                        .addComponent(popularTVShowsButton)
                        .addComponent(findTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
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
                    .addComponent(searchButton)
                    .addComponent(genreScrollPane, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(resultsScrollPane, GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(exitBackupModeButton, Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(readSummaryButton)
                        .addComponent(watchTrailerButton)
                        .addComponent(downloadLink1Button)
                        .addComponent(downloadLink2Button)
                        .addComponent(hqVideoTypeCheckBox)
                        .addComponent(dvdCheckBox)
                        .addComponent(hd720CheckBox)
                        .addComponent(hd1080CheckBox)
                        .addComponent(loadMoreResultsButton)))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(statusBarTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchProgressTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
        );

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {endDateChooser, ratingComboBox, startDateChooser, titleTextField, typeComboBox});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {genreLabel, ratingLabel, releasedLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {downloadLink1Button, downloadLink2Button, findTextField, readSummaryButton, watchTrailerButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {searchProgressTextField, statusBarTextField});

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

        StringBuilder downloadIDs = new StringBuilder(96);
        for (Long downloadID : bannedDownloadIDs) {
            downloadIDs.append(downloadID).append(Constant.STD_NEWLINE);
        }
        preferences.put(Constant.BANNED_DOWNLOAD_IDS, downloadIDs.toString().trim());
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
        synchronized (playlistRestorationLock) {
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
        }
    }

    private boolean stopSearch(AbstractButton button) {
        if (UI.isStop(button)) {
            button.setEnabled(false);
            workerListener.searchStopped(isRegularSearcher);
            return true;
        }
        return false;
    }

    private boolean stopTorrentSearch(AbstractButton button) {
        if (UI.isStop(button)) {
            button.setEnabled(false);
            workerListener.torrentSearchStopped();
            return true;
        }
        return false;
    }

    private boolean stopSubtitleSearch(AbstractButton button) {
        if (UI.isStop(button)) {
            button.setEnabled(false);
            workerListener.subtitleSearchStopped();
            return true;
        }
        return false;
    }

    void searchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
        if (stopSearch(searchButton)) {
            return;
        }

        findControl.hide();

        int numResultsPerSearch = Integer.parseInt((String) regularResultsPerSearchComboBox.getSelectedItem());
        isTVShowSearch = Constant.TV_SHOW.equals(typeComboBox.getSelectedItem());
        Calendar startDate = ((DateChooser) startDateChooser).getTime(), endDate = ((DateChooser) endDateChooser).getTime();

        String title = titleTextField.getText().trim(), minRating = (String) ratingComboBox.getSelectedItem();
        String[] genres = UI.selectAnyIfNoSelectionAndCopy(genreList), languages = UI.selectAnyIfNoSelectionAndCopy(languageList), countries
                = UI.selectAnyIfNoSelectionAndCopy(countryList);

        isRegularSearcher = true;
        workerListener.regularSearchStarted(numResultsPerSearch, isTVShowSearch, startDate, endDate, title, genres, languages, countries, minRating);
    }//GEN-LAST:event_searchButtonActionPerformed

    void yesButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_yesButtonActionPerformed
        proceedWithDownload = true;
        safetyDialog.setVisible(false);
    }//GEN-LAST:event_yesButtonActionPerformed

    void noButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_noButtonActionPerformed
        safetyDialog.setVisible(false);
    }//GEN-LAST:event_noButtonActionPerformed

    void loadMoreResultsButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_loadMoreResultsButtonActionPerformed
        workerListener.loadMoreSearchResults(isRegularSearcher);
    }//GEN-LAST:event_loadMoreResultsButtonActionPerformed

    void faqMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_faqMenuItemActionPerformed
        faqEditorPane.setSelectionStart(0);
        faqEditorPane.setSelectionEnd(0);
        UI.setVisible(faqFrame);
    }//GEN-LAST:event_faqMenuItemActionPerformed

    void aboutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        aboutEditorPane.setSelectionStart(0);
        aboutEditorPane.setSelectionEnd(0);
        UI.setVisible(aboutDialog);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    void exitMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_exitMenuItemActionPerformed

    void printMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_printMenuItemActionPerformed
        if (resultsSyncTable.getRowCount() == 0) {
            showMsg(Str.str("noPrintResults"), Constant.INFO_MSG);
            return;
        }
        printMenuItem.setEnabled(false);
        printMenuItem.setText(Str.str("printing"));
        (new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() {
                Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR), defaultCursor = Cursor.getDefaultCursor();
                rootPane.setCursor(waitCursor);
                resultsSyncTable.table.setCursor(waitCursor);
                try {
                    resultsSyncTable.table.print(PrintMode.FIT_WIDTH);
                } catch (Exception e) {
                    showException(e);
                }
                rootPane.setCursor(defaultCursor);
                resultsSyncTable.table.setCursor(defaultCursor);
                printMenuItem.setText(Str.str("GUI.printMenuItem.text"));
                printMenuItem.setEnabled(true);
                return null;
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
        maximize();
    }//GEN-LAST:event_resetWindowMenuItemActionPerformed

    void timeoutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_timeoutMenuItemActionPerformed
        resultsToBackground(true);
        UI.setVisible(timeoutDialog);
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
        resultsToBackground(true);
        UI.setVisible(resultsPerSearchDialog);
    }//GEN-LAST:event_resultsPerSearchMenuItemActionPerformed

    void genreListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_genreListValueChanged
        UI.updateList(genreList);
    }//GEN-LAST:event_genreListValueChanged

    void faqEditorPaneHyperlinkUpdate(HyperlinkEvent evt) {
        try {
            hyperlinkHandler(evt);
        } catch (Exception e) {
            showException(e);
        }
    }

    private static void hyperlinkHandler(HyperlinkEvent evt) throws IOException {
        if (evt.getEventType().equals(EventType.ACTIVATED)) {
            String url = evt.getURL().toString();
            if (url.startsWith("mailto:")) {
                Connection.email(url);
            } else {
                Connection.browse(url);
            }
        }
    }

    void popularMoviesButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularMoviesButtonActionPerformed
        if (!stopSearch(popularMoviesButton)) {
            if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
                showFeed(false);
            } else {
                doPopularVideosSearch(false);
            }
        }
    }//GEN-LAST:event_popularMoviesButtonActionPerformed

    private void doPopularVideosSearch(boolean isPopularTVShows) {
        findControl.hide();
        isTVShowSearch = isPopularTVShows;
        isRegularSearcher = false;
        int numResultsPerSearch = Integer.parseInt((String) (isPopularTVShows ? popularTVShowsResultsPerSearchComboBox.getSelectedItem()
                : popularMoviesResultsPerSearchComboBox.getSelectedItem()));
        String[] languages = UI.selectAnyIfNoSelectionAndCopy(languageList), countries = UI.selectAnyIfNoSelectionAndCopy(countryList);
        workerListener.popularSearchStarted(numResultsPerSearch, isPopularTVShows, languages, countries, false, true);
    }

    void downloadSizeMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadSizeMenuItemActionPerformed
        resultsToBackground(true);
        updateDownloadSizeComboBoxes();
        UI.setVisible(downloadSizeDialog);
    }//GEN-LAST:event_downloadSizeMenuItemActionPerformed

    void maxDownloadSizeComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_maxDownloadSizeComboBoxActionPerformed
        updateDownloadSizeComboBoxes();
    }//GEN-LAST:event_maxDownloadSizeComboBoxActionPerformed

    void popularTVShowsButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularTVShowsButtonActionPerformed
        if (!stopSearch(popularTVShowsButton)) {
            doPopularVideosSearch(true);
        }
    }//GEN-LAST:event_popularTVShowsButtonActionPerformed

    void fileExtensionsMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_fileExtensionsMenuItemActionPerformed
        resultsToBackground(true);
        customExtensionTextField.requestFocusInWindow();
        UI.setVisible(extensionsDialog);
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

    void summaryCloseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_summaryCloseButtonActionPerformed
        summaryDialog.setVisible(false);
    }//GEN-LAST:event_summaryCloseButtonActionPerformed

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
        resultsToBackground(true);
        UI.setVisible(profileDialog);
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

        Component[] components = {playlistRemoveButton, playlistRemoveMenuItem, playlistCopyMenuItem, playlistMoveDownButton, playlistMoveDownMenuItem,
            playlistMoveUpButton, playlistMoveUpMenuItem};
        UI.enable(false, components);
        int[] selectedRows;
        if (evt.getFirstIndex() >= 0 && (selectedRows = playlistSyncTable.getSelectedRows()).length >= 1) {
            UI.enable(true, selectedRows.length == 1 ? (UI.getUnfilteredRowCount(playlistSyncTable) == 1 ? new Component[]{playlistRemoveButton,
                playlistRemoveMenuItem, playlistCopyMenuItem} : components) : new Component[]{playlistRemoveButton, playlistRemoveMenuItem, playlistCopyMenuItem});
        }
        refreshPlaylistControls();
    }

    void updateMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_updateMenuItemActionPerformed
        workerListener.updateStarted(false);
    }//GEN-LAST:event_updateMenuItemActionPerformed

    void safetyEditorPaneHyperlinkUpdate(HyperlinkEvent evt) {//GEN-FIRST:event_safetyEditorPaneHyperlinkUpdate
        if (evt.getEventType().equals(EventType.ACTIVATED)) {
            Window alwaysOnTopFocus = resultsToBackground();
            commentsTextPane.setText(workerListener.getSafetyComments());
            commentsTextPane.setSelectionStart(0);
            commentsTextPane.setSelectionEnd(0);
            commentsDialog.setVisible(true);
            resultsToForeground(alwaysOnTopFocus);
        }
    }//GEN-LAST:event_safetyEditorPaneHyperlinkUpdate

    private SelectedTableRow selectedRow() {
        findControl.hide();
        SelectedTableRow row = new SelectedTableRow();
        JViewport viewport = (JViewport) resultsSyncTable.table.getParent();
        viewport.scrollRectToVisible(rectangle(viewport, resultsSyncTable.getCellRect(row.VIEW_VAL, 0, true)));
        return row;
    }

    private Rectangle rectangle(JViewport viewport, Rectangle cellRect) {
        Point viewPosition = viewport.getViewPosition();
        cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);
        return cellRect;
    }

    void readSummaryButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_readSummaryButtonActionPerformed
        if (UI.isStop(readSummaryButton)) {
            readSummaryButton.setEnabled(false);
            workerListener.summarySearchStopped();
            return;
        }

        readSummaryActionPerformed(selectedRow(), null);
    }//GEN-LAST:event_readSummaryButtonActionPerformed

    void readSummaryActionPerformed(SelectedTableRow row, VideoStrExportListener strExportListener) {
        workerListener.summarySearchStarted(row.VAL, row.video, strExportListener);
    }

    void watchTrailerButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchTrailerButtonActionPerformed
        if (UI.isStop(watchTrailerButton)) {
            watchTrailerButton.setEnabled(false);
            workerListener.trailerSearchStopped();
            return;
        }

        watchTrailerActionPerformed(selectedRow(), null);
    }//GEN-LAST:event_watchTrailerButtonActionPerformed

    private void watchTrailerActionPerformed(SelectedTableRow row, VideoStrExportListener strExportListener) {
        if (row.video.IS_TV_SHOW) {
            downloadLinkEpisodes.add(row.VAL);
            subtitleEpisodes.add(row.VAL);
            if (!trailerEpisodes.add(row.VAL)) {
                row.video.season = "";
                row.video.episode = "";
            }
        }
        workerListener.trailerSearchStarted(row.VAL, row.video, strExportListener);
    }

    private void downloadLinkActionPerformed(ContentType downloadContentType, SelectedTableRow row, VideoStrExportListener strExportListener) {
        if (row.video.IS_TV_SHOW && !downloadLinkEpisodes.add(row.VAL)) {
            row.video.season = "";
            row.video.episode = "";
        }
        workerListener.torrentSearchStarted(Connection.downloadLinkInfoFail() ? ContentType.DOWNLOAD3 : downloadContentType, row.VAL, row.video,
                strExportListener);
    }

    void downloadLink1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink1ButtonActionPerformed
        if (!stopTorrentSearch(downloadLink1Button)) {
            downloadLinkActionPerformed(ContentType.DOWNLOAD1, selectedRow(), null);
        }
    }//GEN-LAST:event_downloadLink1ButtonActionPerformed

    void downloadLink2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink2ButtonActionPerformed
        if (!stopTorrentSearch(downloadLink2Button)) {
            downloadLinkActionPerformed(ContentType.DOWNLOAD2, selectedRow(), null);
        }
    }//GEN-LAST:event_downloadLink2ButtonActionPerformed

    void languageCountryMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_languageCountryMenuItemActionPerformed
        resultsToBackground(true);
        UI.setVisible(languageCountryDialog);
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
        } else if (popupTextComponent == titleTextField || popupTextComponent == startDateTextField || popupTextComponent == endDateTextField) {
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
        } else {
            if (copyMenuItem.isEnabled()) {
                copyMenuItem.setEnabled(false);
            }
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
                || popupTextComponent == titleTextField || popupTextComponent == startDateTextField || popupTextComponent == endDateTextField);

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
        resultsToBackground(true);
        UI.setVisible(proxyDialog);
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
        proxyDialog.setVisible(false);
        addProxiesTextArea.setText("");
        UI.setVisible(addProxiesDialog);
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

        proxyDialog.setVisible(false);
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

        UI.setVisible(removeProxiesDialog);
    }//GEN-LAST:event_proxyRemoveButtonActionPerformed

    private void restoreProxyDialog(boolean addButtonPressed) {
        if (addButtonPressed) {
            addProxiesDialog.setVisible(false);
            addProxiesAddButton.setEnabled(true);
        } else {
            removeProxiesDialog.setVisible(false);
            removeProxiesRemoveButton.setEnabled(true);
        }
        enableProxyButtons(true);
        UI.setVisible(proxyDialog);
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
        proxyDialog.setVisible(false);

        proxyFileChooser.setFileFilter(proxyListFileFilter);
        proxyFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        if (!proxyImportFile.isEmpty()) {
            proxyFileChooser.setSelectedFile(new File(proxyImportFile));
        }
        if (proxyFileChooser.showOpenDialog(showing()) == JFileChooser.APPROVE_OPTION) {
            try {
                File proxyFile = proxyFileChooser.getSelectedFile();
                proxyImportFile = proxyFile.getPath();
                addProxies(IO.read(proxyFile), false);
            } catch (Exception e) {
                showException(e);
            }
        }

        enableProxyButtons(true);
        UI.setVisible(proxyDialog);
    }//GEN-LAST:event_proxyImportButtonActionPerformed

    void proxyExportButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyExportButtonActionPerformed
        int numProxies = exportProxies("Export");
        if (numProxies == 1) {
            return;
        }

        proxyFileChooser.setFileFilter(proxyListFileFilter);
        proxyFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        proxyFileChooser.setSelectedFile(new File(proxyExportFile.isEmpty() ? Constant.PROXIES : proxyExportFile));
        if (proxyFileChooser.showSaveDialog(showing()) == JFileChooser.APPROVE_OPTION) {
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
        UI.setVisible(proxyDialog);
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
            settings.loadSettings(profile == 0 ? Constant.PROGRAM_DIR + Constant.DEFAULT_SETTINGS : Constant.APP_DIR + Constant.PROFILE + profile + Constant.TXT);
            playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(null);
            maximize();
            timedMsg(Str.str("settingsRestored", profileComboBox.getItemAt(profile)));
        } else {
            showMsg(Str.str("setProfileBeforeUse"), Constant.ERROR_MSG);
        }
    }

    public void maximize() {
        if (UI.isMaxSize(this, getSize())) {
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
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
        profileDialog.setVisible(false);
        profileNameChangeTextField.setText("");
        profileNameChangeTextField.requestFocusInWindow();
        UI.setVisible(profileNameChangeDialog);
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

        profileNameChangeDialog.setVisible(false);
        UI.setVisible(profileDialog);
    }//GEN-LAST:event_profileNameChangeOKButtonActionPerformed

    void profileNameChangeCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileNameChangeCancelButtonActionPerformed
        profileNameChangeDialog.setVisible(false);
        UI.setVisible(profileDialog);
    }//GEN-LAST:event_profileNameChangeCancelButtonActionPerformed

    void profileNameChangeDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_profileNameChangeDialogWindowClosing
        profileNameChangeDialog.setVisible(false);
        UI.setVisible(profileDialog);
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
        connectionIssueButton.setIcon(noWarningIcon);
        connectionIssueButton.setToolTipText(null);
        connectionIssueButton.setBorderPainted(false);
        connectionIssueButton.setEnabled(false);
        if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
            msgDialogWindowClosing(null);
        } else {
            UI.setVisible(msgDialog);
        }
    }//GEN-LAST:event_connectionIssueButtonActionPerformed

    void findSubtitleMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_findSubtitleMenuItemActionPerformed
        findSubtitleActionPerformed(selectedRow(), null);
    }//GEN-LAST:event_findSubtitleMenuItemActionPerformed

    void findSubtitleActionPerformed(SelectedTableRow row, VideoStrExportListener strExportListener) {
        subtitleVideo = new Video(row.video.ID, Regex.clean(row.video.title), row.video.year, row.video.IS_TV_SHOW, row.video.IS_TV_SHOW_AND_MOVIE);
        subtitleStrExportListener = strExportListener;
        isTVShowSubtitle = isTVShowSearch;

        if (subtitleFormat != null) {
            movieSubtitleFormatComboBox.setSelectedItem(subtitleFormat);
            tvSubtitleFormatComboBox.setSelectedItem(subtitleFormat);
            subtitleFormat = null;
        }

        if (isTVShowSubtitle) {
            if (subtitleEpisodes.add(row.VAL)) {
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
            UI.setVisible(tvSubtitleDialog);
        } else {
            movieSubtitleDownloadMatch1Button.requestFocusInWindow();
            UI.setVisible(movieSubtitleDialog);
        }
    }

    private void resultsTableMouseClicked(MouseEvent evt) {//GEN-FIRST:event_resultsTableMouseClicked
        if (evt.getClickCount() == 2 && resultsSyncTable.getSelectedRows().length == 1) {
            readSummaryButtonActionPerformed(null);
        }
    }//GEN-LAST:event_resultsTableMouseClicked

    private void portMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_portMenuItemActionPerformed
        resultsToBackground(true);
        if (!viewedPortBefore) {
            portRandomizeCheckBox.setSelected(false);
        }
        viewedPortBefore = true;
        UI.setVisible(portDialog);
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
        if (!stopSubtitleSearch(tvSubtitleDownloadMatch1Button)) {
            startSubtitleSearch(tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox, tvSubtitleEpisodeComboBox, true);
        }
    }//GEN-LAST:event_tvSubtitleDownloadMatch1ButtonActionPerformed

    private void movieSubtitleDownloadMatch1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleDownloadMatch1ButtonActionPerformed
        if (!stopSubtitleSearch(movieSubtitleDownloadMatch1Button)) {
            startSubtitleSearch(movieSubtitleFormatComboBox, movieSubtitleLanguageComboBox, null, null, true);
        }
    }//GEN-LAST:event_movieSubtitleDownloadMatch1ButtonActionPerformed

    private void findMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_findMenuItemActionPerformed
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

    private void summaryCloseButtonKeyPressed(KeyEvent evt) {//GEN-FIRST:event_summaryCloseButtonKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            summaryCloseButtonActionPerformed(null);
        }
    }//GEN-LAST:event_summaryCloseButtonKeyPressed

    private void resultsTableKeyPressed(KeyEvent evt) {//GEN-FIRST:event_resultsTableKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER && resultsSyncTable.getSelectedRows().length == 1) {
            readSummaryButtonActionPerformed(null);
        }
    }//GEN-LAST:event_resultsTableKeyPressed

    private void summaryTextToSpeechButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_summaryTextToSpeechButtonActionPerformed
        if (UI.isStop(summaryTextToSpeechButton)) {
            summaryTextToSpeechButton.setEnabled(false);
            workerListener.summaryReadStopped();
            return;
        }

        workerListener.summaryReadStarted(summaryEditorPane.getText());
    }//GEN-LAST:event_summaryTextToSpeechButtonActionPerformed

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
        if (UI.trayIcon(playlistFrame) != null) {
            return;
        }
        if (!playlistFrame.isShowing()) {
            System.exit(0);
        }

        boolean isPlaylistActive = false;
        synchronized (playlistSyncTable.lock) {
            for (int row = playlistSyncTable.tableModel.getRowCount() - 1; row > -1; row--) {
                if (((PlaylistItem) playlistSyncTable.tableModel.getValueAt(row, playlistItemCol)).isActive()) {
                    isPlaylistActive = true;
                    break;
                }
            }
        }

        if (!isPlaylistActive) {
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

    private void hqVideoTypeCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_hqVideoTypeCheckBoxActionPerformed
        subtitleFormat = UI.groupButtonSelectionChanged(hqVideoTypeCheckBox, dvdCheckBox, hd720CheckBox, hd1080CheckBox);
    }//GEN-LAST:event_hqVideoTypeCheckBoxActionPerformed

    private void hd720CheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_hd720CheckBoxActionPerformed
        subtitleFormat = UI.groupButtonSelectionChanged(hd720CheckBox, hqVideoTypeCheckBox, dvdCheckBox, hd1080CheckBox);
    }//GEN-LAST:event_hd720CheckBoxActionPerformed

    private void dvdCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_dvdCheckBoxActionPerformed
        subtitleFormat = UI.groupButtonSelectionChanged(dvdCheckBox, hqVideoTypeCheckBox, hd720CheckBox, hd1080CheckBox);
    }//GEN-LAST:event_dvdCheckBoxActionPerformed

    private void hd1080CheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_hd1080CheckBoxActionPerformed
        subtitleFormat = UI.groupButtonSelectionChanged(hd1080CheckBox, hqVideoTypeCheckBox, dvdCheckBox, hd720CheckBox);
    }//GEN-LAST:event_hd1080CheckBoxActionPerformed

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
        if (!stopSubtitleSearch(tvSubtitleDownloadMatch2Button)) {
            startSubtitleSearch(tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox, tvSubtitleEpisodeComboBox, false);
        }
    }//GEN-LAST:event_tvSubtitleDownloadMatch2ButtonActionPerformed

    private void movieSubtitleDownloadMatch2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleDownloadMatch2ButtonActionPerformed
        if (!stopSubtitleSearch(movieSubtitleDownloadMatch2Button)) {
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

    private void viewNewHighQualityMoviesMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_viewNewHighQualityMoviesMenuItemActionPerformed
        showFeed(false);
    }//GEN-LAST:event_viewNewHighQualityMoviesMenuItemActionPerformed

    private void copyFullTitleAndYearMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyFullTitleAndYearMenuItemActionPerformed
        SelectedTableRow row = selectedRow();
        readSummaryActionPerformed(row, row.strExportListener(false));
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
        readSummaryActionPerformed(row, strExportListener);
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
                resultsToBackground(true);
                UI.setVisible(portDialog);
            } else {
                portTextField.setText(null);
                portRandomizeCheckBox.setSelected(true);
            }
        } else {
            workerListener.portChanged(portNum);
        }
    }//GEN-LAST:event_portDialogComponentHidden

    private void playlistMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistMenuItemActionPerformed
        UI.show(playlistFrame);
        if (!isPlaylistRestored.get()) {
            (new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() {
                    restorePlaylist(true);
                    return null;
                }
            }).execute();
        }
    }//GEN-LAST:event_playlistMenuItemActionPerformed

    PlaylistItem selectedPlaylistItem() {
        synchronized (playlistSyncTable.lock) {
            int[] rows = playlistSyncTable.table.getSelectedRows();
            if (rows.length != 1) {
                return null;
            }
            return (PlaylistItem) playlistSyncTable.tableModel.getValueAt(playlistSyncTable.table.convertRowIndexToModel(rows[0]), playlistItemCol);
        }
    }

    private void playlistPlayButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistPlayButtonActionPerformed
        playlistFindControl.hide();
        boolean force = forcePlay;
        forcePlay = false;
        PlaylistItem playlistItem = selectedPlaylistItem();
        if (playlistItem != null) {
            if (playlistItem.canPlay()) {
                playlistItem.play(force);
            } else {
                playlistItem.stop();
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
        playlistFindControl.hide();
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
        playlistFindControl.hide();
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

            List<?> rows = playlistSyncTable.tableModel.getDataVector();
            for (int i = modelRows.length - 1; i > -1; i--) {
                ((PlaylistItem) playlistSyncTable.tableModel.getValueAt(modelRows[i], playlistItemCol)).stop();
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

    private void playlistCopyMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistCopyMenuItemActionPerformed
        if (playlistSyncTable.getSelectedRowCount() != 0) {
            playlistTableCopyListener.actionPerformed(new ActionEvent(playlistSyncTable, 0, Constant.COPY));
        }
    }//GEN-LAST:event_playlistCopyMenuItemActionPerformed

    private void playlistOpenMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistOpenMenuItemActionPerformed
        playlistFindControl.hide();
        PlaylistItem playlistItem = selectedPlaylistItem();
        if (playlistItem != null) {
            playlistItem.open();
        }
    }//GEN-LAST:event_playlistOpenMenuItemActionPerformed

    private void playlistFrameWindowClosing(WindowEvent evt) {//GEN-FIRST:event_playlistFrameWindowClosing
        playlistFrame.setVisible(false);
        if (UI.trayIcon(this) == null && !isVisible()) {
            System.exit(0);
        }
    }//GEN-LAST:event_playlistFrameWindowClosing

    private void activationButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_activationButtonActionPerformed
        workerListener.license(activationTextField.getText().trim());
    }//GEN-LAST:event_activationButtonActionPerformed

    private void activationUpgradeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_activationUpgradeButtonActionPerformed
        workerListener.license(null);
    }//GEN-LAST:event_activationUpgradeButtonActionPerformed

    private void activationDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_activationDialogWindowClosing
        if (!isVisible()) {
            setVisible(true);
        }
        downloadMenu.doClick();
        downloaderMenu.doClick();
        webBrowserAppDownloaderRadioButtonMenuItem.setArmed(true);
    }//GEN-LAST:event_activationDialogWindowClosing

    private void playlistPlayButtonMousePressed(MouseEvent evt) {//GEN-FIRST:event_playlistPlayButtonMousePressed
        if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
            forcePlay = true;
        }
    }//GEN-LAST:event_playlistPlayButtonMousePressed

    private void playlistPlayMenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_playlistPlayMenuItemMousePressed
        playlistPlayButtonMousePressed(evt);
    }//GEN-LAST:event_playlistPlayMenuItemMousePressed

    private void playlistTableKeyPressed(KeyEvent evt) {//GEN-FIRST:event_playlistTableKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_DELETE) {
            playlistRemoveButtonActionPerformed(null);
        } else if (key == KeyEvent.VK_ENTER) {
            playlistPlayButtonActionPerformed(null);
        } else {
            playlistKeyPressed(evt);
        }
    }//GEN-LAST:event_playlistTableKeyPressed

    private void playlistPlayButtonKeyPressed(KeyEvent evt) {//GEN-FIRST:event_playlistPlayButtonKeyPressed
        playlistKeyPressed(evt);
    }//GEN-LAST:event_playlistPlayButtonKeyPressed

    private void playlistMoveUpButtonKeyPressed(KeyEvent evt) {//GEN-FIRST:event_playlistMoveUpButtonKeyPressed
        playlistKeyPressed(evt);
    }//GEN-LAST:event_playlistMoveUpButtonKeyPressed

    private void playlistMoveDownButtonKeyPressed(KeyEvent evt) {//GEN-FIRST:event_playlistMoveDownButtonKeyPressed
        playlistKeyPressed(evt);
    }//GEN-LAST:event_playlistMoveDownButtonKeyPressed

    private void playlistRemoveButtonKeyPressed(KeyEvent evt) {//GEN-FIRST:event_playlistRemoveButtonKeyPressed
        playlistKeyPressed(evt);
    }//GEN-LAST:event_playlistRemoveButtonKeyPressed

    private void playlistTableMouseClicked(MouseEvent evt) {//GEN-FIRST:event_playlistTableMouseClicked
        if (evt.getClickCount() == 2) {
            playlistPlayButtonActionPerformed(null);
        }
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
        findControl.hide();
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
        boolean enable = true;
        if (!downloadLink2Button.isEnabled() && workerListener.isTorrentSearchDone() && resultsSyncTable.getSelectedRows().length == 1) {
            UI.enable(enable, downloadLink2Button, downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem);
        }
        enableVideoFormats(enable);
    }//GEN-LAST:event_exitBackupModeButtonActionPerformed

    private void playlistReloadGroupMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistReloadGroupMenuItemActionPerformed
        playlistFindControl.hide();
        PlaylistItem playlistItem = selectedPlaylistItem();
        if (playlistItem != null) {
            workerListener.reloadGroup(playlistItem);
        }
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
        playlistFindControl.hide();
        PlaylistItem playlistItem = selectedPlaylistItem();
        if (playlistItem == null) {
            return;
        }

        Long downloadID = playlistItem.groupDownloadID();
        String textKey = "banGroup";
        if (bannedDownloadIDs.add(downloadID)) {
            textKey = "un" + textKey;
        } else {
            bannedDownloadIDs.remove(downloadID);
        }
        playlistBanGroupMenuItem.setText(Str.str(textKey));
    }//GEN-LAST:event_playlistBanGroupMenuItemActionPerformed

    private void playlistKeyPressed(KeyEvent evt) {
        if (!evt.isControlDown()) {
            return;
        }

        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_O) {
            playlistAutoOpenCheckBoxMenuItem.setSelected(!playlistAutoOpenCheckBoxMenuItem.isSelected());
        } else if (key == KeyEvent.VK_N) {
            playlistShowNonVideoItemsCheckBoxMenuItem.setSelected(!playlistShowNonVideoItemsCheckBoxMenuItem.isSelected());
            playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(null);
        } else if (key == KeyEvent.VK_F) {
            playlistFindControl.show();
        } else if (key == KeyEvent.VK_W) {
            UI.show(this);
        }
    }

    private void exportSummaryLink(SelectedTableRow row, VideoStrExportListener strExportListener) {
        strExportListener.export(ContentType.TITLE, Str.get(519) + row.video.ID, false, this);
    }

    private void exportPosterImage(SelectedTableRow row, VideoStrExportListener strExportListener) {
        strExportListener.export(ContentType.IMAGE, row.video.imagePath.startsWith(Constant.NO_IMAGE) ? Constant.PROGRAM_DIR + "noPosterBig.jpg"
                : row.video.imagePath, false, this);
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

    private JTextArea getTextArea(String msg) {
        JTextArea textArea = new JTextArea();
        textArea.setSize(300, 200);
        JOptionPane tempOptionPane = new JOptionPane();
        textArea.setForeground(tempOptionPane.getForeground());
        textArea.setBackground(tempOptionPane.getBackground());
        textArea.setFont(tempOptionPane.getFont());
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(msg);
        textArea.addMouseListener(textComponentPopupListener);
        return textArea;
    }

    private JEditorPane getEditorPane(String msg) {
        JEditorPane editorPane = new JEditorPane("text/html", msg);
        editorPane.setOpaque(false);
        editorPane.setEditable(false);
        editorPane.setMaximumSize(null);
        editorPane.setMinimumSize(null);
        UI.addHyperlinkListener(editorPane, new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent evt) {
                try {
                    hyperlinkHandler(evt);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    Window alwaysOnTopFocus = resultsToBackground();
                    JOptionPane.showMessageDialog(showing(), getTextArea(ExceptionUtil.toString(e)), Constant.APP_TITLE, Constant.ERROR_MSG);
                    resultsToForeground(alwaysOnTopFocus);
                    IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
                }
            }
        });
        editorPane.addMouseListener(textComponentPopupListener);
        return editorPane;
    }

    int showOptionDialog(Object msg, String title, int type, boolean confirm) {
        return showOptionDialog(showing(), msg, title, type, confirm);
    }

    private int showOptionDialog(Component parent, Object msg, String title, int type, boolean confirm) {
        Window alwaysOnTopFocus = resultsToBackground();
        int result;
        if (confirm) {
            result = JOptionPane.showConfirmDialog(parent, msg, title, type);
        } else {
            JOptionPane.showMessageDialog(parent, msg, title, type);
            result = -1;
        }
        resultsToForeground(alwaysOnTopFocus);
        return result;
    }

    void showException(Exception e) {
        if (Debug.DEBUG) {
            Debug.print(e);
        }
        if (e.getClass().equals(ConnectionException.class)) {
            return;
        }

        showMsg(ExceptionUtil.toString(e), Constant.ERROR_MSG);
        IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
    }

    private void showOptionalMsg(String msg, JMenuItem menuItem) {
        synchronized (optionDialogLock) {
            optionalMsgTextArea.setSize(300, 200);
            optionalMsgTextArea.setText(msg);
            showOptionDialog(optionalMsgPanel, Constant.APP_TITLE, Constant.INFO_MSG, false);
            updateOptionalMsgCheckBox(menuItem);
        }
    }

    private void showMsg(String msg, int msgType) {
        synchronized (optionDialogLock) {
            showOptionDialog(getTextArea(msg), Constant.APP_TITLE, msgType, false);
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
            if (!connectionIssueButton.isEnabled() && !msgDialog.isVisible()) {
                connectionIssueButton.setEnabled(true);
                connectionIssueButton.setBorderPainted(true);
                connectionIssueButton.setIcon(warningIcon);
                connectionIssueButton.setToolTipText(Str.ctrlStr("GUI.connectionIssueButton.toolTipText2"));
            }
        }
    }

    private int showOptionalConfirm(Component parent, String msg, JMenuItem menuItem) {
        synchronized (optionDialogLock) {
            optionalMsgTextArea.setSize(300, 200);
            optionalMsgTextArea.setText(msg);
            int result = showOptionDialog(parent, optionalMsgPanel, Constant.APP_TITLE, JOptionPane.YES_NO_OPTION, true);
            updateOptionalMsgCheckBox(menuItem);
            return result;
        }
    }

    private int showConfirm(String msg) {
        synchronized (optionDialogLock) {
            return showOptionDialog(getTextArea(msg), Constant.APP_TITLE, JOptionPane.YES_NO_OPTION, true);
        }
    }

    Component showing() {
        return isShowing() ? this : null;
    }

    private void setSafetyDialog(String statistic, String link, String name) {
        safetyEditorPane.setText("<html><head><title></title></head><body><table cellpadding=\"5\"><tr><td>" + Constant.HTML_FONT + Str.str(
                "linkSafetyWarningPart1", name) + (statistic == null || link == null ? "" : " " + Str.htmlLinkStr("linkSafetyWarningPart2", link, statistic))
                + "<br><br><b>" + Str.str("linkSafetyWarningPart3") + "</b></font></td></tr></table></body></html>");
    }

    private Window[] windows() {
        return new Window[]{this, safetyDialog, msgDialog, summaryDialog, faqFrame, aboutDialog, timeoutDialog, tvDialog, resultsPerSearchDialog,
            downloadSizeDialog, extensionsDialog, languageCountryDialog, dummyDialog /* Backward compatibility */, proxyDialog, addProxiesDialog,
            removeProxiesDialog, profileDialog, profileNameChangeDialog, commentsDialog, portDialog, tvSubtitleDialog, movieSubtitleDialog};
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
                changeLocale(settings[Constant.SETTINGS_LEN - 4]);
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
                viewedPortBefore = Boolean.parseBoolean(settings[++i]);
                ++i; // Backward compatibility

                restoreSize(GUI.this, settings[++i]);
                i += restoreWindows(settings, i, windows());

                restoreList("whitelist", settings[++i], whitelistListModel);
                restoreList("blacklist", settings[++i], blacklistListModel);
                restoreList("languageList", settings[++i], languageList);
                restoreList("countryList", settings[++i], countryList);

                i += restoreButtons(settings, i, dummyMenuItem5 /* Backward compatibility */, feedCheckBoxMenuItem);

                proxyImportFile = getPath(settings, ++i);
                proxyExportFile = getPath(settings, ++i);
                torrentDir = getPath(settings, ++i);
                subtitleDir = getPath(settings, ++i);

                i += restoreComboBoxes(settings, i, dummyComboBox); // Backward compatibility
                i += restoreComboBoxes(settings, i, timeoutDownloadLinkComboBox);
                i += restoreButtons(settings, i, emailWithDefaultAppCheckBoxMenuItem);
                i += restoreWindows(settings, i, playlistFrame);
                activationDialog.pack();

                playlistDir = getPath(settings, ++i);
                usePeerBlock = Boolean.parseBoolean(settings[++i]);
                i += restoreButtons(settings, i, playlistAutoOpenCheckBoxMenuItem, dummyMenuItem2 /* Backward compatibility */,
                        playlistShowNonVideoItemsCheckBoxMenuItem, playDefaultAppMenuItem);
                i += restoreColumnWidths(settings, i, resultsTable, yearCol, ratingCol);
                ++i; // language
                ++i; // Backward compatibility

                UI.select(downloaderButtonGroup, Integer.parseInt(settings[++i]));
                UI.select(trailerPlayerButtonGroup2, Integer.parseInt(settings[++i]));

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
            settings.append(viewedPortBefore).append(Constant.NEWLINE);
            saveButtons(settings, dummyMenuItem6 /* Backward compatibility */);

            settings.append(saveSize(GUI.this));
            for (Window window : windows()) {
                settings.append(savePosition(window));
            }

            saveList(settings, "whitelist", whitelistListModel.toArray());
            saveList(settings, "blacklist", blacklistListModel.toArray());
            saveList(settings, "languageList", languageList.getSelectedValues());
            saveList(settings, "countryList", countryList.getSelectedValues());
            saveButtons(settings, dummyMenuItem5 /* Backward compatibility */, feedCheckBoxMenuItem);
            savePaths(settings, proxyImportFile, proxyExportFile, torrentDir, subtitleDir);
            saveComboBoxes(settings, dummyComboBox); // Backward compatibility
            saveComboBoxes(settings, timeoutDownloadLinkComboBox);
            saveButtons(settings, emailWithDefaultAppCheckBoxMenuItem);
            settings.append(savePosition(playlistFrame));
            savePaths(settings, playlistDir);
            settings.append(usePeerBlock).append(Constant.NEWLINE);
            saveButtons(settings, playlistAutoOpenCheckBoxMenuItem, dummyMenuItem2 /* Backward compatibility */, playlistShowNonVideoItemsCheckBoxMenuItem,
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
            settings.append(UI.selectedIndex(trailerPlayerButtonGroup2));

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
            return new AbstractButton[]{dummyMenuItem4 /* Backward compatibility */, updateCheckBoxMenuItem, dummyMenuItem /* Backward compatibility */,
                proxyDownloadLinkInfoCheckBox, proxyVideoInfoCheckBox, proxySearchEnginesCheckBox, proxyTrailersCheckBox,
                dummyMenuItem3 /* Backward compatibility */, proxyUpdatesCheckBox, proxySubtitlesCheckBox, browserNotificationCheckBoxMenuItem};
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
                String[] point = Regex.split(location, ",");
                window.setLocation(new Point(Integer.parseInt(point[0]), Integer.parseInt(point[1])));
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
                UI.exportToClipboard(text.replace(Constant.ZERO_WIDTH_SPACE, "").replace("  ", Constant.NEWLINE2).trim());
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

            StringBuilder str = new StringBuilder(2048), str2 = new StringBuilder(64);
            for (int i = 0; i < selectedRows.length; i++) {
                str2.setLength(0);
                for (int j = 0; j < selectedCols.length; j++) {
                    int col = resultsSyncTable.convertColumnIndexToModel(selectedCols[j]);
                    if (col == imageCol) {
                        continue;
                    }
                    str2.append(col == titleCol ? Regex.htmlToPlainText((String) resultsSyncTable.getModelValueAt(resultsSyncTable.convertRowIndexToModel(
                            selectedRows[i]), currTitleCol)) : UI.innerHTML((String) resultsSyncTable.getViewValueAt(selectedRows[i],
                                            selectedCols[j]))).append('\t');
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

        final int VIEW_VAL, VAL;
        final Video video;

        SelectedTableRow() {
            VIEW_VAL = resultsSyncTable.getSelectedRow();
            VAL = resultsSyncTable.convertRowIndexToModel(VIEW_VAL);
            video = new Video((String) resultsSyncTable.getModelValueAt(VAL, idCol), (String) resultsSyncTable.getModelValueAt(VAL, currTitleCol),
                    UI.innerHTML((String) resultsSyncTable.getModelValueAt(VAL, yearCol)), resultsSyncTable.getModelValueAt(VAL, isTVShowCol).equals("1"),
                    resultsSyncTable.getModelValueAt(VAL, isTVShowAndMovieCol).equals("1"));
            video.oldTitle = (String) resultsSyncTable.getModelValueAt(VAL, oldTitleCol);
            video.imagePath = (String) resultsSyncTable.getModelValueAt(VAL, imageCol);
            video.summary = (String) resultsSyncTable.getModelValueAt(VAL, summaryCol);
            video.imageLink = (String) resultsSyncTable.getModelValueAt(VAL, imageLinkCol);
            video.season = (String) resultsSyncTable.getModelValueAt(VAL, seasonCol);
            video.episode = (String) resultsSyncTable.getModelValueAt(VAL, episodeCol);
        }

        VideoStrExportListener strExportListener(boolean exportToEmail) {
            VideoStrExportListener strExportListener = strExportListener(exportToEmail, false, exportToEmail ? 3 : 1);
            if (exportToEmail) {
                readSummaryActionPerformed(this, strExportListener);
                exportPosterImage(this, strExportListener);
            }
            return strExportListener;
        }

        VideoStrExportListener strExportListener(boolean exportToEmail, boolean exportSecondaryContent, int numStrsToExport) {
            return new VideoStrExporter(video.title, video.year, video.IS_TV_SHOW, exportToEmail, exportSecondaryContent, numStrsToExport);
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
        showException(e);
    }

    @Override
    public void enable(Boolean enablePrimary, Boolean enableSecondary, Boolean startPrimary, ContentType contentType) {
        AbstractButton[] primaryButtons1;
        AbstractButton primaryButtons2 = null;
        Boolean enablePrimary1 = enablePrimary, enablePrimary2 = null, enableSecondary2 = null;
        Component[] secondaryComponents1, secondaryComponents2 = null;
        if (contentType == ContentType.SUMMARY) {
            primaryButtons1 = new AbstractButton[]{readSummaryButton};
            secondaryComponents1 = new Component[]{readSummaryMenuItem, copyFullTitleAndYearMenuItem};
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

    private void enableVideoFormats(boolean enable) {
        UI.enable(enable, hd720CheckBox, hd1080CheckBox, hqVideoTypeCheckBox, dvdCheckBox);
    }

    @Override
    public void altVideoDownloadStarted() {
        if (!isAltSearch) {
            showConnectionException(new ConnectionException("<font color=\"red\">" + Str.str("switchingToBackupMode") + "</font> " + Str.str("connectionProblem",
                    Connection.getShortUrl(Connection.downloadLinkInfoFailUrl(), false))));
            enableVideoFormats(false);
            exitBackupModeButton.setEnabled(true);
            exitBackupModeButton.setBorderPainted(true);
            exitBackupModeButton.setText(Str.str("GUI.exitBackupModeButton.text2"));
            resizeExitBackupModeButton();
            isAltSearch = true;
        }
    }

    @Override
    public void msg(String msg, int msgType) {
        showMsg(msg, msgType);
    }

    @Override
    public void timedMsg(final String msg) {
        if (timedMsgThread != null) {
            timedMsgThread.interrupt();
        }
        (timedMsgThread = new Thread() {
            @Override
            public void run() {
                synchronized (timedMsgLock) {
                    timedMsgLabel.setText(msg);
                    timedMsgDialog.pack();
                    timedMsgDialog.setLocationRelativeTo(GUI.this);
                    UI.setVisible(timedMsgDialog);
                    try {
                        Thread.sleep(Regex.split(msg, " ").length * 400L);
                        timedMsgDialog.setVisible(false);
                    } catch (InterruptedException e) {
                        if (Debug.DEBUG) {
                            Debug.print(e);
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public void initSafetyDialog(String name) {
        proceedWithDownload = false;
        setSafetyDialog(null, null, name);
    }

    @Override
    public void safetyDialogMsg(String statistic, String link, String name) {
        setSafetyDialog(statistic, link, name);
    }

    Window resultsToBackground() {
        Window alwaysOnTopFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        resultsToBackground(false);
        if (alwaysOnTopFocus != null) {
            if (alwaysOnTopFocus.isAlwaysOnTop()) {
                alwaysOnTopFocus.setAlwaysOnTop(false);
            } else {
                return null;
            }
        }
        return alwaysOnTopFocus;
    }

    private void resultsToBackground(boolean permanent) {
        JScrollBar resultsScrollBar = resultsScrollPane.getVerticalScrollBar();
        JScrollBar resultsScrollBarCopy = new JScrollBar(resultsScrollBar.getOrientation(), resultsScrollBar.getValue(),
                resultsScrollBar.getVisibleAmount(), resultsScrollBar.getMinimum(), resultsScrollBar.getMaximum());
        resultsScrollBarCopy.setUnitIncrement(resultsScrollBar.getUnitIncrement());
        resultsScrollBarCopy.setBlockIncrement(resultsScrollBar.getBlockIncrement());
        resultsScrollPane.setVerticalScrollBar(resultsScrollBarCopy); // Stop scrolling
        if (permanent) {
            summaryDialog.setVisible(false);
            playlistFrame.setVisible(false);
        } else {
            summaryDialog.setAlwaysOnTop(false);
            playlistFrame.setAlwaysOnTop(false);
        }
    }

    void resultsToForeground(Window alwaysOnTopFocus) {
        summaryDialog.setAlwaysOnTop(true);
        playlistFrame.setAlwaysOnTop(true);
        if (alwaysOnTopFocus != null) {
            alwaysOnTopFocus.setAlwaysOnTop(true);
        }
    }

    @Override
    public void showSafetyDialog() {
        Window alwaysOnTopFocus = resultsToBackground();
        UI.setVisible(safetyDialog);
        resultsToForeground(alwaysOnTopFocus);
    }

    @Override
    public boolean canProceedWithUnsafeDownload() {
        return proceedWithDownload;
    }

    @Override
    public boolean canProceedWithUnsafeDownload(String name) {
        if (!safetyCheckBoxMenuItem.isSelected()) {
            return true;
        }

        proceedWithDownload = false;
        setSafetyDialog(null, null, name);
        showSafetyDialog();
        return proceedWithDownload;
    }

    @Override
    public void summary(String summary, String imagePath) {
        String summaryPage = "<html><head><title></title></head><body><table><tr>";
        if (imagePath != null) {
            String imageSize = "";
            String posterFilePath = Constant.TEMP_DIR + (new File(imagePath)).getName();
            File posterFile = new File(posterFilePath);
            if (!posterFile.exists()) {
                try {
                    RenderedImage posterImage = UI.image(new ImageIcon((new ImageIcon(imagePath)).getImage().getScaledInstance(Integer.parseInt(Str.get(495)),
                            Integer.parseInt(Str.get(496)), Image.SCALE_SMOOTH)));
                    IO.fileOp(Constant.TEMP_DIR, IO.MK_DIR);
                    ImageIO.write(posterImage, "png", posterFile);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    IO.fileOp(posterFile, IO.RM_FILE);
                    posterFilePath = imagePath;
                    imageSize = " width=\"" + Str.get(495) + "\" height=\"" + Str.get(496) + "\"";
                }
            }
            summaryPage += "<td align=\"left\" valign=\"top\"><img src=\"file:///" + Regex.replaceAll(posterFilePath, 237) + '"' + imageSize + "></td>";
        }
        summaryEditorPane.setText(summaryPage + "<td align=\"left\" valign=\"top\">" + Constant.HTML_FONT + Regex.replaceFirst(summary, "\\</body\\>",
                "</td></tr></table></body>"));
        summaryEditorPane.setSelectionStart(0);
        summaryEditorPane.setSelectionEnd(0);
        UI.setVisible(summaryDialog);
        summaryCloseButton.requestFocusInWindow();
    }

    @Override
    public Element getSummaryElement(String id) {
        return summaryEditorPaneDocument.getElement(id);
    }

    @Override
    public void insertAfterSummaryElement(Element element, String text) {
        try {
            if (text.length() > Constant.TV_EPISODE_PLACEHOLDER_LEN) {
                summaryEditorPaneDocument.insertAfterEnd(element, Constant.HTML_FONT + text + "</font>");
                return;
            }

            // Handle Java 7 lack of word-wrap bug by replacing exactly text.length() placeholder characters with text
            MutableAttributeSet simpleAttributeSet = new SimpleAttributeSet();
            AttributeSet attributes = element.getAttributes();
            Enumeration<?> attributeNames = attributes.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                Object attributeName = attributeNames.nextElement();
                if (!attributeName.equals(Tag.B) && !attributeName.equals(Attribute.FONT_WEIGHT)) {
                    simpleAttributeSet.addAttribute(attributeName, attributes.getAttribute(attributeName));
                }
            }
            summaryEditorPaneDocument.replace(element.getEndOffset(), text.length(), text, simpleAttributeSet);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
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

    private void updateOptionalMsgCheckBox(JMenuItem menuItem) {
        if (optionalMsgCheckBox.isSelected()) {
            menuItem.setSelected(false);
            optionalMsgCheckBox.setSelected(false);
        }
    }

    @Override
    public void startPeerBlock() {
        if (!workerListener.canFilterIpsWithoutBlocking()) {
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
        if (canShowPeerBlock && showOptionalConfirm(showing(), Str.str("startPeerblock"), peerBlockNotificationCheckBoxMenuItem) != JOptionPane.YES_OPTION) {
            usePeerBlock = false;
            return;
        }

        usePeerBlock = true;

        if (!WindowsUtil.canRunProgramsAsAdmin()) {
            msg(Str.str("adminPermissionsNeededForPeerBlock"), Constant.ERROR_MSG);
            return;
        }

        try {
            File peerBlock = new File(Constant.APP_DIR + Constant.PEER_BLOCK_VERSION);
            if (!peerBlock.exists()) {
                try {
                    IO.unzip(Constant.PROGRAM_DIR + Constant.PEER_BLOCK_VERSION + Constant.ZIP, Constant.APP_DIR);
                } catch (Exception e) {
                    IO.fileOp(peerBlock, IO.RM_DIR);
                    throw e;
                }
            }
            String peerBlockProgram = Constant.APP_DIR + Constant.PEER_BLOCK_VERSION + Constant.FILE_SEPARATOR + Constant.PEER_BLOCK + Constant.EXE;
            WindowsUtil.addMicrosoftRegistryEntry("Windows NT\\CurrentVersion\\AppCompatFlags\\Layers", "SZ", peerBlockProgram, "RUNASADMIN");
            (new ProcessBuilder(Constant.JAVA, Constant.JAR_OPTION, Constant.PROGRAM_DIR + Constant.PEER_BLOCK + Constant.JAR, peerBlockProgram,
                    Constant.APP_TITLE, Constant.APP_DIR + Constant.PEER_BLOCK + "Running", Constant.APP_DIR + Constant.PEER_BLOCK + "Exit")).start();
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            showMsg(Str.str("peerblockStartError") + ' ' + ExceptionUtil.toString(e), Constant.ERROR_MSG);
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
        boolean result = (fileChooser.showSaveDialog(showing()) == JFileChooser.APPROVE_OPTION);
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
    public boolean tvChoices(String season, String episode) {
        resultsToBackground(true);
        tvSeasonComboBox.setSelectedItem(season);
        tvEpisodeComboBox.setSelectedItem(episode);
        cancelTVSelection = true;
        tvDialog.setVisible(true);
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

        Component[] secondaryComponents;
        AbstractButton primaryButton;
        if (isRegularSearcher) {
            secondaryComponents = new Component[]{popularTVShowsButton, popularMoviesButton};
            primaryButton = searchButton;
        } else if (isTVShowSearch) {
            secondaryComponents = new Component[]{searchButton, popularMoviesButton};
            primaryButton = popularTVShowsButton;
        } else {
            secondaryComponents = new Component[]{searchButton, popularTVShowsButton};
            primaryButton = popularMoviesButton;
        }
        UI.enable(new AbstractButton[]{primaryButton}, false, new Component[]{loadMoreResultsButton, secondaryComponents[0], secondaryComponents[1],
            viewNewHighQualityMoviesMenuItem}, false);

        resultsSyncTable.requestFocusInWindow();
    }

    @Override
    public void newSearch(boolean isTVShow) {
        enableVideoFormats(true);
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

        Connection.clearCache();
        if (isTVShow) {
            trailerEpisodes.clear();
            downloadLinkEpisodes.clear();
            subtitleEpisodes.clear();
        }
        findControl.clearFindables();
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

        UI.enable(new AbstractButton[]{isRegularSearcher ? searchButton : (isTVShowSearch ? popularTVShowsButton : popularMoviesButton)}, true, new Component[]{
            searchButton, popularTVShowsButton, popularMoviesButton, viewNewHighQualityMoviesMenuItem}, true);

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
        findControl.addFindable((String) result[currTitleCol]);
    }

    @Override
    public void newResults(Iterable<Object[]> results) {
        for (Object[] result : results) {
            resultsSyncTable.addRow(result);
            posterImagePaths.add((String) result[imageCol]);
            findControl.addFindable((String) result[currTitleCol]);
        }
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
    public boolean unbanDownload(Long downloadID, String downloadName) {
        if (bannedDownloadIDs.contains(downloadID)) {
            if (isConfirmed(Str.str("banDownloadConfirm", downloadName))) {
                bannedDownloadIDs.remove(downloadID);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void removePlaylistItem(final PlaylistItem playlistItem) {
        UI.run(true, new Runnable() {
            @Override
            public void run() {
                synchronized (playlistSyncTable.lock) {
                    for (int row = playlistSyncTable.tableModel.getRowCount() - 1; row > -1; row--) {
                        if (playlistSyncTable.tableModel.getValueAt(row, playlistItemCol).equals(playlistItem)) {
                            int[] viewRows = playlistSyncTable.table.getSelectedRows();
                            playlistSyncTable.tableModel.removeRow(row);
                            if (viewRows.length != 0) {
                                selectFirstRow(viewRows);
                            }
                            return;
                        }
                    }
                }
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
                            }
                            return;
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean showPlaylist(final PlaylistItem selectedPlaylistItem) {
        synchronized (optionDialogLock) {
            UI.show(playlistFrame);
        }
        restorePlaylist(false);
        boolean selected = UI.run(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                synchronized (playlistSyncTable.lock) {
                    for (int row = playlistSyncTable.tableModel.getRowCount() - 1; row > -1; row--) {
                        if (playlistSyncTable.tableModel.getValueAt(row, playlistItemCol).equals(selectedPlaylistItem)) {
                            int viewRow = playlistSyncTable.table.convertRowIndexToView(row);
                            if (viewRow != -1) {
                                playlistSyncTable.table.setRowSelectionInterval(viewRow, viewRow);
                                JViewport viewport = (JViewport) playlistSyncTable.table.getParent();
                                viewport.scrollRectToVisible(rectangle(viewport, playlistSyncTable.table.getCellRect(viewRow, 0, true)));
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
    public void playlistError(String msg) {
        synchronized (optionDialogLock) {
            showOptionDialog(UI.deiconifyThenIsShowing(playlistFrame) ? playlistFrame : showing(), getTextArea(msg), Constant.APP_TITLE, Constant.ERROR_MSG,
                    false);
        }
    }

    private void play(PlaylistItem playlistItem) {
        if (playlistItem == null) {
            playlistPlayButton.setIcon(playIcon);
            playlistPlayMenuItem.setText(Str.str("play"));
            playlistBanGroupMenuItem.setText(Str.str("banGroup"));
            UI.enable(false, playlistBanGroupMenuItem, playlistReloadGroupMenuItem, playlistOpenMenuItem, playlistPlayButton, playlistPlayMenuItem);
            return;
        }
        playlistBanGroupMenuItem.setText(Str.str((bannedDownloadIDs.contains(playlistItem.groupDownloadID()) ? "un" : "") + "banGroup"));
        playlistBanGroupMenuItem.setEnabled(true);
        UI.enable(playlistItem.canOpen(), playlistReloadGroupMenuItem, playlistOpenMenuItem);
        boolean play = playlistItem.canPlay(), active = playlistItem.isActive();
        playlistPlayButton.setIcon(play ? playIcon : stopIcon);
        playlistPlayMenuItem.setText(Str.str(play ? "play" : Constant.STOP_KEY));
        UI.enable((play && !active) || (!play && active), playlistPlayButton, playlistPlayMenuItem);
    }

    @Override
    public void refreshPlaylistControls() {
        UI.run(false, new Runnable() {
            @Override
            public void run() {
                play(selectedPlaylistItem());
            }
        });
    }

    @Override
    public void setPlaylistPlayHint(String msg) {
        playlistPlayButton.setToolTipText(playlistPlayButton.getToolTipText() + msg);
        playlistPlayMenuItem.setToolTipText(playlistPlayMenuItem.getToolTipText() + msg);
    }

    @Override
    public boolean isConfirmed(String msg) {
        return showConfirm(msg) == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean isAuthorizationConfirmed(String msg) {
        synchronized (optionDialogLock) {
            authenticationMessageLabel.setText(msg);
            return showOptionDialog(authenticationPanel, Str.str("authenticationRequired"), JOptionPane.OK_CANCEL_OPTION, true) == JOptionPane.OK_OPTION;
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
        return Integer.parseInt((String) timeoutDownloadLinkComboBox.getSelectedItem());
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
        for (AbstractButton button : new AbstractButton[]{hqVideoTypeCheckBox, dvdCheckBox, hd720CheckBox, hd1080CheckBox}) {
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
    public String getWebBrowserAppDownloader() {
        return webBrowserAppDownloaderRadioButtonMenuItem.isSelected() ? Str.get(394) : (webBrowserAltAppDownloaderRadioButtonMenuItem.isSelected() ? Str.get(393)
                : null);
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
    public void commentsFinderStarted() {
        safetyLoadingLabel.setIcon(loadingIcon);
    }

    @Override
    public void commentsFinderStopped() {
        safetyLoadingLabel.setIcon(notLoadingIcon);
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
    public void updateMsg(String msg) {
        synchronized (optionDialogLock) {
            showOptionDialog(getEditorPane(msg), Constant.APP_TITLE, Constant.INFO_MSG, false);
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
    public void summaryReadStarted() {
        UI.enable(new AbstractButton[]{summaryTextToSpeechButton}, false, null, null);
        summaryLoadingLabel.setIcon(loadingIcon);
    }

    @Override
    public void summaryReadStopped() {
        UI.enable(new AbstractButton[]{summaryTextToSpeechButton}, true, null, null);
        summaryLoadingLabel.setIcon(notLoadingIcon);
    }

    private static int portNum(String port) {
        int portNum;
        return Regex.isMatch(port, "\\d{1,5}+") && (portNum = Integer.parseInt(port)) <= 65535 ? portNum : -1;
    }

    int setRandomPort() {
        int portNum = (new Random()).nextInt(16373) + 49161;
        portTextField.setText(String.valueOf(portNum));
        return portNum;
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

    @Override
    public void showLicenseActivation() {
        activationDialog.setLocationRelativeTo(UI.deiconifyThenIsShowing(playlistFrame) ? playlistFrame : this);
        resultsToBackground(true);
        UI.setVisible(activationDialog);
    }

    @Override
    public void licenseActivated(String activationCode) {
        workerListener.licenseActivated();
        activationTextField.setForeground(new Color(21, 138, 12));
        activationTextField.setText(activationCode);
        activationDialog.setVisible(false);
        showMsg(Str.str("activationSuccessful"), Constant.INFO_MSG);
    }

    @Override
    public void licenseDeactivated() {
        activationTextField.setForeground(Color.BLACK);
        showMsg(Str.str("activationFailed"), Constant.ERROR_MSG);
    }

    @Override
    public void licenseActivationStarted() {
        UI.enable(false, activationButton, activationUpgradeButton);
        activationLoadingLabel.setIcon(loadingIcon);
    }

    @Override
    public void licenseActivationStopped() {
        UI.enable(true, activationButton, activationUpgradeButton);
        activationLoadingLabel.setIcon(notLoadingIcon);
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
        activationButton.setText(Str.str("GUI.activationButton.text"));
        activationButton.setToolTipText(Str.str("GUI.activationButton.toolTipText"));
        activationCodeLabel.setText(Str.str("GUI.activationCodeLabel.text"));
        activationDialog.setTitle(Str.str("GUI.activationDialog.title"));
        activationUpgradeButton.setText(Str.str("GUI.activationUpgradeButton.text"));
        activationUpgradeButton.setToolTipText(Str.str("GUI.activationUpgradeButton.toolTipText"));
        activationUpgradeLabel.setText(Str.str("GUI.activationUpgradeLabel.text"));
        addProxiesAddButton.setText(Str.str("GUI.addProxiesAddButton.text"));
        addProxiesCancelButton.setText(Str.str("GUI.addProxiesCancelButton.text"));
        addProxiesDialog.setTitle(Str.str("GUI.addProxiesDialog.title"));
        addProxiesLabel.setText(Str.str("GUI.addProxiesLabel.text"));
        authenticationMessageLabel.setText(Str.str("GUI.authenticationMessageLabel.text"));
        authenticationPasswordLabel.setText(Str.str("GUI.authenticationPasswordLabel.text"));
        authenticationUsernameLabel.setText(Str.str("GUI.authenticationUsernameLabel.text"));
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
        downloadLink1Button.setToolTipText(Str.str("GUI.downloadLink1Button.toolTipText"));
        downloadLink1MenuItem.setText(Str.str("GUI.downloadLink1MenuItem.text"));
        downloadLink2Button.setToolTipText(Str.str("GUI.downloadLink2Button.toolTipText"));
        downloadLink2MenuItem.setText(Str.str("GUI.downloadLink2MenuItem.text"));
        downloadMenu.setText(Str.str("GUI.downloadMenu.text"));
        downloadSizeButton.setText(Str.str("GUI.downloadSizeButton.text"));
        downloadSizeDialog.setTitle(Str.str("GUI.downloadSizeDialog.title"));
        downloadSizeIgnoreCheckBox.setText(Str.str("GUI.downloadSizeIgnoreCheckBox.text"));
        downloadSizeLabel.setText(Str.str("GUI.downloadSizeLabel.text"));
        downloadSizeLabel.setToolTipText(Str.str("GUI.downloadSizeLabel.toolTipText"));
        downloadSizeMenuItem.setText(Str.str("GUI.downloadSizeMenuItem.text"));
        downloadSizeToLabel.setText(Str.str("GUI.downloadSizeToLabel.text"));
        downloaderMenu.setText(Str.str("GUI.downloaderMenu.text"));
        dvdCheckBox.setToolTipText(Str.str("GUI.dvdCheckBox.toolTipText"));
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
        hd1080CheckBox.setToolTipText(Str.str("GUI.hd1080CheckBox.toolTipText"));
        hd720CheckBox.setToolTipText(Str.str("GUI.hd720CheckBox.toolTipText"));
        helpMenu.setText(Str.str("GUI.helpMenu.text"));
        hideMenuItem.setText(Str.str("GUI.hideMenuItem.text"));
        hqVideoTypeCheckBox.setToolTipText(Str.str("GUI.hqVideoTypeCheckBox.toolTipText"));
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
        noButton.setText(Str.str("GUI.noButton.text"));
        noButton.setToolTipText(Str.str("GUI.noButton.toolTipText"));
        noDownloaderRadioButtonMenuItem.setText(Str.str("GUI.noDownloaderRadioButtonMenuItem.text"));
        optionalMsgCheckBox.setText(Str.str("GUI.optionalMsgCheckBox.text"));
        pasteMenuItem.setText(Str.str("GUI.pasteMenuItem.text"));
        peerBlockNotificationCheckBoxMenuItem.setText(Str.str("GUI.peerBlockNotificationCheckBoxMenuItem.text"));
        playlistAutoOpenCheckBoxMenuItem.setText(Str.str("GUI.playlistAutoOpenCheckBoxMenuItem.text"));
        playlistCopyMenuItem.setText(Str.str("GUI.playlistCopyMenuItem.text"));
        playlistDownloaderRadioButtonMenuItem.setText(Str.str("GUI.playlistDownloaderRadioButtonMenuItem.text"));
        playlistFrame.setTitle(Str.str("GUI.playlistFrame.title"));
        playlistMenu.setText(Str.str("GUI.playlistMenu.text"));
        playlistMenuItem.setText(Str.str("GUI.playlistMenuItem.text"));
        playlistMoveDownButton.setToolTipText(Str.str("GUI.playlistMoveDownButton.toolTipText"));
        playlistMoveDownMenuItem.setText(Str.str("GUI.playlistMoveDownMenuItem.text"));
        playlistMoveUpButton.setToolTipText(Str.str("GUI.playlistMoveUpButton.toolTipText"));
        playlistMoveUpMenuItem.setText(Str.str("GUI.playlistMoveUpMenuItem.text"));
        playlistOpenMenuItem.setText(Str.str("GUI.playlistOpenMenuItem.text"));
        playlistPlayButton.setToolTipText(Str.ctrlStr("GUI.playlistPlayButton.toolTipText"));
        playlistPlayMenuItem.setToolTipText(Str.ctrlStr("GUI.playlistPlayMenuItem.toolTipText"));
        playlistPlayWithDefaultAppCheckBoxMenuItem.setText(Str.str("GUI.playlistPlayWithDefaultAppCheckBoxMenuItem.text"));
        playlistReloadGroupMenuItem.setText(Str.str("GUI.playlistReloadGroupMenuItem.text"));
        playlistRemoveButton.setToolTipText(Str.str("GUI.playlistRemoveButton.toolTipText"));
        playlistRemoveMenuItem.setText(Str.str("GUI.playlistRemoveMenuItem.text"));
        playlistSaveFolderMenuItem.setText(Str.str("GUI.playlistSaveFolderMenuItem.text"));
        playlistShowNonVideoItemsCheckBoxMenuItem.setText(Str.str("GUI.playlistShowNonVideoItemsCheckBoxMenuItem.text"));
        playlistShowNonVideoItemsCheckBoxMenuItem.setToolTipText(Str.str("GUI.playlistShowNonVideoItemsCheckBoxMenuItem.toolTipText"));
        popularMoviesButton.setToolTipText(Str.ctrlStr("GUI.popularMoviesButton.toolTipText"));
        popularMoviesResultsPerSearchLabel.setText(Str.str("GUI.popularMoviesResultsPerSearchLabel.text"));
        popularMoviesResultsPerSearchLabel.setToolTipText(Str.str("GUI.popularMoviesResultsPerSearchLabel.toolTipText"));
        popularTVShowsButton.setToolTipText(Str.str("GUI.popularTVShowsButton.toolTipText"));
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
        safetyDialog.setTitle(Str.str("GUI.safetyDialog.title"));
        searchButton.setToolTipText(Str.str("GUI.searchButton.toolTipText"));
        searchMenu.setText(Str.str("GUI.searchMenu.text"));
        seasonLabel.setText(Str.str("GUI.seasonLabel.text"));
        seasonLabel.setToolTipText(Str.str("GUI.seasonLabel.toolTipText"));
        selectAllMenuItem.setText(Str.str("GUI.selectAllMenuItem.text"));
        summaryCloseButton.setText(Str.str("GUI.summaryCloseButton.text"));
        summaryDialog.setTitle(Str.str("GUI.summaryDialog.title"));
        summaryTextToSpeechButton.setToolTipText(Str.str("GUI.summaryTextToSpeechButton.toolTipText"));
        textComponentCopyMenuItem.setText(Str.str("GUI.textComponentCopyMenuItem.text"));
        textComponentCutMenuItem.setText(Str.str("GUI.textComponentCutMenuItem.text"));
        textComponentDeleteMenuItem.setText(Str.str("GUI.textComponentDeleteMenuItem.text"));
        textComponentPasteMenuItem.setText(Str.str("GUI.textComponentPasteMenuItem.text"));
        textComponentPasteSearchMenuItem.setText(Str.str("GUI.textComponentPasteSearchMenuItem.text"));
        textComponentSelectAllMenuItem.setText(Str.str("GUI.textComponentSelectAllMenuItem.text"));
        timeoutButton.setText(Str.str("GUI.timeoutButton.text"));
        timeoutDialog.setTitle(Str.str("GUI.timeoutDialog.title"));
        timeoutDownloadLinkLabel.setText(Str.str("GUI.timeoutDownloadLinkLabel.text"));
        timeoutDownloadLinkLabel.setToolTipText(Str.str("GUI.timeoutDownloadLinkLabel.toolTipText"));
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
        viewNewHighQualityMoviesMenuItem.setText(Str.str("GUI.viewNewHighQualityMoviesMenuItem.text"));
        watchOnDeviceMenuItem.setText(Str.str("GUI.watchOnDeviceMenuItem.text"));
        watchOnDeviceMenuItem.setToolTipText(Str.str("GUI.watchOnDeviceMenuItem.toolTipText"));
        watchTrailerButton.setToolTipText(Str.str("GUI.watchTrailerButton.toolTipText"));
        watchTrailerMenuItem.setText(Str.str("GUI.watchTrailerMenuItem.text"));
        webBrowserAltAppDownloaderRadioButtonMenuItem.setText(Str.str("GUI.webBrowserAltAppDownloaderRadioButtonMenuItem.text"));
        webBrowserAppDownloaderRadioButtonMenuItem.setText(Str.str("GUI.webBrowserAppDownloaderRadioButtonMenuItem.text"));
        whitelistLabel.setText(Str.str("GUI.whitelistLabel.text"));
        whitelistedToBlacklistedButton.setToolTipText(Str.str("GUI.whitelistedToBlacklistedButton.toolTipText"));
        yesButton.setText(Str.str("GUI.yesButton.text"));
        yesButton.setToolTipText(Str.str("GUI.yesButton.toolTipText"));

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
            List<List<Object>> rows = resultsSyncTable.tableModel.getDataVector();
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
            List<List<Object>> rows = playlistSyncTable.tableModel.getDataVector();
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
            if (playlistTrayIcon != null) {
                UI.updateTrayIconLabels(playlistTrayIcon, playlistFrame);
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

        resizeContent();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    JDialog aboutDialog;
    JEditorPane aboutEditorPane;
    JMenuItem aboutMenuItem;
    JScrollPane aboutScrollPane;
    JButton activationButton;
    JLabel activationCodeLabel;
    JDialog activationDialog;
    JLabel activationLoadingLabel;
    JTextField activationTextField;
    JButton activationUpgradeButton;
    JLabel activationUpgradeLabel;
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
    JButton downloadLink1Button;
    JMenuItem downloadLink1MenuItem;
    JButton downloadLink2Button;
    JMenuItem downloadLink2MenuItem;
    JMenu downloadMenu;
    Separator downloadMenuSeparator1;
    Separator downloadMenuSeparator2;
    Separator downloadMenuSeparator3;
    Separator downloadMenuSeparator4;
    JButton downloadSizeButton;
    JDialog downloadSizeDialog;
    JCheckBox downloadSizeIgnoreCheckBox;
    JLabel downloadSizeLabel;
    JMenuItem downloadSizeMenuItem;
    JLabel downloadSizeToLabel;
    ButtonGroup downloaderButtonGroup;
    JMenu downloaderMenu;
    JRadioButtonMenuItem dutchRadioButtonMenuItem;
    JCheckBox dvdCheckBox;
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
    JCheckBox hd1080CheckBox;
    JCheckBox hd720CheckBox;
    JMenu helpMenu;
    Separator helpMenuSeparator1;
    Separator helpMenuSeparator2;
    JMenuItem hideMenuItem;
    JCheckBox hqVideoTypeCheckBox;
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
    JButton noButton;
    JRadioButtonMenuItem noDownloaderRadioButtonMenuItem;
    JCheckBox optionalMsgCheckBox;
    JPanel optionalMsgPanel;
    JTextArea optionalMsgTextArea;
    JMenuItem pasteMenuItem;
    JCheckBoxMenuItem peerBlockNotificationCheckBoxMenuItem;
    JCheckBoxMenuItem playlistAutoOpenCheckBoxMenuItem;
    JMenuItem playlistBanGroupMenuItem;
    JMenuItem playlistCopyMenuItem;
    JRadioButtonMenuItem playlistDownloaderRadioButtonMenuItem;
    JFileChooser playlistFileChooser;
    JTextField playlistFindTextField;
    JFrame playlistFrame;
    JMenu playlistMenu;
    JMenuItem playlistMenuItem;
    Separator playlistMenuSeparator1;
    JButton playlistMoveDownButton;
    JMenuItem playlistMoveDownMenuItem;
    JButton playlistMoveUpButton;
    JMenuItem playlistMoveUpMenuItem;
    JMenuItem playlistOpenMenuItem;
    JButton playlistPlayButton;
    JMenuItem playlistPlayMenuItem;
    JCheckBoxMenuItem playlistPlayWithDefaultAppCheckBoxMenuItem;
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
    JButton popularMoviesButton;
    JPopupMenu popularMoviesButtonPopupMenu;
    JComboBox popularMoviesResultsPerSearchComboBox;
    JLabel popularMoviesResultsPerSearchLabel;
    JButton popularTVShowsButton;
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
    JButton resultsPerSearchButton;
    JDialog resultsPerSearchDialog;
    JMenuItem resultsPerSearchMenuItem;
    JScrollPane resultsScrollPane;
    JTable resultsTable;
    JCheckBoxMenuItem safetyCheckBoxMenuItem;
    JDialog safetyDialog;
    JEditorPane safetyEditorPane;
    JLabel safetyLoadingLabel;
    JScrollPane safetyScrollPane;
    JButton searchButton;
    JMenu searchMenu;
    Separator searchMenuSeparator1;
    Separator searchMenuSeparator2;
    Separator searchMenuSeparator3;
    Separator searchMenuSeparator4;
    Separator searchMenuSeparator5;
    Separator searchMenuSeparator6;
    Separator searchMenuSeparator7;
    JTextField searchProgressTextField;
    JLabel seasonLabel;
    JMenuItem selectAllMenuItem;
    JRadioButtonMenuItem spanishRadioButtonMenuItem;
    JDateChooser startDateChooser;
    JTextField statusBarTextField;
    JFileChooser subtitleFileChooser;
    JButton summaryCloseButton;
    JDialog summaryDialog;
    JEditorPane summaryEditorPane;
    JLabel summaryLoadingLabel;
    JScrollPane summaryScrollPane;
    JButton summaryTextToSpeechButton;
    JPopupMenu tablePopupMenu;
    Separator tablePopupMenuSeparator1;
    Separator tablePopupMenuSeparator2;
    Separator tablePopupMenuSeparator3;
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
    JComboBox timeoutDownloadLinkComboBox;
    JLabel timeoutDownloadLinkLabel;
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
    JMenuItem viewNewHighQualityMoviesMenuItem;
    JMenuItem watchOnDeviceMenuItem;
    JButton watchTrailerButton;
    JMenuItem watchTrailerMenuItem;
    JRadioButtonMenuItem webBrowserAltAppDownloaderRadioButtonMenuItem;
    JRadioButtonMenuItem webBrowserAppDownloaderRadioButtonMenuItem;
    JLabel whitelistLabel;
    JList whitelistedList;
    JScrollPane whitelistedScrollPane;
    JButton whitelistedToBlacklistedButton;
    JButton yesButton;
    // End of variables declaration//GEN-END:variables
}
