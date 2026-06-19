plugins {
    `kotlin-dsl`
}

// buildSrc is an isolated build that provides custom Gradle tasks to the root
// project. It compiles separately from `:app` and never ships in the APK.
//
// The bundled-Bible generator (`PrepareBundledBiblesTask`) relies only on the
// JDK's HttpClient and Gradle's bundled Groovy JSON, so no extra dependencies
// are required here.
repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}
