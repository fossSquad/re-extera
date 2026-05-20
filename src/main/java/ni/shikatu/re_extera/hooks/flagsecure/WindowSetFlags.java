package ni.shikatu.re_extera.hooks.flagsecure;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class WindowSetFlags extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getRemoveFlagSecure()) {
            int flags = ((Integer) param.args[0]).intValue();
            int mask = ((Integer) param.args[1]).intValue();
            if ((mask & 8192) != 0) {
                param.args[0] = Integer.valueOf(flags & (-8193));
            }
        }
    }
}
