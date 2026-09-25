# Add project specific ProGuard rules here.

# Keep Room entities and database classes (core:database)
-keep class com.deepanjanxyz.notepad.core.database.** {
    *;
}

# Keep domain models (core:model) for serialization
-keep class com.deepanjanxyz.notepad.core.model.** {
    *;
}

# Keep the shared ViewModel / UI state (feature:notes) to prevent stripping
# during R8 optimization
-keep class com.deepanjanxyz.notepad.feature.notes.** {
    *;
}

# Retain generic signature info for reflection-heavy libraries
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
