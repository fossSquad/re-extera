package ni.shikatu.re_extera.hooks.dialogsactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.tgnet.TLRPC;

public class GetDialogsArray extends XC_MethodHook {
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Object result = param.getResult();
        if (result == null) {
            return;
        }
        ArrayList<TLRPC.Dialog> dialogs = (ArrayList) result;
        if (dialogs.isEmpty()) {
            return;
        }
        Iterator<TLRPC.Dialog> iterator = dialogs.iterator();
        while (iterator.hasNext()) {
            TLRPC.Dialog dialog = iterator.next();
            if (dialog != null && dialog.id > 0 && ShadowbanCache.shouldHideDialog(dialog.id)) {
                iterator.remove();
            }
        }
    }
}
