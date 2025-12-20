# Product Requirements Document: BeAware

**Version:** 1.0 (MVP)  
**Date:** December 20, 2025  
**Author:** Product Owner (AI-Assisted)  
**Timeline:** 36-Hour Hackathon Build  

---

## 1. Introduction / Overview

**BeAware** is an AI-powered "Sixth Sense" for pedestrians wearing noise-canceling headphones. The app continuously monitors environmental audio via the smartphone's microphone and uses on-device AI to identify high-urgency dangers (sirens, screeches, shouting, etc.). When a threat is detected, it "hijacks" the user's audio experience—pausing music, triggering haptic feedback, and displaying alerts—to break through the audio isolation and warn the user of potential danger.

### The Problem
Noise-canceling headphones create a dangerous "audio bubble" that blocks critical environmental sounds like approaching vehicles, emergency sirens, or confrontational situations. Users are unaware of threats until it's too late.

### The Solution
BeAware acts as an always-on safety layer that listens *for* the user, intelligently filtering ambient noise and only interrupting when a genuine threat is detected.

---

## 2. Goals

| Goal | Success Criteria |
|------|------------------|
| **G1: Real-time Threat Detection** | Detect and classify Level 1/2/3 sounds within 500ms of occurrence |
| **G2: Immediate User Alert** | Interrupt audio playback and deliver haptic/visual alerts within 1 second of detection |
| **G3: Privacy-First Architecture** | 100% on-device processing; zero audio data leaves the phone |
| **G4: Battery Efficiency** | Foreground service runs continuously without draining >5% battery per hour |
| **G5: Demo-Ready MVP** | Fully functional demo with all 3 urgency levels working within 36 hours |

---

## 3. User Stories

### Primary User: Urban Pedestrian with Noise-Canceling Headphones

| ID | User Story | Priority |
|----|------------|----------|
| US-1 | As a pedestrian, I want the app to automatically pause my music when an emergency vehicle approaches, so I can hear and react to it. | **Must Have** |
| US-2 | As a user, I want to feel a strong vibration on my phone/watch when a critical danger is detected, so I'm alerted even if I'm not looking at my screen. | **Must Have** |
| US-3 | As a user walking at night, I want the app to detect sounds of a scuffle or aggressive shouting nearby, so I can be aware of potential danger. | **Must Have** |
| US-4 | As a user, I want the app to automatically alert my emergency contact if I don't respond to a Level 2 danger within 30 seconds, so someone knows I may need help. | **Must Have** |
| US-5 | As a cyclist, I want to hear a ping when a car honks behind me, so I know to move over without removing my headphones. | **Should Have** |
| US-6 | As a user, I want to see which direction a sound came from (front/behind), so I know where to look. | **Nice to Have** |

---

## 4. Functional Requirements

### 4.1 Core Detection Engine

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1 | The app MUST use the YAMNet TensorFlow Lite model (via Google MediaPipe Audio Classifier) for sound classification. | Must Have |
| FR-2 | The app MUST run audio classification entirely on-device with no network calls. | Must Have |
| FR-3 | The app MUST continuously listen via the device microphone, even when the screen is locked, using an Android Foreground Service. | Must Have |
| FR-4 | The app MUST classify detected sounds into one of three urgency levels (see Section 4.2). | Must Have |
| FR-5 | The app MUST implement a 5-second "refractory period" (cooldown) per sound category to prevent repeated alerts from the same ongoing sound. | Must Have |

### 4.2 Urgency Levels & Response Matrix

| Level | Sound Types | Audio Response | Haptic Response | Visual Response |
|-------|-------------|----------------|-----------------|-----------------|
| **Level 1: Critical** | Sirens, Tire Screeches, Glass Breaking, Gunshots | Pause all media (AudioFocus GAIN_TRANSIENT) | SOS Vibrate Pattern (3 long pulses) | Full-screen red alert overlay |
| **Level 2: Danger** | Shouting, Aggressive Voices, Running Footsteps, Scuffling/Fighting | Duck volume to 10% | Pulse Vibrate (repeated short pulses) | Alert overlay + 30-second countdown |
| **Level 3: Warning** | Car Horns, Bicycle Bells | Play audio overlay "ping" (do not pause music) | None | Brief toast notification |

### 4.3 Level 1: Critical Response Flow

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-6 | Upon Level 1 detection, the app MUST immediately request AudioFocus (AUDIOFOCUS_GAIN_TRANSIENT) to pause other media apps (Spotify, YouTube, etc.). | Must Have |
| FR-7 | The app MUST trigger an SOS vibration pattern (3 long pulses of 500ms each). | Must Have |
| FR-8 | The app MUST display a full-screen high-contrast alert overlay (black background, red text) showing: the detected sound type, "Danger Detected", and a 10-second countdown. | Must Have |
| FR-9 | The alert overlay MUST include a prominent "I'm Safe" button to cancel the countdown. | Must Have |
| FR-10 | If the user does NOT cancel within 10 seconds, the app MUST send an emergency SMS to the pre-configured contact. | Must Have |

### 4.4 Level 2: Danger Response Flow (Dead Man's Switch)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-11 | Upon Level 2 detection, the app MUST duck the media volume to 10% (not pause). | Must Have |
| FR-12 | The app MUST trigger a pulse vibration pattern (repeated short pulses). | Must Have |
| FR-13 | The app MUST display an alert overlay showing: the detected sound type, "Potential Danger Nearby", and a 30-second countdown. | Must Have |
| FR-14 | The alert overlay MUST include an "I'm Safe" button to cancel the countdown. | Must Have |
| FR-15 | If the user does NOT cancel within 30 seconds, the app MUST begin audio recording and store it locally. | Must Have |
| FR-16 | If the user does NOT cancel within 30 seconds, the app MUST send an emergency SMS to the pre-configured contact. | Must Have |
| FR-17 | The SMS for Level 2 timeout MUST include the message: "BeAware Dead Man's Switch Activated: I did not respond to a potential danger detected near me. My location: [Google Maps Link]. Audio recording started." | Must Have |

### 4.5 Level 3: Warning Response Flow

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-18 | Upon Level 3 detection, the app MUST play a short, high-frequency "ping" sound overlaid on the current audio (do not pause or duck). | Must Have |
| FR-19 | The app MUST display a brief toast notification showing the detected sound type (e.g., "Car horn detected"). | Should Have |

### 4.6 Emergency Contact & SMS

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-20 | The app MUST allow the user to configure ONE emergency contact phone number in the Settings screen. | Must Have |
| FR-21 | The emergency SMS for Level 1 MUST contain: "BeAware Emergency Alert: I am wearing noise-canceling headphones and a Level 1 danger ([Sound Type]) was detected near me. My location: [Google Maps Link]." | Must Have |
| FR-22 | The app MUST request SMS permission on first launch. | Must Have |
| FR-23 | The app MUST request Location permission to include a Google Maps link in the SMS. | Must Have |

### 4.7 Foreground Service & Notification

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-24 | The app MUST run as an Android Foreground Service to ensure continuous microphone access when the screen is locked. | Must Have |
| FR-25 | The persistent notification MUST display: "BeAware: Protection Active 🛡️". | Must Have |
| FR-26 | The persistent notification MUST include a "Stop Protection" action button that terminates the foreground service. | Must Have |

### 4.8 User Interface

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-27 | **Home Screen:** MUST display a large, prominent ON/OFF toggle to start/stop protection. | Must Have |
| FR-28 | **Home Screen:** MUST display a live audio waveform visualizer when protection is active (shows the mic is listening). | Must Have |
| FR-29 | **Home Screen:** MUST display the current protection status ("Active" / "Inactive"). | Must Have |
| FR-30 | **Settings Screen:** MUST allow input of one emergency contact phone number. | Must Have |
| FR-31 | **Alert Overlay:** MUST be a full-screen, high-contrast (black/red) overlay that appears over any app. | Must Have |
| FR-32 | **Alert Overlay:** MUST display the detected sound type, countdown timer, and "I'm Safe" cancel button. | Must Have |

### 4.9 Directionality (Nice-to-Have)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-33 | The app SHOULD use TDOA (Time Difference of Arrival) across the phone's dual microphones to estimate sound direction (Front/Behind or Left/Right). | Nice to Have |
| FR-34 | If directionality is implemented, the alert ping SHOULD play only in the corresponding earbud (left sound → left earbud). | Nice to Have |

---

## 5. Non-Goals (Out of Scope for MVP)

The following are explicitly **NOT** included in the 36-hour MVP:

| Item | Reason |
|------|--------|
| iOS Version | Native Android only for hackathon |
| Backend/Cloud Processing | Privacy-first, on-device only |
| User Accounts / Login | No need for MVP |
| Event History Log | Adds complexity without demo value |
| Multiple Emergency Contacts | One contact is sufficient for MVP |
| Custom Alert Sounds per Category | Single generic ping is faster to implement |
| Onboarding Tutorial Flow | Simple permission prompts are sufficient |
| Smartwatch Companion App | Phone vibration is sufficient for MVP |
| Customizable Sensitivity Settings | Use default YAMNet confidence thresholds |

---

## 6. Design Considerations

### 6.1 Visual Design Principles

- **High Contrast:** All alerts use black backgrounds with red/white text for visibility in any lighting condition.
- **Large Touch Targets:** The "I'm Safe" button must be at least 60dp tall for easy tapping under stress.
- **Minimal Chrome:** No unnecessary UI elements during alerts—focus attention on the critical action.

### 6.2 Screen Mockup Descriptions

#### Home Screen
```
┌─────────────────────────────────┐
│         BeAware 🛡️              │
├─────────────────────────────────┤
│                                 │
│     ┌───────────────────┐       │
│     │   ≋≋≋≋≋≋≋≋≋≋≋≋   │       │  ← Live Waveform
│     │   ≋≋≋≋≋≋≋≋≋≋≋≋   │       │
│     └───────────────────┘       │
│                                 │
│         [ ◉ ACTIVE ]            │  ← Large Toggle Button
│                                 │
│     "Protection is active"      │
│                                 │
│  ────────────────────────────   │
│  ⚙️ Settings                    │
└─────────────────────────────────┘
```

#### Alert Overlay (Level 1)
```
┌─────────────────────────────────┐
│ ████████████████████████████████│ ← Black/Red Background
│                                 │
│         ⚠️ DANGER ⚠️             │
│                                 │
│     "SIREN DETECTED"            │
│                                 │
│     Sending SMS in: 07          │  ← Countdown Timer
│                                 │
│     ┌─────────────────────┐     │
│     │      I'M SAFE       │     │  ← Large Cancel Button
│     └─────────────────────┘     │
│                                 │
└─────────────────────────────────┘
```

---

## 7. Technical Considerations

### 7.1 Technology Stack

| Component | Technology |
|-----------|------------|
| Platform | Native Android (Kotlin) |
| Min SDK | API 26 (Android 8.0) |
| AI Engine | YAMNet via TensorFlow Lite / MediaPipe Audio Classifier |
| Audio Capture | Android AudioRecord API |
| Audio Control | AudioManager (AUDIOFOCUS_GAIN_TRANSIENT, AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) |
| Background Processing | Foreground Service with persistent notification |
| Haptics | Android Vibrator API with VibrationEffect |
| Location | FusedLocationProviderClient |
| SMS | SmsManager API |

### 7.2 Key Technical Implementation Notes

1. **Audio Sampling:** Capture audio at 16kHz sample rate (YAMNet requirement) in chunks of ~1 second for inference.

2. **AudioFocus Strategy:**
   - Level 1: `AUDIOFOCUS_GAIN_TRANSIENT` (pauses other apps)
   - Level 2: `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` (lowers other apps' volume)
   - Level 3: No AudioFocus change; overlay ping using SoundPool

3. **Foreground Service:** Must be started with `startForegroundService()` and display notification within 5 seconds to avoid ANR.

4. **YAMNet Output Mapping:** YAMNet outputs 521 sound classes. Map relevant classes to urgency levels:
   - Level 1: "Siren", "Emergency vehicle", "Screech", "Glass breaking", "Gunshot"
   - Level 2: "Shout", "Scream", "Yell", "Running", "Fight"
   - Level 3: "Car horn", "Honk", "Bicycle bell", "Doorbell"

5. **Confidence Threshold:** Only trigger alerts when YAMNet confidence > 0.6 (tune during testing).

6. **Overlay Permission:** Use `SYSTEM_ALERT_WINDOW` permission to display alerts over other apps.

### 7.3 Required Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

---

## 8. Success Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Detection Accuracy | >80% true positive rate for Level 1 sounds | Manual testing with 10 sample sounds |
| Detection Latency | <1 second from sound to alert | Stopwatch during demo |
| Battery Drain | <5% per hour | Android Battery Stats |
| Demo Completion | All 3 urgency levels demonstrated successfully | Live demo to judges |
| SMS Delivery | Emergency SMS sends with correct location | Test with real phone number |

---

## 9. Open Questions

| ID | Question | Owner | Status |
|----|----------|-------|--------|
| OQ-1 | What is the exact list of YAMNet class IDs to map to each urgency level? | Dev | Open |
| OQ-2 | Should the audio recording (Level 2 timeout) have a maximum duration? | Product | Open |
| OQ-3 | How should the app behave if SMS permission is denied? (Fallback to just recording?) | Dev | Open |
| OQ-4 | Should there be an audible countdown voice ("10... 9... 8...") or just visual? | Product | Open |
| OQ-5 | What happens if Location permission is denied? (Send SMS without location link?) | Dev | Open |

---

## 10. MVP Development Prioritization

Given the 36-hour constraint, here is the recommended build order:

### Phase 1: Core Infrastructure (Hours 0-8)
- [ ] Project setup with Kotlin, Gradle, MediaPipe dependencies
- [ ] Foreground Service skeleton with persistent notification
- [ ] Basic audio capture and YAMNet integration
- [ ] Sound classification pipeline (output class names to Logcat)

### Phase 2: Alert System (Hours 8-18)
- [ ] Urgency level mapping (YAMNet classes → Levels 1/2/3)
- [ ] AudioFocus integration (pause/duck media)
- [ ] Haptic feedback patterns
- [ ] Alert overlay UI (Level 1 & 2)
- [ ] Countdown timer logic
- [ ] "I'm Safe" cancel button

### Phase 3: Emergency Features (Hours 18-26)
- [ ] Settings screen with emergency contact input
- [ ] SMS sending with location
- [ ] Level 2 "Dead Man's Switch" (30-sec timeout → record + SMS)
- [ ] Audio recording functionality
- [ ] Level 3 ping overlay sound

### Phase 4: Polish & Demo Prep (Hours 26-36)
- [ ] Home screen UI with waveform visualizer
- [ ] 5-second cooldown logic
- [ ] Edge case testing
- [ ] Demo script preparation
- [ ] Bug fixes

### Stretch Goal (If Time Permits)
- [ ] Directionality via TDOA / spatial audio

---

## Appendix A: Vibration Patterns

```kotlin
// Level 1: SOS Pattern (3 long pulses)
val sosPattern = longArrayOf(0, 500, 200, 500, 200, 500)

// Level 2: Pulse Pattern (repeated short pulses)  
val pulsePattern = longArrayOf(0, 100, 100, 100, 100, 100, 100, 100)
```

---

## Appendix B: Sample Alert Messages

**Level 1 SMS:**
> BeAware Emergency Alert: I am wearing noise-canceling headphones and a Level 1 danger (Siren) was detected near me. My location: https://maps.google.com/?q=42.3601,-71.0589

**Level 2 Timeout SMS:**
> BeAware Dead Man's Switch Activated: I did not respond to a potential danger detected near me. My location: https://maps.google.com/?q=42.3601,-71.0589. Audio recording started.

---

*End of PRD*

