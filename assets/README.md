# Mixtape Player - Unified Assets

This directory contains all media assets for the Mixtape Player monorepo, shared between Android and iOS applications.

## Directory Structure

```
assets/
├── audio/           # Audio files (.wav, .mp3, .flac)
├── artwork/         # Visual assets (images and videos)
│   ├── *.png        # Album/track artwork images
│   ├── *.jpg        # Album artwork
│   └── *.mp4        # Video artwork (Canvas-style)
└── branding/        # App branding assets
```

## Asset Naming Convention

### Audio Files
- Format: `[track_number] [track_name].wav`
- Example: `01 while it counts.wav`

### Artwork Files
- **Images**: `[track_number].png` or `[track_number].jpg`
- **Videos**: `[track_number].mp4` (for Spotify Canvas-style video backgrounds)
- **Album Art**: `album-art.jpg` (fallback artwork)

Examples:
- `01.png` - Image artwork for track 1
- `03.mp4` - Video artwork for track 3 (Canvas-style)
- `07.mp4` - Video artwork for track 7 (Canvas-style)

## Video Artwork (Canvas Feature)

Video files in the artwork directory enable Spotify Canvas-style full-screen video backgrounds that play while the audio track is playing. 

### Requirements:
- Format: MP4
- Resolution: 1080x1920 (portrait) or 1920x1080 (landscape)
- Duration: Should loop seamlessly
- Audio: Will be muted (audio from separate audio file is used)

### Usage:
1. Place video file with track number: `03.mp4`
2. Ensure corresponding audio file exists: `03 away.wav`
3. Video will auto-loop as background while audio plays

## Syncing to Platform Apps

### Android
Use the sync script to copy assets to the Android app:

```bash
./scripts/sync-assets.sh
```

This copies assets to `app/src/main/assets/` and updates the Android app.

### iOS (Future)
Assets will be synced to the iOS app bundle during build process.

## Catalog Integration

The `app/src/main/assets/music/catalog.json` file references these assets using URIs:

- **Audio**: `file:///android_asset/audio/[filename]`
- **Artwork**: `file:///android_asset/artwork/[filename]`
- **Video**: `file:///android_asset/artwork/[filename].mp4`

## Adding New Tracks

1. Add audio file to `assets/audio/`
2. Add artwork to `assets/artwork/` (optional video for Canvas effect)
3. Update `catalog.json` with new track entry
4. Run `./scripts/sync-assets.sh` to sync to Android
5. Rebuild the app

## File Size Considerations

- **Audio**: High quality WAV files (44.1kHz/16-bit recommended)
- **Images**: Optimized PNG/JPG (< 5MB per image)
- **Videos**: Compressed MP4 (< 10MB per video for mobile distribution)

## Supported Formats

- **Audio**: WAV, MP3, FLAC
- **Images**: PNG, JPG, JPEG
- **Video**: MP4 (H.264 codec recommended) 