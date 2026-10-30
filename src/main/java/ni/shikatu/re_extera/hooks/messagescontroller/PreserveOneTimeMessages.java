package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.List;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.AccountUtils;

public class PreserveOneTimeMessages extends XC_MethodHook {
    @Override
    @SuppressWarnings("unchecked")
    public void beforeHookedMethod(MethodHookParam param) {
        try {
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
            Object updates = param.args[0];
            if (!(updates instanceof List)) {
                return;
            }
            List<Object> copy;
            synchronized (updates) {
                copy = new ArrayList<>((List<Object>) updates);
            }
            for (Object update : copy) {
                ProcessUpdates.preserveOneTimeMessage(update);
                ProcessUpdates.recordLastOnlineFromUpdate(currentAccount, update);
            }
        } catch (Throwable t) {
            Main.log("PreserveOneTimeMessages.beforeHookedMethod error: %s", t.getMessage());
        }
    }
}
