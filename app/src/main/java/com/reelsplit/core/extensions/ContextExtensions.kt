package com.reelsplit.core.extensions

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

/**
 * Extension functions for Android Context.
 */

/**
 * Checks if the device is currently in dark mode.
 * 
 * @return true if dark mode is enabled, false otherwise.
 */
fun Context.isDarkMode(): Boolean {
    val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Shows a toast message with a String.
 * 
 * @param message The message to display.
 * @param duration Toast duration, defaults to [Toast.LENGTH_SHORT].
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Shows a toast message with a String resource.
 * 
 * @param messageResId The string resource ID for the message.
 * @param duration Toast duration, defaults to [Toast.LENGTH_SHORT].
 */
fun Context.showToast(@StringRes messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageResId, duration).show()
}

/**
 * Gets a color resource with backward compatibility.
 * 
 * @param colorRes The color resource ID.
 * @return The resolved color as an Int.
 */
fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(this, colorRes)
}

/**
 * Gets a drawable resource with backward compatibility.
 * 
 * @param drawableRes The drawable resource ID.
 * @return The resolved Drawable, or null if not found.
 */
fun Context.getDrawableCompat(@DrawableRes drawableRes: Int): Drawable? {
    return ContextCompat.getDrawable(this, drawableRes)
}
