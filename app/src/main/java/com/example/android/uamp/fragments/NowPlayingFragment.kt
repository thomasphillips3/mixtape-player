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
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    private var currentVideoView: PlayerView? = null
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
            
            // Immediately update position when playback state changes to ready
            if (state == Player.STATE_READY) {
                // Get current position immediately and update display
                val currentPos = viewModel.mediaPosition.value ?: 0L
                updateSeekBarAndCurrentTime(currentPos)
                
                // Ensure position tracking is running
                if (!isTrackingPosition) {
                    startPositionTracking()
                }
            }
            
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

        // Subscribe to isPlaying state changes for immediate position updates
        musicServiceConnection.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            if (isPlaying) {
                // Immediately update position when playback starts
                val currentPos = viewModel.mediaPosition.value ?: 0L
                updateSeekBarAndCurrentTime(currentPos)
                
                // Ensure position tracking is running
                if (!isTrackingPosition) {
                    startPositionTracking()
                }
            }
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
            private var userSeeking = false
            
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && seekBar != null) {
                    onUserInteraction()
                    
                    // Get the total duration to calculate the actual seek position
                    val totalDuration = viewModel.mediaDuration.value ?: 0L
                    if (totalDuration > 0) {
                        // Calculate seek position based on progress percentage
                        val seekPosition = (progress.toFloat() / seekBar.max.toFloat() * totalDuration).toLong()
                        
                        // Update time display immediately while seeking
                        updateTimeDisplay(seekPosition, totalDuration)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                onUserInteraction()
                userSeeking = true
                // Stop position tracking while user is seeking
                stopPositionTracking()
            }
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                userSeeking = false
                
                if (seekBar != null) {
                    // Get the total duration to calculate the actual seek position
                    val totalDuration = viewModel.mediaDuration.value ?: 0L
                    if (totalDuration > 0) {
                        // Calculate seek position based on progress percentage
                        val seekPosition = (seekBar.progress.toFloat() / seekBar.max.toFloat() * totalDuration).toLong()
                        viewModel.seekTo(seekPosition)
                    }
                }
                
                // Resume position tracking after seeking
                Handler(Looper.getMainLooper()).postDelayed({
                    startPositionTracking()
                }, 500) // Small delay to let seek complete
                
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
                currentVideoView?.player?.play()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to resume video", e)
            }
        }
        
        // Ensure position tracking is running
        if (!isTrackingPosition) {
            startPositionTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause video playback to save resources
        if (isVideoArtwork && currentVideoView != null) {
            try {
                currentVideoView?.player?.pause()
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
            
            // Load artwork based on track type
            loadArtworkForTrack(it)
        }
    }

    private fun loadArtworkForTrack(metadata: NowPlayingFragmentViewModel.NowPlayingMetadata) {
        // Get track type from metadata extras
        val trackType = getTrackTypeFromMetadata()
        
        when (trackType) {
            "MUSIC_VIDEO" -> {
                // For music videos, load and play the actual video
                val videoUri = getVideoUriFromMetadata()
                
                if (videoUri != null) {
                    loadVideoArtwork(videoUri)
                } else {
                    // Fallback to static artwork if video URI not found
                    loadImageArtwork(metadata.albumArtUri)
                }
            }
            else -> {
                // For audio-only tracks, show static artwork
                loadImageArtwork(metadata.albumArtUri)
            }
        }
    }

    private fun getTrackTypeFromMetadata(): String? {
        return try {
            // Get raw MediaMetadata from musicServiceConnection to access extras
            val rawMetadata = musicServiceConnection.nowPlaying.value
            val extras = rawMetadata?.extras
            
            val trackType = extras?.getString("track_type")
            
            if (!trackType.isNullOrEmpty()) {
                trackType
            } else {
                "AUDIO_ONLY" // Default fallback
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get track type", e)
            "AUDIO_ONLY" // Default fallback
        }
    }

    private fun getVideoUriFromMetadata(): Uri? {
        return try {
            // Get raw MediaMetadata from musicServiceConnection to access extras
            val rawMetadata = musicServiceConnection.nowPlaying.value
            val extras = rawMetadata?.extras
            
            val videoPath = extras?.getString("video_path")
            
            if (!videoPath.isNullOrEmpty()) {
                // Create asset URI for the video path (ExoPlayer can handle assets)
                val filename = videoPath.substringAfterLast("/")
                val uri = Uri.parse("file:///android_asset/music-videos/$filename")
                return uri
            } else {
                return null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get video URI", e)
            null
        }
    }

    private fun loadVideoArtwork(videoUri: Uri) {
        isVideoArtwork = true
        
        // Stop any existing video first
        stopVideo()
        
        // Hide image view, show video view
        binding.albumArt.visibility = View.GONE
        binding.backgroundVideo.visibility = View.VISIBLE
        binding.darkOverlay.visibility = View.VISIBLE
        
        // Setup and start video playback
        setupVideoView(binding.backgroundVideo, videoUri)
        
        // Load thumbnail for color palette extraction
        val metadata = viewModel.mediaMetadata.value
        metadata?.let { loadThumbnailForPalette(it.albumArtUri) }
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

    private fun setupVideoView(videoView: PlayerView, videoUri: Uri) {
        try {
            currentVideoView = videoView
            
            // Verify asset exists before setting up video player
            try {
                val assetPath = videoUri.path?.removePrefix("/android_asset/")
                val inputStream = requireContext().assets.open(assetPath!!)
                inputStream.close()
            } catch (e: Exception) {
                android.util.Log.e(TAG, getString(R.string.asset_not_found), e)
                fallbackToImageArtwork()
                return
            }
            
            // Create ExoPlayer instance for video playback
            val exoPlayer = ExoPlayer.Builder(requireContext()).build()
            
            // Configure ExoPlayer for background video (muted, looping)
            exoPlayer.volume = 0f // Mute to prevent audio interference
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE // Loop video
            
            // Set player to PlayerView
            videoView.player = exoPlayer
            
            // Set up player event listener
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            if (currentVideoView == videoView && _binding != null) {
                                exoPlayer.play()
                                videoView.visibility = View.VISIBLE
                                binding.albumArt.visibility = View.GONE
                                binding.darkOverlay.visibility = View.VISIBLE
                            }
                        }
                        Player.STATE_ENDED -> {
                            // Video will restart due to repeat mode
                        }
                        Player.STATE_BUFFERING -> {
                            // Video is buffering
                        }
                        Player.STATE_IDLE -> {
                            // Video is idle
                        }
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e(TAG, getString(R.string.video_playback_error), error)
                    Handler(Looper.getMainLooper()).post {
                        fallbackToImageArtwork()
                    }
                }
            })
            
            // Load and prepare the video
            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            
            // Load thumbnail for color palette extraction
            val metadata = viewModel.mediaMetadata.value
            metadata?.let { loadThumbnailForPalette(it.albumArtUri) }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, getString(R.string.video_playback_error), e)
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
        if (!isVideoArtwork) return // Don't stop if not currently showing video
        
        currentVideoView?.player?.release()
        currentVideoView = null
        isVideoArtwork = false
        
        // Ensure video view is hidden and image view is visible
        if (_binding != null) {
            binding.backgroundVideo.visibility = View.GONE
            binding.albumArt.visibility = View.VISIBLE
            binding.darkOverlay.visibility = View.GONE
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

    private fun applyColorsToUI(primaryColor: Int, _secondaryColor: Int) {
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
            activity?.let { _mainActivity ->
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
        
        // Update time display based on mode
        updateTimeDisplay(position, totalDuration)
    }
    
    private fun updateTimeDisplay(position: Long, totalDuration: Long) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        // Update current time display 
        val currentTimeText = NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(position)
        val totalTimeText = NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(totalDuration)
        
        // Show different format based on mode
        if (isFullScreen && totalDuration > 0) {
            // Full screen mode: show current time / total time format
            val timeText = "$currentTimeText / $totalTimeText"
            binding.duration.text = timeText
        } else {
            // Mini player mode: show only current time
            binding.duration.text = currentTimeText
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
                
                // Continue tracking if still playing and tracking is enabled
                if (isTrackingPosition && 
                    (viewModel.playbackState.value == Player.STATE_READY || 
                     viewModel.playbackState.value == Player.STATE_BUFFERING)) {
                    positionHandler.postDelayed(this, 250) // Update every 250ms for smoother updates
                } else if (isTrackingPosition) {
                    // Restart tracking after a brief pause if playback resumes
                    positionHandler.postDelayed(this, 500) // Check again in 500ms
                }
            }
        }
        
        // Start immediately without delay, then update every 250ms
        positionHandler.post(positionRunnable!!)
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
