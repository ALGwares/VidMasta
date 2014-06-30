package listener;

public class Video {

    public final String ID;
    public volatile String title, year, oldTitle = "", imagePath = "", rating = "", summary = "", imageLink = "", season = "", episode = "";
    public final boolean IS_TV_SHOW, IS_TV_SHOW_AND_MOVIE;

    public Video(String id, String title, String year, boolean isTVShow, boolean isTVShowAndMovie) {
        ID = id;
        this.title = title;
        this.year = year;
        IS_TV_SHOW = isTVShow;
        IS_TV_SHOW_AND_MOVIE = isTVShowAndMovie;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Video && ID.equals(((Video) obj).ID));
    }

    @Override
    public int hashCode() {
        return 7 * 31 + (ID == null ? 0 : ID.hashCode());
    }

    @Override
    public String toString() {
        return "{'" + title + "'/'" + oldTitle + "' '" + year + "' " + IS_TV_SHOW + " " + IS_TV_SHOW_AND_MOVIE + " '" + ID + "' '" + season + "x" + episode
                + "' '" + rating + "'\n'" + imageLink + "'\n'" + imagePath + "'\n'" + summary + "'}\n";
    }
}
