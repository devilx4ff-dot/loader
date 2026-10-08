package top.niunaijun.blackbox.fake.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.IAccountManagerResponse;
import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.frameworks.BAccountManager;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


/**
 * Pass-through for Google accounts.
 *
 * Inside a guest process, AccountManager calls land here. We:
 *   1. return the host's real accounts for getAccounts / getAccountsByType,
 *      so the account picker populates correctly
 *   2. forward getAuthToken / peekAuthToken / getPassword / getUserData to
 *      the HOST's AccountManager — that's the one that can actually mint
 *      a Google OAuth token
 *   3. return honest nulls for anything that would otherwise fabricate state
 */
public class GoogleAccountManagerProxy extends ClassInvocationStub {

    public static final String TAG = "GoogleAccountManagerProxy";

    public GoogleAccountManagerProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        try {
            Context context = BlackBoxCore.getContext();
            if (context != null) {
                return AccountManager.get(context);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get AccountManager instance", e);
        }
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // ────────────────── account enumeration ──────────────────
    // Return the host's real accounts. The account picker needs to see them.

    @ProxyMethod("getAccounts")
    public static class GetAccounts extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (result instanceof Account[]) {
                    Account[] accs = (Account[]) result;
                    Slog.d(TAG, "getAccounts → " + accs.length + " host accounts");
                    return accs;
                }
            } catch (Throwable t) {
                Slog.w(TAG, "getAccounts: " + t.getMessage());
            }
            return new Account[0];
        }
    }

    @ProxyMethod("getAccountsByType")
    public static class GetAccountsByType extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args == null || args.length == 0 || !(args[0] instanceof String)) {
                return method.invoke(who, args);
            }
            String type = (String) args[0];

            // Query the virtual store first — if the user has signed in
            // *inside* the container, use that.
            try {
                Account[] virtual = BAccountManager.get().getAccountsAsUser(type);
                if (virtual != null && virtual.length > 0) {
                    Slog.d(TAG, "getAccountsByType(" + type + ") → " + virtual.length + " virtual");
                    return virtual;
                }
            } catch (Throwable ignored) { }

            // Otherwise return the host's real accounts so the picker fills.
            try {
                Object result = method.invoke(who, args);
                if (result instanceof Account[]) {
                    Account[] host = (Account[]) result;
                    Slog.d(TAG, "getAccountsByType(" + type + ") → " + host.length + " host");
                    return host;
                }
            } catch (Throwable t) {
                Slog.w(TAG, "getAccountsByType: " + t.getMessage());
            }
            return new Account[0];
        }
    }

    // ────────────────── token / password / userdata ──────────────────
    // Forward to the HOST's AccountManager — only it can mint Google tokens.

    @ProxyMethod("getPassword")
    public static class GetPassword extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object r = method.invoke(who, args);
                Slog.d(TAG, "getPassword → " + (r != null ? "ok" : "null"));
                return r;
            } catch (Throwable t) {
                Slog.w(TAG, "getPassword: " + t.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("getUserData")
    public static class GetUserData extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "getUserData: " + t.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("peekAuthToken")
    public static class PeekAuthToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object r = method.invoke(who, args);
                Slog.d(TAG, "peekAuthToken → " + (r != null ? "hit" : "miss"));
                return r;
            } catch (Throwable t) {
                Slog.w(TAG, "peekAuthToken: " + t.getMessage());
                return null;
            }
        }
    }

    // ────────────────── the actual sign-in path ──────────────────
    // getAuthToken(account, authTokenType, options, notifyOnAuthFailure, expectActivityLaunch, callback)
    // this is the method GMS calls when it needs a fresh OAuth token.

    @ProxyMethod("getAuthToken")
    public static class GetAuthToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "getAuthToken called — forwarding to host");
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "getAuthToken: " + t.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("invalidateAuthToken")
    public static class InvalidateAuthToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return null;
            }
        }
    }

    @ProxyMethod("hasFeatures")
    public static class HasFeatures extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return false;
            }
        }
    }

    @ProxyMethod("getAuthenticatorTypes")
    public static class GetAuthenticatorTypes extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return new String[0];
            }
        }
    }

    @ProxyMethod("addAccount")
    public static class AddAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return null;
            }
        }
    }

    @ProxyMethod("removeAccount")
    public static class RemoveAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return null;
            }
        }
    }

    @ProxyMethod("setPassword")
    public static class SetPassword extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return null;
            }
        }
    }

    @ProxyMethod("setUserData")
    public static class SetUserData extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return null;
            }
        }
    }
}