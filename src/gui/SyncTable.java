package gui;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class SyncTable {

    private JTable table;
    private DefaultTableModel tableModel;
    private final Object lock = new Object();

    SyncTable(JTable table) {
        this.table = table;
        tableModel = (DefaultTableModel) table.getModel();
    }

    public void addRow(Object[] rowData) {
        synchronized (lock) {
            tableModel.addRow(rowData);
        }
    }

    public void setModelValueAt(Object aValue, int row, int column) {
        synchronized (lock) {
            tableModel.setValueAt(aValue, row, column);
        }
    }

    public Object getModelValueAt(int rowIndex, int columnIndex) {
        synchronized (lock) {
            return tableModel.getValueAt(rowIndex, columnIndex);
        }
    }

    public void setRowCount(int rowCount) {
        synchronized (lock) {
            tableModel.setRowCount(rowCount);
        }
    }

    public int getRowCount() {
        synchronized (lock) {
            return tableModel.getRowCount();
        }
    }

    public int getSelectedRow() {
        synchronized (lock) {
            return table.getSelectedRow();
        }
    }

    public int[] getSelectedRows() {
        synchronized (lock) {
            return table.getSelectedRows();
        }
    }

    public int getSelectedRowCount() {
        synchronized (lock) {
            return table.getSelectedRowCount();
        }
    }

    public int convertRowIndexToView(int modelRowIndex) {
        synchronized (lock) {
            return table.convertRowIndexToView(modelRowIndex);
        }
    }

    public int convertRowIndexToModel(int viewRowIndex) {
        synchronized (lock) {
            return table.convertRowIndexToModel(viewRowIndex);
        }
    }

    public int rowAtPoint(Point point) {
        synchronized (lock) {
            return table.rowAtPoint(point);
        }
    }

    public int columnAtPoint(Point point) {
        synchronized (lock) {
            return table.columnAtPoint(point);
        }
    }

    public void clearSelection() {
        synchronized (lock) {
            table.clearSelection();
        }
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        synchronized (lock) {
            table.changeSelection(rowIndex, columnIndex, toggle, extend);
        }
    }

    public void selectAll() {
        synchronized (lock) {
            table.selectAll();
        }
    }

    public ListSelectionModel getSelectionModel() {
        synchronized (lock) {
            return table.getSelectionModel();
        }
    }

    public int getSelectedColumnCount() {
        synchronized (lock) {
            return table.getSelectedColumnCount();
        }
    }

    public int[] getSelectedColumns() {
        synchronized (lock) {
            return table.getSelectedColumns();
        }
    }

    public Object getViewValueAt(int rowIndex, int columnIndex) {
        synchronized (lock) {
            return table.getValueAt(rowIndex, columnIndex);
        }
    }

    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        synchronized (lock) {
            return table.getCellRect(row, column, includeSpacing);
        }
    }

    public boolean requestFocusInWindow() {
        synchronized (lock) {
            return table.requestFocusInWindow();
        }
    }

    public void setRowSelectionInterval(int index0, int index1) {
        synchronized (lock) {
            table.setRowSelectionInterval(index0, index1);
        }
    }
}
