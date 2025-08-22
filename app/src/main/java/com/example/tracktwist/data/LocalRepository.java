package com.example.tracktwist.data;

import java.util.Random;

public class LocalRepository implements TrackRepository {
    private final Random rng = new Random();

    // Single genre and single track for scaffolding
    private final String[] genres = new String[] { "LoFi" };
    private final TrackRepository.Track[] lofi = new TrackRepository.Track[] {
            new TrackRepository.Track("Pompeii", "Studio Demo", "pompeii")
    };

    @Override
    public String[] getGenres() {
        return genres;
    }

    @Override
    public TrackRepository.Track randomByGenre(String genre) {
        return lofi[rng.nextInt(lofi.length)];
    }

    @Override
    public SourceType sourceType(TrackRepository.Track track) {
        return SourceType.LOCAL_RAW;
    }

    @Override
    public String locator(TrackRepository.Track track) {
        return track.rawOrUrl;
    }
}
