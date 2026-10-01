# Add project specific ProGuard rules here.

# Keep Room entities and database classes
-keep class com.deepanjanxyz.notepad.data.local.database.** { 
    *; 
}

# Keep Model/Data classes for serialization
-keep class com.deepanjanxyz.notepad.data.model.** { 
    *; 
}

# Keep ViewModel classes to prevent stripping during R8 optimization
-keep class com.deepanjanxyz.notepad.ui.viewmodel.** { 
    *; 
}

# Retain generic signature info for reflection-heavy libraries
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
