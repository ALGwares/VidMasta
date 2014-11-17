package listener;

public interface PlaylistItem {

    boolean canPlay();

    boolean isActive();

    void play();

    void stop();

    boolean canOpen();

    void open();
}
