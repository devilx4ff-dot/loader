package top.niunaijun.blackbox.core;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.Slog;


/**
 * Uncaught exception handler installed in the guest process.
 *
 * gms's bgExecutor pool produces a "Cannot cast snb to auin/avcr"
 * ClassCastException during its init, before any hook we've installed
 * can intercept the bad object. the thread pool's own handler swallows
 * the stack, and the process dies — killing gms entirely.
 *
 * fix: catch the cast at this level, print the full stack for diagnostics,
 * then let the crashing THREAD die without killing the PROCESS. gms keeps
 * running minus one executor thread.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void create() {
        install();
    }

    public static void install() {
        try {
            Thread.UncaughtExceptionHandler existing = Thread.getDefaultUncaughtExceptionHandler();
            if (existing instanceof CrashHandler) {
                return;
            }
            Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
            Slog.e("CrashHandler", "=== CrashHandler installed in pid " + android.os.Process.myPid() + " ===");
        } catch (Throwable t) {
            Slog.e("CrashHandler", "install failed: " + t.getMessage());
        }
    }
    public CrashHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        // ── gms internal cast — swallow ──
        if (isGmsBadCast(e)) {
            Slog.e(TAG, "=== SWALLOWING gms bad cast on thread " + t.getName() + " ===");
            logFullStack(e);
            Slog.e(TAG, "=== END — process survives ===");
            // do NOT call the default handler. the process must keep running.
            return;
        }

        // ── other exceptions: pass through ──
        if (BlackBoxCore.get().getExceptionHandler() != null) {
            try {
                BlackBoxCore.get().getExceptionHandler().uncaughtException(t, e);
            } catch (Throwable ignored) { }
        }
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        }
    }

    private static boolean isGmsBadCast(Throwable e) {
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 5) {
            String msg = cur.getMessage();
            if (msg != null
                    && msg.contains("snb")
                    && (msg.contains("auin") || msg.contains("avcr"))) {
                return true;
            }
            cur = cur.getCause();
            depth++;
        }
        return false;
    }

    private static void logFullStack(Throwable e) {
        Slog.e(TAG, "Thread: " + Thread.currentThread().getName());
        Throwable cur = e;
        while (cur != null) {
            Slog.e(TAG, cur.getClass().getName() + ": " + cur.getMessage());
            for (StackTraceElement el : cur.getStackTrace()) {
                Slog.e(TAG, "  at " + el.toString());
            }
            cur = cur.getCause();
        }
    }
}