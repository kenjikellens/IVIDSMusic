package com.kenjigames.ividsmusic.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.kenjigames.ividsmusic.R;
import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.network.OfflineDownloader;
import com.kenjigames.ividsmusic.repository.MusicRepository;
import com.kenjigames.ividsmusic.viewmodel.PlaybackViewModel;

import java.util.List;

/**
 * Fragment rendering the Search screen.
 * Handles online search queries via Deezer API and triggers background track downloads.
 */
public class SearchFragment extends Fragment {

    private EditText etSearch;
    private ImageButton btnClear;
    private RecyclerView rvSearchResults;
    private ProgressBar pbLoading;
    private LinearLayout layoutPlaceholder;

    private TrackAdapter adapter;
    private MusicRepository repository;
    private PlaybackViewModel playbackViewModel;
    private OfflineDownloader downloader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind layouts
        etSearch = view.findViewById(R.id.et_search);
        btnClear = view.findViewById(R.id.btn_clear);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        pbLoading = view.findViewById(R.id.pb_search_loading);
        layoutPlaceholder = view.findViewById(R.id.search_placeholder);

        // Hook up adapters
        adapter = new TrackAdapter(new TrackAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(Track track, int position) {
                playbackViewModel.playTrack(track);
            }

            @Override
            public void onActionClick(Track track, int position) {
                // Download button clicked
                startTrackDownload(track, position);
            }
        });
        rvSearchResults.setAdapter(adapter);

        // Initialize controllers
        repository = new MusicRepository(requireContext());
        playbackViewModel = new ViewModelProvider(requireActivity()).get(PlaybackViewModel.class);
        downloader = new OfflineDownloader(requireContext());

        // Keyboard search button listener
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                performSearch(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        // Search text watcher to toggle X clear button
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button listener
        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            adapter.setTracks(null);
            layoutPlaceholder.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        });
    }

    /**
     * Executes Deezer search API based on the input text.
     */
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }

        // Configure UI loading status
        pbLoading.setVisibility(View.VISIBLE);
        layoutPlaceholder.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);

        repository.searchTracks(query, new MusicRepository.RepositoryCallback<List<Track>>() {
            @Override
            public void onSuccess(final List<Track> result) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    if (result == null || result.isEmpty()) {
                        layoutPlaceholder.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "No results found", Toast.LENGTH_SHORT).show();
                    } else {
                        rvSearchResults.setVisibility(View.VISIBLE);
                        // Check if any search results are already downloaded locally
                        checkDownloadStatus(result);
                    }
                });
            }

            @Override
            public void onError(final Throwable throwable) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    layoutPlaceholder.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Cross-checks search results against local Room DB to tag downloaded tracks.
     */
    private void checkDownloadStatus(List<Track> searchResults) {
        repository.getSavedTracks().observe(getViewLifecycleOwner(), savedTracks -> {
            if (savedTracks != null) {
                for (Track s : searchResults) {
                    for (Track dbTrack : savedTracks) {
                        if (dbTrack.getTitle().equalsIgnoreCase(s.getTitle()) &&
                            dbTrack.getArtist().equalsIgnoreCase(s.getArtist())) {
                            s.setDownloaded(true);
                            s.setLocalFilePath(dbTrack.getLocalFilePath());
                            s.setVideoId(dbTrack.getVideoId());
                        }
                    }
                }
            }
            adapter.setTracks(searchResults);
        });
    }

    /**
     * Triggers the OfflineDownloader background download.
     */
    private void startTrackDownload(final Track track, final int position) {
        if (track.isDownloaded()) {
            Toast.makeText(getContext(), "Track is already downloaded", Toast.LENGTH_SHORT).show();
            return;
        }

        downloader.downloadTrack(track, new OfflineDownloader.DownloadListener() {
            @Override
            public void onDownloadStarted(Track t) {
                Toast.makeText(getContext(), "Download started: " + t.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDownloadCompleted(Track t) {
                Toast.makeText(getContext(), "Download complete: " + t.getTitle(), Toast.LENGTH_SHORT).show();
                // Refresh list row download indicator
                track.setDownloaded(true);
                adapter.notifyItemChanged(position);
            }

            @Override
            public void onDownloadFailed(Track t, String errorMsg) {
                Toast.makeText(getContext(), "Download failed: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
