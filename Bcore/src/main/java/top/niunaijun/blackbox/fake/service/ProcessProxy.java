package top.niunaijun.blackbox.fake.service;

import android.os.Process;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Inside a guest process, Process.myUid() must report the guest's uid,
 * not the host's. GMS keys off this and self-invalidates otherwise.
 */
public class ProcessProxy extends ClassInvocationStub {

    public static final String TAG = "ProcessProxy";

    public ProcessProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        Slog.d(TAG, "Process.myUid() proxy installed");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("myUid")
    public static class MyUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (BActivityThread.isThreadInit()) {
                    int bUid = BActivityThread.getBUid();
                    if (bUid > 0) return bUid;
                }
            } catch (Throwable ignored) { }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("myPid")
    public static class MyPid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("myTid")
    public static class MyTid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }
}