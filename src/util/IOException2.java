package util;

import java.io.IOException;

public class IOException2 extends IOException {

    private static final long serialVersionUID = 1L;

    public final String extraMsg;

    public IOException2(IOException e, String extraMsg) {
        super(e.getMessage(), e);
        this.extraMsg = extraMsg;
    }
}
