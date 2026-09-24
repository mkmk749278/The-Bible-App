package com.manna.bible.ui.stealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.data.stealth.PinHasher
import com.manna.bible.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Backs the disguised Stealth-Mode lock screen. Holds the latest persisted PIN
 * credential so an entered PIN can be checked offline, with no hint as to whether a
 * lock even exists (the screen looks like an ordinary calculator).
 */
@HiltViewModel
class StealthLockViewModel @Inject constructor(
    preferencesStore: PreferencesStore,
    @DefaultDispatcher private val cryptoDispatcher: CoroutineDispatcher,
) : ViewModel() {

    @Volatile
    private var credential: String = ""

    init {
        viewModelScope.launch {
            preferencesStore.stealthPinCredential.collectLatest { credential = it }
        }
    }

    /**
     * True when [pin] matches the stored credential — the cue to reveal the app. The
     * PBKDF2 derivation runs on [cryptoDispatcher], never the main thread, so the keypad
     * stays responsive on low-end phones.
     */
    suspend fun check(pin: String): Boolean {
        val stored = credential
        if (stored.isBlank()) return false
        return withContext(cryptoDispatcher) { PinHasher.verify(pin, stored) }
    }
}
