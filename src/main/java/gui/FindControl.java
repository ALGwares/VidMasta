package gui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import util.Regex;

public class FindControl {

  private JTextComponent findTextComponent;
  private Color hideColor, showBgColor, showFgColor;
  private Border showBorder;
  private final List<String> findables = new CopyOnWriteArrayList<String>();
  private int findRow = -2;
  private String query;

  public FindControl(JTextComponent findTextComponent) {
    this.findTextComponent = findTextComponent;
    hideColor = findTextComponent.getParent().getBackground();
    JTextComponent tempTextComponent = new JTextField();
    showBgColor = tempTextComponent.getBackground();
    showFgColor = tempTextComponent.getForeground();
    showBorder = tempTextComponent.getBorder();
    hide(true);
    UI.addAutoCompleteSupport(findTextComponent, findables);
  }

  public void addDataSource(DefaultTableModel tableModel, int col) {
    Map<String, List<String>> cache = new HashMap<>(100);
    tableModel.addTableModelListener(evt -> {
      findables.clear();
      tableModel.getDataVector().forEach(row -> findables.addAll(cache.computeIfAbsent((String) ((List<?>) row).get(col), key -> Arrays.asList(key,
              Regex.cleanAbbreviations(key)).stream().map(str -> {
        String findable = Regex.htmlToPlainText(str);
        return Arrays.asList(findable, Regex.replaceFirst(findable, 327), findable = Regex.clean(findable), Regex.replaceFirst(findable, 327));
      }).flatMap(Collection::stream).collect(Collectors.toList()))));
    });
  }

  public final void hide(boolean clearFindRow) {
    if (findTextComponent.isEnabled()) {
      findTextComponent.setEnabled(false);
      query = findTextComponent.getText();
      findTextComponent.setText(null);
      findTextComponent.setBackground(hideColor);
      findTextComponent.setForeground(hideColor);
      findTextComponent.setBorder(null);
      if (clearFindRow) {
        findRow = -2;
      }
    }
  }

  public void show() {
    if (findTextComponent.isEnabled()) {
      findTextComponent.requestFocusInWindow();
      findTextComponent.selectAll();
    } else {
      findTextComponent.setBorder(showBorder);
      findTextComponent.setBackground(showBgColor);
      findTextComponent.setForeground(showFgColor);
      findTextComponent.requestFocusInWindow();
      findTextComponent.setEnabled(true);
      findTextComponent.setText(query);
    }
  }

  public void find(KeyEvent evt, Runnable enterKeyAction, SyncTable table) {
    int selectedRow = table.getSelectedRow(), key = KeyEvent.VK_UNDEFINED;
    if (evt != null) {
      if ((key = evt.getKeyCode()) == KeyEvent.VK_ENTER && selectedRow != -1) {
        enterKeyAction.run();
        return;
      } else if (key == KeyEvent.VK_ESCAPE) {
        hide(false);
        return;
      }
    }

    String text = findTextComponent.getText().toLowerCase(Locale.ENGLISH);
    if (text.isEmpty()) {
      return;
    }

    boolean continueFind = (findRow != -2 && selectedRow != -1 && (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_UP));
    int numFindables = findables.size();
    List<Integer> foundRows = new ArrayList<Integer>((numFindables / 32) + 1);
    for (int i = 0, n = 8; i < numFindables; i += n) {
      for (int j = i, k = i + n; j < k; j++) {
        if (!findables.get(j).toLowerCase(Locale.ENGLISH).contains(text)) {
          continue;
        }
        int foundRow = table.convertRowIndexToView(i / n);
        if (continueFind) {
          if (foundRow != findRow) {
            foundRows.add(foundRow);
          }
        } else {
          setFindRow(table, foundRow);
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
        if (foundRow > findRow) {
          setFindRow(table, foundRow);
          return;
        }
      }
      setFindRow(table, foundRows.get(0));
    } else {
      for (int i = numFoundRows - 1; i > -1; i--) {
        int foundRow = foundRows.get(i);
        if (foundRow < findRow) {
          setFindRow(table, foundRow);
          return;
        }
      }
      setFindRow(table, foundRows.get(numFoundRows - 1));
    }
  }

  private void setFindRow(SyncTable table, int row) {
    table.changeSelection(row, 0, false, false);
    findRow = row;
  }
}
