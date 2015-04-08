package gui;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javax.swing.ListModel;
import javax.swing.RootPaneContainer;
import javax.swing.RowFilter;
import javax.swing.RowFilter.Entry;
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
import javax.swing.plaf.RootPaneUI;
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
    private WorkerListener workerListener;
    private boolean isRegularSearcher = true, proceedWithDownload, cancelTVSelection, isAltSearch, isTVShowSearch, isTVShowSubtitle, exitBackupMode, forcePlay,
            useMediaServer;
    boolean viewedPortBefore;
    private final AtomicBoolean isPlaylistRestored = new AtomicBoolean();
    String proxyImportFile, proxyExportFile, torrentDir, subtitleDir, playlistDir;
    DefaultListModel blacklistListModel, whitelistListModel;
    private DefaultListModel removeProxiesListModel;
    private HTMLDocument summaryEditorPaneDocument;
    int imageCol, titleCol, yearCol, ratingCol, idCol, currTitleCol, oldTitleCol, summaryCol, imageLinkCol, isTVShowCol, isTVShowAndMovieCol,
            seasonCol, episodeCol;
    int playlistNameCol, playlistSizeCol, playlistProgressCol, playlistItemCol;
    String randomPort;
    private static final String HQ_FORMAT = "high/TV+ quality ";
    private static final String CTRL_CLICK = KeyEvent.getKeyModifiersText(KeyEvent.CTRL_MASK).toLowerCase(Locale.ENGLISH) + "+click to ";
    private String subtitleFormat;
    private Video subtitleVideo;
    private VideoStrExportListener subtitleStrExportListener;
    private final Set<Integer> trailerEpisodes = new HashSet<Integer>(4), downloadLinkEpisodes = new HashSet<Integer>(4),
            subtitleEpisodes = new HashSet<Integer>(4);
    private Icon loadingIcon, notLoadingIcon, noWarningIcon, warningIcon, playIcon, stopIcon;
    JList popupList;
    JTextComponent popupTextComponent;
    Component downloadLinkPopupComponent;
    SyncTable resultsSyncTable, playlistSyncTable;
    private AbstractPopupListener textComponentPopupListener;
    private final ActionListener htmlCopyListener = new HTMLCopyListener(), tableCopyListener = new TableCopyListener(),
            playlistTableCopyListener = new PlaylistTableCopyListener();
    private final Object msgDialogLock = new Object(), optionDialogLock = new Object(), playlistRestorationLock = new Object();
    private final Settings settings = new Settings();
    final Map<String, Icon> posters = new ConcurrentHashMap<String, Icon>(100);
    final BlockingQueue<String> posterImagePaths = new LinkedBlockingQueue<String>();
    private Thread posterCacher;
    private final JCalendar startCalendar = new JCalendar(null, Locale.ENGLISH, true, true);
    private final JCalendar endCalendar = new JCalendar(null, Locale.ENGLISH, true, true);
    private JTextFieldDateEditor startDateTextField, endDateTextField;
    boolean usePeerBlock;
    private volatile Thread timedMsgThread;
    final Object timedMsgLock = new Object();
    private final List<String> findTitles = new CopyOnWriteArrayList<String>();
    private int findTitleRow = -2;
    private SplashScreen splashScreen;
    JDialog dummyDialog = new JDialog();
    JMenuItem dummyMenuItem = new JMenuItem(), peerBlockMenuItem;
    JComboBox dummyComboBox = new JComboBox();

    public GUI(WorkerListener workerListener, SplashScreen splashScreen) throws Exception {
        this.workerListener = workerListener;
        this.splashScreen = splashScreen;

        splashScreen.progress();

        initComponents();

        dummyComboBox.setEditable(true);

        splashScreen.progress();

        hideFindTextField();
        AutoCompleteDecorator.decorate(findTextField, findTitles, false);

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

        for (JCalendar calendar : new JCalendar[]{startCalendar, endCalendar}) {
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
        for (JComponent component : new JComponent[]{anyTitleCheckBox, titleTextField, genreList, typeComboBox, ratingComboBox, startDateTextField,
            endDateTextField}) {
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
        }, titleTextField, findTextField, addProxiesTextArea, profileNameChangeTextField, customExtensionTextField,
                portTextField, optionalMsgTextArea, commentsTextPane, msgEditorPane, faqEditorPane, aboutEditorPane, summaryEditorPane, safetyEditorPane,
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

        UI.addPopupMenu(readSummaryButtonPopupMenu, readSummaryButton);
        UI.addPopupMenu(watchTrailerButtonPopupMenu, watchTrailerButton);

        UI.addMouseListener(new AbstractPopupListener() {
            @Override
            protected void showPopup(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    downloadLinkPopupComponent = (Component) evt.getSource();
                    show(downloadLinkButtonPopupMenu, evt);
                }
            }
        }, playButton, downloadLink1Button, downloadLink2Button);

        UI.addPopupMenu(watchSourceButtonPopupMenu, watchSource1Button, watchSource2Button);

        UI.addMouseListener(new AbstractPopupListener() {
            @Override
            protected void showPopup(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    downloadLinkPopupComponent = (Component) evt.getSource();
                    show(exitBackupModePopupMenu, evt);
                }
            }
        }, hqVideoTypeCheckBox, dvdCheckBox, hd720CheckBox, hd1080CheckBox);

        UI.addPopupMenu(popularMoviesButtonPopupMenu, popularMoviesButton);

        splashScreen.progress();

        summaryEditorPaneDocument = (HTMLDocument) summaryEditorPane.getDocument();

        TableColumnModel colModel = resultsTable.getColumnModel();
        imageCol = colModel.getColumnIndex(Constant.IMAGE_COL);
        titleCol = colModel.getColumnIndex(Constant.TITLE_COL);
        yearCol = colModel.getColumnIndex(Constant.YEAR_COL);
        ratingCol = colModel.getColumnIndex(Constant.RATING_COL);
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
        playlistNameCol = playlistColModel.getColumnIndex(Constant.PLAYLIST_NAME_COL);
        playlistSizeCol = playlistColModel.getColumnIndex(Constant.PLAYLIST_SIZE_COL);
        playlistProgressCol = playlistColModel.getColumnIndex(Constant.PLAYLIST_PROGRESS_COL);
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
            protected Float convert(String rating) {
                String tempRating = UI.innerHTML(rating);
                return tempRating.equals("-") ? 0.0f : Float.parseFloat(tempRating);
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

        splashScreen.progress();

        autoDownloadersButtonGroup.add(defaultRadioButtonMenuItem);
        autoDownloadersButtonGroup.add(customRadioButtonMenuItem);

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
        UI.setIcon(closeBoxButton, "closeBox");
        UI.setIcon(summaryTextToSpeechButton, "speaker");
        playIcon = UI.icon("play.png");
        stopIcon = UI.icon("stop.png");
        play(null);
        UI.setIcon(playlistMoveUpButton, "up");
        UI.setIcon(playlistMoveDownButton, "down");
        UI.setIcon(playlistRemoveButton, "remove");

        splashScreen.progress();

        List<String> genreArr = new ArrayList<String>(32);
        genreArr.add(Constant.ANY);
        Collections.addAll(genreArr, Regex.split(359, Constant.SEPARATOR1));
        genreList.setListData(genreArr.toArray());
        genreList.setSelectedValue(Constant.ANY, true);

        ratingComboBox.addItem(Constant.ANY);
        for (String rating : Regex.split(360, Constant.SEPARATOR1)) {
            ratingComboBox.addItem(rating);
        }
        ratingComboBox.setSelectedItem(Constant.ANY);

        splashScreen.progress();

        if (new File(Constant.APP_DIR + Constant.PROXIES).exists()) {
            proxyComboBox.removeAllItems();
            proxyComboBox.addItem(Constant.NO_PROXY);
            for (String proxy : Regex.split(IO.read(Constant.APP_DIR + Constant.PROXIES), Constant.NEWLINE)) {
                String newProxy = proxy.trim();
                if (!newProxy.isEmpty()) {
                    proxyComboBox.addItem(newProxy);
                }
            }
            proxyComboBox.setSelectedItem(Constant.NO_PROXY);
        }

        splashScreen.progress();

        profileComboBox.addItem(Constant.DEFAULT_PROFILE);
        String[] profiles = Regex.split(IO.read(Constant.APP_DIR + Constant.PROFILES), Constant.NEWLINE);
        if (profiles.length != 9) {
            profiles = Regex.split(IO.read(Constant.PROGRAM_DIR + Constant.PROFILES), Constant.NEWLINE);
        }
        for (int i = 0; i < profiles.length; i++) {
            profileComboBox.addItem(profiles[i]);
            updateProfileGUIitems(i + 1);
        }
        profileComboBox.setSelectedItem(Constant.DEFAULT_PROFILE);
        faqEditorPane.setText(Regex.replaceFirst(IO.read(Constant.PROGRAM_DIR + "FAQ" + Constant.HTML), "<br><br><br>", Str.get(555) + "<br><br><br>"));

        splashScreen.progress();

        AutoCompleteDecorator.decorate(titleTextField, Arrays.asList(Regex.split(IO.read(Constant.PROGRAM_DIR + "autoCompleteTitles" + Constant.TXT),
                Constant.NEWLINE)), false);

        splashScreen.progress();

        updatedTVComboBoxes();

        for (String language : Regex.subtitleLanguages.keySet()) {
            tvSubtitleLanguageComboBox.addItem(language);
            movieSubtitleLanguageComboBox.addItem(language);
        }

        languageList.setListData(Regex.languages.keySet().toArray());
        countryList.setListData(Regex.countries.keySet().toArray());

        UI.initCountComboBoxes(414, 502, regularResultsPerSearchComboBox);
        UI.initCountComboBoxes(412, 413, popularMoviesResultsPerSearchComboBox, popularTVShowsResultsPerSearchComboBox);

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
            UI.addMinimizeToTraySupport(this);
            UI.addMinimizeToTraySupport(playlistFrame);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }

        splashScreen.progress();

        if (Constant.CAN_PEER_BLOCK) {
            peerBlockMenuItem = peerBlockNotificationCheckBoxMenuItem;
        } else {
            peerBlockMenuItem = new JMenuItem();
            peerBlockNotificationCheckBoxMenuItem.setEnabled(false);
            peerBlockNotificationCheckBoxMenuItem.setSelected(false);
            peerBlockNotificationCheckBoxMenuItem.setToolTipText("only available for Windows XP or higher");
            usePeerBlock = false;
        }
        settings.loadSettings(Constant.APP_DIR + Constant.USER_SETTINGS);
        playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(null);

        splashScreen.progress();
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
            image = new ImageIcon((new ImageIcon(imagePath)).getImage().getScaledInstance(60, 89, Image.SCALE_SMOOTH));
            posters.put(imagePath, image); // Not a concurrency bug
        }
        return image;
    }

    public void showFeed(boolean isStartUp) {
        if (isStartUp && !feedCheckBoxMenuItem.isSelected()) {
            return;
        }

        hideFindTextField();
        isTVShowSearch = false;
        isRegularSearcher = false;
        int numResultsPerSearch = Integer.parseInt((String) popularMoviesResultsPerSearchComboBox.getSelectedItem());
        String[] languages = UI.copy(languageList), countries = UI.copy(countryList);
        workerListener.popularSearchStarted(numResultsPerSearch, isTVShowSearch, languages, countries, true, !isStartUp);
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
        faqFrame = new JFrame();
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
        langaugeCountryOkButton = new JButton();
        languageScrollPane = new JScrollPane();
        languageList = new JList();
        countryScrollPane = new JScrollPane();
        countryList = new JList();
        languageCountryWarningLabel = new JLabel();
        tablePopupMenu = new JPopupMenu();
        readSummaryMenuItem = new JMenuItem();
        watchTrailerMenuItem = new JMenuItem();
        playMenuItem = new JMenuItem();
        downloadLink1MenuItem = new JMenuItem();
        downloadLink2MenuItem = new JMenuItem();
        watchSource1MenuItem = new JMenuItem();
        watchSource2MenuItem = new JMenuItem();
        tablePopupMenuSeparator1 = new Separator();
        copyMenu = new JMenu();
        copySelectionMenuItem = new JMenuItem();
        copyFullTitleAndYearMenuItem = new JMenuItem();
        copyPosterImageMenuItem = new JMenuItem();
        copyMenuSeparator1 = new Separator();
        copySummaryLinkMenuItem = new JMenuItem();
        copyTrailerLinkMenuItem = new JMenuItem();
        copyDownloadLink1MenuItem = new JMenuItem();
        copyDownloadLink2MenuItem = new JMenuItem();
        copyWatchLink1MenuItem = new JMenuItem();
        copyWatchLink2MenuItem = new JMenuItem();
        copySubtitleLinkMenuItem = new JMenuItem();
        emailMenu = new JMenu();
        emailSummaryLinkMenuItem = new JMenuItem();
        emailTrailerLinkMenuItem = new JMenuItem();
        emailDownloadLink1MenuItem = new JMenuItem();
        emailDownloadLink2MenuItem = new JMenuItem();
        emailWatchLink1MenuItem = new JMenuItem();
        emailWatchLink2MenuItem = new JMenuItem();
        emailMenuSeparator1 = new Separator();
        emailEverythingMenuItem = new JMenuItem();
        tablePopupMenuSeparator2 = new Separator();
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
        proxyVideoStreamersCheckBox = new JCheckBox();
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
        autoDownloadersButtonGroup = new ButtonGroup();
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
        tvSubtitleCancelButton = new JButton();
        tvSubtitleLoadingLabel = new JLabel();
        movieSubtitleDialog = new JDialog();
        movieSubtitleLanguageLabel = new JLabel();
        movieSubtitleLanguageComboBox = new JComboBox();
        movieSubtitleFormatLabel = new JLabel();
        movieSubtitleFormatComboBox = new JComboBox();
        movieSubtitleDownloadMatch1Button = new JButton();
        movieSubtitleDownloadMatch2Button = new JButton();
        movieSubtitleCancelButton = new JButton();
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
        exitBackupModePopupMenu = new JPopupMenu();
        exitBackupModeMenuItem = new JMenuItem();
        readSummaryButtonPopupMenu = new JPopupMenu();
        readSummaryCancelMenuItem = new JMenuItem();
        watchTrailerButtonPopupMenu = new JPopupMenu();
        watchTrailerCancelMenuItem = new JMenuItem();
        downloadLinkButtonPopupMenu = new JPopupMenu();
        downloadLinkCancelMenuItem = new JMenuItem();
        downloadLinkExitBackupModeMenuItem = new JMenuItem();
        watchSourceButtonPopupMenu = new JPopupMenu();
        watchSourceCancelMenuItem = new JMenuItem();
        playlistFrame = new JFrame();
        playlistScrollPane = new JScrollPane();
        playlistTable = new JTable();
        playlistPlayButton = new JButton();
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
        activationDialog = new JDialog();
        activationUpgradeButton = new JButton();
        activationUpgradeLabel = new JLabel();
        activationCodeLabel = new JLabel();
        activationTextField = new JTextField();
        activationButton = new JButton();
        activationLoadingLabel = new JLabel();
        titleTextField = new JTextField();
        titleLabel = new JLabel();
        releasedLabel = new JLabel();
        genreLabel = new JLabel();
        ratingComboBox = new JComboBox();
        ratingLabel = new JLabel();
        resultsScrollPane = new JScrollPane();
        resultsTable = new JTable();
        progressBar = new JProgressBar();
        progressBarLabel = new JLabel();
        resultsLabel = new JLabel();
        searchButton = new JButton();
        stopButton = new JButton();
        anyTitleCheckBox = new JCheckBox();
        genreScrollPane = new JScrollPane();
        genreList = new JList();
        loadMoreResultsButton = new JButton();
        typeLabel = new JLabel();
        typeComboBox = new JComboBox();
        releasedToLabel = new JLabel();
        linkProgressBar = new JProgressBar();
        hqVideoTypeCheckBox = new JCheckBox();
        dvdCheckBox = new JCheckBox();
        hd720CheckBox = new JCheckBox();
        hd1080CheckBox = new JCheckBox();
        popularMoviesButton = new JButton();
        popularTVShowsButton = new JButton();
        closeBoxButton = new JButton();
        loadingLabel = new JLabel();
        readSummaryButton = new JButton();
        watchTrailerButton = new JButton();
        playButton = new JButton();
        downloadLink1Button = new JButton();
        downloadLink2Button = new JButton();
        watchSource1Button = new JButton();
        watchSource2Button = new JButton();
        statusBarTextField = new JTextField();
        connectionIssueButton = new JButton();
        startDateChooser = new JDateChooser(startCalendar, null, null, null);
        endDateChooser = new JDateChooser(endCalendar, null, null, null);
        findTextField = new JTextField();
        menuBar = new JMenuBar();
        fileMenu = new JMenu();
        useProfileMenu = new JMenu();
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
        useProfileMenuSeparator1 = new Separator();
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
        playlistMenu = new JMenu();
        playlistMenuItem = new JMenuItem();
        playlistMenuSeparator1 = new Separator();
        playlistSaveFolderMenuItem = new JMenuItem();
        playlistAutoOpenCheckBoxMenuItem = new JCheckBoxMenuItem();
        playlistShowNonVideoItemsCheckBoxMenuItem = new JCheckBoxMenuItem();
        playlistTipsCheckBoxMenuItem = new JCheckBoxMenuItem();
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
        downloadWithDefaultAppCheckBoxMenuItem = new JCheckBoxMenuItem();
        autoDownloadingCheckBoxMenuItem = new JCheckBoxMenuItem();
        downloaderMenu = new JMenu();
        defaultRadioButtonMenuItem = new JRadioButtonMenuItem();
        customRadioButtonMenuItem = new JRadioButtonMenuItem();
        helpMenu = new JMenu();
        faqMenuItem = new JMenuItem();
        helpMenuSeparator1 = new Separator();
        activationMenuItem = new JMenuItem();
        updateMenuItem = new JMenuItem();
        updateCheckBoxMenuItem = new JCheckBoxMenuItem();
        helpMenuSeparator2 = new Separator();
        aboutMenuItem = new JMenuItem();
        splashScreen.progress();

        safetyDialog.setTitle("Warning: Untrustworthy Source");
        safetyDialog.setAlwaysOnTop(true);
        safetyDialog.setIconImage(null);
        safetyDialog.setModalityType(ModalityType.APPLICATION_MODAL);

        yesButton.setText("Yes");
        yesButton.setToolTipText("proceed");
        yesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                yesButtonActionPerformed(evt);
            }
        });

        noButton.setText("No");
        noButton.setToolTipText("do not proceed");
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

        summaryDialog.setTitle("Title Summary");
        summaryDialog.setAlwaysOnTop(true);
        summaryDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                summaryDialogWindowClosing(evt);
            }
        });

        summaryCloseButton.setText("Close");
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
        summaryTextToSpeechButton.setToolTipText("hear summary");
        summaryTextToSpeechButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                summaryTextToSpeechButtonActionPerformed(evt);
            }
        });

        summaryLoadingLabel.setText(null);

        GroupLayout summaryDialogLayout = new GroupLayout(summaryDialog.getContentPane());
        summaryDialog.getContentPane().setLayout(summaryDialogLayout);
        summaryDialogLayout.setHorizontalGroup(summaryDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(summaryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(summaryDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(summaryScrollPane, GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
                    .addGroup(summaryDialogLayout.createSequentialGroup()
                        .addComponent(summaryTextToSpeechButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 143, Short.MAX_VALUE)
                        .addComponent(summaryCloseButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 143, Short.MAX_VALUE)
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

        faqFrame.setTitle("FAQ");
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

        aboutDialog.setTitle("About");
        aboutDialog.setAlwaysOnTop(true);

        aboutScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        aboutScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        aboutEditorPane.setEditable(false);
        aboutEditorPane.setContentType("text/html"); // NOI18N
        aboutEditorPane.setText("<html><head></head><body><table cellpadding=\"5\"><tr><td>" + Constant.HTML_FONT + Constant.APP_TITLE + "<br><br>Version " + Constant.APP_VERSION + "<br><br>Created by Anthony Gray</font></td></tr></table></body></html>");
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

        timeoutDialog.setTitle("Connection Timeout");
        timeoutDialog.setAlwaysOnTop(true);
        timeoutDialog.setModal(true);
        timeoutDialog.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);

        timeoutLabel.setText("Connection Timeout (Seconds):");
        timeoutLabel.setToolTipText("set the connection timeout that the application uses when connecting to websites");

        timeoutComboBox.setModel(new DefaultComboBoxModel(new String[] { "5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60", "65", "70", "75", "80", "85", "90", "95", "100", "105", "110", "115", "120", "125", "130", "135", "140", "145", "150", "155", "160", "165", "170", "175", "180" }));
        timeoutComboBox.setSelectedIndex(2);
        timeoutComboBox.setSelectedItem("15");

        timeoutButton.setText("OK");
        timeoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                timeoutButtonActionPerformed(evt);
            }
        });

        timeoutDownloadLinkLabel.setText("Download Link Timeout (Seconds):");
        timeoutDownloadLinkLabel.setToolTipText("set the timeout that the application uses when downloading download links");

        timeoutDownloadLinkComboBox.setModel(new DefaultComboBoxModel(new String[] { "0", "5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60", "65", "70", "75", "80", "85", "90", "95", "100", "105", "110", "115", "120", "125", "130", "135", "140", "145", "150", "155", "160", "165", "170", "175", "180" }));
        timeoutDownloadLinkComboBox.setSelectedIndex(18);
        timeoutDownloadLinkComboBox.setSelectedItem("90");

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

        tvDialog.setTitle("Season and Episode");
        tvDialog.setModal(true);

        tvSeasonComboBox.setModel(new DefaultComboBoxModel(new String[] { "ANY", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100" }));
        tvSeasonComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvSeasonComboBoxActionPerformed(evt);
            }
        });

        tvEpisodeComboBox.setModel(new DefaultComboBoxModel(new String[] { "ANY", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111", "112", "113", "114", "115", "116", "117", "118", "119", "120", "121", "122", "123", "124", "125", "126", "127", "128", "129", "130", "131", "132", "133", "134", "135", "136", "137", "138", "139", "140", "141", "142", "143", "144", "145", "146", "147", "148", "149", "150", "151", "152", "153", "154", "155", "156", "157", "158", "159", "160", "161", "162", "163", "164", "165", "166", "167", "168", "169", "170", "171", "172", "173", "174", "175", "176", "177", "178", "179", "180", "181", "182", "183", "184", "185", "186", "187", "188", "189", "190", "191", "192", "193", "194", "195", "196", "197", "198", "199", "200", "201", "202", "203", "204", "205", "206", "207", "208", "209", "210", "211", "212", "213", "214", "215", "216", "217", "218", "219", "220", "221", "222", "223", "224", "225", "226", "227", "228", "229", "230", "231", "232", "233", "234", "235", "236", "237", "238", "239", "240", "241", "242", "243", "244", "245", "246", "247", "248", "249", "250", "251", "252", "253", "254", "255", "256", "257", "258", "259", "260", "261", "262", "263", "264", "265", "266", "267", "268", "269", "270", "271", "272", "273", "274", "275", "276", "277", "278", "279", "280", "281", "282", "283", "284", "285", "286", "287", "288", "289", "290", "291", "292", "293", "294", "295", "296", "297", "298", "299", "300" }));
        tvEpisodeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvEpisodeComboBoxActionPerformed(evt);
            }
        });

        tvSubmitButton.setText("Submit");
        tvSubmitButton.setToolTipText("submit");
        tvSubmitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvSubmitButtonActionPerformed(evt);
            }
        });

        tvSelectionLabel.setText("Enter the season and episode of the television show:");

        seasonLabel.setText("Season:");
        seasonLabel.setToolTipText("select a season");

        episodeLabel.setText("Episode:");
        episodeLabel.setToolTipText("select an episode");

        tvCancelButton.setText("Cancel");
        tvCancelButton.setToolTipText("cancel");
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

        resultsPerSearchDialog.setTitle("Number of Results Per Search");
        resultsPerSearchDialog.setAlwaysOnTop(true);
        resultsPerSearchDialog.setModal(true);

        regularResultsPerSearchLabel.setText("Regular Search:");
        regularResultsPerSearchLabel.setToolTipText("set the number of results to display per search");

        resultsPerSearchButton.setText("OK");
        resultsPerSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                resultsPerSearchButtonActionPerformed(evt);
            }
        });

        popularMoviesResultsPerSearchLabel.setText("Popular Movies Search:");
        popularMoviesResultsPerSearchLabel.setToolTipText("set the number of results to display per search for \"Popular Movies\"");

        popularTVShowsResultsPerSearchLabel.setText("Popular TV Shows Search:");
        popularTVShowsResultsPerSearchLabel.setToolTipText("set the number of results to display per search for \"Popular TV Shows\"");

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

        downloadSizeDialog.setTitle("Download Size");
        downloadSizeDialog.setAlwaysOnTop(true);
        downloadSizeDialog.setModal(true);

        downloadSizeLabel.setText("Download Size (GB):");
        downloadSizeLabel.setToolTipText("set the size of a video file that can be downloaded");

        maxDownloadSizeComboBox.setModel(new DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100", Constant.INFINITY }));
        maxDownloadSizeComboBox.setSelectedItem(Constant.INFINITY);
        maxDownloadSizeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                maxDownloadSizeComboBoxActionPerformed(evt);
            }
        });

        downloadSizeToLabel.setText("to");

        minDownloadSizeComboBox.setModel(new DefaultComboBoxModel(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100" }));
        minDownloadSizeComboBox.setSelectedItem("0");
        minDownloadSizeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                minDownloadSizeComboBoxActionPerformed(evt);
            }
        });

        downloadSizeButton.setText("OK");
        downloadSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadSizeButtonActionPerformed(evt);
            }
        });

        downloadSizeIgnoreCheckBox.setSelected(true);
        downloadSizeIgnoreCheckBox.setText("Ignore size if a video is found within a video box set");
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

        extensionsDialog.setTitle("Video File Extensions");
        extensionsDialog.setAlwaysOnTop(true);
        extensionsDialog.setModal(true);

        blacklistedScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        blacklistedList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                blacklistedListKeyPressed(evt);
            }
        });
        blacklistedScrollPane.setViewportView(blacklistedList);

        blacklistedLabel.setText("Blacklisted Extensions");

        whitelistedScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        whitelistedList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                whitelistedListKeyPressed(evt);
            }
        });
        whitelistedScrollPane.setViewportView(whitelistedList);

        whitelistedToBlacklistedButton.setText("->");
        whitelistedToBlacklistedButton.setToolTipText("move from whitelist to blacklist");
        whitelistedToBlacklistedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                whitelistedToBlacklistedButtonActionPerformed(evt);
            }
        });

        blacklistedToWhitelistedButton.setText("<-");
        blacklistedToWhitelistedButton.setToolTipText("move from blacklist to whitelist");
        blacklistedToWhitelistedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                blacklistedToWhitelistedButtonActionPerformed(evt);
            }
        });

        whitelistLabel.setText("Whitelisted Extensions");

        fileExtensionsLabel.setText("Set the video file extensions to download.");

        extensionsButton.setText("OK");
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
                            .addComponent(blacklistedScrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
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

        languageCountryDialog.setTitle("Language/Country");
        languageCountryDialog.setAlwaysOnTop(true);
        languageCountryDialog.setModal(true);

        countryLabel.setText("Country:");

        languageLabel.setText("Language:");

        langaugeCountryOkButton.setText("OK");
        langaugeCountryOkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                langaugeCountryOkButtonActionPerformed(evt);
            }
        });

        languageList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                languageListValueChanged(evt);
            }
        });
        languageScrollPane.setViewportView(languageList);

        countryList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                countryListValueChanged(evt);
            }
        });
        countryScrollPane.setViewportView(countryList);

        languageCountryWarningLabel.setText("<html>Changing these settings may cause a significant slowdown of<br>\"Popular Movies\" and \"Popular TV Shows\" searches.</html>");

        GroupLayout languageCountryDialogLayout = new GroupLayout(languageCountryDialog.getContentPane());
        languageCountryDialog.getContentPane().setLayout(languageCountryDialogLayout);
        languageCountryDialogLayout.setHorizontalGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(languageCountryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(languageCountryWarningLabel)
                    .addComponent(langaugeCountryOkButton, Alignment.CENTER)
                    .addGroup(Alignment.CENTER, languageCountryDialogLayout.createSequentialGroup()
                        .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
                            .addComponent(languageScrollPane, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                            .addComponent(languageLabel))
                        .addGap(18, 18, 18)
                        .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
                            .addComponent(countryLabel)
                            .addComponent(countryScrollPane, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))))
                .addContainerGap())
        );
        languageCountryDialogLayout.setVerticalGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(languageCountryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(languageCountryWarningLabel)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(countryLabel, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
                    .addComponent(languageLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(languageCountryDialogLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(countryScrollPane, GroupLayout.PREFERRED_SIZE, 159, GroupLayout.PREFERRED_SIZE)
                    .addComponent(languageScrollPane, GroupLayout.PREFERRED_SIZE, 159, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(langaugeCountryOkButton, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        splashScreen.progress();

        readSummaryMenuItem.setText("Read Summary");
        readSummaryMenuItem.setEnabled(false);
        readSummaryMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readSummaryMenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(readSummaryMenuItem);

        watchTrailerMenuItem.setText("Watch Trailer");
        watchTrailerMenuItem.setEnabled(false);
        watchTrailerMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchTrailerMenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(watchTrailerMenuItem);

        String exitBackupModeToolTip = CTRL_CLICK + "exit backup mode";
        playMenuItem.setText("Play");
        playMenuItem.setToolTipText(exitBackupModeToolTip);
        playMenuItem.setEnabled(false);
        playMenuItem.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                playMenuItemMousePressed(evt);
            }
        });
        playMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playMenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(playMenuItem);

        downloadLink1MenuItem.setText("Download (Link 1)");
        downloadLink1MenuItem.setToolTipText(exitBackupModeToolTip);
        downloadLink1MenuItem.setEnabled(false);
        downloadLink1MenuItem.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                downloadLink1MenuItemMousePressed(evt);
            }
        });
        downloadLink1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadLink1MenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(downloadLink1MenuItem);

        downloadLink2MenuItem.setText("Download (Link 2)");
        downloadLink2MenuItem.setToolTipText(exitBackupModeToolTip);
        downloadLink2MenuItem.setEnabled(false);
        downloadLink2MenuItem.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                downloadLink2MenuItemMousePressed(evt);
            }
        });
        downloadLink2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadLink2MenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(downloadLink2MenuItem);

        watchSource1MenuItem.setText("Watch (Source 1)");
        watchSource1MenuItem.setEnabled(false);
        watchSource1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchSource1MenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(watchSource1MenuItem);

        watchSource2MenuItem.setText("Watch (Source 2)");
        watchSource2MenuItem.setEnabled(false);
        watchSource2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchSource2MenuItemActionPerformed(evt);
            }
        });
        tablePopupMenu.add(watchSource2MenuItem);
        tablePopupMenu.add(tablePopupMenuSeparator1);

        copyMenu.setText("Copy");

        copySelectionMenuItem.setText("Selection");
        copySelectionMenuItem.setEnabled(false);
        copySelectionMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copySelectionMenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copySelectionMenuItem);

        copyFullTitleAndYearMenuItem.setText("Title & Year");
        copyFullTitleAndYearMenuItem.setToolTipText("includes title alias if found");
        copyFullTitleAndYearMenuItem.setEnabled(false);
        copyFullTitleAndYearMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyFullTitleAndYearMenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copyFullTitleAndYearMenuItem);

        copyPosterImageMenuItem.setText("Image");
        copyPosterImageMenuItem.setEnabled(false);
        copyPosterImageMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyPosterImageMenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copyPosterImageMenuItem);
        copyMenu.add(copyMenuSeparator1);

        copySummaryLinkMenuItem.setText("Summary Link");
        copySummaryLinkMenuItem.setEnabled(false);
        copySummaryLinkMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copySummaryLinkMenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copySummaryLinkMenuItem);

        copyTrailerLinkMenuItem.setText("Trailer Link");
        copyTrailerLinkMenuItem.setEnabled(false);
        copyTrailerLinkMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyTrailerLinkMenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copyTrailerLinkMenuItem);

        copyDownloadLink1MenuItem.setText("Download Link 1");
        copyDownloadLink1MenuItem.setToolTipText(exitBackupModeToolTip);
        copyDownloadLink1MenuItem.setEnabled(false);
        copyDownloadLink1MenuItem.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                copyDownloadLink1MenuItemMousePressed(evt);
            }
        });
        copyDownloadLink1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyDownloadLink1MenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copyDownloadLink1MenuItem);

        copyDownloadLink2MenuItem.setText("Download Link 2");
        copyDownloadLink2MenuItem.setToolTipText(exitBackupModeToolTip);
        copyDownloadLink2MenuItem.setEnabled(false);
        copyDownloadLink2MenuItem.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                copyDownloadLink2MenuItemMousePressed(evt);
            }
        });
        copyDownloadLink2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyDownloadLink2MenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copyDownloadLink2MenuItem);

        copyWatchLink1MenuItem.setText("Watch Link 1");
        copyWatchLink1MenuItem.setEnabled(false);
        copyWatchLink1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyWatchLink1MenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copyWatchLink1MenuItem);

        copyWatchLink2MenuItem.setText("Watch Link 2");
        copyWatchLink2MenuItem.setEnabled(false);
        copyWatchLink2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyWatchLink2MenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copyWatchLink2MenuItem);

        copySubtitleLinkMenuItem.setText("Subtitle Link");
        copySubtitleLinkMenuItem.setEnabled(false);
        copySubtitleLinkMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copySubtitleLinkMenuItemActionPerformed(evt);
            }
        });
        copyMenu.add(copySubtitleLinkMenuItem);

        tablePopupMenu.add(copyMenu);

        emailMenu.setText("Email");

        emailSummaryLinkMenuItem.setText("Summary Link");
        emailSummaryLinkMenuItem.setEnabled(false);
        emailSummaryLinkMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                emailSummaryLinkMenuItemActionPerformed(evt);
            }
        });
        emailMenu.add(emailSummaryLinkMenuItem);

        emailTrailerLinkMenuItem.setText("Trailer Link");
        emailTrailerLinkMenuItem.setEnabled(false);
        emailTrailerLinkMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                emailTrailerLinkMenuItemActionPerformed(evt);
            }
        });
        emailMenu.add(emailTrailerLinkMenuItem);

        emailDownloadLink1MenuItem.setText("Download Link 1");
        emailDownloadLink1MenuItem.setToolTipText(exitBackupModeToolTip);
        emailDownloadLink1MenuItem.setEnabled(false);
        emailDownloadLink1MenuItem.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                emailDownloadLink1MenuItemMousePressed(evt);
            }
        });
        emailDownloadLink1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                emailDownloadLink1MenuItemActionPerformed(evt);
            }
        });
        emailMenu.add(emailDownloadLink1MenuItem);

        emailDownloadLink2MenuItem.setText("Download Link 2");
        emailDownloadLink2MenuItem.setToolTipText(exitBackupModeToolTip);
        emailDownloadLink2MenuItem.setEnabled(false);
        emailDownloadLink2MenuItem.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                emailDownloadLink2MenuItemMousePressed(evt);
            }
        });
        emailDownloadLink2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                emailDownloadLink2MenuItemActionPerformed(evt);
            }
        });
        emailMenu.add(emailDownloadLink2MenuItem);

        emailWatchLink1MenuItem.setText("Watch Link 1");
        emailWatchLink1MenuItem.setEnabled(false);
        emailWatchLink1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                emailWatchLink1MenuItemActionPerformed(evt);
            }
        });
        emailMenu.add(emailWatchLink1MenuItem);

        emailWatchLink2MenuItem.setText("Watch Link 2");
        emailWatchLink2MenuItem.setEnabled(false);
        emailWatchLink2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                emailWatchLink2MenuItemActionPerformed(evt);
            }
        });
        emailMenu.add(emailWatchLink2MenuItem);
        emailMenu.add(emailMenuSeparator1);

        emailEverythingMenuItem.setText("Everything");
        emailEverythingMenuItem.setEnabled(false);
        emailEverythingMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                emailEverythingMenuItemActionPerformed(evt);
            }
        });
        emailMenu.add(emailEverythingMenuItem);

        tablePopupMenu.add(emailMenu);
        tablePopupMenu.add(tablePopupMenuSeparator2);

        findSubtitleMenuItem.setText("Find Subtitle");
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

        textComponentCutMenuItem.setText("Cut");
        textComponentCutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                textComponentCutMenuItemActionPerformed(evt);
            }
        });
        textComponentPopupMenu.add(textComponentCutMenuItem);

        textComponentCopyMenuItem.setText("Copy");
        textComponentCopyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                textComponentCopyMenuItemActionPerformed(evt);
            }
        });
        textComponentPopupMenu.add(textComponentCopyMenuItem);

        textComponentPasteMenuItem.setText("Paste");
        textComponentPasteMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                textComponentPasteMenuItemActionPerformed(evt);
            }
        });
        textComponentPopupMenu.add(textComponentPasteMenuItem);

        textComponentPasteSearchMenuItem.setText("Paste & Search");
        textComponentPasteSearchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                textComponentPasteSearchMenuItemActionPerformed(evt);
            }
        });
        textComponentPopupMenu.add(textComponentPasteSearchMenuItem);

        textComponentDeleteMenuItem.setText("Delete");
        textComponentDeleteMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                textComponentDeleteMenuItemActionPerformed(evt);
            }
        });
        textComponentPopupMenu.add(textComponentDeleteMenuItem);
        textComponentPopupMenu.add(textComponentPopupMenuSeparator1);

        textComponentSelectAllMenuItem.setText("Select All");
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

        proxyDialog.setTitle("Proxy Settings");
        proxyDialog.setAlwaysOnTop(true);
        proxyDialog.setModal(true);

        proxyAddButton.setText("Add");
        proxyAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyAddButtonActionPerformed(evt);
            }
        });

        proxyRemoveButton.setText("Remove");
        proxyRemoveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyRemoveButtonActionPerformed(evt);
            }
        });

        proxyComboBox.setModel(new DefaultComboBoxModel(new String[] { "NO PROXY" }));

        proxyDownloadLinkInfoCheckBox.setSelected(true);
        proxyDownloadLinkInfoCheckBox.setText("Download Link Info");
        proxyDownloadLinkInfoCheckBox.setToolTipText("e.g. " + Str.get(577));

        proxyUseForLabel.setText("Use for:");

        proxyVideoInfoCheckBox.setText("Video Info");
        proxyVideoInfoCheckBox.setToolTipText("e.g. " + Str.get(578));

        proxySearchEnginesCheckBox.setText("Search Engines");
        proxySearchEnginesCheckBox.setToolTipText("e.g. " + Str.get(579));

        proxyOKButton.setText("OK");
        proxyOKButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyOKButtonActionPerformed(evt);
            }
        });

        proxyDownloadButton.setText("Download");
        proxyDownloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyDownloadButtonActionPerformed(evt);
            }
        });

        proxyTrailersCheckBox.setText("Trailers");
        proxyTrailersCheckBox.setToolTipText("e.g. " + Str.get(580));

        proxyLoadingLabel.setText(null);

        proxyImportButton.setText("Import");
        proxyImportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyImportButtonActionPerformed(evt);
            }
        });

        proxyExportButton.setText("Export");
        proxyExportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyExportButtonActionPerformed(evt);
            }
        });

        proxyVideoStreamersCheckBox.setText("Video Streamers");
        proxyVideoStreamersCheckBox.setToolTipText("e.g. " + Str.get(581));

        proxyUpdatesCheckBox.setText("Updates");
        proxyUpdatesCheckBox.setToolTipText("e.g. " + Str.get(582));

        proxySubtitlesCheckBox.setText("Subtitles");
        proxySubtitlesCheckBox.setToolTipText("e.g. " + Str.get(583));

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
                        .addComponent(proxyVideoStreamersCheckBox)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(proxyUpdatesCheckBox)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(proxySubtitlesCheckBox))
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
                    .addComponent(proxyVideoStreamersCheckBox)
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

        proxyDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {proxyDownloadLinkInfoCheckBox, proxySearchEnginesCheckBox, proxySubtitlesCheckBox, proxyTrailersCheckBox, proxyUpdatesCheckBox, proxyVideoInfoCheckBox, proxyVideoStreamersCheckBox});

        splashScreen.progress();

        addProxiesDialog.setTitle("Add Proxies");
        addProxiesDialog.setAlwaysOnTop(true);
        addProxiesDialog.setModal(true);
        addProxiesDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                addProxiesDialogWindowClosing(evt);
            }
        });

        addProxiesLabel.setText("Enter HTTP/HTTPS proxies, one per line, in the format of ip:port (e.g. 1.234.56.78:9999):");

        addProxiesTextArea.setColumns(20);
        addProxiesTextArea.setRows(5);
        addProxiesScrollPane.setViewportView(addProxiesTextArea);

        addProxiesCancelButton.setText("Cancel");
        addProxiesCancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                addProxiesCancelButtonActionPerformed(evt);
            }
        });

        addProxiesAddButton.setText("Add");
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

        removeProxiesDialog.setTitle("Remove Proxies");
        removeProxiesDialog.setAlwaysOnTop(true);
        removeProxiesDialog.setModal(true);
        removeProxiesDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                removeProxiesDialogWindowClosing(evt);
            }
        });

        removeProxiesLabel.setText("Select proxies to remove:");

        removeProxiesList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                removeProxiesListKeyPressed(evt);
            }
        });
        removeProxiesScrollPane.setViewportView(removeProxiesList);

        removeProxiesRemoveButton.setText("Remove");
        removeProxiesRemoveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                removeProxiesRemoveButtonActionPerformed(evt);
            }
        });

        removeProxiesCancelButton.setText("Cancel");
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

        msgOKButton.setText("OK");
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

        profileDialog.setTitle("Profiles");
        profileDialog.setAlwaysOnTop(true);
        profileDialog.setModal(true);

        profileSetButton.setText("Set");
        profileSetButton.setEnabled(false);
        profileSetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profileSetButtonActionPerformed(evt);
            }
        });

        profileClearButton.setText("Clear");
        profileClearButton.setEnabled(false);
        profileClearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profileClearButtonActionPerformed(evt);
            }
        });

        profileUseButton.setText("Use");
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

        profileRenameButton.setText("Rename");
        profileRenameButton.setEnabled(false);
        profileRenameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profileRenameButtonActionPerformed(evt);
            }
        });

        profileOKButton.setText("OK");
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

        profileNameChangeDialog.setTitle("Profile Name Change");
        profileNameChangeDialog.setAlwaysOnTop(true);
        profileNameChangeDialog.setModal(true);
        profileNameChangeDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                profileNameChangeDialogWindowClosing(evt);
            }
        });

        profileNameChangeLabel.setText("Enter a new name:");

        profileNameChangeOKButton.setText("OK");
        profileNameChangeOKButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profileNameChangeOKButtonActionPerformed(evt);
            }
        });

        profileNameChangeCancelButton.setText("Cancel");
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

        commentsDialog.setTitle("Comments");
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

        portDialog.setTitle("Port Settings");
        portDialog.setAlwaysOnTop(true);
        portDialog.setModal(true);
        portDialog.addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent evt) {
                portDialogComponentHidden(evt);
            }
        });

        portLabel.setText("Incoming Listen Port:");
        portLabel.setToolTipText("recommended port range: 49161 to 65533");

        portTextField.setToolTipText("recommended port range: 49161 to 65533");

        portRandomizeCheckBox.setSelected(true);
        portRandomizeCheckBox.setText("Randomize port on startup");
        portRandomizeCheckBox.setToolTipText("use a random port each time the application starts up");
        portRandomizeCheckBox.setBorder(null);
        portRandomizeCheckBox.setFocusPainted(false);
        portRandomizeCheckBox.setMargin(new Insets(2, 0, 2, 2));

        portOkButton.setText("OK");
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

        optionalMsgCheckBox.setText("Don't show again");
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

        tvSubtitleDialog.setTitle("TV Show Subtitle");
        tvSubtitleDialog.setAlwaysOnTop(true);

        tvSubtitleLanguageLabel.setText("Language:");

        tvSubtitleLanguageComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvSubtitleLanguageComboBoxActionPerformed(evt);
            }
        });

        tvSubtitleFormatLabel.setText("Video Source:");

        tvSubtitleFormatComboBox.setModel(new DefaultComboBoxModel(new String[]{Constant.ANY, Constant.HQ, Constant.DVD, Constant.HD720, Constant.HD1080}));
        tvSubtitleFormatComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvSubtitleFormatComboBoxActionPerformed(evt);
            }
        });

        tvSubtitleSeasonLabel.setText("Season:");
        tvSubtitleSeasonLabel.setToolTipText("select a season");

        tvSubtitleSeasonComboBox.setModel(new DefaultComboBoxModel(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100" }));

        tvSubtitleEpisodeLabel.setText("Episode:");
        tvSubtitleEpisodeLabel.setToolTipText("select an episode");

        tvSubtitleEpisodeComboBox.setModel(new DefaultComboBoxModel(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111", "112", "113", "114", "115", "116", "117", "118", "119", "120", "121", "122", "123", "124", "125", "126", "127", "128", "129", "130", "131", "132", "133", "134", "135", "136", "137", "138", "139", "140", "141", "142", "143", "144", "145", "146", "147", "148", "149", "150", "151", "152", "153", "154", "155", "156", "157", "158", "159", "160", "161", "162", "163", "164", "165", "166", "167", "168", "169", "170", "171", "172", "173", "174", "175", "176", "177", "178", "179", "180", "181", "182", "183", "184", "185", "186", "187", "188", "189", "190", "191", "192", "193", "194", "195", "196", "197", "198", "199", "200", "201", "202", "203", "204", "205", "206", "207", "208", "209", "210", "211", "212", "213", "214", "215", "216", "217", "218", "219", "220", "221", "222", "223", "224", "225", "226", "227", "228", "229", "230", "231", "232", "233", "234", "235", "236", "237", "238", "239", "240", "241", "242", "243", "244", "245", "246", "247", "248", "249", "250", "251", "252", "253", "254", "255", "256", "257", "258", "259", "260", "261", "262", "263", "264", "265", "266", "267", "268", "269", "270", "271", "272", "273", "274", "275", "276", "277", "278", "279", "280", "281", "282", "283", "284", "285", "286", "287", "288", "289", "290", "291", "292", "293", "294", "295", "296", "297", "298", "299", "300" }));

        tvSubtitleDownloadMatch1Button.setText("Download (Match 1)");
        tvSubtitleDownloadMatch1Button.setToolTipText("download subtitle");
        tvSubtitleDownloadMatch1Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvSubtitleDownloadMatch1ButtonActionPerformed(evt);
            }
        });

        tvSubtitleDownloadMatch2Button.setText("Download (Match 2)");
        tvSubtitleDownloadMatch2Button.setToolTipText("download subtitle");
        tvSubtitleDownloadMatch2Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvSubtitleDownloadMatch2ButtonActionPerformed(evt);
            }
        });

        tvSubtitleCancelButton.setText("Cancel");
        tvSubtitleCancelButton.setToolTipText("cancel download");
        tvSubtitleCancelButton.setEnabled(false);
        tvSubtitleCancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tvSubtitleCancelButtonActionPerformed(evt);
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
                        .addComponent(tvSubtitleLanguageComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(tvSubtitleFormatLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(tvSubtitleFormatComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(tvSubtitleCancelButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 218, Short.MAX_VALUE)
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
                    .addComponent(tvSubtitleCancelButton)
                    .addComponent(tvSubtitleLoadingLabel))
                .addContainerGap())
        );

        tvSubtitleDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {tvSubtitleEpisodeComboBox, tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox});

        tvSubtitleDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {tvSubtitleCancelButton, tvSubtitleDownloadMatch1Button, tvSubtitleDownloadMatch2Button});

        splashScreen.progress();

        movieSubtitleDialog.setTitle("Movie Subtitle");
        movieSubtitleDialog.setAlwaysOnTop(true);

        movieSubtitleLanguageLabel.setText("Language:");

        movieSubtitleLanguageComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                movieSubtitleLanguageComboBoxActionPerformed(evt);
            }
        });

        movieSubtitleFormatLabel.setText("Video Source:");

        movieSubtitleFormatComboBox.setModel(new DefaultComboBoxModel(new String[]{Constant.ANY, Constant.HQ, Constant.DVD, Constant.HD720, Constant.HD1080}));
        movieSubtitleFormatComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                movieSubtitleFormatComboBoxActionPerformed(evt);
            }
        });

        movieSubtitleDownloadMatch1Button.setText("Download (Match 1)");
        movieSubtitleDownloadMatch1Button.setToolTipText("download subtitle");
        movieSubtitleDownloadMatch1Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                movieSubtitleDownloadMatch1ButtonActionPerformed(evt);
            }
        });

        movieSubtitleDownloadMatch2Button.setText("Download (Match 2)");
        movieSubtitleDownloadMatch2Button.setToolTipText("download subtitle");
        movieSubtitleDownloadMatch2Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                movieSubtitleDownloadMatch2ButtonActionPerformed(evt);
            }
        });

        movieSubtitleCancelButton.setText("Cancel");
        movieSubtitleCancelButton.setToolTipText("cancel download");
        movieSubtitleCancelButton.setEnabled(false);
        movieSubtitleCancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                movieSubtitleCancelButtonActionPerformed(evt);
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
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(movieSubtitleCancelButton)
                        .addGap(18, 18, Short.MAX_VALUE)
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
                    .addComponent(movieSubtitleCancelButton)
                    .addComponent(movieSubtitleLoadingLabel))
                .addContainerGap())
        );

        movieSubtitleDialogLayout.linkSize(SwingConstants.VERTICAL, new Component[] {movieSubtitleCancelButton, movieSubtitleDownloadMatch1Button, movieSubtitleDownloadMatch2Button});

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

        authenticationMessageLabel.setText("A username and password are being requested.");

        authenticationUsernameLabel.setText("Username:");

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

        authenticationPasswordLabel.setText("Password:");

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

        listCutMenuItem.setText("Cut");
        listCutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                listCutMenuItemActionPerformed(evt);
            }
        });
        listPopupMenu.add(listCutMenuItem);

        listCopyMenuItem.setText("Copy");
        listCopyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                listCopyMenuItemActionPerformed(evt);
            }
        });
        listPopupMenu.add(listCopyMenuItem);

        listDeleteMenuItem.setText("Delete");
        listDeleteMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                listDeleteMenuItemActionPerformed(evt);
            }
        });
        listPopupMenu.add(listDeleteMenuItem);
        listPopupMenu.add(listPopupMenuSeparator1);

        listSelectAllMenuItem.setText("Select All");
        listSelectAllMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                listSelectAllMenuItemActionPerformed(evt);
            }
        });
        listPopupMenu.add(listSelectAllMenuItem);

        splashScreen.progress();

        hideMenuItem.setText("Hide");
        hideMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hideMenuItemActionPerformed(evt);
            }
        });
        connectionIssueButtonPopupMenu.add(hideMenuItem);

        splashScreen.progress();

        viewNewHighQualityMoviesMenuItem.setText("View New " + Str.capitalize(HQ_FORMAT) + "Movies");
        viewNewHighQualityMoviesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                viewNewHighQualityMoviesMenuItemActionPerformed(evt);
            }
        });
        popularMoviesButtonPopupMenu.add(viewNewHighQualityMoviesMenuItem);

        splashScreen.progress();

        exitBackupModeMenuItem.setText("Exit Backup Mode");
        exitBackupModeMenuItem.setEnabled(false);
        exitBackupModeMenuItem.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                exitBackupModeMenuItemMouseReleased(evt);
            }
        });
        exitBackupModeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                exitBackupModeMenuItemActionPerformed(evt);
            }
        });
        exitBackupModePopupMenu.add(exitBackupModeMenuItem);

        splashScreen.progress();

        readSummaryCancelMenuItem.setText("Stop");
        readSummaryCancelMenuItem.setEnabled(false);
        readSummaryCancelMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readSummaryCancelMenuItemActionPerformed(evt);
            }
        });
        readSummaryButtonPopupMenu.add(readSummaryCancelMenuItem);

        watchTrailerCancelMenuItem.setText("Stop");
        watchTrailerCancelMenuItem.setEnabled(false);
        watchTrailerCancelMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchTrailerCancelMenuItemActionPerformed(evt);
            }
        });
        watchTrailerButtonPopupMenu.add(watchTrailerCancelMenuItem);

        downloadLinkCancelMenuItem.setText("Stop");
        downloadLinkCancelMenuItem.setEnabled(false);
        downloadLinkCancelMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadLinkCancelMenuItemActionPerformed(evt);
            }
        });
        downloadLinkButtonPopupMenu.add(downloadLinkCancelMenuItem);

        downloadLinkExitBackupModeMenuItem.setText("Exit Backup Mode");
        downloadLinkExitBackupModeMenuItem.setEnabled(false);
        downloadLinkExitBackupModeMenuItem.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                downloadLinkExitBackupModeMenuItemMouseReleased(evt);
            }
        });
        downloadLinkExitBackupModeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadLinkExitBackupModeMenuItemActionPerformed(evt);
            }
        });
        downloadLinkButtonPopupMenu.add(downloadLinkExitBackupModeMenuItem);

        watchSourceCancelMenuItem.setText("Stop");
        watchSourceCancelMenuItem.setEnabled(false);
        watchSourceCancelMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchSourceCancelMenuItemActionPerformed(evt);
            }
        });
        watchSourceButtonPopupMenu.add(watchSourceCancelMenuItem);

        playlistFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        playlistFrame.setTitle("Playlist");
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
                "", "", "", ""
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
            playlistTable.getColumnModel().getColumn(0).setHeaderValue(Constant.PLAYLIST_NAME_COL);
            playlistTable.getColumnModel().getColumn(1).setPreferredWidth(70);
            playlistTable.getColumnModel().getColumn(1).setHeaderValue(Constant.PLAYLIST_SIZE_COL);
            playlistTable.getColumnModel().getColumn(2).setPreferredWidth(158);
            playlistTable.getColumnModel().getColumn(2).setHeaderValue(Constant.PLAYLIST_PROGRESS_COL);
            playlistTable.getColumnModel().getColumn(3).setPreferredWidth(0);
            playlistTable.getColumnModel().getColumn(3).setHeaderValue(Constant.PLAYLIST_ITEM_COL);
        }

        String playlistPlayToolTip = "play/stop (" + CTRL_CLICK + "force play)";
        playlistPlayButton.setText(null);
        playlistPlayButton.setToolTipText(playlistPlayToolTip);
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

        playlistMoveUpButton.setText(null);
        playlistMoveUpButton.setToolTipText("move up");
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
        playlistMoveDownButton.setToolTipText("move down");
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
        playlistRemoveButton.setToolTipText("remove");
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
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addComponent(playlistScrollPane, GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(playlistFrameLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(playlistPlayButton)
                    .addComponent(playlistMoveUpButton)
                    .addComponent(playlistMoveDownButton)
                    .addComponent(playlistRemoveButton))
                .addContainerGap())
        );

        playlistPlayMenuItem.setToolTipText(playlistPlayToolTip);
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

        playlistOpenMenuItem.setText("Open");
        playlistOpenMenuItem.setEnabled(false);
        playlistOpenMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistOpenMenuItemActionPerformed(evt);
            }
        });
        playlistTablePopupMenu.add(playlistOpenMenuItem);

        playlistMoveUpMenuItem.setText("Move Up");
        playlistMoveUpMenuItem.setEnabled(false);
        playlistMoveUpMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistMoveUpMenuItemActionPerformed(evt);
            }
        });
        playlistTablePopupMenu.add(playlistMoveUpMenuItem);

        playlistMoveDownMenuItem.setText("Move Down");
        playlistMoveDownMenuItem.setEnabled(false);
        playlistMoveDownMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistMoveDownMenuItemActionPerformed(evt);
            }
        });
        playlistTablePopupMenu.add(playlistMoveDownMenuItem);
        playlistTablePopupMenu.add(playlistTablePopupMenuSeparator1);

        playlistCopyMenuItem.setText("Copy");
        playlistCopyMenuItem.setEnabled(false);
        playlistCopyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistCopyMenuItemActionPerformed(evt);
            }
        });
        playlistTablePopupMenu.add(playlistCopyMenuItem);
        playlistTablePopupMenu.add(playlistTablePopupMenuSeparator2);

        playlistRemoveMenuItem.setText("Remove");
        playlistRemoveMenuItem.setEnabled(false);
        playlistRemoveMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistRemoveMenuItemActionPerformed(evt);
            }
        });
        playlistTablePopupMenu.add(playlistRemoveMenuItem);

        activationDialog.setTitle("Activation");
        activationDialog.setAlwaysOnTop(true);
        activationDialog.setIconImage(null);
        activationDialog.setModal(true);
        activationDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                activationDialogWindowClosing(evt);
            }
        });

        String upgrade = "to unlimited easy play of movies and TV shows";
        activationUpgradeButton.setText("Upgrade");
        activationUpgradeButton.setToolTipText("upgrade " + upgrade);
        activationUpgradeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                activationUpgradeButtonActionPerformed(evt);
            }
        });

        activationUpgradeLabel.setText(upgrade + ".");

        activationCodeLabel.setText("Activation Code:");

        activationButton.setText("OK");
        activationButton.setToolTipText("check activation code");
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

        activationDialog.pack();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(Constant.APP_TITLE);
        setMinimumSize(null);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        titleTextField.setText(null);
        titleTextField.setToolTipText(null);

        titleLabel.setLabelFor(titleTextField);
        titleLabel.setText("Title:");
        titleLabel.setToolTipText("enter title");

        releasedLabel.setText("Released:");
        releasedLabel.setToolTipText("select release date");

        genreLabel.setLabelFor(genreScrollPane);
        genreLabel.setText("Genre:");
        genreLabel.setToolTipText("select genre");

        ratingComboBox.setMaximumRowCount(11);
        ratingComboBox.setToolTipText(null);

        ratingLabel.setLabelFor(ratingComboBox);
        ratingLabel.setText("Rating (minimum):");
        ratingLabel.setToolTipText("select minimum rating");

        resultsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setAutoscrolls(true);

        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", "", "", "", "", "", "", "", "", "", "", "", ""
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
            resultsTable.getColumnModel().getColumn(1).setHeaderValue(Constant.TITLE_COL);
            resultsTable.getColumnModel().getColumn(2).setMinWidth(65);
            resultsTable.getColumnModel().getColumn(2).setPreferredWidth(65);
            resultsTable.getColumnModel().getColumn(2).setMaxWidth(65);
            resultsTable.getColumnModel().getColumn(2).setHeaderValue(Constant.YEAR_COL);
            resultsTable.getColumnModel().getColumn(3).setMinWidth(65);
            resultsTable.getColumnModel().getColumn(3).setPreferredWidth(65);
            resultsTable.getColumnModel().getColumn(3).setMaxWidth(65);
            resultsTable.getColumnModel().getColumn(3).setHeaderValue(Constant.RATING_COL);
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

        progressBar.setStringPainted(true);

        progressBarLabel.setLabelFor(progressBar);
        progressBarLabel.setText("Search Progress:");

        resultsLabel.setText("Results: 0");
        resultsLabel.setToolTipText("number of titles found");

        searchButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        searchButton.setText("Search");
        searchButton.setToolTipText("start search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        stopButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        stopButton.setText("Stop");
        stopButton.setToolTipText("stop search");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        anyTitleCheckBox.setText("Any");
        anyTitleCheckBox.setToolTipText("check to search for all titles");
        anyTitleCheckBox.setBorder(null);
        anyTitleCheckBox.setFocusPainted(false);
        anyTitleCheckBox.setMargin(new Insets(2, 0, 2, 2));
        anyTitleCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                anyTitleCheckBoxActionPerformed(evt);
            }
        });

        genreList.setToolTipText(null);
        genreList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                genreListValueChanged(evt);
            }
        });
        genreScrollPane.setViewportView(genreList);

        loadMoreResultsButton.setText("Load More");
        loadMoreResultsButton.setToolTipText("load more of the remaining search results");
        loadMoreResultsButton.setEnabled(false);
        loadMoreResultsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                loadMoreResultsButtonActionPerformed(evt);
            }
        });

        typeLabel.setLabelFor(typeComboBox);
        typeLabel.setText("Type:");
        typeLabel.setToolTipText("select title type");

        typeComboBox.setModel(new DefaultComboBoxModel(new String[]{"Movie", Constant.TV_SHOW}));

        releasedToLabel.setText("to");
        releasedToLabel.setToolTipText(null);

        linkProgressBar.setToolTipText(null);
        linkProgressBar.setRequestFocusEnabled(false);
        linkProgressBar.setString("Searching");

        hqVideoTypeCheckBox.setText(Constant.HQ);
        hqVideoTypeCheckBox.setToolTipText(HQ_FORMAT + "video formats for the download links");
        hqVideoTypeCheckBox.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                hqVideoTypeCheckBoxMousePressed(evt);
            }
        });
        hqVideoTypeCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hqVideoTypeCheckBoxActionPerformed(evt);
            }
        });

        dvdCheckBox.setText(Constant.DVD);
        dvdCheckBox.setToolTipText("DVD video format for the download links");
        dvdCheckBox.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                dvdCheckBoxMousePressed(evt);
            }
        });
        dvdCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dvdCheckBoxActionPerformed(evt);
            }
        });

        hd720CheckBox.setText(Constant.HD720);
        hd720CheckBox.setToolTipText("720p high-definition video format for the download links");
        hd720CheckBox.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                hd720CheckBoxMousePressed(evt);
            }
        });
        hd720CheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hd720CheckBoxActionPerformed(evt);
            }
        });

        hd1080CheckBox.setText(Constant.HD1080);
        hd1080CheckBox.setToolTipText("1080i/p high-definition video format for the download links");
        hd1080CheckBox.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                hd1080CheckBoxMousePressed(evt);
            }
        });
        hd1080CheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hd1080CheckBoxActionPerformed(evt);
            }
        });

        popularMoviesButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        popularMoviesButton.setText("Popular Movies");
        popularMoviesButton.setToolTipText("view most downloaded movies (" + CTRL_CLICK + "view new " + HQ_FORMAT + "movies)");
        popularMoviesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                popularMoviesButtonActionPerformed(evt);
            }
        });

        popularTVShowsButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        popularTVShowsButton.setText("Popular TV Shows");
        popularTVShowsButton.setToolTipText("view most downloaded television shows");
        popularTVShowsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                popularTVShowsButtonActionPerformed(evt);
            }
        });

        closeBoxButton.setText(null);
        closeBoxButton.setToolTipText("stop play, download, and watch searches");
        closeBoxButton.setEnabled(false);
        closeBoxButton.setMargin(new Insets(0, 0, 0, 0));
        closeBoxButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeBoxButtonActionPerformed(evt);
            }
        });

        loadingLabel.setText(null);

        readSummaryButton.setText("Read Summary");
        readSummaryButton.setToolTipText("read summary of title");
        readSummaryButton.setEnabled(false);
        readSummaryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readSummaryButtonActionPerformed(evt);
            }
        });

        watchTrailerButton.setText("Watch Trailer");
        watchTrailerButton.setToolTipText("watch trailer of title");
        watchTrailerButton.setEnabled(false);
        watchTrailerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchTrailerButtonActionPerformed(evt);
            }
        });

        playButton.setText("Play");
        playButton.setToolTipText("play title in default media player (" + exitBackupModeToolTip + ")");
        playButton.setEnabled(false);
        playButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                playButtonMousePressed(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                playButtonMouseReleased(evt);
            }
        });
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playButtonActionPerformed(evt);
            }
        });

        downloadLink1Button.setText("Download (Link 1)");
        downloadLink1Button.setToolTipText("download title (" + exitBackupModeToolTip + ")");
        downloadLink1Button.setEnabled(false);
        downloadLink1Button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                downloadLink1ButtonMousePressed(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                downloadLink1ButtonMouseReleased(evt);
            }
        });
        downloadLink1Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadLink1ButtonActionPerformed(evt);
            }
        });

        downloadLink2Button.setText("Download (Link 2)");
        downloadLink2Button.setToolTipText("download title (" + exitBackupModeToolTip + ")");
        downloadLink2Button.setEnabled(false);
        downloadLink2Button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                downloadLink2ButtonMousePressed(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                downloadLink2ButtonMouseReleased(evt);
            }
        });
        downloadLink2Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadLink2ButtonActionPerformed(evt);
            }
        });

        watchSource1Button.setText("Watch (Source 1)");
        watchSource1Button.setToolTipText("watch title");
        watchSource1Button.setEnabled(false);
        watchSource1Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchSource1ButtonActionPerformed(evt);
            }
        });

        watchSource2Button.setText("Watch (Source 2)");
        watchSource2Button.setToolTipText("watch title");
        watchSource2Button.setEnabled(false);
        watchSource2Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                watchSource2ButtonActionPerformed(evt);
            }
        });

        statusBarTextField.setEditable(false);
        statusBarTextField.setFont(new Font("Verdana", 0, 10)); // NOI18N
        statusBarTextField.setText(null);
        statusBarTextField.setBorder(BorderFactory.createEtchedBorder());

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

        fileMenu.setText("File");
        splashScreen.progress();

        useProfileMenu.setText("Use Profile");
        useProfileMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent evt) {
            }
            public void menuDeselected(MenuEvent evt) {
            }
            public void menuSelected(MenuEvent evt) {
                useProfileMenuMenuSelected(evt);
            }
        });

        profile0MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_MASK));
        profile0MenuItem.setText("Default Profile");
        profile0MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile0MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile0MenuItem);

        profile1MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_MASK));
        profile1MenuItem.setText("Profile 1");
        profile1MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile1MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile1MenuItem);

        profile2MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_MASK));
        profile2MenuItem.setText("Profile 2");
        profile2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile2MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile2MenuItem);

        profile3MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_MASK));
        profile3MenuItem.setText("Profile 3");
        profile3MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile3MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile3MenuItem);

        profile4MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_MASK));
        profile4MenuItem.setText("Profile 4");
        profile4MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile4MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile4MenuItem);

        profile5MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.CTRL_MASK));
        profile5MenuItem.setText("Profile 5");
        profile5MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile5MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile5MenuItem);

        profile6MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, InputEvent.CTRL_MASK));
        profile6MenuItem.setText("Profile 6");
        profile6MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile6MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile6MenuItem);

        profile7MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_MASK));
        profile7MenuItem.setText("Profile 7");
        profile7MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile7MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile7MenuItem);

        profile8MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.CTRL_MASK));
        profile8MenuItem.setText("Profile 8");
        profile8MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile8MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile8MenuItem);

        profile9MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.CTRL_MASK));
        profile9MenuItem.setText("Profile 9");
        profile9MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                profile9MenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(profile9MenuItem);
        useProfileMenu.add(useProfileMenuSeparator1);

        editProfilesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK | InputEvent.CTRL_MASK));
        editProfilesMenuItem.setText("Edit Profiles");
        editProfilesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                editProfilesMenuItemActionPerformed(evt);
            }
        });
        useProfileMenu.add(editProfilesMenuItem);

        fileMenu.add(useProfileMenu);
        fileMenu.add(fileMenuSeparator1);

        printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK));
        printMenuItem.setText("Print");
        printMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                printMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(printMenuItem);
        fileMenu.add(fileMenuSeparator2);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");
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
        cutMenuItem.setText("Cut");
        cutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cutMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(cutMenuItem);

        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        copyMenuItem.setText("Copy");
        copyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copyMenuItem);

        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
        pasteMenuItem.setText("Paste");
        pasteMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pasteMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteMenuItem.setText("Delete");
        deleteMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                deleteMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(deleteMenuItem);
        editMenu.add(editMenuSeparator1);

        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        selectAllMenuItem.setText("Select All");
        selectAllMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                selectAllMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(selectAllMenuItem);
        editMenu.add(editMenuSeparator2);

        findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
        findMenuItem.setText("Find");
        findMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                findMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(findMenuItem);

        menuBar.add(editMenu);

        viewMenu.setText("View");

        resetWindowMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
        resetWindowMenuItem.setText("Reset Window");
        resetWindowMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                resetWindowMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(resetWindowMenuItem);

        menuBar.add(viewMenu);

        searchMenu.setText("Search");

        resultsPerSearchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
        resultsPerSearchMenuItem.setText("Set Number of Results Per Search");
        resultsPerSearchMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                resultsPerSearchMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(resultsPerSearchMenuItem);
        searchMenu.add(searchMenuSeparator1);

        timeoutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
        timeoutMenuItem.setText("Set Connection Timeout");
        timeoutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                timeoutMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(timeoutMenuItem);
        searchMenu.add(searchMenuSeparator2);

        proxyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
        proxyMenuItem.setText("Manage Proxies");
        proxyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                proxyMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(proxyMenuItem);
        searchMenu.add(searchMenuSeparator3);

        languageCountryMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
        languageCountryMenuItem.setText("Set Language/Country");
        languageCountryMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                languageCountryMenuItemActionPerformed(evt);
            }
        });
        searchMenu.add(languageCountryMenuItem);
        searchMenu.add(searchMenuSeparator4);

        feedCheckBoxMenuItem.setText("Show New HQ Movies on Startup");
        feedCheckBoxMenuItem.setToolTipText("show new " + HQ_FORMAT + "movies on startup");
        searchMenu.add(feedCheckBoxMenuItem);
        searchMenu.add(searchMenuSeparator5);

        browserNotificationCheckBoxMenuItem.setSelected(true);
        browserNotificationCheckBoxMenuItem.setText("Show Web Browser Start Notification");
        searchMenu.add(browserNotificationCheckBoxMenuItem);
        searchMenu.add(searchMenuSeparator6);

        emailWithDefaultAppCheckBoxMenuItem.setSelected(true);
        emailWithDefaultAppCheckBoxMenuItem.setText("Email with Default Application");
        searchMenu.add(emailWithDefaultAppCheckBoxMenuItem);

        menuBar.add(searchMenu);

        playlistMenu.setText("Playlist");

        playlistMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
        playlistMenuItem.setText("Show Playlist");
        playlistMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistMenuItemActionPerformed(evt);
            }
        });
        playlistMenu.add(playlistMenuItem);
        playlistMenu.add(playlistMenuSeparator1);

        playlistSaveFolderMenuItem.setText("Set Save Folder");
        playlistSaveFolderMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistSaveFolderMenuItemActionPerformed(evt);
            }
        });
        playlistMenu.add(playlistSaveFolderMenuItem);

        playlistAutoOpenCheckBoxMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        playlistAutoOpenCheckBoxMenuItem.setSelected(true);
        playlistAutoOpenCheckBoxMenuItem.setText("Auto-Open Items");
        playlistMenu.add(playlistAutoOpenCheckBoxMenuItem);

        playlistShowNonVideoItemsCheckBoxMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        playlistShowNonVideoItemsCheckBoxMenuItem.setText("Show Non-Video Items");
        playlistShowNonVideoItemsCheckBoxMenuItem.setToolTipText("show non-video items (" + Str.get(684)+ ")");
        playlistShowNonVideoItemsCheckBoxMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                playlistShowNonVideoItemsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        playlistMenu.add(playlistShowNonVideoItemsCheckBoxMenuItem);

        playlistTipsCheckBoxMenuItem.setSelected(true);
        playlistTipsCheckBoxMenuItem.setText("Show Tips");
        playlistMenu.add(playlistTipsCheckBoxMenuItem);

        menuBar.add(playlistMenu);

        downloadMenu.setText("Download");

        downloadSizeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        downloadSizeMenuItem.setText("Set Download Size");
        downloadSizeMenuItem.setToolTipText(null);
        downloadSizeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadSizeMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(downloadSizeMenuItem);
        downloadMenu.add(downloadMenuSeparator1);

        fileExtensionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
        fileExtensionsMenuItem.setText("Set Video File Extensions");
        fileExtensionsMenuItem.setToolTipText(null);
        fileExtensionsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fileExtensionsMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(fileExtensionsMenuItem);
        downloadMenu.add(downloadMenuSeparator2);

        safetyCheckBoxMenuItem.setSelected(true);
        safetyCheckBoxMenuItem.setText("Show Link Safety Warning");
        safetyCheckBoxMenuItem.setToolTipText("warn before downloading from unsafe source");
        downloadMenu.add(safetyCheckBoxMenuItem);

        peerBlockNotificationCheckBoxMenuItem.setSelected(true);
        peerBlockNotificationCheckBoxMenuItem.setText("Show " + Constant.PEER_BLOCK_APP_TITLE + " Notification");
        downloadMenu.add(peerBlockNotificationCheckBoxMenuItem);
        downloadMenu.add(downloadMenuSeparator3);

        portMenuItem.setText("Set Incoming Listen Port");
        portMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                portMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(portMenuItem);
        downloadMenu.add(downloadMenuSeparator4);

        downloadWithDefaultAppCheckBoxMenuItem.setText("Download with Default Application");
        downloadWithDefaultAppCheckBoxMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downloadWithDefaultAppCheckBoxMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(downloadWithDefaultAppCheckBoxMenuItem);

        autoDownloadingCheckBoxMenuItem.setSelected(true);
        autoDownloadingCheckBoxMenuItem.setText("Enable Auto-Downloading");
        autoDownloadingCheckBoxMenuItem.setToolTipText("start downloads in web browser");
        autoDownloadingCheckBoxMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                autoDownloadingCheckBoxMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(autoDownloadingCheckBoxMenuItem);

        downloaderMenu.setText("Select Auto-Downloader");

        defaultRadioButtonMenuItem.setSelected(true);
        defaultRadioButtonMenuItem.setText("Default");
        downloaderMenu.add(defaultRadioButtonMenuItem);

        customRadioButtonMenuItem.setText(Constant.APP_TITLE + "'s");
        downloaderMenu.add(customRadioButtonMenuItem);

        downloadMenu.add(downloaderMenu);

        menuBar.add(downloadMenu);

        helpMenu.setText("Help");

        faqMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
        faqMenuItem.setText("FAQ");
        faqMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                faqMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(faqMenuItem);
        helpMenu.add(helpMenuSeparator1);

        activationMenuItem.setText("Activation");
        activationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                activationMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(activationMenuItem);

        updateMenuItem.setText("Check for Updates");
        updateMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                updateMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(updateMenuItem);

        updateCheckBoxMenuItem.setSelected(true);
        updateCheckBoxMenuItem.setText("Show Update Notification");
        updateCheckBoxMenuItem.setToolTipText(null);
        helpMenu.add(updateCheckBoxMenuItem);
        helpMenu.add(helpMenuSeparator2);

        aboutMenuItem.setText("About");
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
            .addComponent(statusBarTextField, GroupLayout.DEFAULT_SIZE, 1329, Short.MAX_VALUE)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(resultsScrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 1309, Short.MAX_VALUE)
                    .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(progressBarLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED, 146, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(dvdCheckBox)
                            .addComponent(hqVideoTypeCheckBox))
                        .addGap(0, 0, 0)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(hd720CheckBox)
                            .addComponent(hd1080CheckBox))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(linkProgressBar, GroupLayout.PREFERRED_SIZE, 258, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(closeBoxButton)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(loadMoreResultsButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(resultsLabel))
                    .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(popularMoviesButton)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(popularTVShowsButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 963, Short.MAX_VALUE)
                        .addComponent(loadingLabel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(anyTitleCheckBox, GroupLayout.PREFERRED_SIZE, 44, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(titleLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(titleTextField, GroupLayout.DEFAULT_SIZE, 827, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(typeLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(typeComboBox, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 20, Short.MAX_VALUE)
                                .addComponent(ratingLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(ratingComboBox, GroupLayout.PREFERRED_SIZE, 59, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 21, Short.MAX_VALUE)
                                .addComponent(releasedLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(startDateChooser, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                                .addGap(7, 7, 7)
                                .addComponent(releasedToLabel)
                                .addGap(6, 6, 6)
                                .addComponent(endDateChooser, GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addComponent(genreLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(genreScrollPane, GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                            .addComponent(searchButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(stopButton, GroupLayout.PREFERRED_SIZE, 71, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(readSummaryButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(watchTrailerButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(playButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(downloadLink1Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(downloadLink2Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(watchSource1Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(watchSource2Button)
                        .addGap(18, 18, 18)
                        .addComponent(findTextField, GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(connectionIssueButton)))
                .addContainerGap())
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {dvdCheckBox, hqVideoTypeCheckBox});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {hd1080CheckBox, hd720CheckBox});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {searchButton, stopButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {downloadLink1Button, downloadLink2Button, playButton, readSummaryButton, watchSource1Button, watchSource2Button, watchTrailerButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {ratingComboBox, typeComboBox});

        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(popularMoviesButton, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                        .addComponent(popularTVShowsButton))
                    .addComponent(loadingLabel))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(anyTitleCheckBox)
                            .addComponent(titleLabel)
                            .addComponent(titleTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(genreLabel))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(typeComboBox)
                            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(typeLabel)
                                .addComponent(ratingLabel)
                                .addComponent(ratingComboBox)
                                .addComponent(releasedLabel)
                                .addComponent(releasedToLabel))
                            .addComponent(startDateChooser, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(endDateChooser, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(searchButton, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(stopButton))
                    .addComponent(genreScrollPane, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(resultsScrollPane, GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(readSummaryButton)
                    .addComponent(watchTrailerButton)
                    .addComponent(downloadLink1Button)
                    .addComponent(downloadLink2Button)
                    .addComponent(watchSource1Button)
                    .addComponent(watchSource2Button)
                    .addComponent(connectionIssueButton)
                    .addComponent(findTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(playButton))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(Alignment.CENTER)
                    .addComponent(resultsLabel)
                    .addComponent(loadMoreResultsButton)
                    .addComponent(closeBoxButton)
                    .addComponent(linkProgressBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(hd720CheckBox)
                        .addGap(0, 0, 0)
                        .addComponent(hd1080CheckBox))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(hqVideoTypeCheckBox)
                        .addGap(0, 0, 0)
                        .addComponent(dvdCheckBox))
                    .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                    .addComponent(progressBarLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(statusBarTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {anyTitleCheckBox, endDateChooser, ratingComboBox, startDateChooser, titleTextField, typeComboBox});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {genreLabel, ratingLabel, releasedLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {progressBarLabel, resultsLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {linkProgressBar, loadMoreResultsButton, progressBar});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {searchButton, stopButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {downloadLink1Button, downloadLink2Button, findTextField, playButton, readSummaryButton, watchSource1Button, watchSource2Button, watchTrailerButton});

        setSize(new Dimension(1337, 773));
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

        StringBuilder profiles = new StringBuilder(128);
        for (int i = 1, numProfiles = profileComboBox.getItemCount(); i < numProfiles; i++) {
            profiles.append(profileComboBox.getItemAt(i)).append(Constant.NEWLINE);
        }
        try {
            IO.write(Constant.APP_DIR + Constant.PROFILES, profiles.toString().trim());
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
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

    void searchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
        hideFindTextField();

        int numResultsPerSearch = Integer.parseInt((String) regularResultsPerSearchComboBox.getSelectedItem());
        isTVShowSearch = Constant.TV_SHOW.equals(typeComboBox.getSelectedItem());
        Calendar startDate = startDateChooser.getCalendar(), endDate = endDateChooser.getCalendar();
        if (startDate != null) {
            startDate = (Calendar) startDate.clone();
        }
        if (endDate != null) {
            endDate = (Calendar) endDate.clone();
        }

        String title = titleTextField.getText();
        if (anyTitleCheckBox.isSelected() || title == null || (title = title.trim()).isEmpty()) {
            titleTextField.setText(null);
            titleTextField.setEnabled(false);
            anyTitleCheckBox.setSelected(true);
            title = "";
        }

        String[] genres = UI.copy(genreList), languages = UI.copy(languageList), countries = UI.copy(countryList);
        String minRating = (String) ratingComboBox.getSelectedItem();

        UI.enable(false, loadMoreResultsButton, searchButton, popularTVShowsButton, popularMoviesButton, viewNewHighQualityMoviesMenuItem);

        isRegularSearcher = true;
        workerListener.regularSearchStarted(numResultsPerSearch, isTVShowSearch, startDate, endDate, title, genres, languages, countries, minRating);
    }//GEN-LAST:event_searchButtonActionPerformed

    void stopButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        stopButton.setEnabled(false);
        workerListener.searchStopped(isRegularSearcher);
    }//GEN-LAST:event_stopButtonActionPerformed

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
        faqFrame.setVisible(true);
    }//GEN-LAST:event_faqMenuItemActionPerformed

    void aboutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        aboutEditorPane.setSelectionStart(0);
        aboutEditorPane.setSelectionEnd(0);
        aboutDialog.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    void exitMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_exitMenuItemActionPerformed

    void printMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_printMenuItemActionPerformed
        if (resultsSyncTable.getRowCount() == 0) {
            showMsg("There are no search results to print.", Constant.INFO_MSG);
            return;
        }
        printMenuItem.setEnabled(false);
        printMenuItem.setText("Preparing to Print...");
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
                printMenuItem.setText("Print");
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
        if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH && !UI.isMaxSize(dimension)) {
            setExtendedState(Frame.NORMAL);
        }
        setSize(dimension);
        UI.centerOnScreen(this);
        maximize();
    }//GEN-LAST:event_resetWindowMenuItemActionPerformed

    void anyTitleCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_anyTitleCheckBoxActionPerformed
        if (anyTitleCheckBox.isSelected()) {
            titleTextField.setText(null);
            titleTextField.setEnabled(false);
        } else {
            titleTextField.setEnabled(true);
            titleTextField.requestFocusInWindow();
        }
    }//GEN-LAST:event_anyTitleCheckBoxActionPerformed

    void timeoutMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_timeoutMenuItemActionPerformed
        resultsToBackground(true);
        timeoutDialog.setVisible(true);
    }//GEN-LAST:event_timeoutMenuItemActionPerformed

    void tvSubmitButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubmitButtonActionPerformed
        cancelTVSelection = false;
        tvDialog.setVisible(false);
    }//GEN-LAST:event_tvSubmitButtonActionPerformed

    void tvEpisodeComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvEpisodeComboBoxActionPerformed
        updatedTVComboBoxes();
    }//GEN-LAST:event_tvEpisodeComboBoxActionPerformed

    void tvSeasonComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSeasonComboBoxActionPerformed
        updatedTVComboBoxes();
    }//GEN-LAST:event_tvSeasonComboBoxActionPerformed

    void tvCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvCancelButtonActionPerformed
        tvDialog.setVisible(false);
    }//GEN-LAST:event_tvCancelButtonActionPerformed

    void resultsPerSearchMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_resultsPerSearchMenuItemActionPerformed
        resultsToBackground(true);
        resultsPerSearchDialog.setVisible(true);
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
        if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
            showFeed(false);
        } else {
            doPopularVideosSearch(false);
        }
    }//GEN-LAST:event_popularMoviesButtonActionPerformed

    private void doPopularVideosSearch(boolean isPopularTVShows) {
        hideFindTextField();
        isTVShowSearch = isPopularTVShows;
        isRegularSearcher = false;
        int numResultsPerSearch = Integer.parseInt((String) (isPopularTVShows ? popularTVShowsResultsPerSearchComboBox.getSelectedItem()
                : popularMoviesResultsPerSearchComboBox.getSelectedItem()));
        String[] languages = UI.copy(languageList), countries = UI.copy(countryList);
        workerListener.popularSearchStarted(numResultsPerSearch, isPopularTVShows, languages, countries, false, true);
    }

    void downloadSizeMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadSizeMenuItemActionPerformed
        resultsToBackground(true);
        updateDownloadSizeComboBoxes();
        downloadSizeDialog.setVisible(true);
    }//GEN-LAST:event_downloadSizeMenuItemActionPerformed

    void maxDownloadSizeComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_maxDownloadSizeComboBoxActionPerformed
        updateDownloadSizeComboBoxes();
    }//GEN-LAST:event_maxDownloadSizeComboBoxActionPerformed

    void popularTVShowsButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_popularTVShowsButtonActionPerformed
        doPopularVideosSearch(true);
    }//GEN-LAST:event_popularTVShowsButtonActionPerformed

    void fileExtensionsMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_fileExtensionsMenuItemActionPerformed
        resultsToBackground(true);
        customExtensionTextField.requestFocusInWindow();
        extensionsDialog.setVisible(true);
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
                showMsg("Custom extensions must be 1-20 letters, digits, or hyphens.", Constant.ERROR_MSG);
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
        workerListener.summaryReadStopped();
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
        profileDialog.setVisible(true);
    }//GEN-LAST:event_editProfilesMenuItemActionPerformed

    void closeBoxButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_closeBoxButtonActionPerformed
        boolean enable = false;
        linkProgressBar.setIndeterminate(enable);
        linkProgressBar.setStringPainted(enable);
        UI.enable(enable, closeBoxButton, downloadLinkCancelMenuItem, watchSourceCancelMenuItem);
        workerListener.torrentSearchStopped();
        workerListener.streamSearchStopped();
    }//GEN-LAST:event_closeBoxButtonActionPerformed

    void resultsTableValueChanged(ListSelectionEvent evt) {
        if (!evt.getSource().equals(resultsSyncTable.getSelectionModel())) {
            return;
        }

        boolean enable = false;
        UI.enable(enable, findSubtitleMenuItem, emailEverythingMenuItem, copySubtitleLinkMenuItem);
        enableWatch(enable);
        enableDownload(enable);
        UI.enable(enable, watchTrailerButton, watchTrailerMenuItem, emailTrailerLinkMenuItem, copyTrailerLinkMenuItem, readSummaryButton, readSummaryMenuItem,
                emailSummaryLinkMenuItem, copySummaryLinkMenuItem, copyPosterImageMenuItem, copyFullTitleAndYearMenuItem, copySelectionMenuItem);

        if (evt.getFirstIndex() < 0 || resultsSyncTable.getSelectedRows().length != 1) {
            return;
        }

        enable = true;
        boolean isStreamSearchDone = workerListener.isStreamSearchDone(), isTorrentSearchDone = workerListener.isTorrentSearchDone(),
                isTrailerSearchDone = workerListener.isTrailerSearchDone(), isSummarySearchDone = workerListener.isSummarySearchDone();

        findSubtitleMenuItem.setEnabled(enable);
        if (isStreamSearchDone && isTorrentSearchDone && isTrailerSearchDone && isSummarySearchDone) {
            emailEverythingMenuItem.setEnabled(enable);
        }
        copySubtitleLinkMenuItem.setEnabled(enable);
        if (isStreamSearchDone) {
            enableWatch(enable);
        }
        if (isTorrentSearchDone) {
            enableDownload(enable);
        }
        if (isTrailerSearchDone) {
            UI.enable(enable, watchTrailerButton, watchTrailerMenuItem, emailTrailerLinkMenuItem, copyTrailerLinkMenuItem);
        }
        UI.enable(enable, emailSummaryLinkMenuItem, copySummaryLinkMenuItem, copyPosterImageMenuItem);
        if (isSummarySearchDone) {
            UI.enable(enable, readSummaryButton, readSummaryMenuItem, copyFullTitleAndYearMenuItem);
        }
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
        hideFindTextField();
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
        readSummaryActionPerformed(selectedRow(), null);
    }//GEN-LAST:event_readSummaryButtonActionPerformed

    void readSummaryActionPerformed(SelectedTableRow row, VideoStrExportListener strExportListener) {
        workerListener.summarySearchStarted(row.VAL, row.video, strExportListener);
    }

    void watchTrailerButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchTrailerButtonActionPerformed
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

    private void downloadLinkActionPerformed(ContentType downloadContentType, SelectedTableRow row, VideoStrExportListener strExportListener, boolean play) {
        if (row.video.IS_TV_SHOW && !downloadLinkEpisodes.add(row.VAL)) {
            row.video.season = "";
            row.video.episode = "";
        }
        workerListener.torrentSearchStarted(Connection.downloadLinkInfoFail() ? ContentType.DOWNLOAD3 : downloadContentType, row.VAL, row.video, strExportListener,
                play);
    }

    void downloadLink1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink1ButtonActionPerformed
        SelectedTableRow row = selectedRow();
        downloadLinkActionPerformed(ContentType.DOWNLOAD1, row, null, false);
    }//GEN-LAST:event_downloadLink1ButtonActionPerformed

    void downloadLink2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLink2ButtonActionPerformed
        SelectedTableRow row = selectedRow();
        downloadLinkActionPerformed(ContentType.DOWNLOAD2, row, null, false);
    }//GEN-LAST:event_downloadLink2ButtonActionPerformed

    void languageCountryMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_languageCountryMenuItemActionPerformed
        resultsToBackground(true);
        languageCountryDialog.setVisible(true);
    }//GEN-LAST:event_languageCountryMenuItemActionPerformed

    void languageListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_languageListValueChanged
        UI.updateList(languageList);
    }//GEN-LAST:event_languageListValueChanged

    void countryListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_countryListValueChanged
        UI.updateList(countryList);
    }//GEN-LAST:event_countryListValueChanged

    void langaugeCountryOkButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_langaugeCountryOkButtonActionPerformed
        languageCountryDialog.setVisible(false);
    }//GEN-LAST:event_langaugeCountryOkButtonActionPerformed

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
        textComponentPasteSearchMenuItem.setVisible(popupTextComponent == findTextField || popupTextComponent == titleTextField
                || popupTextComponent == startDateTextField || popupTextComponent == endDateTextField);

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
        proxyDialog.setVisible(true);
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
        addProxiesDialog.setVisible(true);
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
            showMsg("There are no proxies to " + type + ".", Constant.INFO_MSG);
            enableProxyButtons(true);
            return numProxies;
        }

        proxyDialog.setVisible(false);
        return numProxies;
    }

    void proxyRemoveButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyRemoveButtonActionPerformed
        int numProxies = exportProxies("remove");
        if (numProxies == 1) {
            return;
        }

        removeProxiesListModel.clear();
        for (int i = 1; i < numProxies; i++) {
            removeProxiesListModel.addElement(proxyComboBox.getItemAt(i));
        }

        removeProxiesDialog.setVisible(true);
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
        proxyDialog.setVisible(true);
    }

    void removeProxiesRemoveButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_removeProxiesRemoveButtonActionPerformed
        removeProxiesRemoveButton.setEnabled(false);

        Object[] selectedProxies = removeProxiesList.getSelectedValues();
        if (selectedProxies.length == 0) {
            showMsg("0 proxies have been removed.", Constant.INFO_MSG);
            restoreProxyDialog(false);
            return;
        }

        for (Object proxy : selectedProxies) {
            proxyComboBox.removeItem(proxy);
        }

        showMsg(selectedProxies.length + (selectedProxies.length == 1 ? " proxy has " : " proxies have ") + "been removed.", Constant.INFO_MSG);

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
        showMsg("0 new proxies have been added.", Constant.INFO_MSG);
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

        int numProxies = proxyComboBox.getItemCount() - 1;
        Collection<String> oldProxies = new ArrayList<String>(numProxies);
        for (int i = 0; i < numProxies; i++) {
            oldProxies.add((String) proxyComboBox.getItemAt(i + 1));
        }

        String[] proxyList = Regex.split(proxies, Constant.STD_NEWLINE);
        Collection<String> validProxies = new ArrayList<String>(proxyList.length);
        for (String proxy : proxyList) {
            String newProxy = proxy.trim();
            if (newProxy.isEmpty()) {
                continue;
            }

            if ((newProxy = Connection.getProxy(newProxy)) == null) {
                showMsg("\'" + proxy + "' is invalid.", Constant.ERROR_MSG);
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
        showMsg(numNewProxies + " new" + (numNewProxies == 1 ? " proxy has " : " proxies have ") + "been added.", Constant.INFO_MSG);

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

        if (torrentFileChooser.isShowing() || subtitleFileChooser.isShowing()) {
            enableProxyButtons(true);
            proxyDialog.setVisible(true);
            return;
        }
        proxyFileChooser.setFileFilter(Regex.proxyListFileFilter);
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
        proxyDialog.setVisible(true);
    }//GEN-LAST:event_proxyImportButtonActionPerformed

    void proxyExportButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_proxyExportButtonActionPerformed
        int numProxies = exportProxies("export");
        if (numProxies == 1) {
            return;
        }

        if (torrentFileChooser.isShowing() || subtitleFileChooser.isShowing()) {
            enableProxyButtons(true);
            proxyDialog.setVisible(true);
            return;
        }
        proxyFileChooser.setFileFilter(Regex.proxyListFileFilter);
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
                showMsg(numProxies + (numProxies == 1 ? " proxy has " : " proxies have ") + "been exported.", Constant.INFO_MSG);
            } catch (Exception e) {
                showException(e);
            }
        }

        enableProxyButtons(true);
        proxyDialog.setVisible(true);
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
            String profileName;
            if (profile == 0) {
                settings.loadSettings(Constant.PROGRAM_DIR + Constant.DEFAULT_SETTINGS);
                profileName = "Default";
            } else {
                settings.loadSettings(Constant.APP_DIR + Constant.PROFILE + profile + Constant.TXT);
                profileName = profileComboBox.getItemAt(profile) + "'s";
            }
            maximize();
            timedMsg(profileName + " settings restored");
        } else {
            showMsg("The profile needs to be set before use.", Constant.ERROR_MSG);
        }
    }

    public void maximize() {
        RootPaneUI rootPaneUI = rootPane.getUI();
        if (UI.isMaxSize(getSize()) && rootPaneUI instanceof SyntheticaRootPaneUI) {
            ((SyntheticaRootPaneUI) rootPaneUI).setMaximizedBounds(this);
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
    }

    void profileComboBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileComboBoxActionPerformed
        updateProfileGUIitems(profileComboBox.getSelectedIndex());
    }//GEN-LAST:event_profileComboBoxActionPerformed

    private void updateProfileGUIitems(int profile) {
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
            showMsg("The profile has been set to the application's current settings.", Constant.INFO_MSG);
        } catch (Exception e) {
            showException(e);
        }
    }//GEN-LAST:event_profileSetButtonActionPerformed

    void useProfileMenuMenuSelected(MenuEvent evt) {//GEN-FIRST:event_useProfileMenuMenuSelected
        profile1MenuItem.setText((String) profileComboBox.getItemAt(1));
        profile2MenuItem.setText((String) profileComboBox.getItemAt(2));
        profile3MenuItem.setText((String) profileComboBox.getItemAt(3));
        profile4MenuItem.setText((String) profileComboBox.getItemAt(4));
        profile5MenuItem.setText((String) profileComboBox.getItemAt(5));
        profile6MenuItem.setText((String) profileComboBox.getItemAt(6));
        profile7MenuItem.setText((String) profileComboBox.getItemAt(7));
        profile8MenuItem.setText((String) profileComboBox.getItemAt(8));
        profile9MenuItem.setText((String) profileComboBox.getItemAt(9));
    }//GEN-LAST:event_useProfileMenuMenuSelected

    void profileRenameButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileRenameButtonActionPerformed
        profileDialog.setVisible(false);
        profileNameChangeTextField.setText("");
        profileNameChangeTextField.requestFocusInWindow();
        profileNameChangeDialog.setVisible(true);
    }//GEN-LAST:event_profileRenameButtonActionPerformed

    void profileNameChangeOKButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileNameChangeOKButtonActionPerformed
        String newProfileName = profileNameChangeTextField.getText().trim();
        if (!Regex.isMatch(newProfileName, "(\\p{Alpha}|\\d|-| ){1,20}+")) {
            showMsg("Profile names must be 1-20 letters, digits, hyphens, or spaces.", Constant.ERROR_MSG);
            return;
        }

        if (newProfileName.equals(Constant.DEFAULT_PROFILE)) {
            showMsg("The name is already in use by another profile. Choose a different name.", Constant.ERROR_MSG);
            return;
        }

        int profile = profileComboBox.getSelectedIndex();
        String[] profileNames = new String[9];
        for (int i = 1; i < 10; i++) {
            if (i == profile) {
                profileNames[i - 1] = newProfileName;
            } else {
                String profileName = (String) profileComboBox.getItemAt(i);
                if (newProfileName.equals(profileName)) {
                    showMsg("The name is already in use by another profile. Choose a different name.", Constant.ERROR_MSG);
                    return;
                }
                profileNames[i - 1] = profileName;
            }
        }

        profileComboBox.removeAllItems();
        profileComboBox.addItem(Constant.DEFAULT_PROFILE);
        for (String profileName : profileNames) {
            profileComboBox.addItem(profileName);
        }
        profileComboBox.setSelectedItem(newProfileName);

        profileNameChangeDialog.setVisible(false);
        profileDialog.setVisible(true);
    }//GEN-LAST:event_profileNameChangeOKButtonActionPerformed

    void profileNameChangeCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileNameChangeCancelButtonActionPerformed
        profileNameChangeDialog.setVisible(false);
        profileDialog.setVisible(true);
    }//GEN-LAST:event_profileNameChangeCancelButtonActionPerformed

    void profileNameChangeDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_profileNameChangeDialogWindowClosing
        profileNameChangeDialog.setVisible(false);
        profileDialog.setVisible(true);
    }//GEN-LAST:event_profileNameChangeDialogWindowClosing

    void profileClearButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileClearButtonActionPerformed
        int profile = profileComboBox.getSelectedIndex();
        IO.fileOp(Constant.APP_DIR + Constant.PROFILE + profile + Constant.TXT, IO.RM_FILE);
        updateProfileGUIitems(profile);
    }//GEN-LAST:event_profileClearButtonActionPerformed

    void profileOKButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_profileOKButtonActionPerformed
        profileDialog.setVisible(false);
    }//GEN-LAST:event_profileOKButtonActionPerformed

    void watchSource1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchSource1ButtonActionPerformed
        watchSourceActionPerformed(ContentType.STREAM1, selectedRow(), null);
    }//GEN-LAST:event_watchSource1ButtonActionPerformed

    private void watchSourceActionPerformed(ContentType streamContentType, SelectedTableRow row, VideoStrExportListener strExportListener) {
        workerListener.streamSearchStarted(streamContentType, row.VAL, row.video, strExportListener);
    }

    void watchSource2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchSource2ButtonActionPerformed
        watchSourceActionPerformed(ContentType.STREAM2, selectedRow(), null);
    }//GEN-LAST:event_watchSource2ButtonActionPerformed

    void watchSource1MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchSource1MenuItemActionPerformed
        watchSource1ButtonActionPerformed(evt);
    }//GEN-LAST:event_watchSource1MenuItemActionPerformed

    void watchSource2MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchSource2MenuItemActionPerformed
        watchSource2ButtonActionPerformed(evt);
    }//GEN-LAST:event_watchSource2MenuItemActionPerformed

    void connectionIssueButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_connectionIssueButtonActionPerformed
        connectionIssueButton.setIcon(noWarningIcon);
        connectionIssueButton.setToolTipText(null);
        connectionIssueButton.setBorderPainted(false);
        connectionIssueButton.setEnabled(false);
        if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
            msgDialogWindowClosing(null);
        } else {
            msgDialog.setVisible(true);
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
            tvSubtitleDialog.setVisible(true);
        } else {
            movieSubtitleDownloadMatch1Button.requestFocusInWindow();
            movieSubtitleDialog.setVisible(true);
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
        portDialog.setVisible(true);
    }//GEN-LAST:event_portMenuItemActionPerformed

    private void portOkButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_portOkButtonActionPerformed
        portDialog.setVisible(false);
    }//GEN-LAST:event_portOkButtonActionPerformed

    private void tvSubtitleCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubtitleCancelButtonActionPerformed
        tvSubtitleCancelButton.setEnabled(false);
        workerListener.subtitleSearchStopped();
    }//GEN-LAST:event_tvSubtitleCancelButtonActionPerformed

    private void startSubtitleSearch(JComboBox format, JComboBox language, JComboBox season, JComboBox episode, boolean firstMatch) {
        subtitleVideo.season = (season == null ? "" : (String) season.getSelectedItem());
        subtitleVideo.episode = (episode == null ? "" : (String) episode.getSelectedItem());
        workerListener.subtitleSearchStarted((String) format.getSelectedItem(), Regex.subtitleLanguages.get((String) language.getSelectedItem()), subtitleVideo,
                firstMatch, subtitleStrExportListener);
    }

    private void tvSubtitleDownloadMatch1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_tvSubtitleDownloadMatch1ButtonActionPerformed
        startSubtitleSearch(tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox, tvSubtitleEpisodeComboBox, true);
    }//GEN-LAST:event_tvSubtitleDownloadMatch1ButtonActionPerformed

    private void movieSubtitleCancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleCancelButtonActionPerformed
        movieSubtitleCancelButton.setEnabled(false);
        workerListener.subtitleSearchStopped();
    }//GEN-LAST:event_movieSubtitleCancelButtonActionPerformed

    private void movieSubtitleDownloadMatch1ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleDownloadMatch1ButtonActionPerformed
        startSubtitleSearch(movieSubtitleFormatComboBox, movieSubtitleLanguageComboBox, null, null, true);
    }//GEN-LAST:event_movieSubtitleDownloadMatch1ButtonActionPerformed

    private void hideFindTextField() {
        if (findTextField.isEnabled()) {
            findTextField.setEnabled(false);
            findTextField.setText(null);
            Color bgColor = getBackground();
            findTextField.setBackground(bgColor);
            findTextField.setForeground(bgColor);
            findTextField.setBorder(null);
            findTitleRow = -2;
        }
    }

    private void findMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_findMenuItemActionPerformed
        if (findTextField.isEnabled()) {
            findTextField.requestFocusInWindow();
            findTextField.selectAll();
        } else {
            findTextField.setBorder(titleTextField.getBorder());
            findTextField.setForeground(titleTextField.getForeground());
            findTextField.requestFocusInWindow();
            findTextField.setEnabled(true);
        }
    }//GEN-LAST:event_findMenuItemActionPerformed

    private void updateFindTitles(String currTitle) {
        String title = Regex.htmlToPlainText(currTitle);
        findTitles.addAll(Arrays.asList(title, Regex.replaceFirst(title, 327), title = Regex.clean(title), Regex.replaceFirst(title, 327)));
    }

    private void findTextFieldKeyPressed(KeyEvent evt) {//GEN-FIRST:event_findTextFieldKeyPressed
        int selectedRow = resultsSyncTable.getSelectedRow(), key = KeyEvent.VK_UNDEFINED;
        if (evt != null) {
            if ((key = evt.getKeyCode()) == KeyEvent.VK_ENTER && selectedRow == findTitleRow) {
                readSummaryButtonActionPerformed(null);
                return;
            } else if (key == KeyEvent.VK_ESCAPE) {
                hideFindTextField();
                return;
            }
        }

        String text = findTextField.getText().toLowerCase(Locale.ENGLISH);
        if (text.isEmpty()) {
            return;
        }

        boolean continueFind = (findTitleRow != -2 && selectedRow != -1 && (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_UP));
        int numTitles = findTitles.size();
        List<Integer> foundRows = new ArrayList<Integer>((numTitles / 16) + 1);
        for (int i = 0; i < numTitles; i += 4) {
            for (int j = i, k = i + 4; j < k; j++) {
                if (!findTitles.get(j).toLowerCase(Locale.ENGLISH).startsWith(text)) {
                    continue;
                }
                int foundRow = resultsSyncTable.convertRowIndexToView(i / 4);
                if (continueFind) {
                    if (foundRow != findTitleRow) {
                        foundRows.add(foundRow);
                    }
                } else {
                    setFindTitleRow(foundRow);
                    return;
                }
            }
        }

        if (foundRows.isEmpty()) {
            return;
        }

        Collections.sort(foundRows);
        int numFoundRows = foundRows.size();
        if (key == KeyEvent.VK_DOWN) {
            for (int i = 0; i < numFoundRows; i++) {
                int foundRow = foundRows.get(i);
                if (foundRow > findTitleRow) {
                    setFindTitleRow(foundRow);
                    return;
                }
            }
            setFindTitleRow(foundRows.get(0));
        } else {
            for (int i = numFoundRows - 1; i > -1; i--) {
                int foundRow = foundRows.get(i);
                if (foundRow < findTitleRow) {
                    setFindTitleRow(foundRow);
                    return;
                }
            }
            setFindTitleRow(foundRows.get(numFoundRows - 1));
        }
    }//GEN-LAST:event_findTextFieldKeyPressed

    private void setFindTitleRow(int row) {
        resultsSyncTable.changeSelection(row, 0, false, false);
        findTitleRow = row;
    }

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
        workerListener.summaryReadStarted(summaryEditorPane.getText());
    }//GEN-LAST:event_summaryTextToSpeechButtonActionPerformed

    private void summaryDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_summaryDialogWindowClosing
        workerListener.summaryReadStopped();
    }//GEN-LAST:event_summaryDialogWindowClosing

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
                != KeyEvent.VK_BACK_SPACE && key != KeyEvent.VK_ENTER && key != KeyEvent.VK_DELETE && key != KeyEvent.VK_ESCAPE && !evt.isActionKey()) {
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
        startSubtitleSearch(tvSubtitleFormatComboBox, tvSubtitleLanguageComboBox, tvSubtitleSeasonComboBox, tvSubtitleEpisodeComboBox, false);
    }//GEN-LAST:event_tvSubtitleDownloadMatch2ButtonActionPerformed

    private void movieSubtitleDownloadMatch2ButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_movieSubtitleDownloadMatch2ButtonActionPerformed
        startSubtitleSearch(movieSubtitleFormatComboBox, movieSubtitleLanguageComboBox, null, null, false);
    }//GEN-LAST:event_movieSubtitleDownloadMatch2ButtonActionPerformed

    private void downloadLink1ButtonMousePressed(MouseEvent evt) {//GEN-FIRST:event_downloadLink1ButtonMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_downloadLink1ButtonMousePressed

    private void downloadLink2ButtonMousePressed(MouseEvent evt) {//GEN-FIRST:event_downloadLink2ButtonMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_downloadLink2ButtonMousePressed

    private void downloadLink1MenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_downloadLink1MenuItemMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_downloadLink1MenuItemMousePressed

    private void downloadLink2MenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_downloadLink2MenuItemMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_downloadLink2MenuItemMousePressed

    private void hqVideoTypeCheckBoxMousePressed(MouseEvent evt) {//GEN-FIRST:event_hqVideoTypeCheckBoxMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_hqVideoTypeCheckBoxMousePressed

    private void hd720CheckBoxMousePressed(MouseEvent evt) {//GEN-FIRST:event_hd720CheckBoxMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_hd720CheckBoxMousePressed

    private void dvdCheckBoxMousePressed(MouseEvent evt) {//GEN-FIRST:event_dvdCheckBoxMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_dvdCheckBoxMousePressed

    private void hd1080CheckBoxMousePressed(MouseEvent evt) {//GEN-FIRST:event_hd1080CheckBoxMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_hd1080CheckBoxMousePressed

    private void downloadLink2ButtonMouseReleased(MouseEvent evt) {//GEN-FIRST:event_downloadLink2ButtonMouseReleased
        if (exitBackupMode) {
            exitBackupMode = false;
            downloadLink2ButtonActionPerformed(null);
        }
    }//GEN-LAST:event_downloadLink2ButtonMouseReleased

    private void downloadWithDefaultAppCheckBoxMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadWithDefaultAppCheckBoxMenuItemActionPerformed
        if (downloadWithDefaultAppCheckBoxMenuItem.isSelected()) {
            autoDownloadingCheckBoxMenuItem.setSelected(false);
        }
    }//GEN-LAST:event_downloadWithDefaultAppCheckBoxMenuItemActionPerformed

    private void autoDownloadingCheckBoxMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_autoDownloadingCheckBoxMenuItemActionPerformed
        if (autoDownloadingCheckBoxMenuItem.isSelected()) {
            downloadWithDefaultAppCheckBoxMenuItem.setSelected(false);
        }
    }//GEN-LAST:event_autoDownloadingCheckBoxMenuItemActionPerformed

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

    private void exitBackupModeMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_exitBackupModeMenuItemActionPerformed
        exitBackupMode(new MouseEvent(downloadLinkPopupComponent, 0, 0, ActionEvent.CTRL_MASK, 0, 0, 0, false));
    }//GEN-LAST:event_exitBackupModeMenuItemActionPerformed

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
        downloadLinkActionPerformed(downloadContentType, row, row.strExportListener(exportToEmail), false);
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

    private void watchSourceExportActionPerformed(ContentType streamContentType, boolean exportToEmail) {
        SelectedTableRow row = selectedRow();
        watchSourceActionPerformed(streamContentType, row, row.strExportListener(exportToEmail));
    }

    private void copyWatchLink1MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyWatchLink1MenuItemActionPerformed
        watchSourceExportActionPerformed(ContentType.STREAM1, false);
    }//GEN-LAST:event_copyWatchLink1MenuItemActionPerformed

    private void emailWatchLink1MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailWatchLink1MenuItemActionPerformed
        watchSourceExportActionPerformed(ContentType.STREAM1, true);
    }//GEN-LAST:event_emailWatchLink1MenuItemActionPerformed

    private void copyWatchLink2MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyWatchLink2MenuItemActionPerformed
        watchSourceExportActionPerformed(ContentType.STREAM2, false);
    }//GEN-LAST:event_copyWatchLink2MenuItemActionPerformed

    private void emailWatchLink2MenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailWatchLink2MenuItemActionPerformed
        watchSourceExportActionPerformed(ContentType.STREAM2, true);
    }//GEN-LAST:event_emailWatchLink2MenuItemActionPerformed

    private void copySubtitleLinkMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copySubtitleLinkMenuItemActionPerformed
        SelectedTableRow row = selectedRow();
        findSubtitleActionPerformed(row, row.strExportListener(false));
    }//GEN-LAST:event_copySubtitleLinkMenuItemActionPerformed

    private void emailEverythingMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_emailEverythingMenuItemActionPerformed
        SelectedTableRow row = selectedRow();
        VideoStrExportListener strExportListener = row.strExportListener(true, true, 8);
        readSummaryActionPerformed(row, strExportListener);
        exportPosterImage(row, strExportListener);
        downloadLinkActionPerformed(ContentType.DOWNLOAD1, row, strExportListener, false);
        watchSourceActionPerformed(ContentType.STREAM1, row, strExportListener);
        watchTrailerActionPerformed(row, strExportListener);
        exportSummaryLink(row, strExportListener);
    }//GEN-LAST:event_emailEverythingMenuItemActionPerformed

    private void copyPosterImageMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyPosterImageMenuItemActionPerformed
        SelectedTableRow row = selectedRow();
        exportPosterImage(row, row.strExportListener(false));
    }//GEN-LAST:event_copyPosterImageMenuItemActionPerformed

    private void copyDownloadLink1MenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_copyDownloadLink1MenuItemMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_copyDownloadLink1MenuItemMousePressed

    private void copyDownloadLink2MenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_copyDownloadLink2MenuItemMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_copyDownloadLink2MenuItemMousePressed

    private void emailDownloadLink1MenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_emailDownloadLink1MenuItemMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_emailDownloadLink1MenuItemMousePressed

    private void emailDownloadLink2MenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_emailDownloadLink2MenuItemMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_emailDownloadLink2MenuItemMousePressed

    private void exitBackupModeMenuItemMouseReleased(MouseEvent evt) {//GEN-FIRST:event_exitBackupModeMenuItemMouseReleased
        if (exitBackupMode) {
            exitBackupMode = false;
            if (downloadLinkPopupComponent == playButton) {
                playButtonActionPerformed(null);
            } else if (downloadLinkPopupComponent == downloadLink1Button) {
                downloadLink1ButtonActionPerformed(null);
            } else if (downloadLinkPopupComponent == downloadLink2Button) {
                downloadLink2ButtonActionPerformed(null);
            }
        }
    }//GEN-LAST:event_exitBackupModeMenuItemMouseReleased

    private void removeProxiesListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_removeProxiesListKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            removeProxiesRemoveButtonActionPerformed(null);
        }
    }//GEN-LAST:event_removeProxiesListKeyPressed

    private void readSummaryCancelMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_readSummaryCancelMenuItemActionPerformed
        readSummaryCancelMenuItem.setEnabled(false);
        workerListener.summarySearchStopped();
    }//GEN-LAST:event_readSummaryCancelMenuItemActionPerformed

    private void watchTrailerCancelMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchTrailerCancelMenuItemActionPerformed
        watchTrailerCancelMenuItem.setEnabled(false);
        workerListener.trailerSearchStopped();
    }//GEN-LAST:event_watchTrailerCancelMenuItemActionPerformed

    private void downloadLinkCancelMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLinkCancelMenuItemActionPerformed
        downloadLinkCancelMenuItem.setEnabled(false);
        workerListener.torrentSearchStopped();
    }//GEN-LAST:event_downloadLinkCancelMenuItemActionPerformed

    private void watchSourceCancelMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_watchSourceCancelMenuItemActionPerformed
        watchSourceCancelMenuItem.setEnabled(false);
        workerListener.streamSearchStopped();
    }//GEN-LAST:event_watchSourceCancelMenuItemActionPerformed

    private void downloadLinkExitBackupModeMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_downloadLinkExitBackupModeMenuItemActionPerformed
        exitBackupModeMenuItemActionPerformed(evt);
    }//GEN-LAST:event_downloadLinkExitBackupModeMenuItemActionPerformed

    private void downloadLinkExitBackupModeMenuItemMouseReleased(MouseEvent evt) {//GEN-FIRST:event_downloadLinkExitBackupModeMenuItemMouseReleased
        exitBackupModeMenuItemMouseReleased(evt);
    }//GEN-LAST:event_downloadLinkExitBackupModeMenuItemMouseReleased

    private void portDialogComponentHidden(ComponentEvent evt) {//GEN-FIRST:event_portDialogComponentHidden
        String port = portTextField.getText().trim();
        portTextField.setText(port);
        int portNum;

        if (port.isEmpty()) {
            portRandomizeCheckBox.setSelected(true);
        } else if ((portNum = portNum(port)) == -1) {
            if (isConfirmed("\'" + port + "' is invalid. The port must be a number between 0 and 65535. A number between 49161 and 65533 is best. Retry?")) {
                resultsToBackground(true);
                portDialog.setVisible(true);
            } else {
                portTextField.setText(null);
                portRandomizeCheckBox.setSelected(true);
            }
        } else {
            workerListener.portChanged(portNum);
        }
    }//GEN-LAST:event_portDialogComponentHidden

    private void playButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playButtonActionPerformed
        SelectedTableRow row = selectedRow();
        downloadLinkActionPerformed(ContentType.DOWNLOAD1, row, null, true);
    }//GEN-LAST:event_playButtonActionPerformed

    private void playMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playMenuItemActionPerformed
        playButtonActionPerformed(evt);
    }//GEN-LAST:event_playMenuItemActionPerformed

    private void playButtonMousePressed(MouseEvent evt) {//GEN-FIRST:event_playButtonMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_playButtonMousePressed

    private void playMenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_playMenuItemMousePressed
        exitBackupMode(evt);
    }//GEN-LAST:event_playMenuItemMousePressed

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

    private PlaylistItem selectedPlaylistItem() {
        synchronized (playlistSyncTable.lock) {
            int[] rows = playlistSyncTable.table.getSelectedRows();
            if (rows.length != 1) {
                return null;
            }
            return (PlaylistItem) playlistSyncTable.tableModel.getValueAt(playlistSyncTable.table.convertRowIndexToModel(rows[0]), playlistItemCol);
        }
    }

    private void playlistPlayButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_playlistPlayButtonActionPerformed
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

    private void activationMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_activationMenuItemActionPerformed
        licenseActivation();
    }//GEN-LAST:event_activationMenuItemActionPerformed

    private void activationButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_activationButtonActionPerformed
        workerListener.license(activationTextField.getText().trim(), false);
    }//GEN-LAST:event_activationButtonActionPerformed

    private void activationUpgradeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_activationUpgradeButtonActionPerformed
        workerListener.license(null, false);
    }//GEN-LAST:event_activationUpgradeButtonActionPerformed

    private void activationDialogWindowClosing(WindowEvent evt) {//GEN-FIRST:event_activationDialogWindowClosing
        if (!isVisible()) {
            setVisible(true);
        }
    }//GEN-LAST:event_activationDialogWindowClosing

    private void playlistPlayButtonMousePressed(MouseEvent evt) {//GEN-FIRST:event_playlistPlayButtonMousePressed
        if ((ActionEvent.CTRL_MASK & evt.getModifiers()) == ActionEvent.CTRL_MASK) {
            forcePlay = true;
        }
    }//GEN-LAST:event_playlistPlayButtonMousePressed

    private void playlistPlayMenuItemMousePressed(MouseEvent evt) {//GEN-FIRST:event_playlistPlayMenuItemMousePressed
        playlistPlayButtonMousePressed(evt);
    }//GEN-LAST:event_playlistPlayMenuItemMousePressed

    private void playButtonMouseReleased(MouseEvent evt) {//GEN-FIRST:event_playButtonMouseReleased
        exitBackupMode = false;
    }//GEN-LAST:event_playButtonMouseReleased

    private void downloadLink1ButtonMouseReleased(MouseEvent evt) {//GEN-FIRST:event_downloadLink1ButtonMouseReleased
        exitBackupMode = false;
    }//GEN-LAST:event_downloadLink1ButtonMouseReleased

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

    private void exitBackupMode(MouseEvent evt) {
        if ((ActionEvent.CTRL_MASK & evt.getModifiers()) != ActionEvent.CTRL_MASK || !Connection.downloadLinkInfoFail()) {
            return;
        }

        Connection.unfailDownloadLinkInfo();
        if (!isAltSearch) {
            return;
        }

        isAltSearch = false;
        boolean enable = true;
        if (!downloadLink2Button.isEnabled() && workerListener.isTorrentSearchDone()) {
            UI.enable(enable, downloadLink2Button, downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem);
            Object evtSource = evt.getSource();
            if (playButton == evtSource || downloadLink1Button == evtSource || downloadLink2Button == evtSource) {
                exitBackupMode = true;
            }
        }
        enableVideoFormats(enable);
        UI.enable(false, exitBackupModeMenuItem, downloadLinkExitBackupModeMenuItem);
    }

    private void updateDownloadSizeComboBoxes() {
        String max = (String) maxDownloadSizeComboBox.getSelectedItem();
        if (!max.equals(Constant.INFINITY) && Integer.parseInt((String) minDownloadSizeComboBox.getSelectedItem()) >= Integer.parseInt(max)) {
            maxDownloadSizeComboBox.setSelectedItem(Constant.INFINITY);
        }
    }

    private void updatedTVComboBoxes() {
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
                connectionIssueButton.setToolTipText("view connection issues (" + CTRL_CLICK + "hide)");
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

    private Component showing() {
        return isShowing() ? this : null;
    }

    private void setSafetyDialog(String statistic, String link, String name) {
        safetyEditorPane.setText("<html><head><title></title></head><body><table cellpadding=\"5\"><tr><td>" + Constant.HTML_FONT + "This download link (" + name
                + ") is from an untrustworthy source." + (statistic == null || link == null ? "" : " " + statistic + " of the <a href=\"" + link
                        + "\">comments</a> on the video indicate that it may be fake.")
                + "<br><br><b>Do you want to proceed with this download link anyway?</b></font></td></tr></table></body></html>");
    }

    private Window[] windows() {
        return new Window[]{this, safetyDialog, msgDialog, summaryDialog, faqFrame, aboutDialog, timeoutDialog, tvDialog, resultsPerSearchDialog,
            downloadSizeDialog, extensionsDialog, languageCountryDialog, dummyDialog /* Backward compatibility */, proxyDialog, addProxiesDialog,
            removeProxiesDialog, profileDialog, profileNameChangeDialog, commentsDialog, portDialog, tvSubtitleDialog, movieSubtitleDialog};
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
                autoDownloadersButtonGroup.setSelected(Boolean.parseBoolean(settings[++i]) ? defaultRadioButtonMenuItem.getModel()
                        : customRadioButtonMenuItem.getModel(), true);

                restoreSize(GUI.this, settings[++i]);
                i += restoreWindows(settings, i, windows());

                restoreList("whitelist", settings[++i], whitelistListModel);
                restoreList("blacklist", settings[++i], blacklistListModel);
                restoreList("languageList", settings[++i], languageList);
                restoreList("countryList", settings[++i], countryList);

                i += restoreButtons(settings, i, downloadWithDefaultAppCheckBoxMenuItem, feedCheckBoxMenuItem);

                proxyImportFile = getPath(settings, ++i);
                proxyExportFile = getPath(settings, ++i);
                torrentDir = getPath(settings, ++i);
                subtitleDir = getPath(settings, ++i);

                i += restoreComboBoxes(settings, i, dummyComboBox); // Backward compatibility
                i += restoreComboBoxes(settings, i, timeoutDownloadLinkComboBox);
                i += restoreButtons(settings, i, emailWithDefaultAppCheckBoxMenuItem);
                i += restoreWindows(settings, i, playlistFrame);

                playlistDir = getPath(settings, ++i);
                usePeerBlock = Boolean.parseBoolean(settings[++i]);
                restoreButtons(settings, i, playlistAutoOpenCheckBoxMenuItem, playlistTipsCheckBoxMenuItem, playlistShowNonVideoItemsCheckBoxMenuItem);

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
            saveButtons(settings, defaultRadioButtonMenuItem);

            settings.append(saveSize(GUI.this));
            for (Window window : windows()) {
                settings.append(savePosition(window));
            }

            saveList(settings, "whitelist", whitelistListModel.toArray());
            saveList(settings, "blacklist", blacklistListModel.toArray());
            saveList(settings, "languageList", languageList.getSelectedValues());
            saveList(settings, "countryList", countryList.getSelectedValues());
            saveButtons(settings, downloadWithDefaultAppCheckBoxMenuItem, feedCheckBoxMenuItem);
            savePaths(settings, proxyImportFile, proxyExportFile, torrentDir, subtitleDir);
            saveComboBoxes(settings, dummyComboBox); // Backward compatibility
            saveComboBoxes(settings, timeoutDownloadLinkComboBox);
            saveButtons(settings, emailWithDefaultAppCheckBoxMenuItem);
            settings.append(savePosition(playlistFrame));
            savePaths(settings, playlistDir);
            settings.append(usePeerBlock).append(Constant.NEWLINE);
            saveButtons(settings, playlistAutoOpenCheckBoxMenuItem, playlistTipsCheckBoxMenuItem, playlistShowNonVideoItemsCheckBoxMenuItem);

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
            return new AbstractButton[]{autoDownloadingCheckBoxMenuItem, updateCheckBoxMenuItem, dummyMenuItem /* Backward compatibility */,
                proxyDownloadLinkInfoCheckBox, proxyVideoInfoCheckBox, proxySearchEnginesCheckBox, proxyTrailersCheckBox, proxyVideoStreamersCheckBox,
                proxyUpdatesCheckBox, proxySubtitlesCheckBox, browserNotificationCheckBoxMenuItem};
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
            if (str.equals(name + EMPTY_LIST)) {
                return;
            }

            for (String currStr : Regex.split(str, ":")) {
                listModel.addElement(currStr);
            }
        }

        private void restoreList(String name, String str, JList list) {
            if (str.equals(name + EMPTY_LIST)) {
                return;
            }

            String[] strs = Regex.split(str, ":");
            int[] selection = new int[strs.length];
            ListModel listModel = list.getModel();
            int size = listModel.getSize(), k = 0;

            for (int j = 0; j < size; j++) {
                for (String currStr : strs) {
                    if (currStr.equals(listModel.getElementAt(j))) {
                        selection[k++] = j;
                    }
                }
            }

            list.setSelectedIndices(selection);
            list.ensureIndexIsVisible(list.getSelectedIndex());
        }

        private String saveSize(Window window) {
            Dimension size = window.getSize();
            return size.width + "x" + size.height + Constant.NEWLINE;
        }

        private void restoreSize(Window window, String size) {
            String[] dimensions = Regex.split(size, "x");
            Dimension dimension = new Dimension(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]));
            if (window instanceof Frame && (((Frame) window).getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH && !UI.isMaxSize(dimension)) {
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
    public void readSummaryStarted() {
        UI.enable(false, readSummaryButton, readSummaryMenuItem, copyFullTitleAndYearMenuItem);
    }

    @Override
    public void readSummaryStopped() {
        if (resultsSyncTable.getSelectedRows().length == 1) {
            UI.enable(true, readSummaryButton, readSummaryMenuItem, copyFullTitleAndYearMenuItem);
        }
    }

    @Override
    public void watchTrailerStarted() {
        UI.enable(false, watchTrailerButton, watchTrailerMenuItem, emailTrailerLinkMenuItem, copyTrailerLinkMenuItem);
    }

    @Override
    public void watchTrailerStopped() {
        if (resultsSyncTable.getSelectedRows().length == 1) {
            UI.enable(true, watchTrailerButton, watchTrailerMenuItem, emailTrailerLinkMenuItem, copyTrailerLinkMenuItem);
        }
    }

    @Override
    public void enableDownload(boolean enable) {
        if (isAltSearch) {
            UI.enable(false, downloadLink2Button, downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem);
        } else {
            UI.enable(enable, downloadLink2Button, downloadLink2MenuItem, emailDownloadLink2MenuItem, copyDownloadLink2MenuItem);
        }
        UI.enable(enable, downloadLink1Button, downloadLink1MenuItem, emailDownloadLink1MenuItem, copyDownloadLink1MenuItem, playButton, playMenuItem);
    }

    @Override
    public void enableWatch(boolean enable) {
        UI.enable(enable, watchSource2Button, watchSource2MenuItem, emailWatchLink2MenuItem, copyWatchLink2MenuItem, watchSource1Button, watchSource1MenuItem,
                emailWatchLink1MenuItem, copyWatchLink1MenuItem);
    }

    @Override
    public void enableSummarySearchStop(boolean enable) {
        readSummaryCancelMenuItem.setEnabled(enable);
    }

    @Override
    public void enableTrailerSearchStop(boolean enable) {
        watchTrailerCancelMenuItem.setEnabled(enable);
    }

    @Override
    public void enableTorrentSearchStop(boolean enable) {
        downloadLinkCancelMenuItem.setEnabled(enable);
    }

    @Override
    public void enableStreamSearchStop(boolean enable) {
        watchSourceCancelMenuItem.setEnabled(enable);
    }

    @Override
    public void enableLinkProgress(boolean enable) {
        if (enable || workerListener.isLinkProgressDone()) {
            linkProgressBar.setIndeterminate(enable);
            linkProgressBar.setStringPainted(enable);
            closeBoxButton.setEnabled(enable);
        }
    }

    @Override
    public void videoDownloadStopped() {
        if (resultsSyncTable.getSelectedRows().length == 1) {
            enableDownload(true);
        }
    }

    @Override
    public void videoWatchStopped() {
        if (resultsSyncTable.getSelectedRows().length == 1) {
            enableWatch(true);
        }
    }

    private void enableVideoFormats(boolean enable) {
        UI.enable(enable, hd720CheckBox, hd1080CheckBox, hqVideoTypeCheckBox, dvdCheckBox);
    }

    @Override
    public void altVideoDownloadStarted() {
        if (!isAltSearch) {
            showConnectionException(new ConnectionException("<font color=\"red\">Switching to backup mode.</font> " + Connection.error("", null,
                    Connection.downloadLinkInfoFailUrl())));
            enableVideoFormats(false);
            UI.enable(true, exitBackupModeMenuItem, downloadLinkExitBackupModeMenuItem);
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
                    timedMsgDialog.setVisible(true);
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
        safetyDialog.setVisible(true);
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
        summaryDialog.setVisible(true);
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
    public void browserNotification(String item, String action, DomainType domainType) {
        if (!browserNotificationCheckBoxMenuItem.isSelected()) {
            return;
        }

        String proxy = Constant.NO_PROXY;
        switch (domainType) {
            case DOWNLOAD_LINK_INFO:
                if (proxyDownloadLinkInfoCheckBox.isSelected()) {
                    proxy = getSelectedProxy();
                }
                break;
            case VIDEO_INFO:
                if (proxyVideoInfoCheckBox.isSelected()) {
                    proxy = getSelectedProxy();
                }
                break;
            case SEARCH_ENGINE:
                if (proxySearchEnginesCheckBox.isSelected()) {
                    proxy = getSelectedProxy();
                }
                break;
            case TRAILER:
                if (proxyTrailersCheckBox.isSelected()) {
                    proxy = getSelectedProxy();
                }
                break;
            case VIDEO_STREAMER:
                if (proxyVideoStreamersCheckBox.isSelected()) {
                    proxy = getSelectedProxy();
                }
                break;
            case UPDATE:
                if (proxyUpdatesCheckBox.isSelected()) {
                    proxy = getSelectedProxy();
                }
                break;
            case SUBTITLE:
                if (proxySubtitlesCheckBox.isSelected()) {
                    proxy = getSelectedProxy();
                }
                break;
            default:
                break;
        }

        String newMsg = "The " + item + " will be " + action + " in your web browser.";
        if (proxy.equals(Constant.NO_PROXY)) {
            showOptionalMsg(newMsg, browserNotificationCheckBoxMenuItem);
        } else {
            String[] proxyParts = Regex.split(proxy, ":");
            showOptionalMsg(newMsg + " Set your web browser's proxy to " + proxyParts[0] + " on port " + proxyParts[1] + '.', browserNotificationCheckBoxMenuItem);
        }
    }

    private void updateOptionalMsgCheckBox(JMenuItem menuItem) {
        if (optionalMsgCheckBox.isSelected()) {
            menuItem.setSelected(false);
            optionalMsgCheckBox.setSelected(false);
        }
    }

    @Override
    public void startPeerBlock() {
        boolean canShowPeerBlock = peerBlockNotificationCheckBoxMenuItem.isSelected();
        if (!Constant.CAN_PEER_BLOCK || (!usePeerBlock && !canShowPeerBlock) || (new File(Constant.APP_DIR + Constant.PEER_BLOCK + "Running")).exists()) {
            return;
        }
        if ((new File(Constant.APP_DIR + Constant.PEER_BLOCK + "Exit")).exists() || WindowsUtil.isProcessRunning(Constant.PEER_BLOCK)) {
            usePeerBlock = false;
            peerBlockNotificationCheckBoxMenuItem.setSelected(false);
            IO.fileOp(Constant.APP_DIR + Constant.PEER_BLOCK + "Exit", IO.RM_FILE_NOW_AND_ON_EXIT);
            return;
        }
        if (canShowPeerBlock && (showOptionalConfirm(showing(), "Start " + Constant.PEER_BLOCK_APP_TITLE + " to block untrusty IPs?",
                peerBlockNotificationCheckBoxMenuItem) != JOptionPane.YES_OPTION)) {
            usePeerBlock = false;
            return;
        }

        usePeerBlock = true;
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
            showMsg("Error starting " + Constant.PEER_BLOCK_APP_TITLE + ": " + e.getMessage(), Constant.ERROR_MSG);
        }
    }

    private boolean save(JFileChooser fileChooser) {
        Window alwaysOnTopFocus = resultsToBackground();
        boolean result = (fileChooser.showSaveDialog(showing()) == JFileChooser.APPROVE_OPTION);
        resultsToForeground(alwaysOnTopFocus);
        return result;
    }

    @Override
    public void saveTorrent(File torrentFile) throws Exception {
        if (proxyFileChooser.isShowing()) {
            return;
        }

        subtitleFileChooser.cancelSelection();
        torrentFileChooser.setFileFilter(Regex.torrentFileFilter);
        torrentFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        torrentFileChooser.setSelectedFile(new File(torrentDir + torrentFile.getName()));

        if (save(torrentFileChooser)) {
            File torrent = torrentFileChooser.getSelectedFile();
            torrentDir = IO.parentDir(torrent);
            IO.write(torrentFile, torrent);
        }
    }

    @Override
    public void saveSubtitle(String saveFileName, File subtitleFile) throws Exception {
        if (proxyFileChooser.isShowing() || torrentFileChooser.isShowing()) {
            return;
        }

        subtitleFileChooser.setFileFilter(Regex.subtitleFileFilter);
        subtitleFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        subtitleFileChooser.setSelectedFile(new File(subtitleDir + saveFileName));

        if (save(subtitleFileChooser)) {
            File subtitle = subtitleFileChooser.getSelectedFile();
            subtitleDir = IO.parentDir(subtitle);
            IO.write(subtitleFile, subtitle);
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
    public void setTitle(String title, int row, String titleID) {
        resultsSyncTable.setModelValueAt(title, row, titleCol, idCol, titleID);
    }

    @Override
    public void setSummary(String summary, int row, String titleID) {
        resultsSyncTable.setModelValueAt(summary, row, summaryCol, idCol, titleID);
    }

    @Override
    public String getSeason(int row, String titleID) {
        return (String) resultsSyncTable.getModelValueAt(row, seasonCol, idCol, titleID);
    }

    @Override
    public void setSeason(String season, int row, String titleID) {
        resultsSyncTable.setModelValueAt(season, row, seasonCol, idCol, titleID);
    }

    @Override
    public void setEpisode(String episode, int row, String titleID) {
        resultsSyncTable.setModelValueAt(episode, row, episodeCol, idCol, titleID);
    }

    @Override
    public void setImageLink(String imageLink, int row, String titleID) {
        resultsSyncTable.setModelValueAt(imageLink, row, imageLinkCol, idCol, titleID);
    }

    @Override
    public void setImagePath(String imagePath, int row, String titleID) {
        resultsSyncTable.setModelValueAt(imagePath, row, imageCol, idCol, titleID);
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
        UI.enable(false, loadMoreResultsButton, searchButton, popularTVShowsButton, popularMoviesButton, viewNewHighQualityMoviesMenuItem);
        stopButton.setEnabled(true);
        resultsSyncTable.requestFocusInWindow();
    }

    @Override
    public void newSearch(int maxProgress, boolean isTVShow) {
        enableVideoFormats(true);
        UI.enable(false, exitBackupModeMenuItem, downloadLinkExitBackupModeMenuItem);
        resultsSyncTable.setRowCount(0);
        isAltSearch = false;
        resultsLabel.setText("Results: 0");
        progressBar.setMinimum(0);
        progressBar.setMaximum(maxProgress);
        progressBar.setValue(0);

        Connection.clearCache();
        if (isTVShow) {
            trailerEpisodes.clear();
            downloadLinkEpisodes.clear();
            subtitleEpisodes.clear();
        }
        findTitles.clear();
    }

    @Override
    public boolean oldSearch(int maxProgress) {
        if (progressBar.getValue() != progressBar.getMaximum()) {
            return false;
        }
        progressBar.setMinimum(0);
        progressBar.setMaximum(maxProgress);
        progressBar.setValue(0);
        return true;
    }

    private void stopSearch() {
        try {
            for (int i = 0; i < 25; i++) {
                if (resultsSyncTable.getRowCount() > 0 && resultsSyncTable.getSelectedRow() == -1) {
                    JViewport viewport = (JViewport) resultsSyncTable.table.getParent();
                    if ((new Rectangle(viewport.getExtentSize())).intersects(rectangle(viewport, resultsSyncTable.getCellRect(0, 0, true)))) {
                        resultsSyncTable.setRowSelectionInterval(0, 0);
                    }
                    break;
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        UI.enable(true, searchButton, popularTVShowsButton, popularMoviesButton, viewNewHighQualityMoviesMenuItem);
    }

    @Override
    public void searchStopped() {
        stopSearch();
        stopButton.setEnabled(false);
        if (titleTextField.isEnabled() && resultsSyncTable.getRowCount() == 0) {
            titleTextField.requestFocusInWindow();
        }
    }

    @Override
    public void searchProgressMaxOut() {
        progressBar.setValue(progressBar.getMaximum());
    }

    @Override
    public void moreResults(boolean areMoreResults) {
        loadMoreResultsButton.setEnabled(areMoreResults);
    }

    @Override
    public void newResult(Object[] result) {
        resultsSyncTable.addRow(result);
        posterImagePaths.add((String) result[imageCol]);
        updateFindTitles((String) result[currTitleCol]);
    }

    @Override
    public void newResults(Iterable<Object[]> results) {
        for (Object[] result : results) {
            resultsSyncTable.addRow(result);
            posterImagePaths.add((String) result[imageCol]);
            updateFindTitles((String) result[currTitleCol]);
        }
    }

    @Override
    public void searchNumResultsUpdate(int numResults) {
        resultsLabel.setText("Results: " + numResults);
    }

    @Override
    public void searchProgressIncrement() {
        progressBar.setValue(progressBar.getValue() + 1);
    }

    @Override
    public boolean newPlaylistItems(List<Object[]> items, int insertRow, int primaryItemIndex) {
        boolean isPrimaryItemNew = true;
        synchronized (playlistSyncTable.lock) {
            for (int i = 0, numItems = items.size(), currInsertRow = insertRow, prevInsertRow; i < numItems; i++) {
                if ((prevInsertRow = newPlaylistItem(items.get(i), currInsertRow)) >= 0) {
                    currInsertRow = prevInsertRow + 1;
                } else if (i == primaryItemIndex) {
                    isPrimaryItemNew = false;
                }
            }
        }
        return isPrimaryItemNew;
    }

    @Override
    public int newPlaylistItem(Object[] item, int insertRow) {
        synchronized (playlistSyncTable.lock) {
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
    }

    @Override
    public void removePlaylistItem(PlaylistItem playlistItem) {
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

    @Override
    public void setPlaylistItemProgress(FormattedNum progress, PlaylistItem playlistItem, boolean updateValOnly) {
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

    @Override
    public boolean showPlaylist(PlaylistItem selectedPlaylistItem) {
        UI.show(playlistFrame);
        restorePlaylist(false);
        boolean selected = false;
        synchronized (playlistSyncTable.lock) {
            for (int row = playlistSyncTable.tableModel.getRowCount() - 1; row > -1; row--) {
                if (playlistSyncTable.tableModel.getValueAt(row, playlistItemCol).equals(selectedPlaylistItem)) {
                    int viewRow = playlistSyncTable.table.convertRowIndexToView(row);
                    if (viewRow != -1) {
                        playlistSyncTable.table.setRowSelectionInterval(viewRow, viewRow);
                        JViewport viewport = (JViewport) playlistSyncTable.table.getParent();
                        viewport.scrollRectToVisible(rectangle(viewport, playlistSyncTable.table.getCellRect(viewRow, 0, true)));
                        selected = true;
                    }
                    break;
                }
            }
        }
        refreshPlaylistControls();
        return selected;
    }

    @Override
    public String getPlaylistSaveDir() {
        return playlistDir;
    }

    @Override
    public void playlistError(String msg, boolean html) {
        synchronized (optionDialogLock) {
            showOptionDialog(UI.deiconifyThenIsShowing(playlistFrame) ? playlistFrame : showing(), html ? getEditorPane(msg) : getTextArea(msg),
                    Constant.APP_TITLE, Constant.ERROR_MSG, false);
        }
    }

    private void play(PlaylistItem playlistItem) {
        if (playlistItem == null) {
            playlistPlayButton.setIcon(playIcon);
            playlistPlayMenuItem.setText("Play");
            UI.enable(false, playlistOpenMenuItem, playlistPlayButton, playlistPlayMenuItem);
            return;
        }
        playlistOpenMenuItem.setEnabled(playlistItem.canOpen());
        boolean play = playlistItem.canPlay(), active = playlistItem.isActive();
        playlistPlayButton.setIcon(play ? playIcon : stopIcon);
        playlistPlayMenuItem.setText(play ? "Play" : "Stop");
        UI.enable((play && !active) || (!play && active), playlistPlayButton, playlistPlayMenuItem);
    }

    @Override
    public void refreshPlaylistControls() {
        play(selectedPlaylistItem());
    }

    @Override
    public void setPlaylistPlayHint(String msg) {
        playlistPlayButton.setToolTipText(playlistPlayButton.getToolTipText() + msg);
        playlistPlayMenuItem.setToolTipText(playlistPlayMenuItem.getToolTipText() + msg);
    }

    @Override
    public boolean useMediaServer() {
        if (!useMediaServer) {
            useMediaServer = true;
            return playlistTipsCheckBoxMenuItem.isSelected() && showOptionalConfirm(UI.deiconifyThenIsShowing(playlistFrame) ? playlistFrame : showing(),
                    "Watch on television or other device?", playlistTipsCheckBoxMenuItem) == JOptionPane.YES_OPTION;
        }
        return false;
    }

    @Override
    public boolean isConfirmed(String msg) {
        return showConfirm(msg) == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean isAuthorizationConfirmed(String msg) {
        synchronized (optionDialogLock) {
            authenticationMessageLabel.setText(msg);
            return showOptionDialog(authenticationPanel, "Authentication Required", JOptionPane.OK_CANCEL_OPTION, true) == JOptionPane.OK_OPTION;
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
    public boolean canProxyVideoStreamers() {
        return proxyVideoStreamersCheckBox.isSelected();
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
    public String getAutoDownloader() {
        return Str.get(defaultRadioButtonMenuItem.isSelected() ? 393 : 394);
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
    public boolean canAutoDownload() {
        return autoDownloadingCheckBoxMenuItem.isSelected();
    }

    @Override
    public boolean canDownloadWithDefaultApp() {
        return downloadWithDefaultAppCheckBoxMenuItem.isSelected();
    }

    @Override
    public boolean canEmailWithDefaultApp() {
        return emailWithDefaultAppCheckBoxMenuItem.isSelected();
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
        updateMenuItem.setText("Checking for Updates...");
    }

    @Override
    public void updateStopped() {
        updateMenuItem.setText("Check for Updates");
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
            tvSubtitleCancelButton.setEnabled(true);
            UI.enable(false, tvSubtitleDownloadMatch2Button, tvSubtitleDownloadMatch1Button);
        } else {
            movieSubtitleLoadingLabel.setIcon(loadingIcon);
            movieSubtitleCancelButton.setEnabled(true);
            UI.enable(false, movieSubtitleDownloadMatch2Button, movieSubtitleDownloadMatch1Button);
        }
    }

    @Override
    public void subtitleSearchStopped() {
        if (isTVShowSubtitle) {
            tvSubtitleDialog.setVisible(false);
            tvSubtitleCancelButton.setEnabled(false);
            UI.enable(true, tvSubtitleDownloadMatch2Button, tvSubtitleDownloadMatch1Button);
            tvSubtitleLoadingLabel.setIcon(notLoadingIcon);
        } else {
            movieSubtitleDialog.setVisible(false);
            movieSubtitleCancelButton.setEnabled(false);
            UI.enable(true, movieSubtitleDownloadMatch2Button, movieSubtitleDownloadMatch1Button);
            movieSubtitleLoadingLabel.setIcon(notLoadingIcon);
        }
    }

    @Override
    public void summaryReadStarted() {
        summaryTextToSpeechButton.setEnabled(false);
        summaryLoadingLabel.setIcon(loadingIcon);
    }

    @Override
    public void summaryReadStopped() {
        summaryTextToSpeechButton.setEnabled(true);
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
        return UI.displayableStr(progressBar, "\u2004", " ");
    }

    @Override
    public String invisibleSeparator() {
        return UI.displayableStr(playlistSyncTable.table, "\u200B\u200B\u200B", "\u0009\u0009\u0009");
    }

    @Override
    public void licenseActivation() {
        workerListener.license(null, true);
    }

    @Override
    public void showLicenseActivation() {
        activationDialog.setLocationRelativeTo(UI.deiconifyThenIsShowing(playlistFrame) ? playlistFrame : this);
        resultsToBackground(true);
        activationDialog.setVisible(true);
    }

    @Override
    public void licenseActivated(boolean alert, String activationCode) {
        activationTextField.setForeground(new Color(21, 138, 12));
        activationTextField.setText(activationCode);
        if (alert) {
            activationDialog.setVisible(false);
            showMsg("Activation Successful!", Constant.INFO_MSG);
        }
    }

    @Override
    public void licenseDeactivated(boolean alert) {
        activationTextField.setForeground(Color.BLACK);
        if (alert) {
            showMsg("Activation Failed.", Constant.ERROR_MSG);
        }
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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    JDialog aboutDialog;
    JEditorPane aboutEditorPane;
    JMenuItem aboutMenuItem;
    JScrollPane aboutScrollPane;
    JButton activationButton;
    JLabel activationCodeLabel;
    JDialog activationDialog;
    JLabel activationLoadingLabel;
    JMenuItem activationMenuItem;
    JTextField activationTextField;
    JButton activationUpgradeButton;
    JLabel activationUpgradeLabel;
    JButton addProxiesAddButton;
    JButton addProxiesCancelButton;
    JDialog addProxiesDialog;
    JLabel addProxiesLabel;
    JScrollPane addProxiesScrollPane;
    JTextArea addProxiesTextArea;
    JCheckBox anyTitleCheckBox;
    JLabel authenticationMessageLabel;
    JPanel authenticationPanel;
    JPasswordField authenticationPasswordField;
    JLabel authenticationPasswordLabel;
    JLabel authenticationUsernameLabel;
    JTextField authenticationUsernameTextField;
    ButtonGroup autoDownloadersButtonGroup;
    JCheckBoxMenuItem autoDownloadingCheckBoxMenuItem;
    JLabel blacklistedLabel;
    JList blacklistedList;
    JScrollPane blacklistedScrollPane;
    JButton blacklistedToWhitelistedButton;
    JCheckBoxMenuItem browserNotificationCheckBoxMenuItem;
    JButton closeBoxButton;
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
    JMenuItem copyWatchLink1MenuItem;
    JMenuItem copyWatchLink2MenuItem;
    JLabel countryLabel;
    JList countryList;
    JScrollPane countryScrollPane;
    JTextField customExtensionTextField;
    JRadioButtonMenuItem customRadioButtonMenuItem;
    JMenuItem cutMenuItem;
    JRadioButtonMenuItem defaultRadioButtonMenuItem;
    JMenuItem deleteMenuItem;
    JButton downloadLink1Button;
    JMenuItem downloadLink1MenuItem;
    JButton downloadLink2Button;
    JMenuItem downloadLink2MenuItem;
    JPopupMenu downloadLinkButtonPopupMenu;
    JMenuItem downloadLinkCancelMenuItem;
    JMenuItem downloadLinkExitBackupModeMenuItem;
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
    JCheckBoxMenuItem downloadWithDefaultAppCheckBoxMenuItem;
    JMenu downloaderMenu;
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
    JMenuItem emailWatchLink1MenuItem;
    JMenuItem emailWatchLink2MenuItem;
    JCheckBoxMenuItem emailWithDefaultAppCheckBoxMenuItem;
    JDateChooser endDateChooser;
    JLabel episodeLabel;
    JMenuItem exitBackupModeMenuItem;
    JPopupMenu exitBackupModePopupMenu;
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
    JButton langaugeCountryOkButton;
    JDialog languageCountryDialog;
    JMenuItem languageCountryMenuItem;
    JLabel languageCountryWarningLabel;
    JLabel languageLabel;
    JList languageList;
    JScrollPane languageScrollPane;
    JProgressBar linkProgressBar;
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
    JButton movieSubtitleCancelButton;
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
    JCheckBox optionalMsgCheckBox;
    JPanel optionalMsgPanel;
    JTextArea optionalMsgTextArea;
    JMenuItem pasteMenuItem;
    JCheckBoxMenuItem peerBlockNotificationCheckBoxMenuItem;
    JButton playButton;
    JMenuItem playMenuItem;
    JCheckBoxMenuItem playlistAutoOpenCheckBoxMenuItem;
    JMenuItem playlistCopyMenuItem;
    JFileChooser playlistFileChooser;
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
    JButton playlistRemoveButton;
    JMenuItem playlistRemoveMenuItem;
    JMenuItem playlistSaveFolderMenuItem;
    JScrollPane playlistScrollPane;
    JCheckBoxMenuItem playlistShowNonVideoItemsCheckBoxMenuItem;
    JTable playlistTable;
    JPopupMenu playlistTablePopupMenu;
    Separator playlistTablePopupMenuSeparator1;
    Separator playlistTablePopupMenuSeparator2;
    JCheckBoxMenuItem playlistTipsCheckBoxMenuItem;
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
    JButton profileNameChangeCancelButton;
    JDialog profileNameChangeDialog;
    JLabel profileNameChangeLabel;
    JButton profileNameChangeOKButton;
    JTextField profileNameChangeTextField;
    JButton profileOKButton;
    JButton profileRenameButton;
    JButton profileSetButton;
    JButton profileUseButton;
    JProgressBar progressBar;
    JLabel progressBarLabel;
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
    JCheckBox proxyVideoStreamersCheckBox;
    JComboBox ratingComboBox;
    JLabel ratingLabel;
    JButton readSummaryButton;
    JPopupMenu readSummaryButtonPopupMenu;
    JMenuItem readSummaryCancelMenuItem;
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
    JLabel resultsLabel;
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
    JLabel seasonLabel;
    JMenuItem selectAllMenuItem;
    JDateChooser startDateChooser;
    JTextField statusBarTextField;
    JButton stopButton;
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
    JButton trashCanButton;
    JButton tvCancelButton;
    JDialog tvDialog;
    JComboBox tvEpisodeComboBox;
    JComboBox tvSeasonComboBox;
    JLabel tvSelectionLabel;
    JButton tvSubmitButton;
    JButton tvSubtitleCancelButton;
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
    JMenu useProfileMenu;
    Separator useProfileMenuSeparator1;
    JMenu viewMenu;
    JMenuItem viewNewHighQualityMoviesMenuItem;
    JButton watchSource1Button;
    JMenuItem watchSource1MenuItem;
    JButton watchSource2Button;
    JMenuItem watchSource2MenuItem;
    JPopupMenu watchSourceButtonPopupMenu;
    JMenuItem watchSourceCancelMenuItem;
    JButton watchTrailerButton;
    JPopupMenu watchTrailerButtonPopupMenu;
    JMenuItem watchTrailerCancelMenuItem;
    JMenuItem watchTrailerMenuItem;
    JLabel whitelistLabel;
    JList whitelistedList;
    JScrollPane whitelistedScrollPane;
    JButton whitelistedToBlacklistedButton;
    JButton yesButton;
    // End of variables declaration//GEN-END:variables
}
