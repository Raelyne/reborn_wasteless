package com.reborn.wasteless.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.view.updateLayoutParams

/**
 * Call this on Toolbar to prevent it from overlapping the Status Bar
 */
fun View.applyTopWindowInsets() {
    // Store the original fixed height defined in XML
    val originalHeight = this.layoutParams.height

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        //a. Expand the total height of the view to include the status bar height
        view.updateLayoutParams {
            // Add the system inset to the original layout height
            height = originalHeight + insets.top
        }

        //b. Add top padding to push the *content* down, away from the status bar area
        view.updatePadding(top = insets.top)

        // Consume the insets so child views don't apply them again unnecessarily
        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Call this on ScrollView/RecyclerView to ensure the last item
 * isn't hidden behind the Navigation Bar guh
 */
fun View.applyBottomWindowInsets() {
    val originalBottom = this.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(bottom = originalBottom + insets.bottom)
        windowInsets
    }
}