package gui;

import i18n.Bundle;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.text.NumberFormat;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import util.Constant;

public class SplashScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int MAX_PROGRESS = 50;
    private final ResourceBundle bundle;
    private final NumberFormat percentFormat;
    private final String INITIALIZING, DONE;
    private int progress;

    public SplashScreen() {
        Bundle newBundle = new Bundle(null);
        bundle = newBundle.bundle;
        percentFormat = NumberFormat.getPercentInstance(newBundle.LOCALE);
        percentFormat.setMinimumFractionDigits(0);
        percentFormat.setMaximumFractionDigits(0);
        INITIALIZING = ' ' + bundle.getString("initializing") + " - ";
        DONE = ' ' + bundle.getString("done");

        initComponents();

        UI.setIcon(closeBoxButton, "closeBox");
        loadingLabel.setIcon(UI.icon("loading.gif"));
        UI.setIcon(connectionIssueButton, "noWarning");

        Color bgColor = getBackground();
        genreList.setBackground(bgColor);
        findTextField.setBackground(bgColor);
        findTextField.setForeground(bgColor);

        Dimension windowSize = new Dimension(1022, 680);
        setSize(windowSize);
        setIconImage(Toolkit.getDefaultToolkit().getImage(Constant.PROGRAM_DIR + "icon16x16.png"));
        setLocation(UI.screenCenter(windowSize));
    }

    void progress() {
        statusBarTextField.setText(INITIALIZING + percentFormat.format(++progress / (double) MAX_PROGRESS) + DONE);
    }

    private void initComponents() {
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
        statusBarTextField = new JTextField();
        connectionIssueButton = new JButton();
        startDateTextField = new JTextField();
        endDateTextField = new JTextField();
        findTextField = new JTextField();
        menuBar = new JMenuBar();
        fileMenu = new JMenu();
        editMenu = new JMenu();
        viewMenu = new JMenu();
        searchMenu = new JMenu();
        playlistMenu = new JMenu();
        downloadMenu = new JMenu();
        helpMenu = new JMenu();

        setTitle(Constant.APP_TITLE);
        setMinimumSize(null);

        titleTextField.setEnabled(false);

        titleLabel.setLabelFor(titleTextField);
        titleLabel.setText(bundle.getString("GUI.titleLabel.text"));
        titleLabel.setEnabled(false);

        releasedLabel.setText(bundle.getString("GUI.releasedLabel.text"));
        releasedLabel.setEnabled(false);

        genreLabel.setLabelFor(genreList);
        genreLabel.setText(bundle.getString("GUI.genreLabel.text"));
        genreLabel.setEnabled(false);

        ratingComboBox.setMaximumRowCount(11);
        ratingComboBox.setEnabled(false);
        ratingComboBox.addItem(bundle.getString("any"));

        ratingLabel.setLabelFor(ratingComboBox);
        ratingLabel.setText(bundle.getString("GUI.ratingLabel.text"));
        ratingLabel.setEnabled(false);

        resultsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setAutoscrolls(true);

        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "", "Title", "Year", "Rating"
                }
        ) {
            private static final long serialVersionUID = 1L;
            Class<?>[] types = new Class<?>[]{
                Object.class, String.class, String.class, String.class
            };
            boolean[] canEdit = new boolean[]{
                false, false, false, false
            };

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        resultsTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        resultsTable.setEnabled(false);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setMaximumSize(new Dimension(32767, 32767));
        resultsTable.setMinimumSize(new Dimension(24, 24));
        resultsTable.setName("Search Results");
        resultsTable.setOpaque(false);
        resultsTable.setPreferredSize(null);
        resultsTable.setRowHeight(90);
        resultsScrollPane.setViewportView(resultsTable);
        if (resultsTable.getColumnModel().getColumnCount() > 0) {
            resultsTable.getColumnModel().getColumn(0).setMinWidth(61);
            resultsTable.getColumnModel().getColumn(0).setPreferredWidth(61);
            resultsTable.getColumnModel().getColumn(0).setMaxWidth(61);
            resultsTable.getColumnModel().getColumn(0).setHeaderValue(Constant.IMAGE_COL);
            resultsTable.getColumnModel().getColumn(1).setPreferredWidth(798);
            resultsTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("GUI.resultsTable.columnModel.title1"));
            resultsTable.getColumnModel().getColumn(2).setMinWidth(65);
            resultsTable.getColumnModel().getColumn(2).setPreferredWidth(65);
            resultsTable.getColumnModel().getColumn(2).setMaxWidth(100);
            resultsTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("GUI.resultsTable.columnModel.title2"));
            resultsTable.getColumnModel().getColumn(3).setMinWidth(65);
            resultsTable.getColumnModel().getColumn(3).setPreferredWidth(65);
            resultsTable.getColumnModel().getColumn(3).setMaxWidth(100);
            resultsTable.getColumnModel().getColumn(3).setHeaderValue(bundle.getString("GUI.resultsTable.columnModel.title3"));
        }

        progressBar.setEnabled(false);
        progressBar.setStringPainted(true);

        progressBarLabel.setLabelFor(progressBar);
        progressBarLabel.setText(bundle.getString("GUI.progressBarLabel.text"));
        progressBarLabel.setEnabled(false);

        resultsLabel.setText(bundle.getString("results") + " " + 0);
        resultsLabel.setEnabled(false);

        searchButton.setText(bundle.getString("GUI.searchButton.text"));
        searchButton.setEnabled(false);

        stopButton.setText(bundle.getString("GUI.stopButton.text"));
        stopButton.setEnabled(false);

        anyTitleCheckBox.setText(bundle.getString("GUI.anyTitleCheckBox.text") + " ");
        anyTitleCheckBox.setBorder(null);
        anyTitleCheckBox.setEnabled(false);
        anyTitleCheckBox.setFocusPainted(false);
        anyTitleCheckBox.setMargin(new Insets(2, 0, 2, 2));

        genreScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        genreList.setEnabled(false);
        genreList.setListData(new String[]{bundle.getString("any")});
        genreScrollPane.setViewportView(genreList);

        loadMoreResultsButton.setText(bundle.getString("GUI.loadMoreResultsButton.text"));
        loadMoreResultsButton.setEnabled(false);

        typeLabel.setLabelFor(typeComboBox);
        typeLabel.setText(bundle.getString("GUI.typeLabel.text"));
        typeLabel.setEnabled(false);

        typeComboBox.setEnabled(false);
        typeComboBox.addItem(bundle.getString("GUI.typeComboBox.model").split(",")[0]);

        releasedToLabel.setText(bundle.getString("GUI.releasedToLabel.text"));
        releasedToLabel.setEnabled(false);

        linkProgressBar.setEnabled(false);
        linkProgressBar.setRequestFocusEnabled(false);
        linkProgressBar.setString(bundle.getString("GUI.linkProgressBar.string"));

        hqVideoTypeCheckBox.setText(Constant.HQ);
        hqVideoTypeCheckBox.setEnabled(false);

        dvdCheckBox.setText(Constant.DVD);
        dvdCheckBox.setEnabled(false);

        hd720CheckBox.setText(Constant.HD720);
        hd720CheckBox.setEnabled(false);

        hd1080CheckBox.setText(Constant.HD1080);
        hd1080CheckBox.setEnabled(false);

        popularMoviesButton.setText(bundle.getString("GUI.popularMoviesButton.text"));
        popularMoviesButton.setEnabled(false);

        popularTVShowsButton.setText(bundle.getString("GUI.popularTVShowsButton.text"));
        popularTVShowsButton.setEnabled(false);

        closeBoxButton.setText(null);
        closeBoxButton.setEnabled(false);
        closeBoxButton.setMargin(new Insets(0, 0, 0, 0));

        loadingLabel.setText(null);

        readSummaryButton.setText(bundle.getString("GUI.readSummaryButton.text"));
        readSummaryButton.setEnabled(false);

        watchTrailerButton.setText(bundle.getString("GUI.watchTrailerButton.text"));
        watchTrailerButton.setEnabled(false);

        playButton.setText(bundle.getString("GUI.playButton.text"));
        playButton.setEnabled(false);

        downloadLink1Button.setText(bundle.getString("GUI.downloadLink1Button.text"));
        downloadLink1Button.setEnabled(false);

        downloadLink2Button.setText(bundle.getString("GUI.downloadLink2Button.text"));
        downloadLink2Button.setEnabled(false);

        statusBarTextField.setEditable(false);
        statusBarTextField.setFont(new Font("Verdana", 0, 10));
        statusBarTextField.setText(INITIALIZING + percentFormat.format(0.0) + DONE);
        statusBarTextField.setBorder(BorderFactory.createEtchedBorder());

        connectionIssueButton.setText(null);
        connectionIssueButton.setBorderPainted(false);
        connectionIssueButton.setEnabled(false);
        connectionIssueButton.setMargin(new Insets(0, 0, 0, 0));

        startDateTextField.setEnabled(false);

        endDateTextField.setEnabled(false);

        findTextField.setBorder(null);
        findTextField.setEnabled(false);

        fileMenu.setText(bundle.getString("GUI.fileMenu.text"));
        fileMenu.setEnabled(false);
        menuBar.add(fileMenu);

        editMenu.setText(bundle.getString("GUI.editMenu.text"));
        editMenu.setEnabled(false);
        menuBar.add(editMenu);

        viewMenu.setText(bundle.getString("GUI.viewMenu.text"));
        viewMenu.setEnabled(false);
        menuBar.add(viewMenu);

        searchMenu.setText(bundle.getString("GUI.searchMenu.text"));
        searchMenu.setEnabled(false);
        menuBar.add(searchMenu);

        playlistMenu.setText(bundle.getString("GUI.playlistMenu.text"));
        playlistMenu.setEnabled(false);
        menuBar.add(playlistMenu);

        downloadMenu.setText(bundle.getString("GUI.downloadMenu.text"));
        downloadMenu.setEnabled(false);
        menuBar.add(downloadMenu);

        helpMenu.setText(bundle.getString("GUI.helpMenu.text"));
        helpMenu.setEnabled(false);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

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
                                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(loadingLabel))
                                .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addComponent(anyTitleCheckBox)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(titleLabel)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(titleTextField, GroupLayout.DEFAULT_SIZE, 775, Short.MAX_VALUE))
                                                .addGroup(layout.createSequentialGroup()
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
                                                        .addComponent(startDateTextField)
                                                        .addGap(7, 7, 7)
                                                        .addComponent(releasedToLabel)
                                                        .addGap(6, 6, 6)
                                                        .addComponent(endDateTextField)))
                                        .addGap(18, 18, 18)
                                        .addComponent(genreLabel)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(genreScrollPane, GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE)
                                        .addGap(18, 18, 18)
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                .addComponent(searchButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(stopButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
                                        .addGap(18, 18, 18)
                                        .addComponent(findTextField, GroupLayout.DEFAULT_SIZE, 581, Short.MAX_VALUE)
                                        .addGap(18, 18, 18)
                                        .addComponent(connectionIssueButton)))
                        .addContainerGap())
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[]{dvdCheckBox, hqVideoTypeCheckBox});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[]{hd1080CheckBox, hd720CheckBox});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[]{popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[]{downloadLink1Button, downloadLink2Button, playButton, readSummaryButton, watchTrailerButton});

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
                                                .addComponent(startDateTextField, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(endDateTextField, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
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

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{anyTitleCheckBox, endDateTextField, ratingComboBox, startDateTextField, titleTextField, typeComboBox});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{genreLabel, ratingLabel, releasedLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{progressBarLabel, resultsLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{linkProgressBar, loadMoreResultsButton, progressBar});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{searchButton, stopButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{downloadLink1Button, downloadLink2Button, findTextField, playButton, readSummaryButton, watchTrailerButton});

        setSize(new Dimension(1337, 773));
        setLocationRelativeTo(null);
    }

    JCheckBox anyTitleCheckBox;
    JButton closeBoxButton;
    JButton connectionIssueButton;
    JButton downloadLink1Button;
    JButton downloadLink2Button;
    JMenu downloadMenu;
    JCheckBox dvdCheckBox;
    JMenu editMenu;
    JTextField endDateTextField;
    JMenu fileMenu;
    JTextField findTextField;
    JLabel genreLabel;
    JList genreList;
    JScrollPane genreScrollPane;
    JCheckBox hd1080CheckBox;
    JCheckBox hd720CheckBox;
    JMenu helpMenu;
    JCheckBox hqVideoTypeCheckBox;
    JProgressBar linkProgressBar;
    JButton loadMoreResultsButton;
    JLabel loadingLabel;
    JMenuBar menuBar;
    JButton playButton;
    JMenu playlistMenu;
    JButton popularMoviesButton;
    JButton popularTVShowsButton;
    JProgressBar progressBar;
    JLabel progressBarLabel;
    JComboBox ratingComboBox;
    JLabel ratingLabel;
    JButton readSummaryButton;
    JLabel releasedLabel;
    JLabel releasedToLabel;
    JLabel resultsLabel;
    JScrollPane resultsScrollPane;
    JTable resultsTable;
    JButton searchButton;
    JMenu searchMenu;
    JTextField startDateTextField;
    JTextField statusBarTextField;
    JButton stopButton;
    JLabel titleLabel;
    JTextField titleTextField;
    JComboBox typeComboBox;
    JLabel typeLabel;
    JMenu viewMenu;
    JButton watchTrailerButton;
}
