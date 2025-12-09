package com.reborn.wasteless.data.mappers

import com.reborn.wasteless.data.entity.FoodLogEntity
import com.reborn.wasteless.data.model.FoodLogSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Map FoodLogEntity items into FoodLogSummary
fun FoodLogEntity.toSummary(): FoodLogSummary =
    FoodLogSummary(
        title = title,
        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date)),
        wasteType = when {
            wasteTypes.size > 1 -> "Mixed"
            wasteTypes.size == 1 -> wasteTypes.first().displayName
            else -> wasteType.displayName
        },
        totalWeight = "%.0f g".format(totalWeight),
        imageUrl = imageUrl
    )