package ni.shikatu.re_extera.flagsecure;

import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Settings;

public class WindowHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getRemoveFlagSecure()) return;
		Global.log("Window hook? removing flag_secure");
		int original_flags = (int) param.args[0];
		int original_mask = (int) param.args[1];
		if((original_mask & WindowManager.LayoutParams.FLAG_SECURE) != 0){
			int modified_flags = original_flags & ~WindowManager.LayoutParams.FLAG_SECURE;
			param.args[0] = modified_flags;
			param.args[1] = original_mask;
		}
	}
}
