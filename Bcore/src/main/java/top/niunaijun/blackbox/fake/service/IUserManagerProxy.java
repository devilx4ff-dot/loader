package top.niunaijun.blackbox.fake.service;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.ArrayList;

import black.android.content.pm.BRUserInfo;
import black.android.os.BRIUserManagerStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class IUserManagerProxy extends BinderInvocationStub {
    public static final String TAG = "UserManagerProxy";

    public IUserManagerProxy() {
        super(BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIUserManagerStub.get().asInterface(
                BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    private static Object userInfoFor(int userId) {
        try {
            return BRUserInfo.get()._new(userId, "BlackBox",
                    BRUserInfo.get().FLAG_PRIMARY());
        } catch (Throwable t) {
            Slog.w(TAG, "userInfoFor failed: " + t.getMessage());
            return null;
        }
    }

    private static Object typeAwareUser(Class<?> methodReturn, int userId) {
        if (methodReturn == int.class || methodReturn == Integer.class) return userId;
        if (methodReturn.getName().equals("android.content.pm.UserInfo")) {
            return userInfoFor(userId);
        }
        return null;
    }

    @ProxyMethod("getApplicationRestrictions")
    public static class GetApplicationRestrictions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                args[0] = BlackBoxCore.getHostPkg();
            }
            try {
                return method.invoke(who, args);
            } catch (SecurityException se) {
                return new android.os.Bundle();
            }
        }
    }

    @ProxyMethod("getProfileParent")
    public static class GetProfileParent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return typeAwareUser(method.getReturnType(), BActivityThread.getUserId());
        }
    }

    @ProxyMethod("getUsers")
    public static class GetUsers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return new ArrayList<>();
        }
    }

    @ProxyMethod("getUserInfo")
    public static class GetUserInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = (args != null && args.length > 0 && args[0] instanceof Integer)
                    ? (Integer) args[0] : 0;
            return typeAwareUser(method.getReturnType(), userId);
        }
    }

    @ProxyMethod("getUserHandle")
    public static class GetUserHandle extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return typeAwareUser(method.getReturnType(), 0);
        }
    }

    @ProxyMethod("getProfileIds")
    public static class GetProfileIds extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return new int[]{0};
        }
    }

    @ProxyMethod("getUserSerialNumber")
    public static class GetUserSerialNumber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("getUserIcon")
    public static class GetUserIcon extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("isUserUnlocked")
    public static class IsUserUnlocked extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return true;
        }
    }

    @ProxyMethod("isUserRunning")
    public static class IsUserRunning extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return true;
        }
    }

    @ProxyMethod("isManagedProfile")
    public static class IsManagedProfile extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return false;
        }
    }

}