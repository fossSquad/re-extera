package ni.shikatu.re_extera.hooks.flagsecure;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class FlagSecureReasonAttach extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getRemoveFlagSecure()) {
            param.setResult((Object) null);
        }
    }
}
