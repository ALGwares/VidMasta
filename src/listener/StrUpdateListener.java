package listener;

public interface StrUpdateListener {

  String get(int index);

  void update(boolean showConfirmation, GuiListener guiListener);

  void update();

  void addListener(UpdateListener listener);

  void removeListener(UpdateListener listener);

  boolean containsListener(UpdateListener listener);

  void waitForUpdate();

  interface UpdateListener {

    void update(String[] strs);
  }
}
