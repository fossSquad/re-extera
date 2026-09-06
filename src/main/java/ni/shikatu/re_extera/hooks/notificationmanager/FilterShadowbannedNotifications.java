package ni.shikatu.re_extera.hooks.notificationmanager;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.MessageObject;

public class FilterShadowbannedNotifications extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        ArrayList<MessageObject> messages = (ArrayList) param.args[0];
        if (messages == null || messages.isEmpty()) {
            return;
        }
        boolean saveLastOnline = Settings.getSaveLastOnline();
        Iterator<MessageObject> iterator = messages.iterator();
        while (iterator.hasNext()) {
            MessageObject message = iterator.next();
            if (message != null && !message.isOut()) {
                long fromId = message.getFromChatId();
                if (fromId > 0) {
                    if (saveLastOnline && message.messageOwner != null && message.messageOwner.date > 0) {
                        ReExteraDb.get().saveLastOnlineAsync(fromId, message.messageOwner.date);
                    }
                    boolean isDm = message.getDialogId() > 0;
                    if (ShadowbanCache.shouldHideInGroups(fromId) || (isDm && ShadowbanCache.shouldHideDialog(fromId))) {
                        Main.log("FilterShadowbannedNotifications: filtered from %d (dm=%b)", Long.valueOf(fromId), Boolean.valueOf(isDm));
                        iterator.remove();
                        continue;
                    }
                }
                if (Settings.getFiltersEnabled() && MessageUtils.shouldFilterMessage(message)) {
                    Main.log("FilterShadowbannedNotifications: filtered by regex mid=%d did=%d", Integer.valueOf(message.getId()), Long.valueOf(message.getDialogId()));
                    iterator.remove();
                }
            }
        }
    }
}
