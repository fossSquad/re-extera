package ni.shikatu.re_extera.hooks.messageobject;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.db.ReExteraDb;
import org.telegram.messenger.MessageObject;

public class CanDeleteMessage extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        MessageObject thisObject = (MessageObject) param.thisObject;
        if (ReExteraDb.get().messageIsDeleted(thisObject)) {
            param.setResult(true);
        }
    }
}
