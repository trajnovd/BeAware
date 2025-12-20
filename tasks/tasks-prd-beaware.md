# Task List: BeAware MVP

**Generated from:** `prd-beaware.md`  
**Timeline:** 36-Hour Hackathon Build

---

## Relevant Files

### Project Root

- `settings.gradle.kts` - ✅ Project settings and module includes
- `build.gradle.kts` - ✅ Root build configuration
- `gradle.properties` - ✅ Gradle JVM and Android settings
- `gradle/libs.versions.toml` - ✅ Version catalog for dependencies
- `gradle/wrapper/gradle-wrapper.properties` - ✅ Gradle wrapper configuration
- `gradlew` - ✅ Gradle wrapper script (executable)
- `.gitignore` - ✅ Git ignore rules for Android project
- `README.md` - ✅ Project documentation with hackathon structure

### Core Application

- `app/build.gradle.kts` - ✅ Gradle dependencies (MediaPipe, TensorFlow Lite, Location Services)
- `app/src/main/AndroidManifest.xml` - ✅ Permissions and service declarations
- `app/src/main/java/com/beaware/app/BeAwareApplication.kt` - ✅ Application class for global initialization

### Audio Detection Engine

- `app/src/main/java/com/beaware/app/service/AudioClassifierService.kt` - Foreground service for continuous audio monitoring
- `app/src/main/java/com/beaware/app/audio/AudioRecorder.kt` - Microphone capture wrapper (16kHz)
- `app/src/main/java/com/beaware/app/audio/SoundClassifier.kt` - YAMNet/MediaPipe integration and inference
- `app/src/main/java/com/beaware/app/audio/UrgencyMapper.kt` - Maps YAMNet classes to Level 1/2/3
- `app/src/main/java/com/beaware/app/audio/CooldownManager.kt` - 5-second refractory period logic
- `app/src/main/assets/yamnet.tflite` - YAMNet TensorFlow Lite model file

### Alert Response System

- `app/src/main/java/com/beaware/app/alert/AlertManager.kt` - Central alert routing and orchestration
- `app/src/main/java/com/beaware/app/alert/AudioFocusController.kt` - AudioManager focus handling (pause/duck)
- `app/src/main/java/com/beaware/app/alert/HapticController.kt` - Vibration patterns (SOS, Pulse)
- `app/src/main/java/com/beaware/app/alert/PingSoundPlayer.kt` - Level 3 ping sound via SoundPool
- `app/src/main/res/raw/ping_alert.mp3` - Alert ping sound file

### User Interface

- `app/src/main/java/com/beaware/app/ui/MainActivity.kt` - Home screen with toggle and waveform
- `app/src/main/java/com/beaware/app/ui/SettingsActivity.kt` - Emergency contact configuration
- `app/src/main/java/com/beaware/app/ui/AlertOverlayActivity.kt` - Full-screen alert with countdown
- `app/src/main/java/com/beaware/app/ui/WaveformView.kt` - Custom view for audio visualization
- `app/src/main/res/layout/activity_main.xml` - Home screen layout
- `app/src/main/res/layout/activity_settings.xml` - Settings screen layout
- `app/src/main/res/layout/activity_alert_overlay.xml` - Alert overlay layout

### Emergency Features

- `app/src/main/java/com/beaware/app/emergency/LocationProvider.kt` - FusedLocationProviderClient wrapper
- `app/src/main/java/com/beaware/app/emergency/SmsSender.kt` - SMS sending with location link
- `app/src/main/java/com/beaware/app/emergency/AudioRecorderEmergency.kt` - Emergency audio recording to local storage
- `app/src/main/java/com/beaware/app/emergency/CountdownTimer.kt` - Countdown logic for Level 1/2 alerts

### Data & Preferences

- `app/src/main/java/com/beaware/app/data/PreferencesManager.kt` - ✅ SharedPreferences for emergency contact storage

### Resources

- `app/src/main/res/values/strings.xml` - ✅ String resources
- `app/src/main/res/values/colors.xml` - ✅ Color definitions (alert red, black, etc.)
- `app/src/main/res/values/themes.xml` - ✅ App themes (dark theme, alert overlay theme)
- `app/src/main/res/drawable/ic_launcher_foreground.xml` - ✅ App icon (shield with eye)
- `app/src/main/res/mipmap-hdpi/ic_launcher.xml` - ✅ Launcher icon
- `app/proguard-rules.pro` - ✅ ProGuard rules for MediaPipe

### Notes

- This is a Native Android (Kotlin) project targeting API 26+ (Android 8.0)
- All audio processing happens on-device; no network calls for classification
- Use `./gradlew assembleDebug` to build the debug APK
- Use `./gradlew installDebug` to install on connected device
- Test with real audio samples by playing sounds near the device microphone

---

## Tasks

- [ ] 1.0 Project Setup & Core Infrastructure _(6/7 sub-tasks complete - YAMNet model pending)_

  - [x] 1.1 Create new Android project in Android Studio with Kotlin, minimum SDK 26, package name `com.beaware.app`
  - [x] 1.2 Configure `build.gradle.kts` with dependencies: MediaPipe Tasks Audio (`com.google.mediapipe:tasks-audio`), Google Play Services Location
  - [x] 1.3 Add all required permissions to `AndroidManifest.xml`: RECORD_AUDIO, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, VIBRATE, SEND_SMS, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, SYSTEM_ALERT_WINDOW
  - [x] 1.4 Create package structure: `service/`, `audio/`, `alert/`, `ui/`, `emergency/`, `data/`
  - [ ] 1.5 Download YAMNet TFLite model from TensorFlow Hub and place in `app/src/main/assets/` _(placeholder created, model to be added manually)_
  - [x] 1.6 Create `BeAwareApplication.kt` application class and register in manifest
  - [x] 1.7 Create `PreferencesManager.kt` for storing emergency contact phone number using SharedPreferences

- [ ] 2.0 Audio Detection Engine (Foreground Service + YAMNet)

  - [ ] 2.1 Create `AudioClassifierService.kt` extending Service with `START_STICKY` return
  - [ ] 2.2 Implement `startForeground()` with persistent notification showing "BeAware: Protection Active 🛡️"
  - [ ] 2.3 Add "Stop Protection" action button to notification that calls `stopSelf()`
  - [ ] 2.4 Create `AudioRecorder.kt` using AudioRecord API with 16kHz sample rate, mono channel, 16-bit PCM
  - [ ] 2.5 Implement continuous audio capture loop that buffers ~1 second of audio for inference
  - [ ] 2.6 Create `SoundClassifier.kt` that initializes MediaPipe AudioClassifier with YAMNet model
  - [ ] 2.7 Implement `classify(audioData: ShortArray): List<Classification>` method that runs inference
  - [ ] 2.8 Create `UrgencyMapper.kt` with maps for Level 1 (Siren, Screech, Glass, Gunshot), Level 2 (Shout, Scream, Running, Fight), Level 3 (Horn, Bell)
  - [ ] 2.9 Implement `mapToUrgencyLevel(classifications: List<Classification>): UrgencyLevel?` method with 0.6 confidence threshold
  - [ ] 2.10 Create `CooldownManager.kt` that tracks last trigger time per sound category
  - [ ] 2.11 Implement `shouldTrigger(category: String): Boolean` that returns false if <5 seconds since last trigger

- [ ] 3.0 Alert Response System (AudioFocus, Haptics, Urgency Routing)

  - [ ] 3.1 Create `AlertManager.kt` singleton that receives urgency events from the service
  - [ ] 3.2 Create `AudioFocusController.kt` with methods `requestFocusPause()` (AUDIOFOCUS_GAIN_TRANSIENT) and `requestFocusDuck()` (AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
  - [ ] 3.3 Implement `abandonFocus()` method to release audio focus when alert is dismissed
  - [ ] 3.4 Create `HapticController.kt` using Vibrator API with VibrationEffect
  - [ ] 3.5 Implement `vibrateSOSPattern()` - 3 long pulses: `longArrayOf(0, 500, 200, 500, 200, 500)`
  - [ ] 3.6 Implement `vibratePulsePattern()` - repeated short pulses: `longArrayOf(0, 100, 100, 100, 100, 100, 100, 100)`
  - [ ] 3.7 Create `PingSoundPlayer.kt` using SoundPool to play the alert ping
  - [ ] 3.8 Add `ping_alert.mp3` (short high-frequency chime) to `res/raw/`
  - [ ] 3.9 Implement `playPing()` method that plays sound without interrupting current media
  - [ ] 3.10 In `AlertManager.kt`, implement routing logic: Level 1 → pause + SOS + overlay, Level 2 → duck + pulse + overlay, Level 3 → ping + toast
  - [ ] 3.11 Create broadcast mechanism (LocalBroadcastManager or EventBus) to communicate alerts from service to UI

- [ ] 4.0 User Interface (Home, Settings, Alert Overlay)

  - [ ] 4.1 Create `activity_main.xml` layout with: app title, waveform container, large circular toggle button, status text, settings button
  - [ ] 4.2 Implement `MainActivity.kt` with toggle button that starts/stops `AudioClassifierService`
  - [ ] 4.3 Create `WaveformView.kt` custom View that visualizes audio amplitude in real-time
  - [ ] 4.4 Implement waveform updates by receiving audio level broadcasts from the service
  - [ ] 4.5 Add permission request flow on first launch for RECORD_AUDIO, POST_NOTIFICATIONS, SMS, LOCATION
  - [ ] 4.6 Create `activity_settings.xml` layout with: phone number input field, save button, back navigation
  - [ ] 4.7 Implement `SettingsActivity.kt` that saves emergency contact to `PreferencesManager`
  - [ ] 4.8 Add phone number validation (basic format check)
  - [ ] 4.9 Create `activity_alert_overlay.xml` layout: black background, red "DANGER" header, sound type text, countdown timer, large "I'M SAFE" button (min 60dp height)
  - [ ] 4.10 Implement `AlertOverlayActivity.kt` with `Theme.Translucent` and `FLAG_SHOW_WHEN_LOCKED`
  - [ ] 4.11 Request `SYSTEM_ALERT_WINDOW` permission and use `Settings.canDrawOverlays()` check
  - [ ] 4.12 Implement countdown timer display (10 sec for Level 1, 30 sec for Level 2)
  - [ ] 4.13 Implement "I'm Safe" button click handler that cancels countdown and dismisses overlay
  - [ ] 4.14 Define color resources in `colors.xml`: alert_red (#FF0000), alert_black (#000000), safe_green (#00FF00)

- [ ] 5.0 Emergency Features (SMS, Location, Audio Recording, Dead Man's Switch)

  - [ ] 5.1 Create `LocationProvider.kt` using FusedLocationProviderClient
  - [ ] 5.2 Implement `getCurrentLocation(callback: (Location?) -> Unit)` with timeout fallback
  - [ ] 5.3 Generate Google Maps link format: `https://maps.google.com/?q={lat},{lng}`
  - [ ] 5.4 Create `SmsSender.kt` using SmsManager API
  - [ ] 5.5 Implement `sendEmergencySms(phoneNumber: String, soundType: String, location: Location?)` for Level 1
  - [ ] 5.6 Implement `sendDeadManSwitchSms(phoneNumber: String, location: Location?)` for Level 2 timeout
  - [ ] 5.7 Create `CountdownTimer.kt` using Android CountDownTimer
  - [ ] 5.8 Implement `startLevel1Countdown(onTick: (Int) -> Unit, onFinish: () -> Unit)` - 10 seconds
  - [ ] 5.9 Implement `startLevel2Countdown(onTick: (Int) -> Unit, onFinish: () -> Unit)` - 30 seconds
  - [ ] 5.10 Implement `cancel()` method to stop countdown when user taps "I'm Safe"
  - [ ] 5.11 Create `AudioRecorderEmergency.kt` for recording audio during Level 2 timeout
  - [ ] 5.12 Implement recording to local file in app's private storage directory
  - [ ] 5.13 In `AlertOverlayActivity.kt`, wire up Level 1 flow: countdown → fetch location → send SMS → dismiss
  - [ ] 5.14 In `AlertOverlayActivity.kt`, wire up Level 2 flow: countdown → fetch location → start recording → send SMS → show "Recording..." status
  - [ ] 5.15 Handle edge case: if SMS permission denied, show toast and skip SMS (still vibrate/alert)
  - [ ] 5.16 Handle edge case: if Location permission denied, send SMS without location link

- [ ] 6.0 Polish & Demo Preparation
  - [ ] 6.1 Test Level 1 detection with siren/screech audio samples played from another device
  - [ ] 6.2 Test Level 2 detection with shouting/scream audio samples
  - [ ] 6.3 Test Level 3 detection with car horn audio samples
  - [ ] 6.4 Verify SMS is received by emergency contact with correct message and location link
  - [ ] 6.5 Verify audio recording file is created during Level 2 timeout
  - [ ] 6.6 Test cooldown: trigger same sound twice within 5 seconds, verify second is ignored
  - [ ] 6.7 Test "I'm Safe" button cancels countdown correctly
  - [ ] 6.8 Test app behavior when screen is locked (foreground service should keep running)
  - [ ] 6.9 Tune YAMNet confidence threshold if too many false positives (try 0.7 or 0.8)
  - [ ] 6.10 Add app icon and splash screen (optional polish)
  - [ ] 6.11 Prepare demo script: show home screen → activate → play test sound → show alert → demonstrate SMS
  - [ ] 6.12 Final bug fixes and edge case handling
