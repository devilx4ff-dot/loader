package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * telecom calls enforcePhoneAccountModificationForPackage which checks
 * "does package X belong to uid Y". guests fail this because the caller
 * uid is the host's but the package name is the guest's.
 *
 * treat all phone-account methods as no-ops for guests.
 */
public class ITelecomServiceProxy extends BinderInvocationStub {
    public static final String TAG = "TelecomServiceProxy";
    private static final String NAME = "telecom";

    public ITelecomServiceProxy() {
        super(BRServiceManager.get().getService(NAME));
    }

    @Override
    protected Object getWho() {
        try {
            IBinder binder = BRServiceManager.get().getService(NAME);
            if (binder == null) return null;
            Class<?> stub = Class.forName("com.android.internal.telecom.ITelecomService$Stub");
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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        // for guest processes, don't even try — telecom self-identity checks
        // will always fail because the guest package doesn't belong to host uid
        if (name.startsWith("enforce")
                || name.startsWith("registerPhoneAccount")
                || name.startsWith("unregisterPhoneAccount")
                || name.startsWith("addNewIncomingCall")
                || name.startsWith("addNewUnknownCall")
                || name.startsWith("placeCall")
                || name.startsWith("getPhoneAccount")
                || name.startsWith("getCallState")
                || name.startsWith("isVoiceMailNumber")
                || name.startsWith("getDefaultOutgoingPhoneAccount")
                || name.startsWith("getSimCallManager")) {
            Slog.d(TAG, "Telecom: short-circuiting " + name);

            Class<?> ret = method.getReturnType();
            if (ret == void.class)      return null;
            if (ret == boolean.class)   return false;
            if (ret == int.class)       return 0;
            if (ret == long.class)      return 0L;
            if (ret == float.class)     return 0f;
            if (ret == double.class)    return 0d;
            if (ret == String.class)    return "";
            return null;
        }

        // fall back to the base — ClassInvocationStub.invoke will use
        // its own mBase to actually call the real service
        return super.invoke(proxy, method, args);
    }
    @ProxyMethod("enforcePhoneAccountModificationForPackage")
    public static class EnforcePhoneAccountModificationForPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // void return; skip the permission check
            Slog.d(TAG, "Telecom: skipping enforcePhoneAccountModificationForPackage");
            return null;
        }
    }

    @ProxyMethod("getPhoneAccount")
    public static class GetPhoneAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getAllPhoneAccountsCount")
    public static class GetAllPhoneAccountsCount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("getAllPhoneAccounts")
    public static class GetAllPhoneAccounts extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return new java.util.ArrayList<>();
        }
    }
}