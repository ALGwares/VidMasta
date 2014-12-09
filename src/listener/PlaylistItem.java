package listener;

public interface PlaylistItem {

    boolean canPlay();

    boolean isActive();

    void play(boolean force);

    void stop();

    boolean canOpen();

    void open();
}
