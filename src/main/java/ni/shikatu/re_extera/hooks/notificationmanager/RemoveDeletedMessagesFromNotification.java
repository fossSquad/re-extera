package ni.shikatu.re_extera.hooks.notificationmanager;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class RemoveDeletedMessagesFromNotification extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveDeletedMessages()) {
            boolean isReact = ((Boolean) param.args[1]).booleanValue();
            if (!isReact) {
                param.setResult((Object) null);
            }
        }
    }
}
