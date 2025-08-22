package com.example.tracktwist;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Spinner spGenres;
    private TextView tvNowPlaying;
    private MediaPlayer mediaPlayer;
    private Button btnPlayPause;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spGenres = findViewById(R.id.spGenres);
        tvNowPlaying = findViewById(R.id.tvNowPlaying);
        btnPlayPause = findViewById(R.id.btnPlayPause);

        // Genres for spinner
        List<String> genres = Arrays.asList("LoFi", "Rock", "Pop");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genres
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGenres.setAdapter(adapter);

        // Randomize button
        Button btnRandomize = findViewById(R.id.btnRandomize);
        btnRandomize.setOnClickListener(v -> {
            int randomIndex = new Random().nextInt(genres.size());
            String randomGenre = genres.get(randomIndex);
            spGenres.setSelection(randomIndex);
            tvNowPlaying.setText("Now Playing: " + randomGenre);
            Toast.makeText(this, "Random genre: " + randomGenre, Toast.LENGTH_SHORT).show();
        });

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
    }

    private void togglePlayPause() {
        if (isPlaying) {
            // Pause
            mediaPlayer.pause();
            btnPlayPause.setText("Play");
            isPlaying = false;
        } else {
            if (mediaPlayer == null) {
                // Load first sample file from res/raw
                mediaPlayer = MediaPlayer.create(this, R.raw.pompeii);
            }
            mediaPlayer.start();
            btnPlayPause.setText("Pause");
            isPlaying = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
