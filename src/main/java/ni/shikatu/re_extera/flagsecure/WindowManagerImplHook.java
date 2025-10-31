package ni.shikatu.re_extera.flagsecure;


import android.view.WindowManager;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;

public class WindowManagerImplHook extends XC_MethodHook {


	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getRemoveFlagSecure()) return;
		WindowManager.LayoutParams params = (WindowManager.LayoutParams) param.args[1];
		if((params.flags & WindowManager.LayoutParams.FLAG_SECURE) != 0){
			params.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
		}

	}
}
