package com.reborn.wasteless.ui.tamagotchi



import android.animation.ObjectAnimator

import android.animation.PropertyValuesHolder

import android.app.AlertDialog

import android.content.Context

import android.content.SharedPreferences

import android.graphics.Color

import android.graphics.PorterDuff

import android.os.Bundle

import android.os.Handler

import android.os.Looper

import android.util.Log

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.view.animation.AccelerateDecelerateInterpolator

import android.view.animation.BounceInterpolator

import android.view.animation.LinearInterpolator

import android.widget.EditText

import android.widget.ImageView

import android.widget.Toast

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import com.reborn.wasteless.R

import com.reborn.wasteless.databinding.FragmentTamagotchiBinding

import java.text.SimpleDateFormat

import java.util.Calendar

import java.util.Date

import java.util.Locale

import kotlin.math.abs

import androidx.core.content.edit

import androidx.core.graphics.toColorInt



/**

 * Tamagotchi Fragment

 * - Manages all logic for the pet duck (stats, evolution, animation, day/night cycle, autonomous behavior, etc.)

 * - Persists pet state via SharedPreferences

 */

class TamagotchiFragment : Fragment() {



    private var _binding: FragmentTamagotchiBinding? = null

    private val binding get() = _binding!!



// --- 💾 Data storage keys (added V2 after migration to avoid conflict with old data) ---

    private val PREF_NAME = "PetData"

    private val KEY_HEALTH = "KEY_PET_HEALTH_V2" // Health

    private val KEY_CLEAN = "KEY_PET_CLEAN_V2" // Cleanliness

    private val KEY_MOOD = "KEY_PET_MOOD_V2" // Mood

    private val KEY_COINS = "KEY_PET_COINS_V2" // Coins

    private val KEY_LOGIN_DATE = "KEY_PET_LOGIN_DATE_V2" // Last login date (used for daily rewards)

    private val KEY_PET_NAME = "KEY_PET_NAME_V2" // Pet name

    private val KEY_LEVEL = "KEY_PET_LEVEL_V2" // Current level

    private val KEY_SPENT_ACC = "KEY_PET_SPENT_ACC_V2" // Accumulated spending (used for spending-based leveling)

    private val KEY_LAST_STAGE = "KEY_PET_LAST_STAGE_V2" // Previous evolution stage (avoids duplicate popups)



    private lateinit var sharedPreferences: SharedPreferences



// -- Pet data variables (e.g. their mood, health, cleanliness)

    private var health = 80

    private var cleanliness = 80

    private var mood = 80

    private var coins = 200

    private var lastLoginDate = ""

    private var petName = "Gilbert"



// --- 🧬 Level & Evolution Control ---

    private var currentLevel = 1 // Initial level

    private val MAX_LEVEL = 500 // Max level

    private var spentCoinsAccumulator = 0 // Counter to check spent coins, used to check if available for level up

    private var lastStage = 1 // Growth stage



// -- Animation handling

    private val handler = Handler(Looper.getMainLooper())

    private var hideSpeechRunnable: Runnable? = null // Runnable to auto-hide speech bubble



    private val randomActionHandler = Handler(Looper.getMainLooper())

    private var autonomousRunnable: Runnable? = null // Runnable for autonomous behavior loop



    private var walkWaddleAnim: ObjectAnimator? = null // Walking waddle animation

    private var isSleeping = false // Whether currently sleeping



    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        _binding = FragmentTamagotchiBinding.inflate(inflater, container, false)

        return binding.root

    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)



// 1. Initialize local data

        try {

            sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            loadData() // Load pet state

            checkDailyLevelUp() // Check if it's a new day, give daily reward

        } catch (e: Exception) {

            Log.e("Tamagotchi", "Init error: ${e.message}")

        }



// 2. Refresh UI based on latest data

        updateUI()



// 3. Check if evolution prompt is needed

        checkEvolutionEvent()



// 4. Day/Night System: Decide if pet sleeps based on time + night overlay

        checkDayNightCycle()



// 5. Start breathing/idle animation + autonomous behavior loop

        startIdleAnimations()

        startAutonomousBehavior()



// --- Button Event Binding ---



// Back button: Close pet screen

        binding.buttonClose.setOnClickListener { findNavController().popBackStack() }



// Rename button: Show dialog to enter new name

        binding.buttonEditName.setOnClickListener { showRenameDialog() }



// 🛠️ Debug Backdoor: Click name to level up quickly (for testing levels and evolution)

        binding.textPetName.setOnClickListener {

            if (currentLevel < MAX_LEVEL) {

                currentLevel += 10

                saveData()

                updateUI()

                checkEvolutionEvent()

                showSpeech("CHEAT! Lv.$currentLevel")

                playAnimation("JUMP")

            }

        }



// 🎮 "Play" button logic

        binding.buttonPlay.setOnClickListener {

            if (isSleeping) {

// Won't play while sleeping

                playAnimation("SHAKE_NO")

                showSpeech("I'm sleeping... Zzz")

            } else if (health >= 5) {

// Play: Increase mood, consume slight health

                updateStats(moodDelta = 10, healthDelta = -5)

                playAnimation("JUMP")

                showSpeech("Wheee! Fun!")

            } else {

// Not enough energy

                playAnimation("SHAKE_NO")

                showSpeech("Too tired...")

            }

        }



// 🎁 "Gift" button logic

        binding.buttonGift.setOnClickListener {

            if (coins >= 10) {

// Spend coins, improve mood

                updateStats(moodDelta = 25, coinsDelta = -10)

                playAnimation("HEARTBEAT")

                showSpeech("I love it!")

            } else {

                playAnimation("SHAKE_NO")

                showSpeech("Need 10 coins")

            }

        }



// 🛁 "Clean" button logic

        binding.buttonClean.setOnClickListener {

            if (coins >= 10) {

                if (cleanliness < 100) {

// Increase cleanliness, deduct coins

                    updateStats(cleanlinessDelta = 20, coinsDelta = -10)

                    playAnimation("SHAKE_BODY")

                    showSpeech("So clean!")

                } else {

// Already clean

                    showSpeech("Already clean!")

                }

            } else {

                playAnimation("SHAKE_NO")

                showSpeech("Need 10 coins")

            }

        }



// 💊 "Feed" button logic

        binding.buttonFeed.setOnClickListener {

            if (coins >= 10) {

                if (health < 100) {

// Increase health, deduct coins

                    updateStats(healthDelta = 15, coinsDelta = -10)

                    playAnimation("EAT")

                    showSpeech("Yummy!")

                } else {

                    playAnimation("SHAKE_NO")

                    showSpeech("I'm full!")

                }

            } else {

                playAnimation("SHAKE_NO")

                showSpeech("Need 10 coins")

            }

        }



// 🦆 Click on the duck itself

        binding.imagePetDuck.setOnClickListener {

            if (isSleeping) {

// Poked while sleeping, only shakes slightly

                binding.imagePetDuck.animate()

                    .translationXBy(10f).setDuration(50)

                    .withEndAction { binding.imagePetDuck.animate().translationXBy(-10f).start() }

                    .start()

                showSpeech("Zzz... sleepy...")

            } else {

// Daytime click: Play "POKE" animation

                playAnimation("POKE")

                showSpeech("Quack?")

            }

        }

    }



// --- 📈 Core: Daily Login Level Up + Reward Logic ---

    /**

     * First time entering the page each day:

     * - Check if date has changed

     * - If new day: +40 coins, level +1, trigger animation and toast

     */

    private fun checkDailyLevelUp() {

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastLoginDate != today) {

// New Day: Check-in reward

            coins += 40

            if (currentLevel < MAX_LEVEL) {

                currentLevel++

                showSpeech("New Day! Level Up!")

                playAnimation("JUMP")

                checkEvolutionEvent()

            }

            lastLoginDate = today

            saveData()

// Wrap Toast in try-catch to prevent crash if context is invalid

            try { Toast.makeText(requireContext(), "☀️ New Day! +40 Coins & Level Up!", Toast.LENGTH_LONG).show() } catch (e: Exception) {}

        }

    }



    /**

     * Update various pet stats (Health/Cleanliness/Mood/Coins)

     * - Automatically clamps within reasonable range (0~100)

     * - If coins decrease, accumulate spending and level up based on consumption

     */

    private fun updateStats(

        healthDelta: Int = 0,

        cleanlinessDelta: Int = 0,

        moodDelta: Int = 0,

        coinsDelta: Int = 0

    ) {

// coerceIn: Ensures value stays within 0~100 range

        health = (health + healthDelta).coerceIn(0, 100)

        cleanliness = (cleanliness + cleanlinessDelta).coerceIn(0, 100)

        mood = (mood + moodDelta).coerceIn(0, 100)

// Coins only limited by lower bound, can exceed 100

        coins = (coins + coinsDelta).coerceAtLeast(0)



// If "spending coins" (coinsDelta < 0), accumulate to spentCoinsAccumulator

        if (coinsDelta < 0) {

            val spent = abs(coinsDelta)

            spentCoinsAccumulator += spent

// Every 20 coins spent -> Level up (can accumulate multiple levels)

            if (spentCoinsAccumulator >= 20) {

                val levelsToGain = spentCoinsAccumulator / 20

                if (currentLevel < MAX_LEVEL) {

                    currentLevel = (currentLevel + levelsToGain).coerceAtMost(MAX_LEVEL)

                    showSpeech("Big Spender! Lv.$currentLevel")

                    playAnimation("HEARTBEAT")

                    checkEvolutionEvent()

                    try { Toast.makeText(requireContext(), "💰 Spent coins -> Level Up!", Toast.LENGTH_SHORT).show() } catch (e: Exception) {}

                }

// Keep only the remainder less than 20

                spentCoinsAccumulator %= 20

            }

        }

        updateUI()

        saveData()

    }



// --- 🦋 Evolution Stage Check & Trigger ---



    /**

     * Calculate evolution stage based on level:

     * 1: Egg (1~20)

     * 2: Young (21~60)

     * 3: Adult (61~200)

     * 4: King (201+)

     */

    private fun getStageFromLevel(level: Int): Int {

        return when {

            level <= 20 -> 1 // Egg

            level <= 60 -> 2 // Young

            level <= 200 -> 3 // Adult

            else -> 4 // King

        }

    }



    /**

     * Check if current level has entered a new evolution stage:

     * - If stage changed (currentStage > lastStage), show dialog + play celebration animation

     */

    private fun checkEvolutionEvent() {

        val currentStage = getStageFromLevel(currentLevel)

        if (currentStage > lastStage) {

            showEvolutionDialog(currentStage)

            playCelebrationAnimation()

            lastStage = currentStage

            saveData()

        }

    }



    /**

     * Celebration animation during evolution: Rotate + Scale + Float

     */

    private fun playCelebrationAnimation() {

        val duck = binding.imagePetDuck

        duck.animate()

            .rotation(360f)

            .scaleX(1.5f)

            .scaleY(1.5f)

            .translationY(-200f)

            .setDuration(600)

            .withEndAction {

                duck.animate()

                    .rotation(0f)

                    .scaleX(1f)

                    .scaleY(1f)

                    .translationY(0f)

                    .setDuration(400)

                    .start()

            }.start()

    }



    /**

     * Show evolution prompt dialog:

     * - Switch display image based on new stage

     * - Show evolution name + congratulatory text

     */

    private fun showEvolutionDialog(newStage: Int) {

        val stageName = when (newStage) {

            2 -> "Baby Duck!"

            3 -> "Adult Duck!"

            4 -> "King Gilbert!"

            else -> "New Form!"

        }



        val imageView = ImageView(requireContext())

// Select corresponding image resource based on stage

        val imageResId = when (newStage) {

            2 -> R.drawable.icon_tamagotchi_2 // Young

            3 -> R.drawable.icon_tamagotchi_3 // Adult

            4 -> R.drawable.icon_tamagotchi_4 // King

            else -> R.drawable.icon_sitting_duck

        }

        imageView.setImageResource(imageResId)

        imageView.adjustViewBounds = true

        imageView.maxHeight = 400



        AlertDialog.Builder(requireContext())

            .setTitle("✨ EVOLUTION! ✨")

            .setMessage("Congratulations! $petName evolved into $stageName")

            .setView(imageView)

            .setPositiveButton("Awesome!") { _, _ -> showSpeech("I feel stronger!") }

            .setCancelable(false)

            .show()

    }



// --- 💾 Data Persistence Logic ---



    /**

     * Write current pet state to SharedPreferences

     */

    private fun saveData() {

        sharedPreferences.edit(commit = true) {

            putInt(KEY_HEALTH, health)

            putInt(KEY_CLEAN, cleanliness)

            putInt(KEY_MOOD, mood)

            putInt(KEY_COINS, coins)

            putString(KEY_LOGIN_DATE, lastLoginDate)

            putString(KEY_PET_NAME, petName)

            putInt(KEY_LEVEL, currentLevel)

            putInt(KEY_SPENT_ACC, spentCoinsAccumulator)

            putInt(KEY_LAST_STAGE, lastStage)

        } // Use commit to ensure immediate write (relative to apply)

    }



    /**

     * Read pet state from SharedPreferences

     * - If not saved before, use default values

     */

    private fun loadData() {

        health = sharedPreferences.getInt(KEY_HEALTH, 80)

        cleanliness = sharedPreferences.getInt(KEY_CLEAN, 80)

        mood = sharedPreferences.getInt(KEY_MOOD, 80)

        coins = sharedPreferences.getInt(KEY_COINS, 100)

        lastLoginDate = sharedPreferences.getString(KEY_LOGIN_DATE, "") ?: ""

        petName = sharedPreferences.getString(KEY_PET_NAME, "Gilbert") ?: "Gilbert"

        currentLevel = sharedPreferences.getInt(KEY_LEVEL, 1)

        spentCoinsAccumulator = sharedPreferences.getInt(KEY_SPENT_ACC, 0)

        lastStage = sharedPreferences.getInt(KEY_LAST_STAGE, 1)

    }



// --- 🎨 UI Update (Health Bars + Name + Form Switch + Size Adjustment) ---

    /**

     * Refresh UI display based on current values:

     * - Health bars, cleanliness, mood bar, coins

     * - Add "Lv.X" prefix to name

     * - Switch image based on level stage, and adjust size

     */

    private fun updateUI() {

        if (_binding == null) return



// Progress bars & Value text

        binding.progressHealth.progress = health

        binding.progressCleanliness.progress = cleanliness

        binding.progressMood.progress = mood

        binding.valueHealth.text = "$health%"

        binding.valueCleanliness.text = "$cleanliness%"

        binding.valueMood.text = "$mood%"

        binding.textPetCoins.text = coins.toString()



// Name display: Add "Sad" prefix if mood is below 30

        val displayName = "Lv.$currentLevel $petName"

        binding.textPetName.text = if (mood < 30) "Sad $displayName" else displayName

        binding.textPetName.setTextColor(Color.BLACK)



// Prepare to modify image size

        val params = binding.imagePetDuck.layoutParams



// Evolution form + Image replacement + Size change

        if (currentLevel <= 20) {

// 🥚 Stage 1: Egg (Small)

            binding.imagePetDuck.setImageResource(R.drawable.icon_tamagotchi_1)

            params.width = dpToPx(60)

            params.height = dpToPx(80)

        } else if (currentLevel <= 60) {

// 🐥 Stage 2: Young (Medium)

            binding.imagePetDuck.setImageResource(R.drawable.icon_tamagotchi_2)

            params.width = dpToPx(80)

            params.height = dpToPx(100)

        } else if (currentLevel <= 200) {

// 🦆 Stage 3: Adult (Large)

            binding.imagePetDuck.setImageResource(R.drawable.icon_tamagotchi_3)

            params.width = dpToPx(90)

            params.height = dpToPx(120)

        } else {

// 👑 Stage 4: King (Larger)

            binding.imagePetDuck.setImageResource(R.drawable.icon_tamagotchi_4)

            params.width = dpToPx(110)

            params.height = dpToPx(140)

        }



// Apply new size

        binding.imagePetDuck.layoutParams = params

    }



// Convert dp to px to ensure consistent size across resolutions

    private fun dpToPx(dp: Int): Int {

        val density = resources.displayMetrics.density

        return (dp * density).toInt()

    }



    override fun onPause() {

        super.onPause()

// Save data again before leaving to prevent loss

        saveData()

    }



// --- 🌙 Day/Night System: Auto-decide Sleep/Wake based on time ---

    /**

     * 21:00 ~ Next day 7:00 is considered "Night"

     * - Night: isSleeping = true, cover with dark overlay, darken duck

     * - Day: Remove overlay and color filter

     * If forced night testing is needed, change isTestMode to true

     */

    private fun checkDayNightCycle() {

        val calendar = Calendar.getInstance()

        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val isTestMode = false // If forced night testing is needed, change isTestMode to true

        isSleeping = isTestMode || (hour >= 21 || hour < 7)



        if (isSleeping) {

            try {

// Show night overlay + add dark color filter to duck

                binding.root.findViewById<View>(R.id.view_night_overlay)?.visibility = View.VISIBLE

                binding.imagePetDuck.setColorFilter(

                    "#99000000".toColorInt(),

                    PorterDuff.Mode.SRC_ATOP

                )

            } catch (e: Exception) { }

            showSpeech("Zzz...")

        } else {

            try {

// Day mode: Remove overlay and color filter

                binding.root.findViewById<View>(R.id.view_night_overlay)?.visibility = View.INVISIBLE

                binding.imagePetDuck.clearColorFilter()

            } catch (e: Exception) { }

        }

    }



// --- ✏️ Rename Function ---

    /**

     * Show input dialog for user to enter new pet name

     */

    private fun showRenameDialog() {

        val editText = EditText(requireContext())

        editText.hint = "Enter new name"

        editText.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(requireContext())

            .setTitle("Rename Pet")

            .setMessage("Name your duck:")

            .setView(editText)

            .setPositiveButton("Save") { _, _ ->

                val newName = editText.text.toString().trim()

                if (newName.isNotEmpty()) {

                    petName = newName

                    saveData()

                    updateUI()

                    showSpeech("Call me $petName!")

                }

            }

            .setNegativeButton("Cancel", null)

            .show()

    }



// --- ✨ Animation Logic ---



    /**

     * Idle "Breathing" Animation: Slight Y-axis scale to simulate breathing

     */

    private fun startIdleAnimations() {

        if (_binding == null) return

        val breathAnim = ObjectAnimator.ofFloat(binding.imagePetDuck, "scaleY", 1f, 1.05f)

        breathAnim.duration = 1500

        breathAnim.repeatCount = ObjectAnimator.INFINITE

        breathAnim.repeatMode = ObjectAnimator.REVERSE

        breathAnim.start()

    }



    /**

     * Autonomous Behavior Loop:

     * - If sleeping: Occasionally say "Zzz..."

     * - If awake: Randomly choose "Walk / Look Around / Idle"

     */

    private fun startAutonomousBehavior() {

        autonomousRunnable = Runnable {

            if (_binding != null) {

                if (isSleeping) {

// When sleeping, occasionally only show Zzz...

                    if (binding.textSpeechBubble.visibility != View.VISIBLE) {

                        showSpeech("Zzz...")

                    }

                    randomActionHandler.postDelayed(autonomousRunnable!!, 5000)

                } else {

// Day: 0~1 random number decides next action

                    val choice = Math.random()

                    if (choice > 0.6) {

                        doWalkAction()

                    } else if (choice > 0.3) {

                        doLookAroundAction()

                    } else {

// Occasionally do nothing, wait 2 seconds then continue

                        randomActionHandler.postDelayed(autonomousRunnable!!, 2000)

                    }

                }

            }

        }

// Start first loop

        randomActionHandler.post(autonomousRunnable!!)

    }



    /**

     * Autonomous Action: Free Walk (Up/Down/Left/Right)

     * - Random target X and Y positions

     * - Start "Waddle" animation when walking, stop when finished

     */

    private fun doWalkAction() {

// 1. Randomly generate X [-150, 150] and Y [-60, 60]

// Y-axis note: Negative is up (far), positive is down (near)

        val targetX = (Math.random() * 300f - 150f).toFloat()

        val targetY = (Math.random() * 120f - 60f).toFloat() // ✅ Added: Vertical random range



        val currentX = binding.imagePetDuck.translationX

        val currentY = binding.imagePetDuck.translationY



// 2. Calculate linear distance between points (Pythagorean theorem) to ensure even speed

// If only using X for distance, diagonal walking looks too fast

        val distance = Math.hypot((targetX - currentX).toDouble(), (targetY - currentY).toDouble()).toFloat()



// The further the distance, the longer the duration (factor 10 makes it walk calmly)

        val duration = (distance * 10).toLong().coerceAtLeast(1500)



// 3. Flip facing direction based on X-axis (Look Left/Right)

        val movingRight = targetX > currentX

        val targetScaleX = if (movingRight) -1f else 1f

        binding.imagePetDuck.animate().scaleX(targetScaleX).setDuration(200).start()



// 4. Execute dual-axis movement animation

        binding.imagePetDuck.animate()

            .translationX(targetX)

            .translationY(targetY) // ✅ Let it move up/down here

            .setDuration(duration)

            .setInterpolator(LinearInterpolator())

            .withEndAction { stopWalkingWaddle() }

            .start()



// Start walking waddle

        startWalkingWaddle()



        if (Math.random() > 0.8) showSpeech("Quack!")



// Wait for walk to finish + 2~4 seconds, then continue next round

        randomActionHandler.postDelayed(

            autonomousRunnable!!,

            duration + (2000..4000).random()

        )

    }



    /**

     * Autonomous Action: Look Around (Change facing only, no movement)

     */

    private fun doLookAroundAction() {

        val currentScaleX = binding.imagePetDuck.scaleX

        val targetScaleX = if (currentScaleX > 0) -1f else 1f

        binding.imagePetDuck.animate().scaleX(targetScaleX).setDuration(400).start()

        randomActionHandler.postDelayed(autonomousRunnable!!, 3000)

    }



    /**

     * Start "Walking Waddle" Animation: Small angle left/right swing

     */

    private fun startWalkingWaddle() {

        if (walkWaddleAnim == null) {

            walkWaddleAnim = ObjectAnimator.ofFloat(binding.imagePetDuck, "rotation", -8f, 8f)

            walkWaddleAnim?.duration = 300

            walkWaddleAnim?.repeatCount = ObjectAnimator.INFINITE

            walkWaddleAnim?.repeatMode = ObjectAnimator.REVERSE

        }

        if (walkWaddleAnim?.isRunning == false) {

            walkWaddleAnim?.start()

        }

    }



    /**

     * Stop walking waddle animation, and reset duck angle to 0

     */

    private fun stopWalkingWaddle() {

        walkWaddleAnim?.cancel()

        binding.imagePetDuck.animate().rotation(0f).setDuration(200).start()

    }



    /**

     * Unified animation entry point, select interaction animation based on type

     */

    private fun playAnimation(type: String) {

        if (_binding == null) return

        val duck = binding.imagePetDuck

// Before playing new animation, ensure walking waddle is stopped

        stopWalkingWaddle()



        when (type) {

// Poke: Squash Y, stretch X, then bounce back

            "POKE" -> {

                val currentDir = if (duck.scaleX < 0) -1f else 1f

                duck.animate()

                    .scaleY(0.75f)

                    .scaleX(1.25f * currentDir)

                    .setDuration(100)

                    .setInterpolator(AccelerateDecelerateInterpolator())

                    .withEndAction {

                        duck.animate()

                            .scaleY(1f)

                            .scaleX(1f * currentDir)

                            .setDuration(500)

                            .setInterpolator(BounceInterpolator())

                            .start()

                    }.start()

            }

// Jump: Move up then fall

            "JUMP" -> duck.animate()

                .translationY(-120f)

                .setDuration(300)

                .withEndAction {

                    duck.animate()

                        .translationY(0f)

                        .setDuration(300)

                        .start()

                }.start()



// Shake body (for cleaning)

            "SHAKE_BODY" -> ObjectAnimator

                .ofFloat(duck, "rotation", 0f, -15f, 15f, -15f, 15f, 0f)

                .setDuration(600)

                .start()



// Eat: Slightly stretch and squash

            "EAT" -> ObjectAnimator.ofPropertyValuesHolder(

                duck,

                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f, 1f),

                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.9f, 1f)

            ).setDuration(400).start()



// Heartbeat: Scale up and down

            "HEARTBEAT" -> ObjectAnimator.ofPropertyValuesHolder(

                duck,

                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.3f, 1f),

                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.3f, 1f)

            ).setDuration(400).start()



// Refuse/Shake Head: Pan left/right

            "SHAKE_NO" -> ObjectAnimator

                .ofFloat(duck, "translationX", 0f, 20f, -20f, 20f, -20f, 0f)

                .setDuration(400)

                .start()

        }

    }



    /**

     * Show top speech bubble:

     * - Position follows current duck position (translationX/Y)

     * - Fade out and hide automatically after 2 seconds

     */

    private fun showSpeech(text: String) {

        if (_binding == null) return

        binding.textSpeechBubble.translationX = binding.imagePetDuck.translationX

        binding.textSpeechBubble.translationY = binding.imagePetDuck.translationY

        binding.textSpeechBubble.text = text

        binding.textSpeechBubble.alpha = 0f

        binding.textSpeechBubble.visibility = View.VISIBLE

        binding.textSpeechBubble.animate().alpha(1f).setDuration(200).start()



// Remove old hide task every time a new speech is shown

        hideSpeechRunnable?.let { handler.removeCallbacks(it) }

        hideSpeechRunnable = Runnable {

            if (_binding != null) {

                binding.textSpeechBubble.animate()

                    .alpha(0f)

                    .setDuration(300)

                    .withEndAction {

                        if (_binding != null) binding.textSpeechBubble.visibility = View.INVISIBLE

                    }.start()

            }

        }

        handler.postDelayed(hideSpeechRunnable!!, 2000)

    }



// --- ♻️ Lifecycle Cleanup: Prevent Memory Leaks & Animation Residuals ---

    override fun onDestroyView() {

        super.onDestroyView()



// Cancel all delayed tasks & animations to avoid memory leaks

        hideSpeechRunnable?.let { handler.removeCallbacks(it) }

        autonomousRunnable?.let { randomActionHandler.removeCallbacks(it) }

        handler.removeCallbacksAndMessages(null)

        randomActionHandler.removeCallbacksAndMessages(null)



// Stop walking animation

        walkWaddleAnim?.cancel()

        walkWaddleAnim = null



// Cancel ongoing view animations

        try {

            if (_binding != null) {

                binding.imagePetDuck.animate().cancel()

                binding.textSpeechBubble.animate().cancel()

            }

        } catch (e: Exception) { }



        _binding = null

    }

}

