package com.reborn.wasteless.ui.logging

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.reborn.wasteless.data.CalcType
import com.reborn.wasteless.data.WasteType
import com.reborn.wasteless.repo.LogRepository
import com.reborn.wasteless.data.entity.LoggedWasteItem
import com.reborn.wasteless.data.entity.FoodLogEntity
import com.reborn.wasteless.data.WasteItem
import com.reborn.wasteless.data.WasteItemsRepository
import com.reborn.wasteless.data.model.WasteItemSelection

class LoggingViewModel(
    private val logRepository: LogRepository = LogRepository()
) : ViewModel() {

    // UI state
    val dateTime = MutableLiveData<Long>(System.currentTimeMillis())
    val title = MutableLiveData<String>("")
    val wasteType = MutableLiveData<WasteType>(WasteType.AVOIDABLE)
    val calcType = MutableLiveData<CalcType>(CalcType.PORTION)
    val remarks = MutableLiveData<String>("")
    val imageUri = MutableLiveData<Uri?>(null)

    // Item selections kept per waste type so switching tabs does not lose input
    private val _selectionsByType = MutableLiveData<Map<WasteType, List<WasteItemSelection>>>(buildInitialSelections())
    private val _selections = MediatorLiveData<List<WasteItemSelection>>().apply {
        fun updateCurrentList() {
            val currentType = wasteType.value ?: WasteType.AVOIDABLE
            value = _selectionsByType.value.orEmpty()[currentType].orEmpty()
        }
        addSource(wasteType) { updateCurrentList() }
        addSource(_selectionsByType) { updateCurrentList() }
    }
    val selections: LiveData<List<WasteItemSelection>> = _selections

    fun updateQuantity(item: WasteItem, qty: Double) {
        val currentType = wasteType.value ?: WasteType.AVOIDABLE
        val currentMap = _selectionsByType.value.orEmpty()
        val updatedList = currentMap[currentType].orEmpty()
            .map { sel -> if (sel.item == item) sel.copy(quantity = qty) else sel }
        _selectionsByType.value = currentMap + (currentType to updatedList)
    }

    // Compute total weight live across all waste types
    val totalWeight: LiveData<Double> = MediatorLiveData<Double>().apply {
        fun recalc() {
            val allSelections = _selectionsByType.value.orEmpty().values.flatten()
            val ct = calcType.value ?: CalcType.PORTION
            value = allSelections.sumOf { sel ->
                if (ct == CalcType.GRAMS) sel.quantity
                else sel.quantity * sel.item.portionWeight
            }
        }
        addSource(_selectionsByType) { recalc() }
        addSource(calcType) { recalc() }
    }

    // Save‐status back to the Fragment
    private val _saveStatus = MutableLiveData<Result<Void>>()
    val saveStatus: LiveData<Result<Void>> = _saveStatus

    /** Upload image, then write one Firestore doc per item with qty>0 */
    fun saveAll(context: Context) {
        val dt = dateTime.value ?: return
        val t = title.value.orEmpty()
        val type = wasteType.value ?: return
        val rem = remarks.value
        val uri = imageUri.value

        val itemsLog = _selectionsByType.value.orEmpty()
            .values
            .flatten()
            .filter { it.quantity > 0}
            .map { sel ->
                val wt = if (calcType.value == CalcType.GRAMS)
                    sel.quantity
                else
                    sel.quantity * sel.item.portionWeight

                LoggedWasteItem(
                    wasteItemId = sel.item.name,
                    quantity = sel.quantity,
                    weight = wt
                )
            }

        val totalWt = itemsLog.sumOf { it.weight }

        fun commit(imageUrl: String?) {
            val session = FoodLogEntity(
                id = dt.toString(),
                date = dt,
                title = t,
                wasteType = type,
                calcType = calcType.value ?: CalcType.PORTION,
                totalWeight = totalWt,
                remarks = rem,
                imageUrl = imageUrl,
                items = itemsLog
            )
            logRepository.saveLog(session)
                .addOnSuccessListener { _saveStatus.value = Result.success(it) }
                .addOnFailureListener { _saveStatus.value = Result.failure(it) }
        }

        if (uri != null) {
            logRepository.uploadImage(uri)
                .addOnSuccessListener { url -> commit(url) }
                .addOnFailureListener { e -> _saveStatus.value = Result.failure(e) }
        } else {
            commit(null)
        }
    }

    private fun buildInitialSelections(): Map<WasteType, List<WasteItemSelection>> =
        WasteType.entries.associateWith { type ->
            WasteItemsRepository.getItemsForWasteType(type)
                .map { WasteItemSelection(it, quantity = 0.0) }
        }
}