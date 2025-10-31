package ni.shikatu.re_extera;

import de.robv.android.xposed.XC_MethodHook;

public class NotificationsRemoveDeletedHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getSaveDeletedMessages()) return;

		boolean isReact = (boolean) param.args[1];
		if (Settings.getSaveDeletedMessages() && !isReact) {
			param.setResult(null);
		}
	}
}

