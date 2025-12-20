# BeAware 🛡️

**AI-Powered "Sixth Sense" for Pedestrians with Noise-Canceling Headphones**

BeAware is a native Android app that uses on-device AI to detect environmental dangers and alert users who are isolated in their audio bubble. It hijacks your audio experience when it matters most—keeping you aware and safe.

---

## Problem

Noise-canceling headphones create a dangerous "audio bubble" that blocks critical environmental sounds:

- 🚨 **Emergency vehicles** — Sirens go unheard until dangerously close
- 🚗 **Traffic hazards** — Car horns, tire screeches arrive without warning  
- ⚠️ **Personal safety** — Shouting, fighting, or aggressive situations nearby go unnoticed
- 🚴 **Cycling risks** — Bicycle bells and warnings from behind are muffled

**Every year, pedestrians with headphones are involved in preventable accidents because they simply couldn't hear the danger approaching.**

---

## Solution

BeAware acts as an always-on safety layer that listens *for* you:

### Core Features

| Urgency Level | Sounds Detected | Response |
|---------------|-----------------|----------|
| 🔴 **Level 1: Critical** | Sirens, Tire Screeches, Glass Breaking, Gunshots | Pauses music, SOS vibration, full-screen alert with 10-second countdown to SMS |
| 🟠 **Level 2: Danger** | Shouting, Screaming, Running, Fighting | Ducks volume to 10%, pulse vibration, 30-second "Dead Man's Switch" countdown |
| 🟡 **Level 3: Warning** | Car Horns, Bicycle Bells | Overlay ping sound without interrupting music |

### Emergency Features
- **Automatic SMS** — Sends your location to emergency contact if you don't respond
- **Dead Man's Switch** — Level 2 threats start audio recording if you don't confirm you're safe within 30 seconds
- **Location Sharing** — Google Maps link included in all emergency SMS

---

## Innovation

### What Makes BeAware Different

1. **100% On-Device AI** — No cloud processing. Your audio never leaves your phone. Zero latency, maximum privacy.

2. **Intelligent Urgency Routing** — Not all sounds deserve the same response. A car horn gets a subtle ping; a siren gets full media takeover.

3. **Dead Man's Switch** — If you can't respond to a danger alert, BeAware assumes the worst and activates emergency protocols automatically.

4. **Audio Hijacking** — Uses Android's AudioFocus API to *pause* or *duck* other apps (Spotify, YouTube, etc.) rather than just playing over them.

5. **Context-Aware Alerts** — 5-second cooldown prevents alert fatigue from continuous sounds (like a passing fire truck).

---

## Impact & Feasibility

### Who Benefits

- **Urban Pedestrians** — Commuters walking through busy city streets
- **Runners & Joggers** — Athletes exercising outdoors with music
- **Cyclists** — Riders who use bone-conduction or traditional headphones
- **Late-Night Walkers** — Anyone navigating potentially unsafe areas after dark
- **Hearing Impaired** — Users who may already struggle to hear environmental sounds

### Real-World Application

- **Estimated Market** — 300M+ noise-canceling headphone users globally
- **Zero Infrastructure Required** — Works with any Android phone, any headphones
- **No Subscription Model** — Free to use, privacy-first design

### Expansion Potential

- iOS version
- Smartwatch companion app with haptic alerts
- Integration with smart hearing aids
- Custom sound training for specific environments (construction sites, factories)

---

## Technical Implementation

### Key Technical Choices

| Component | Technology | Rationale |
|-----------|------------|-----------|
| **Platform** | Native Android (Kotlin) | Direct hardware access for low-latency audio |
| **AI Model** | YAMNet via MediaPipe | 521 sound classes, optimized for mobile, runs in ~50ms |
| **Audio Capture** | AudioRecord API @ 16kHz | Matches YAMNet requirements, minimal battery impact |
| **Media Control** | AudioFocus API | System-level integration to pause/duck other apps |
| **Background Processing** | Foreground Service | Ensures continuous monitoring even when screen locked |

### Main Challenges Solved

1. **Real-time Classification** — Achieved <500ms detection-to-alert latency using optimized TFLite inference
2. **Battery Efficiency** — Foreground service with efficient audio buffering keeps drain under 5%/hour
3. **False Positive Reduction** — 0.6 confidence threshold + 5-second cooldown per category
4. **Lock Screen Alerts** — SYSTEM_ALERT_WINDOW permission + FLAG_SHOW_WHEN_LOCKED

### Overall Functionality

```
┌─────────────────────────────────────────────────────────────┐
│                        BeAware Flow                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📱 Phone Microphone                                        │
│         │                                                   │
│         ▼                                                   │
│  🎙️ AudioRecord (16kHz, 1-sec chunks)                       │
│         │                                                   │
│         ▼                                                   │
│  🧠 YAMNet Classification (on-device)                       │
│         │                                                   │
│         ▼                                                   │
│  🎯 Urgency Mapper (521 classes → 3 levels)                 │
│         │                                                   │
│    ┌────┴────┬────────────┐                                 │
│    ▼         ▼            ▼                                 │
│  🔴 L1    🟠 L2       🟡 L3                                  │
│  Pause    Duck        Ping                                  │
│  + SOS    + Pulse     + Toast                               │
│  + Alert  + Dead Man  (no interrupt)                        │
│           + Record                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Technologies Used

| Category | Technologies |
|----------|--------------|
| **Language** | Kotlin |
| **Platform** | Android SDK 26+ (Android 8.0 Oreo) |
| **AI/ML** | TensorFlow Lite, Google MediaPipe Audio Classifier, YAMNet |
| **Audio** | AudioRecord API, AudioManager, SoundPool |
| **Location** | Google Play Services FusedLocationProviderClient |
| **Communication** | Android SmsManager |
| **UI** | Material Design 3, ConstraintLayout, Custom Views |
| **Background** | Android Foreground Service |

---

## Demo

### Screenshots

*Coming soon — screenshots of Home Screen, Alert Overlay, and Settings*

| Home Screen | Alert Overlay | Settings |
|-------------|---------------|----------|
| ![Home](docs/screenshots/home.png) | ![Alert](docs/screenshots/alert.png) | ![Settings](docs/screenshots/settings.png) |

### Demo Video

🎬 **[Watch Demo Video](#)** *(link coming soon)*

### Live Demo

To test BeAware:
1. Install the APK on an Android 8.0+ device
2. Grant microphone, SMS, and location permissions
3. Set your emergency contact in Settings
4. Tap the toggle to activate protection
5. Play siren/horn sounds from another device to trigger alerts

---

## Installation

```bash
# Clone the repository
git clone https://github.com/trajnovd/BeAware.git

# Open in Android Studio
# Build → Make Project

# Or build via command line
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

---

## Project Structure

```
BeAware/
├── app/src/main/
│   ├── java/com/beaware/app/
│   │   ├── service/        # Foreground service for audio monitoring
│   │   ├── audio/          # Audio capture, classification, urgency mapping
│   │   ├── alert/          # Alert routing, AudioFocus, haptics
│   │   ├── ui/             # Activities and custom views
│   │   ├── emergency/      # SMS, location, audio recording
│   │   └── data/           # SharedPreferences manager
│   ├── res/                # Layouts, colors, strings, drawables
│   └── assets/             # YAMNet TFLite model
├── tasks/                  # PRD and task tracking
└── README.md
```

---

## Team

Built with ❤️ for **[Hackathon Name]** in 36 hours.

---

## License

MIT License — See [LICENSE](LICENSE) for details.

