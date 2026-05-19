package com.kenjigames.ividsmusic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles automatic OTA update checking for the Mobile flavor.
 */
public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/kenjikellens/IVIDSMusic/releases/latest";
    private static final String REPO_APK_URL = "https://github.com/kenjikellens/IVIDSMusic/raw/main/IVIDSMusic_Mobile.apk";

    private final Activity mActivity;
    private final WebView mWebView;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private String mDownloadUrl;

    public UpdateManager(Activity activity, WebView webView) {
        this.mActivity = activity;
        this.mWebView = webView;
    }

    @JavascriptInterface
    public void checkForUpdates() {
        if (!isNetworkAvailable()) {
            notifyWebUpdateError();
            return;
        }

        mExecutor.execute(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                notifyWebUpdateStatus("connecting-api");
                conn = (HttpURLConnection) URI.create(GITHUB_API_URL).toURL().openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "IVIDS-Music-Android-App");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);

                    JSONObject latestRelease = new JSONObject(response.toString());
                    String latestVersion = latestRelease.getString("tag_name");
                    String currentVersion = getCurrentVersion();

                    if (isNewerVersion(currentVersion, latestVersion)) {
                        org.json.JSONArray assets = latestRelease.getJSONArray("assets");
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);
                            String assetName = asset.getString("name").toLowerCase();
                            if (assetName.endsWith(".apk")) {
                                mDownloadUrl = asset.getString("browser_download_url");
                                if (assetName.contains("mobile")) break;
                            }
                        }

                        if (mDownloadUrl != null) notifyWebFoundUpdate(latestVersion);
                        else notifyWebNoUpdateFound();
                    } else {
                        notifyWebNoUpdateFound();
                    }
                } else {
                    notifyWebNoUpdateFound();
                }
            } catch (Exception e) {
                notifyWebUpdateError();
            } finally {
                try { if (reader != null) reader.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private boolean isNewerVersion(String current, String latest) {
        try {
            notifyWebUpdateStatus("comparing-versions");
            String c = current.trim().replaceAll("^v", "");
            String l = latest.trim().replaceAll("^v", "");
            if (c.equals(l)) return false;

            String[] cParts = c.split("\\.");
            String[] lParts = l.split("\\.");
            int length = Math.max(cParts.length, lParts.length);

            for (int i = 0; i < length; i++) {
                int cP = i < cParts.length ? Integer.parseInt(cParts[i].replaceAll("[^0-9]", "")) : 0;
                int lP = i < lParts.length ? Integer.parseInt(lParts[i].replaceAll("[^0-9]", "")) : 0;
                if (lP > cP) return true;
                if (cP > lP) return false;
            }
        } catch (Exception e) {
            return !current.equals(latest);
        }
        return false;
    }

    @JavascriptInterface
    public String getCurrentVersion() {
        try {
            return mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0.2.0";
        }
    }

    @JavascriptInterface
    public void downloadAndInstallForUrl(String url) {
        this.mDownloadUrl = url;
        downloadAndInstall();
    }

    @JavascriptInterface
    public void downloadAndInstall() {
        if (mDownloadUrl == null) return;
        mExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                notifyWebUpdateStatus("downloading");
                String finalUrl = mDownloadUrl;
                for (int i = 0; i < 5; i++) {
                    conn = (HttpURLConnection) URI.create(finalUrl).toURL().openConnection();
                    conn.setRequestProperty("User-Agent", "IVIDS-Music-Android-App");
                    conn.setInstanceFollowRedirects(false);
                    if (conn.getResponseCode() >= 300 && conn.getResponseCode() <= 308) {
                        finalUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                    } else break;
                }

                File downloadDir = new File(mActivity.getExternalCacheDir(), "updates");
                if (!downloadDir.exists()) downloadDir.mkdirs();
                File apkFile = new File(downloadDir, "IVIDSMusic-update.apk");

                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream out = new FileOutputStream(apkFile)) {
                    byte[] data = new byte[8192];
                    int count;
                    long total = 0;
                    int length = conn.getContentLength();
                    while ((count = in.read(data)) != -1) {
                        total += count;
                        if (length > 0) {
                            int progress = (int) (total * 100 / length);
                            mActivity.runOnUiThread(() -> mWebView.evaluateJavascript("if(typeof onUpdateProgress === 'function') onUpdateProgress(" + progress + ");", null));
                        }
                        out.write(data, 0, count);
                    }
                }
                installApk(apkFile);
            } catch (Exception e) {
                notifyWebUpdateError();
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void installApk(File apkFile) {
        try {
            notifyWebUpdateStatus("installing");
            android.net.Uri apkUri = FileProvider.getUriForFile(mActivity, mActivity.getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            notifyWebUpdateError();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public void shutdown() { mExecutor.shutdown(); }
    private void notifyWebFoundUpdate(String v) { mActivity.runOnUiThread(() -> mWebView.evaluateJavascript("onUpdateFound('" + v + "')", null)); }
    private void notifyWebNoUpdateFound() { mActivity.runOnUiThread(() -> mWebView.evaluateJavascript("onNoUpdateFound()", null)); }
    private void notifyWebUpdateStatus(String s) { mActivity.runOnUiThread(() -> mWebView.evaluateJavascript("onUpdateStatus('" + s + "')", null)); }
    private void notifyWebUpdateError() { mActivity.runOnUiThread(() -> mWebView.evaluateJavascript("onUpdateCheckError()", null)); }
}
