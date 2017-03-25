package gui;

import debug.Debug;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultRowSorter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SortOrder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import str.Str;
import util.Constant;
import util.ThrowableUtil;

public class UI {

    public static void registerCutCopyPasteKeyboardActions(JComponent component, ActionListener listener) {
        component.registerKeyboardAction(listener, Constant.CUT, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);
        component.registerKeyboardAction(listener, Constant.COPY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);
        component.registerKeyboardAction(listener, Constant.PASTE, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);
    }

    public static void initCountComboBoxes(int incrementSizeIndex, int maxCountIndex, JComboBox... comboBoxes) {
        int incrementSize = Integer.parseInt(Str.get(incrementSizeIndex)), maxCount = Integer.parseInt(Str.get(maxCountIndex));
        for (int i = incrementSize; i <= maxCount; i += incrementSize) {
            String count = String.valueOf(i);
            for (JComboBox comboBox : comboBoxes) {
                comboBox.addItem(count);
            }
        }
        for (JComboBox comboBox : comboBoxes) {
            comboBox.setSelectedIndex(0);
        }
    }

    public static void addMouseListener(MouseListener mouseListener, JComponent... components) {
        for (JComponent component : components) {
            component.addMouseListener(mouseListener);
        }
    }

    public static void addPopupMenu(final JPopupMenu popupMenu, JComponent... components) {
        addMouseListener(new AbstractPopupListener() {
            @Override
            protected void showPopup(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    show(popupMenu, evt);
                }
            }
        }, components);
    }

    public static void addPopupMenu(final JPopupMenu popupMenu, final SyncTable syncTable, final boolean oneSelectedRowMode) {
        addMouseListener(new AbstractPopupListener() {
            @Override
            protected void showPopup(MouseEvent evt) {
                int row, col;
                if (!evt.isPopupTrigger() || (row = syncTable.rowAtPoint(evt.getPoint())) == -1 || (col = syncTable.columnAtPoint(evt.getPoint()))
                        == -1) {
                    return;
                }

                if (oneSelectedRowMode || syncTable.getSelectedRowCount() <= 1) {
                    syncTable.clearSelection();
                    syncTable.changeSelection(row, col, false, false);
                }
                show(popupMenu, evt);
            }
        }, syncTable.table);
    }

    public static boolean isClipboardEmpty() {
        Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return ((String) contents.getTransferData(DataFlavor.stringFlavor)).isEmpty();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                return false;
            }
        }
        return true;
    }

    public static String[] copy(Object[] objs) {
        return Arrays.copyOf(objs, objs.length, String[].class);
    }

    public static String[] selectAnyIfNoSelectionAndCopy(JList list) {
        if (list.isSelectionEmpty()) {
            list.setSelectedValue(Constant.ANY, true);
        }
        return copy(list.getSelectedValues());
    }

    public static int indexOf(DefaultListModel listModel, String element) {
        Object[] items = listModel.toArray();
        for (int i = 0; i < items.length; i++) {
            if (element.equalsIgnoreCase((String) items[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void sort(DefaultListModel listModel) {
        Object[] items = listModel.toArray();
        Arrays.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((String) o1).compareToIgnoreCase((String) o2);
            }
        });
        listModel.removeAllElements();
        for (Object item : items) {
            listModel.addElement(item);
        }
    }

    public static void textComponentDelete(JTextComponent textField) {
        int start = textField.getSelectionStart();
        String text = textField.getText();
        String s1 = text.substring(0, start);
        String s2 = text.substring(textField.getSelectionEnd(), text.length());
        text = s1 + s2;
        textField.setText(text);
        if (start < text.length()) {
            textField.setCaretPosition(start);
        }
    }

    public static void updateList(JList list) {
        int[] selection = list.getSelectedIndices();
        if (selection.length < 2) {
            return;
        }

        ListModel listModel = list.getModel();
        for (int i = 0; i < selection.length; i++) {
            if (Constant.ANY.equals(listModel.getElementAt(selection[i]))) {
                int[] newSelection = new int[selection.length - 1];

                int k = 0, j = 0;
                for (; j < i; j++, k++) {
                    newSelection[k] = selection[j];
                }
                for (++j; j < selection.length; j++, k++) {
                    newSelection[k] = selection[j];
                }

                list.setSelectedIndices(newSelection);
                return;
            }
        }
    }

    public static void exportToClipboard(String str) {
        exportToClipboard(null, str);
    }

    public static boolean exportToClipboard(String imagePath, final String str) {
        final List<DataFlavor> dataFlavors = new ArrayList<DataFlavor>(2);
        final Image image;
        if (imagePath == null) {
            image = null;
        } else {
            image = image(new ImageIcon(imagePath));
            dataFlavors.add(DataFlavor.imageFlavor);
        }
        if (!str.isEmpty()) {
            dataFlavors.add(DataFlavor.stringFlavor);
        }
        if (dataFlavors.isEmpty()) {
            return false;
        }
        final DataFlavor[] dataFlavorsArray = dataFlavors.toArray(new DataFlavor[dataFlavors.size()]);

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                if (flavor.equals(DataFlavor.stringFlavor)) {
                    return str;
                }
                return image;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return dataFlavorsArray;
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return dataFlavors.contains(flavor);
            }
        }, new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
            }
        });

        return true;
    }

    public static boolean isMaxSize(Component component, Dimension size) {
        Rectangle screenBounds = getUsableScreenBounds(component);
        return size.width >= screenBounds.width && size.height >= screenBounds.height;
    }

    public static void centerOnScreen(Window window) {
        window.setLocation(screenCenter(window));
    }

    public static Point screenCenter(Component component) {
        Dimension componentSize = component.getSize();
        Rectangle screenBounds = getUsableScreenBounds(component, true);
        if (componentSize.height > screenBounds.height) {
            componentSize.height = screenBounds.height;
        }
        if (componentSize.width > screenBounds.width) {
            componentSize.width = screenBounds.width;
        }
        return new Point(screenBounds.x + ((screenBounds.width - componentSize.width) / 2), screenBounds.y + ((screenBounds.height - componentSize.height) / 2));
    }

    public static Rectangle getUsableScreenBounds(Component component) {
        return getUsableScreenBounds(component, false);
    }

    public static Rectangle getUsableScreenBounds(Component component, boolean usePoint) {
        GraphicsConfiguration graphicsConfig = component.getGraphicsConfiguration();
        if (graphicsConfig == null) {
            graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        Rectangle bounds = graphicsConfig.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfig);
        Point point = (usePoint ? new Point(bounds.x + insets.left, bounds.y + insets.top) : new Point(insets.left, insets.top));
        return new Rectangle(point.x, point.y, bounds.width - (insets.left + insets.right), bounds.height - (insets.top + insets.bottom));
    }

    public static boolean isOnScreen(Point point) {
        for (GraphicsDevice graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            GraphicsConfiguration graphicsConfig = graphicsDevice.getDefaultConfiguration();
            Rectangle bounds = graphicsConfig.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfig);
            if ((new Rectangle(bounds.x + insets.left, bounds.y + insets.top, bounds.width - (insets.left + insets.right), bounds.height - (insets.top
                    + insets.bottom))).contains(point.x, point.y)) {
                return true;
            }
        }
        return false;
    }

    public static BufferedImage image(Icon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        return image;
    }

    public static String innerHTML(String htmlText, int beginOffset) {
        int beginIndex = 6, endOffSet = 7;
        if (htmlText.startsWith("<b>", beginIndex)) {
            beginIndex += 3;
            endOffSet += 4;
        }
        return htmlText.substring(beginIndex + beginOffset, htmlText.length() - endOffSet);
    }

    public static String innerHTML(String htmlText) {
        return htmlText.startsWith("<html>") ? innerHTML(htmlText, 0) : htmlText;
    }

    public static Icon icon(String name) {
        return new ImageIcon(Constant.PROGRAM_DIR + name);
    }

    public static void setIcon(AbstractButton button, String name) {
        button.setIcon(icon(name + ".png"));
    }

    public static void updateToggleButton(AbstractButton button, String nameKey, boolean init) {
        String name = Str.str(nameKey);
        button.setName(name);
        if (init) {
            button.putClientProperty(Constant.STOP_KEY, new AtomicBoolean());
        } else {
            button.setText(isStop(button) ? Str.str(Constant.STOP_KEY) : name);
        }
    }

    public static void initToggleButton(AbstractButton button, String startIconName) {
        Icon startIcon = icon(startIconName);
        button.setIcon(startIcon);
        button.putClientProperty(Constant.STOP_KEY, new AtomicBoolean());
        button.putClientProperty(Constant.STOP_ICON_KEY, icon("cancel.png"));
        button.putClientProperty(Constant.START_ICON_KEY, startIcon);
    }

    public static boolean isStop(AbstractButton button) {
        return ((AtomicBoolean) button.getClientProperty(Constant.STOP_KEY)).get();
    }

    public static void enable(AbstractButton[] primaryButtons, Boolean startPrimary, Component[] secondaryComponents, Boolean enableSecondary) {
        enable(primaryButtons, true, startPrimary, null, null, secondaryComponents, enableSecondary, null, null);
    }

    public static void enable(AbstractButton[] primaryButtons, Boolean enablePrimary, Boolean startPrimary, AbstractButton primaryButtons2, Boolean enablePrimary2,
            Component[] secondaryComponents, Boolean enableSecondary, Component[] secondaryComponents2, Boolean enableSecondary2) {
        if (enablePrimary2 != null) {
            enable(enablePrimary2, primaryButtons2);
        }
        if (enablePrimary != null) {
            enable(enablePrimary, primaryButtons);
        }
        if (startPrimary != null) {
            for (AbstractButton primaryButton : primaryButtons) {
                ((AtomicBoolean) primaryButton.getClientProperty(Constant.STOP_KEY)).set(!startPrimary);
                Object icon = primaryButton.getClientProperty(startPrimary ? Constant.START_ICON_KEY : Constant.STOP_ICON_KEY);
                if (icon == null) {
                    primaryButton.setText(startPrimary ? primaryButton.getName() : Str.str(Constant.STOP_KEY));
                } else {
                    primaryButton.setIcon((Icon) icon);
                }
            }
        }
        if (secondaryComponents2 != null && enableSecondary2 != null) {
            enable(enableSecondary2, secondaryComponents2);
        }
        if (enableSecondary != null) {
            enable(enableSecondary, secondaryComponents);
        }
    }

    public static void enable(boolean enable, Component... components) {
        for (Component component : components) {
            component.setEnabled(enable);
        }
    }

    public static DefaultRowSorter<TableModel, Integer> setRowSorter(SyncTable syncTable, final int... reversedCols) {
        DefaultRowSorter<TableModel, Integer> rowSorter = new TableRowSorter<TableModel>(syncTable.tableModel) {
            @Override
            @SuppressWarnings("unchecked")
            public void setSortKeys(List<? extends SortKey> sortKeys) {
                List<SortKey> newSortKeys = (List<SortKey>) sortKeys;
                SortKey primarySortKey;
                if (sortKeys != null && !sortKeys.isEmpty() && (primarySortKey = sortKeys.get(0)) != null) {
                    int sortCol = primarySortKey.getColumn();
                    SortOrder sortOrder = null;

                    for (SortKey sortKey : getSortKeys()) {
                        if (sortKey.getColumn() == sortCol) {
                            sortOrder = sortKey.getSortOrder();
                            break;
                        }
                    }

                    for (int reversedCol : reversedCols) {
                        if (sortCol == reversedCol) {
                            if (sortOrder == null) {
                                (newSortKeys = new ArrayList<SortKey>(sortKeys)).set(0, new SortKey(sortCol, SortOrder.DESCENDING));
                            }
                            sortOrder = primarySortKey.getSortOrder();
                            break;
                        }
                    }

                    if (sortOrder == SortOrder.DESCENDING) {
                        newSortKeys = null;
                    }
                }
                super.setSortKeys(newSortKeys);
            }
        };
        syncTable.table.setRowSorter(rowSorter);
        return rowSorter;
    }

    public static void show(Frame frame) {
        TrayIcon trayIcon = trayIcon(frame);
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
        setVisible(frame);
        deiconify(frame);
    }

    public static void setVisible(final Window window) {
        if (window.isAlwaysOnTop()) {
            for (final Window currWindow : Window.getWindows()) {
                if (currWindow.isVisible() && currWindow instanceof Dialog && ((Dialog) currWindow).getModalityType() != ModalityType.MODELESS) {
                    window.setAlwaysOnTop(false);
                    currWindow.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentHidden(ComponentEvent evt) {
                            currWindow.removeComponentListener(this);
                            window.setAlwaysOnTop(true);
                        }
                    });
                    break;
                }
            }
        }
        window.setVisible(true);
    }

    public static boolean deiconifyThenIsShowing(Frame frame) {
        deiconify(frame);
        return isShowing(frame);
    }

    public static void deiconify(Frame frame) {
        frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
    }

    public static boolean isShowing(Frame frame) {
        return frame.isShowing() && (frame.getExtendedState() & Frame.ICONIFIED) != Frame.ICONIFIED;
    }

    public static void addHyperlinkListener(final JEditorPane editorPane, final HyperlinkListener hyperlinkListener) {
        if (!Constant.MAC) {
            editorPane.addHyperlinkListener(hyperlinkListener);
            return;
        }

        editorPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                Object source;
                if (evt.getButton() != MouseEvent.BUTTON1 || (source = evt.getSource()) != editorPane) {
                    return;
                }

                int position = editorPane.getCaretPosition();
                ElementIterator elementIt = new ElementIterator(editorPane.getDocument());
                Element element;

                while ((element = elementIt.next()) != null) {
                    Object anchor, url;
                    if (element.isLeaf() && position >= element.getStartOffset() && position <= element.getEndOffset() && (anchor
                            = element.getAttributes().getAttribute(Tag.A)) instanceof AttributeSet && (url = ((AttributeSet) anchor).getAttribute(
                                    Attribute.HREF)) instanceof String) {
                        try {
                            hyperlinkListener.hyperlinkUpdate(new HyperlinkEvent(source, EventType.ACTIVATED, new URL((String) url)));
                        } catch (Exception e) {
                            if (Debug.DEBUG) {
                                Debug.print(e);
                            }
                        }
                        return;
                    }
                }
            }
        });
    }

    public static int getUnfilteredRowCount(SyncTable syncTable) {
        synchronized (syncTable.lock) {
            return getUnfilteredRowCount(syncTable.table);
        }
    }

    public static int getUnfilteredRowCount(JTable table) {
        return ((DefaultRowSorter<?, ?>) table.getRowSorter()).getRowFilter() == null ? table.getModel().getRowCount() : table.getRowCount();
    }

    private static Component getIconifyComponent(JFrame frame) {
        for (Component contentPaneComponent : frame.getRootPane().getLayeredPane().getComponentsInLayer(JLayeredPane.FRAME_CONTENT_LAYER)) {
            if (contentPaneComponent instanceof Container && "RootPane.titlePane".equals(contentPaneComponent.getName())) {
                for (Component titlePaneComponent : ((Container) contentPaneComponent).getComponents()) {
                    if ("RootPane.titlePane.iconifyButton".equals(titlePaneComponent.getName())) {
                        return titlePaneComponent;
                    }
                }
                return null;
            }
        }
        return null;
    }

    public static TrayIcon addMinimizeToTraySupport(final JFrame frame) {
        Component iconify;
        if (!SystemTray.isSupported() || (iconify = getIconifyComponent(frame)) == null) {
            return null;
        }

        final TrayIcon trayIcon = new TrayIcon(frame.getIconImage());
        final ActionListener openActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                show(frame);
            }
        };
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON1) {
                    openActionListener.actionPerformed(null);
                }
            }
        });

        MenuItem menuItem = new MenuItem();
        menuItem.addActionListener(openActionListener);
        PopupMenu trayPopupMenu = new PopupMenu();
        trayPopupMenu.add(menuItem);
        trayPopupMenu.addSeparator();
        (menuItem = new MenuItem()).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                try {
                    SystemTray.getSystemTray().remove(trayIcon);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                }
            }
        });
        trayPopupMenu.add(menuItem);
        trayIcon.setPopupMenu(trayPopupMenu);
        updateTrayIconLabels(trayIcon, frame);

        iconify.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON3) {
                    try {
                        SystemTray.getSystemTray().add(trayIcon);
                        frame.setVisible(false);
                    } catch (Exception e) {
                        if (Debug.DEBUG) {
                            Debug.print(e);
                        }
                    }
                }
            }
        });

        return trayIcon;
    }

    private static String trayIconTitle(Frame frame) {
        String title = frame.getTitle();
        return Constant.APP_TITLE.equals(title) ? title : Constant.APP_TITLE + ' ' + title.toLowerCase(Str.locale());
    }

    public static void updateTrayIconLabels(TrayIcon trayIcon, Frame frame) {
        String title = trayIconTitle(frame);
        trayIcon.setToolTip(title);
        PopupMenu popupMenu = trayIcon.getPopupMenu();
        popupMenu.getItem(0).setLabel(Str.str("openWindow", title));
        popupMenu.getItem(2).setLabel(Str.str("GUI.exitMenuItem.text"));
    }

    public static TrayIcon trayIcon(Frame frame) {
        try {
            if (!SystemTray.isSupported()) {
                return null;
            }

            String title = trayIconTitle(frame);
            for (TrayIcon trayIcon : SystemTray.getSystemTray().getTrayIcons()) {
                if (title.equals(trayIcon.getToolTip())) {
                    return trayIcon;
                }
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return null;
    }

    public static String displayableStr(Component component, String str, String defaultStr) {
        Font font = component.getFont();
        return font != null && font.canDisplayUpTo(str) == -1 ? str : defaultStr;
    }

    public static void select(JList list, Object[] vals) {
        int[] selection = new int[vals.length];
        ListModel listModel = list.getModel();

        for (int i = 0, j = 0, size = listModel.getSize(); i < size; i++) {
            Object element = listModel.getElementAt(i);
            for (Object val : vals) {
                if (val.equals(element)) {
                    selection[j++] = i;
                    break;
                }
            }
        }

        list.setSelectedIndices(selection);
        list.ensureIndexIsVisible(list.getSelectedIndex());
        list.repaint();
    }

    public static void add(ButtonGroup buttonGroup, AbstractButton... buttons) {
        for (AbstractButton button : buttons) {
            buttonGroup.add(button);
        }
    }

    public static String[] items(int min, int max, int increment, boolean pad, String head, String tail) {
        List<String> items = new ArrayList<String>(102);
        if (head != null) {
            items.add(head);
        }
        for (int i = min; i <= max; i += increment) {
            items.add(pad && i < 10 ? "0" + i : String.valueOf(i));
        }
        if (tail != null) {
            items.add(tail);
        }
        return items.toArray(Constant.EMPTY_STRS);
    }

    public static void init(JComboBox comboBox, String... model) {
        comboBox.setRenderer(new Renderer(comboBox.getRenderer(), model));
        comboBox.setModel(new DefaultComboBoxModel(model));
    }

    public static void init(JList list, String... model) {
        list.setCellRenderer(new Renderer(list.getCellRenderer(), model));
        list.setListData(model);
    }

    public static void update(JComboBox comboBox, boolean sort, String... view) {
        Object item = comboBox.getSelectedItem();
        comboBox.setModel(new DefaultComboBoxModel(((Renderer) comboBox.getRenderer()).setView(sort, view)));
        comboBox.setSelectedItem(item);
    }

    public static void update(JList list, boolean sort, String... view) {
        Object[] vals = list.getSelectedValues();
        list.setListData(((Renderer) list.getCellRenderer()).setView(sort, view));
        select(list, vals);
    }

    public static String about() {
        return "<html><head></head><body><table cellpadding=\"5\"><tr><td>" + Constant.HTML_FONT + Constant.APP_TITLE + "<br><br>" + Str.str("version") + ' '
                + Str.getNumFormat(Constant.VERSION_FORMAT).format(Constant.APP_VERSION) + "<br><br>" + Str.str("createdBy")
                + "</font></td></tr></table></body></html>";
    }

    public static void run(boolean wait, Runnable runnable) {
        try {
            if (EventQueue.isDispatchThread()) {
                runnable.run();
            } else if (wait) {
                EventQueue.invokeAndWait(runnable);
            } else {
                EventQueue.invokeLater(runnable);
            }
        } catch (InvocationTargetException e) {
            throw ThrowableUtil.unwrap(e);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    public static <T> T run(Callable<T> callable) {
        try {
            if (EventQueue.isDispatchThread()) {
                return callable.call();
            } else {
                FutureTask<T> task = new FutureTask<T>(callable);
                EventQueue.invokeAndWait(task);
                return task.get();
            }
        } catch (ExecutionException e) {
            throw ThrowableUtil.unwrap(e);
        } catch (InvocationTargetException e) {
            throw ThrowableUtil.unwrap(e);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            return null;
        }
    }

    public static void resize(AbstractComponent component, String... referenceTexts) {
        component.component.setMinimumSize(null);
        component.component.setPreferredSize(null);
        Dimension minSize = new Dimension();
        String text = component.getText();
        for (String referenceText : referenceTexts) {
            component.setText(referenceText);
            Dimension preferredSize = component.component.getPreferredSize();
            if (preferredSize != null && preferredSize.width > minSize.width) {
                minSize = new Dimension(preferredSize);
            }
        }
        component.setText(text);
        if (minSize.width > 0) {
            component.component.setMinimumSize(minSize);
            component.component.setPreferredSize(minSize);
        }
    }

    public static int selectedIndex(ButtonGroup buttonGroup) {
        Enumeration<AbstractButton> buttons = buttonGroup.getElements();
        ButtonModel selectedButtonModel = buttonGroup.getSelection();
        int selectedIndex = -1;

        while (buttons.hasMoreElements()) {
            selectedIndex++;
            if (buttons.nextElement().getModel() == selectedButtonModel) {
                break;
            }
        }

        return selectedIndex;
    }

    public static void select(ButtonGroup buttonGroup, int index) {
        Enumeration<AbstractButton> buttons = buttonGroup.getElements();
        int currIndex = -1;

        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            if (++currIndex == index) {
                buttonGroup.setSelected(button.getModel(), true);
                return;
            }
        }
    }

    public static Set<KeyStroke> accelerators(JMenuBar menuBar, JMenuItem... excludedMenuItems) {
        Set<KeyStroke> accelerators = new HashSet<KeyStroke>(40);
        Set<JMenuItem> excludedItems = new HashSet<JMenuItem>(Arrays.asList(excludedMenuItems));
        for (int i = 0, numMenus = menuBar.getMenuCount(); i < numMenus; i++) {
            accelerators(menuBar.getMenu(i), accelerators, excludedItems);
        }
        return accelerators;
    }

    private static void accelerators(JMenuItem menuItem, Set<KeyStroke> accelerators, Set<JMenuItem> excludedMenuItems) {
        if (menuItem == null || excludedMenuItems.contains(menuItem)) {
            return;
        }

        KeyStroke menuAccelerator = menuItem.getAccelerator();
        if (menuAccelerator != null) {
            accelerators.add(menuAccelerator);
        }

        if (menuItem instanceof JMenu) {
            JMenu menu = (JMenu) menuItem;
            for (int j = 0, numMenuItems = menu.getItemCount(); j < numMenuItems; j++) {
                accelerators(menu.getItem(j), accelerators, excludedMenuItems);
            }
        }
    }

    private UI() {
    }
}
