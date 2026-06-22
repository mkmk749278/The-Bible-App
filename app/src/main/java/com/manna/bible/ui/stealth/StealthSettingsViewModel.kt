package com.manna.bible.ui.stealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.data.stealth.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Minimum PIN length accepted when arming Stealth Mode. */
const val STEALTH_MIN_PIN_LENGTH = 4

/**
 * UI state for the Stealth (Persecution) Mode settings.
 *
 * @property enabled Whether Stealth Mode is currently armed.
 * @property message A transient, already-resolved status/error message to show, or null.
 */
data class StealthSettingsUiState(
    val enabled: Boolean = false,
    val message: StealthMessage? = null,
)

/** Resolved, localizable outcomes the screen renders as a short status line. */
enum class StealthMessage {
    ENABLED, DISABLED, PIN_CHANGED, PIN_TOO_SHORT, PIN_MISMATCH, WRONG_PIN,
}

/**
 * Backs the Stealth (Persecution) Mode settings: arm the mode with a PIN, change the
 * PIN, or disarm it (which requires the current PIN). The PIN is hashed with PBKDF2
 * via [PinHasher] before it ever reaches storage — the plaintext is never persisted.
 */
@HiltViewModel
class StealthSettingsViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore,
) : ViewModel() {

    private val messageFlow = kotlinx.coroutines.flow.MutableStateFlow<StealthMessage?>(null)

    @Volatile
    private var credential: String = ""

    init {
        viewModelScope.launch {
            preferencesStore.stealthPinCredential.collectLatest { credential = it }
        }
    }

    val uiState: StateFlow<StealthSettingsUiState> =
        combine(preferencesStore.stealthEnabled, messageFlow) { enabled, message ->
            StealthSettingsUiState(enabled = enabled, message = message)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StealthSettingsUiState(),
        )

    /**
     * Arms Stealth Mode with [pin] (confirmed by [confirm]). Rejects PINs shorter than
     * [STEALTH_MIN_PIN_LENGTH] or a mismatched confirmation without persisting anything.
     */
    fun enable(pin: String, confirm: String) {
        when {
            pin.length < STEALTH_MIN_PIN_LENGTH -> messageFlow.value = StealthMessage.PIN_TOO_SHORT
            pin != confirm -> messageFlow.value = StealthMessage.PIN_MISMATCH
            else -> viewModelScope.launch {
                preferencesStore.setStealthCredential(PinHasher.hash(pin))
                messageFlow.value = StealthMessage.ENABLED
            }
        }
    }

    /** Disarms Stealth Mode, but only when [pin] matches the current PIN. */
    fun disable(pin: String) {
        if (!PinHasher.verify(pin, credential)) {
            messageFlow.value = StealthMessage.WRONG_PIN
            return
        }
        viewModelScope.launch {
            preferencesStore.clearStealth()
            messageFlow.value = StealthMessage.DISABLED
        }
    }

    /** Replaces the PIN: requires the [current] PIN, then sets [new] (confirmed by [confirm]). */
    fun changePin(current: String, new: String, confirm: String) {
        when {
            !PinHasher.verify(current, credential) -> messageFlow.value = StealthMessage.WRONG_PIN
            new.length < STEALTH_MIN_PIN_LENGTH -> messageFlow.value = StealthMessage.PIN_TOO_SHORT
            new != confirm -> messageFlow.value = StealthMessage.PIN_MISMATCH
            else -> viewModelScope.launch {
                preferencesStore.setStealthCredential(PinHasher.hash(new))
                messageFlow.value = StealthMessage.PIN_CHANGED
            }
        }
    }

    /** Clears any shown status message (e.g. after the user reads it). */
    fun clearMessage() {
        messageFlow.value = null
    }
}
