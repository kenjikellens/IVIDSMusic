# Ktor & Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.kenjigames.ividsmusic.**$$serializer { *; }
-keepclassmembers class com.kenjigames.ividsmusic.** {
    *** Companion;
}
-keepclasseswithmembers class com.kenjigames.ividsmusic.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# NewPipeExtractor Rhino JS Engine
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# SQLDelight & SQLite
-keep class app.cash.sqldelight.** { *; }
-keep class org.sqlite.** { *; }

# Koin DI
-keep class org.koin.** { *; }
