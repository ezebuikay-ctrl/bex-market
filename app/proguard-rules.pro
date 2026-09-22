# Standard Android / Obfuscation settings
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# Obfuscation intensity (Optional: remove if you want full obfuscation)
# -repackageclasses ''
# -allowaccessmodification

# Appwrite SDK
-keep class io.appwrite.** { *; }
-keep interface io.appwrite.** { *; }
-dontwarn io.appwrite.**

# Networking: OkHttp3
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded at runtime for some 3rd party signing libs
-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    private byte[] publicSuffixListBytes;
    private byte[] publicSuffixExceptionListBytes;
}

# Networking: Retrofit 2
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowoptimization class * {
    @retrofit2.http.* <methods>;
}

# JSON Parsing: Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Data Models (Specific to your app)
# Ensure your marketplace and user models aren't stripped or renamed
-keep class com.bexmarket.ng.app.MarketItem { *; }
-keep class com.bexmarket.ng.app.UserProfile { *; }
-keep class com.bexmarket.ng.app.AuthStatus { *; }
-keep class com.bexmarket.ng.app.MarketUiState { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

# Firebase & Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Jetpack Compose & Material3
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# Android Lifecycle & ViewModel
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Coil (Image Loading)
-keep class coil.** { *; }
-dontwarn coil.**

# App Credentials & Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Security Utils (Protect specific logic)
-keepclassmembers class com.bexmarket.ng.app.SecurityUtils {
    private static final java.lang.String OFFICIAL_SIGNATURE_HASH;
}

# Explicit rules for Appwrite, OkHttp, and Retrofit to prevent release crashes
-keep class io.appwrite.** { *; }
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
