# Elite Memo Pro release rules.
# The app is fully self-contained (no reflection-based serialization),
# so no additional keep rules are currently required.

# Keep the SQLite database contract stable across obfuscation is not needed
# (constants are inlined at compile time), but keep crash reports readable.
-keepattributes SourceFile,LineNumberTable
