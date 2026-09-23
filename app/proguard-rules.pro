# Add project specific ProGuard rules here.

# Tink (via androidx.security:security-crypto) references error-prone
# annotations that are compile-time only and not shipped on the runtime
# classpath. Safe to ignore: R8 suggests this exact rule.
-dontwarn com.google.errorprone.annotations.**
