package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.AttachmentSaver;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;

public class DeleteDialogHook extends XC_MethodHook {
    private static final ConcurrentHashMap<Long, Long> manualDeleteDialogs = new ConcurrentHashMap<>();

    public static void markManualDelete(long did) {
        manualDeleteDialogs.put(did, System.currentTimeMillis() + 5000L);
    }

    public static boolean isManualDelete(long did) {
        Long expire = manualDeleteDialogs.get(did);
        if (expire == null) {
            return false;
        }
        if (System.currentTimeMillis() > expire) {
            manualDeleteDialogs.remove(did);
            return false;
        }
        return true;
    }

    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        try {
            long did = ((Long) param.args[0]).longValue();
            int messagesOnly = ((Integer) param.args[1]).intValue();
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);

            if (!Settings.getSaveDeletedMessages()) {
                return;
            }
            if (!ReExteraDb.get().shouldSaveDeletedMessages(did)) {
                return;
            }
            if (!Settings.getSaveBotChats() && did != 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                if (user != null && user.bot) {
                    return;
                }
            }

            boolean manual = isManualDelete(did);
            if (manual && messagesOnly == 0 && !Settings.getSaveManuallyDeleted()) {
                manualDeleteDialogs.remove(did);
                return;
            }

            SQLiteDatabase db = null;
            if (param.thisObject instanceof MessagesStorage) {
                db = ((MessagesStorage) param.thisObject).getDatabase();
            } else {
                db = MessagesStorage.getInstance(currentAccount).getDatabase();
            }
            if (db == null) {
                return;
            }

            List<Integer> foundMids = new ArrayList<>();
            long targetDid = did;
            try {
                SQLiteCursor cursor = db.queryFinalized("SELECT uid, mid FROM messages_v2 WHERE (uid = ? OR uid = ?) AND mid > 0", did, -did);
                while (cursor.next()) {
                    long msgUid = cursor.longValue(0);
                    int msgMid = cursor.intValue(1);
                    targetDid = msgUid;
                    foundMids.add(msgMid);
                }
                cursor.dispose();
            } catch (Throwable t) {
                Main.log("DeleteDialogHook: error querying messages_v2: %s", t.getMessage());
            }

            if (!foundMids.isEmpty()) {
                Main.log("DeleteDialogHook: saving %d messages for did=%d (messagesOnly=%d)", foundMids.size(), targetDid, messagesOnly);
                ReExteraDb.get().batchPutDeletedMessagesAsync(targetDid, foundMids);
                MessageUtils.forceUpdateViews(currentAccount, targetDid, foundMids);
                if (Settings.getSaveAttachments()) {
                    AttachmentSaver.saveAttachments(currentAccount, targetDid, new ArrayList<>(foundMids));
                }
            }

            // Prevent deletion of messages_v2 and dialogs:
            // messagesOnly == 1: Clear history only
            // messagesOnly == 2: Clear history except last message
            // messagesOnly == 3: checkLastDialogMessage cleanup of empty dialog
            // messagesOnly == 0: Full dialog deletion (when remote / saveManuallyDeleted is true)
            if (messagesOnly == 1 || messagesOnly == 2 || messagesOnly == 3 || (!manual || Settings.getSaveManuallyDeleted())) {
                try {
                    db.executeFast("UPDATE dialogs SET unread_count = 0, unread_count_i = 0 WHERE did = " + did).stepThis().dispose();
                    db.executeFast("DELETE FROM bot_keyboard WHERE uid = " + did).stepThis().dispose();
                    db.executeFast("DELETE FROM bot_keyboard_topics WHERE uid = " + did).stepThis().dispose();
                    MediaDataController.getInstance(currentAccount).clearBotKeyboard(did);
                    AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.needReloadRecentDialogsSearch));
                } catch (Throwable t) {
                    Main.log("DeleteDialogHook: cleanup error: %s", t.getMessage());
                }
                param.setResult(null);
            }
        } catch (Throwable t) {
            Main.log("DeleteDialogHook error: %s", t.getMessage());
        }
    }
}
