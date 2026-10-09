package top.niunaijun.blackbox.fake.service.context.providers;

import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.IInterface;

import java.lang.reflect.Method;
import java.security.MessageDigest;

import black.android.content.BRAttributionSource;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ContextCompat;


public class SystemProviderStub extends ClassInvocationStub implements BContentProvider {
    public static final String TAG = "SystemProviderStub";
    private IInterface mBase;

    @Override
    public IInterface wrapper(IInterface contentProviderProxy, String appPkg) {
        mBase = contentProviderProxy;
        injectHook();
        return (IInterface) getProxyInvocation();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // nothing — the proxy is installed by ContentProviderDelegate via field swap
    }

    @Override
    protected void onBindMethod() {
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("asBinder".equals(method.getName())) {
            return method.invoke(mBase, args);
        }

        String methodName = method.getName();

        // ── android_id intercept ──
        if ("call".equals(methodName) || "query".equals(methodName)) {
            Object id = maybeReturnAndroidId(args);
            if (id != null) {
                return id;
            }
        }

        if ("call".equals(methodName)) {
            if (args != null) {
                Class<?> attributionSourceClass = BRAttributionSource.getRealClass();
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg != null && attributionSourceClass != null &&
                            arg.getClass().getName().equals(attributionSourceClass.getName())) {
                        ContextCompat.fixAttributionSourceState(arg, BlackBoxCore.getHostUid());
                    }
                }
            }
            return method.invoke(mBase, args);
        }

        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof String) {
                String authority = (String) arg;
                if (!isSystemProviderAuthority(authority)) {
                    args[0] = BlackBoxCore.getHostPkg();
                }
            } else if (arg != null) {
                Class<?> attrSourceClass = BRAttributionSource.getRealClass();
                if (attrSourceClass != null && arg.getClass().getName().equals(attrSourceClass.getName())) {
                    ContextCompat.fixAttributionSourceState(arg, BlackBoxCore.getHostUid());
                }
            }
        }
        return method.invoke(mBase, args);
    }

    private Object maybeReturnAndroidId(Object[] args) {
        if (args == null || args.length < 2) return null;

        // ContentProvider.call(method, arg, extras)
        if (args.length >= 2 && args[0] instanceof String && args[1] instanceof String) {
            String callMethod = (String) args[0];
            String key = (String) args[1];
            if ("android_id".equals(key)
                    && (callMethod.startsWith("GET_") || callMethod.equals("getString"))) {
                String id = guestAndroidId();
                Slog.d(TAG, "android_id via call() -> " + id);
                Bundle result = new Bundle();
                result.putString("value", id);
                result.putString("_value", id);
                result.putString("android_id", id);
                return result;
            }
        }

        // ContentProvider.query(uri, projection, selection, selectionArgs, sortOrder)
        if (args.length >= 4 && args[0] != null
                && args[0].getClass().getName().equals("android.net.Uri")
                && args[2] instanceof String
                && args[3] instanceof String[]) {
            String selection = (String) args[2];
            String[] selArgs = (String[]) args[3];
            if (selection != null && selection.contains("name")
                    && selArgs.length > 0 && "android_id".equals(selArgs[0])) {
                String id = guestAndroidId();
                Slog.d(TAG, "android_id via query() -> " + id);
                MatrixCursor cursor = new MatrixCursor(new String[]{"value"});
                cursor.addRow(new Object[]{id});
                return cursor;
            }
        }

        return null;
    }

    private boolean isSystemProviderAuthority(String authority) {
        if (authority == null) return false;
        return authority.equals("settings")
                || authority.equals("media")
                || authority.equals("downloads")
                || authority.equals("contacts")
                || authority.equals("call_log")
                || authority.equals("telephony")
                || authority.equals("calendar")
                || authority.equals("browser")
                || authority.equals("user_dictionary")
                || authority.equals("applications")
                || authority.startsWith("com.android.")
                || authority.startsWith("android.");
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