# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room entities/DAOs are referenced reflectively in generated code.
-keep class com.manna.bible.data.local.** { *; }

# Retrofit
-keepattributes Signature, Exceptions
-keep,allowobfuscation interface * { @retrofit2.http.* <methods>; }
