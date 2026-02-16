package com.reelsplit.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive data using Android Keystore-backed encryption.
 *
 * Uses [EncryptedSharedPreferences] with AES-256 encryption for both keys and values.
 * The master key is stored in the Android Keystore, making it hardware-backed on
 * supported devices.
 *
 * Initialization is lazy to avoid crashing the app at construction time if the
 * Keystore is in a bad state. If initialization fails, a recovery is attempted
 * by clearing the corrupted prefs file and re-creating. All public methods
 * gracefully return defaults (null / no-op) if the backing store is unavailable.
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val encryptedPrefs: SharedPreferences? by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, attempting recovery")
            attemptRecovery()
        }
    }

    fun saveApiToken(token: String) {
        encryptedPrefs?.edit()?.putString(KEY_API_TOKEN, token)?.apply()
            ?: Timber.w("SecurePreferences unavailable – cannot save API token")
    }

    fun getApiToken(): String? {
        return encryptedPrefs?.getString(KEY_API_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        encryptedPrefs?.edit()?.putString(KEY_REFRESH_TOKEN, token)?.apply()
            ?: Timber.w("SecurePreferences unavailable – cannot save refresh token")
    }

    fun getRefreshToken(): String? {
        return encryptedPrefs?.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Clears all sensitive data from encrypted storage.
     * Call this on user logout or when security-critical events occur.
     */
    fun clearAll() {
        encryptedPrefs?.edit()?.clear()?.apply()
            ?: Timber.w("SecurePreferences unavailable – cannot clear data")
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Attempts recovery when the encrypted prefs file is corrupted or the
     * Keystore key is invalidated (e.g. after a system update or biometric change).
     *
     * Strategy: delete the prefs file + remove the master key alias, then retry.
     * All previously stored data will be lost, but the app won't crash.
     */
    private fun attemptRecovery(): SharedPreferences? {
        return try {
            // Delete the corrupted prefs file
            context.deleteSharedPreferences(PREFS_FILE_NAME)

            // Remove the master key from Keystore to force a fresh key
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (e: Exception) {
                Timber.w(e, "Could not remove old master key from Keystore")
            }

            createEncryptedPrefs().also {
                Timber.i("Recovery succeeded – recreated EncryptedSharedPreferences")
            }
        } catch (e: Exception) {
            Timber.e(e, "Recovery failed – SecurePreferences will be unavailable")
            null
        }
    }

    private companion object {
        const val PREFS_FILE_NAME = "secure_prefs"
        const val KEY_API_TOKEN = "api_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
