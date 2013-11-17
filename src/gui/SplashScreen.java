package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
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
    private int progress;

    public SplashScreen() {
        initComponents();

        closeBoxButton.setIcon(new ImageIcon(Constant.PROGRAM_DIR + "closeBox.png"));
        loadingLabel.setIcon(new ImageIcon(Constant.PROGRAM_DIR + "loading.gif"));
        connectionIssueButton.setIcon(new ImageIcon(Constant.PROGRAM_DIR + "noWarning.png"));

        Color bgColor = getBackground();
        genreList.setListData(new String[]{Constant.ANY_GENRE});
        genreList.setBackground(bgColor);
        ratingComboBox.addItem(Constant.ANY);

        findTextField.setBackground(bgColor);
        findTextField.setForeground(bgColor);

        Dimension windowSize = new Dimension(1000, 680);
        setSize(windowSize);
        setIconImage(Toolkit.getDefaultToolkit().getImage(Constant.PROGRAM_DIR + "icon16x16.png"));
        setLocation(screenCenter(windowSize));
    }

    static Point screenCenter(Dimension windowSize) {
        Rectangle screenBounds = getUsableScreenBounds();
        if (windowSize.height > screenBounds.height) {
            windowSize.height = screenBounds.height;
        }
        if (windowSize.width > screenBounds.width) {
            windowSize.width = screenBounds.width;
        }
        return new Point((screenBounds.width - windowSize.width) / 2, (screenBounds.height - windowSize.height) / 2);
    }

    static Rectangle getUsableScreenBounds() {
        GraphicsConfiguration graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = graphicsConfig.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfig);
        bounds.y += insets.top;
        bounds.x += insets.left;
        bounds.height -= (insets.top + insets.bottom);
        bounds.width -= (insets.left + insets.right);
        return bounds;
    }

    void progress() {
        statusBarTextField.setText(" Initializing - " + ((int) ((++progress / (double) MAX_PROGRESS) * 100)) + "% Done");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
        toLabel = new JLabel();
        linkProgressBar = new JProgressBar();
        anyVideoTypeRadioButton = new JRadioButton();
        dvdRadioButton = new JRadioButton();
        hd720RadioButton = new JRadioButton();
        hd1080RadioButton = new JRadioButton();
        popularMoviesButton = new JButton();
        popularTVShowsButton = new JButton();
        closeBoxButton = new JButton();
        loadingLabel = new JLabel();
        readSummaryButton = new JButton();
        watchTrailerButton = new JButton();
        downloadLink1Button = new JButton();
        downloadLink2Button = new JButton();
        watchSource1Button = new JButton();
        watchSource2Button = new JButton();
        statusBarTextField = new JTextField();
        connectionIssueButton = new JButton();
        findTextField = new JTextField();
        startDateTextField = new JTextField();
        endDateTextField = new JTextField();
        menuBar = new JMenuBar();
        fileMenu = new JMenu();
        editMenu = new JMenu();
        viewMenu = new JMenu();
        searchMenu = new JMenu();
        downloadMenu = new JMenu();
        helpMenu = new JMenu();

        setTitle(Constant.APP_TITLE);
        setMinimumSize(null);

        titleTextField.setEnabled(false);

        titleLabel.setLabelFor(titleTextField);
        titleLabel.setText("Title:");
        titleLabel.setEnabled(false);

        releasedLabel.setText("Released:");
        releasedLabel.setEnabled(false);

        genreLabel.setLabelFor(genreScrollPane);
        genreLabel.setText("Genre:");
        genreLabel.setEnabled(false);

        ratingComboBox.setMaximumRowCount(11);
        ratingComboBox.setEnabled(false);

        ratingLabel.setLabelFor(ratingComboBox);
        ratingLabel.setText("Rating (minimum):");
        ratingLabel.setEnabled(false);

        resultsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setAutoscrolls(true);

        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", "", "", "", "", ""
            }
        ) {
            Class[] types = new Class [] {
                Object.class, String.class, String.class, String.class, String.class, String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        resultsTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        resultsTable.setEnabled(false);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setMaximumSize(new Dimension(32767, 32767));
        resultsTable.setMinimumSize(new Dimension(24, 24));
        resultsTable.setName("Search Results"); // NOI18N
        resultsTable.setOpaque(false);
        resultsTable.setPreferredSize(null);
        resultsTable.setRowHeight(90);
        resultsScrollPane.setViewportView(resultsTable);
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
        resultsTable.getColumnModel().getColumn(4).setHeaderValue(Constant.SUMMARY_COL);
        resultsTable.getColumnModel().getColumn(5).setMinWidth(0);
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(0);
        resultsTable.getColumnModel().getColumn(5).setMaxWidth(0);
        resultsTable.getColumnModel().getColumn(5).setHeaderValue(Constant.ID_COL);

        progressBar.setEnabled(false);
        progressBar.setStringPainted(true);

        progressBarLabel.setLabelFor(progressBar);
        progressBarLabel.setText("Search Progress:");
        progressBarLabel.setEnabled(false);

        resultsLabel.setText("Results: 0");
        resultsLabel.setEnabled(false);

        searchButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        searchButton.setText("Search");
        searchButton.setEnabled(false);

        stopButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        stopButton.setText("Stop");
        stopButton.setEnabled(false);

        anyTitleCheckBox.setText("Any");
        anyTitleCheckBox.setBorder(null);
        anyTitleCheckBox.setEnabled(false);
        anyTitleCheckBox.setFocusPainted(false);
        anyTitleCheckBox.setMargin(new Insets(2, 0, 2, 2));

        genreList.setEnabled(false);
        genreScrollPane.setViewportView(genreList);

        loadMoreResultsButton.setText("Load More");
        loadMoreResultsButton.setEnabled(false);

        typeLabel.setLabelFor(typeComboBox);
        typeLabel.setText("Type:");
        typeLabel.setEnabled(false);

        typeComboBox.setModel(new DefaultComboBoxModel(new String[]{"Movie", Constant.TV_SHOW}));
        typeComboBox.setEnabled(false);

        toLabel.setText("to");
        toLabel.setEnabled(false);

        linkProgressBar.setEnabled(false);
        linkProgressBar.setRequestFocusEnabled(false);
        linkProgressBar.setString("Searching");

        anyVideoTypeRadioButton.setText(Constant.ANY);
        anyVideoTypeRadioButton.setEnabled(false);

        dvdRadioButton.setText(Constant.DVD);
        dvdRadioButton.setEnabled(false);

        hd720RadioButton.setText(Constant.HD720);
        hd720RadioButton.setEnabled(false);

        hd1080RadioButton.setText(Constant.HD1080);
        hd1080RadioButton.setEnabled(false);

        popularMoviesButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        popularMoviesButton.setText("Popular Movies");
        popularMoviesButton.setEnabled(false);

        popularTVShowsButton.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        popularTVShowsButton.setText("Popular TV Shows");
        popularTVShowsButton.setEnabled(false);

        closeBoxButton.setText(null);
        closeBoxButton.setEnabled(false);
        closeBoxButton.setMargin(new Insets(0, 0, 0, 0));

        loadingLabel.setText(null);

        readSummaryButton.setText("Read Summary");
        readSummaryButton.setEnabled(false);

        watchTrailerButton.setText("Watch Trailer");
        watchTrailerButton.setEnabled(false);

        downloadLink1Button.setText("Download (Link 1)");
        downloadLink1Button.setEnabled(false);

        downloadLink2Button.setText("Download (Link 2)");
        downloadLink2Button.setEnabled(false);

        watchSource1Button.setText("Watch (Source 1)");
        watchSource1Button.setEnabled(false);

        watchSource2Button.setText("Watch (Source 2)");
        watchSource2Button.setEnabled(false);

        statusBarTextField.setEditable(false);
        statusBarTextField.setFont(new Font("Verdana", 0, 10)); // NOI18N
        statusBarTextField.setText(" Initializing - 0% Done");
        statusBarTextField.setBorder(BorderFactory.createEtchedBorder());

        connectionIssueButton.setText(null);
        connectionIssueButton.setBorderPainted(false);
        connectionIssueButton.setEnabled(false);
        connectionIssueButton.setMargin(new Insets(0, 0, 0, 0));

        findTextField.setBorder(null);
        findTextField.setEnabled(false);

        startDateTextField.setEnabled(false);

        endDateTextField.setEnabled(false);

        fileMenu.setText("File");
        fileMenu.setEnabled(false);
        menuBar.add(fileMenu);

        editMenu.setText("Edit");
        editMenu.setEnabled(false);
        menuBar.add(editMenu);

        viewMenu.setText("View");
        viewMenu.setEnabled(false);
        menuBar.add(viewMenu);

        searchMenu.setText("Search");
        searchMenu.setEnabled(false);
        menuBar.add(searchMenu);

        downloadMenu.setText("Download");
        downloadMenu.setEnabled(false);
        menuBar.add(downloadMenu);

        helpMenu.setText("Help");
        helpMenu.setEnabled(false);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(statusBarTextField, GroupLayout.DEFAULT_SIZE, 1329, Short.MAX_VALUE)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(resultsScrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 1309, Short.MAX_VALUE)
                    .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(progressBarLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED, 145, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(dvdRadioButton)
                            .addComponent(anyVideoTypeRadioButton))
                        .addGap(0, 0, 0)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(hd720RadioButton)
                            .addComponent(hd1080RadioButton))
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
                                .addComponent(titleTextField, GroupLayout.DEFAULT_SIZE, 848, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(typeLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(typeComboBox, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addComponent(ratingLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(ratingComboBox, GroupLayout.PREFERRED_SIZE, 59, GroupLayout.PREFERRED_SIZE)
                                .addGap(19, 19, 19)
                                .addComponent(releasedLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(startDateTextField)
                                .addGap(7, 7, 7)
                                .addComponent(toLabel)
                                .addGap(7, 7, 7)
                                .addComponent(endDateTextField)))
                        .addGap(18, 18, 18)
                        .addComponent(genreLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(genreScrollPane, GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                            .addComponent(searchButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(stopButton, GroupLayout.PREFERRED_SIZE, 71, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(readSummaryButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(watchTrailerButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(downloadLink1Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(downloadLink2Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(watchSource1Button)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(watchSource2Button)
                        .addGap(18, 18, 18)
                        .addComponent(findTextField, GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(connectionIssueButton)))
                .addContainerGap())
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {anyVideoTypeRadioButton, dvdRadioButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {hd1080RadioButton, hd720RadioButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {searchButton, stopButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {downloadLink1Button, downloadLink2Button, readSummaryButton, watchSource1Button, watchSource2Button, watchTrailerButton});

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {ratingComboBox, typeComboBox});

        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
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
                                .addComponent(toLabel)
                                .addComponent(endDateTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(startDateTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
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
                    .addComponent(findTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(Alignment.CENTER)
                    .addComponent(resultsLabel)
                    .addComponent(loadMoreResultsButton)
                    .addComponent(closeBoxButton)
                    .addComponent(linkProgressBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(hd720RadioButton)
                        .addGap(0, 0, 0)
                        .addComponent(hd1080RadioButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(anyVideoTypeRadioButton)
                        .addGap(0, 0, 0)
                        .addComponent(dvdRadioButton))
                    .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                    .addComponent(progressBarLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(statusBarTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {anyTitleCheckBox, ratingComboBox, titleTextField, typeComboBox});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {genreLabel, ratingLabel, releasedLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {progressBarLabel, resultsLabel});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {linkProgressBar, loadMoreResultsButton, progressBar});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {searchButton, stopButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {popularMoviesButton, popularTVShowsButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {downloadLink1Button, downloadLink2Button, findTextField, readSummaryButton, watchSource1Button, watchSource2Button, watchTrailerButton});

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {endDateTextField, startDateTextField});

        setSize(new Dimension(1337, 773));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    JCheckBox anyTitleCheckBox;
    JRadioButton anyVideoTypeRadioButton;
    JButton closeBoxButton;
    JButton connectionIssueButton;
    JButton downloadLink1Button;
    JButton downloadLink2Button;
    JMenu downloadMenu;
    JRadioButton dvdRadioButton;
    JMenu editMenu;
    JTextField endDateTextField;
    JMenu fileMenu;
    JTextField findTextField;
    JLabel genreLabel;
    JList genreList;
    JScrollPane genreScrollPane;
    JRadioButton hd1080RadioButton;
    JRadioButton hd720RadioButton;
    JMenu helpMenu;
    JProgressBar linkProgressBar;
    JButton loadMoreResultsButton;
    JLabel loadingLabel;
    JMenuBar menuBar;
    JButton popularMoviesButton;
    JButton popularTVShowsButton;
    JProgressBar progressBar;
    JLabel progressBarLabel;
    JComboBox ratingComboBox;
    JLabel ratingLabel;
    JButton readSummaryButton;
    JLabel releasedLabel;
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
    JLabel toLabel;
    JComboBox typeComboBox;
    JLabel typeLabel;
    JMenu viewMenu;
    JButton watchSource1Button;
    JButton watchSource2Button;
    JButton watchTrailerButton;
    // End of variables declaration//GEN-END:variables
}
