#!/usr/bin/env python3

"""
Music Video Creation Script for Mixtape Player
Combines video artwork with lossless audio to create complete music videos
"""

import os
import subprocess
import json
import re
from pathlib import Path
import wave

class MusicVideoCreator:
    def __init__(self):
        self.project_root = Path(__file__).parent.parent
        self.assets_dir = self.project_root / "assets"
        self.artwork_dir = self.assets_dir / "artwork"
        self.audio_dir = self.assets_dir / "audio"
        self.output_dir = self.project_root / "generated" / "music-videos"
        self.catalog_path = self.project_root / "app" / "src" / "main" / "assets" / "music" / "catalog.json"
        
        # Ensure output directory exists
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
    def get_audio_duration(self, audio_file: Path) -> float:
        """Get duration of audio file in seconds."""
        try:
            if audio_file.suffix.lower() == '.wav':
                with wave.open(str(audio_file), 'r') as wav_file:
                    frames = wav_file.getnframes()
                    sample_rate = wav_file.getframerate()
                    duration = frames / float(sample_rate)
                    return duration
            else:
                # Use FFmpeg for other formats
                result = subprocess.run([
                    'ffprobe', '-v', 'error', '-show_entries', 'format=duration',
                    '-of', 'default=noprint_wrappers=1:nokey=1', str(audio_file)
                ], capture_output=True, text=True, check=True)
                return float(result.stdout.strip())
        except Exception as e:
            print(f"❌ Error getting duration for {audio_file}: {e}")
            return 0.0
    
    def extract_track_number(self, filename: str) -> int:
        """Extract track number from filename."""
        match = re.match(r'^(\d{1,2})_', filename)
        if match:
            return int(match.group(1))
        return 0
    
    def find_audio_file(self, track_number: int) -> Path:
        """Find corresponding audio file for track number."""
        # Look for audio files that start with the track number
        for audio_file in self.audio_dir.glob('*'):
            if audio_file.is_file() and not audio_file.name.startswith('.'):
                audio_track_num = self.extract_track_number(audio_file.name)
                if audio_track_num == track_number:
                    return audio_file
        
        # Fallback: look for files starting with zero-padded track number
        track_prefix = f"{track_number:02d} "
        for audio_file in self.audio_dir.glob(f"{track_prefix}*"):
            if audio_file.is_file():
                return audio_file
                
        raise FileNotFoundError(f"No audio file found for track {track_number}")
    
    def create_music_video(self, video_file: Path, audio_file: Path, output_file: Path) -> bool:
        """Create a music video by combining video artwork with lossless audio."""
        try:
            print(f"🎬 Creating music video: {output_file.name}")
            print(f"   📼 Video: {video_file.name}")
            print(f"   🎵 Audio: {audio_file.name}")
            
            # Get audio duration
            audio_duration = self.get_audio_duration(audio_file)
            if audio_duration <= 0:
                print(f"❌ Invalid audio duration: {audio_duration}")
                return False
            
            print(f"   ⏱️  Duration: {audio_duration:.2f} seconds")
            
            # FFmpeg command to create lossless music video
            cmd = [
                'ffmpeg',
                '-y',  # Overwrite output file
                '-stream_loop', '-1',  # Loop video indefinitely
                '-i', str(video_file),  # Input video
                '-i', str(audio_file),  # Input audio
                '-t', str(audio_duration),  # Duration = audio length
                '-c:v', 'libx264',  # Video codec
                '-preset', 'medium',  # Encoding preset
                '-crf', '18',  # High quality video
                '-c:a', 'flac',  # LOSSLESS audio codec
                '-ar', '44100',  # Audio sample rate
                '-shortest',  # Stop when shortest stream ends
                '-avoid_negative_ts', 'make_zero',  # Fix timestamp issues
                str(output_file)
            ]
            
            print(f"   🔧 Running FFmpeg...")
            result = subprocess.run(cmd, capture_output=True, text=True, check=True)
            
            if output_file.exists():
                file_size = output_file.stat().st_size / (1024 * 1024)  # MB
                print(f"   ✅ Created: {output_file.name} ({file_size:.1f} MB)")
                return True
            else:
                print(f"   ❌ Output file not created")
                return False
                
        except subprocess.CalledProcessError as e:
            print(f"   ❌ FFmpeg error: {e}")
            print(f"   📝 FFmpeg stderr: {e.stderr}")
            return False
        except Exception as e:
            print(f"   ❌ Error creating music video: {e}")
            return False
    
    def update_catalog(self, video_tracks: dict):
        """Update catalog.json to reference music videos."""
        try:
            with open(self.catalog_path, 'r') as f:
                catalog = json.load(f)
            
            # Ensure Android assets music-videos directory exists
            android_videos_dir = self.project_root / "app" / "src" / "main" / "assets" / "music-videos"
            android_videos_dir.mkdir(parents=True, exist_ok=True)
            
            for track in catalog['tracks']:
                track_num = track.get('trackNumber', 0)
                if track_num in video_tracks:
                    video_file = video_tracks[track_num]
                    
                    # Copy music video to Android assets
                    android_video_file = android_videos_dir / video_file.name
                    subprocess.run(['cp', str(video_file), str(android_video_file)], check=True)
                    
                    # Update to use the generated music video
                    track['hasVideo'] = True
                    track['videoPath'] = f"generated/music-videos/{video_file.name}"
                    track['originalAudioUri'] = track.get('audioUri', '')
                    track['audioUri'] = f"file:///android_asset/music-videos/{video_file.name}"
                    print(f"   📝 Updated catalog for track {track_num}: {video_file.name}")
                    print(f"   📁 Copied to Android assets: {android_video_file}")
                else:
                    track['hasVideo'] = False
            
            # Write updated catalog
            with open(self.catalog_path, 'w') as f:
                json.dump(catalog, f, indent=2)
                
            print(f"✅ Updated catalog: {self.catalog_path}")
                
        except Exception as e:
            print(f"❌ Error updating catalog: {e}")
    
    def process_all_videos(self):
        """Process all video files and create music videos."""
        print("🎬 Music Video Creator")
        print("=" * 50)
        
        if not self.artwork_dir.exists():
            print(f"❌ Artwork directory not found: {self.artwork_dir}")
            return False
        
        if not self.audio_dir.exists():
            print(f"❌ Audio directory not found: {self.audio_dir}")
            return False
        
        # Check for FFmpeg
        try:
            subprocess.run(['ffmpeg', '-version'], capture_output=True, check=True)
            subprocess.run(['ffprobe', '-version'], capture_output=True, check=True)
        except (subprocess.CalledProcessError, FileNotFoundError):
            print("❌ FFmpeg not found. Please install FFmpeg:")
            print("   macOS: brew install ffmpeg")
            print("   Ubuntu: sudo apt install ffmpeg")
            print("   Windows: Download from https://ffmpeg.org/")
            return False
        
        # Find video files
        video_files = []
        for video_file in self.artwork_dir.glob('*.mp4'):
            if video_file.is_file():
                track_num = self.extract_track_number(video_file.name)
                if track_num > 0:
                    video_files.append((track_num, video_file))
        
        if not video_files:
            print("ℹ️  No video files found in artwork directory")
            return True
        
        print(f"🎥 Found {len(video_files)} video files")
        
        # Process each video file
        video_tracks = {}
        success_count = 0
        
        for track_num, video_file in sorted(video_files):
            try:
                # Find corresponding audio file
                audio_file = self.find_audio_file(track_num)
                
                # Create output filename
                output_filename = f"track_{track_num:02d}_video.mp4"
                output_file = self.output_dir / output_filename
                
                # Create music video
                if self.create_music_video(video_file, audio_file, output_file):
                    video_tracks[track_num] = output_file
                    success_count += 1
                else:
                    print(f"❌ Failed to create music video for track {track_num}")
                    
            except FileNotFoundError as e:
                print(f"⚠️  Skipping track {track_num}: {e}")
            except Exception as e:
                print(f"❌ Error processing track {track_num}: {e}")
        
        # Update catalog with video information
        if video_tracks:
            self.update_catalog(video_tracks)
        
        print(f"\n✅ Music video creation completed!")
        print(f"   📼 Created: {success_count} music videos")
        print(f"   📁 Output: {self.output_dir}")
        
        return success_count > 0

def main():
    creator = MusicVideoCreator()
    success = creator.process_all_videos()
    return 0 if success else 1

if __name__ == '__main__':
    exit(main()) 