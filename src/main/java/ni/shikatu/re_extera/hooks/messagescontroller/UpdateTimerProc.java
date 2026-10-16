package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.GhostMenuHelper;

public class UpdateTimerProc extends XC_MethodHook {
    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        GhostMenuHelper.enforceOnlineStatus(AccountUtils.getCurrentAccount(param.thisObject));
    }
}
