# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-printmapping build/mapping.txt
-android
-dontpreverify
-allowaccessmodification
-optimizations !code/simplification/arithmetic
-keepattributes SourceFile,LineNumberTable,Exception,*Annotation*
-dontobfuscate

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepclassmembers class * {
    @com.google.api.client.util.Key *;
}
