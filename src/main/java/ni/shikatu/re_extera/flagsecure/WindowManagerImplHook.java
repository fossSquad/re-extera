package ni.shikatu.re_extera.flagsecure;

import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;

public class WindowManagerImplHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getRemoveFlagSecure()) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) param.args[1];
            if ((params.flags & 8192) != 0) {
                params.flags &= -8193;
            }
        }
    }
}
