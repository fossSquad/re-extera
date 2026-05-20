package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class CheckDeletingTask extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveOneTimeMessages()) {
            param.setResult(false);
        }
    }
}
