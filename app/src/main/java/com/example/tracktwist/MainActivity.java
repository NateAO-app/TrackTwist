package com.example.tracktwist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tracktwist.data.DeezerRepository;
import com.example.tracktwist.data.LocalRepository;
import com.example.tracktwist.data.TrackRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Spinner genreSpinner;
    private Button randomizeButton;
    private Button playButton;
    private Button saveFavoriteButton;
    private Button viewFavoritesButton;
    private Button shareButton;
    private EditText artistSearchInput;
    private Button searchArtistButton;
    private ProgressBar loadingIndicator;
    private ImageView albumArt;
    private TextView nowPlayingText;
    private TextView artistText;

    private MediaPlayer mediaPlayer;

    private TrackRepository localRepo;
    private TrackRepository deezerRepo;

    private TrackRepository.Track currentTrack;
    private TrackRepository currentRepo;

    private static final String PREFS = "tracktwist_prefs";
    private static final String KEY_FAVORITES = "favorites";
    private SharedPreferences prefs;
    private final List<String> favorites = new ArrayList<>();

    private final OkHttpClient http = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        genreSpinner = findViewById(R.id.genreSpinner);
        randomizeButton = findViewById(R.id.randomizeButton);
        playButton = findViewById(R.id.playButton);
        saveFavoriteButton = findViewById(R.id.saveFavoriteButton);
        viewFavoritesButton = findViewById(R.id.viewFavoritesButton);
        shareButton = findViewById(R.id.shareButton);
        artistSearchInput = findViewById(R.id.artistSearchInput);
        searchArtistButton = findViewById(R.id.searchArtistButton);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        albumArt = findViewById(R.id.albumArt);
        nowPlayingText = findViewById(R.id.nowPlayingText);
        artistText = findViewById(R.id.artistText);

        localRepo = new LocalRepository(this);
        deezerRepo = new DeezerRepository();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                localRepo.getGenres()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadFavorites();

        randomizeButton.setOnClickListener(v -> {
            String genre = (String) genreSpinner.getSelectedItem();
            currentTrack = localRepo.randomByGenre(genre);
            currentRepo = localRepo;
            if (currentTrack != null) {
                nowPlayingText.setText("Now Playing: " + currentTrack.title);
                artistText.setText(currentTrack.artist);
            }
            clearAlbumArt();
            stopPlayback();
        });

        playButton.setOnClickListener(v -> {
            if (currentTrack == null) {
                String genre = (String) genreSpinner.getSelectedItem();
                currentTrack = localRepo.randomByGenre(genre);
                currentRepo = localRepo;
                if (currentTrack != null) {
                    nowPlayingText.setText("Now Playing: " + currentTrack.title);
                    artistText.setText(currentTrack.artist);
                }
                clearAlbumArt();
            }

            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                resetPlayButton();
            } else {
                startPlaybackFromCurrentRepo();
            }
        });

        saveFavoriteButton.setOnClickListener(v -> {
            String genre = (String) genreSpinner.getSelectedItem();
            if (genre == null) {
                Toast.makeText(this, "No genre selected.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!favorites.contains(genre)) {
                favorites.add(genre);
                saveFavorites();
                Toast.makeText(this, "Saved to favorites: " + genre, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Already in favorites: " + genre, Toast.LENGTH_SHORT).show();
            }
        });

        viewFavoritesButton.setOnClickListener(v -> showFavoritesDialog());

        searchArtistButton.setOnClickListener(v -> {
            String q = artistSearchInput.getText().toString().trim();
            if (q.isEmpty()) {
                Toast.makeText(this, "Enter an artist name.", Toast.LENGTH_SHORT).show();
                return;
            }
            setLoading(true);
            new Thread(() -> {
                TrackRepository.Track found = deezerRepo.findByArtist(q);
                runOnUiThread(() -> {
                    setLoading(false);
                    if (found == null) {
                        String genre = (String) genreSpinner.getSelectedItem();
                        currentTrack = localRepo.randomByGenre(genre);
                        currentRepo = localRepo;
                        if (currentTrack != null) {
                            nowPlayingText.setText("Now Playing: " + currentTrack.title + " (local)");
                            artistText.setText(currentTrack.artist);
                            clearAlbumArt();
                            Toast.makeText(this, "No matches. Playing a local " + genre + " track.", Toast.LENGTH_SHORT).show();
                            stopPlayback();
                        } else {
                            Toast.makeText(this, "No local tracks available.", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    currentTrack = found;
                    currentRepo = deezerRepo;
                    nowPlayingText.setText("Now Playing: " + currentTrack.title);
                    artistText.setText(currentTrack.artist);
                    stopPlayback();
                    if (currentTrack.artUrl != null && !currentTrack.artUrl.isEmpty()) {
                        loadAlbumArt(currentTrack.artUrl);
                    } else {
                        clearAlbumArt();
                    }
                });
            }).start();
        });

        shareButton.setOnClickListener(v -> {
            if (currentTrack == null || currentRepo == null) {
                Toast.makeText(this, "Nothing to share.", Toast.LENGTH_SHORT).show();
                return;
            }
            String shareText;
            if (currentRepo.sourceType(currentTrack) == TrackRepository.SourceType.REMOTE_URL) {
                shareText = currentTrack.title + " — " + currentTrack.artist + "\n" + currentRepo.locator(currentTrack);
            } else {
                shareText = currentTrack.title + " — " + currentTrack.artist + " (local preview)";
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(intent, "Share track"));
        });
    }

    private void setLoading(boolean on) {
        loadingIndicator.setVisibility(on ? View.VISIBLE : View.GONE);
        searchArtistButton.setEnabled(!on);
        randomizeButton.setEnabled(!on);
        playButton.setEnabled(!on);
        saveFavoriteButton.setEnabled(!on);
        viewFavoritesButton.setEnabled(!on);
        shareButton.setEnabled(!on);
    }

    private void loadAlbumArt(String url) {
        albumArt.setImageDrawable(null);
        new Thread(() -> {
            try {
                Request req = new Request.Builder().url(url).build();
                try (Response resp = http.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) return;
                    byte[] bytes = resp.body().bytes();
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    runOnUiThread(() -> albumArt.setImageBitmap(bmp));
                }
            } catch (Exception ignored) { }
        }).start();
    }

    private void clearAlbumArt() {
        albumArt.setImageDrawable(null);
    }

    private void startPlaybackFromCurrentRepo() {
        if (currentTrack == null || currentRepo == null) {
            Toast.makeText(this, "No track selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentRepo.sourceType(currentTrack) == TrackRepository.SourceType.LOCAL_RAW) {
            String baseName = currentRepo.locator(currentTrack);
            int resId = getResources().getIdentifier(baseName, "raw", getPackageName());
            if (resId == 0) {
                Toast.makeText(this, "Audio not found: " + baseName + ".mp3", Toast.LENGTH_LONG).show();
                Log.e("TrackTwist", "Missing raw resource: " + baseName + ".mp3");
                return;
            }
            stopPlayback();
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer == null) {
                Toast.makeText(this, "Unable to create MediaPlayer.", Toast.LENGTH_SHORT).show();
                return;
            }
            mediaPlayer.setOnCompletionListener(mp -> resetPlayButton());
            mediaPlayer.start();
            playButton.setText("Pause");
            clearAlbumArt();

        } else {
            String url = currentRepo.locator(currentTrack);
            try {
                stopPlayback();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(mp -> resetPlayButton());
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    playButton.setText("Pause");
                });
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Toast.makeText(this, "Stream error.", Toast.LENGTH_SHORT).show();
                Log.e("TrackTwist", "Streaming failed", e);
                resetPlayButton();
            }
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        resetPlayButton();
    }

    private void resetPlayButton() {
        playButton.setText("Play");
    }

    private void loadFavorites() {
        Set<String> set = getSharedPreferences(PREFS, MODE_PRIVATE).getStringSet(KEY_FAVORITES, null);
        favorites.clear();
        if (set != null) favorites.addAll(set);
    }

    private void saveFavorites() {
        Set<String> set = new HashSet<>(favorites);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_FAVORITES, set).apply();
    }

    private void showFavoritesDialog() {
        if (favorites.isEmpty()) {
            Toast.makeText(this, "No favorites saved.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = favorites.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Favorites")
                .setItems(items, null)
                .setNegativeButton("Clear All", (d, w) -> {
                    favorites.clear();
                    saveFavorites();
                    Toast.makeText(this, "Favorites cleared.", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayback();
    }
}
