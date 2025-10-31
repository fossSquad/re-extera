package ni.shikatu.re_extera.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;

public class IsChatNoForwardsHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.noForward()) {
            param.setResult(false);
        }
    }
}
