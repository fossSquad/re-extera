package ni.shikatu.re_extera.flagsecure;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Settings;

public class WindowHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getRemoveFlagSecure()) {
            Global.log("Window hook? removing flag_secure");
            int original_flags = ((Integer) param.args[0]).intValue();
            int original_mask = ((Integer) param.args[1]).intValue();
            if ((original_mask & 8192) != 0) {
                int modified_flags = original_flags & (-8193);
                param.args[0] = Integer.valueOf(modified_flags);
                param.args[1] = Integer.valueOf(original_mask);
            }
        }
    }
}
