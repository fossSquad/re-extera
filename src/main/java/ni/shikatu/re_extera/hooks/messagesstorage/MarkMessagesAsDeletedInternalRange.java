package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.List;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.AttachmentSaver;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.TLRPC;

public class MarkMessagesAsDeletedInternalRange extends XC_MethodHook {
    private final ReExteraDb redb = ReExteraDb.get();

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (DeleteMessagesRangeFlag.ACTIVE.get()) {
            return;
        }
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        long did = ((Long) param.args[0]).longValue();
        int maxMid = ((Integer) param.args[1]).intValue();

        if (did != 0) {
            if (!this.redb.shouldSaveDeletedMessages(did)) {
                return;
            }
            if (!Settings.getSaveBotChats()) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                if (user != null && user.bot) {
                    return;
                }
            }
        } else if (!Settings.getSaveDeletedMessages()) {
            return;
        }

        SQLiteDatabase db = null;
        try {
            if (param.thisObject instanceof MessagesStorage) {
                db = ((MessagesStorage) param.thisObject).getDatabase();
            } else {
                db = MessagesStorage.getInstance(currentAccount).getDatabase();
            }
        } catch (Throwable t) {
            Main.log("MarkMessagesAsDeletedInternalRange: failed to get database: %s", t.getMessage());
        }

        if (db != null) {
            java.util.Map<Long, List<Integer>> midsByUid = new java.util.HashMap<>();
            try {
                SQLiteCursor cursor = db.queryFinalized("SELECT uid, mid FROM messages_v2 WHERE (uid = ? OR uid = ?) AND mid <= ?", did, -did, maxMid);
                while (cursor.next()) {
                    long msgUid = cursor.longValue(0);
                    int msgMid = cursor.intValue(1);
                    if (msgMid > 0) {
                        List<Integer> list = midsByUid.get(msgUid);
                        if (list == null) {
                            list = new ArrayList<>();
                            midsByUid.put(msgUid, list);
                        }
                        list.add(msgMid);
                    }
                }
                cursor.dispose();
            } catch (Throwable t) {
                Main.log("MarkMessagesAsDeletedInternalRange: error querying messages_v2: %s", t.getMessage());
            }

            for (java.util.Map.Entry<Long, List<Integer>> entry : midsByUid.entrySet()) {
                long targetUid = entry.getKey();
                List<Integer> foundMids = entry.getValue();
                if (!foundMids.isEmpty()) {
                    Main.log("MarkMessagesAsDeletedInternalRange: found %d messages to save for uid=%d maxMid=%d", foundMids.size(), targetUid, maxMid);
                    this.redb.batchPutDeletedMessagesAsync(targetUid, foundMids);
                    MessageUtils.forceUpdateViews(currentAccount, targetUid, foundMids);
                    if (Settings.getSaveAttachments()) {
                        AttachmentSaver.saveAttachments(currentAccount, targetUid, new ArrayList<>(foundMids));
                    }
                }
            }
        }

        // Return empty list of randoms to prevent Telegram from deleting them
        param.setResult(new ArrayList<Long>());
    }
}
