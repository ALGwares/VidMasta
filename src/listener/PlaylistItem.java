package listener;

import java.io.File;

public interface PlaylistItem {

    boolean canPlay();

    boolean isActive();

    void play(boolean force);

    void stop();

    boolean canOpen();

    void open();

    String groupID();

    String groupName();

    File groupFile();

    int groupIndex();

    String name();
}
