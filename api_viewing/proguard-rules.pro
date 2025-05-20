
# Xiaomi push detection
-keep class com.xiaomi.mipush.sdk.m {
 void d(...);
}
# keep the class and specified members from being renamed only
# classes in the sdk are obfuscated already
# those that are not are APIs and should be kept
-keepnames class com.xiaomi.** { *; }
