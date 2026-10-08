package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Blocks anti-virtual-environment detection for apps like Meituan, Douyin, etc.
 * Two layers:
 *   1) ContentProvider skip (see BActivityThread.isAntiDetectProvider)
 *   2) Runtime.exit / Process.killProcess soft-block
 */
public class AntiVirtualDetectProxy extends ClassInvocationStub {

    public static final String TAG = "AntiVirtualDetect";

    public AntiVirtualDetectProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // hooks target: java.lang.Runtime
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        // nothing to inject — the method hooks fire through the annotation scanner
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // ── block System.exit() ──
    @ProxyMethod("exit")
    public static class Exit extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int code = args != null && args.length > 0 ? (int) args[0] : 0;
            Slog.w(TAG, "Blocked Runtime.exit(" + code + ") — anti-detect");
            // swallow the call: don't kill the process, just return
            return null;
        }
    }

    // ── block halt() (rarely called by detector libraries) ──
    @ProxyMethod("halt")
    public static class Halt extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int code = args != null && args.length > 0 ? (int) args[0] : 0;
            Slog.w(TAG, "Blocked Runtime.halt(" + code + ") — anti-detect");
            return null;
        }
    }

    // ── block killProcess() ──
    @ProxyMethod("killProcess")
    public static class KillProcess extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int pid = args != null && args.length > 0 ? (int) args[0] : -1;
            Slog.w(TAG, "Blocked Process.killProcess(" + pid + ") — anti-detect");
            return null;
        }
    }
}