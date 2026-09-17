# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.neurasamu.build.solo_leveling_tasker.**$$serializer { *; }
-keepclassmembers class com.neurasamu.build.solo_leveling_tasker.** { *** Companion; }
