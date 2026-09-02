package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.TLRPC;

public class UpdateDialogsWithDeletedMessages extends XC_MethodHook {
    private final ReExteraDb redb = ReExteraDb.get();

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if ("deleteMessagesRange".equals(ste.getMethodName())) {
                return;
            }
        }
        if (Settings.getSaveDeletedMessages()) {
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
            long uid = ((Long) param.args[0]).longValue();
            long channelId = ((Long) param.args[1]).longValue();
            long did = channelId != 0 ? -channelId : uid;
            
            if (did != 0 && !Settings.getSaveBotChats()) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                if (user != null && user.bot) {
                    return;
                }
            }
            
            ArrayList<Integer> ids = (ArrayList) param.args[2];
            if (ids == null || ids.isEmpty()) {
                return;
            }
            
            ArrayList<Integer> validIds = new ArrayList<>();
            ArrayList<Integer> tempIds = new ArrayList<>();
            for (Integer id : ids) {
                if (id != null && id > 0) {
                    validIds.add(id);
                } else if (id != null) {
                    tempIds.add(id);
                }
            }

            if (!validIds.isEmpty()) {
                Map<Long, List<Integer>> messagesByDialog = new HashMap<>();
                if (did != 0) {
                    messagesByDialog.put(did, validIds);
                } else {
                    SQLiteDatabase db = null;
                    try {
                        if (param.thisObject instanceof MessagesStorage) {
                            db = ((MessagesStorage) param.thisObject).getDatabase();
                        } else {
                            db = MessagesStorage.getInstance(currentAccount).getDatabase();
                        }
                    } catch (Throwable t) {
                        Main.log("UpdateDialogsWithDeletedMessages: failed to get database: %s", t.getMessage());
                    }

                    List<Integer> unresolvedIds = new ArrayList<>(validIds);
                    if (db != null) {
                        try {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < validIds.size(); i++) {
                                if (i > 0) sb.append(",");
                                sb.append(validIds.get(i));
                            }
                            SQLiteCursor cursor = db.queryFinalized("SELECT uid, mid FROM messages_v2 WHERE mid IN (" + sb.toString() + ") AND is_channel = 0");
                            while (cursor.next()) {
                                long msgUid = cursor.longValue(0);
                                int msgMid = cursor.intValue(1);
                                unresolvedIds.remove(Integer.valueOf(msgMid));
                                List<Integer> list = messagesByDialog.get(msgUid);
                                if (list == null) {
                                    list = new ArrayList<>();
                                    messagesByDialog.put(msgUid, list);
                                }
                                list.add(msgMid);
                            }
                            cursor.dispose();
                        } catch (Throwable t) {
                            Main.log("UpdateDialogsWithDeletedMessages: error querying messages_v2: %s", t.getMessage());
                        }
                    }

                    if (!unresolvedIds.isEmpty()) {
                        for (Integer mid : unresolvedIds) {
                            long foundDid = 0;
                            try {
                                org.telegram.messenger.MessageObject obj = MessageUtils.getMessage(currentAccount, 0, mid);
                                if (obj != null) {
                                    foundDid = obj.getDialogId();
                                }
                            } catch (Throwable ignored) {}
                            List<Integer> list = messagesByDialog.get(foundDid);
                            if (list == null) {
                                list = new ArrayList<>();
                                messagesByDialog.put(foundDid, list);
                            }
                            list.add(mid);
                        }
                    }
                }

                for (Map.Entry<Long, List<Integer>> entry : messagesByDialog.entrySet()) {
                    long targetDid = entry.getKey();
                    List<Integer> targetIds = entry.getValue();
                    if (!Settings.getSaveBotChats() && targetDid != 0) {
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(targetDid);
                        if (user != null && user.bot) {
                            continue;
                        }
                    }
                    Main.log("UpdateDialogsWithDeletedMessages: intercepting %d ids for did=%d (args=%d)", targetIds.size(), targetDid, param.args.length);
                    this.redb.lambda$batchPutDeletedMessagesAsync$1(targetDid, targetIds);
                    MessageUtils.forceUpdateViews(currentAccount, targetDid, targetIds);
                }
            }

            param.args[2] = tempIds;
            boolean isInternalVariant = param.args.length == 4;
            if (isInternalVariant && tempIds.isEmpty()) {
                param.setResult((Object) null);
            }
        }
    }
}