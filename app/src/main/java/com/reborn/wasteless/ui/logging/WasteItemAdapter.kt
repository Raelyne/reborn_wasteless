package com.reborn.wasteless.ui.logging

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reborn.wasteless.R
import com.reborn.wasteless.data.WasteItem
import com.reborn.wasteless.data.model.WasteItemSelection
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.roundToInt

class WasteItemAdapter(
    private val onQtyChanged: (item: WasteItem, newQty: Double) -> Unit
) : ListAdapter<WasteItemSelection, WasteItemAdapter.VH>(DIFF) {

    // We do this to let the adapter know that we're using stable ids to differentiate
    init {
        setHasStableIds(true)
    }

    //Getting the stable id from the data model
    override fun getItemId(position: Int): Long {
        return getItem(position).item.wasteId.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waste, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val nameView: TextView = view.findViewById(R.id.text_item_waste)
        private val qtyInput: TextInputEditText =
            view.findViewById(R.id.edt_item_waste)

        private var lastRawQty: Double? = null

        // a single watcher per holder, initialized once
        private val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                lastRawQty = s?.toString()?.toDoubleOrNull()
            }
        }

        init {
            //Attaching watcher to qtyInput
            qtyInput.addTextChangedListener(watcher)

            qtyInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val raw = lastRawQty ?: return@setOnFocusChangeListener
                    val rounded = (raw * 10).roundToInt() / 10.0
                    val pos = this@VH.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?: return@setOnFocusChangeListener
                    val sel = getItem(pos)
                    if (sel.quantity != rounded) {
                        onQtyChanged(sel.item, rounded)
                    }
                }
            }
        }

        fun bind(selection: WasteItemSelection) {
            nameView.text = selection.item.name

            // detach watcher during programmatic update
            qtyInput.removeTextChangedListener(watcher)
            qtyInput.setText(selection.quantity.toString())
            // re‐attach afterwards so user edits still fire
            qtyInput.addTextChangedListener(watcher)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WasteItemSelection>() {
            override fun areItemsTheSame(
                oldItem: WasteItemSelection, newItem: WasteItemSelection
            ) = oldItem.item.wasteId == newItem.item.wasteId

            override fun areContentsTheSame(
                oldItem: WasteItemSelection, newItem: WasteItemSelection
            ) = oldItem.quantity == newItem.quantity
        }
    }
}