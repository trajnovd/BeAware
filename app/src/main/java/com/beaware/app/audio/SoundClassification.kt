package com.beaware.app.audio

/**
 * Represents a classified sound with its urgency level and confidence score.
 * 
 * Sound categories are mapped to three danger levels:
 * 🔴 Level 1 (DANGER): Screaming, shouting for help, physical struggle, glass breaking
 * 🟡 Level 2 (CAUTION): Sirens, car horns, engine revving, non-distress shouting
 * 🟢 Level 3 (AWARENESS): Bicycle bells, bus sounds, footsteps, dog barking
 */
data class SoundClassification(
    val label: String,
    val displayName: String,
    val confidence: Float,
    val urgencyLevel: UrgencyLevel,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        // Threshold is now handled in AudioClassifier with priority filtering
        const val CONFIDENCE_THRESHOLD = 0.2f

        // =================================================================
        // 🔴 LEVEL 1: DANGER (Safety Mode Triggered)
        // Purpose: Protect the user if they may be in danger
        // Response: Music stops, continuous alert, 30-second countdown to SOS
        // =================================================================
        private val DANGER_SOUNDS_LEVEL1 = mapOf(
            // Shouting for help / distress
            "Screaming" to "SCREAMING",
            "Scream" to "SCREAMING",
            "Shout" to "SHOUTING FOR HELP",
            "Yell" to "SHOUTING",
            "Crying" to "DISTRESS",
            "Sobbing" to "DISTRESS",
            "Wail, moan" to "DISTRESS",
            "Whimper" to "DISTRESS",
            "Groan" to "DISTRESS",
            
            // Physical struggle / assault sounds
            "Battle cry" to "AGGRESSIVE SHOUTING",
            "Grunt" to "PHYSICAL STRUGGLE",
            "Slap, smack" to "PHYSICAL STRUGGLE",
            "Punch" to "PHYSICAL STRUGGLE",
            "Thump, thud" to "IMPACT SOUND",
            "Whack, thwack" to "PHYSICAL STRUGGLE",
            
            // Glass breaking
            "Glass" to "GLASS BREAKING",
            "Shatter" to "GLASS BREAKING",
            "Breaking" to "GLASS BREAKING",
            "Crash" to "CRASH",
            "Smash" to "GLASS BREAKING",
            
            // Weapons / extreme danger
            "Gunshot, gunfire" to "GUNSHOT",
            "Gunshot" to "GUNSHOT",
            "Explosion" to "EXPLOSION",
            "Boom" to "EXPLOSION",
            
//            // Running (often combined with other danger sounds)
//            "Run" to "RUNNING",
//            "Running" to "RUNNING"
        )

        // =================================================================
        // 🟡 LEVEL 2: CAUTION (Potential Danger)
        // Purpose: Get attention immediately
        // Response: Music lowers to 20%, double ping, strong vibration
        // =================================================================
        private val CAUTION_SOUNDS_LEVEL2 = mapOf(
            // Emergency vehicle sirens
            "Siren" to "SIREN",
            "Civil defense siren" to "SIREN",
            "Ambulance (siren)" to "AMBULANCE SIREN",
            "Fire engine, fire truck (siren)" to "FIRE TRUCK SIREN",
            "Police car (siren)" to "POLICE SIREN",
            "Emergency vehicle" to "EMERGENCY VEHICLE",
            
            // Vehicle sounds
            "Vehicle horn, car horn, honking" to "CAR HORN",
            "Car horn" to "CAR HORN",
            "Honk" to "CAR HORN",
            "Honking" to "CAR HORN",
//            "Air horn, truck horn" to "TRUCK HORN",
//            "Truck horn" to "TRUCK HORN",
//            "Beep, bleep" to "WARNING BEEP",
            
            // Vehicle engine sounds
            "Engine" to "ENGINE REVVING",
            "Revving, vroom" to "ENGINE REVVING",
            "Engine starting" to "ENGINE STARTING",
            "Accelerating, revving, vroom" to "ENGINE REVVING",
//            "Race car, auto racing" to "RACING ENGINE",
//            "Motorcycle" to "MOTORCYCLE",
            
            // Tire sounds
            "Tire squeal" to "TIRE SCREECH",
            "Skidding" to "TIRE SCREECH",
            "Squeal" to "TIRE SCREECH",
            
            // Shouting (non-distress)
//            "Crowd" to "CROWD SHOUTING",
//            "Cheering" to "CROWD NOISE",
//            "Cheer" to "CROWD NOISE",
            
            // Alarms
            "Alarm" to "ALARM",
            "Alarm clock" to "ALARM",
            "Buzzer" to "BUZZER",
            "Fire alarm" to "FIRE ALARM",
            "Smoke detector" to "SMOKE ALARM",
            "Car alarm" to "CAR ALARM"
        )

        // =================================================================
        // 🟢 LEVEL 3: AWARENESS (No Immediate Danger)
        // Purpose: Inform the user without stress
        // Response: Soft ping, light vibration, no UI interruption
        // =================================================================
        private val AWARENESS_SOUNDS_LEVEL3 = mapOf(
            // Bicycle / light vehicle bells
            "Bicycle bell" to "BICYCLE BELL",
            "Bell" to "BELL",
            "Ding" to "BELL",
            "Chime" to "CHIME",
            
            // Bus / public transport
            "Bus" to "BUS",
            "Vehicle" to "VEHICLE NEARBY",
            "Idling" to "VEHICLE IDLING",
            "Air brake" to "BUS BRAKING",
            
            // Doors
            "Door" to "DOOR",
//            "Sliding door" to "DOOR CLOSING",
            "Slam" to "DOOR SLAM",
            
            // Footsteps
//            "Footsteps" to "FOOTSTEPS",
//            "Walk, footsteps" to "FOOTSTEPS",
            
            // Dog sounds (non-aggressive)
            "Dog" to "DOG NEARBY",
            "Bark" to "DOG BARKING",
            "Barking" to "DOG BARKING",
            "Howl" to "DOG HOWLING",
            "Whine, whimper" to "DOG WHIMPERING",
            
            // Construction / work sounds (distant)
//            "Power tool" to "CONSTRUCTION",
//            "Drill" to "CONSTRUCTION",
//            "Hammer" to "CONSTRUCTION",
//            "Sawing" to "CONSTRUCTION",
//            "Jackhammer" to "CONSTRUCTION",
//
            // General environment
            "Traffic noise, roadway noise" to "TRAFFIC",
//            "Toot" to "HORN",
//            "Whistle" to "WHISTLE",
//            "Steam whistle" to "WHISTLE",
//            "Train whistle" to "TRAIN WHISTLE",
//            "Train horn" to "TRAIN HORN",
            
            // Other awareness sounds
//            "Bird" to "BIRD",
//            "Chirp, tweet" to "BIRD",
//            "Clapping" to "CLAPPING",
//            "Fireworks" to "FIREWORKS"
        )

        /**
         * Get the urgency level and display name for a YAMNet classification label.
         * Priority: Level 1 (most dangerous) > Level 2 > Level 3
         */
        fun getUrgencyForLabel(label: String): Pair<UrgencyLevel, String>? {
            val lowerLabel = label.lowercase()
            
            // 🔴 Check Level 1 (DANGER) first - highest priority
            DANGER_SOUNDS_LEVEL1.entries.find { 
                lowerLabel.contains(it.key.lowercase()) || it.key.lowercase().contains(lowerLabel)
            }?.let {
                return UrgencyLevel.CRITICAL to it.value
            }

            // 🟡 Check Level 2 (CAUTION)
            CAUTION_SOUNDS_LEVEL2.entries.find { 
                lowerLabel.contains(it.key.lowercase()) || it.key.lowercase().contains(lowerLabel)
            }?.let {
                return UrgencyLevel.DANGER to it.value
            }

            // 🟢 Check Level 3 (AWARENESS)
            AWARENESS_SOUNDS_LEVEL3.entries.find { 
                lowerLabel.contains(it.key.lowercase()) || it.key.lowercase().contains(lowerLabel)
            }?.let {
                return UrgencyLevel.WARNING to it.value
            }

            return null
        }

        /**
         * Create a SoundClassification from a YAMNet result
         */
        fun fromYamNetResult(label: String, confidence: Float): SoundClassification? {
            val (urgency, displayName) = getUrgencyForLabel(label) ?: return null
            
            return SoundClassification(
                label = label,
                displayName = displayName,
                confidence = confidence,
                urgencyLevel = urgency
            )
        }
    }
}
