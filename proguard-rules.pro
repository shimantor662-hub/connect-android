# CONNECT Release ProGuard / R8 Rules
# Preserve Firestore model classes and @Keep members if release minification is enabled locally.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

-keep class com.example.models.** { *; }
-keepclassmembers class com.example.models.** { *; }
