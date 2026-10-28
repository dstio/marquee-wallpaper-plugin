# kotlinx-serialization, OkHttp and WorkManager ship their own R8 rules; the manifest's service,
# provider and activity are kept by the build.

# Keep line numbers in a release stack trace, under one file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager makes its Room database and the workers by reflection; R8's full mode keeps no
# constructor the rules do not name, and a release crashed at start without these.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.InputMerger { public <init>(); }

# The wallpaper API's parcelables cross the binder to the launcher: their CREATORs are read by name.
-keep class ca.dstudio.atvlauncher.wallpaper.api.** implements android.os.Parcelable {
    public static final ** CREATOR;
}
