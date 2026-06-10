package com.manna.bible.di

import com.manna.bible.data.AnnotationLocalDataSource
import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.AndroidConnectivityChecker
import com.manna.bible.data.TranslationLocalDataSource
import com.manna.bible.data.TranslationRemoteDataSource
import com.manna.bible.data.canon.AssetCanonDefinitionDataSource
import com.manna.bible.data.canon.CanonDefinitionDataSource
import com.manna.bible.data.local.RoomAnnotationLocalDataSource
import com.manna.bible.data.local.RoomTranslationLocalDataSource
import com.manna.bible.data.preferences.DataStorePreferencesStore
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.data.remote.StubTranslationRemoteDataSource
import com.manna.bible.data.repository.DefaultAnnotationRepository
import com.manna.bible.data.repository.DefaultPendingDownloadRepository
import com.manna.bible.data.repository.DefaultTranslationRepository
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.canon.DefaultCanonEngine
import com.manna.bible.domain.lectionary.DefaultLectionaryProvider
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.repository.AnnotationRepository
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

    @Binds
    @Singleton
    abstract fun bindTranslationRemoteDataSource(
        impl: StubTranslationRemoteDataSource
    ): TranslationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindConnectivityChecker(impl: AndroidConnectivityChecker): ConnectivityChecker

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
}
