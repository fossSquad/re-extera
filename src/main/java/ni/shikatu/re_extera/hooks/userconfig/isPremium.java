package ni.shikatu.re_extera.hooks.userconfig;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class isPremium extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getLocalPremium()) {
            param.setResult(true);
        }
    }
}
