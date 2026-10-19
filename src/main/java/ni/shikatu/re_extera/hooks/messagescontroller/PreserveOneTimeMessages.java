package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.utils.AccountUtils;

public class PreserveOneTimeMessages extends XC_MethodHook {
    @Override
    @SuppressWarnings("unchecked")
    public void beforeHookedMethod(MethodHookParam param) {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        Object updates = param.args[0];
        if (!(updates instanceof ArrayList)) {
            return;
        }
        for (Object update : (ArrayList<Object>) updates) {
            ProcessUpdates.preserveOneTimeMessage(update);
            ProcessUpdates.recordLastOnlineFromUpdate(currentAccount, update);
        }
    }
}
