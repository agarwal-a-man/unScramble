# Retrofit official rules
-keepattributes Signature, InnerClasses, AnnotationDefault
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp official rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Serialization official rules
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
