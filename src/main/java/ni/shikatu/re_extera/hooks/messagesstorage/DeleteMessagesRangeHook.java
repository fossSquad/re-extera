package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;

public class DeleteMessagesRangeHook extends XC_MethodHook {
    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        DeleteMessagesRangeFlag.ACTIVE.set(Boolean.TRUE);
    }

    @Override
    public void afterHookedMethod(MethodHookParam param) {
        DeleteMessagesRangeFlag.ACTIVE.remove();
    }
}
