# Keep Firestore @Keep models across shared module
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.example.data.model.** { *; }
