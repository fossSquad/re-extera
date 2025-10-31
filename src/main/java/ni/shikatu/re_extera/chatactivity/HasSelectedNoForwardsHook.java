package ni.shikatu.re_extera.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;

public class HasSelectedNoForwardsHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.noForward()) {
            param.setResult(false);
        }
    }
}
