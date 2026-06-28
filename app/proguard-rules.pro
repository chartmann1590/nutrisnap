# Keep kotlinx.serialization metadata for @Serializable DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * { @androidx.room.* <methods>; }

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# LiteRT-LM SDK (com.google.ai.edge.litertlm)
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# LiteRT native (deprecated sub-package, keep for safety)
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**

# Gson — LiteRT-LM parses JSON via Gson reflectively at runtime.
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keepattributes Signature, *Annotation*, EnclosingMethod

# Feedback / GitHub models (kotlinx.serialization)
-keep class com.charles.nutrisnap.data.feedback.** { *; }
-keepclassmembers class com.charles.nutrisnap.data.feedback.** { *; }
