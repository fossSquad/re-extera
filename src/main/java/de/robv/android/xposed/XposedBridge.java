package de.robv.android.xposed;

import java.lang.reflect.Member;
import ni.shikatu.re_extera.utils.InuHookBridge;

/**
 * Minimal Xposed API shim backed by the Inugram lsplant hook engine.
 * On exteraGram the real classes from the host app shadow these (parent-first class loading).
 */
public class XposedBridge {

    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) {
        return InuHookBridge.hook(method, callback);
    }

    public static void unhookMethod(Member method, XC_MethodHook callback) {
        InuHookBridge.unhook(method, callback);
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) {
        return InuHookBridge.invokeOriginal(method, thisObject, args);
    }
}
