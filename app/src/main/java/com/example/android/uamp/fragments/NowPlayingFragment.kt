/*
 * Copyright 2019 Google Inc. All rights reserved.
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

package com.example.android.uamp.fragments

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.android.uamp.MainActivity
import com.example.android.uamp.R
import com.example.android.uamp.databinding.FragmentNowplayingBinding
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel
import kotlin.math.abs

/**
 * A fragment representing a now playing screen with support for both video and image artwork.
 */
class NowPlayingFragment : Fragment() {

    private val viewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(requireContext())
    }

    private val musicServiceConnection by lazy {
        InjectorUtils.provideMusicServiceConnection(requireContext())
    }

    private val mainActivityViewModel by lazy {
        (activity as? MainActivity)?.viewModel
    }

    private var _binding: FragmentNowplayingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var gestureDetector: GestureDetector
    
    // Auto-hide functionality
    private val hideHandler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null
    
    // Controls auto-hide functionality (for full-screen mode)
    private val controlsHideHandler = Handler(Looper.getMainLooper())
    private var controlsHideRunnable: Runnable? = null
    
    // Position tracking functionality
    private val positionHandler = Handler(Looper.getMainLooper())
    private var positionRunnable: Runnable? = null
    private var isTrackingPosition = false
    
    private var isFullScreen: Boolean = false

    // Video artwork management
    private var currentVideoView: VideoView? = null
    private var isVideoArtwork = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowplayingBinding.inflate(inflater, container, false)
        isFullScreen = arguments?.getBoolean("isFullScreen", false) ?: false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize gesture detector
        setupGestureDetector()

        // Subscribe to metadata changes
        viewModel.mediaMetadata.observe(viewLifecycleOwner) { metadata ->
            android.util.Log.d(TAG, "Metadata observer called: title=${metadata?.title}, duration=${metadata?.duration}")
            updateUI(metadata)
        }

        // Subscribe to media button changes
        viewModel.mediaButtonRes.observe(viewLifecycleOwner) { btnRes ->
            binding.mediaButton.setImageResource(btnRes)
        }

        // Subscribe to playback state
        viewModel.playbackState.observe(viewLifecycleOwner) { state: Int ->
            binding.mediaButton.isEnabled = state != Player.STATE_IDLE
            // Show/hide the fragment based on playback state
            val shouldShow = state != Player.STATE_IDLE
            binding.root.visibility = if (shouldShow) View.VISIBLE else View.GONE
            
            // Start auto-hide timer when mini player becomes visible (both contexts)
            if (shouldShow && !isFullScreen) {
                // Add a small delay to ensure the view is fully visible before starting timer
                Handler(Looper.getMainLooper()).postDelayed({
                    if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE && !isInMediaItemFragment()) {
                        resetAutoHideTimer()
                    }
                }, 100) // 100ms delay
            } else if (!shouldShow) {
                cancelAutoHideTimer()
            }
        }

        // Subscribe to repeat mode changes
        viewModel.repeatMode.observe(viewLifecycleOwner) { repeatMode ->
            updateRepeatButton(repeatMode)
        }

        // Subscribe to shuffle mode changes
        viewModel.shuffleMode.observe(viewLifecycleOwner) { shuffleEnabled ->
            updateShuffleButton(shuffleEnabled)
        }

        // Show/hide collapse button based on mode
        binding.collapseButton.visibility = if (isFullScreen) View.VISIBLE else View.GONE

        // Set up click listeners with interaction tracking
        binding.mediaButton.setOnClickListener {
            onUserInteraction()
            viewModel.playPause()
        }

        binding.nextButton.setOnClickListener {
            onUserInteraction()
            viewModel.skipNext()
        }

        binding.previousButton.setOnClickListener {
            onUserInteraction()
            viewModel.skipPrevious()
        }

        // Collapse button click listener
        binding.collapseButton.setOnClickListener {
            if (isFullScreen) {
                cancelControlsHideTimer() // Cancel controls timer when collapsing
            }
            collapseFromFullScreen()
        }

        // Repeat button click listener
        binding.repeatButton.setOnClickListener {
            onUserInteraction()
            viewModel.toggleRepeatMode()
        }

        // Shuffle button click listener
        binding.shuffleButton.setOnClickListener {
            onUserInteraction()
            viewModel.toggleShuffleMode()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && seekBar != null) {
                    onUserInteraction()
                    
                    // Get the total duration to calculate the actual seek position
                    val totalDuration = viewModel.mediaDuration.value ?: 0L
                    if (totalDuration > 0) {
                        // Calculate seek position based on progress percentage
                        val seekPosition = (progress.toFloat() / seekBar.max.toFloat() * totalDuration).toLong()
                        viewModel.seekTo(seekPosition)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                onUserInteraction()
            }
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                // Reset controls timer when user finishes seeking
                if (isFullScreen) {
                    resetControlsHideTimer()
                }
            }
        })

        // Set up touch handling with gesture detection
        setupTouchHandling()
        
        // Start auto-hide timer if mini player is already visible when fragment is created
        if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            // Add a small delay to ensure everything is properly set up
            Handler(Looper.getMainLooper()).postDelayed({
                if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE) {
                    resetAutoHideTimer()
                }
            }, 200) // 200ms delay for initial setup
        }
        
        // Start controls auto-hide timer if in full-screen mode
        if (isFullScreen) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (_binding != null && isFullScreen) {
                    resetControlsHideTimer()
                }
            }, 1000) // 1 second delay to let user see controls initially
        }

        // Start manual position tracking timer instead of observing LiveData to avoid conflicts
        startPositionTracking()
    }

    override fun onResume() {
        super.onResume()
        // Start auto-hide timer when fragment resumes in mini player mode
        // This ensures auto-hide works when returning from full-screen or when the fragment becomes active
        if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            resetAutoHideTimer()
        }
        
        // Resume video playback if it was video artwork
        if (isVideoArtwork && currentVideoView != null) {
            try {
                currentVideoView?.resume()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to resume video", e)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause video playback to save resources
        if (isVideoArtwork && currentVideoView != null) {
            try {
                currentVideoView?.pause()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to pause video", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop video and clean up
        stopVideo()
        cancelAutoHideTimer()
        cancelControlsHideTimer()
        stopPositionTracking()
        _binding = null
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                
                // Determine if this is a horizontal or vertical swipe
                if (abs(diffX) > abs(diffY)) {
                    // Horizontal swipe - check threshold and velocity
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - previous track
                            onSwipeRight()
                        } else {
                            // Swipe left - next track
                            onSwipeLeft()
                        }
                        return true
                    }
                } else {
                    // Vertical swipe - check threshold and velocity
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            // Swipe down - navigate back (full screen only)
                            onSwipeDown()
                        } else {
                            // Swipe up - expand to full screen (mini player only)
                            onSwipeUp()
                        }
                        return true
                    }
                }
                return false
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Handle single tap based on mode
                if (!isFullScreen) {
                    // Mini player mode - expand to full screen
                    onUserInteraction()
                    expandToFullScreen()
                    return true
                } else {
                    // Full-screen mode - show controls
                    showControls()
                    return true
                }
            }
            
            override fun onDown(e: MotionEvent): Boolean {
                // Track any touch interaction and enable gesture detection
                onUserInteraction()
                return true // Must return true to enable gesture detection
            }
        })
    }

    private fun setupTouchHandling() {
        if (!isFullScreen) {
            // In mini player mode, allow click to expand and swipe gestures
            binding.root.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }
        } else {
            // In full-screen mode, handle gestures and show controls on tap
            binding.root.setOnTouchListener { _, event ->
                // Always let gesture detector handle the event first
                gestureDetector.onTouchEvent(event)
                
                // Always return true to consume all touch events and prevent them from reaching fragments underneath
                true
            }
            
            // Remove the separate album art click listener since gestures handle this
        }
    }

    private fun onSwipeLeft() {
        // Swipe left - next track
        onUserInteraction()
        viewModel.skipNext()
    }

    private fun onSwipeRight() {
        // Swipe right - previous track
        onUserInteraction()
        viewModel.skipPrevious()
    }

    private fun onSwipeDown() {
        // Swipe down - navigate back to MediaItemFragment (full screen only)
        if (isFullScreen) {
            onUserInteraction()
            collapseFromFullScreen()
        }
    }

    private fun onSwipeUp() {
        // Swipe up - expand to full screen (mini player only)
        if (!isFullScreen) {
            onUserInteraction()
            expandToFullScreen()
        }
    }

    private fun onUserInteraction() {
        // Track user interaction and reset auto-hide timer (mini player only)
        if (!isFullScreen) {
            resetAutoHideTimer()
        } else {
            // In full-screen mode, reset controls hide timer and show controls if hidden
            showControls()
        }
    }

    private fun resetAutoHideTimer() {
        // Don't start auto-hide timer if we're in MediaItemFragment context
        if (isInMediaItemFragment()) {
            return
        }
        
        cancelAutoHideTimer()
        
        hideRunnable = Runnable {
            // Only hide if we're still in mini player mode and fragment is still valid
            // Also check we're not in MediaItemFragment
            if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE && !isInMediaItemFragment()) {
                hideMiniPlayer()
            }
        }
        
        hideHandler.postDelayed(hideRunnable!!, AUTO_HIDE_DELAY_MS)
    }

    private fun cancelAutoHideTimer() {
        hideRunnable?.let { runnable ->
            hideHandler.removeCallbacks(runnable)
            hideRunnable = null
        }
    }

    private fun hideMiniPlayer() {
        // Animate the mini player out with a fade effect
        if (_binding != null) {
            val fadeOut = ObjectAnimator.ofFloat(binding.root, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.start()
            
            // Hide after animation completes
            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (_binding != null) {
                        binding.root.visibility = View.GONE
                        binding.root.alpha = 1f // Reset alpha for next show
                    }
                }
            })
        }
    }

    private fun showMiniPlayer() {
        // Show mini player with fade in effect
        if (_binding != null && !isFullScreen) {
            binding.root.alpha = 0f
            binding.root.visibility = View.VISIBLE
            
            val fadeIn = ObjectAnimator.ofFloat(binding.root, "alpha", 0f, 1f)
            fadeIn.duration = 300
            fadeIn.start()
            
            // Only start auto-hide timer if NOT in MediaItemFragment
            if (!isInMediaItemFragment()) {
                resetAutoHideTimer()
            }
        }
    }

    private fun updateUI(metadata: NowPlayingFragmentViewModel.NowPlayingMetadata?) {
        // Update UI with metadata
        metadata?.let {
            binding.title.text = it.title
            binding.subtitle.text = it.subtitle
            binding.duration.text = it.duration
            
            // If mini player was hidden and we get new metadata, show it
            if (!isFullScreen && binding.root.visibility != View.VISIBLE) {
                showMiniPlayer()
            } else if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
                // If mini player is already visible, reset the auto-hide timer
                // Add a small delay to ensure the UI update is complete
                Handler(Looper.getMainLooper()).postDelayed({
                    if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE && !isInMediaItemFragment()) {
                        resetAutoHideTimer()
                    }
                }, 50) // Small delay for UI update completion
            }
            
            // Load artwork (video or image) based on metadata
            loadArtwork(it)
        }
    }

    private fun loadArtwork(metadata: NowPlayingFragmentViewModel.NowPlayingMetadata) {
        // Stop any existing video before loading new artwork
        stopVideo()
        
        // Get artwork type from metadata extras
        val artworkType = getArtworkTypeFromMetadata()
        val videoUri = getVideoUriFromMetadata()
        
        when (artworkType) {
            "VIDEO" -> {
                if (videoUri != null) {
                    loadVideoArtwork(videoUri, metadata.albumArtUri)
                } else {
                    // Fallback to image if video URI is missing
                    loadImageArtwork(metadata.albumArtUri)
                }
            }
            else -> {
                loadImageArtwork(metadata.albumArtUri)
            }
        }
    }

    private fun loadVideoArtwork(videoUri: Uri, thumbnailUri: Uri) {
        isVideoArtwork = true
        
        // Show video view, hide image view
        binding.backgroundVideo.visibility = View.VISIBLE
        binding.albumArt.visibility = View.GONE
        binding.darkOverlay.visibility = View.VISIBLE // Add overlay for better text readability
        
        // Load thumbnail for palette extraction first
        loadThumbnailForPalette(thumbnailUri)
        
        // Delay video setup to ensure music playback is established first
        Handler(Looper.getMainLooper()).postDelayed({
            setupVideoView(binding.backgroundVideo, videoUri)
        }, 500) // 500ms delay to let music start first
    }

    private fun loadImageArtwork(imageUri: Uri) {
        isVideoArtwork = false
        
        // Stop any existing video first
        stopVideo()
        
        // Show image view, hide video view
        binding.albumArt.visibility = View.VISIBLE
        binding.backgroundVideo.visibility = View.GONE
        binding.darkOverlay.visibility = View.GONE
        
        // Load image with dynamic color extraction
        Glide.with(this)
            .asBitmap()
            .load(imageUri)
            .placeholder(R.drawable.default_art)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Check if fragment is still valid before accessing binding
                    if (_binding == null) return
                    
                    // Set the album art
                    binding.albumArt.setImageBitmap(resource)
                    
                    // Generate color palette from the bitmap
                    Palette.from(resource).generate { palette ->
                        // Check again in case fragment was destroyed during palette generation
                        if (_binding == null) return@generate
                        
                        palette?.let { extractedPalette ->
                            applyPaletteColors(extractedPalette)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Check if fragment is still valid before accessing binding
                    if (_binding == null) return
                    
                    // Set placeholder and use default white colors
                    binding.albumArt.setImageDrawable(placeholder)
                    applyDefaultColors()
                }
            })
    }

    private fun setupVideoView(videoView: VideoView, videoUri: Uri) {
        try {
            android.util.Log.d(TAG, "Setting up video view with URI: $videoUri")
            
            currentVideoView = videoView
            
            // Set video URI (works for both resource and asset URIs)
            videoView.setVideoURI(videoUri)
            
            // Set up completion listener to restart video (looping)
            videoView.setOnCompletionListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                videoView.start()
            }
            
            // Set up prepared listener
            videoView.setOnPreparedListener { mediaPlayer ->
                try {
                    // Configure video for background playback without interfering with music
                    mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    mediaPlayer.isLooping = true
                    
                    // Critical: Mute the video audio completely to prevent interference
                    mediaPlayer.setVolume(0f, 0f)
                    
                    // Set audio session ID to isolate from main music playback
                    try {
                        // Use a separate audio session to avoid conflicts
                        val audioManager = requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                        val audioSessionId = audioManager.generateAudioSessionId()
                        mediaPlayer.audioSessionId = audioSessionId
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Could not set separate audio session for video", e)
                    }
                    
                    // Start video playback
                    videoView.start()
                    android.util.Log.d(TAG, "Video started successfully")
                    
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error configuring video player", e)
                    fallbackToImageArtwork()
                }
            }
            
            // Set up error listener
            videoView.setOnErrorListener { _, what, extra ->
                android.util.Log.e(TAG, "Video playback error: what=$what, extra=$extra, uri=$videoUri")
                // Fallback to image artwork
                fallbackToImageArtwork()
                true // Error handled
            }
            
            // Set up info listener to track video events
            videoView.setOnInfoListener { _, what, extra ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        android.util.Log.d(TAG, "Video rendering started")
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        android.util.Log.d(TAG, "Video buffering started")
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        android.util.Log.d(TAG, "Video buffering ended")
                    }
                }
                false // Don't consume the info event
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to setup video", e)
            fallbackToImageArtwork()
        }
    }

    private fun loadThumbnailForPalette(thumbnailUri: Uri) {
        // Load thumbnail for color palette extraction (without displaying it)
        Glide.with(this)
            .asBitmap()
            .load(thumbnailUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Generate color palette from the thumbnail
                    Palette.from(resource).generate { palette ->
                        if (_binding == null) return@generate
                        
                        palette?.let { extractedPalette ->
                            applyPaletteColors(extractedPalette)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Apply default colors if thumbnail fails
                    applyDefaultColors()
                }
            })
    }

    private fun fallbackToImageArtwork() {
        // Fallback to current track's image artwork if video fails
        val metadata = viewModel.mediaMetadata.value
        metadata?.let { loadImageArtwork(it.albumArtUri) }
    }

    private fun stopVideo() {
        currentVideoView?.let { videoView ->
            try {
                android.util.Log.d(TAG, "Stopping video playback")
                
                // Stop playback if it's playing
                if (videoView.isPlaying) {
                    videoView.stopPlayback()
                }
                
                // Clear the video view
                videoView.suspend()
                
                // Hide the video view
                videoView.visibility = View.GONE
                
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Error stopping video", e)
            }
        }
        currentVideoView = null
        isVideoArtwork = false
        
        // Ensure video view is hidden and image view is visible
        if (_binding != null) {
            binding.backgroundVideo.visibility = View.GONE
            binding.albumArt.visibility = View.VISIBLE
            binding.darkOverlay.visibility = View.GONE
        }
        
        android.util.Log.d(TAG, "Video cleanup completed")
    }

    private fun getArtworkTypeFromMetadata(): String? {
        return try {
            // Get raw MediaMetadata from musicServiceConnection to access extras
            val rawMetadata = musicServiceConnection.nowPlaying.value
            val artworkType = rawMetadata?.extras?.getString("artwork_type")
            
            if (!artworkType.isNullOrEmpty()) {
                android.util.Log.d(TAG, "Got artwork type from metadata extras: $artworkType")
                artworkType.uppercase()
            } else {
                // Fallback to legacy detection method
                val metadata = viewModel.mediaMetadata.value
                metadata?.id?.let { mediaId ->
                    val trackNumber = extractTrackNumberFromId(mediaId)
                    if (trackNumber > 0) {
                        val videoResourceName = "track_${trackNumber.toString().padStart(2, '0')}_video"
                        if (hasVideoResource(videoResourceName)) "VIDEO" else "IMAGE"
                    } else {
                        "IMAGE"
                    }
                } ?: "IMAGE"
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get artwork type", e)
            "IMAGE" // Default fallback
        }
    }

    private fun getVideoUriFromMetadata(): Uri? {
        return try {
            // Get raw MediaMetadata from musicServiceConnection to access extras
            val rawMetadata = musicServiceConnection.nowPlaying.value
            val videoUriString = rawMetadata?.extras?.getString("video_uri")
            
            if (!videoUriString.isNullOrEmpty()) {
                val videoUri = Uri.parse(videoUriString)
                android.util.Log.d(TAG, "Got video URI from metadata extras: $videoUri")
                videoUri
            } else {
                // Fallback to legacy construction method
                val metadata = viewModel.mediaMetadata.value
                val mediaId = metadata?.id
                if (mediaId != null) {
                    val trackNumber = extractTrackNumberFromId(mediaId)
                    if (trackNumber > 0) {
                        val videoResourceName = "track_${trackNumber.toString().padStart(2, '0')}_video"
                        val fallbackUri = Uri.parse("android.resource://${requireContext().packageName}/raw/$videoResourceName")
                        android.util.Log.d(TAG, "Generated fallback video URI: $fallbackUri")
                        fallbackUri
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get video URI", e)
            null
        }
    }

    private fun extractTrackNumberFromId(mediaId: String): Int {
        return try {
            // Try to extract track number from media ID (assuming format like "track_01" or similar)
            val match = Regex("(\\d+)").find(mediaId)
            match?.value?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun hasVideoResource(resourceName: String): Boolean {
        return try {
            val resourceId = resources.getIdentifier(resourceName, "raw", requireContext().packageName)
            resourceId != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun applyPaletteColors(palette: Palette) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        // Extract colors with fallbacks
        val vibrantColor = palette.vibrantSwatch?.rgb
        val darkVibrantColor = palette.darkVibrantSwatch?.rgb
        val lightVibrantColor = palette.lightVibrantSwatch?.rgb
        val mutedColor = palette.mutedSwatch?.rgb
        val darkMutedColor = palette.darkMutedSwatch?.rgb

        // Choose the best color for text and controls
        val primaryColor = vibrantColor 
            ?: lightVibrantColor 
            ?: mutedColor 
            ?: ContextCompat.getColor(requireContext(), android.R.color.white)

        val secondaryColor = darkVibrantColor 
            ?: darkMutedColor 
            ?: vibrantColor 
            ?: ContextCompat.getColor(requireContext(), android.R.color.white)

        // Determine background and text colors for app theme
        val backgroundColor = darkMutedColor 
            ?: darkVibrantColor 
            ?: android.graphics.Color.argb(200, 
                android.graphics.Color.red(primaryColor),
                android.graphics.Color.green(primaryColor),
                android.graphics.Color.blue(primaryColor)
            )

        val textColor = if (isColorDark(backgroundColor)) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.BLACK
        }

        // Update app-wide theme
        mainActivityViewModel?.updateAppTheme(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            backgroundColor = backgroundColor,
            textColor = textColor
        )

        // Apply colors to UI elements
        applyColorsToUI(primaryColor, secondaryColor)
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * android.graphics.Color.red(color) + 
                           0.587 * android.graphics.Color.green(color) + 
                           0.114 * android.graphics.Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun applyDefaultColors() {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        
        // Reset app theme to default colors
        mainActivityViewModel?.resetAppThemeToDefault()
        
        applyColorsToUI(whiteColor, whiteColor)
    }

    private fun applyColorsToUI(primaryColor: Int, secondaryColor: Int) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        // Apply colors to text
        binding.title.setTextColor(primaryColor)
        binding.subtitle.setTextColor(primaryColor)
        binding.duration.setTextColor(primaryColor)

        // Apply primary color to ALL control buttons for consistent visibility
        binding.mediaButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)
        binding.previousButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)
        binding.nextButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)
        binding.collapseButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)
        binding.repeatButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)
        binding.shuffleButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)

        // Apply colors to seek bar - use primary color for better visibility
        binding.seekBar.progressTintList = android.content.res.ColorStateList.valueOf(primaryColor)
        binding.seekBar.thumbTintList = android.content.res.ColorStateList.valueOf(primaryColor)
        binding.seekBar.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.argb(60, 
                android.graphics.Color.red(primaryColor),
                android.graphics.Color.green(primaryColor),
                android.graphics.Color.blue(primaryColor)
            )
        )
    }

    private fun expandToFullScreen() {
        // Cancel auto-hide timer when expanding to full-screen
        cancelAutoHideTimer()
        
        // Create a new instance for full screen - this will use the same layout
        // but without height constraints, showing the full album art
        val fullScreenFragment = newInstance(true)
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fullScreenFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun collapseFromFullScreen() {
        // Pop the back stack to remove the full-screen fragment
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            // If no back stack, manually navigate back to the MediaItemFragment
            activity?.let { mainActivity ->
                // Remove this full-screen fragment from the content view
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .commit()
            }
        }
    }

    /**
     * Public method to show the mini player when called from other fragments
     * (e.g., when scrolling in MediaItemFragment)
     */
    fun showMiniPlayerOnScroll() {
        // Only show if music is playing and we're in mini player mode
        val isPlayingMusic = viewModel.playbackState.value != Player.STATE_IDLE &&
                           viewModel.mediaMetadata.value != null
        
        if (!isFullScreen && isPlayingMusic && binding.root.visibility != View.VISIBLE) {
            showMiniPlayer()
        } else if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            // If already visible, only reset auto-hide timer if NOT in MediaItemFragment
            if (!isInMediaItemFragment()) {
                resetAutoHideTimer()
            }
        }
    }

    /**
     * Public method to start auto-hide timer (useful for external calls)
     */
    fun startAutoHideTimer() {
        if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            resetAutoHideTimer()
        }
    }

    /**
     * Check if we're currently in a MediaItemFragment context
     * (i.e., the MediaItemFragment is visible in the main container)
     */
    private fun isInMediaItemFragment(): Boolean {
        val activity = activity ?: return false
        val fragmentManager = activity.supportFragmentManager
        
        // Check if there's a MediaItemFragment in the main container
        val mainFragment = fragmentManager.findFragmentById(R.id.mediaItemFragment)
        return mainFragment is MediaItemFragment
    }

    private fun updateRepeatButton(repeatMode: Int) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> {
                binding.repeatButton.setImageResource(R.drawable.ic_repeat_black_24dp)
                binding.repeatButton.alpha = 0.5f // Dimmed when off
            }
            Player.REPEAT_MODE_ONE -> {
                binding.repeatButton.setImageResource(R.drawable.ic_repeat_one_black_24dp)
                binding.repeatButton.alpha = 1.0f // Full opacity when on
            }
            Player.REPEAT_MODE_ALL -> {
                binding.repeatButton.setImageResource(R.drawable.ic_repeat_black_24dp)
                binding.repeatButton.alpha = 1.0f // Full opacity when on
            }
        }
    }

    private fun updateShuffleButton(shuffleEnabled: Boolean) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        binding.shuffleButton.alpha = if (shuffleEnabled) 1.0f else 0.5f
    }

    private fun resetControlsHideTimer() {
        // Only hide controls in full-screen mode
        if (!isFullScreen) return
        
        cancelControlsHideTimer()
        
        controlsHideRunnable = Runnable {
            // Only hide if we're still in full-screen mode and fragment is still valid
            if (_binding != null && isFullScreen) {
                hideControls()
            }
        }
        
        controlsHideHandler.postDelayed(controlsHideRunnable!!, CONTROLS_HIDE_DELAY_MS)
    }

    private fun cancelControlsHideTimer() {
        controlsHideRunnable?.let { runnable ->
            controlsHideHandler.removeCallbacks(runnable)
            controlsHideRunnable = null
        }
    }

    private fun hideControls() {
        // Hide the controls container in full-screen mode
        if (_binding != null && isFullScreen) {
            // Find the LinearLayout with controls at the bottom
            val controlsContainer = binding.root.getChildAt(binding.root.childCount - 1) as? android.widget.LinearLayout
            
            controlsContainer?.let { container ->
                // Fade out the controls container
                val fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0f)
                fadeOut.duration = 300
                fadeOut.start()
                
                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (_binding != null && isFullScreen) {
                            container.visibility = View.GONE
                        }
                    }
                })
            }
        }
    }

    private fun showControls() {
        // Show controls container in full-screen mode
        if (_binding != null && isFullScreen) {
            // Find the LinearLayout with controls at the bottom
            val controlsContainer = binding.root.getChildAt(binding.root.childCount - 1) as? android.widget.LinearLayout
            
            controlsContainer?.let { container ->
                if (container.visibility != View.VISIBLE) {
                    container.alpha = 0f
                    container.visibility = View.VISIBLE
                    
                    val fadeIn = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f)
                    fadeIn.duration = 300
                    fadeIn.start()
                }
                
                // Reset the controls hide timer
                resetControlsHideTimer()
            }
        }
    }

    private fun updateSeekBarAndCurrentTime(position: Long) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        // Get the total duration - try multiple approaches
        val totalDuration = viewModel.mediaDuration.value?.let { duration ->
            duration
        } ?: viewModel.mediaMetadata.value?.let { metadata ->
            // Parse duration from the metadata duration string (format: "X:XX")
            val durationStr = metadata.duration
            val parts = durationStr.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toIntOrNull() ?: 0
                val seconds = parts[1].toIntOrNull() ?: 0
                val durationMs = (minutes * 60 + seconds) * 1000L
                durationMs
            } else {
                0L
            }
        } ?: 0L
        
        if (totalDuration > 0 && position >= 0) {
            // Use a fixed max value for the seek bar (10000 for good precision)
            val seekBarMax = 10000
            binding.seekBar.max = seekBarMax
            
            // Calculate progress as a percentage of total duration
            val progressPercent = (position.toFloat() / totalDuration.toFloat() * seekBarMax).toInt()
            binding.seekBar.progress = progressPercent
        }
        
        // Update current time display 
        val currentTimeText = NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(position)
        val totalTimeText = NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(totalDuration)
        
        // Show current time / total time format
        if (totalDuration > 0) {
            val timeText = "$currentTimeText / $totalTimeText"
            binding.duration.text = timeText
        }
    }
    
    private fun startPositionTracking() {
        if (isTrackingPosition) return
        isTrackingPosition = true
        
        positionRunnable = object : Runnable {
            override fun run() {
                if (_binding == null || !isTrackingPosition) return
                
                // Get current position directly from ViewModel without observing
                val currentPos = viewModel.mediaPosition.value ?: 0L
                if (currentPos >= 0) {
                    updateSeekBarAndCurrentTime(currentPos)
                }
                
                // Continue tracking if still playing
                if (viewModel.playbackState.value == Player.STATE_READY && isTrackingPosition) {
                    positionHandler.postDelayed(this, 1000) // Update every second
                } else {
                    isTrackingPosition = false
                }
            }
        }
        
        positionHandler.postDelayed(positionRunnable!!, 1000)
    }
    
    private fun stopPositionTracking() {
        isTrackingPosition = false
        positionRunnable?.let {
            positionHandler.removeCallbacks(it)
            positionRunnable = null
        }
    }

    companion object {
        private const val TAG = "NowPlayingFragment"
        
        // Auto-hide delay for mini player (7 seconds)
        private const val AUTO_HIDE_DELAY_MS = 7000L
        
        // Controls hide delay for full-screen mode (5 seconds)
        private const val CONTROLS_HIDE_DELAY_MS = 5000L
        
        // Gesture detection thresholds
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100

        fun newInstance(isFullScreen: Boolean = false): NowPlayingFragment {
            return NowPlayingFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isFullScreen", isFullScreen)
                }
            }
        }
    }
}
