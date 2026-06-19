package com.manna.bible.data.explain

import android.content.Context
import android.os.Build
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.domain.explain.ExplanationEngine
import com.manna.bible.domain.explain.ExplanationPrompt
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device [ExplanationEngine] backed by Gemini Nano via the AICore Prompt API
 * (Phase 3, FeatureFlags.GEMINI_NANO_AI). It runs fully offline on supported
 * devices (Android 14+ with AICore — e.g. recent Pixel / Galaxy), so a passage can
 * be explained with no network and no API key.
 *
 * It is intentionally conservative about *availability*:
 *  - [isConfigured] is false unless the feature flag is on **and** the device is new
 *    enough to host AICore (the library itself requires API 31; the on-device model
 *    requires more). On older devices the engine never touches AICore classes.
 *  - Any failure at inference time (model not downloaded, device unsupported, the
 *    AICore service missing) is mapped to [ExplanationUnavailableReason.ERROR] so the
 *    hybrid engine can fall back to the cloud rather than surfacing a crash.
 *
 * The [GenerativeModel] is created lazily and reused (it is expensive to spin up),
 * guarded by a mutex so concurrent explains share one instance.
 */
@Singleton
class GeminiNanoExplanationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : ExplanationEngine {

    private val mutex = Mutex()
    @Volatile private var model: GenerativeModel? = null

    /**
     * On-device generation is only attempted when the feature is enabled and the OS
     * can host AICore. Actual model readiness is resolved at call time (and falls back).
     */
    override val isConfigured: Boolean
        get() = FeatureFlags.GEMINI_NANO_AI && Build.VERSION.SDK_INT >= MIN_AICORE_SDK

    override suspend fun explain(request: ExplanationRequest): ExplanationResult {
        if (!isConfigured) {
            return ExplanationResult.Unavailable(ExplanationUnavailableReason.NOT_CONFIGURED)
        }
        return try {
            val response = model().generateContent(ExplanationPrompt.build(request))
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                ExplanationResult.Unavailable(ExplanationUnavailableReason.ERROR)
            } else {
                ExplanationResult.Success(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Model unavailable / not downloaded / device unsupported — let the caller
            // fall back to the cloud engine. Drop the cached model so a later attempt
            // re-initializes cleanly.
            runCatching { model?.close() }
            model = null
            ExplanationResult.Unavailable(ExplanationUnavailableReason.ERROR)
        }
    }

    private suspend fun model(): GenerativeModel = mutex.withLock {
        model ?: GenerativeModel(
            generationConfig = generationConfig {
                context = this@GeminiNanoExplanationEngine.context
                temperature = 0.3f
                topK = 16
                maxOutputTokens = 1024
            }
        ).also { model = it }
    }

    private companion object {
        /** AICore library floor; the on-device model needs Android 14+ in practice. */
        const val MIN_AICORE_SDK = Build.VERSION_CODES.S // API 31
    }
}
