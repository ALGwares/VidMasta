package gui;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

public abstract class AbstractComponent {

    public final JComponent component;

    private AbstractComponent(JComponent component) {
        this.component = component;
    }

    public abstract String getText();

    public abstract void setText(String text);

    public static AbstractComponent newInstance(final JTextComponent textComponent) {
        return new AbstractComponent(textComponent) {
            @Override
            public String getText() {
                return textComponent.getText();
            }

            @Override
            public void setText(String text) {
                textComponent.setText(text);
            }
        };
    }

    public static AbstractComponent newInstance(final AbstractButton button) {
        return new AbstractComponent(button) {
            @Override
            public String getText() {
                return button.getText();
            }

            @Override
            public void setText(String text) {
                button.setText(text);
            }
        };
    }
}
