package com.kenjigames.ividsmusic.network;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.kenjigames.ividsmusic.data.Track;
import com.kenjigames.ividsmusic.repository.MusicRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Thread-safe utility managing offline downloads of audio tracks.
 * Resolves Invidious streams, streams bytes via OkHttp, caches them locally,
 * and persists the track configuration inside the Room DB.
 */
public class OfflineDownloader {

    private static final String TAG = "OfflineDownloader";
    
    private final Context context;
    private final MusicRepository repository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Constructs the OfflineDownloader.
     *
     * @param context The application context.
     */
    public OfflineDownloader(Context context) {
        this.context = context.getApplicationContext();
        this.repository = new MusicRepository(this.context);
    }

    /**
     * Interface callback to publish download progress.
     */
    public interface DownloadListener {
        void onDownloadStarted(Track track);
        void onDownloadCompleted(Track track);
        void onDownloadFailed(Track track, String errorMsg);
    }

    /**
     * Enqueues a track for download on the background thread pool.
     *
     * @param track The track details to download.
     * @param listener Callback listener to notify of progress updates.
     */
    public void downloadTrack(final Track track, final DownloadListener listener) {
        if (listener != null) {
            listener.onDownloadStarted(track);
        }

        executorService.execute(() -> {
            try {
                String query = track.getArtist() + " - " + track.getTitle();
                
                // 1. Resolve video ID
                String videoId = track.getVideoId();
                if (videoId == null || videoId.isEmpty()) {
                    Log.d(TAG, "Searching video ID for: " + query);
                    videoId = InvidiousResolver.resolveVideoId(query);
                    if (videoId == null) {
                        throw new Exception("Could not find video ID matching query.");
                    }
                    track.setVideoId(videoId);
                }

                // 2. Resolve audio stream URL
                Log.d(TAG, "Resolving stream url for videoId: " + videoId);
                String streamUrl = InvidiousResolver.resolveAudioStreamUrl(videoId);
                if (streamUrl == null) {
                    throw new Exception("Could not resolve audio streams from Invidious.");
                }

                // 3. Prepare target output file in Context.DIRECTORY_MUSIC
                File musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                if (musicDir != null && !musicDir.exists()) {
                    musicDir.mkdirs();
                }

                // Clean filename characters
                String cleanArtist = track.getArtist().trim().replaceAll("[/\\\\?%*:|\"<>]", "");
                String cleanTitle = track.getTitle().trim().replaceAll("[/\\\\?%*:|\"<>]", "");
                String filename = cleanArtist + " - " + cleanTitle + ".m4a";
                File outputFile = new File(musicDir, filename);

                // 4. Download file bytes using OkHttp
                Log.d(TAG, "Downloading stream to: " + outputFile.getAbsolutePath());
                Request request = new Request.Builder()
                        .url(streamUrl)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new Exception("HTTP response error: " + response.code());
                    }

                    try (InputStream input = response.body().byteStream();
                         FileOutputStream output = new FileOutputStream(outputFile)) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        output.flush();
                    }
                }

                // 5. Update track entity and save to database
                if (outputFile.exists() && outputFile.length() > 0) {
                    track.setDownloaded(true);
                    track.setLocalFilePath(outputFile.getAbsolutePath());
                    repository.saveTrack(track);

                    Log.d(TAG, "Successfully downloaded track: " + query);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (listener != null) {
                            listener.onDownloadCompleted(track);
                        }
                    });
                } else {
                    throw new Exception("File download is empty.");
                }

            } catch (final Exception e) {
                Log.e(TAG, "Download failed for track: " + track.getTitle(), e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) {
                        listener.onDownloadFailed(track, e.getMessage());
                    }
                });
            }
        });
    }
}
