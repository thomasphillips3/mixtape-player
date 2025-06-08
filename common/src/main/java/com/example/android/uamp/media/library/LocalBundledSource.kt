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
                
                // Scan for artwork files and build artwork map
                val artworkMap = scanArtworkFiles()
                
                // Convert to MediaMetadata with bundled file URIs and artwork
                musicCatalog.map { track ->
                    val trackArtwork = artworkMap[track.trackNumber] ?: artworkMap[0] // 0 = fallback album art
                    
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setAlbumArtist(track.albumArtist)
                        .setComposer(track.composer)
                        .setTrackNumber(track.trackNumber)
                        .setDiscNumber(track.discNumber)
                        .setArtworkUri(trackArtwork?.imageUri ?: Uri.parse("android.resource://${context.packageName}/drawable/${track.artworkResource}"))
                        .setExtras(Bundle().apply {
                            putString("media_id", track.id)
                            putString("media_uri", "android.resource://${context.packageName}/raw/${track.audioResource}")
                            putLong("duration", track.durationMs)
                            putLong("download_status", DOWNLOAD_STATUS_DOWNLOADED) // Always downloaded since bundled
                            putLong("flag", 1L) // FLAG_PLAYABLE
                            putString("audio_format", track.format)
                            putString("sample_rate", track.sampleRate)
                            putString("bit_depth", track.bitDepth)
                            putString("mixing_notes", track.mixingNotes)
                            
                            // Add artwork information
                            trackArtwork?.let { artwork ->
                                putString("artwork_type", artwork.type.name)
                                putString("artwork_uri", artwork.imageUri.toString())
                                if (artwork.type == ArtworkType.VIDEO) {
                                    putString("video_uri", artwork.videoUri.toString())
                                }
                            }
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
     * Scan assets/artwork directory for video and image files
     * Returns map of track number to artwork info
     */
    private fun scanArtworkFiles(): Map<Int, ArtworkInfo> {
        val artworkMap = mutableMapOf<Int, ArtworkInfo>()
        
        try {
            val assetManager = context.assets
            val artworkFiles = assetManager.list("artwork") ?: emptyArray()
            
            android.util.Log.d(TAG, "Found ${artworkFiles.size} artwork files")
            
            // Scan for fallback album art first
            val fallbackImage = artworkFiles.find { 
                it.lowercase().startsWith("album-art.") && 
                (it.lowercase().endsWith(".png") || it.lowercase().endsWith(".jpg") || it.lowercase().endsWith(".jpeg"))
            }
            
            fallbackImage?.let { filename ->
                val resourceName = "album_art_fallback"
                val imageUri = Uri.parse("android.resource://${context.packageName}/drawable/$resourceName")
                artworkMap[0] = ArtworkInfo(ArtworkType.IMAGE, imageUri, null)
                android.util.Log.d(TAG, "Found fallback album art: $filename")
            }
            
            // Scan for track-specific artwork
            artworkFiles.forEach { filename ->
                val trackNumber = extractTrackNumber(filename)
                if (trackNumber > 0) {
                    val artworkInfo = when {
                        filename.lowercase().endsWith(".mp4") -> {
                            // Video artwork
                            val resourceName = "track_${trackNumber.toString().padStart(2, '0')}_video"
                            val videoUri = Uri.parse("android.resource://${context.packageName}/raw/$resourceName")
                            // For video, we'll also need a thumbnail image
                            val thumbnailUri = Uri.parse("android.resource://${context.packageName}/drawable/track_${trackNumber.toString().padStart(2, '0')}_thumb")
                            ArtworkInfo(ArtworkType.VIDEO, thumbnailUri, videoUri)
                        }
                        filename.lowercase().endsWith(".png") || 
                        filename.lowercase().endsWith(".jpg") || 
                        filename.lowercase().endsWith(".jpeg") -> {
                            // Image artwork
                            val resourceName = "track_${trackNumber.toString().padStart(2, '0')}_art"
                            val imageUri = Uri.parse("android.resource://${context.packageName}/drawable/$resourceName")
                            ArtworkInfo(ArtworkType.IMAGE, imageUri, null)
                        }
                        else -> null
                    }
                    
                    artworkInfo?.let {
                        artworkMap[trackNumber] = it
                        android.util.Log.d(TAG, "Found artwork for track $trackNumber: $filename (${it.type})")
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error scanning artwork files", e)
        }
        
        return artworkMap
    }

    /**
     * Extract track number from filename (e.g., "01_title.mp4" -> 1)
     */
    private fun extractTrackNumber(filename: String): Int {
        return try {
            val match = Regex("^(\\d{1,2})_").find(filename)
            match?.groupValues?.get(1)?.toInt() ?: 0
        } catch (e: Exception) {
            0
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
        val audioResource: String, // Resource name without extension (e.g., "track01")
        val artworkResource: String, // Resource name for artwork (e.g., "album_art")
        val format: String, // "FLAC", "WAV", "MP3"
        val sampleRate: String, // "96kHz", "48kHz", etc.
        val bitDepth: String, // "24-bit", "16-bit"
        val mixingNotes: String = "" // Professional notes from mixing engineer
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