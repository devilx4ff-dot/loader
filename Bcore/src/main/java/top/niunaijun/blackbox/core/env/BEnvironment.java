package top.niunaijun.blackbox.core.env;

import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.Slog;


public class BEnvironment {
    private static final File sVirtualRoot = new File(BlackBoxCore.getContext().getCacheDir().getParent(), "blackbox");
    private static final File sExternalVirtualRoot = BlackBoxCore.getContext().getExternalFilesDir("blackbox");

    public static File JUNIT_JAR = new File(getCacheDir(), "junit.apk");
    public static File EMPTY_JAR = new File(getCacheDir(), "empty.apk");

    public static void load() {
        FileUtils.mkdirs(sVirtualRoot);
        FileUtils.mkdirs(sExternalVirtualRoot);
        FileUtils.mkdirs(getSystemDir());
        FileUtils.mkdirs(getCacheDir());
        FileUtils.mkdirs(getProcDir());

    }

    public static File getVirtualRoot() {
        return sVirtualRoot;
    }

    public static File getExternalVirtualRoot() {
        return sExternalVirtualRoot;
    }

    public static File getSystemDir() {
        return new File(sVirtualRoot, "system");
    }

    public static File getProcDir() {
        return new File(sVirtualRoot, "proc");
    }

    public static File getCacheDir() {
        return new File(sVirtualRoot, "cache");
    }

    public static File getUserInfoConf() {
        return new File(getSystemDir(), "user.conf");
    }

    public static File getAccountsConf() {
        return new File(getSystemDir(), "accounts.conf");
    }

    public static File getUidConf() {
        return new File(getSystemDir(), "uid.conf");
    }

    public static File getSharedUserConf() {
        return new File(getSystemDir(), "shared-user.conf");
    }

    public static File getXPModuleConf() {
        return new File(getSystemDir(), "xposed-module.conf");
    }

    public static File getFakeLocationConf() {
        return new File(getSystemDir(), "fake-location.conf");
    }

    public static File getPackageConf(String packageName) {
        return new File(getAppDir(packageName), "package.conf");
    }

    public static File getExternalUserDir(int userId) {
        return new File(sExternalVirtualRoot, String.format(Locale.CHINA, "storage/emulated/%d/", userId));
    }

    public static File getUserDir(int userId) {
        return new File(sVirtualRoot, String.format(Locale.CHINA, "data/user/%d", userId));
    }

    public static File getExternalObbDir(String packageName, int userId) {
        return new File(getExternalUserDir(userId), String.format(Locale.CHINA, "Android/obb/%s", packageName));
    }


    public static File getDeDataDir(String packageName, int userId) {
        return new File(sVirtualRoot, String.format(Locale.CHINA, "data/user_de/%d/%s", userId, packageName));
    }

    public static File getExternalDataDir(String packageName, int userId) {
        return new File(getExternalUserDir(userId), String.format(Locale.CHINA, "Android/data/%s", packageName));
    }


    public static File getDataDir(String packageName, int userId) {
        return new File(sVirtualRoot, String.format(Locale.CHINA, "data/user/%d/%s", userId, packageName));
    }

    public static File getProcDir(int pid) {
        File file = new File(getProcDir(), String.format(Locale.CHINA, "%d", pid));
        FileUtils.mkdirs(file);
        return file;
    }

    public static File getExternalDataFilesDir(String packageName, int userId) {
        return new File(getExternalDataDir(packageName, userId), "files");
    }

    public static File getDataFilesDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "files");
    }

    public static File getExternalDataCacheDir(String packageName, int userId) {
        return new File(getExternalDataDir(packageName, userId), "cache");
    }

    public static File getDataCacheDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "cache");
    }

    public static File getDataLibDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "lib");
    }

    public static File getDataDatabasesDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "databases");
    }

    public static File getAppRootDir() {
        return getAppDir("");
    }

    public static File getAppDir(String packageName) {
        return new File(sVirtualRoot, "data/app/" + packageName);
    }

    public static File getBaseApkDir(String packageName) {
        return new File(sVirtualRoot, "data/app/" + packageName + "/base.apk");
    }

    public static File getAppLibDir(String packageName) {
        return new File(getAppDir(packageName), "lib");
    }

    public static File getXSharedPreferences(String packageName, String prefFileName) {
       return new File(BEnvironment.getDataDir(packageName, BlackBoxCore.getUserId()), "shared_prefs/" + prefFileName + ".xml");
    }

    public interface ObbCopyListener {
        void onStart(int totalFiles, long totalBytes);
        void onProgress(int filesDone, long bytesDone, long totalBytes, String currentFile);
        void onDone(boolean success, int filesDone, long bytesDone);
    }

    /**
     * Copies the real OBB folder from /sdcard/Android/obb/<pkg>
     * into the virtual obb dir so the guest app can find it.
     * Runs on the calling thread. Pass null listener for silent copy.
     */
    public static void copyObb(String packageName, int userId, ObbCopyListener listener) {
        if (packageName == null || packageName.isEmpty()) {
            if (listener != null) listener.onDone(false, 0, 0);
            return;
        }

        File realObbDir = new File(Environment.getExternalStorageDirectory(),
                "Android/obb/" + packageName);

        if (!realObbDir.exists()) {
            Slog.d("BEnvironment", "copyObb: no real obb for " + packageName);
            if (listener != null) listener.onDone(true, 0, 0); // nothing to do — not an error
            return;
        }

        File virtualObbDir = getExternalObbDir(packageName, userId);
        if (!virtualObbDir.exists()) {
            virtualObbDir.mkdirs();
        }

        File[] files = realObbDir.listFiles();
        if (files == null || files.length == 0) {
            Slog.d("BEnvironment", "copyObb: real obb dir empty for " + packageName);
            if (listener != null) listener.onDone(true, 0, 0);
            return;
        }

        // compute totals
        long totalBytes = 0;
        int pendingFiles = 0;
        for (File f : files) {
            totalBytes += f.length();
            File out = new File(virtualObbDir, f.getName());
            if (!(out.exists() && out.length() == f.length())) {
                pendingFiles++;
            }
        }

        if (pendingFiles == 0) {
            Slog.d("BEnvironment", "copyObb: obb already in virtual dir for " + packageName);
            if (listener != null) listener.onDone(true, files.length, totalBytes);
            return;
        }

        Slog.d("BEnvironment", "copyObb: copying " + pendingFiles + " file(s) for " + packageName);
        if (listener != null) listener.onStart(files.length, totalBytes);

        byte[] buf = new byte[64 * 1024]; // 64k buffer — faster for big obb
        long bytesDone = 0;
        int filesDone = 0;

        for (File f : files) {
            File out = new File(virtualObbDir, f.getName());
            if (out.exists() && out.length() == f.length()) {
                filesDone++;
                bytesDone += f.length();
                if (listener != null) {
                    listener.onProgress(filesDone, bytesDone, totalBytes, f.getName());
                }
                continue;
            }

            try (FileInputStream in = new FileInputStream(f);
                 FileOutputStream os = new FileOutputStream(out)) {
                int n;
                long fileWritten = 0;
                long fileSize = f.length();
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    bytesDone += n;
                    fileWritten += n;
                    if (listener != null) {
                        listener.onProgress(filesDone + 1, bytesDone, totalBytes, f.getName());
                    }
                }
                filesDone++;
                Slog.d("BEnvironment", "copyObb: copied " + f.getName()
                        + " (" + fileSize + " bytes)");
            } catch (IOException e) {
                Slog.e("BEnvironment", "copyObb: failed to copy " + f.getAbsolutePath(), e);
                if (listener != null) listener.onDone(false, filesDone, bytesDone);
                return;
            }
        }

        Slog.d("BEnvironment", "copyObb: done for " + packageName
                + " (" + filesDone + "/" + files.length + ")");
        if (listener != null) listener.onDone(true, filesDone, bytesDone);
    }
}

