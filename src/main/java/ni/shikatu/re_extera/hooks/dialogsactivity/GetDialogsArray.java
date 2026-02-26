package ni.shikatu.re_extera.hooks.dialogsactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.function.Predicate;
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
        dialogs.removeIf(new Predicate() { // from class: ni.shikatu.re_extera.hooks.dialogsactivity.GetDialogsArray$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GetDialogsArray.lambda$afterHookedMethod$0((TLRPC.Dialog) obj);
            }
        });
    }

    static /* synthetic */ boolean lambda$afterHookedMethod$0(TLRPC.Dialog dialog) {
        return dialog != null && dialog.id > 0 && ShadowbanCache.shouldHideDialog(dialog.id);
    }
}
