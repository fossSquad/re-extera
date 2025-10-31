package ni.shikatu.re_extera.flagsecure;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Settings;

public class FlagSecureReasonHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getRemoveFlagSecure()) return;
		Global.log("Do not attach on FlagSecureReason");
		param.setResult(null);
	}
}
