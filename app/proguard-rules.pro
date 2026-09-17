# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# AdMob / Google Play Services
# ============================================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep AdMob classes
-keep class * extends com.google.android.gms.ads.mediation.MediationAdapter
-keep class * extends com.google.android.gms.ads.mediation.MediationServerParameters
-keep class * extends com.google.android.gms.ads.mediation.customevent.CustomEvent

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# ============================================
# Play Store Requirements
# ============================================

# Keep activity classes
-keep class com.cash.guide.MainActivity { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R class
-keep class com.cash.guide.R$* { *; }
-keep class **.R$* { *; }

# Keep View constructors for Compose
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep all model classes (for data classes)
-keep class com.cash.guide.domain.** { *; }
-keep class com.cash.guide.data.** { *; }

# Keep Kotlin data classes
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin reflection
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }

# Keep AppCompat and Material Components
-keep class androidx.appcompat.** { *; }
-keep class com.google.android.material.** { *; }

# Keep Lifecycle components
-keep class androidx.lifecycle.** { *; }

# Keep DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }

# Play Store: Keep all UI-related classes for Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }