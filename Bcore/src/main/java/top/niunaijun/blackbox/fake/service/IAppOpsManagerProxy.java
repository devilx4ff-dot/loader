package top.niunaijun.blackbox.fake.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.app.BRAppOpsManager;
import black.android.os.BRServiceManager;
import black.com.android.internal.app.BRIAppOpsServiceStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;


public class IAppOpsManagerProxy extends BinderInvocationStub {
    public static final String TAG = "AppOpsManagerProxy";

    public IAppOpsManagerProxy() {
        super(BRServiceManager.get().getService(Context.APP_OPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder call = BRServiceManager.get().getService(Context.APP_OPS_SERVICE);
        return BRIAppOpsServiceStub.get().asInterface(call);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (BRAppOpsManager.get(null)._check_mService() != null) {
            AppOpsManager appOpsManager = (AppOpsManager) BlackBoxCore.getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                BRAppOpsManager.get(appOpsManager)._set_mService(getProxyInvocation());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        replaceSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();

        // checkXxx / noteXxx — return int (MODE_ALLOWED)
        if (methodName.startsWith("check") || methodName.startsWith("note")) {
            if (returnType == int.class || returnType == Integer.class) {
                return AppOpsManager.MODE_ALLOWED;
            }
            if (returnType == void.class) {
                return null;
            }
        }

        // startXxx — returns SyncNotedAppOp on modern Android, int on older
        if (methodName.startsWith("start")) {
            if ("android.app.SyncNotedAppOp".equals(returnType.getName())) {
                return makeFakeSyncNotedAppOp(methodName, args);
            }
            if (returnType == int.class || returnType == Integer.class) {
                return AppOpsManager.MODE_ALLOWED;
            }
            if (returnType == void.class) {
                return null;
            }
        }

        // finishXxx — void
        if (methodName.startsWith("finish")) {
            return null;
        }

        // everything else — try real, fall back on SecurityException
        try {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            Slog.w(TAG, "AppOps invoke: SecurityException in " + methodName, e);
            return safeDefault(returnType);
        } catch (Exception e) {
            Slog.e(TAG, "AppOps invoke: error in " + methodName, e);
            return safeDefault(returnType);
        }
    }

    private static Object safeDefault(Class<?> returnType) {
        if (returnType == void.class)      return null;
        if (returnType == int.class)       return AppOpsManager.MODE_ALLOWED;
        if (returnType == Integer.class)   return AppOpsManager.MODE_ALLOWED;
        if (returnType == boolean.class)   return false;
        if (returnType == Boolean.class)   return false;
        if (returnType == long.class)      return 0L;
        if (returnType == Long.class)      return 0L;
        if (returnType == String.class)    return "";
        return null;
    }

    private static Object makeFakeSyncNotedAppOp(String methodName, Object[] args) {
        try {
            Class<?> cls = Class.forName("android.app.SyncNotedAppOp");
            int op = 0;
            String tag = null;

            if (args != null) {
                for (Object a : args) {
                    if (a instanceof Integer) {
                        op = (Integer) a;
                        break;
                    }
                }
                for (Object a : args) {
                    if (a instanceof String) {
                        tag = (String) a;
                        break;
                    }
                }
            }

            try {
                java.lang.reflect.Constructor<?> c = cls.getDeclaredConstructor(int.class, String.class);
                c.setAccessible(true);
                return c.newInstance(op, tag);
            } catch (NoSuchMethodException ignored) { }

            try {
                java.lang.reflect.Constructor<?> c = cls.getDeclaredConstructor(int.class);
                c.setAccessible(true);
                return c.newInstance(op);
            } catch (NoSuchMethodException ignored) { }

            Slog.w(TAG, "makeFakeSyncNotedAppOp: no usable constructor for " + methodName);
            return null;
        } catch (Throwable t) {
            Slog.w(TAG, "makeFakeSyncNotedAppOp failed for " + methodName + ": " + t.getMessage());
            return null;
        }
    }

    // ───── inner classes for specific methods ─────

    @ProxyMethod("noteProxyOperation")
    public static class NoteProxyOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkPackage")
    public static class CheckPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOperation")
    public static class CheckOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOperationForDevice")
    public static class CheckOperationForDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("noteOperation")
    public static class NoteOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOpNoThrow")
    public static class CheckOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("finishOp")
    public static class FinishOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("noteOp")
    public static class NoteOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("noteOpNoThrow")
    public static class NoteOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return AppOpsManager.MODE_ALLOWED;
        }
    }
}