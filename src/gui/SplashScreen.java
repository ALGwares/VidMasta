package gui;

import i18n.Bundle;
import i18n.I18nStr;
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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
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

        loadingLabel.setIcon(UI.icon("loading.gif"));

        Color bgColor = getBackground();
        genreList.setBackground(bgColor);
        findTextField.setBackground(bgColor);
        findTextField.setForeground(bgColor);

        setSize(new Dimension(1022, 680));
        setIconImage(Toolkit.getDefaultToolkit().getImage(Constant.PROGRAM_DIR + "icon16x16.png"));
        UI.centerOnScreen(this);
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
        searchButton = new JButton();
        genreScrollPane = new JScrollPane();
        genreList = new JList();
        loadMoreResultsButton = new JButton();
        typeLabel = new JLabel();
        typeComboBox = new JComboBox();
        releasedToLabel = new JLabel();
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

        searchButton.setText(bundle.getString("GUI.searchButton.text"));
        searchButton.setEnabled(false);

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

        popularMoviesButton.setText(bundle.getString("GUI.popularMoviesButton.text"));
        popularMoviesButton.setEnabled(false);

        popularTVShowsButton.setText(bundle.getString("GUI.popularTVShowsButton.text"));
        popularTVShowsButton.setEnabled(false);

        loadingLabel.setText(null);

        readSummaryButton.setText(bundle.getString("GUI.readSummaryButton.text"));
        readSummaryButton.setEnabled(false);

        watchTrailerButton.setText(bundle.getString("GUI.watchTrailerButton.text"));
        watchTrailerButton.setEnabled(false);

        downloadLink1Button.setText(bundle.getString("GUI.downloadLink1Button.text"));
        downloadLink1Button.setEnabled(false);

        downloadLink2Button.setText(bundle.getString("GUI.downloadLink2Button.text"));
        downloadLink2Button.setEnabled(false);

        statusBarTextField.setEditable(false);
        statusBarTextField.setFont(new Font("Verdana", 0, 10));
        statusBarTextField.setText(INITIALIZING + percentFormat.format(0.0) + DONE);
        statusBarTextField.setBorder(BorderFactory.createEtchedBorder());

        searchProgressTextField.setEditable(false);
        searchProgressTextField.setFont(new Font("Verdana", 0, 10));
        searchProgressTextField.setHorizontalAlignment(JTextField.RIGHT);
        searchProgressTextField.setText(' ' + I18nStr.replace(bundle.getString("results"), 0, I18nStr.percent(0, 0)) + ' ');
        UI.resize(AbstractComponent.newInstance(searchProgressTextField), ' ' + I18nStr.replace(bundle.getString("results"), 11111, I18nStr.percent(1, 0)) + ' ',
                searchProgressTextField.getText());
        searchProgressTextField.setBorder(BorderFactory.createEtchedBorder());

        exitBackupModeButton.setBorderPainted(false);
        exitBackupModeButton.setEnabled(false);
        exitBackupModeButton.setMinimumSize(new Dimension(0, 0));
        exitBackupModeButton.setMaximumSize(new Dimension(0, 0));

        connectionIssueButton.setText(null);
        connectionIssueButton.setBorder(BorderFactory.createEtchedBorder());
        connectionIssueButton.setEnabled(false);
        connectionIssueButton.setMargin(new Insets(0, 0, 0, 0));
        connectionIssueButton.setMaximumSize(new Dimension(18, 18));
        connectionIssueButton.setPreferredSize(new Dimension(0, 0));

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
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                                .addComponent(resultsScrollPane, Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(readSummaryButton)
                                        .addPreferredGap(ComponentPlacement.UNRELATED)
                                        .addComponent(watchTrailerButton)
                                        .addPreferredGap(ComponentPlacement.UNRELATED)
                                        .addComponent(downloadLink1Button)
                                        .addGap(1, 1, 1)
                                        .addComponent(downloadLink2Button)
                                        .addPreferredGap(ComponentPlacement.UNRELATED)
                                        .addComponent(exitBackupModeButton)
                                        .addPreferredGap(ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                                                        .addComponent(startDateTextField, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGap(7, 7, 7)
                                                        .addComponent(releasedToLabel)
                                                        .addGap(6, 6, 6)
                                                        .addComponent(endDateTextField, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addGap(18, 18, 18)
                                        .addComponent(genreLabel)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(genreScrollPane)
                                        .addGap(18, 18, 18)
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                .addComponent(searchButton, Alignment.TRAILING)
                                                .addComponent(loadingLabel, Alignment.TRAILING))))
                        .addContainerGap())
                .addGroup(layout.createSequentialGroup()
                        .addComponent(statusBarTextField)
                        .addGap(0, 0, 0)
                        .addComponent(connectionIssueButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(searchProgressTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[]{popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[]{downloadLink1Button, readSummaryButton, watchTrailerButton});

        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(loadingLabel)
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
                                                        .addComponent(startDateTextField, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(endDateTextField, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
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
                                        .addComponent(loadMoreResultsButton)))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(statusBarTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(searchProgressTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(connectionIssueButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
        );

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{endDateTextField, ratingComboBox, startDateTextField, titleTextField, typeComboBox});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{genreLabel, ratingLabel, releasedLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{downloadLink1Button, downloadLink2Button, findTextField, readSummaryButton, watchTrailerButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[]{connectionIssueButton, searchProgressTextField, statusBarTextField});
    }

    JButton connectionIssueButton;
    JButton downloadLink1Button;
    JButton downloadLink2Button;
    JMenu downloadMenu;
    JMenu editMenu;
    JTextField endDateTextField;
    JButton exitBackupModeButton;
    JMenu fileMenu;
    JTextField findTextField;
    JLabel genreLabel;
    JList genreList;
    JScrollPane genreScrollPane;
    JMenu helpMenu;
    JButton loadMoreResultsButton;
    JLabel loadingLabel;
    JMenuBar menuBar;
    JMenu playlistMenu;
    JButton popularMoviesButton;
    JButton popularTVShowsButton;
    JComboBox ratingComboBox;
    JLabel ratingLabel;
    JButton readSummaryButton;
    JLabel releasedLabel;
    JLabel releasedToLabel;
    JScrollPane resultsScrollPane;
    JTable resultsTable;
    JButton searchButton;
    JMenu searchMenu;
    JTextField searchProgressTextField;
    JTextField startDateTextField;
    JTextField statusBarTextField;
    JLabel titleLabel;
    JTextField titleTextField;
    JComboBox typeComboBox;
    JLabel typeLabel;
    JMenu viewMenu;
    JButton watchTrailerButton;
}
