package listener;

public class Video {

  public final String id;
  public volatile String title, year, oldTitle = "", imagePath = "", rating = "", summary = "", imageLink = "", season = "", episode = "";
  public final boolean isTVShow, isTVShowAndMovie;

  public Video(String id, String title, String year, boolean isTVShow, boolean isTVShowAndMovie) {
    this.id = id;
    this.title = title;
    this.year = year;
    this.isTVShow = isTVShow;
    this.isTVShowAndMovie = isTVShowAndMovie;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof Video && id.equals(((Video) obj).id));
  }

  @Override
  public int hashCode() {
    return 7 * 31 + (id == null ? 0 : id.hashCode());
  }

  @Override
  public String toString() {
    return "{'" + title + "'/'" + oldTitle + "' '" + year + "' " + isTVShow + " " + isTVShowAndMovie + " '" + id + "' '" + season + "x" + episode
            + "' '" + rating + "'\n'" + imageLink + "'\n'" + imagePath + "'\n'" + summary + "'}\n";
  }
}
