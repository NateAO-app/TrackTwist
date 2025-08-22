package com.example.tracktwist;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tracktwist.data.LocalRepository;
import com.example.tracktwist.data.TrackRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Spinner genreSpinner;
    private Button randomizeButton;
    private Button playButton;
    private Button saveFavoriteButton;
    private Button viewFavoritesButton;
    private TextView nowPlayingText;
    private TextView artistText;

    private MediaPlayer mediaPlayer;

    private TrackRepository repo;
    private TrackRepository.Track currentTrack;

    // Favorites persistence
    private static final String PREFS = "tracktwist_prefs";
    private static final String KEY_FAVORITES = "favorites";
    private SharedPreferences prefs;
    private final List<String> favorites = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        genreSpinner = findViewById(R.id.genreSpinner);
        randomizeButton = findViewById(R.id.randomizeButton);
        playButton = findViewById(R.id.playButton);
        saveFavoriteButton = findViewById(R.id.saveFavoriteButton);
        viewFavoritesButton = findViewById(R.id.viewFavoritesButton);
        nowPlayingText = findViewById(R.id.nowPlayingText);
        artistText = findViewById(R.id.artistText);

        // JSON-backed repository (reads res/raw/seed_tracks.json)
        repo = new LocalRepository(this);

        // Populate spinner from repository
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                repo.getGenres()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);

        // Load favorites from SharedPreferences
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadFavorites();

        // Randomize selects a track from the chosen genre and updates labels
        randomizeButton.setOnClickListener(v -> {
            String genre = (String) genreSpinner.getSelectedItem();
            currentTrack = repo.randomByGenre(genre);
            if (currentTrack != null) {
                nowPlayingText.setText("Now Playing: " + currentTrack.title);
                artistText.setText(currentTrack.artist);
            }
            stopPlayback();
        });

        // Play toggles playback; if no track yet, auto-select from current genre
        playButton.setOnClickListener(v -> {
            if (currentTrack == null) {
                String genre = (String) genreSpinner.getSelectedItem();
                currentTrack = repo.randomByGenre(genre);
                if (currentTrack != null) {
                    nowPlayingText.setText("Now Playing: " + currentTrack.title);
                    artistText.setText(currentTrack.artist);
                }
            }

            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                resetPlayButton();
            } else {
                startPlaybackFromRepository();
            }
        });

        // Save the currently selected genre as a favorite
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

        // Show a simple dialog listing favorites, with Clear All
        viewFavoritesButton.setOnClickListener(v -> showFavoritesDialog());
    }

    private void startPlaybackFromRepository() {
        if (currentTrack == null) {
            Toast.makeText(this, "No track selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (repo.sourceType(currentTrack) == TrackRepository.SourceType.LOCAL_RAW) {
            String baseName = repo.locator(currentTrack); // e.g., "personalholloway"
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
        } else {
            Toast.makeText(this, "Remote playback not implemented yet.", Toast.LENGTH_SHORT).show();
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
        Set<String> set = prefs.getStringSet(KEY_FAVORITES, null);
        favorites.clear();
        if (set != null) {
            favorites.addAll(set);
        }
    }

    private void saveFavorites() {
        Set<String> set = new HashSet<>(favorites);
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply();
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
