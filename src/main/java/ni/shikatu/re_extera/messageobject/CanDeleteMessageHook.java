package ni.shikatu.re_extera.messageobject;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.MessageObject;

public class CanDeleteMessageHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        MessageObject thisObject = (MessageObject) param.thisObject;
        if (DbDeletedStore.get().exists(thisObject.getDialogId(), thisObject.getId()) || Main.cachedDeleted.contains(thisObject.getDialogId() + "_" + thisObject.getId())) {
            param.setResult(true);
        }
    }
}
