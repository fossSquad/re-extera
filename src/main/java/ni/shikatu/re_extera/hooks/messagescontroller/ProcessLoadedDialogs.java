package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.tgnet.TLRPC;

public class ProcessLoadedDialogs extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        Object dialogsRes = param.args[0];
        if (!(dialogsRes instanceof TLRPC.messages_Dialogs)) {
            return;
        }
        TLRPC.messages_Dialogs messagesDialogs = (TLRPC.messages_Dialogs) dialogsRes;
        ArrayList<TLRPC.Dialog> dialogs = messagesDialogs.dialogs;
        if (dialogs == null || dialogs.isEmpty()) {
            return;
        }
        int removed = 0;
        Iterator<TLRPC.Dialog> iterator = dialogs.iterator();
        while (iterator.hasNext()) {
            TLRPC.Dialog dialog = iterator.next();
            if (dialog.id > 0 && ShadowbanCache.shouldHideDialog(dialog.id)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            Main.log("ProcessLoadedDialogs: filtered %d shadowbanned dialogs", Integer.valueOf(removed));
        }
    }
}
