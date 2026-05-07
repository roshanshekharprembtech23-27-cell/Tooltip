# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Gson uses generic type information stored in a class file when working
# with fields. Proguard removes such information by default, so configure
# it to keep all of it.
-keepattributes Signature

# Keep Gson TypeToken and its sub-classes
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep our tooltip model classes (Gson needs them at runtime)
-keep class com.example.tooltipguide.models.** { *; }
