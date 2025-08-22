package com.example.tracktwist.data;

public interface TrackRepository {
    String[] getGenres();
    Track randomByGenre(String genre);

    enum SourceType { LOCAL_RAW, REMOTE_URL }
    SourceType sourceType(Track track);
    String locator(Track track); // raw name without extension or a URL

    final class Track {
        public final String title;
        public final String artist;
        public final String rawOrUrl;

        public Track(String title, String artist, String rawOrUrl) {
            this.title = title;
            this.artist = artist;
            this.rawOrUrl = rawOrUrl;
        }
    }
}
