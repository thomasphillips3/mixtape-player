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
                
                android.util.Log.d(TAG, "Scanned artwork map: $artworkMap")
                
                // Convert to MediaMetadata with bundled file URIs and artwork
                musicCatalog.map { track ->
                    val trackArtwork = artworkMap[track.trackNumber] ?: artworkMap[0] // 0 = fallback album art
                    
                    // Determine the best artwork URI - prioritize scanned artwork over catalog reference
                    val artworkUri = when {
                        trackArtwork != null -> {
                            android.util.Log.d(TAG, "Using scanned artwork for track ${track.trackNumber}: ${trackArtwork.imageUri}")
                            trackArtwork.imageUri
                        }
                        else -> {
                            val fallbackUri = Uri.parse("android.resource://${context.packageName}/drawable/${track.artworkResource}")
                            android.util.Log.d(TAG, "Using catalog fallback artwork for track ${track.trackNumber}: $fallbackUri")
                            fallbackUri
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
     * Scan drawable and raw resource directories for processed artwork files
     * Returns map of track number to artwork info
     */
    private fun scanArtworkFiles(): Map<Int, ArtworkInfo> {
        val artworkMap = mutableMapOf<Int, ArtworkInfo>()
        
        try {
            android.util.Log.d(TAG, "Scanning for processed artwork resources...")
            
            // Check for fallback album art first
            val fallbackResourceName = "album_art_fallback"
            if (hasDrawableResource(fallbackResourceName)) {
                val imageUri = Uri.parse("android.resource://${context.packageName}/drawable/$fallbackResourceName")
                artworkMap[0] = ArtworkInfo(ArtworkType.IMAGE, imageUri, null)
                android.util.Log.d(TAG, "Found fallback album art resource: $fallbackResourceName")
            }
            
            // Scan for track-specific artwork (tracks 1-20 to be safe)
            for (trackNumber in 1..20) {
                val trackPadded = trackNumber.toString().padStart(2, '0')
                
                // Check for video artwork first (higher priority)
                val videoResourceName = "track_${trackPadded}_video"
                val thumbResourceName = "track_${trackPadded}_thumb"
                
                if (hasRawResource(videoResourceName)) {
                    // For video artwork, always use fallback album art as thumbnail for better visual consistency
                    val thumbnailUri = Uri.parse("android.resource://${context.packageName}/drawable/album_art_fallback")
                    val videoUri = Uri.parse("android.resource://${context.packageName}/raw/$videoResourceName")
                    artworkMap[trackNumber] = ArtworkInfo(ArtworkType.VIDEO, thumbnailUri, videoUri)
                    android.util.Log.d(TAG, "Found video artwork for track $trackNumber: $videoResourceName + album_art_fallback thumbnail")
                } else {
                    // For all other tracks, use the fallback album art to ensure consistent artwork display
                    val imageUri = Uri.parse("android.resource://${context.packageName}/drawable/album_art_fallback")
                    artworkMap[trackNumber] = ArtworkInfo(ArtworkType.IMAGE, imageUri, null)
                    android.util.Log.d(TAG, "Using fallback album art for track $trackNumber")
                }
            }
            
            android.util.Log.d(TAG, "Final artwork map: $artworkMap")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error scanning artwork resources", e)
        }
        
        return artworkMap
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