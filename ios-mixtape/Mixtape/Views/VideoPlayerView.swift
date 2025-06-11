//
//  VideoPlayerView.swift
//  Mixtape
//
//  Video player for music videos with muted, looping background playback
//

import SwiftUI
import AVFoundation
import UIKit

struct VideoPlayerView: UIViewRepresentable {
    let videoURL: URL
    @Binding var isPlaying: Bool
    
    func makeUIView(context: Context) -> VideoPlayerUIView {
        let view = VideoPlayerUIView()
        view.configure(with: videoURL)
        return view
    }
    
    func updateUIView(_ uiView: VideoPlayerUIView, context: Context) {
        if isPlaying {
            uiView.play()
        } else {
            uiView.pause()
        }
    }
}

class VideoPlayerUIView: UIView {
    private var player: AVPlayer?
    private var playerLayer: AVPlayerLayer?
    private var videoURL: URL?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupPlayer()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupPlayer()
    }
    
    private func setupPlayer() {
        backgroundColor = .black
    }
    
    func configure(with url: URL) {
        videoURL = url
        setupVideoPlayer()
    }
    
    private func setupVideoPlayer() {
        guard let videoURL = videoURL else { return }
        
        // Create player
        let playerItem = AVPlayerItem(url: videoURL)
        player = AVPlayer(playerItem: playerItem)
        
        // Configure for background video (muted, looping)
        player?.isMuted = true
        
        // Create player layer
        playerLayer = AVPlayerLayer(player: player)
        playerLayer?.videoGravity = .resizeAspectFill
        playerLayer?.frame = bounds
        
        if let playerLayer = playerLayer {
            layer.addSublayer(playerLayer)
        }
        
        // Set up looping
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(playerDidFinishPlaying),
            name: .AVPlayerItemDidPlayToEndTime,
            object: playerItem
        )
    }
    
    @objc private func playerDidFinishPlaying() {
        // Loop the video
        player?.seek(to: .zero)
        player?.play()
    }
    
    func play() {
        player?.play()
    }
    
    func pause() {
        player?.pause()
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        playerLayer?.frame = bounds
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        player?.pause()
        playerLayer?.removeFromSuperlayer()
    }
}

// MARK: - Video Background View

struct VideoBackgroundView: View {
    let track: Track
    @Binding var isPlaying: Bool
    
    var body: some View {
        ZStack {
            if track.trackType == .musicVideo,
               let videoURL = getBundleVideoURL(for: track) {
                VideoPlayerView(videoURL: videoURL, isPlaying: $isPlaying)
                    .clipped()
                
                // Dark overlay for better text readability
                Color.black.opacity(0.3)
            } else {
                // Static album artwork for audio-only tracks
                AsyncImage(url: Bundle.main.url(forResource: track.albumArtURL, withExtension: "png", subdirectory: "Resources/artwork")) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.gray.opacity(0.3)
                }
                .clipped()
            }
        }
    }
    
    private func getBundleVideoURL(for track: Track) -> URL? {
        guard let videoFileName = track.videoURL else { return nil }
        
        let fileExtension = "mp4"
        return Bundle.main.url(forResource: videoFileName, withExtension: fileExtension, subdirectory: "Resources/music-videos")
    }
} 