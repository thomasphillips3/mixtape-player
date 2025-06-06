# Mixtape Player - Professional Audio Delivery Template

A white-label music player template designed for **mixing engineers** to deliver professional, branded apps to their artist clients. Each app contains high-quality bundled audio files with lossless playback and in-vehicle integration.

**Cross-Platform Support**: Complete Android and iOS implementations with Android Auto and CarPlay integration.

## ✨ Features

### 🎵 **Music Experience**
- **Full-screen album art backgrounds** with dynamic color theming extracted from artwork
- **Gesture controls**: swipe for track navigation, tap to expand, swipe down to dismiss
- **Smart mini player** with auto-hide after 5 seconds of inactivity
- **Complete playback controls**: play, pause, skip, seek, shuffle, repeat modes
- **Remote music catalog** loaded from cloud sources with local fallback

### 🚗 **In-Vehicle Integration**

#### **Android Auto**
- **Dual service architecture**: 
  - MusicService (MediaSessionService) for phone app compatibility
  - AndroidAutoService (MediaBrowserServiceCompat) for Android Auto browsing
- **Full browsing capability** in Android Auto with searchable music library
- **Now Playing screen** with track metadata, album artwork, and progress tracking
- **Seamless synchronization** between phone app and in-vehicle display
- **Voice command support** through Google Assistant integration

#### **CarPlay (iOS)**
- **Native CarPlay integration** with CarPlaySceneDelegate
- **Browseable music library** organized by songs, artists, albums
- **Now Playing integration** with vehicle controls and display
- **Siri voice command support** for hands-free control
- **Tab-based interface** matching iOS design patterns

### 📱 **Phone Apps (Cross-Platform)**

#### **Android**
- **Material Design 3** with dynamic theming
- **Multiple viewing modes**: library browsing, now playing, mini player
- **Album art integration** throughout the interface
- **Real-time playback state** synchronized across all interfaces

#### **iOS**
- **SwiftUI implementation** with native iOS design patterns
- **Dynamic color theming** extracted from album artwork
- **Gesture controls**: swipe navigation, tap to expand, pull to dismiss
- **Background audio** with lock screen and Control Center integration
- **Apple Watch support** and Shortcuts app compatibility

## 🏗️ Architecture

### **Android Architecture**
```
┌─ Phone App ────────────────┐    ┌─ Android Auto ─────────────┐
│                            │    │                            │
│  MainActivity              │    │  Car Infotainment System   │
│  NowPlayingFragment        │    │  Browse + Now Playing UI   │
│  MiniPlayerFragment        │    │                            │
│                            │    │                            │
└────────────┬───────────────┘    └────────────┬───────────────┘
             │                                 │
             ▼                                 ▼
    ┌────────────────────┐            ┌─────────────────────┐
    │   MusicService     │            │ AndroidAutoService  │
    │ (MediaSessionService)          │ (MediaBrowserService) │
    │                    │            │                     │
    │ - ExoPlayer        │◄───────────┤ - Shared Player     │
    │ - MediaSession     │            │ - MediaSession Sync │
    │ - Notifications    │            │ - Browse Capability │
    └────────────────────┘            └─────────────────────┘
```

### **iOS Architecture**
```
┌─ iPhone App ───────────────┐    ┌─ CarPlay ──────────────────┐
│                            │    │                            │
│  ContentView (SwiftUI)     │    │  Car Dashboard Interface   │
│  NowPlayingView           │    │  Browse + Now Playing UI   │
│  MiniPlayerView           │    │                            │
│  MusicLibraryView         │    │                            │
│                            │    │                            │
└────────────┬───────────────┘    └────────────┬───────────────┘
             │                                 │
             ▼                                 ▼
    ┌────────────────────┐            ┌─────────────────────┐
    │   AudioManager     │            │ CarPlaySceneDelegate│
    │ (AVFoundation)     │            │ (CarPlay Framework) │
    │                    │            │                     │
    │ - AVAudioEngine    │◄───────────┤ - Shared Player     │
    │ - MPNowPlayingInfo │            │ - CPListTemplate    │
    │ - Remote Commands  │            │ - Browse Capability │
    └────────────────────┘            └─────────────────────┘
```

### **Key Components**

#### **Android**
- **MusicService**: Core playback service using Media3 ExoPlayer
- **AndroidAutoService**: Browser service for Android Auto UI
- **MusicServiceConnection**: Bridge between UI and services
- **LocalBundledSource**: Bundled audio file management
- **UampNotificationManager**: Rich media notifications

#### **iOS**
- **AudioManager**: Core audio service using AVFoundation
- **CarPlaySceneDelegate**: CarPlay integration with browseable interface
- **MusicCatalog**: Shared catalog management with network loading
- **ContentView**: Main SwiftUI navigation and app structure
- **NowPlayingView**: Full-screen player with dynamic theming

## 🚀 Getting Started

### **Prerequisites**

#### **Android Development**
- Android Studio Arctic Fox (2020.3.1) or later
- Android 6.0 (API level 23) or higher
- Android Auto compatible vehicle or Android Auto Desktop Head Unit for testing

#### **iOS Development (macOS only)**
- Xcode 14.0 or later
- iOS 15.0 or higher
- CarPlay compatible vehicle or CarPlay Simulator for testing
- Apple Developer account for device testing

### **Building the Apps**

#### **Android**
```bash
# Clone the repository
git clone <repository-url>
cd mixtape-player

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### **iOS**
```bash
# Navigate to iOS project
cd ios-mixtape

# Open in Xcode
open Mixtape.xcodeproj

# Or build from command line
xcodebuild -project Mixtape.xcodeproj -scheme Mixtape -destination 'platform=iOS Simulator,name=iPhone 15' build
```

### **Android Auto Testing**

#### **Option 1: Real Vehicle Testing**
1. Connect phone via USB to Android Auto compatible vehicle
2. Launch Mixtape app and start playing music
3. Access through vehicle's Android Auto interface

#### **Option 2: Desktop Head Unit (Development)**
```bash
# Install Android Auto Desktop Head Unit
# Available from: https://developer.android.com/training/cars/testing

# Enable Android Auto developer mode on phone
adb shell am start -n "com.google.android.projection.gearhead/.MainActivity"

# Launch Desktop Head Unit and connect phone
```

### **CarPlay Testing**

#### **Option 1: CarPlay Simulator (Development)**
```bash
# In Xcode, go to Window > Devices and Simulators
# Select "CarPlay Simulator" 
# Launch iOS app and test CarPlay interface
```

#### **Option 2: Real Vehicle Testing**
1. Connect iPhone via USB to CarPlay compatible vehicle
2. Launch Mixtape app and start playing music
3. Access through vehicle's CarPlay interface
4. Test browsing, Now Playing, and Siri commands

## 🛠️ Development

### **Monorepo Structure**
```
mixtape-player/
├── Android/
│   ├── app/                    # Main Android application module
│   ├── common/                 # Shared Android code and services  
│   ├── automotive/            # Android Automotive OS specific code
│   ├── gradle/                # Gradle wrapper and configuration
│   ├── build.gradle           # Android project build configuration
│   └── settings.gradle        # Android module settings
├── iOS/
│   └── ios-mixtape/           # Complete iOS Xcode project
│       ├── Mixtape/           # Main iOS app source code
│       │   ├── Views/         # SwiftUI views (NowPlaying, MiniPlayer, etc.)
│       │   ├── Services/      # AudioManager, CarPlaySceneDelegate
│       │   ├── Models/        # MusicCatalog, Track data models
│       │   └── ContentView.swift # Main app navigation
│       ├── Mixtape.xcodeproj  # Xcode project file
│       ├── Info.plist         # iOS app configuration
│       └── build.sh           # iOS build script
├── assets/
│   ├── audio/                 # High-quality source files (FLAC/WAV)
│   └── branding/             # Client logos, app icons, artwork
├── scripts/
│   └── build-client-app.py   # Automated white-label build script
├── client-builds/            # Output directory for client deliverables
├── client-config.json        # Client customization settings
└── docs/                     # Documentation and guides
```

### **Key Files**

#### **Android**
- `MusicService.kt` - Core music playback service
- `AndroidAutoService.kt` - Android Auto browsing integration
- `MusicServiceConnection.kt` - Service communication layer
- `LocalBundledSource.kt` - Bundled audio file management

#### **iOS**
- `AudioManager.swift` - Core audio service using AVFoundation
- `CarPlaySceneDelegate.swift` - CarPlay integration and interface
- `MusicCatalog.swift` - Music catalog and track management
- `ContentView.swift` - Main SwiftUI app navigation
- `NowPlayingView.swift` - Full-screen player interface

### **Testing Vehicle Integration**

#### **Android Auto Testing**
1. **Enable Developer Options** on Android device
2. **Install Android Auto** from Play Store
3. **Connect to Desktop Head Unit** or vehicle
4. **Test browsing and playback** functionality

#### **CarPlay Testing**
1. **Enable CarPlay** in iOS Settings > General > CarPlay
2. **Connect to CarPlay Simulator** in Xcode or compatible vehicle
3. **Test in CarPlay Simulator**:
   ```bash
   # Launch CarPlay Simulator from Xcode
   # Window > Devices and Simulators > CarPlay Simulator
   ```
4. **Test browsing and Now Playing** functionality

## 🎵 Music Catalog

The app loads music from a remote JSON catalog with fallback to local tracks:
- Dynamic loading from cloud sources
- Automatic retry with exponential backoff
- Local fallback catalog for offline testing
- Support for album artwork URLs

## 🔧 Configuration

### **Android Auto Setup**
The app includes proper Android Auto configuration:
- `automotive_app_desc.xml` - Android Auto app descriptor
- `allowed_media_browser_callers.xml` - Security for media browsing
- Manifest declarations for Android Auto support

### **CarPlay Setup (iOS)**
The iOS app includes proper CarPlay configuration:
- `Info.plist` - CarPlay capability declarations
- `CarPlaySceneDelegate.swift` - CarPlay scene management
- Audio session setup for vehicle integration
- MPNowPlayingInfoCenter integration

### **Media Session Integration**
- **Android**: Full Media3 MediaSession implementation
- **iOS**: MPNowPlayingInfoCenter and remote command handling
- Proper metadata handling for Now Playing display
- Synchronized playback state across all interfaces

## 📖 Documentation

- [Android Auto Integration Guide](docs/android-auto-integration.md)
- [Service Architecture](docs/service-architecture.md)  
- [Original UAMP Documentation](docs/FullGuide.md)

## 🐛 Known Issues

### **Android**
- Shuffle and repeat buttons may not appear in some Android Auto implementations
- Album art loading may be slow on poor network connections
- Some vehicles may have limited Android Auto UI capabilities

### **iOS**
- CarPlay may require specific iOS version compatibility
- Some vehicles may have limited CarPlay capabilities
- Background audio requires proper iOS permissions setup

## 🤝 Contributing

This project is based on Google's UAMP sample with significant enhancements for Android Auto integration. Contributions welcome!

## 📄 License

Based on Google's Universal Android Music Player sample:

```
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🎵 Music Credits

Music provided by the [Free Music Archive](http://freemusicarchive.org/):
- **The Kyoto Connection** - [Wake Up](http://freemusicarchive.org/music/The_Kyoto_Connection/Wake_Up_1957/)

Ambisonic recordings by [Ambisonic Sound Library](https://library.soundfield.com/).
