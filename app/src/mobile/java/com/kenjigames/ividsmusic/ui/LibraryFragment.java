package com.kenjigames.ividsmusic.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.kenjigames.ividsmusic.R;
import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.repository.MusicRepository;
import com.kenjigames.ividsmusic.viewmodel.PlaybackViewModel;

/**
 * Fragment rendering the Library screen.
 * Displays local saved tracks and manages deletions.
 */
public class LibraryFragment extends Fragment {

    private TextView tvTrackCount;
    private RecyclerView rvLibraryTracks;
    private LinearLayout layoutPlaceholder;

    private TrackAdapter adapter;
    private MusicRepository repository;
    private PlaybackViewModel playbackViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind layouts
        tvTrackCount = view.findViewById(R.id.tv_track_count);
        rvLibraryTracks = view.findViewById(R.id.rv_library_tracks);
        layoutPlaceholder = view.findViewById(R.id.library_placeholder);

        // Set up adapters
        adapter = new TrackAdapter(new TrackAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(Track track, int position) {
                // Play track and pass entire library as active queue
                playbackViewModel.setQueue(adapter.getTracks(), position);
            }

            @Override
            public void onActionClick(Track track, int position) {
                // Delete track confirmation dialog
                confirmTrackDeletion(track);
            }
        });
        rvLibraryTracks.setAdapter(adapter);

        // Initialize controllers
        repository = new MusicRepository(requireContext());
        playbackViewModel = new ViewModelProvider(requireActivity()).get(PlaybackViewModel.class);

        // Observe local saved tracks list from DB
        repository.getSavedTracks().observe(getViewLifecycleOwner(), tracks -> {
            if (tracks == null || tracks.isEmpty()) {
                layoutPlaceholder.setVisibility(View.VISIBLE);
                rvLibraryTracks.setVisibility(View.GONE);
                tvTrackCount.setText("0 tracks saved");
                adapter.setTracks(null);
            } else {
                layoutPlaceholder.setVisibility(View.GONE);
                rvLibraryTracks.setVisibility(View.VISIBLE);
                tvTrackCount.setText(tracks.size() + " tracks saved");
                adapter.setTracks(tracks);
            }
        });
    }

    /**
     * Shows a confirmation dialog before deleting a track.
     */
    private void confirmTrackDeletion(final Track track) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Track")
                .setMessage("Are you sure you want to delete '" + track.getTitle() + "' from your local device?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteTrack(track);
                    Toast.makeText(getContext(), "Track deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
