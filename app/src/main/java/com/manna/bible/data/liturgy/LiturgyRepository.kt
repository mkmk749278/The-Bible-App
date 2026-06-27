package com.manna.bible.data.liturgy

import com.manna.bible.domain.liturgy.Liturgy
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the bundled liturgies once via [LiturgyAssetReader], caches the result in memory,
 * and exposes only the successfully-parsed orders — dropping any [LiturgyParseResult.Failure]
 * so a single malformed asset never removes the valid ones (Req 10.2).
 *
 * Fully offline: the reader performs no network request.
 */
@Singleton
class LiturgyRepository @Inject constructor(
    private val reader: LiturgyAssetReader
) {

    private val cached = AtomicReference<List<Liturgy>?>(null)

    /**
     * The valid bundled liturgies. Parsed on first call and cached for subsequent ones; the
     * cache is populated atomically so concurrent callers converge on a single parsed list.
     */
    suspend fun liturgies(): List<Liturgy> {
        cached.get()?.let { return it }
        val loaded = reader.readAll()
            .filterIsInstance<LiturgyParseResult.Success>()
            .map { it.liturgy }
        cached.compareAndSet(null, loaded)
        return cached.get() ?: loaded
    }
}
