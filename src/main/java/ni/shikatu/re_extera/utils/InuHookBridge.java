package ni.shikatu.re_extera.utils;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ni.shikatu.re_extera.Main;

/**
 * Xposed-style hook registry on top of the Inugram plugin engine (lsplant).
 * The engine exposes {@code PluginXposed$Native.nativeHook(Member, Object, Method)},
 * where the hooker is any object with a {@code public Object callback(Object[])} method.
 */
public final class InuHookBridge {

    private static final String NATIVE_CLASS = "desu.inugram.helpers.plugins.platform.PluginXposed$Native";
    private static final Object LOCK = new Object();
    private static final Map<Member, Entry> ENTRIES = new HashMap<>();
    private static final Object[] NO_ARGS = new Object[0];

    private static boolean nativeResolved;
    private static boolean nativeAvailable;
    private static Object nativeInstance;
    private static Method nativeHook;
    private static Method nativeUnhook;
    private static Method hookerCallback;

    private InuHookBridge() {
    }

    public static XC_MethodHook.Unhook hook(Member target, XC_MethodHook callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }
        XC_MethodHook.Unhook unhook = callback.new Unhook(target);
        if (target == null || !resolveNative()) {
            return unhook;
        }
        synchronized (LOCK) {
            Entry entry = ENTRIES.get(target);
            if (entry == null) {
                entry = new Entry(target);
                Method backup = null;
                try {
                    Object result = nativeHook.invoke(nativeInstance, target, entry.hooker, hookerCallback);
                    if (result instanceof Method) {
                        backup = (Method) result;
                    }
                } catch (Throwable t) {
                    Main.log("Inugram hooks: failed to hook %s: %s", target, t);
                }
                if (backup == null) {
                    Main.log("Inugram hooks: no backup method for %s", target);
                    return unhook;
                }
                entry.backup = backup;
                ENTRIES.put(target, entry);
            }
            entry.hooks.add(callback);
        }
        return unhook;
    }

    public static void unhook(Member target, XC_MethodHook callback) {
        if (target == null || callback == null) {
            return;
        }
        synchronized (LOCK) {
            Entry entry = ENTRIES.get(target);
            if (entry == null) {
                return;
            }
            entry.hooks.remove(callback);
            if (!entry.hooks.isEmpty()) {
                return;
            }
            ENTRIES.remove(target);
            if (nativeUnhook != null && nativeInstance != null) {
                try {
                    nativeUnhook.invoke(nativeInstance, target);
                } catch (Throwable t) {
                    Main.log("Inugram hooks: failed to unhook %s: %s", target, t);
                }
            }
        }
    }

    public static Object invokeOriginal(Member member, Object thisObject, Object[] args) {
        if (member == null) {
            return null;
        }
        Method backup;
        synchronized (LOCK) {
            Entry entry = ENTRIES.get(member);
            backup = entry != null ? entry.backup : null;
        }
        try {
            if (backup != null) {
                return backup.invoke(thisObject, args);
            }
            if (member instanceof Method) {
                Method method = (Method) member;
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method.invoke(thisObject, args);
            }
        } catch (InvocationTargetException e) {
            throw sneaky(e.getCause() != null ? e.getCause() : e);
        } catch (Throwable t) {
            throw sneaky(t);
        }
        return null;
    }

    private static boolean resolveNative() {
        synchronized (LOCK) {
            if (nativeResolved) {
                return nativeAvailable;
            }
            nativeResolved = true;
            try {
                try {
                    System.loadLibrary("lsplant");
                } catch (Throwable ignored) {
                }
                Class<?> cls = Class.forName(NATIVE_CLASS);
                Object instance = cls.getField("INSTANCE").get(null);
                Method init = cls.getDeclaredMethod("nativeInit");
                Method hook = cls.getDeclaredMethod("nativeHook", Member.class, Object.class, Method.class);
                Method unhook = cls.getDeclaredMethod("nativeUnhook", Member.class);
                init.setAccessible(true);
                hook.setAccessible(true);
                unhook.setAccessible(true);
                Object result = init.invoke(instance);
                if (!Boolean.TRUE.equals(result)) {
                    Main.log("Inugram hooks: nativeInit returned %s", result);
                    return false;
                }
                hookerCallback = Hooker.class.getDeclaredMethod("callback", Object[].class);
                hookerCallback.setAccessible(true);
                nativeInstance = instance;
                nativeHook = hook;
                nativeUnhook = unhook;
                nativeAvailable = true;
                Main.log("Inugram hooks: lsplant bridge ready");
            } catch (Throwable t) {
                Main.log("Inugram hooks: bridge unavailable: %s", t);
                nativeAvailable = false;
            }
            return nativeAvailable;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    private static RuntimeException sneaky(Throwable t) {
        InuHookBridge.<RuntimeException>sneakyThrow(t);
        return null;
    }

    private static Class<?> returnType(Member member) {
        return member instanceof Method ? ((Method) member).getReturnType() : null;
    }

    private static Object defaultValue(Class<?> type) {
        if (type == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (type == Character.TYPE) {
            return Character.valueOf('\0');
        }
        if (type == Byte.TYPE) {
            return Byte.valueOf((byte) 0);
        }
        if (type == Short.TYPE) {
            return Short.valueOf((short) 0);
        }
        if (type == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (type == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (type == Float.TYPE) {
            return Float.valueOf(0f);
        }
        if (type == Double.TYPE) {
            return Double.valueOf(0d);
        }
        return null;
    }

    public static final class Entry {
        final Member target;
        final Hooker hooker = new Hooker(this);
        final List<XC_MethodHook> hooks = new ArrayList<>(1);
        Method backup;

        Entry(Member target) {
            this.target = target;
        }
    }

    /** Callback object handed to lsplant; the method signature must stay exactly (Object[]) -> Object. */
    public static final class Hooker {
        private final Entry entry;

        public Hooker(Entry entry) {
            this.entry = entry;
        }

        public Object callback(Object[] args) {
            Entry entry = this.entry;
            XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
            param.method = entry.target;

            if (Modifier.isStatic(entry.target.getModifiers()) || args == null || args.length == 0) {
                param.thisObject = null;
                param.args = args != null ? args : NO_ARGS;
            } else {
                param.thisObject = args[0];
                Object[] params = new Object[args.length - 1];
                System.arraycopy(args, 1, params, 0, params.length);
                param.args = params;
            }

            XC_MethodHook[] snapshot;
            synchronized (LOCK) {
                snapshot = entry.hooks.toArray(new XC_MethodHook[0]);
            }

            for (int i = snapshot.length - 1; i >= 0; i--) {
                try {
                    snapshot[i].beforeHookedMethod(param);
                } catch (Throwable t) {
                    param.setThrowable(t);
                }
                if (param.hasThrowable() || param.returnEarly) {
                    break;
                }
            }

            if (!param.hasThrowable() && !param.returnEarly) {
                try {
                    param.setResult(entry.backup.invoke(param.thisObject, param.args));
                } catch (InvocationTargetException e) {
                    param.setThrowable(e.getCause() != null ? e.getCause() : e);
                } catch (Throwable t) {
                    param.setThrowable(t);
                }
            }

            for (XC_MethodHook hook : snapshot) {
                try {
                    hook.afterHookedMethod(param);
                } catch (Throwable t) {
                    param.setThrowable(t);
                }
            }

            try {
                Object result = param.getResultOrThrowable();
                Class<?> type = returnType(entry.target);
                if (result == null && type != null && type.isPrimitive() && type != Void.TYPE) {
                    return defaultValue(type);
                }
                return result;
            } catch (Throwable t) {
                throw sneaky(t);
            }
        }
    }
}
