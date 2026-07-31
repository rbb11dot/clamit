# Gson model classes are deserialized reflectively — keep them intact in release builds.
-keep class com.clamit.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
