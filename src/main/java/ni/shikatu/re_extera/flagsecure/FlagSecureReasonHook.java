package ni.shikatu.re_extera.flagsecure;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Settings;

public class FlagSecureReasonHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getRemoveFlagSecure()) {
            Global.log("Do not attach on FlagSecureReason");
            param.setResult((Object) null);
        }
    }
}
