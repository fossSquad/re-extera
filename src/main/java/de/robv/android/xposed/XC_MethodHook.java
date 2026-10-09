package de.robv.android.xposed;

import java.lang.reflect.Member;

/**
 * Minimal Xposed API shim for forks that do not bundle one (Inugram).
 * On exteraGram the real classes from the host app shadow these (parent-first class loading).
 */
public abstract class XC_MethodHook {

    public XC_MethodHook() {
    }

    public XC_MethodHook(int priority) {
    }

    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    public void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    public final class Unhook {
        private final Member hookMethod;

        public Unhook(Member hookMethod) {
            this.hookMethod = hookMethod;
        }

        public Member getHookedMethod() {
            return this.hookMethod;
        }

        public XC_MethodHook getCallback() {
            return XC_MethodHook.this;
        }

        public void unhook() {
            XposedBridge.unhookMethod(this.hookMethod, XC_MethodHook.this);
        }
    }

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        public boolean returnEarly;
        private Object result;
        private Throwable throwable;

        public MethodHookParam() {
        }

        public Object getResult() {
            return this.result;
        }

        public void setResult(Object result) {
            this.result = result;
            this.returnEarly = true;
        }

        public Throwable getThrowable() {
            return this.throwable;
        }

        public boolean hasThrowable() {
            return this.throwable != null;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }

        public Object getResultOrThrowable() throws Throwable {
            if (this.throwable != null) {
                throw this.throwable;
            }
            return this.result;
        }
    }
}
