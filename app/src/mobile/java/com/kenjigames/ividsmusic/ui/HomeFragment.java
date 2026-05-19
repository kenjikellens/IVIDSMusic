package com.kenjigames.ividsmusic.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kenjigames.ividsmusic.R;
import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.repository.MusicRepository;
import com.kenjigames.ividsmusic.viewmodel.PlaybackViewModel;

import java.util.List;

/**
 * Fragment rendering the Home landing screen.
 * Displays recently played tracks (offline tracks) and trending recommendations.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private RecyclerView rvRecentlyPlayed;
    private RecyclerView rvRecommended;

    private TrackCardAdapter recentlyPlayedAdapter;
    private TrackAdapter recommendedAdapter;

    private MusicRepository repository;
    private PlaybackViewModel playbackViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind layouts
        rvRecentlyPlayed = view.findViewById(R.id.rv_recently_played);
        rvRecommended = view.findViewById(R.id.rv_recommended);

        // Configure layout managers
        rvRecentlyPlayed.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommended.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapters
        recentlyPlayedAdapter = new TrackCardAdapter((track, position) -> {
            playTrack(track);
        });
        rvRecentlyPlayed.setAdapter(recentlyPlayedAdapter);

        recommendedAdapter = new TrackAdapter(new TrackAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(Track track, int position) {
                playTrack(track);
            }

            @Override
            public void onActionClick(Track track, int position) {
                // Actions (like download triggers) from Home recommendations
                Toast.makeText(getContext(), "Downloading: " + track.getTitle(), Toast.LENGTH_SHORT).show();
                // We'll hook up actual downloading in Step 3.6
            }
        });
        rvRecommended.setAdapter(recommendedAdapter);

        // Hook up repository and ViewModel
        repository = new MusicRepository(requireContext());
        playbackViewModel = new ViewModelProvider(requireActivity()).get(PlaybackViewModel.class);

        // Fetch offline tracks to serve as recently played fallback
        repository.getSavedTracks().observe(getViewLifecycleOwner(), tracks -> {
            if (tracks != null) {
                recentlyPlayedAdapter.setTracks(tracks);
            }
        });

        // Load recommended charts
        loadRecommendations();
    }

    /**
     * Pulls popular chart tracks from Deezer API to show as home recommendations.
     */
    private void loadRecommendations() {
        repository.searchTracks("hits", new MusicRepository.RepositoryCallback<List<Track>>() {
            @Override
            public void onSuccess(List<Track> result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> recommendedAdapter.setTracks(result));
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "Failed to load home recommendations", throwable);
            }
        });
    }

    /**
     * Dispatches active track details to the global media controller ViewModel.
     */
    private void playTrack(Track track) {
        playbackViewModel.playTrack(track);
    }
}
