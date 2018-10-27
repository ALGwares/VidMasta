package listener;

import java.io.File;

public interface PlaylistItem {

    boolean canPlay();

    boolean isActive();

    boolean isStoppable();

    void play(boolean force);

    void stop();

    boolean canOpen();

    void open();

    String groupID();

    String uri();

    String link();

    File groupFile();

    int groupIndex();

    Long groupDownloadID();

    String name();

    boolean canBan();
}
