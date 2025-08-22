package com.example.tracktwist.data;

public interface TrackRepository {
    String[] getGenres();
    Track randomByGenre(String genre);
    Track findByArtist(String artistQuery);

    enum SourceType { LOCAL_RAW, REMOTE_URL }
    SourceType sourceType(Track track);
    String locator(Track track);

    final class Track {
        public final String title;
        public final String artist;
        public final String rawOrUrl; // raw base name for local, URL for remote
        public final String artUrl;   // nullable album art URL for remote

        public Track(String title, String artist, String rawOrUrl, String artUrl) {
            this.title = title;
            this.artist = artist;
            this.rawOrUrl = rawOrUrl;
            this.artUrl = artUrl;
        }
    }
}
