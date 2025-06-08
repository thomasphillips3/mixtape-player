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
- **Video & Image Artwork Support** - Individual track videos (MP4) or images (PNG/JPEG)
- **Smart fallback system** - Album art when track-specific artwork unavailable
- **Dynamic color theming** extracted from album artwork
- **Immersive full-screen Now Playing** with video backgrounds

### 🚗 **Android Auto Integration**
- **Seamless car connectivity** - Browse and play music in compatible vehicles
- **Professional presentation** - Clean interface optimized for driving
- **Complete metadata display** - Track info, album art, and controls

### 📱 **Cross-Platform Support**
- **Android** - Native Kotlin implementation
- **iOS** - SwiftUI with CarPlay support (documented)
- **Consistent experience** across all platforms

### 🎛️ **White-Label Customization**
- **client-config.json** - Easy branding and content updates
- **Automated build system** - Generate client apps efficiently
- **Professional documentation** - Ready for client delivery

---

## 🎬 **Video & Image Artwork**

### Supported Formats
- **Videos**: MP4 files for immersive track backgrounds
- **Images**: PNG, JPEG for traditional album art
- **Naming Convention**: `01_songname.mp4`, `02_artwork.png`, etc.
- **Fallback**: `album-art.png` or `album-art.jpeg` for tracks without specific artwork

### Features
- **Automatic detection** - App detects and uses appropriate artwork type
- **Video management** - Muted, looping background videos
- **Color extraction** - Dynamic theming from video thumbnails or images
- **Seamless switching** - Smooth transitions between video and image tracks

### Setup Process
1. Place artwork files in `assets/artwork/`
2. Use track number prefix: `01_`, `02_`, etc.
3. Run processing script: `python scripts/process-artwork.py`
4. Rebuild the Android app

---

## 🚀 **Quick Start**

### 1. **Audio Setup**
```bash
# Place high-quality audio files in assets/audio/
cp your-tracks/*.wav assets/audio/

# Generate catalog (automatically detects files)
python scripts/generate_bundled_catalog.py
```

### 2. **Artwork Setup**
```bash
# Place video/image artwork in assets/artwork/
cp track-videos/*.mp4 assets/artwork/
cp track-images/*.png assets/artwork/
cp album-art.png assets/artwork/

# Process artwork files
python scripts/process-artwork.py
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

# Test in Android Auto (with compatible vehicle or Android Auto simulator)
```

---

## 📂 **Project Structure**

```
mixtape-player/
├── assets/
│   ├── audio/           # High-quality audio files (WAV, FLAC)
│   └── artwork/         # Video/image artwork files
├── scripts/
│   ├── generate_bundled_catalog.py  # Audio catalog generation
│   └── process-artwork.py           # Artwork processing
├── client-config.json   # White-label customization
├── BUSINESS_GUIDE.md   # Business strategy & pricing
└── README.md           # This file
```

---

## 🔧 **Technical Architecture**

### **Android Components**
- **MusicService** - MediaSessionService for phone playback
- **AndroidAutoService** - MediaBrowserServiceCompat for Android Auto
- **LocalBundledSource** - Manages bundled audio and artwork files
- **NowPlayingFragment** - Video/image artwork with dynamic theming

### **Audio Pipeline**
- **ExoPlayer** - Professional audio playback engine
- **MediaSession** - Android Auto and notification integration
- **Bundled Resources** - No network dependency, instant loading

### **Artwork System**
- **Automatic Detection** - Scans for track-specific artwork files
- **Video Support** - VideoView with muted, looping playback
- **Color Extraction** - Palette API for dynamic theming
- **Resource Management** - Efficient memory and lifecycle handling

---

## 🎵 **Audio Specifications**

- **Supported Formats**: WAV, FLAC, MP3
- **Quality**: Up to 96kHz/24-bit (studio quality)
- **Metadata**: ID3 tags with professional credits
- **Playback**: Gapless, crossfade support

---

## 📊 **Business Benefits**

### **For Mixing Engineers**
- **New Revenue Stream**: $500-3000 per client app
- **Professional Branding**: Showcase your mixing work
- **Client Retention**: Ongoing app maintenance contracts
- **Portfolio Enhancement**: Tech-forward service offering

### **For Artists**
- **Direct Fan Connection**: No streaming platform intermediaries
- **Professional Presentation**: Custom-branded music experience
- **Complete Control**: Own their music distribution
- **Enhanced Engagement**: Video artwork, liner notes, behind-the-scenes content

---

## 🛠️ **Development**

### **Requirements**
- Android Studio Hedgehog+ (2023.1.1+)
- Kotlin 1.9.10+
- Gradle 8.2+
- Python 3.8+ (for build scripts)

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
- **Device Testing**: Real Android devices recommended
- **Android Auto**: Test with compatible vehicle or Android Auto simulator
- **Audio Quality**: Use high-quality headphones/speakers for testing

---

## 📱 **Platform Support**

| Platform | Status | Features |
|----------|--------|----------|
| **Android** | ✅ Complete | Video artwork, Android Auto, bundled audio |
| **iOS** | 📋 Documented | SwiftUI, CarPlay, AVAudioEngine implementation |
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
- Artists wanting direct fan engagement

---

## 📈 **Monetization Strategy**

### **Service Pricing**
- **Basic App**: $500-800 (single album, standard features)
- **Premium App**: $1000-2000 (multiple albums, video artwork, custom features)
- **Enterprise**: $2000-3000+ (label services, multiple artists, ongoing support)

### **Ongoing Revenue**
- **App Store Management**: $50-100/month
- **Updates & Maintenance**: $200-500/month
- **Analytics & Insights**: $100-300/month
- **Additional Features**: Custom pricing

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

**Ready to transform your mixing business? Start building premium music apps for your artist clients today.**
