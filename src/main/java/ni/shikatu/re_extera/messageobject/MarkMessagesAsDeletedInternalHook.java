package ni.shikatu.re_extera.messageobject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.Settings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

public class MarkMessagesAsDeletedInternalHook extends XC_MethodHook {
    public static Method markMessagesAsDeletedMethod;
    public static Method updateDialogs;

    static {
        try {
            markMessagesAsDeletedMethod = MessagesStorage.class.getDeclaredMethod("markMessagesAsDeletedInternal", Long.TYPE, ArrayList.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE);
            markMessagesAsDeletedMethod.setAccessible(true);
            updateDialogs = MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessagesInternal", Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class);
            updateDialogs.setAccessible(true);
        } catch (Exception e) {
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        ArrayList<Integer> msgIds;
        long did = ((Long) param.args[0]).longValue();
        if (!Settings.getSaveDeletedMessages() || (msgIds = (ArrayList) param.args[1]) == null || msgIds.isEmpty()) {
            return;
        }
        try {
            DbDeletedStore.get().batchPut(did, msgIds);
        } catch (Exception e) {
            Global.log("DbDeletedStore.batchPut failed: " + e.getMessage());
        }
        Iterator<Integer> it = msgIds.iterator();
        while (it.hasNext()) {
            int mid = it.next().intValue();
            Main.cachedDeleted.add(did + "_" + mid);
        }
        param.setResult((Object) null);
    }

    public static void removeMessages(final ArrayList<Integer> messages, long dialog_id, final Long channelId, boolean shouldNotify) {
        try {
            MessagesStorage storage = MessagesStorage.getInstance(UserConfig.selectedAccount);
            Global.log("Trying to delete already deleted on server message");
            Object[] args = {Long.valueOf(dialog_id), messages, false, 0, 0};
            XposedBridge.invokeOriginalMethod(markMessagesAsDeletedMethod, storage, args);
            if (shouldNotify) {
                updateDialogs.invoke(storage, Long.valueOf(dialog_id), Long.valueOf(channelId != null ? channelId.longValue() : 0L), messages, null);
                AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.messageobject.MarkMessagesAsDeletedInternalHook$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ArrayList arrayList = messages;
                        Long l = channelId;
                        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.messagesDeleted, new Object[]{arrayList, Long.valueOf(l != null ? l.longValue() : 0L), false, false, false, 0});
                    }
                });
            }
        } catch (Exception e) {
        }
    }
}
