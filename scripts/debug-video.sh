#!/bin/bash

# Debug Video Playback Script for Time Off 3
# This script monitors Android logs with video-specific filtering

echo "🎬 Video Debug Monitor for Time Off 3"
echo "================================="
echo ""
echo "📱 Clearing logcat buffer..."
adb logcat -c

echo "🔍 Starting video debug monitoring..."
echo "   🎬 = Video UI events"
echo "   📹 = Video URI extraction"
echo "   🔍 = Metadata analysis"
echo "   📦 = Bundle/source events"
echo "   📱 = MediaPlayer events"
echo ""
echo "Press Ctrl+C to stop monitoring"
echo ""

# Monitor logs with video-specific tags and emojis for easy identification
adb logcat -v time | grep -E "(NowPlayingFragment|LocalBundledSource|MusicService)" | grep -E "(🎬|📹|🔍|📦|📱|video|Video|MUSIC_VIDEO|track_type|video_path|VideoView|MediaPlayer|ERROR|Exception)" 