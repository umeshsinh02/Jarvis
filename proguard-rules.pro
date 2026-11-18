# Retrofit & Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jarvis.assistant.data.remote.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Google Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn io.ktor.**

# Picovoice Porcupine
-keep class ai.picovoice.porcupine.** { *; }
