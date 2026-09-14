package com.deepanjanxyz.notepad

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Centralised biometric / device-credential gating for the app.
 *
 * Encapsulates availability detection and prompt presentation so callers only
 * deal with three states and two callbacks. Biometrics and the device
 * credential (PIN / pattern / password) are both accepted, so the lock also
 * works on devices without a fingerprint sensor.
 */
class BiometricLockManager(private val activity: FragmentActivity) {

    /** Whether the app can be locked right now. */
    fun canLock(): Boolean = lockAvailability(activity) == LockAvailability.READY

    /**
     * Shows the system authentication prompt.
     *
     * @param onSuccess invoked once the user has authenticated.
     * @param onError invoked with the localized error message when
     * authentication fails or is cancelled; the caller should treat this as
     * a hard failure and close the protected surface.
     */
    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(promptInfo)
    }

    enum class LockAvailability { READY, NONE_ENROLLED, UNAVAILABLE }

    companion object {
        /**
         * Combining [BiometricManager.Authenticators.BIOMETRIC_STRONG] with
         * DEVICE_CREDENTIAL is rejected on API levels below 30, so WEAK is used
         * to support the full minSdk range.
         */
        private const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        fun lockAvailability(context: Context): LockAvailability =
            when (
                BiometricManager.from(context)
                    .canAuthenticate(AUTHENTICATORS)
            ) {
                BiometricManager.BIOMETRIC_SUCCESS -> LockAvailability.READY
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> LockAvailability.NONE_ENROLLED
                else -> LockAvailability.UNAVAILABLE
            }
    }
}
