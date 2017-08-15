# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/sdk/SDK/tools/proguard/proguard-android.txt
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
#指定压缩级别
-optimizationpasses 5
-dontusemixedcaseclassnames

-verbose
#混淆时采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-ignorewarnings



-keep class com.wilddog.video.**{*;}
-keep class com.wilddog.client.**{*;}
-keep class com.wilddog.wilddogcore.**{*;}

-keep class org.webrtc.**{*;}
-keep class org.java_websocket.**{*;}

-keep class io.socket.**{*;}
-keep class io.socket.engineio.**{*;}

-keep class okhttp3.**{*;}
-keep class okhttp3.internal.**{*;}


-dontshrink


-dontwarn org.json.**
-dontwarn com.wilddog.video.**
-dontwarn okhttp3.internal.**