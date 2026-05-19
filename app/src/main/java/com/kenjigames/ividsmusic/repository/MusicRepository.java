package com.kenjigames.ividsmusic.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.kenjigames.ividsmusic.data.AppDatabase;
import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.data.TrackDao;
import com.kenjigames.ividsmusic.network.DeezerApi;
import com.kenjigames.ividsmusic.network.DeezerSearchResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Data Repository coordinating offline Room database queries
 * and online Retrofit Deezer searches.
 */
public class MusicRepository {

    private final TrackDao trackDao;
    private final DeezerApi deezerApi;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Interface callback for dispatching asynchronous network results.
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    /**
     * Constructs the MusicRepository.
     *
     * @param context The application context to initialize databases.
     */
    public MusicRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.trackDao = db.trackDao();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.deezerApi = retrofit.create(DeezerApi.class);
    }

    /**
     * Retrieves all saved offline tracks from Room.
     */
    public LiveData<List<Track>> getSavedTracks() {
        return trackDao.getAllTracks();
    }

    /**
     * Inserts a track metadata entity into Room on a background thread.
     */
    public void saveTrack(final Track track) {
        executorService.execute(() -> trackDao.insertTrack(track));
    }

    /**
     * Deletes a track from the local database and purges its cached/saved audio file.
     */
    public void deleteTrack(final Track track) {
        executorService.execute(() -> {
            trackDao.deleteTrack(track);
            if (track.getLocalFilePath() != null) {
                File file = new File(track.getLocalFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
        });
    }

    /**
     * Searches for music online using the Deezer API, mapping response models to Track entities.
     *
     * @param query The search query.
     * @param callback The repository callback to dispatch the results.
     */
    public void searchTracks(String query, final RepositoryCallback<List<Track>> callback) {
        deezerApi.searchTracks(query).enqueue(new Callback<DeezerSearchResponse>() {
            @Override
            public void onResponse(Call<DeezerSearchResponse> call, Response<DeezerSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Track> tracks = new ArrayList<>();
                    List<DeezerSearchResponse.TrackData> dataList = response.body().getData();
                    if (dataList != null) {
                        for (DeezerSearchResponse.TrackData data : dataList) {
                            Track track = new Track();
                            // Generate a unique ID for search tracks (using Deezer ID)
                            track.setId(String.valueOf(data.getId()));
                            track.setTitle(data.getTitle());
                            if (data.getArtist() != null) {
                                track.setArtist(data.getArtist().getName());
                            } else {
                                track.setArtist("Unknown Artist");
                            }
                            if (data.getAlbum() != null) {
                                track.setCover(data.getAlbum().getCoverMedium());
                            }
                            track.setDownloaded(false);
                            tracks.add(track);
                        }
                    }
                    callback.onSuccess(tracks);
                } else {
                    callback.onError(new Exception("API call failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<DeezerSearchResponse> call, Throwable t) {
                callback.onError(t);
            }
        });
    }
}
