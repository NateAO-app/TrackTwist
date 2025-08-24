package com.example.tracktwist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // UI
    private Spinner genreSpinner;
    private Button randomizeButton;
    private EditText artistSearchInput;
    private Button searchArtistButton;
    private ProgressBar loadingIndicator;
    private ImageView albumArt;
    private TextView nowPlayingText;
    private TextView artistText;
    private Button playButton;
    private Button saveFavoriteButton;
    private Button shareButton;
    private Button viewFavoritesButton;
    private Button thumbsUpButton;
    private Button thumbsDownButton;

    // Playback
    private MediaPlayer mediaPlayer;
    private boolean isPreparing = false;
    private boolean hasSourceLoaded = false;

    private enum PlaybackMode { REMOTE_ARTIST, REMOTE_GENRE }
    private PlaybackMode playbackMode = PlaybackMode.REMOTE_ARTIST;

    private boolean autoQueueNext = false;         // set by 👍; consumed on completion
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Remote queue (both artist and genre fill into this)
    private static class RemoteTrack {
        String title;
        String artist;
        String previewUrl;
        String artUrl;
        RemoteTrack(String t, String a, String p, String art) {
            title = t; artist = a; previewUrl = p; artUrl = art;
        }
    }
    private final List<RemoteTrack> remoteQueue = new ArrayList<>();
    private int remoteIndex = -1;

    // Deezer genre model (id <-> name)
    private static class DZGenre {
        int id; String name;
        DZGenre(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
    private final List<DZGenre> deezerGenres = new ArrayList<>();

    // Storage keys (only remote tracks now)
    private static final String PREFS = "tracktwist_prefs";
    private static final String KEY_TRACKS = "favorite_tracks";

    // Networking
    private static final String USER_AGENT = "TrackTwist/0.4";
    private static final int TIMEOUT_MS = 12000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setClickListeners();
        prepareMediaPlayer();

        // Populate Deezer genres into the spinner
        fetchDeezerGenres();
    }

    private void bindViews() {
        genreSpinner = findViewById(R.id.genreSpinner);
        randomizeButton = findViewById(R.id.randomizeButton);
        artistSearchInput = findViewById(R.id.artistSearchInput);
        searchArtistButton = findViewById(R.id.searchArtistButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        albumArt = findViewById(R.id.albumArt);
        nowPlayingText = findViewById(R.id.nowPlayingText);
        artistText = findViewById(R.id.artistText);
        playButton = findViewById(R.id.playButton);
        saveFavoriteButton = findViewById(R.id.saveFavoriteButton);
        shareButton = findViewById(R.id.shareButton);
        viewFavoritesButton = findViewById(R.id.viewFavoritesButton);
        thumbsUpButton = findViewById(R.id.thumbsUpButton);
        thumbsDownButton = findViewById(R.id.thumbsDownButton);
    }

    private void setClickListeners() {
        // GENRE: fetch tracks seeded by the selected Deezer genre
        randomizeButton.setOnClickListener(v -> {
            DZGenre g = selectedGenre();
            if (g == null) {
                toast("Pick a genre first.");
                return;
            }
            playbackMode = PlaybackMode.REMOTE_GENRE;
            startRemoteByGenre(g.id);
        });

        // ARTIST: fetch tracks by artist search
        searchArtistButton.setOnClickListener(v -> {
            String query = artistSearchInput.getText().toString().trim();
            if (TextUtils.isEmpty(query)) {
                toast("Enter an artist name.");
                return;
            }
            playbackMode = PlaybackMode.REMOTE_ARTIST;
            startRemoteByArtist(query);
        });

        playButton.setOnClickListener(v -> {
            if (mediaPlayer == null || isPreparing) return;
            if (!hasSourceLoaded) {
                toast("Start a track with Search Artist or Randomize.");
                return;
            }
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playButton.setText("Play");
                } else {
                    mediaPlayer.start();
                    playButton.setText("Pause");
                }
            } catch (IllegalStateException ignored) {}
        });

        saveFavoriteButton.setOnClickListener(v -> saveCurrentAsFavorite());
        shareButton.setOnClickListener(v -> shareCurrent());
        viewFavoritesButton.setOnClickListener(v -> toast("Favorites screen will come next."));

        thumbsUpButton.setOnClickListener(v -> {
            autoQueueNext = true;
            toast("Queued next after this sample.");
        });

        thumbsDownButton.setOnClickListener(v -> {
            autoQueueNext = false;
            stopIfPlaying();
            skipToNextImmediate();
        });
    }

    private void prepareMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        mediaPlayer.setOnCompletionListener(mp -> {
            playButton.setText("Play");
            autoQueueNext = false;
            skipToNextImmediate();
        });

        hasSourceLoaded = false;
    }

    // =========================
    // Deezer Genres (Spinner)
    // =========================
    private void fetchDeezerGenres() {
        showLoading(true);
        disableControls(true);
        new Thread(() -> {
            List<DZGenre> loaded = new ArrayList<>();
            try {
                JSONObject json = getJson("https://api.deezer.com/genre");
                JSONArray data = json.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject g = data.optJSONObject(i);
                        if (g == null) continue;
                        int id = g.optInt("id", -1);
                        String name = g.optString("name");
                        if (id <= 0 || TextUtils.isEmpty(name)) continue;
                        // Skip non-music “All” or “Podcasts” buckets if present
                        if ("All".equalsIgnoreCase(name) || "Podcasts".equalsIgnoreCase(name)) continue;
                        loaded.add(new DZGenre(id, name));
                    }
                }
            } catch (IOException ignored) { }

            mainHandler.post(() -> {
                deezerGenres.clear();
                deezerGenres.addAll(loaded);
                List<String> names = new ArrayList<>();
                for (DZGenre g : deezerGenres) names.add(g.name);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, names);
                genreSpinner.setAdapter(adapter);
                showLoading(false);
                disableControls(false);
                if (names.isEmpty()) toast("Could not load genres from Deezer.");
            });
        }).start();
    }

    private DZGenre selectedGenre() {
        int pos = genreSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= deezerGenres.size()) return null;
        return deezerGenres.get(pos);
    }

    // =================================
    // Remote playback by ARTIST or GENRE
    // =================================

    private void startRemoteByArtist(String query) {
        showLoading(true);
        disableControls(true);
        new Thread(() -> {
            try {
                long artistId = resolveArtistId(query);
                List<RemoteTrack> tracks = fetchTopTracksForArtist(artistId);
                fillQueueAndStart(tracks);
            } catch (IOException e) {
                mainHandler.post(() -> {
                    toast("Network issue. Try again or search a different artist.");
                    showLoading(false);
                    disableControls(false);
                });
            }
        }).start();
    }

    private void startRemoteByGenre(int genreId) {
        showLoading(true);
        disableControls(true);

        new Thread(() -> {
            try {
                // 1) Get a pool of artists for the genre
                List<Long> artistIds = fetchTopArtistsForGenre(genreId, 25); // up to 25 artists

                // 2) Build a mixed track list by sampling several artists
                List<RemoteTrack> mixed = new ArrayList<>();
                Random rnd = new Random();
                int attempts = 0;
                while (mixed.size() < 30 && !artistIds.isEmpty() && attempts < artistIds.size() * 2) {
                    long pick = artistIds.get(rnd.nextInt(artistIds.size()));
                    List<RemoteTrack> tops = fetchTopTracksForArtist(pick);
                    for (RemoteTrack t : tops) {
                        if (!TextUtils.isEmpty(t.previewUrl)) mixed.add(t);
                    }
                    attempts++;
                }

                fillQueueAndStart(mixed);
            } catch (IOException e) {
                mainHandler.post(() -> {
                    toast("Network issue. Try again or pick another genre.");
                    showLoading(false);
                    disableControls(false);
                });
            }
        }).start();
    }

    private void fillQueueAndStart(List<RemoteTrack> tracks) {
        mainHandler.post(() -> {
            remoteQueue.clear();
            for (RemoteTrack t : tracks) {
                if (!TextUtils.isEmpty(t.previewUrl)) remoteQueue.add(t);
            }
            remoteIndex = -1;
            if (remoteQueue.isEmpty()) {
                toast("No previews found. Try another choice.");
                showLoading(false);
                disableControls(false);
            } else {
                playNextRemoteImmediate();
                showLoading(false);
                disableControls(false);
            }
        });
    }

    private void playNextRemoteImmediate() {
        RemoteTrack t = advanceRemote();
        if (t == null) {
            toast("End of queue. Search or Randomize again.");
            return;
        }
        setNowPlaying(t.title, t.artist);
        loadAlbumArtAsync(t.artUrl);
        startRemoteStream(t.previewUrl);
    }

    private RemoteTrack advanceRemote() {
        if (remoteQueue.isEmpty()) return null;
        remoteIndex++;
        if (remoteIndex >= remoteQueue.size()) return null;
        return remoteQueue.get(remoteIndex);
    }

    private void startRemoteStream(String url) {
        stopReleasePlayer();
        try {
            isPreparing = true;
            hasSourceLoaded = false;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mediaPlayer.setOnPreparedListener(mp -> {
                isPreparing = false;
                hasSourceLoaded = true;
                mp.start();
                playButton.setText("Pause");
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                playButton.setText("Play");
                autoQueueNext = false;
                skipToNextImmediate();
            });
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            isPreparing = false;
            toast("Could not play preview. Skipping.");
            playNextRemoteImmediate();
        }
    }


    // ===================
    // Deezer HTTP helpers
    // ===================

    private long resolveArtistId(String artistQuery) throws IOException {
        String q = artistQuery.replace(" ", "%20");
        String api = "https://api.deezer.com/search/artist?q=" + q;
        JSONObject json = getJson(api);
        JSONArray data = json.optJSONArray("data");
        if (data == null || data.length() == 0) throw new IOException("No artist");
        JSONObject first = data.optJSONObject(0);
        if (first == null) throw new IOException("No artist");
        long id = first.optLong("id", -1L);
        if (id <= 0L) throw new IOException("No artist");
        return id;
    }

    private List<Long> fetchTopArtistsForGenre(int genreId, int limit) throws IOException {
        // Deezer provides artists by genre.
        String api = "https://api.deezer.com/genre/" + genreId + "/artists";
        JSONObject json = getJson(api);
        JSONArray data = json.optJSONArray("data");
        List<Long> ids = new ArrayList<>();
        if (data != null) {
            for (int i = 0; i < data.length() && ids.size() < limit; i++) {
                JSONObject a = data.optJSONObject(i);
                if (a == null) continue;
                long id = a.optLong("id", -1L);
                if (id > 0) ids.add(id);
            }
        }
        return ids;
    }

    private List<RemoteTrack> fetchTopTracksForArtist(long artistId) throws IOException {
        String api = "https://api.deezer.com/artist/" + artistId + "/top?limit=50";
        JSONObject json = getJson(api);
        JSONArray data = json.optJSONArray("data");
        List<RemoteTrack> out = new ArrayList<>();
        if (data == null) return out;
        for (int i = 0; i < data.length(); i++) {
            JSONObject t = data.optJSONObject(i);
            if (t == null) continue;
            String title = t.optString("title");
            String preview = t.optString("preview");
            JSONObject artist = t.optJSONObject("artist");
            String artistName = artist == null ? "" : artist.optString("name");
            JSONObject album = t.optJSONObject("album");
            String art = album == null ? "" : album.optString("cover_medium");
            out.add(new RemoteTrack(title, artistName, preview, art));
        }
        return out;
    }

    private JSONObject getJson(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(conn.getInputStream())));
            StringBuilder sb = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) sb.append(line);
            return new JSONObject(sb.toString());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Parse error", e);
        } finally {
            if (reader != null) try { reader.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    // ======================
    // Reactions and queueing
    // ======================

    private void skipToNextImmediate() {
        if (!remoteQueue.isEmpty()) {
            playNextRemoteImmediate();
        } else {
            toast("Start a track first.");
        }
    }

    private void stopIfPlaying() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.stop();
        } catch (Exception ignored) {}
    }

    private void stopReleasePlayer() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        } catch (Exception ignored) {}
        mediaPlayer = null;
        isPreparing = false;
        hasSourceLoaded = false;
        playButton.setText("Play");
    }

    // ======================
    // UI helpers
    // ======================

    private void setNowPlaying(String title, String artist) {
        nowPlayingText.setText("Now Playing: " + (TextUtils.isEmpty(title) ? "—" : title));
        artistText.setText(TextUtils.isEmpty(artist) ? "Artist" : artist);
    }

    private void loadAlbumArtAsync(String artUrl) {
        if (TextUtils.isEmpty(artUrl)) {
            albumArt.setImageResource(R.mipmap.ic_launcher);
            return;
        }
        showLoading(true);
        disableControls(true);
        new Thread(new ImageFetchTask(artUrl, albumArt, () -> {
            showLoading(false);
            disableControls(false);
        })).start();
    }

    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void disableControls(boolean disable) {
        randomizeButton.setEnabled(!disable);
        searchArtistButton.setEnabled(!disable);
        playButton.setEnabled(!disable);
        saveFavoriteButton.setEnabled(!disable);
        shareButton.setEnabled(!disable);
        viewFavoritesButton.setEnabled(!disable);
        thumbsUpButton.setEnabled(!disable);
        thumbsDownButton.setEnabled(!disable);
        artistSearchInput.setEnabled(!disable);
        genreSpinner.setEnabled(!disable);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ======================
    // Save and Share (remote only)
    // ======================

    private void saveCurrentAsFavorite() {
        if (remoteIndex >= 0 && remoteIndex < remoteQueue.size()) {
            RemoteTrack t = remoteQueue.get(remoteIndex);
            try {
                JSONArray arr = new JSONArray(getPrefs().getString(KEY_TRACKS, "[]"));
                JSONObject o = new JSONObject();
                o.put("title", t.title);
                o.put("artist", t.artist);
                o.put("previewUrl", t.previewUrl);
                o.put("artUrl", t.artUrl);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.optJSONObject(i);
                    if (e == null) continue;
                    if (t.previewUrl.equals(e.optString("previewUrl"))) {
                        toast("Already saved.");
                        return;
                    }
                }
                arr.put(o);
                getPrefs().edit().putString(KEY_TRACKS, arr.toString()).apply();
                toast("Saved to favorites.");
            } catch (Exception e) {
                toast("Could not save favorite.");
            }
        } else {
            toast("Start a remote track to save it.");
        }
    }

    private void shareCurrent() {
        if (remoteIndex >= 0 && remoteIndex < remoteQueue.size()) {
            RemoteTrack t = remoteQueue.get(remoteIndex);
            String text = "Check out " + t.title + " by " + t.artist + ": " + t.previewUrl;
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(send, "Share"));
        } else {
            toast("Start a remote track before sharing.");
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ======================
    // Image fetch task
    // ======================

    private static class ImageFetchTask implements Runnable {
        private final String url;
        private final WeakReference<ImageView> targetRef;
        private final Runnable onDone;

        ImageFetchTask(String url, ImageView target, Runnable onDone) {
            this.url = url;
            this.targetRef = new WeakReference<>(target);
            this.onDone = onDone;
        }

        @Override
        public void run() {
            Bitmap bmp = null;
            HttpURLConnection conn = null;
            try {
                URL u = new URL(url);
                conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = conn.getInputStream()) {
                        bmp = BitmapFactory.decodeStream(is);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
            ImageView target = targetRef.get();
            if (target != null) {
                final Bitmap result = bmp;
                target.post(() -> {
                    if (result != null) target.setImageBitmap(result);
                    if (onDone != null) onDone.run();
                });
            } else if (onDone != null) {
                onDone.run();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopReleasePlayer();
    }
}
