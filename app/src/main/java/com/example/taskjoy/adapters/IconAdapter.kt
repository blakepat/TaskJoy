package com.example.taskjoy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.model.TaskJoyIcon

class IconAdapter(
    private val icons: Array<TaskJoyIcon>,
    private var selectedIcon: TaskJoyIcon,
    private val onIconSelected: (TaskJoyIcon) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconViewHolder>() {

    class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: CardView = view.findViewById(R.id.iconContainer)
        val iconImage: ImageView = view.findViewById(R.id.iconImage)
        val iconName: TextView = view.findViewById(R.id.iconName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.icon_item, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val icon = icons[position]
        holder.iconImage.setImageResource(icon.drawableResId)
        holder.iconName.text = icon.name.lowercase().capitalize()

        // Highlight selected icon
        holder.iconContainer.setCardBackgroundColor(
            if (icon == selectedIcon)
                holder.itemView.context.getColor(R.color.black)
            else
                holder.itemView.context.getColor(R.color.white)
        )

        holder.itemView.setOnClickListener {
            val oldSelected = icons.indexOf(selectedIcon)
            selectedIcon = icon
            notifyItemChanged(oldSelected)
            notifyItemChanged(position)
            onIconSelected(icon)
        }
    }

    override fun getItemCount() = icons.size
}