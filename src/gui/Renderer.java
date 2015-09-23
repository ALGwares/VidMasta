package gui;

import java.awt.Component;
import java.text.Collator;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.UIResource;
import str.Str;
import util.Constant;

public class Renderer implements ListCellRenderer, UIResource {

    private final ListCellRenderer renderer;
    private final Entry<String, String>[] modelView;

    @SuppressWarnings("unchecked")
    public Renderer(ListCellRenderer renderer, String[] model) {
        this.renderer = renderer;
        modelView = new Entry[model.length];
        for (int i = 0; i < model.length; i++) {
            modelView[i] = new SimpleEntry<String, String>(model[i], null);
        }
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object modelVal, int index, boolean isSelected, boolean cellHasFocus) {
        Object viewVal = modelVal;
        for (Entry<String, String> modelViewVal : modelView) {
            String val;
            if (modelViewVal.getKey().equals(modelVal) && (val = modelViewVal.getValue()) != null) {
                viewVal = val;
                break;
            }
        }
        return renderer.getListCellRendererComponent(list, viewVal, index, isSelected, cellHasFocus);
    }

    public String[] setView(boolean sort, String... view) {
        for (int i = 0; i < modelView.length; i++) {
            modelView[i].setValue(view[i]);
        }
        return sort ? sortedModel() : model();
    }

    private String[] sortedModel() {
        Entry<String, String>[] sortedModelView = Arrays.copyOf(modelView, modelView.length);
        Arrays.sort(sortedModelView, new Comparator<Entry<String, String>>() {
            private Collator collator = Collator.getInstance(Str.locale());

            @Override
            public int compare(Entry<String, String> entry1, Entry<String, String> entry2) {
                return entry1.getKey().equals(Constant.ANY) ? -1 : (entry2.getKey().equals(Constant.ANY) ? 1 : collator.compare(entry1.getValue(),
                        entry2.getValue()));
            }
        });
        return model(sortedModelView);
    }

    public String[] model() {
        return model(modelView);
    }

    private static String[] model(Entry<String, String>[] modelView) {
        String[] model = new String[modelView.length];
        for (int i = 0; i < modelView.length; i++) {
            model[i] = modelView[i].getKey();
        }
        return model;
    }
}
