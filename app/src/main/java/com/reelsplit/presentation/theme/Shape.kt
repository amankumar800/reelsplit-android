package com.reelsplit.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape definitions for ReelSplit app following Material 3 guidelines.
 * 
 * Shape scale:
 * - None: 0dp (sharp corners)
 * - ExtraSmall: 4dp
 * - Small: 8dp
 * - Medium: 12dp
 * - Large: 16dp
 * - ExtraLarge: 28dp
 */
val Shapes = Shapes(
    // ExtraSmall - for small components like chips
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - for buttons, text fields, snackbars
    small = RoundedCornerShape(8.dp),
    
    // Medium - for cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - for large cards, navigation drawers
    large = RoundedCornerShape(16.dp),
    
    // ExtraLarge - for bottom sheets, large surfaces
    extraLarge = RoundedCornerShape(28.dp)
)

// =============================================================================
// Custom Shapes for specific components
// =============================================================================

/**
 * Shape for video thumbnail cards
 */
val VideoCardShape = RoundedCornerShape(16.dp)

/**
 * Shape for video player controls
 */
val VideoControlsShape = RoundedCornerShape(24.dp)

/**
 * Shape for progress indicators
 */
val ProgressBarShape = RoundedCornerShape(4.dp)

/**
 * Shape for FAB (Floating Action Button)
 */
val FabShape = RoundedCornerShape(16.dp)

/**
 * Shape for bottom navigation bar
 */
val BottomNavShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

/**
 * Shape for top app bar
 */
val TopAppBarShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp
)

/**
 * Shape for bottom sheet
 */
val BottomSheetShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

/**
 * Shape for video segment cards
 */
val SegmentCardShape = RoundedCornerShape(12.dp)

/**
 * Shape for action buttons (share, download, etc.)
 */
val ActionButtonShape = RoundedCornerShape(12.dp)

/**
 * Shape for status badges
 */
val BadgeShape = RoundedCornerShape(8.dp)

/**
 * Pill shape for tags, chips, and circular components.
 * This creates a fully rounded shape regardless of size.
 * Use this for circular buttons, avatars, or pill-shaped chips.
 */
val PillShape = RoundedCornerShape(percent = 50)
