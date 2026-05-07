-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase Firestore
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase — păstrează modelele de date serializate de Firestore
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Room — păstrează entitățile și DAO-urile
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# TFLite / LiteRT
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.android.gms.tflite.** { *; }
-dontwarn org.tensorflow.**

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Enums (needed for Kotlin sealed classes / enums serialized)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}