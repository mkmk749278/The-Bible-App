package com.manna.bible.di

import com.manna.bible.data.AnnotationLocalDataSource
import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.AndroidConnectivityChecker
import com.manna.bible.data.TranslationLocalDataSource
import com.manna.bible.data.TranslationRemoteDataSource
import com.manna.bible.data.canon.AssetCanonDefinitionDataSource
import com.manna.bible.data.canon.CanonDefinitionDataSource
import com.manna.bible.data.download.AndroidDownloadForegroundController
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
import com.manna.bible.data.repository.DefaultPrayerRepository
import com.manna.bible.data.repository.DefaultSermonRepository
import com.manna.bible.data.repository.DefaultTranslationRepository
import com.manna.bible.audio.AndroidSpeechEngine
import com.manna.bible.data.audio.AndroidAudioForegroundController
import com.manna.bible.data.audio.DefaultNarratedAudioPlayer
import com.manna.bible.data.audio.HelloAoChapterAudioSource
import com.manna.bible.domain.attribution.AttributionProvider
import com.manna.bible.domain.audio.AudioForegroundController
import com.manna.bible.domain.audio.ChapterAudioSource
import com.manna.bible.domain.attribution.DefaultAttributionProvider
import com.manna.bible.domain.audio.DefaultTtsReader
import com.manna.bible.domain.audio.NarratedAudioPlayer
import com.manna.bible.domain.audio.SpeechEngine
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.domain.calendar.DefaultJesusEventsProvider
import com.manna.bible.domain.calendar.DefaultLectionaryReadingsProvider
import com.manna.bible.domain.calendar.DefaultLiturgicalCalendarProvider
import com.manna.bible.domain.calendar.JesusEventsProvider
import com.manna.bible.domain.calendar.LectionaryReadingsProvider
import com.manna.bible.domain.calendar.LiturgicalCalendarProvider
import com.manna.bible.data.explain.DefaultExplanationRepository
import com.manna.bible.data.explain.GeminiExplanationEngine
import com.manna.bible.data.explain.GeminiNanoExplanationEngine
import com.manna.bible.data.explain.HybridExplanationEngine
import com.manna.bible.domain.explain.ExplanationEngine
import com.manna.bible.domain.explain.ExplanationRepository
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.crisis.CrisisCompanion
import com.manna.bible.domain.crisis.DefaultCrisisCompanion
import com.manna.bible.domain.crisis.DefaultPersecutionCompanion
import com.manna.bible.domain.crisis.PersecutionCompanion
import com.manna.bible.domain.daily.DailyVerseProvider
import com.manna.bible.domain.devotion.DefaultJesusPrayerProvider
import com.manna.bible.domain.devotion.DefaultParalokaProvider
import com.manna.bible.domain.devotion.DefaultRosaryProvider
import com.manna.bible.domain.devotion.DefaultSramanikalJourney
import com.manna.bible.domain.devotion.DefaultStationsProvider
import com.manna.bible.domain.devotion.JesusPrayerProvider
import com.manna.bible.domain.devotion.ParalokaProvider
import com.manna.bible.domain.devotion.RosaryProvider
import com.manna.bible.domain.devotion.SramanikalJourney
import com.manna.bible.domain.devotion.StationsProvider
import com.manna.bible.domain.fasting.DefaultFastingPlans
import com.manna.bible.domain.fasting.FastingPlans
import com.manna.bible.domain.grief.DefaultGriefJourney
import com.manna.bible.domain.grief.GriefJourney
import com.manna.bible.domain.daily.DefaultDailyVerseProvider
import com.manna.bible.domain.canon.DefaultCanonEngine
import com.manna.bible.domain.topical.DefaultTopicalIndex
import com.manna.bible.domain.topical.TopicalIndex
import com.manna.bible.domain.download.DownloadForegroundController
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.lectionary.DefaultLectionaryProvider
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.liturgy.DefaultLiturgyProvider
import com.manna.bible.domain.liturgy.LiturgyProvider
import com.manna.bible.domain.repository.AnnotationRepository
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.PendingDownloadRepository
import com.manna.bible.domain.repository.PrayerRepository
import com.manna.bible.domain.repository.SermonRepository
import com.manna.bible.data.reminder.AlarmReminderScheduler
import com.manna.bible.domain.reminder.ReminderScheduler
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.share.BookNameProvider
import com.manna.bible.domain.share.DefaultBookNameProvider
import com.manna.bible.domain.translation.DefaultTranslationFilter
import com.manna.bible.domain.translation.TranslationFilter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
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

    @Binds
    abstract fun bindLiturgyProvider(impl: DefaultLiturgyProvider): LiturgyProvider

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

    // --- audio (narrated chapter source, Req 9.8) ----------------------------

    @Binds
    abstract fun bindChapterAudioSource(impl: HelloAoChapterAudioSource): ChapterAudioSource

    @Binds
    @Singleton
    abstract fun bindDownloadForegroundController(
        impl: AndroidDownloadForegroundController
    ): DownloadForegroundController

    @Binds
    @Singleton
    abstract fun bindNarratedAudioPlayer(impl: DefaultNarratedAudioPlayer): NarratedAudioPlayer

    @Binds
    @Singleton
    abstract fun bindAudioForegroundController(
        impl: AndroidAudioForegroundController
    ): AudioForegroundController

    // --- attribution ---------------------------------------------------------

    @Binds
    abstract fun bindAttributionProvider(impl: DefaultAttributionProvider): AttributionProvider

    // --- daily verse ---------------------------------------------------------

    @Binds
    abstract fun bindDailyVerseProvider(impl: DefaultDailyVerseProvider): DailyVerseProvider

    // --- topical search ------------------------------------------------------

    @Binds
    abstract fun bindTopicalIndex(impl: DefaultTopicalIndex): TopicalIndex

    // --- Jesus events calendar -----------------------------------------------

    @Binds
    abstract fun bindJesusEventsProvider(impl: DefaultJesusEventsProvider): JesusEventsProvider

    @Binds
    abstract fun bindLiturgicalCalendarProvider(
        impl: DefaultLiturgicalCalendarProvider
    ): LiturgicalCalendarProvider

    @Binds
    abstract fun bindLectionaryReadingsProvider(
        impl: DefaultLectionaryReadingsProvider
    ): LectionaryReadingsProvider

    // --- Explain this passage ------------------------------------------------
    // On-device Gemini Nano (offline) is preferred, the cloud Gemini Flash is the
    // fallback; HybridExplanationEngine orchestrates the two.

    @Binds
    @Named("nanoEngine")
    abstract fun bindNanoExplanationEngine(impl: GeminiNanoExplanationEngine): ExplanationEngine

    @Binds
    @Named("cloudEngine")
    abstract fun bindCloudExplanationEngine(impl: GeminiExplanationEngine): ExplanationEngine

    @Binds
    abstract fun bindExplanationEngine(impl: HybridExplanationEngine): ExplanationEngine

    @Binds
    abstract fun bindExplanationRepository(
        impl: DefaultExplanationRepository
    ): ExplanationRepository

    // --- crisis / 3AM mode ---------------------------------------------------

    @Binds
    abstract fun bindCrisisCompanion(impl: DefaultCrisisCompanion): CrisisCompanion

    @Binds
    abstract fun bindPersecutionCompanion(
        impl: DefaultPersecutionCompanion
    ): PersecutionCompanion

    // --- grief companion -----------------------------------------------------

    @Binds
    abstract fun bindGriefJourney(impl: DefaultGriefJourney): GriefJourney

    // --- prayer journal ------------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(impl: DefaultPrayerRepository): PrayerRepository

    // --- sermon helper -------------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindSermonRepository(impl: DefaultSermonRepository): SermonRepository

    // --- fasting companion ---------------------------------------------------

    @Binds
    abstract fun bindFastingPlans(impl: DefaultFastingPlans): FastingPlans

    // --- prayers hub (devotional practices) ----------------------------------

    @Binds
    abstract fun bindStationsProvider(impl: DefaultStationsProvider): StationsProvider

    @Binds
    abstract fun bindRosaryProvider(impl: DefaultRosaryProvider): RosaryProvider

    @Binds
    abstract fun bindJesusPrayerProvider(impl: DefaultJesusPrayerProvider): JesusPrayerProvider

    @Binds
    abstract fun bindParalokaProvider(impl: DefaultParalokaProvider): ParalokaProvider

    @Binds
    abstract fun bindSramanikalJourney(impl: DefaultSramanikalJourney): SramanikalJourney

    // --- daily reminder ------------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
}
