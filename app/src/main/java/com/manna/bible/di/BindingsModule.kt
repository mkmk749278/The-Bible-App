package com.manna.bible.di

import com.manna.bible.data.AnnotationLocalDataSource
import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.AndroidConnectivityChecker
import com.manna.bible.data.TranslationLocalDataSource
import com.manna.bible.data.TranslationRemoteDataSource
import com.manna.bible.data.canon.AssetCanonDefinitionDataSource
import com.manna.bible.data.canon.CanonDefinitionDataSource
import com.manna.bible.data.download.DefaultDownloadManager
import com.manna.bible.data.local.RoomAnnotationLocalDataSource
import com.manna.bible.data.local.RoomTranslationLocalDataSource
import com.manna.bible.data.preferences.DataStorePreferencesStore
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.data.remote.DefaultHelloAoRemoteDataSource
import com.manna.bible.data.remote.HelloAoRemoteDataSource
import com.manna.bible.data.repository.DefaultAnnotationRepository
import com.manna.bible.data.repository.DefaultBibleContentRepository
import com.manna.bible.data.repository.DefaultPendingDownloadRepository
import com.manna.bible.data.repository.DefaultTranslationRepository
import com.manna.bible.audio.AndroidSpeechEngine
import com.manna.bible.domain.audio.DefaultTtsReader
import com.manna.bible.domain.audio.SpeechEngine
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.canon.DefaultCanonEngine
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.lectionary.DefaultLectionaryProvider
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.repository.AnnotationRepository
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.PendingDownloadRepository
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.share.BookNameProvider
import com.manna.bible.domain.share.DefaultBookNameProvider
import com.manna.bible.domain.translation.DefaultTranslationFilter
import com.manna.bible.domain.translation.TranslationFilter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every interface in the denomination-aware-setup feature graph to its
 * concrete implementation.
 *
 * Singletons are used for stateful, expensive, or database-derived components
 * (data sources, repositories, the preferences store). Stateless pure-domain
 * helpers (canon engine, translation filter, lectionary provider, book-name
 * provider) are left unscoped — they are cheap to create and hold no state.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    // --- canon ---------------------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindCanonDefinitionDataSource(
        impl: AssetCanonDefinitionDataSource
    ): CanonDefinitionDataSource

    @Binds
    abstract fun bindCanonEngine(impl: DefaultCanonEngine): CanonEngine

    // --- translation / lectionary -------------------------------------------

    @Binds
    abstract fun bindTranslationFilter(impl: DefaultTranslationFilter): TranslationFilter

    @Binds
    abstract fun bindLectionaryProvider(impl: DefaultLectionaryProvider): LectionaryProvider

    // --- preferences ---------------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindPreferencesStore(impl: DataStorePreferencesStore): PreferencesStore

    // --- translation repository / data sources -------------------------------

    @Binds
    @Singleton
    abstract fun bindTranslationRepository(
        impl: DefaultTranslationRepository
    ): TranslationRepository

    @Binds
    @Singleton
    abstract fun bindPendingDownloadRepository(
        impl: DefaultPendingDownloadRepository
    ): PendingDownloadRepository

    @Binds
    @Singleton
    abstract fun bindTranslationLocalDataSource(
        impl: RoomTranslationLocalDataSource
    ): TranslationLocalDataSource

    // The Free Use Bible API backs both the catalog seam ([TranslationRemoteDataSource])
    // and the content-streaming seam ([HelloAoRemoteDataSource]); a single @Singleton
    // impl instance serves both interfaces.
    @Binds
    abstract fun bindTranslationRemoteDataSource(
        impl: DefaultHelloAoRemoteDataSource
    ): TranslationRemoteDataSource

    @Binds
    abstract fun bindHelloAoRemoteDataSource(
        impl: DefaultHelloAoRemoteDataSource
    ): HelloAoRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindConnectivityChecker(impl: AndroidConnectivityChecker): ConnectivityChecker

    // --- offline Bible content ----------------------------------------------

    @Binds
    @Singleton
    abstract fun bindBibleContentRepository(
        impl: DefaultBibleContentRepository
    ): BibleContentRepository

    @Binds
    abstract fun bindDownloadManager(impl: DefaultDownloadManager): DownloadManager

    // --- annotations ---------------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindAnnotationRepository(
        impl: DefaultAnnotationRepository
    ): AnnotationRepository

    @Binds
    @Singleton
    abstract fun bindAnnotationLocalDataSource(
        impl: RoomAnnotationLocalDataSource
    ): AnnotationLocalDataSource

    // --- share ---------------------------------------------------------------

    @Binds
    abstract fun bindBookNameProvider(impl: DefaultBookNameProvider): BookNameProvider

    // --- audio (offline TTS) -------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindTtsReader(impl: DefaultTtsReader): TtsReader

    @Binds
    @Singleton
    abstract fun bindSpeechEngine(impl: AndroidSpeechEngine): SpeechEngine
}
