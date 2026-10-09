package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ISafetyCenterManagerProxy extends BinderInvocationStub {
    public static final String TAG = "SafetyCenterProxy";
    private static final String NAME = "safety_center";

    public ISafetyCenterManagerProxy() {
        super(BRServiceManager.get().getService(NAME));
    }

    @Override
    protected Object getWho() {
        try {
            IBinder b = BRServiceManager.get().getService(NAME);
            if (b == null) return null;
            Class<?> stub = Class.forName("android.safetycenter.ISafetyCenterManager$Stub");
            Method m = stub.getMethod("asInterface", IBinder.class);
            return m.invoke(null, b);
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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        Class<?> ret = method.getReturnType();

        if (name.startsWith("is") || name.startsWith("get") || name.startsWith("has")) {
            Slog.d(TAG, "SafetyCenter: short-circuiting " + name);
            if (ret == boolean.class || ret == Boolean.class) return false;
            if (ret == int.class)      return 0;
            if (ret == long.class)     return 0L;
            if (ret == String.class)   return "";
            return null;
        }

        if (name.startsWith("set") || name.startsWith("add")
                || name.startsWith("remove") || name.startsWith("clear")
                || name.startsWith("report") || name.startsWith("dismiss")) {
            Slog.d(TAG, "SafetyCenter: skipping " + name);
            return null;
        }

        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("isSafetyCenterEnabled")
    public static class IsSafetyCenterEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return false;
        }
    }

    @ProxyMethod("getSafetyCenterConfig")
    public static class GetSafetyCenterConfig extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}