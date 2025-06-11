//
//  MusicCatalog.swift
//  Mixtape
//
//  Music catalog management and Track models
//

import Foundation
import SwiftUI

enum TrackType: String, Codable {
    case audioOnly = "AUDIO_ONLY"
    case musicVideo = "MUSIC_VIDEO"
}

struct Track: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let artist: String
    let album: String
    let albumArtURL: String
    let audioURL: String
    let videoURL: String?
    let trackType: TrackType
    let duration: TimeInterval
    let bitDepth: String
    let sampleRate: String
    let audioFormat: String
    let mixingNotes: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case artist
        case album
        case albumArtURL = "image"
        case audioURL = "source"
        case videoURL = "video_source"
        case trackType = "track_type"
        case duration
        case bitDepth = "bit_depth"
        case sampleRate = "sample_rate"
        case audioFormat = "audio_format"
        case mixingNotes = "mixing_notes"
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle potential variations in JSON structure
        if let idValue = try? container.decode(String.self, forKey: .id) {
            id = idValue
        } else {
            // Generate ID from title if not provided
            let titleValue = try container.decode(String.self, forKey: .title)
            id = titleValue.lowercased().replacingOccurrences(of: " ", with: "_")
        }
        
        title = try container.decode(String.self, forKey: .title)
        artist = try container.decode(String.self, forKey: .artist)
        album = try container.decode(String.self, forKey: .album)
        albumArtURL = try container.decode(String.self, forKey: .albumArtURL)
        audioURL = try container.decode(String.self, forKey: .audioURL)
        videoURL = try? container.decode(String.self, forKey: .videoURL)
        
        // Handle track type
        if let typeString = try? container.decode(String.self, forKey: .trackType),
           let type = TrackType(rawValue: typeString) {
            trackType = type
        } else {
            trackType = videoURL != nil ? .musicVideo : .audioOnly
        }
        
        // Handle duration - might be string or number
        if let durationInt = try? container.decode(Int.self, forKey: .duration) {
            duration = TimeInterval(durationInt)
        } else if let durationString = try? container.decode(String.self, forKey: .duration) {
            duration = TimeInterval(Int(durationString) ?? 0)
        } else {
            duration = 0
        }
        
        bitDepth = (try? container.decode(String.self, forKey: .bitDepth)) ?? "16-bit"
        sampleRate = (try? container.decode(String.self, forKey: .sampleRate)) ?? "44.1kHz"
        audioFormat = (try? container.decode(String.self, forKey: .audioFormat)) ?? "WAV"
        mixingNotes = try? container.decode(String.self, forKey: .mixingNotes)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(title, forKey: .title)
        try container.encode(artist, forKey: .artist)
        try container.encode(album, forKey: .album)
        try container.encode(albumArtURL, forKey: .albumArtURL)
        try container.encode(audioURL, forKey: .audioURL)
        try container.encodeIfPresent(videoURL, forKey: .videoURL)
        try container.encode(trackType.rawValue, forKey: .trackType)
        try container.encode(Int(duration), forKey: .duration)
        try container.encode(bitDepth, forKey: .bitDepth)
        try container.encode(sampleRate, forKey: .sampleRate)
        try container.encode(audioFormat, forKey: .audioFormat)
        try container.encodeIfPresent(mixingNotes, forKey: .mixingNotes)
    }
    
    // Convenience initializer for creating tracks programmatically
    init(id: String, title: String, artist: String, album: String, 
         albumArtURL: String, audioURL: String, videoURL: String? = nil,
         trackType: TrackType = .audioOnly, duration: TimeInterval,
         bitDepth: String = "16-bit", sampleRate: String = "44.1kHz",
         audioFormat: String = "WAV", mixingNotes: String? = nil) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.albumArtURL = albumArtURL
        self.audioURL = audioURL
        self.videoURL = videoURL
        self.trackType = trackType
        self.duration = duration
        self.bitDepth = bitDepth
        self.sampleRate = sampleRate
        self.audioFormat = audioFormat
        self.mixingNotes = mixingNotes
    }
}

struct CatalogResponse: Codable {
    let music: [Track]
}

class MusicCatalog: ObservableObject {
    static let shared = MusicCatalog()
    
    @Published var tracks: [Track] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let catalogURL = "https://storage.googleapis.com/uamp/catalog.json"
    
    private init() {}
    
    func loadCatalog() async -> [Track] {
        await MainActor.run {
            isLoading = true
            errorMessage = nil
        }
        
        // Use bundled local tracks
        let tracks = bundledTracks()
        
        await MainActor.run {
            self.tracks = tracks
            self.isLoading = false
        }
        
        return tracks
    }
    
    private func bundledTracks() -> [Track] {
        // Local bundled tracks matching Android implementation
        return [
            Track(
                id: "track_01",
                title: "While It Counts",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "album_art_fallback",
                audioURL: "01 while it counts",
                duration: 139,
                mixingNotes: "Mixed by Thomas Phillips"
            ),
            Track(
                id: "track_02",
                title: "My Kinda Crazy",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "track_02_art",
                audioURL: "02 my kinda crazy",
                duration: 142,
                mixingNotes: "Mixed by Thomas Phillips"
            ),
            Track(
                id: "track_03",
                title: "Away",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "track_03_art",
                audioURL: "03 away",
                videoURL: "track_03_video",
                trackType: .musicVideo,
                duration: 100,
                mixingNotes: "Mixed by Thomas Phillips"
            ),
            Track(
                id: "track_04",
                title: "Take U Down",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "album_art_fallback",
                audioURL: "04 take u down",
                duration: 151,
                mixingNotes: "Mixed by Thomas Phillips"
            ),
            Track(
                id: "track_05",
                title: "Under The Spell",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "track_05_art",
                audioURL: "05 underthespelll",
                duration: 114,
                mixingNotes: "Mixed by Thomas Phillips"
            ),
            Track(
                id: "track_06",
                title: "Ghosting",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "track_06_art",
                audioURL: "06 ghosting",
                duration: 104,
                mixingNotes: "Mixed by Thomas Phillips"
            ),
            Track(
                id: "track_07",
                title: "What We Imagined",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "track_07_art",
                audioURL: "07 what we imagined",
                videoURL: "track_07_video",
                trackType: .musicVideo,
                duration: 95,
                mixingNotes: "Mixed by Thomas Phillips"
            ),
            Track(
                id: "track_08",
                title: "Any Other Day",
                artist: "Thomas Phillips",
                album: "Mixtape Demo",
                albumArtURL: "album_art_fallback",
                audioURL: "08 any other day",
                duration: 175,
                mixingNotes: "Mixed by Thomas Phillips"
            )
        ]
    }
    
    // MARK: - Search and Filter
    
    func searchTracks(_ query: String) -> [Track] {
        if query.isEmpty {
            return tracks
        }
        
        return tracks.filter { track in
            track.title.localizedCaseInsensitiveContains(query) ||
            track.artist.localizedCaseInsensitiveContains(query) ||
            track.album.localizedCaseInsensitiveContains(query)
        }
    }
    
    func tracksByArtist(_ artist: String) -> [Track] {
        return tracks.filter { $0.artist == artist }
    }
    
    func tracksByAlbum(_ album: String) -> [Track] {
        return tracks.filter { $0.album == album }
    }
    
    var uniqueArtists: [String] {
        return Array(Set(tracks.map { $0.artist })).sorted()
    }
    
    var uniqueAlbums: [String] {
        return Array(Set(tracks.map { $0.album })).sorted()
    }
}

// MARK: - Extensions

extension Track {
    var formattedDuration: String {
        let minutes = Int(duration) / 60
        let seconds = Int(duration) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

// MARK: - Sample Data for Previews

extension Track {
    static let sample = Track(
        id: "sample",
        title: "Sample Track",
        artist: "Thomas Phillips",
        album: "Mixtape Demo",
        albumArtURL: "album_art_fallback",
        audioURL: "01 while it counts",
        duration: 180,
        mixingNotes: "Mixed by Thomas Phillips"
    )
    
    static let samplePlaylist = [
        Track(
            id: "track1",
            title: "While It Counts",
            artist: "Thomas Phillips",
            album: "Mixtape Demo",
            albumArtURL: "album_art_fallback",
            audioURL: "01 while it counts",
            duration: 139,
            mixingNotes: "Mixed by Thomas Phillips"
        ),
        Track(
            id: "track2",
            title: "Away",
            artist: "Thomas Phillips",
            album: "Mixtape Demo",
            albumArtURL: "track_03_art",
            audioURL: "03 away",
            videoURL: "track_03_video",
            trackType: .musicVideo,
            duration: 100,
            mixingNotes: "Mixed by Thomas Phillips"
        )
    ]
} 