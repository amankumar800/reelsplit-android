package com.reelsplit.presentation.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Brand Colors - Instagram-inspired gradient colors
// =============================================================================

// Primary Brand Colors (Instagram gradient: Purple to Pink to Orange)
val InstagramPurple = Color(0xFF833AB4)
val InstagramPink = Color(0xFFE1306C)
val InstagramOrange = Color(0xFFF77737)
val InstagramYellow = Color(0xFFFCAF45)

// Primary palette
val Primary = Color(0xFF6366F1)       // Indigo - modern and clean
val PrimaryVariant = Color(0xFF4F46E5) // Darker indigo
val PrimaryLight = Color(0xFF818CF8)   // Lighter indigo

// Secondary palette (Instagram pink-inspired)
val Secondary = Color(0xFFEC4899)       // Pink
val SecondaryVariant = Color(0xFFDB2777) // Darker pink
val SecondaryLight = Color(0xFFF472B6)   // Lighter pink

// Tertiary palette (Accent)
val Tertiary = Color(0xFFF97316)        // Orange
val TertiaryVariant = Color(0xFFEA580C)  // Darker orange
val TertiaryLight = Color(0xFFFB923C)    // Lighter orange

// =============================================================================
// Light Theme Colors
// =============================================================================

val LightBackground = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF0F172A)

val LightSurface = Color(0xFFF8FAFC)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurface = Color(0xFF1E293B)
val LightOnSurfaceVariant = Color(0xFF64748B)

val LightSurfaceContainer = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFDFDFD)
val LightSurfaceContainerHigh = Color(0xFFF8FAFC)
val LightSurfaceContainerHighest = Color(0xFFF1F5F9)

val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)

// =============================================================================
// Dark Theme Colors
// =============================================================================

val DarkBackground = Color(0xFF0F172A)
val DarkOnBackground = Color(0xFFF8FAFC)

val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)

val DarkSurfaceContainer = Color(0xFF1E293B)
val DarkSurfaceContainerLow = Color(0xFF172032)
val DarkSurfaceContainerHigh = Color(0xFF334155)
val DarkSurfaceContainerHighest = Color(0xFF475569)

val DarkOutline = Color(0xFF475569)
val DarkOutlineVariant = Color(0xFF334155)

// =============================================================================
// Semantic Colors
// =============================================================================

// Error colors
val ErrorLight = Color(0xFFEF4444)
val ErrorDark = Color(0xFFFF6B6B)
val OnError = Color.White
val ErrorContainerLight = Color(0xFFFEE2E2)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerLight = Color(0xFF991B1B)
val OnErrorContainerDark = Color(0xFFFECACA)

// Success colors
val Success = Color(0xFF22C55E)
val SuccessVariant = Color(0xFF16A34A)
val OnSuccess = Color.White
val SuccessContainerLight = Color(0xFFDCFCE7)
val SuccessContainerDark = Color(0xFF166534)

// Warning colors
val Warning = Color(0xFFF59E0B)
val WarningVariant = Color(0xFFD97706)
val OnWarning = Color.White
val WarningContainerLight = Color(0xFFFEF3C7)
val WarningContainerDark = Color(0xFF92400E)

// Info colors
val Info = Color(0xFF3B82F6)
val InfoVariant = Color(0xFF2563EB)
val OnInfo = Color.White
val InfoContainerLight = Color(0xFFDBEAFE)
val InfoContainerDark = Color(0xFF1E40AF)

// =============================================================================
// Status Colors (for download/processing states)
// =============================================================================

val StatusPending = Color(0xFF6B7280)
val StatusDownloading = Color(0xFF3B82F6)
val StatusProcessing = Color(0xFFF59E0B)
val StatusCompleted = Color(0xFF22C55E)
val StatusFailed = Color(0xFFEF4444)

// =============================================================================
// Gradient Colors (for Instagram-inspired gradients)
// =============================================================================

val GradientStart = InstagramPurple
val GradientMiddle = InstagramPink
val GradientEnd = InstagramOrange

// Progress bar gradient
val ProgressGradientStart = Primary
val ProgressGradientEnd = Secondary
