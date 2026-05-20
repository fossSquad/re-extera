package ni.shikatu.re_extera.hooks.flagsecure;

import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class WindowManagerImpl extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getRemoveFlagSecure()) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) param.args[1];
            if ((params.flags & 8192) != 0) {
                params.flags &= -8193;
            }
        }
    }
}
