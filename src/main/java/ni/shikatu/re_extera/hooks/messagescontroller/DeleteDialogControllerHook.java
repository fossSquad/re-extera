package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.hooks.messagesstorage.DeleteDialogHook;

public class DeleteDialogControllerHook extends XC_MethodHook {
    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        try {
            long did = ((Long) param.args[0]).longValue();
            int onlyHistory = 0;
            if (param.args.length > 1 && param.args[1] instanceof Integer) {
                onlyHistory = ((Integer) param.args[1]).intValue();
            }
            if (onlyHistory == 0) {
                DeleteDialogHook.markManualDelete(did);
            }
        } catch (Throwable ignored) {
        }
    }
}
