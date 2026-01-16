package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.tgnet.TLRPC;

public class ProcessLoadedDialogs extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Object dialogsRes = param.args[0];
        if (dialogsRes == null) {
            return;
        }
        ArrayList<TLRPC.Dialog> dialogs = null;
        if (dialogsRes instanceof TLRPC.messages_Dialogs) {
            dialogs = ((TLRPC.messages_Dialogs) dialogsRes).dialogs;
        }
        if (dialogs == null || dialogs.isEmpty()) {
            return;
        }
        Iterator<TLRPC.Dialog> iterator = dialogs.iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            TLRPC.Dialog dialog = iterator.next();
            if (dialog.id > 0 && ShadowbanCache.shouldHideDialog(dialog.id)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            Main.log("ProcessLoadedDialogs: Filtered %d shadowbanned dialogs", Integer.valueOf(removed));
        }
    }
}
