package com.example.tracktwist.data;

import android.content.Context;

import com.example.tracktwist.R;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocalRepository implements TrackRepository {
    private final Random rng = new Random();
    private final Library library;

    public LocalRepository(Context ctx) {
        this.library = load(ctx);
    }

    @Override
    public String[] getGenres() {
        if (library == null || library.genres == null) return new String[0];
        String[] names = new String[library.genres.size()];
        for (int i = 0; i < library.genres.size(); i++) {
            names[i] = library.genres.get(i).name;
        }
        return names;
    }

    @Override
    public Track randomByGenre(String genre) {
        if (library == null || library.genres == null) return null;
        for (Genre g : library.genres) {
            if (g != null && g.name != null && g.name.equals(genre) && g.tracks != null && !g.tracks.isEmpty()) {
                ModelsTrack t = g.tracks.get(rng.nextInt(g.tracks.size()));
                return new Track(t.title, t.artist, t.raw, null);
            }
        }
        for (Genre g : library.genres) {
            if (g != null && g.tracks != null && !g.tracks.isEmpty()) {
                ModelsTrack t = g.tracks.get(0);
                return new Track(t.title, t.artist, t.raw, null);
            }
        }
        return null;
    }

    @Override
    public Track findByArtist(String artistQuery) {
        if (artistQuery == null) return null;
        String q = artistQuery.trim().toLowerCase();
        if (q.isEmpty()) return null;

        List<ModelsTrack> matches = new ArrayList<>();
        if (library != null && library.genres != null) {
            for (Genre g : library.genres) {
                if (g != null && g.tracks != null) {
                    for (ModelsTrack t : g.tracks) {
                        if (t != null && t.artist != null && t.artist.toLowerCase().contains(q)) {
                            matches.add(t);
                        }
                    }
                }
            }
        }
        if (matches.isEmpty()) return null;
        ModelsTrack pick = matches.get(rng.nextInt(matches.size()));
        return new Track(pick.title, pick.artist, pick.raw, null);
    }

    @Override
    public SourceType sourceType(Track track) {
        return SourceType.LOCAL_RAW;
    }

    @Override
    public String locator(Track track) {
        return track.rawOrUrl;
    }

    private Library load(Context ctx) {
        try {
            InputStream is = ctx.getResources().openRawResource(R.raw.seed_tracks);
            try (InputStreamReader reader = new InputStreamReader(is)) {
                Library lib = new Gson().fromJson(reader, Library.class);
                if (lib == null) lib = emptyLibrary();
                if (lib.genres == null) lib.genres = new ArrayList<>();
                return lib;
            }
        } catch (Exception e) {
            return emptyLibrary();
        }
    }

    private Library emptyLibrary() {
        Library lib = new Library();
        lib.genres = new ArrayList<>();
        return lib;
    }

    // JSON models
    private static class Library {
        List<Genre> genres;
    }
    private static class Genre {
        String name;
        List<ModelsTrack> tracks;
    }
    private static class ModelsTrack {
        String title;
        String artist;
        String raw;
    }
}
