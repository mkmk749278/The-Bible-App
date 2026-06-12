package com.manna.bible.di

import android.content.Context
import androidx.room.Room
import com.manna.bible.data.local.AnnotationDao
import com.manna.bible.data.local.BibleContentDao
import com.manna.bible.data.local.MIGRATION_2_3
import com.manna.bible.data.local.MannaDatabase
import com.manna.bible.data.local.PendingDownloadDao
import com.manna.bible.data.local.TranslationDao
import com.manna.bible.data.remote.HelloAoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides framework-level singletons for the dependency graph: the JSON parser,
 * the Room database, and the DAOs derived from it.
 *
 * Interface-to-implementation wiring lives in [BindingsModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Lenient [Json] used to parse the bundled canon assets. Unknown keys are
     * ignored so additive schema changes don't break older builds.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Application-lifetime scope for background work that must survive screen
     * changes (e.g. Bible downloads). A [SupervisorJob] keeps one failing job from
     * cancelling the others; it is never cancelled (lives as long as the process).
     */
    @Provides
    @Singleton
    @DownloadScope
    fun provideDownloadScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * The app's [MannaDatabase]. Applies the additive [MIGRATION_2_3] (offline
     * content tables) and keeps destructive fallback as a last resort so an
     * unexpected schema mismatch never hard-crashes startup.
     */
    @Provides
    @Singleton
    fun provideMannaDatabase(@ApplicationContext context: Context): MannaDatabase =
        Room.databaseBuilder(context, MannaDatabase::class.java, "manna.db")
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTranslationDao(database: MannaDatabase): TranslationDao =
        database.translationDao()

    @Provides
    fun providePendingDownloadDao(database: MannaDatabase): PendingDownloadDao =
        database.pendingDownloadDao()

    @Provides
    fun provideAnnotationDao(database: MannaDatabase): AnnotationDao =
        database.annotationDao()

    @Provides
    fun provideBibleContentDao(database: MannaDatabase): BibleContentDao =
        database.bibleContentDao()

    // --- Free Use Bible API (helloao) networking -----------------------------

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(HELLO_AO_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideHelloAoApi(retrofit: Retrofit): HelloAoApi = retrofit.create(HelloAoApi::class.java)

    private const val HELLO_AO_BASE_URL = "https://bible.helloao.org/api/"
    private const val JSON_MEDIA_TYPE = "application/json"
}
