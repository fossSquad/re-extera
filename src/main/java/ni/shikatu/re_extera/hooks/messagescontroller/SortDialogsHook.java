package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

public class SortDialogsHook extends XC_MethodHook {
    private static Field allDialogsField;

    static {
        try {
            allDialogsField = MessagesController.class.getDeclaredField("allDialogs");
            allDialogsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("SortDialogsHook: allDialogs field not found: %s", e.getMessage());
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (allDialogsField == null) {
            return;
        }
        MessagesController controller = (MessagesController) param.thisObject;
        ArrayList<TLRPC.Dialog> allDialogs = (ArrayList) allDialogsField.get(controller);
        if (allDialogs == null || allDialogs.isEmpty()) {
            return;
        }
        Iterator<TLRPC.Dialog> iterator = allDialogs.iterator();
        while (iterator.hasNext()) {
            TLRPC.Dialog dialog = iterator.next();
            if (dialog.id > 0 && ShadowbanCache.shouldHideDialog(dialog.id)) {
                iterator.remove();
            }
        }
    }
}
