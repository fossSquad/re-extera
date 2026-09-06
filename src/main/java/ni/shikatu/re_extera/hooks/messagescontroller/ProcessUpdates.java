package ni.shikatu.re_extera.hooks.messagescontroller;

import androidx.collection.LongSparseArray;
import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.InternalUtils;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

public class ProcessUpdates extends XC_MethodHook {
    private final ReExteraDb redb = ReExteraDb.get();
    private static final java.util.concurrent.atomic.AtomicBoolean isStatusUpdateQueued = new java.util.concurrent.atomic.AtomicBoolean(false);
    public static final java.util.Set<Integer> serverDeletedMessageIds = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>());

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        TLRPC.Updates updates = (TLRPC.Updates) param.args[0];
        ArrayList<TLRPC.Update> filtered = new ArrayList<>();
        LongSparseArray<ArrayList<Integer>> channelDeleted = new LongSparseArray<>();
        if (updates instanceof org.telegram.tgnet.TLRPC.TL_updateShortMessage) {
            org.telegram.tgnet.TLRPC.TL_updateShortMessage shortMsg = (org.telegram.tgnet.TLRPC.TL_updateShortMessage) updates;
            if (ni.shikatu.re_extera.settings.Settings.getSaveLastOnline() && shortMsg.user_id > 0 && shortMsg.date > 0) {
                ReExteraDb.get().saveLastOnlineAsync(shortMsg.user_id, shortMsg.date);
                triggerUIUpdate(currentAccount);
            }
        } else if (updates instanceof org.telegram.tgnet.TLRPC.TL_updateShortChatMessage) {
            org.telegram.tgnet.TLRPC.TL_updateShortChatMessage shortChatMsg = (org.telegram.tgnet.TLRPC.TL_updateShortChatMessage) updates;
            if (ni.shikatu.re_extera.settings.Settings.getSaveLastOnline() && shortChatMsg.from_id > 0 && shortChatMsg.date > 0) {
                ReExteraDb.get().saveLastOnlineAsync(shortChatMsg.from_id, shortChatMsg.date);
                triggerUIUpdate(currentAccount);
            }
        } else if (updates.update != null) {
            processSingleUpdate(updates.update, channelDeleted, currentAccount, null);
        } else if (updates.updates != null) {
            android.util.SparseArray<Long> midToDid = new android.util.SparseArray<>();
            for (TLRPC.Update update : updates.updates) {
                if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) {
                    org.telegram.tgnet.tl.TL_update.TL_updateNewMessage nm = (org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) update;
                    if (nm.message != null) {
                        if (ni.shikatu.re_extera.settings.Settings.getSaveOneTimeMessages()) {
                            if (nm.message.ttl > 0) nm.message.ttl = 0;
                            if (nm.message.media != null && nm.message.media.ttl_seconds > 0) {
                                nm.message.media.ttl_seconds = 0;
                            }
                        }
                        midToDid.put(nm.message.id, MessageUtils.getDialogIdFromMessage(nm.message));
                    }
                } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage) {
                    org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage ncm = (org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage) update;
                    if (ncm.message != null) {
                        if (ni.shikatu.re_extera.settings.Settings.getSaveOneTimeMessages()) {
                            if (ncm.message.ttl > 0) ncm.message.ttl = 0;
                            if (ncm.message.media != null && ncm.message.media.ttl_seconds > 0) {
                                ncm.message.media.ttl_seconds = 0;
                            }
                        }
                        midToDid.put(ncm.message.id, MessageUtils.getDialogIdFromMessage(ncm.message));
                    }
                }
            }
            for (TLRPC.Update update : updates.updates) {
                if (processSingleUpdate(update, channelDeleted, currentAccount, midToDid)) {
                    filtered.add(update);
                }
            }
            updates.updates = filtered;
        }
        flushChannelDeleted(channelDeleted, currentAccount);
        param.args[0] = updates;
    }

    private void flushChannelDeleted(LongSparseArray<ArrayList<Integer>> channelDeleted, final int currentAccount) {
        for (int i = 0; i < channelDeleted.size(); i++) {
            final ArrayList<Integer> ids = (ArrayList) channelDeleted.valueAt(i);
            if (ids != null && !ids.isEmpty()) {
                serverDeletedMessageIds.addAll(ids);
            }
        }
    }

    private void triggerUIUpdate(int currentAccount) {
        final int currentAccountFinal = currentAccount;
        if (isStatusUpdateQueued.compareAndSet(false, true)) {
            org.telegram.messenger.AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    isStatusUpdateQueued.set(false);
                    org.telegram.messenger.NotificationCenter.getInstance(currentAccountFinal).postNotificationName(
                            org.telegram.messenger.NotificationCenter.updateInterfaces,
                            Integer.valueOf(org.telegram.messenger.MessagesController.UPDATE_MASK_STATUS)
                    );
                }
            }, 1000);
        }
    }

    private boolean processSingleUpdate(TLRPC.Update update, LongSparseArray<ArrayList<Integer>> channelDeleted, int currentAccount, android.util.SparseArray<Long> midToDid) {
        boolean keep = true;
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateEditMessage) {
            org.telegram.tgnet.tl.TL_update.TL_updateEditMessage edit = (org.telegram.tgnet.tl.TL_update.TL_updateEditMessage) update;
            processEditedMessage(edit.message, currentAccount);
        } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateEditChannelMessage) {
            org.telegram.tgnet.tl.TL_update.TL_updateEditChannelMessage edit2 = (org.telegram.tgnet.tl.TL_update.TL_updateEditChannelMessage) update;
            processEditedMessage(edit2.message, currentAccount);
        } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages) {
            org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages del = (org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages) update;
            processDeleteMessages(del, currentAccount, midToDid);
        } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages) {
            org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages del2 = (org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages) update;
            processDeleteChannelMessages(del2, channelDeleted);
        } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) {
            org.telegram.tgnet.tl.TL_update.TL_updateNewMessage newMsg = (org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) update;
            keep = !shadowbanFilterHideDialog(newMsg.message);
        } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage) {
            org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage newMsg2 = (org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage) update;
            keep = !shadowbanFilterHideInGroups(newMsg2.message);
        }
        
        if (!keep) {
            return false;
        }

        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateReadHistoryOutbox) {
            org.telegram.tgnet.tl.TL_update.TL_updateReadHistoryOutbox outbox = (org.telegram.tgnet.tl.TL_update.TL_updateReadHistoryOutbox) update;
            long did = org.telegram.messenger.DialogObject.getPeerDialogId(outbox.peer);
            if (ni.shikatu.re_extera.settings.Settings.getSaveReadDate()) {
                ReExteraDb.get().saveReadEventAsync(did, outbox.max_id);
            }
            if (ni.shikatu.re_extera.settings.Settings.getSaveLastOnline() && did > 0) {
                int now = (int) (System.currentTimeMillis() / 1000L);
                ReExteraDb.get().saveLastOnlineAsync(did, now);
                triggerUIUpdate(currentAccount);
            }
        } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateReadChannelOutbox) {
            org.telegram.tgnet.tl.TL_update.TL_updateReadChannelOutbox outbox2 = (org.telegram.tgnet.tl.TL_update.TL_updateReadChannelOutbox) update;
            long did = -outbox2.channel_id;
            if (ni.shikatu.re_extera.settings.Settings.getSaveReadDate()) {
                ReExteraDb.get().saveReadEventAsync(did, outbox2.max_id);
            }
        } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateEncryptedMessagesRead) {
            org.telegram.tgnet.tl.TL_update.TL_updateEncryptedMessagesRead readEncrypted = (org.telegram.tgnet.tl.TL_update.TL_updateEncryptedMessagesRead) update;
            if (ni.shikatu.re_extera.settings.Settings.getSaveLastOnline()) {
                TLRPC.EncryptedChat chat = MessagesController.getInstance(currentAccount).getEncryptedChat(readEncrypted.chat_id);
                if (chat != null && chat.user_id > 0) {
                    int now = (int) (System.currentTimeMillis() / 1000L);
                    ReExteraDb.get().saveLastOnlineAsync(chat.user_id, now);
                    triggerUIUpdate(currentAccount);
                }
            }
        }
        if (ni.shikatu.re_extera.settings.Settings.getSaveLastOnline()) {
            if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateUserStatus) {
                org.telegram.tgnet.tl.TL_update.TL_updateUserStatus statusUpdate = (org.telegram.tgnet.tl.TL_update.TL_updateUserStatus) update;
                if (statusUpdate.status instanceof TLRPC.TL_userStatusOffline) {
                    ReExteraDb.get().saveLastOnlineAsync(statusUpdate.user_id, ((TLRPC.TL_userStatusOffline) statusUpdate.status).expires);
                } else if (statusUpdate.status instanceof TLRPC.TL_userStatusOnline) {
                    ReExteraDb.get().saveLastOnlineAsync(statusUpdate.user_id, (int) (System.currentTimeMillis() / 1000L));
                }
            } else {
                long onlineUserId = 0;
                int onlineDate = 0;
                if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) {
                    org.telegram.tgnet.tl.TL_update.TL_updateNewMessage newMsg = (org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) update;
                    if (newMsg.message != null && !(newMsg.message instanceof TLRPC.TL_messageEmpty) && !newMsg.message.out) {
                        onlineUserId = getFromId(newMsg.message);
                        onlineDate = newMsg.message.date;
                    }
                } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage) {
                    org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage newMsg2 = (org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage) update;
                    if (newMsg2.message != null && !(newMsg2.message instanceof TLRPC.TL_messageEmpty) && !newMsg2.message.out) {
                        onlineUserId = getFromId(newMsg2.message);
                        onlineDate = newMsg2.message.date;
                    }
                } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateEditMessage) {
                    org.telegram.tgnet.tl.TL_update.TL_updateEditMessage editMsg = (org.telegram.tgnet.tl.TL_update.TL_updateEditMessage) update;
                    if (editMsg.message != null && !(editMsg.message instanceof TLRPC.TL_messageEmpty) && !editMsg.message.out) {
                        if (editMsg.message.edit_date > 0) {
                            onlineUserId = getFromId(editMsg.message);
                            onlineDate = editMsg.message.edit_date;
                        }
                    }
                } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateUserTyping) {
                    org.telegram.tgnet.tl.TL_update.TL_updateUserTyping typing = (org.telegram.tgnet.tl.TL_update.TL_updateUserTyping) update;
                    onlineUserId = typing.user_id;
                    onlineDate = (int) (System.currentTimeMillis() / 1000L);
                } else if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateChatUserTyping) {
                    org.telegram.tgnet.tl.TL_update.TL_updateChatUserTyping typing = (org.telegram.tgnet.tl.TL_update.TL_updateChatUserTyping) update;
                    onlineUserId = typing.from_id != null ? typing.from_id.user_id : 0;
                    onlineDate = (int) (System.currentTimeMillis() / 1000L);
                }
                
                if (onlineUserId > 0 && onlineDate > 0) {
                    ReExteraDb.get().saveLastOnlineAsync(onlineUserId, onlineDate);
                    triggerUIUpdate(currentAccount);
                }
            }
        }
        return true;
    }

    private boolean shadowbanFilterHideDialog(TLRPC.Message message) {
        if (message == null) {
            return false;
        }
        long fromId = getFromId(message);
        if (fromId <= 0 || !ShadowbanCache.shouldHideDialog(fromId)) {
            return false;
        }
        Main.log("ProcessUpdates: filtered new message from shadowbanned user %d", Long.valueOf(fromId));
        return true;
    }

    private boolean shadowbanFilterHideInGroups(TLRPC.Message message) {
        if (message == null) {
            return false;
        }
        long fromId = getFromId(message);
        if (fromId <= 0 || !ShadowbanCache.shouldHideInGroups(fromId)) {
            return false;
        }
        Main.log("ProcessUpdates: filtered channel message from shadowbanned user %d", Long.valueOf(fromId));
        return true;
    }

    private static long getFromId(TLRPC.Message message) {
        if (message.from_id instanceof TLRPC.TL_peerUser) {
            return message.from_id.user_id;
        }
        return 0L;
    }

    private void processEditedMessage(final TLRPC.Message message, final int currentAccount) {
        if (message == null || message.out) {
            return;
        }
        final long did = MessageUtils.getDialogIdFromMessage(message);
        if (did == 0) {
            return;
        }
        if (!ni.shikatu.re_extera.settings.Settings.getSaveBotChats()) {
            org.telegram.tgnet.TLRPC.User user = org.telegram.messenger.MessagesController.getInstance(currentAccount).getUser(did);
            if (user != null && user.bot) {
                return;
            }
        }
        this.redb.postToDbThread(new Runnable() {
            @Override
            public void run() {
                MessageObject oldObj = MessageUtils.getMessage(currentAccount, did, message.id);
                if (oldObj != null && !oldObj.isOut() && oldObj.messageOwner != null) {
                    String oldText = oldObj.messageOwner.message != null ? oldObj.messageOwner.message : "";
                    String newText = message.message != null ? message.message : "";
                    boolean textChanged = !oldText.equals(newText);
                    boolean editDateChanged = message.edit_date != 0 && message.edit_date != oldObj.messageOwner.edit_date;
                    boolean mediaChanged = (oldObj.messageOwner.media == null && message.media != null) ||
                                           (oldObj.messageOwner.media != null && message.media == null);
                    if (textChanged || editDateChanged || mediaChanged) {
                        if (!redb.messageHasSavedEdits(did, message.id)) {
                            redb.saveOriginalMessageAsync(did, message.id, oldObj.messageOwner);
                        }
                        redb.saveNewVersionMessageAsync(did, message.id, message);
                    }
                }
            }
        });
    }

    private void processDeleteMessages(org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages update, final int currentAccount, android.util.SparseArray<Long> midToDid) {
        if (update.messages == null) {
            return;
        }
        serverDeletedMessageIds.addAll(update.messages);
    }

    private void processDeleteScheduledMessages(org.telegram.tgnet.tl.TL_update.TL_updateDeleteScheduledMessages update, int currentAccount) {
        long dialogId = DialogObject.getPeerDialogId(update.peer);
        // update.sent_messages = IDs of the NEW real messages that were just delivered
        //   from the scheduled list — these must NOT be deleted, they are live messages.
        // update.messages     = IDs of the scheduled-slot entries being removed.
        //   Only these should be saved as "deleted scheduled messages".
        if (update.sent_messages != null && !update.sent_messages.isEmpty()) {
            Main.log("processDeleteScheduledMessages: ignoring %d sent_messages (live) for did=%d",
                    update.sent_messages.size(), dialogId);
        }
        if (update.messages == null || update.messages.isEmpty()) {
            return;
        }
        ArrayList<Integer> toDelete = new ArrayList<>(update.messages);
        Main.log("processDeleteScheduledMessages: saving %d deleted scheduled ids for did=%d", toDelete.size(), dialogId);
        InternalUtils.deleteMessages(currentAccount, dialogId, toDelete, true);
    }

    private void processDeleteChannelMessages(org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages update, LongSparseArray<ArrayList<Integer>> channelDeleted) {
        if (update.messages == null || update.messages.isEmpty()) {
            return;
        }
        long did = -update.channel_id;
        ArrayList<Integer> acc = (ArrayList) channelDeleted.get(did);
        if (acc == null) {
            acc = new ArrayList<>();
            channelDeleted.put(did, acc);
        }
        acc.addAll(update.messages);
    }
}
