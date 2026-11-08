# 保留 JavaMail 相关类
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class myjava.awt.datatransfer.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
