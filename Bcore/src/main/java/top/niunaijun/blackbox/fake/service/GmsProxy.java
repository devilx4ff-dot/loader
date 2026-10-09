package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";

    public GmsProxy() {
        super(BRServiceManager.get().getService("gms"));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService("gms");
        if (binder == null) {
            Slog.w(TAG, "getWho: gms binder is null");
            return null;
        }
        try {
            Class<?> stubClass = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            return asInterfaceMethod.invoke(null, binder);
        } catch (Throwable t) {
            Slog.w(TAG, "getWho: failed to resolve IGmsServiceBroker: " + t.getMessage());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("gms");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    //  gms's broker is a binder that hands out service stubs. inside
    //  a container, the host uid cannot hold the google account auth
    //  the broker needs, so any stub it returns is unusable.
    //
    //  the failure mode we're killing: gms asks for a service, gets a
    //  fallback stub, tries to cast it to an internal interface, fails
    //  with "Cannot cast snb to auin/avcr", thread pool dies.
    //
    //  fix: refuse to hand out any stubs. gms fails cleanly.
    // ─────────────────────────────────────────────────────────────

    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GmsProxy: refusing getService — returning null");
            return null;
        }
    }

    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GmsProxy: refusing getServiceBroker");
            return null;
        }
    }

    @ProxyMethod("validateAccount")
    public static class ValidateAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GmsProxy: refusing validateAccount");
            android.os.Bundle b = new android.os.Bundle();
            b.putBoolean("booleanResult", false);
            return b;
        }
    }

    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GmsProxy: refusing authenticate");
            android.os.Bundle b = new android.os.Bundle();
            b.putBoolean("booleanResult", false);
            b.putInt("errorCode", 8); // ERROR_CODE_INTERNAL_ERROR
            return b;
        }
    }

    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GmsProxy: refusing getToken");
            return null;
        }
    }

    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}