#!/bin/bash

# Asset synchronization script for Mixtape Player
# Syncs assets from the unified assets/ directory to the Android app

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory (should be mixtape-player/scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo -e "${GREEN}Mixtape Player Asset Sync${NC}"
echo "Project root: $PROJECT_ROOT"

# Define paths
UNIFIED_ASSETS="$PROJECT_ROOT/assets"
ANDROID_ASSETS="$PROJECT_ROOT/app/src/main/assets"
GENERATED_VIDEOS="$PROJECT_ROOT/generated/music-videos"

# Check if unified assets directory exists
if [ ! -d "$UNIFIED_ASSETS" ]; then
    echo -e "${RED}Error: Unified assets directory not found at $UNIFIED_ASSETS${NC}"
    exit 1
fi

# Create Android assets directories if they don't exist
mkdir -p "$ANDROID_ASSETS/artwork"
mkdir -p "$ANDROID_ASSETS/audio"
mkdir -p "$ANDROID_ASSETS/music-videos"

echo -e "${YELLOW}Syncing artwork files...${NC}"
# Sync artwork (images and videos)
rsync -av --delete "$UNIFIED_ASSETS/artwork/" "$ANDROID_ASSETS/artwork/"

echo -e "${YELLOW}Syncing audio files...${NC}"
# Sync audio files
rsync -av --delete "$UNIFIED_ASSETS/audio/" "$ANDROID_ASSETS/audio/"

# Sync generated music videos if they exist
if [ -d "$GENERATED_VIDEOS" ]; then
    echo -e "${YELLOW}Syncing generated music videos...${NC}"
    rsync -av --delete "$GENERATED_VIDEOS/" "$ANDROID_ASSETS/music-videos/"
fi

echo -e "${GREEN}Asset sync completed successfully!${NC}"

# Show what was synced
echo -e "${YELLOW}Artwork files:${NC}"
ls -la "$ANDROID_ASSETS/artwork/" | grep -E '\.(png|jpg|jpeg|mp4|mov)$' || echo "No artwork files found"

echo -e "${YELLOW}Audio files:${NC}"
ls -la "$ANDROID_ASSETS/audio/" | grep -E '\.(wav|mp3|flac)$' || echo "No audio files found"

echo -e "${YELLOW}Music videos:${NC}"
ls -la "$ANDROID_ASSETS/music-videos/" | grep -E '\.mp4$' || echo "No music videos found"

echo -e "${GREEN}Remember to rebuild the app after syncing assets!${NC}" 