package ni.shikatu.re_extera;

import de.robv.android.xposed.XC_MethodHook;

public class NotificationsRemoveDeletedHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveDeletedMessages()) {
            boolean isReact = ((Boolean) param.args[1]).booleanValue();
            if (Settings.getSaveDeletedMessages() && !isReact) {
                param.setResult((Object) null);
            }
        }
    }
}
