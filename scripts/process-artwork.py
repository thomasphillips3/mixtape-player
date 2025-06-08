#!/usr/bin/env python3

"""
Artwork Processing Script for Mixtape Player
"""

import os
import shutil
import re
from pathlib import Path

class ArtworkProcessor:
    def __init__(self):
        self.project_root = Path(__file__).parent.parent
        self.artwork_source = self.project_root / "assets" / "artwork"
        self.drawable_dest = self.project_root / "app" / "src" / "main" / "res" / "drawable"
        self.raw_dest = self.project_root / "app" / "src" / "main" / "res" / "raw"
        
    def process_all_artwork(self):
        """Process all artwork files from assets/artwork directory."""
        print("🎨 Processing artwork files...")
        
        if not self.artwork_source.exists():
            print(f"❌ Artwork directory not found: {self.artwork_source}")
            return False
            
        # Ensure destination directories exist
        self.drawable_dest.mkdir(parents=True, exist_ok=True)
        self.raw_dest.mkdir(parents=True, exist_ok=True)
        
        # Process fallback album art first
        self.process_fallback_album_art()
        
        # Process track-specific artwork
        artwork_files = list(self.artwork_source.iterdir())
        processed_count = 0
        
        for file_path in artwork_files:
            if file_path.is_file() and not file_path.name.startswith('.') and file_path.name != 'README.md':
                if self.process_artwork_file(file_path):
                    processed_count += 1
                    
        print(f"✅ Processed {processed_count} artwork files")
        return True
        
    def process_fallback_album_art(self):
        """Process fallback album art (album-art.png/jpg/jpeg)."""
        fallback_files = [
            self.artwork_source / "album-art.png",
            self.artwork_source / "album-art.jpg", 
            self.artwork_source / "album-art.jpeg"
        ]
        
        for fallback_file in fallback_files:
            if fallback_file.exists():
                dest_file = self.drawable_dest / "album_art_fallback.png"
                shutil.copy2(fallback_file, dest_file)
                print(f"📷 Processed fallback album art: {fallback_file.name}")
                return True
                
        print("⚠️  No fallback album art found (album-art.png/jpg/jpeg)")
        return False
        
    def process_artwork_file(self, file_path: Path):
        """Process individual artwork file."""
        filename = file_path.name
        track_number = self.extract_track_number(filename)
        
        if track_number == 0:
            return False  # Skip files that don't match track pattern
            
        file_ext = file_path.suffix.lower()
        
        if file_ext == '.mp4':
            return self.process_video_artwork(file_path, track_number)
        elif file_ext in ['.png', '.jpg', '.jpeg']:
            return self.process_image_artwork(file_path, track_number)
        else:
            print(f"⚠️  Skipping unsupported file: {filename}")
            return False
            
    def process_video_artwork(self, file_path: Path, track_number: int):
        """Process MP4 video artwork."""
        # Copy video to raw resources
        video_dest = self.raw_dest / f"track_{track_number:02d}_video.mp4"
        shutil.copy2(file_path, video_dest)
        
        # Create simple placeholder thumbnail for now
        thumbnail_dest = self.drawable_dest / f"track_{track_number:02d}_thumb.xml"
        self.create_simple_placeholder(thumbnail_dest)
        
        print(f"🎬 Processed video artwork for track {track_number}: {file_path.name}")
        return True
        
    def process_image_artwork(self, file_path: Path, track_number: int):
        """Process PNG/JPEG image artwork."""
        dest_file = self.drawable_dest / f"track_{track_number:02d}_art.png"
        shutil.copy2(file_path, dest_file)
            
        print(f"📷 Processed image artwork for track {track_number}: {file_path.name}")
        return True
        
    def create_simple_placeholder(self, output_path: Path):
        """Create a simple placeholder thumbnail."""
        # Copy the default album art as placeholder for now
        default_art = self.drawable_dest / "album_art.xml"
        if default_art.exists():
            shutil.copy2(default_art, output_path)
        
    def extract_track_number(self, filename: str) -> int:
        """Extract track number from filename (e.g., '01_title.mp4' -> 1)."""
        match = re.match(r'^(\d{1,2})_', filename)
        if match:
            return int(match.group(1))
        return 0

def main():
    processor = ArtworkProcessor()
    
    print("🎨 Mixtape Artwork Processor")
    print("=" * 40)
    
    # Process all artwork
    success = processor.process_all_artwork()
    
    if success:
        print("\n✅ Artwork processing completed!")
        print("📱 Rebuild the Android app to use the new artwork")
    else:
        print("\n❌ Artwork processing failed!")
        
    return 0 if success else 1

if __name__ == '__main__':
    exit(main())
