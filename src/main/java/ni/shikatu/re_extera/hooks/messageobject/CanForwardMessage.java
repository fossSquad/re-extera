package ni.shikatu.re_extera.hooks.messageobject;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class CanForwardMessage extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.noForward()) {
            param.setResult(true);
        }
    }
}
