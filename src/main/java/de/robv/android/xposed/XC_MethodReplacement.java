package de.robv.android.xposed;

/** Minimal Xposed API shim, see {@link XC_MethodHook}. */
public abstract class XC_MethodReplacement extends XC_MethodHook {

    public XC_MethodReplacement() {
    }

    public XC_MethodReplacement(int priority) {
        super(priority);
    }

    @Override
    public final void beforeHookedMethod(MethodHookParam param) throws Throwable {
        try {
            param.setResult(replaceHookedMethod(param));
        } catch (Throwable t) {
            param.setThrowable(t);
        }
    }

    @Override
    public final void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;
}
