package gui;

import debug.Debug;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.DefaultRowSorter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JList;
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

                if (oneSelectedRowMode || syncTable.getSelectedRow() == -1) {
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

    public static String[] copy(Object[] array) {
        return Arrays.copyOf(array, array.length, String[].class);
    }

    public static String[] copy(JList list, String anyStr) {
        if (list.isSelectionEmpty()) {
            list.setSelectedValue(anyStr, true);
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

    public static String groupButtonSelectionChanged(AbstractButton button, AbstractButton... otherButtons) {
        if (button.isSelected()) {
            for (AbstractButton otherButton : otherButtons) {
                otherButton.setSelected(false);
            }
            return button.getText();
        }
        return Constant.ANY;
    }

    public static void updateAnyList(JList list, String anyStr) {
        int[] selection = list.getSelectedIndices();
        if (selection.length < 2) {
            return;
        }

        ListModel listModel = list.getModel();
        for (int i = 0; i < selection.length; i++) {
            String currStr = (String) listModel.getElementAt(selection[i]);
            if (currStr.equals(anyStr)) {
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

    public static boolean isMaxSize(Dimension size) {
        Rectangle screenBounds = getUsableScreenBounds();
        return size.width >= screenBounds.width && size.height >= screenBounds.height;
    }

    public static void centerOnScreen(Window window) {
        window.setLocation(screenCenter(window.getSize()));
    }

    public static Point screenCenter(Dimension windowSize) {
        Rectangle screenBounds = getUsableScreenBounds();
        if (windowSize.height > screenBounds.height) {
            windowSize.height = screenBounds.height;
        }
        if (windowSize.width > screenBounds.width) {
            windowSize.width = screenBounds.width;
        }
        return new Point((screenBounds.width - windowSize.width) / 2, (screenBounds.height - windowSize.height) / 2);
    }

    public static Rectangle getUsableScreenBounds() {
        GraphicsConfiguration graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = graphicsConfig.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfig);
        bounds.y += insets.top;
        bounds.x += insets.left;
        bounds.height -= (insets.top + insets.bottom);
        bounds.width -= (insets.left + insets.right);
        return bounds;
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
        frame.setVisible(true);
        deiconify(frame);
    }

    public static boolean deiconifyThenIsShowing(Frame frame) {
        deiconify(frame);
        return frame.isShowing();
    }

    public static void deiconify(Frame frame) {
        frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
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

    private UI() {
    }
}
