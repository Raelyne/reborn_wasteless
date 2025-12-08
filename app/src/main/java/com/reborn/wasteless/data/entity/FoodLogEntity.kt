package com.reborn.wasteless.data.entity

import com.reborn.wasteless.data.CalcType
import com.reborn.wasteless.data.WasteType

// Data/entity layer (Room entity, Firestore document, etc.)

//Entity for food waste logs
data class FoodLogEntity(
    val id: String = "",
    val date: Long,
    val title: String,
    val wasteType: WasteType,
    val wasteTypes: List<WasteType> = emptyList(),
    val calcType: CalcType,
    val totalWeight: Double = 0.0,     // calculated
    val remarks: String? = null,
    val imageUrl: String? = null,
    val items: List<LoggedWasteItem> = emptyList()   // e.g. "Fruit Peels, Weight, Qty. Refer to data class LoggedWasteItem"
)

data class LoggedWasteItem(
    val wasteItemId: String,
    val quantity: Double,
    val weight: Double,
    val wasteType: WasteType
)