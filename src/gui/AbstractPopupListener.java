package gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;

public abstract class AbstractPopupListener extends MouseAdapter {

    @Override
    public void mousePressed(MouseEvent evt) {
        showPopup(evt);
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
        showPopup(evt);
    }

    protected void show(JPopupMenu popupMenu, MouseEvent evt) {
        popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    abstract protected void showPopup(MouseEvent evt);
}
