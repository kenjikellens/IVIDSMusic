package com.kenjigames.ividsmusic.ui;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kenjigames.ividsmusic.R;

import java.io.File;

/**
 * Fragment rendering the Settings screen.
 * Tracks storage usage and handles temporary cache clearance.
 */
public class SettingsFragment extends Fragment {

    private TextView tvCacheInfo;
    private TextView tvAppVersion;
    private Button btnClearCache;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind layouts
        tvCacheInfo = view.findViewById(R.id.tv_cache_info);
        tvAppVersion = view.findViewById(R.id.tv_app_version);
        btnClearCache = view.findViewById(R.id.btn_clear_cache);

        // Display app version
        displayAppVersion();

        // Calculate and display temporary cache size
        updateCacheSizeInfo();

        // Clear cache click handler
        btnClearCache.setOnClickListener(v -> {
            clearAppCache();
            updateCacheSizeInfo();
            Toast.makeText(getContext(), "Temporary cache cleared", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Resolves and sets the active package version name.
     */
    private void displayAppVersion() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            tvAppVersion.setText("Version " + pInfo.versionName + " (Beta)");
        } catch (Exception e) {
            tvAppVersion.setText("Version 0.1.6 (Beta)");
        }
    }

    /**
     * Calculates size of cached stream files in externalCacheDir and cacheDir.
     */
    private void updateCacheSizeInfo() {
        long totalSize = 0;
        File cacheDir = requireContext().getCacheDir();
        File extCacheDir = requireContext().getExternalCacheDir();

        totalSize += getFolderSize(cacheDir);
        if (extCacheDir != null) {
            totalSize += getFolderSize(extCacheDir);
        }

        double sizeMb = (double) totalSize / (1024 * 1024);
        tvCacheInfo.setText(String.format("Cache size: %.2f MB", sizeMb));
    }

    /**
     * Helper to compute recursive folder sizes.
     */
    private long getFolderSize(File folder) {
        if (folder == null || !folder.exists()) return 0;
        long length = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += getFolderSize(file);
                }
            }
        }
        return length;
    }

    /**
     * Deletes temporary downloads and cache directories.
     */
    private void clearAppCache() {
        File cacheDir = requireContext().getCacheDir();
        File extCacheDir = requireContext().getExternalCacheDir();

        deleteDirContent(cacheDir);
        if (extCacheDir != null) {
            deleteDirContent(extCacheDir);
        }
    }

    /**
     * Recursive folder content deletion helper.
     */
    private void deleteDirContent(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirContent(file);
                }
                file.delete();
            }
        }
    }
}
