package com.kenjigames.ividsmusic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.player.PlaybackService;
import com.kenjigames.ividsmusic.ui.HomeFragment;
import com.kenjigames.ividsmusic.ui.LibraryFragment;
import com.kenjigames.ividsmusic.ui.SearchFragment;
import com.kenjigames.ividsmusic.ui.SettingsFragment;
import com.kenjigames.ividsmusic.viewmodel.PlaybackViewModel;
import com.squareup.picasso.Picasso;

/**
 * Main Activity wrapper for the native Android mobile app flavor.
 * Coordinates bottom navigation screen swaps, starts background ExoPlayer services,
 * and handles UI binding of the sticky bottom playback bar.
 */
public class MainActivity extends AppCompatActivity {

    private ConstraintLayout bottomPlayerBar;
    private ProgressBar playerProgress;
    private ImageView playerCover;
    private TextView playerTitle;
    private TextView playerArtist;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;

    private PlaybackViewModel playbackViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout widgets
        bottomPlayerBar = findViewById(R.id.bottom_player_bar);
        playerProgress = findViewById(R.id.player_progress);
        playerCover = findViewById(R.id.player_cover);
        playerTitle = findViewById(R.id.player_title);
        playerArtist = findViewById(R.id.player_artist);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);

        // Start background PlaybackService to initialize ExoPlayer
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);

        // Set up bottom navigation swaps
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.navigation_library) {
                selectedFragment = new LibraryFragment();
            } else if (itemId == R.id.navigation_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default landing screen to Home
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Initialize and bind PlaybackViewModel state listeners
        playbackViewModel = new ViewModelProvider(this).get(PlaybackViewModel.class);
        bindPlayerBar();
    }

    /**
     * Binds bottom player widgets to live database/ExoPlayer status feeds.
     */
    private void bindPlayerBar() {
        // Show or hide bottom player depending on track selection status
        playbackViewModel.getCurrentTrack().observe(this, track -> {
            if (track == null) {
                bottomPlayerBar.setVisibility(View.GONE);
            } else {
                bottomPlayerBar.setVisibility(View.VISIBLE);
                playerTitle.setText(track.getTitle());
                playerArtist.setText(track.getArtist());

                if (track.getCover() != null && !track.getCover().isEmpty()) {
                    Picasso.get()
                            .load(track.getCover())
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(playerCover);
                } else {
                    playerCover.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        });

        // Toggle play/pause buttons
        playbackViewModel.getIsPlaying().observe(this, isPlaying -> {
            if (isPlaying) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        });

        // Bind seek progress changes
        playbackViewModel.getProgress().observe(this, progress -> {
            playerProgress.setProgress(progress.intValue());
        });

        playbackViewModel.getDuration().observe(this, duration -> {
            playerProgress.setMax(duration.intValue());
        });

        // Binds action listeners
        btnPlayPause.setOnClickListener(v -> playbackViewModel.togglePlayPause());
        btnNext.setOnClickListener(v -> playbackViewModel.next());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
