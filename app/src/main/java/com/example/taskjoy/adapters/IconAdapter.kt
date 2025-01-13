package com.example.taskjoy.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.taskjoy.R
import com.example.taskjoy.model.CustomIcon
import com.example.taskjoy.model.TaskJoyIcon
import com.google.android.material.card.MaterialCardView
import java.io.File

class IconAdapter private constructor(
    private val context: Context,
    private var icons: Array<TaskJoyIcon>,
    private var selectedIcon: TaskJoyIcon,
    private val supportsCustomIcons: Boolean,
    private var customIcons: List<CustomIcon> = emptyList(),
    private var selectedCustomIcon: CustomIcon? = null,
    private val onIconSelected: (TaskJoyIcon, CustomIcon?) -> Unit = { icon, _ -> onBasicIconSelected?.invoke(icon) },
    private val onAddCustomIcon: (() -> Unit)? = null,
    private val onDeleteCustomIcon: ((CustomIcon) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ICON = 0
        private const val VIEW_TYPE_ADD = 1
        private const val VIEW_TYPE_CUSTOM = 2
        private var onBasicIconSelected: ((TaskJoyIcon) -> Unit)? = null

        // Factory method for basic icon selection
        fun createBasic(
            context: Context,
            icons: Array<TaskJoyIcon>,
            selectedIcon: TaskJoyIcon,
            onIconSelected: (TaskJoyIcon) -> Unit
        ): IconAdapter {
            onBasicIconSelected = onIconSelected
            // Filter out the CUSTOM type from icons
            val filteredIcons = icons.filter { it != TaskJoyIcon.CUSTOM }.toTypedArray()

            return IconAdapter(
                context = context,
                icons = filteredIcons,  // Use filtered icons
                selectedIcon = selectedIcon,
                supportsCustomIcons = false
            )
        }

        // Factory method for full functionality including custom icons
        fun createWithCustom(
            context: Context,
            icons: Array<TaskJoyIcon>,
            customIcons: List<CustomIcon>,
            selectedIcon: TaskJoyIcon,
            selectedCustomIcon: CustomIcon? = null,
            onIconSelected: (TaskJoyIcon, CustomIcon?) -> Unit,
            onAddCustomIcon: () -> Unit,
            onDeleteCustomIcon: (CustomIcon) -> Unit
        ): IconAdapter {
            return IconAdapter(
                context = context,
                icons = icons,
                selectedIcon = selectedIcon,
                supportsCustomIcons = true,
                customIcons = customIcons,
                selectedCustomIcon = selectedCustomIcon,
                onIconSelected = onIconSelected,
                onAddCustomIcon = onAddCustomIcon,
                onDeleteCustomIcon = onDeleteCustomIcon
            )
        }
    }

    // ViewHolder for regular icons
    private inner class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: MaterialCardView = view.findViewById(R.id.iconContainer)
        val iconImage: ImageView = view.findViewById(R.id.iconImage)
    }

    // ViewHolder for the "Add Custom Icon" button
    private inner class AddIconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: MaterialCardView = view.findViewById(R.id.addIconContainer)
        init {
            container.setOnClickListener {
                onAddCustomIcon?.invoke()
            }
        }
    }

    // ViewHolder for custom icons
    private inner class CustomIconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: MaterialCardView = view.findViewById(R.id.iconContainer)
        val iconImage: ImageView = view.findViewById(R.id.iconImage)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDeleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ICON -> IconViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.icon_item, parent, false)
            )
            VIEW_TYPE_ADD -> AddIconViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_icon_item, parent, false)
            )
            else -> CustomIconViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.custom_icon_item, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is IconViewHolder -> {
                val icon = icons[position]
                bindRegularIcon(holder, icon)
            }
            is CustomIconViewHolder -> {
                val customIcon = customIcons[position - icons.size - if(supportsCustomIcons) 1 else 0]
                bindCustomIcon(holder, customIcon)
            }
        }
    }

    private fun bindRegularIcon(holder: IconViewHolder, icon: TaskJoyIcon) {
        // Don't try to set resource for CUSTOM type
        if (icon != TaskJoyIcon.CUSTOM) {
            try {
                holder.iconImage.setImageResource(icon.drawableResId)
            } catch (e: Exception) {
                Log.e("IconAdapter", "Error setting icon resource: ${e.message}")
                // Set a default icon or placeholder
                holder.iconImage.setImageResource(R.drawable.ic_brush_teeth)  // Use your default icon
            }
        }

        val isSelected = icon == selectedIcon && selectedCustomIcon == null
        updateSelectionState(holder.container, isSelected)

        holder.container.setOnClickListener {
            val oldPosition = getPositionForSelection(selectedIcon, selectedCustomIcon)
            selectedIcon = icon
            selectedCustomIcon = null
            onIconSelected(icon, null)
            notifyItemChanged(oldPosition)
            notifyItemChanged(holder.adapterPosition)
        }
    }

    private fun bindCustomIcon(holder: CustomIconViewHolder, customIcon: CustomIcon) {
        com.bumptech.glide.Glide.with(context)
            .load(File(customIcon.filepath))
            .centerCrop()
            .into(holder.iconImage)

        val isSelected = selectedIcon == TaskJoyIcon.CUSTOM && selectedCustomIcon == customIcon
        updateSelectionState(holder.container, isSelected)

        holder.container.setOnClickListener {
            val oldPosition = getPositionForSelection(selectedIcon, selectedCustomIcon)
            selectedIcon = TaskJoyIcon.CUSTOM
            selectedCustomIcon = customIcon
            onIconSelected(TaskJoyIcon.CUSTOM, customIcon)
            notifyItemChanged(oldPosition)
            notifyItemChanged(holder.adapterPosition)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteCustomIcon?.invoke(customIcon)
        }
    }

    private fun updateSelectionState(container: MaterialCardView, isSelected: Boolean) {
        container.strokeWidth = if (isSelected) {
            1
        } else {
            0
        }
        container.strokeColor = ContextCompat.getColor(context, R.color.primary_blue)
        container.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.primary_blue else R.color.white
            )
        )
    }

    private fun getPositionForSelection(icon: TaskJoyIcon, customIcon: CustomIcon?): Int {
        return when {
            customIcon != null -> {
                icons.size + 1 + customIcons.indexOf(customIcon)
            }
            else -> icons.indexOf(icon)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < icons.size -> VIEW_TYPE_ICON
            supportsCustomIcons && position == icons.size -> VIEW_TYPE_ADD
            else -> VIEW_TYPE_CUSTOM
        }
    }

    override fun getItemCount() = icons.size +
            (if (supportsCustomIcons) 1 else 0) +
            (if (supportsCustomIcons) customIcons.size else 0)

    fun updateData(
        newIcons: Array<TaskJoyIcon>,
        newCustomIcons: List<CustomIcon>,
        newSelectedIcon: TaskJoyIcon = selectedIcon,
        newSelectedCustomIcon: CustomIcon? = selectedCustomIcon
    ) {
        if (supportsCustomIcons) {
            icons = newIcons
            customIcons = newCustomIcons
            selectedIcon = newSelectedIcon
            selectedCustomIcon = newSelectedCustomIcon
            notifyDataSetChanged()
        }
    }
}