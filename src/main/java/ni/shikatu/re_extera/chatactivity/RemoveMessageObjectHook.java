package ni.shikatu.re_extera.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;

public class RemoveMessageObjectHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveDeletedMessages()) {
            param.setResult((Object) null);
        }
    }
}
