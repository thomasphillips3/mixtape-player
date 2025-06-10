# Mixtape Player - Professional White-Label Music App Template

**Transform your mixing engineering business with premium apps for artists**

*A production-ready template for mixing engineers to deliver professional music apps to their artist clients.*

---

## 🎯 **Business Model**

This template enables mixing engineers to offer **premium app delivery services** to artists:

- **Premium Pricing**: $500-3000 per custom app
- **Recurring Revenue**: App store maintenance, updates, analytics
- **Value Proposition**: Professional presentation, direct artist-fan connection
- **Target Market**: Independent artists, record labels, music producers

For detailed business strategy, see [BUSINESS_GUIDE.md](BUSINESS_GUIDE.md)

---

## ✨ **Key Features**

### 🎵 **Audio Excellence**
- **High-quality bundled audio** (FLAC, WAV support)
- **Professional metadata** with mixing engineer credits
- **Gapless playback** for seamless listening experience

### 🎨 **Dynamic Visual Experience**
- **Music Video Support** - Individual track videos (MP4) with ExoPlayer
- **Image Artwork Support** - PNG/JPEG album art with smart fallback
- **Dynamic color theming** extracted from album artwork
- **Immersive full-screen Now Playing** with looping video backgrounds
- **Seamless transitions** between video and audio-only tracks

### 🚗 **Android Auto Integration**
- **Seamless car connectivity** - Browse and play music in compatible vehicles
- **Professional presentation** - Clean interface optimized for driving
- **Complete metadata display** - Track info, album art, and controls

### 📱 **Cross-Platform Support**
- **Android** - Native Kotlin implementation with ExoPlayer
- **iOS** - SwiftUI with CarPlay support (documented)
- **Consistent experience** across all platforms

### 🎛️ **White-Label Customization**
- **client-config.json** - Easy branding and content updates
- **Automated build system** - Generate client apps efficiently
- **Professional documentation** - Ready for client delivery

---

## 🎬 **Music Video Implementation**

### Technical Architecture
- **ExoPlayer Integration** - Professional video playback engine
- **Asset-based Videos** - Full video files stored in app assets
- **Background Playback** - Muted, looping video artwork
- **Automatic Fallback** - Smooth transitions to image artwork when needed

### Supported Formats
- **Videos**: MP4 files for immersive track backgrounds
- **Images**: PNG, JPEG for traditional album art
- **Naming Convention**: `track_03_video.mp4`, `track_07_video.mp4`
- **Fallback**: Individual track artwork or `album-art.png`

### Features
- **Automatic detection** - App detects and uses appropriate artwork type
- **Professional quality** - Full-length videos with lossless audio
- **Color extraction** - Dynamic theming from video thumbnails
- **Memory management** - Efficient video lifecycle handling

---

## 🚀 **Quick Start**

### 1. **Audio Setup**
```bash
# Place high-quality audio files in assets/audio/
cp your-tracks/*.wav assets/audio/

# Generate catalog (automatically detects files)
python scripts/generate_bundled_catalog.py
```

### 2. **Music Video Setup**
```bash
# Place music videos in assets/music-videos/
cp track-videos/*.mp4 app/src/main/assets/music-videos/

# Update catalog.json to mark video tracks
# Set "hasVideo": true and "videoPath": "generated/music-videos/filename.mp4"
```

### 3. **Client Customization**
```json
// Edit client-config.json
{
  "artistName": "Artist Name",
  "appName": "Artist's Mixtape",
  "primaryColor": "#1DB954",
  "description": "Official music app"
}
```

### 4. **Build & Test**
```bash
# Android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Test video playback on tracks with "hasVideo": true
```

---

## 📂 **Project Structure**

```
mixtape-player/
├── app/src/main/assets/
│   ├── audio/           # High-quality audio files (WAV, FLAC)
│   ├── music-videos/    # MP4 music video files
│   └── music/           # Catalog configuration
├── scripts/
│   └── generate_bundled_catalog.py  # Audio catalog generation
├── client-config.json   # White-label customization
├── BUSINESS_GUIDE.md   # Business strategy & pricing
└── README.md           # This file
```

---

## 🔧 **Technical Architecture**

### **Android Components**
- **MusicService** - MediaSessionService for audio playback
- **AndroidAutoService** - MediaBrowserServiceCompat for Android Auto
- **LocalBundledSource** - Manages bundled audio and video metadata
- **NowPlayingFragment** - ExoPlayer video integration with dynamic theming

### **Audio Pipeline**
- **ExoPlayer** - Professional audio and video playback engine
- **MediaSession** - Android Auto and notification integration
- **Bundled Resources** - No network dependency, instant loading

### **Video System**
- **ExoPlayer PlayerView** - Hardware-accelerated video playback
- **Asset Integration** - Direct access to bundled video files
- **Background Mode** - Muted, looping video artwork
- **Lifecycle Management** - Proper cleanup and resource handling

---

## 🎵 **Audio Specifications**

- **Supported Formats**: WAV, FLAC, MP3
- **Quality**: Up to 96kHz/24-bit (studio quality)
- **Metadata**: ID3 tags with professional credits
- **Playback**: Gapless, crossfade support

---

## 🎬 **Video Specifications**

- **Supported Formats**: MP4 with H.264 video codec
- **Resolution**: Up to 720x1280 (portrait) optimized for mobile
- **Audio**: Muted during video playback (main audio track plays separately)
- **Performance**: Hardware-accelerated decoding, smooth looping

---

## 📊 **Business Benefits**

### **For Mixing Engineers**
- **New Revenue Stream**: $500-3000 per client app
- **Professional Branding**: Showcase your mixing work with video
- **Client Retention**: Ongoing app maintenance contracts
- **Portfolio Enhancement**: Tech-forward service offering

### **For Artists**
- **Direct Fan Connection**: No streaming platform intermediaries
- **Professional Presentation**: Custom-branded music experience with video
- **Complete Control**: Own their music distribution
- **Enhanced Engagement**: Full music videos, liner notes, behind-the-scenes content

---

## 🛠️ **Development**

### **Requirements**
- Android Studio Hedgehog+ (2023.1.1+)
- Kotlin 1.9.10+
- Gradle 8.2+
- ExoPlayer 1.2.1+ (included)

### **Build Process**
```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build (for client delivery)
./gradlew assembleRelease
```

### **Testing**
- **Device Testing**: Real Android devices recommended for video performance
- **Android Auto**: Test with compatible vehicle or Android Auto simulator
- **Video Quality**: Test on various screen sizes and orientations

---

## 📱 **Platform Support**

| Platform | Status | Features |
|----------|--------|----------|
| **Android** | ✅ Complete | ExoPlayer videos, Android Auto, bundled audio |
| **iOS** | 📋 Documented | SwiftUI, CarPlay, AVPlayer implementation |
| **Cross-Platform** | ✅ Ready | Shared business logic, consistent UX |

---

## 🎯 **Target Audience**

### **Primary: Mixing Engineers**
- Independent audio engineers
- Studio owners looking to expand services
- Producers wanting to offer premium delivery
- Engineers working with independent artists

### **Secondary: Artists & Labels**
- Independent musicians
- Small record labels
- Producers releasing compilation albums
- Artists wanting direct fan engagement with video content

---

## 📈 **Monetization Strategy**

### **Service Pricing**
- **Basic App**: $500-800 (single album, standard features)
- **Premium App**: $1000-2000 (multiple albums, music videos, custom features)
- **Enterprise**: $2000-3000+ (label services, multiple artists, ongoing support)

### **Ongoing Revenue**
- **App Store Management**: $50-100/month
- **Updates & Maintenance**: $200-500/month
- **Analytics & Insights**: $100-300/month
- **Video Production**: $200-800 per track (if offering video creation services)

---

## 🚗 **Android Auto Integration**

### **Features**
- **Browse Library**: Full music catalog browsing in vehicle
- **Now Playing**: Track info, album art, and playback controls
- **Voice Control**: "Play [artist]" and "Play [track]" commands
- **Safe Driving**: Distraction-optimized interface

### **Testing**
- **Android Auto Simulator**: Available in Android Studio
- **Vehicle Testing**: Test in compatible cars (2017+ Mercedes, Ford, etc.)
- **DHU (Desktop Head Unit)**: Command-line testing tool

---

## 📄 **License**

Apache License 2.0 - See [LICENSE](LICENSE) for details.

---

## 🤝 **Support**

For business inquiries and technical support:
- **Documentation**: Comprehensive setup guides included
- **Business Model**: See [BUSINESS_GUIDE.md](BUSINESS_GUIDE.md)
- **Technical Issues**: Check existing documentation and code comments

---

**Ready to transform your mixing business? Start building premium music apps with professional video integration for your artist clients today.**
