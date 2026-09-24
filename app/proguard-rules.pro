# Add project specific ProGuard rules here.

# kotlinx-serialization: the compiler plugin generates serializer() methods
# that are looked up at runtime; keep them (and @Serializable classes' names)
# so minified release builds can still encode/decode the sync protocol.
# See https://github.com/Kotlin/kotlinx.serialization#androidproguard--r8
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepnames class * {
    static *** Companion;
}
-keepclasseswithmembernames class * {
    kotlinx.serialization.KSerializer serializer(...);
}
