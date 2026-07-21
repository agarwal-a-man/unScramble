# =============================================================================
# OkHttp3
# =============================================================================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# =============================================================================
# Kotlin Serialization
# NOTE: In R8, only the LAST -keepattributes directive takes effect,
# so all required attributes must be on a single line.
# =============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keep @kotlinx.serialization.Serializable class ** { *; }

# =============================================================================
# Hilt / Dagger
# =============================================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# =============================================================================
# App-specific: keep repository implementations (Hilt @Binds target)
# =============================================================================
-keep class com.amanagarwal.unscramble.data.NetworkWordsRepository { *; }
-keep interface com.amanagarwal.unscramble.data.WordsRepository { *; }
-keep interface com.amanagarwal.unscramble.network.WordsApiService { *; }
