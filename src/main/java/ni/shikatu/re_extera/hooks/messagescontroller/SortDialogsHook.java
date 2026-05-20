package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Predicate;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

public class SortDialogsHook extends XC_MethodHook {
    private static final Field ALL_DIALOGS_FIELD;

    static {
        Field f = null;
        try {
            f = MessagesController.class.getDeclaredField("allDialogs");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("SortDialogsHook: allDialogs field not found: %s", e.getMessage());
        }
        ALL_DIALOGS_FIELD = f;
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (ALL_DIALOGS_FIELD == null) {
            return;
        }
        try {
            ArrayList<TLRPC.Dialog> allDialogs = (ArrayList) ReflectionUtils.get(ALL_DIALOGS_FIELD, param.thisObject);
            if (allDialogs != null && !allDialogs.isEmpty()) {
                allDialogs.removeIf(new Predicate() { // from class: ni.shikatu.re_extera.hooks.messagescontroller.SortDialogsHook$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return SortDialogsHook.lambda$afterHookedMethod$0((TLRPC.Dialog) obj);
                    }
                });
            }
        } catch (Exception e) {
            Main.log("SortDialogsHook: %s", e.getMessage());
        }
    }

    static /* synthetic */ boolean lambda$afterHookedMethod$0(TLRPC.Dialog dialog) {
        return dialog.id > 0 && ShadowbanCache.shouldHideDialog(dialog.id);
    }
}
