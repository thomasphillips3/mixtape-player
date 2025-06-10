/*
 * Copyright 2024 Professional Audio Delivery Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.media.library

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaMetadata
import com.example.android.uamp.media.extensions.DOWNLOAD_STATUS_DOWNLOADED
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Source of [MediaMetadata] objects created from bundled audio files and catalog.
 * This is designed for mixing engineers to deliver professional apps to artists
 * with high-quality bundled audio tracks and artwork (video/image).
 */
class LocalBundledSource(
    private val context: Context,
    private val catalogAssetPath: String = "music/catalog.json"
) : AbstractMusicSource() {

    override var catalog: List<MediaMetadata> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator(): Iterator<MediaMetadata> = catalog.iterator()

    override suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    /**
     * Load the catalog from bundled assets and create media metadata
     * for high-quality audio files included in the app.
     */
    private suspend fun updateCatalog(): List<MediaMetadata>? {
        return withContext(Dispatchers.IO) {
            try {
                // Load catalog from assets
                val musicCatalog = loadCatalogFromAssets()
                
                // Catalog loaded successfully
                
                // Convert to MediaMetadata with mixed URI/resource system
                musicCatalog.map { track ->
                    // Determine artwork URI based on processed resources
                    val artworkUri = when {
                        hasDrawableResource(track.artworkResource) -> {
                            val uri = Uri.parse("android.resource://${context.packageName}/drawable/${track.artworkResource}")
                            uri
                        }
                        else -> {
                            // Fallback to default art
                            val uri = Uri.parse("android.resource://${context.packageName}/drawable/album_art_fallback")
                            uri
                        }
                    }
                    
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setAlbumArtist(track.albumArtist)
                        .setComposer(track.composer)
                        .setTrackNumber(track.trackNumber)
                        .setDiscNumber(track.discNumber)
                        .setArtworkUri(artworkUri)
                        .setExtras(Bundle().apply {
                            putString("media_id", track.id)
                            putString("media_uri", track.audioUri)
                            putLong("duration", track.durationMs)
                            putLong("download_status", DOWNLOAD_STATUS_DOWNLOADED) // Always downloaded since bundled
                            putLong("flag", 1L) // FLAG_PLAYABLE
                            putString("audio_format", track.format)
                            putString("sample_rate", track.sampleRate)
                            putString("bit_depth", track.bitDepth)
                            putString("mixing_notes", track.mixingNotes)
                            
                            // Add track type information
                            if (track.hasVideo == true) {
                                putString("track_type", "MUSIC_VIDEO")
                                putString("video_path", track.videoPath ?: "")
                                putString("original_audio_uri", track.originalAudioUri ?: "")
                            } else {
                                putString("track_type", "AUDIO_ONLY") 
                            }
                            
                            putString("artwork_uri", artworkUri.toString())
                        })
                        .build()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to load bundled catalog", e)
                null
            }
        }
    }

    /**
     * Scan assets/artwork directory for numbered artwork files
     * Supports images (png, jpg, jpeg) and videos (mp4)
     * Returns map of track number to artwork info
     */
    private fun scanArtworkFiles(): Map<Int, ArtworkInfo> {
        val artworkMap = mutableMapOf<Int, ArtworkInfo>()
        
        try {
            // Get list of files in assets/artwork directory
            val assetManager = context.assets
            val artworkFiles = try {
                assetManager.list("artwork")?.toList() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "artwork directory not found in assets", e)
                emptyList<String>()
            }
            
            // Process each file
            for (filename in artworkFiles) {
                try {
                    // Extract track number and extension
                    val (trackNumber, extension) = parseArtworkFilename(filename)
                    
                    if (trackNumber > 0) {
                        val artworkInfo = createArtworkInfoFromAsset(filename, extension)
                        if (artworkInfo != null) {
                            artworkMap[trackNumber] = artworkInfo
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Error processing artwork file: $filename", e)
                }
            }
            
            // Add fallback album art if available
            val fallbackFiles = listOf("album-art.jpg", "album-art.jpeg", "album-art.png", "default.jpg", "default.png")
            for (fallbackFile in fallbackFiles) {
                if (fallbackFile in artworkFiles && !artworkMap.containsKey(0)) {
                    val artworkInfo = createArtworkInfoFromAsset(fallbackFile, getFileExtension(fallbackFile))
                    if (artworkInfo != null) {
                        artworkMap[0] = artworkInfo
                        break
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error scanning artwork files", e)
        }
        
        return artworkMap
    }

    /**
     * Parse artwork filename to extract track number and extension
     * Supports formats like: 01.mp4, 02.png, 03.jpg, etc.
     */
    private fun parseArtworkFilename(filename: String): Pair<Int, String> {
        val parts = filename.split(".")
        if (parts.size >= 2) {
            val nameWithoutExt = parts[0]
            val extension = parts.last().lowercase()
            
            // Try to parse track number
            val trackNumber = nameWithoutExt.toIntOrNull() ?: 0
            return Pair(trackNumber, extension)
        }
        return Pair(0, "")
    }

    /**
     * Get file extension from filename
     */
    private fun getFileExtension(filename: String): String {
        return filename.substringAfterLast('.', "").lowercase()
    }

    /**
     * Create ArtworkInfo from asset file based on extension
     */
    private fun createArtworkInfoFromAsset(filename: String, extension: String): ArtworkInfo? {
        return try {
            when (extension) {
                "mp4", "mov", "avi" -> {
                    // Video artwork - use raw resource URI
                    val trackNumber = parseArtworkFilename(filename).first
                    val videoResourceName = "video_${trackNumber.toString().padStart(2, '0')}"
                    
                    // Check if video resource exists in raw folder
                    if (hasRawResource(videoResourceName)) {
                        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/$videoResourceName")
                        // Use album art as thumbnail
                        val thumbnailUri = Uri.parse("file:///android_asset/artwork/album-art.jpg")
                        ArtworkInfo(ArtworkType.VIDEO, thumbnailUri, videoUri)
                    } else {
                        // Fallback to image artwork if video resource doesn't exist
                        val imageUri = Uri.parse("file:///android_asset/artwork/album-art.jpg")
                        ArtworkInfo(ArtworkType.IMAGE, imageUri, null)
                    }
                }
                "png", "jpg", "jpeg", "webp" -> {
                    // Image artwork - use asset URI
                    val imageUri = Uri.parse("file:///android_asset/artwork/$filename")
                    ArtworkInfo(ArtworkType.IMAGE, imageUri, null)
                }
                else -> {
                    android.util.Log.w(TAG, "Unsupported artwork format: $extension")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating artwork info for $filename", e)
            null
        }
    }

    /**
     * Check if a drawable resource exists
     */
    private fun hasDrawableResource(resourceName: String): Boolean {
        return try {
            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            resourceId != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a raw resource exists
     */
    private fun hasRawResource(resourceName: String): Boolean {
        return try {
            val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
            resourceId != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load the music catalog from the assets folder.
     */
    private fun loadCatalogFromAssets(): List<BundledTrack> {
        val assetManager = context.assets
        val inputStream = assetManager.open(catalogAssetPath)
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        val jsonObject = Gson().fromJson(reader, BundledCatalog::class.java)
        reader.close()
        
        return jsonObject.tracks
    }

    /**
     * Wrapper object for the bundled catalog JSON structure.
     */
    private data class BundledCatalog(
        val project: ProjectInfo,
        val tracks: List<BundledTrack>
    )

    /**
     * Project information for the bundled app.
     */
    private data class ProjectInfo(
        val artistName: String,
        val projectName: String,
        val mixingEngineer: String,
        val projectDate: String,
        val totalTracks: Int,
        val audioFormat: String,
        val sampleRate: String,
        val bitDepth: String
    )

    /**
     * Bundled track information with resource references.
     */
    private data class BundledTrack(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val albumArtist: String = "",
        val composer: String = "",
        val trackNumber: Int? = null,
        val discNumber: Int? = null,
        val durationMs: Long,
        val audioUri: String, // Asset URI for audio (e.g., "file:///android_asset/audio/01 while it counts.wav")
        val artworkResource: String, // Resource name for artwork (e.g., "track_01_art")
        val format: String, // "FLAC", "WAV", "MP3"
        val sampleRate: String, // "96kHz", "48kHz", etc.
        val bitDepth: String, // "24-bit", "16-bit"
        val mixingNotes: String = "", // Professional notes from mixing engineer
        val videoResource: String? = null, // Optional video resource name (e.g., "track_03_video")
        val hasVideo: Boolean = false,
        val videoPath: String? = null,
        val originalAudioUri: String? = null
    )

    /**
     * Artwork information for tracks
     */
    private data class ArtworkInfo(
        val type: ArtworkType,
        val imageUri: Uri, // Always present - either actual image or video thumbnail
        val videoUri: Uri? // Only present for video artwork
    )

    /**
     * Type of artwork
     */
    private enum class ArtworkType {
        IMAGE, VIDEO
    }

    companion object {
        const val TAG = "LocalBundledSource"
    }
} 