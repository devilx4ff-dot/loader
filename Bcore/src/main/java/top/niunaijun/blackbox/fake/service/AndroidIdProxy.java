package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import java.security.MessageDigest;

import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


/**
 * Returns a STABLE per-guest ANDROID_ID instead of the host's.
 *
 * GMS keys its caches on ANDROID_ID. If the host's real id leaks through,
 * GMS wipes its own state on every check and refuses sign-in.
 *
 * The id is deterministic: md5("blackbox:" + packageName) truncated to 16
 * hex chars, same format as a real ANDROID_ID.
 */
public class AndroidIdProxy extends ClassInvocationStub {

    public static final String TAG = "AndroidIdProxy";

    public AndroidIdProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // target is a static class, hooked by method name below
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        Slog.d(TAG, "ANDROID_ID proxy installed");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    /**
     * Settings.Secure.getString(ContentResolver cr, String name)
     * Settings.Secure.getStringForUser(ContentResolver cr, String name, int userId)
     */
    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String id = interceptAndroidId(args);
            if (id != null) return id;
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String id = interceptAndroidId(args);
            if (id != null) return id;
            return method.invoke(who, args);
        }
    }

    private static String interceptAndroidId(Object[] args) {
        if (args == null || args.length < 2) return null;
        // arg[1] is the setting name (arg[0] is the ContentResolver)
        Object nameObj = args[1];
        if (!(nameObj instanceof String)) return null;

        String name = (String) nameObj;
        if (!"android_id".equals(name)) return null;

        String id = guestAndroidId();
        Slog.d(TAG, "intercept android_id → " + id);
        return id;
    }

    private static String guestAndroidId() {
        try {
            String pkg = BActivityThread.getAppPackageName();
            if (pkg == null || pkg.isEmpty()) pkg = "unknown";
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(("blackbox:" + pkg).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "0000000000000000";
        }
    }
}