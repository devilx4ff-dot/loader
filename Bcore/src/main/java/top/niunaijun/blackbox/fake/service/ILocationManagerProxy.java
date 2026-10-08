package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.location.LocationManager;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import black.android.location.BRILocationManagerStub;
import black.android.location.provider.BRProviderProperties;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.frameworks.BLocationManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;


public class ILocationManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ILocationManagerProxy";

    public ILocationManagerProxy() {
        super(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRILocationManagerStub.get().asInterface(
                BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // rewrite pkg + uid args so the real service sees the host, not the guest
        MethodParameterUtils.replaceFirstAppPkg(args);
        MethodParameterUtils.replaceLastUid(args);
        return super.invoke(proxy, method, args);
    }

    // ──────────────────────── REAL hooks ────────────────────────

    @ProxyMethod("getLastLocation")
    public static class GetLastLocation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (isFakeLocationEnabled()) {
                try {
                    return BLocationManager.get()
                            .getLocation(BActivityThread.getUserId(),
                                    BActivityThread.getAppPackageName())
                            .convert2SystemLocation();
                } catch (Throwable ignored) { }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                if (cause instanceof SecurityException) {
                    Log.w(TAG, "getLastLocation: perm denied — returning null");
                    return null;
                }
                Log.w(TAG, "getLastLocation: " + cause.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("getLastKnownLocation")
    public static class GetLastKnownLocation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (isFakeLocationEnabled()) {
                try {
                    return BLocationManager.get()
                            .getLocation(BActivityThread.getUserId(),
                                    BActivityThread.getAppPackageName())
                            .convert2SystemLocation();
                } catch (Throwable ignored) { }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Log.w(TAG, "getLastKnownLocation: " + t.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("requestLocationUpdates")
    public static class RequestLocationUpdates extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (isFakeLocationEnabled()) {
                if (args != null && args.length > 1 && args[1] instanceof IInterface) {
                    try {
                        IInterface listener = (IInterface) args[1];
                        BLocationManager.get().requestLocationUpdates(listener.asBinder());
                        return 0;
                    } catch (Throwable ignored) { }
                }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Log.w(TAG, "requestLocationUpdates: " + t.getMessage());
                return 0;
            }
        }
    }

    @ProxyMethod("removeUpdates")
    public static class RemoveUpdates extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof IInterface) {
                try {
                    IInterface listener = (IInterface) args[0];
                    BLocationManager.get().removeUpdates(listener.asBinder());
                    return 0;
                } catch (Throwable ignored) { }
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return 0;
            }
        }
    }

    @ProxyMethod("getProviderProperties")
    public static class GetProviderProperties extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object props;
            try {
                props = method.invoke(who, args);
            } catch (Throwable t) {
                Log.w(TAG, "getProviderProperties: " + t.getMessage());
                return null;
            }
            if (props == null) return null;
            if (isFakeLocationEnabled()) {
                try {
                    BRProviderProperties.get(props)._set_mHasNetworkRequirement(false);
                    if (BLocationManager.get().getCell(BActivityThread.getUserId(),
                            BActivityThread.getAppPackageName()) == null) {
                        BRProviderProperties.get(props)._set_mHasCellRequirement(false);
                    }
                } catch (Throwable ignored) { }
            }
            return props;
        }
    }

    @ProxyMethod("registerGnssStatusCallback")
    public static class RegisterGnssStatusCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // void return — just ack and skip
            return null;
        }
    }

    @ProxyMethod("removeGpsStatusListener")
    public static class RemoveGpsStatusListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getBestProvider")
    public static class GetBestProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (isFakeLocationEnabled()) {
                return LocationManager.GPS_PROVIDER;
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return LocationManager.GPS_PROVIDER;
            }
        }
    }

    @ProxyMethod("getAllProviders")
    public static class GetAllProviders extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return Arrays.asList(LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER);
        }
    }

    @ProxyMethod("isProviderEnabledForUser")
    public static class IsProviderEnabledForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || args.length == 0) return false;
            String provider = (String) args[0];
            return Objects.equals(provider, LocationManager.GPS_PROVIDER);
        }
    }

    @ProxyMethod("setExtraLocationControllerPackageEnabled")
    public static class SetExtraLocationControllerPackageEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    // ──────────────────────── helpers ────────────────────────

    private static boolean isFakeLocationEnabled() {
        try {
            String pkg = BActivityThread.getAppPackageName();
            int userId = BActivityThread.getUserId();
            if (pkg == null) return false;
            return BLocationManager.get().getPattern(userId, pkg)
                    != BLocationManager.CLOSE_MODE;
        } catch (Throwable t) {
            return false;
        }
    }
}