package com.example.taskjoy.screens

import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.adapters.EmotionCardAdapter
import com.example.taskjoy.databinding.ActivityEmotionMemoryBinding
import com.example.taskjoy.model.Emotion
import com.example.taskjoy.model.EmotionCard
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EmotionMemoryActivity : AppCompatActivity() {
    private var _binding: ActivityEmotionMemoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var cards: List<EmotionCard>
    private var flippedCards = mutableListOf<EmotionCard>()
    private var matchedPairs = mutableSetOf<Int>()
    private var moves = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d("EmotionGame", "EmotionMemoryActivity onCreate started")

            _binding = ActivityEmotionMemoryBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d("EmotionGame", "View binding completed")

            setupGame()
            setupClickListeners()

            Log.d("EmotionGame", "EmotionMemoryActivity setup completed")
        } catch (e: Exception) {
            Log.e("EmotionGame", "Error in onCreate", e)
            Toast.makeText(this, "Error starting game: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupGame() {
        try {
            val emotions = listOf(
                Emotion(1, R.drawable.ic_happy, "Happy"),
                Emotion(2, R.drawable.ic_sad, "Sad"),
                Emotion(3, R.drawable.ic_excited, "Excited"),
                Emotion(4, R.drawable.ic_worried, "Worried"),
                Emotion(5, R.drawable.ic_love, "Love"),
                Emotion(6, R.drawable.ic_proud, "Proud")
            )

            Log.d("EmotionGame", "Creating card list")
            cards = (emotions + emotions).shuffled().mapIndexed { index, emotion ->
                EmotionCard(index, emotion, false)  // Make sure isFlipped is initially false
            }
            Log.d("EmotionGame", "Created ${cards.size} cards")

            binding.recyclerViewEmotionCards.apply {
                val spanCount = 3
                layoutManager = GridLayoutManager(this@EmotionMemoryActivity, spanCount)
                val spacing = resources.getDimensionPixelSize(R.dimen.card_spacing)
                addItemDecoration(GridSpaceItemDecoration(spacing))
                adapter = EmotionCardAdapter(cards, ::onCardClick)
                setHasFixedSize(true)
            }

            updateMoveCounter()
            Log.d("EmotionGame", "Game setup completed with ${cards.size} cards")
        } catch (e: Exception) {
            Log.e("EmotionGame", "Error in setupGame", e)
            Toast.makeText(this, "Error setting up game: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.buttonNewEmotionGame.setOnClickListener {
            Log.d("EmotionGame", "New Game button clicked")
            resetGame()
        }
    }

    private fun onCardClick(position: Int) {
        val card = cards[position]

        Log.d("EmotionGame", "Card clicked at position $position, isFlipped: ${card.isFlipped}")
        Log.d("EmotionGame", "Current flipped cards: ${flippedCards.size}")

        // Don't allow more than 2 cards flipped at once
        if (flippedCards.size == 2) {
            Log.d("EmotionGame", "Already have 2 cards flipped, ignoring click")
            return
        }

        // Don't allow flipping already matched or currently flipped cards
        if (matchedPairs.contains(card.emotion.id) || flippedCards.contains(card)) {
            Log.d("EmotionGame", "Card is already matched or flipped, ignoring click")
            return
        }

        // Flip the card
        card.isFlipped = true
        flippedCards.add(card)
        Log.d("EmotionGame", "Flipping card ${card.emotion.name}")
        binding.recyclerViewEmotionCards.adapter?.notifyItemChanged(position)

        if (flippedCards.size == 2) {
            moves++
            updateMoveCounter()
            Log.d("EmotionGame", "Checking for match between ${flippedCards[0].emotion.name} and ${flippedCards[1].emotion.name}")

            if (flippedCards[0].emotion.id == flippedCards[1].emotion.id) {
                // Match found
                Log.d("EmotionGame", "Found a match!")
                matchedPairs.add(flippedCards[0].emotion.id)
                flippedCards.clear()

                if (matchedPairs.size == 6) {
                    Log.d("EmotionGame", "Game won!")
                    showGameWonDialog()
                }
            } else {
                // No match, flip cards back
                Log.d("EmotionGame", "No match, flipping cards back")
                Handler(Looper.getMainLooper()).postDelayed({
                    flippedCards.forEach { it.isFlipped = false }
                    flippedCards.clear()
                    binding.recyclerViewEmotionCards.adapter?.notifyDataSetChanged()
                }, 1000)
            }
        }
    }

    private fun updateMoveCounter() {
        binding.textEmotionMoves.text = getString(R.string.moves_count, moves)
        Log.d("EmotionGame", "Moves updated to: $moves")
    }

    private fun showGameWonDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.congratulations))
            .setMessage(getString(R.string.won_in_moves, moves))
            .setPositiveButton(getString(R.string.play_again)) { _, _ -> resetGame() }
            .setCancelable(false)
            .show()
        Log.d("EmotionGame", "Showing game won dialog")
    }

    private fun resetGame() {
        Log.d("EmotionGame", "Resetting game")
        cards.forEach { it.isFlipped = false }
        flippedCards.clear()
        matchedPairs.clear()
        moves = 0
        updateMoveCounter()
        cards = cards.shuffled()
        binding.recyclerViewEmotionCards.adapter?.notifyDataSetChanged()
        Log.d("EmotionGame", "Game reset completed")
    }
}

class GridSpaceItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = spacing
        outRect.right = spacing
        outRect.top = spacing
        outRect.bottom = spacing
    }
}