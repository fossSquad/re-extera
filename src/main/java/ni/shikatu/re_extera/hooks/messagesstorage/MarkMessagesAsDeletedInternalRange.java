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
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if ("deleteMessagesRange".equals(ste.getMethodName())) {
                return;
            }
        }
        if (Settings.getSaveDeletedMessages()) {
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
            long did = ((Long) param.args[0]).longValue();
            int maxMid = ((Integer) param.args[1]).intValue();

            if (did != 0 && !Settings.getSaveBotChats()) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                if (user != null && user.bot) {
                    return;
                }
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
                List<Integer> foundMids = new ArrayList<>();
                try {
                    SQLiteCursor cursor = db.queryFinalized("SELECT mid FROM messages_v2 WHERE uid = ? AND mid <= ?", did, maxMid);
                    while (cursor.next()) {
                        foundMids.add(cursor.intValue(0));
                    }
                    cursor.dispose();
                } catch (Throwable t) {
                    Main.log("MarkMessagesAsDeletedInternalRange: error querying messages_v2: %s", t.getMessage());
                }

                if (!foundMids.isEmpty()) {
                    Main.log("MarkMessagesAsDeletedInternalRange: found %d messages to save for did=%d maxMid=%d", foundMids.size(), did, maxMid);
                    this.redb.lambda$batchPutDeletedMessagesAsync$1(did, foundMids);
                    MessageUtils.forceUpdateViews(currentAccount, did, foundMids);
                    if (Settings.getSaveAttachments()) {
                        AttachmentSaver.saveAttachments(currentAccount, did, new ArrayList<>(foundMids));
                    }
                }
            }

            // Return empty list of randoms to prevent Telegram from deleting them
            param.setResult(new ArrayList<Long>());
        }
    }
}
