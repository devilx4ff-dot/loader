package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * gms calls setApplicationLocales on the locale manager service. the host
 * uid doesn't hold CHANGE_CONFIGURATION, so the server denies and gms's
 * thread pool dies. short-circuit the whole interface.
 */
public class ILocaleManagerProxy extends BinderInvocationStub {
    public static final String TAG = "LocaleManagerProxy";
    private static final String NAME = "locale";

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService(NAME));
    }

    @Override
    protected Object getWho() {
        try {
            IBinder binder = BRServiceManager.get().getService(NAME);
            if (binder == null) return null;
            Class<?> stub = Class.forName("android.app.ILocaleManager$Stub");
            Method asInterface = stub.getMethod("asInterface", IBinder.class);
            return asInterface.invoke(null, binder);
        } catch (Throwable t) {
            Slog.w(TAG, "getWho failed: " + t.getMessage());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(NAME);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("setApplicationLocales")
    public static class SetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "LocaleManager: skipping setApplicationLocales");
            return null;
        }
    }

    @ProxyMethod("getApplicationLocales")
    public static class GetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Class<?> configClass = Class.forName("android.app.LocaleConfig");
                return null;   // gms handles null gracefully
            } catch (Throwable t) {
                return null;
            }
        }
    }

    @ProxyMethod("getSystemLocales")
    public static class GetSystemLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);   // safe, read-only
        }
    }
}