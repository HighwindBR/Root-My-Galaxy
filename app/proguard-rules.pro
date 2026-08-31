# -----------------------------------------------------------------------
# Root-My-Galaxy ProGuard / R8 rules
# -----------------------------------------------------------------------

# Keep JNI entry points in native_probe.c.
# R8 cannot see native call-sites so these must be kept explicitly.
-keepclasseswithmembernames class dev.busung.s25uroot.NativeProbe {
    native <methods>;
}

# Keep Shizuku IPC binder stub. Shizuku calls into the app across
# process boundaries via reflection; the interface and its methods
# must not be renamed or removed.
-keep interface rikka.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }

# Standard Android component keeps (Activities, Services, etc.).
# The default proguard-android-optimize.txt already covers most of
# these, but list them explicitly for clarity.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Kotlin coroutines: keep the debug metadata that coroutines use for
# stack-trace recovery. Without this, coroutine exceptions lose their
# human-readable frames in release builds.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Compose runtime reflection used by the Compose tooling (debug only,
# but the keep is harmless in release and prevents occasional R8 warnings).
-dontwarn androidx.compose.ui.tooling.**

# Remove all logging in release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
