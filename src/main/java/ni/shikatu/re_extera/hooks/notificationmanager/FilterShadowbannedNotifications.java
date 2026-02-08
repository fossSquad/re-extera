package ni.shikatu.re_extera.hooks.notificationmanager;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.MessageObject;

public class FilterShadowbannedNotifications extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        ArrayList<MessageObject> messages = (ArrayList) param.args[0];
        if (messages == null || messages.isEmpty()) {
            return;
        }
        Iterator<MessageObject> iterator = messages.iterator();
        while (iterator.hasNext()) {
            MessageObject message = iterator.next();
            if (message != null && !message.isOut()) {
                long fromId = message.getFromChatId();
                if (fromId > 0) {
                    boolean isDm = message.getDialogId() > 0;
                    if (ShadowbanCache.shouldHideInGroups(fromId) || (isDm && ShadowbanCache.shouldHideDialog(fromId))) {
                        Main.log("FilterShadowbannedNotifications: filtered notification from %d (dm=%b)", Long.valueOf(fromId), Boolean.valueOf(isDm));
                        iterator.remove();
                    }
                }
            }
        }
    }
}
