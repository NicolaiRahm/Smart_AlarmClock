# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Nicolai\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#TODO warum kann das nich renamen heißen??
-keep public class com.nicolai.alarm_clock.Friends{ *; }

-keep public class com.nicolai.alarm_clock.pojos.SharedEmpfaengerPOJO{ *; }
-keep public class com.nicolai.alarm_clock.pojos.SongPOJO{ *; }
-keep public class com.nicolai.alarm_clock.pojos.ShareContactsPOJO{ *; }
-keep public class com.nicolai.alarm_clock.pojos.SearchPOJO{ *; }

#TODO warum das auch nich??
-keep public class com.nicolai.alarm_clock.pojos.FriendRequestPOJO{ *; }
-keep public class com.nicolai.alarm_clock.pojos.FirebaseMessagePOJO{ *; }
-keep public class com.nicolai.alarm_clock.pojos.FirebaseAlarmPOJO{ *; }
-keep class com.nicolai.alarm_clock.**.model.** { *; }
-keep class androidx.core.app.CoreComponentFactory { *; }

#-keep public class com.nicolai.alarm_clock.pojos{ *; }
-keep public class androidx.renderscript.RenderScript{ *; }

-dontwarn org.checkerframework.checker.nullness.**
-dontwarn org.conscrypt.**

#Für Firebase Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception