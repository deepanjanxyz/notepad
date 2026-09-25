package com.deepanjanxyz.notepad.data.setup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.setupDataStore by preferencesDataStore("notepad_setup")

class EncryptedSetupStore(private val context: Context) {
    private val payloadKey = stringPreferencesKey("encrypted_setup")

    val state: Flow<SetupState> = context.setupDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            preferences[payloadKey]?.let(::decode) ?: SetupState(false, null)
        }

    suspend fun save(config: BackendConfig) {
        context.setupDataStore.edit { it[payloadKey] = encode(config) }
    }

    private fun encode(config: BackendConfig): String {
        val json = JSONObject()
            .put("type", config.type.name)
            .put("url", config.serverUrl)
            .put("key", config.apiKey)
            .put("passphrase", config.passphrase)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decode(value: String): SetupState = runCatching {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, bytes.copyOfRange(0, 12))
        )
        val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        SetupState(
            completed = true,
            config = BackendConfig(
                type = BackendType.valueOf(json.getString("type")),
                serverUrl = json.optString("url").takeIf { it.isNotBlank() && it != "null" },
                apiKey = json.optString("key").takeIf { it.isNotBlank() && it != "null" },
                passphrase = json.optString("passphrase").takeIf { it.isNotBlank() && it != "null" }
            )
        )
    }.getOrElse { SetupState(false, null) }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object { const val KEY_ALIAS = "notepad_setup_key" }
}
