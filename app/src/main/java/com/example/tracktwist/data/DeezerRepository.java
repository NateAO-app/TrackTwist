package com.example.tracktwist.data;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

public class DeezerRepository implements TrackRepository {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Random rng = new Random();

    @Override
    public String[] getGenres() { return new String[0]; }

    @Override
    public Track randomByGenre(String genre) { return null; }

    // Artist-only flow:
    // 1) search/artist?q=<name>  -> pick best artist match
    // 2) artist/{id}/top?limit=50 -> pick a track that has preview + album art
    @Override
    public Track findByArtist(String artistQuery) {
        try {
            if (artistQuery == null) return null;
            String q = artistQuery.trim();
            if (q.isEmpty()) return null;

            // 1) Find artist by name
            String qEncoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
            String artistSearchUrl = "https://api.deezer.com/search/artist?q=" + qEncoded + "&limit=10";

            ArtistItem best = null;
            Request artistReq = new Request.Builder().url(artistSearchUrl).get().build();
            try (Response resp = client.newCall(artistReq).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                String body = resp.body().string();
                ArtistSearchResult artists = gson.fromJson(body, ArtistSearchResult.class);
                if (artists == null || artists.data == null || artists.data.length == 0) return null;

                // Prefer exact (case-insensitive) name match; else take first
                String qLower = q.toLowerCase();
                for (ArtistItem a : artists.data) {
                    if (a != null && a.name != null && a.name.equalsIgnoreCase(q)) {
                        best = a;
                        break;
                    }
                }
                if (best == null) best = artists.data[0];
            }

            if (best == null) return null;

            // 2) Fetch top tracks for the matched artist
            String topUrl = "https://api.deezer.com/artist/" + best.id + "/top?limit=50";
            Request topReq = new Request.Builder().url(topUrl).get().build();
            try (Response resp = client.newCall(topReq).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                String body = resp.body().string();
                TopResult top = gson.fromJson(body, TopResult.class);
                if (top == null || top.data == null || top.data.length == 0) return null;

                // Keep only tracks that have a preview URL
                ArrayList<TrackItem> withPreview = new ArrayList<>();
                for (TrackItem t : top.data) {
                    if (t != null && t.preview != null && !t.preview.isEmpty()) {
                        withPreview.add(t);
                    }
                }
                if (withPreview.isEmpty()) return null;

                TrackItem pick = withPreview.get(rng.nextInt(withPreview.size()));
                String title = pick.title != null ? pick.title : "Unknown";
                String artistName = best.name != null ? best.name
                        : (pick.artist != null && pick.artist.name != null ? pick.artist.name : q);
                String previewUrl = pick.preview;

                String art = null;
                if (pick.album != null) {
                    if (pick.album.cover_big != null && !pick.album.cover_big.isEmpty()) art = pick.album.cover_big;
                    else if (pick.album.cover_medium != null && !pick.album.cover_medium.isEmpty()) art = pick.album.cover_medium;
                    else if (pick.album.cover != null && !pick.album.cover.isEmpty()) art = pick.album.cover;
                }

                return new Track(title, artistName, previewUrl, art);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public SourceType sourceType(Track track) { return SourceType.REMOTE_URL; }

    @Override
    public String locator(Track track) { return track.rawOrUrl; }

    // JSON models
    static class ArtistSearchResult { ArtistItem[] data; }
    static class ArtistItem { long id; String name; }
    static class TopResult { TrackItem[] data; }
    static class TrackItem {
        String title;
        String preview;
        Artist artist;
        Album album;
    }
    static class Artist { String name; }
    static class Album {
        String cover; String cover_medium; String cover_big; String cover_xl;
    }
}
