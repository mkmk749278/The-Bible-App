import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.play.publisher)
    // Enables JUnit Platform launcher interceptors so the JUnit 5 Robolectric extension can
    // set up Robolectric's environment under the existing useJUnitPlatform() configuration.
    alias(libs.plugins.robolectric.junit5)
}

// Build-time secrets: read from the environment (CI injects them from GitHub
// Secrets) first, then fall back to local.properties for local development — so a
// developer can set GEMINI_API_KEY in local.properties (which is .gitignore'd) and
// the cloud "Explain" engine works in local debug builds, matching the docs.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun buildSecret(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: ""

android {
    namespace = "com.manna.bible"
    // NOTE: CLAUDE.md specifies compile/target SDK 36 (for Gemini Nano). We use 35
    // here for reliable CI builds with AGP 8.7.x. Bump to 36 when that toolchain
    // is stable and the on-device AI feature lands.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.manna.bible"
        minSdk = 26
        targetSdk = 35
        versionCode = 208
        versionName = "0.2.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Gemini API key for the cloud "Explain this passage" engine. From the GitHub
        // Secret (GEMINI_API_KEY) at CI time, or local.properties for dev. Blank by
        // default -> Explain falls back to on-device Nano / shows an "add a key" state.
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${buildSecret("GEMINI_API_KEY")}\""
        )
    }

    signingConfigs {
        create("release") {
            // All values supplied by GitHub Actions from GitHub Secrets at CI time.
            // This block is evaluated even when the release build type doesn't use it,
            // so guard against a blank KEYSTORE_PATH: file("") throws "path may not be
            // null or empty". A blank/unset path falls back to the default name (which
            // the release build type then treats as "no keystore" → unsigned build).
            val keystorePath = System.getenv("KEYSTORE_PATH")
                ?.takeIf { it.isNotBlank() }
                ?: "release-keystore.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign only when a real keystore is provided (CI release builds): the path
            // must be set, non-blank, AND point at a file that exists. Otherwise build
            // an unsigned release rather than failing on a missing/garbage keystore, so
            // the pipeline still produces an artifact and reports the cause clearly.
            signingConfig = System.getenv("KEYSTORE_PATH")
                ?.takeIf { it.isNotBlank() && file(it).exists() }
                ?.let { signingConfigs.getByName("release") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Hilt/Dagger 2.52 cannot read Kotlin 2.1's binary metadata version
        // (":app:hiltJavaCompileDebug > Unable to read Kotlin metadata due to
        // unsupported metadata version"). Emitting 2.0-era (1.9) metadata keeps
        // the annotation processor working without a toolchain upgrade. Remove
        // once Hilt is bumped to >= 2.54 (full Kotlin 2.1 metadata support).
        languageVersion = "1.9"
        apiVersion = "1.9"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
        unitTests.isReturnDefaultValues = true
        // Make merged Android resources and assets/ visible to JVM unit tests so
        // Robolectric-backed tests can resolve values-*/ string resources and read
        // bundled assets/liturgy/*.json the same way the app does at runtime.
        unitTests.isIncludeAndroidResources = true
    }
    lint {
        // Localization is incremental: a locale (e.g. values-ta) may translate the
        // UI chrome and core prayers while the longer devotional prose still falls
        // back to the English default until it has been reviewed by native speakers.
        // Don't fail the build on those intentional gaps.
        disable += "MissingTranslation"
    }
    // Disable Play Store language-based APK splitting so all locale string resources
    // (values-te/, values-ta/, values-hi/, values-ml/) are delivered to every device,
    // regardless of the device's system language. Without this, a device set to English
    // receives an APK with only the English resources, and the Bible-language prayer text
    // (e.g. Telugu prayers for a user whose phone is in English) falls back to English.
    bundle {
        language {
            enableSplit = false
        }
    }
}

// Gradle Play Publisher (com.github.triplet.play) — uploads the signed AAB to the
// Google Play Console (CD). Credentials are NOT configured here; the plugin reads the
// service-account JSON from the ANDROID_PUBLISHER_CREDENTIALS environment variable,
// which CI populates from the PLAY_STORE_SERVICE_ACCOUNT secret. Locally, publish
// tasks are simply unusable without that env var, while assemble/bundle stay unaffected.
//
// Defaults are deliberately safe: pushes to the `internal` testing track as a DRAFT,
// so a release is never auto-promoted to production. The app must already exist in the
// Play Console (created once via the web UI) AND have had at least one AAB uploaded
// manually — the Play Developer API cannot create a brand-new app listing.
play {
    track.set("internal")
    defaultToAppBundles.set(true)
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
    // Listing text/graphics stay managed manually (or via fastlane) for now, so the
    // publisher only touches the binary + release notes, never overwrites the listing.
    resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.IGNORE)
}

/**
 * Regenerates the committed offline Bible assets under `src/main/assets/bibles/`
 * from the Free Use Bible API. Developer/CI tool only — the assets are committed,
 * so it is NOT a dependency of `assembleDebug` and normal builds never hit the
 * network. Run manually: `./gradlew :app:prepareBundledBibles`.
 */
tasks.register<com.manna.bible.build.PrepareBundledBiblesTask>("prepareBundledBibles") {
    group = "manna"
    description = "Downloads the bundled offline Bibles into src/main/assets/bibles/."
    outputDir.set(layout.projectDirectory.dir("src/main/assets/bibles"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Extended Material icons (VolumeUp / StopCircle for the Oral AI speak control, F-02).
    // Version is managed by the Compose BOM above.
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Home-screen widget (Jetpack Glance — Compose for app widgets)
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Media3 (ExoPlayer) — streams human-narrated chapter audio (Req 9.8).
    implementation(libs.androidx.media3.exoplayer)

    // On-device Gemini Nano (AICore Prompt API) for offline "Explain this passage".
    // minSdk 31; gated at runtime + via tools:overrideLibrary so the app still ships
    // to API 26. Experimental artifact — pinned and used behind FeatureFlags.GEMINI_NANO_AI.
    implementation(libs.aicore)

    // Unit testing (JUnit 5 + Turbine + MockK) — runs in GitHub Actions CI.
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    // Property-based testing (Kotest property + matchers) used inside JUnit 5 tests.
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.assertions.core)

    // Robolectric + JUnit 5 extension so Android-resource / Compose-rendering tests
    // run on the JVM under useJUnitPlatform(). Compose UI-test artifacts are added to
    // the unit-test classpath (in addition to androidTest) so rendering property tests
    // can drive Compose under Robolectric.
    testImplementation(libs.robolectric)
    testImplementation(libs.junit5.robolectric.extension)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    // Instrumented / Compose UI tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


// Hang-safety (CI reliability): put a hard ceiling on every Test task so a stalled run
// (e.g. a Compose/Robolectric render whose waitForIdle never settles in a headless CI
// runner) fails fast with a diagnostic instead of wedging the Gradle build indefinitely.
// Per design.md > Testing Strategy > Hang-safety. Per-test @Timeout annotations on the
// remaining rendering tests provide the finer-grained, fail-fast bound, so this ceiling
// only needs to bound genuine hangs, not the legitimate full-suite runtime. 20m clipped
// a healthy CI run (no daemon, cold cache) before it finished; 40m gives headroom.
tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(40))
}
