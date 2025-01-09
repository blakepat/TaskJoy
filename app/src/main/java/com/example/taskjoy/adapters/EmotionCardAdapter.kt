package com.example.taskjoy.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.databinding.ItemEmotionCardBinding
import com.example.taskjoy.model.EmotionCard

class EmotionCardAdapter(
    private val cards: List<EmotionCard>,
    private val onCardClick: (Int) -> Unit
) : RecyclerView.Adapter<EmotionCardAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemEmotionCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        Log.d("EmotionGame", "Binding card at position $position")
        holder.bind(cards[position])
    }

    override fun getItemCount() = cards.size

    inner class CardViewHolder(
        private val binding: ItemEmotionCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    Log.d("EmotionGame", "Card clicked in adapter at position $position")
                    onCardClick(position)
                }
            }
        }

        fun bind(card: EmotionCard) {
            Log.d("EmotionGame", "Binding card ${card.emotion.name}, isFlipped: ${card.isFlipped}")
            binding.apply {
                if (card.isFlipped) {
                    imageViewEmotion.setImageResource(card.emotion.iconRes)
                    textViewEmotion.text = card.emotion.name
                    cardFront.visibility = View.VISIBLE
                    cardBack.visibility = View.GONE
                    Log.d("EmotionGame", "Showing front of card: ${card.emotion.name}")
                } else {
                    cardFront.visibility = View.GONE
                    cardBack.visibility = View.VISIBLE
                    Log.d("EmotionGame", "Showing back of card")
                }
                // Force layout update
                root.requestLayout()
            }
        }
    }
}