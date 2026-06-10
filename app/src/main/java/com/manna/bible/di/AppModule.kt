package com.manna.bible.di

import android.content.Context
import androidx.room.Room
import com.manna.bible.data.local.AnnotationDao
import com.manna.bible.data.local.MannaDatabase
import com.manna.bible.data.local.PendingDownloadDao
import com.manna.bible.data.local.TranslationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
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
     * The app's [MannaDatabase]. Uses destructive migration for now since no schema
     * has shipped yet (see the class doc on [MannaDatabase]).
     */
    @Provides
    @Singleton
    fun provideMannaDatabase(@ApplicationContext context: Context): MannaDatabase =
        Room.databaseBuilder(context, MannaDatabase::class.java, "manna.db")
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
}
