# Audio Files Setup Guide

## Where to Place Audio Files

Place all audio files in: **`app/src/main/res/raw/`**

## Required Audio Files

Create the following audio files (MP3, OGG, or WAV format recommended):

### Essential Files (Required):
1. **`tts_bike_coming.mp3`** (or .ogg/.wav)
   - Text: "A bike is coming"
   - Used for: Bells, chimes, bicycle bells (Level 3)

2. **`tts_ambulance_coming.mp3`** (or .ogg/.wav)
   - Text: "An ambulance is coming"
   - Used for: Sirens, emergency vehicles (Level 2)

### Optional Files (If you want more announcements):
3. **`tts_bus_arriving.mp3`**
   - Text: "A bus is arriving"
   - Used for: Bus sounds

4. **`tts_train_approaching.mp3`**
   - Text: "A train is approaching"
   - Used for: Train sounds

5. **`tts_dog_nearby.mp3`**
   - Text: "Dog nearby"
   - Used for: Dog barking

6. **`tts_footsteps_nearby.mp3`**
   - Text: "Footsteps nearby"
   - Used for: Footsteps

7. **`tts_door_closed.mp3`**
   - Text: "A door closed nearby"
   - Used for: Door sounds

8. **`tts_construction_nearby.mp3`**
   - Text: "Construction nearby"
   - Used for: Construction sounds

9. **`tts_traffic_nearby.mp3`**
   - Text: "Traffic nearby"
   - Used for: Traffic sounds

10. **`tts_be_aware.mp3`**
    - Text: "Be aware"
    - Used for: Default fallback

## Audio File Requirements

- **Format**: MP3, OGG, or WAV
- **Sample Rate**: 16kHz or 44.1kHz (both work)
- **Bitrate**: 64-128 kbps is sufficient
- **Duration**: Keep it short (2-4 seconds per message)
- **Volume**: Normalize all files to similar volume levels
- **Language**: English (or your preferred language)

## File Naming

**IMPORTANT**: The file names must match exactly:
- `tts_bike_coming.mp3`
- `tts_ambulance_coming.mp3`
- `tts_bus_arriving.mp3`
- etc.

## How to Generate Audio Files

You can use:
1. **Text-to-Speech online tools** (Google TTS, Amazon Polly, etc.)
2. **Record your own voice** using a voice recorder app
3. **AI voice generators** (ElevenLabs, Murf, etc.)
4. **Android TTS API** - Record the output

## Testing

After adding files, rebuild the app:
```bash
./gradlew assembleDebug
```

The app will automatically use these audio files instead of TTS.

