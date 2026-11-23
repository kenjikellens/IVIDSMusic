package com.kenjigames.ividsmusic.data.stream

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.local.DataStorePreferencesRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [DesktopNativeToolsManager] portable zip extraction and Zip Slip rejection without network calls.
 */
class DesktopNativeToolsManagerTest {

    private lateinit var tempDir: File

    /** Creates an isolated temp workspace before each test. */
    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("ivids_tools_test").toFile()
    }

    /** Removes temp directories after each test. */
    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    /** Ensures only nested `ffmpeg.exe` is extracted safely while Zip Slip traversal entries are ignored. */
    @Test
    fun extractFfmpegFromZipExtractsSingleBinaryAndRejectsZipSlip() {
        val zipFile = File(tempDir, "bundle.zip")
        val maliciousEscape = File(tempDir, "evil.exe")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("../../evil.exe"))
            zos.write("bad".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("ffmpeg-build/bin/ffmpeg.exe"))
            zos.write("GOOD_FFMPEG_BINARY".toByteArray())
            zos.closeEntry()
        }

        val dirs = AppStorageDirectories(
            cacheDir = File(tempDir, "cache").absolutePath,
            filesDir = File(tempDir, "data").absolutePath,
            savedDir = File(tempDir, "saved").absolutePath,
            bundledToolsDir = null
        )
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { File(tempDir, "prefs.preferences_pb").absolutePath.toPath() }
        )
        val manager = DesktopNativeToolsManager(
            directories = dirs,
            preferencesRepository = DataStorePreferencesRepository(dataStore),
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        val dest = File(manager.resolveManagedToolsDir(), "ffmpeg.exe")
        assertTrue(manager.extractFfmpegFromZip(zipFile, dest))
        assertTrue(dest.isFile)
        assertEquals("GOOD_FFMPEG_BINARY", dest.readText())
        assertFalse(maliciousEscape.exists())
    }
}
