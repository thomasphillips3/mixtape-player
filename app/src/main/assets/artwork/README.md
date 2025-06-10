# Mixtape Player - Artwork System

## Simple Asset-Based Artwork

This system automatically detects artwork files based on track numbers. Simply place your artwork files in this directory using the following naming convention:

### Naming Convention
- Use track numbers as filenames: `01`, `02`, `03`, etc.
- Supported image formats: `.png`, `.jpg`, `.jpeg`, `.webp`
- Supported video formats: `.mp4`, `.mov`, `.avi`

### Examples
```
01.png          # Track 1 - Image artwork
02.mp4          # Track 2 - Video artwork  
03.jpg          # Track 3 - Image artwork
04.mp4          # Track 4 - Video artwork
05.jpeg         # Track 5 - Image artwork
```

### Fallback Album Art
- `album-art.jpg` (or `.png`) - Used for tracks without specific artwork
- Automatically detected and used as fallback

### Features
- **Automatic Detection**: No need to update catalog.json
- **Mixed Media**: Combine images and videos in the same album
- **Flexible Formats**: Support for multiple image and video formats
- **Fallback System**: Graceful degradation to album art
- **Video Features**: Auto-looping, muted background videos

### How It Works
1. App scans `assets/artwork/` directory at startup
2. Detects file types by extension
3. Maps files to track numbers automatically  
4. Videos are copied to app resources for optimized playback
5. No configuration required!

### Migration from Old System
Simply rename your artwork files to use track numbers and place them here. Remove any `artworkType` fields from `catalog.json` - they're no longer needed!
