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

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        TLRPC.Updates updates = (TLRPC.Updates) param.args[0];
        ArrayList<TLRPC.Update> filtered = new ArrayList<>();
        LongSparseArray<ArrayList<Integer>> channelDeleted = new LongSparseArray<>();
        if (updates.update != null) {
            if (!processSingleUpdate(updates.update, channelDeleted, currentAccount)) {
                param.setResult((Object) null);
                return;
            }
        } else {
            for (TLRPC.Update update : updates.updates) {
                if (processSingleUpdate(update, channelDeleted, currentAccount)) {
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
            final long did = channelDeleted.keyAt(i);
            final ArrayList<Integer> ids = (ArrayList) channelDeleted.valueAt(i);
            if (ids != null && !ids.isEmpty()) {
                this.redb.batchPutDeletedMessagesAsync(did, ids);
                this.redb.postToDbThread(new Runnable() {
                    @Override
                    public void run() {
                        MessageUtils.forceUpdateViews(currentAccount, did, ids);
                    }
                });
            }
        }
    }

    private boolean processSingleUpdate(TLRPC.Update update, LongSparseArray<ArrayList<Integer>> channelDeleted, int currentAccount) {
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateEditMessage) {
            org.telegram.tgnet.tl.TL_update.TL_updateEditMessage edit = (org.telegram.tgnet.tl.TL_update.TL_updateEditMessage) update;
            processEditedMessage(edit.message, currentAccount);
            return true;
        }
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateEditChannelMessage) {
            org.telegram.tgnet.tl.TL_update.TL_updateEditChannelMessage edit2 = (org.telegram.tgnet.tl.TL_update.TL_updateEditChannelMessage) update;
            processEditedMessage(edit2.message, currentAccount);
            return true;
        }
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages) {
            org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages del = (org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages) update;
            processDeleteMessages(del, currentAccount);
            return true;
        }
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages) {
            org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages del2 = (org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages) update;
            processDeleteChannelMessages(del2, channelDeleted);
            return true;
        }
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateDeleteScheduledMessages) {
            org.telegram.tgnet.tl.TL_update.TL_updateDeleteScheduledMessages del3 = (org.telegram.tgnet.tl.TL_update.TL_updateDeleteScheduledMessages) update;
            processDeleteScheduledMessages(del3, currentAccount);
            return true;
        }
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) {
            org.telegram.tgnet.tl.TL_update.TL_updateNewMessage newMsg = (org.telegram.tgnet.tl.TL_update.TL_updateNewMessage) update;
            return true ^ shadowbanFilterHideDialog(newMsg.message);
        }
        if (!(update instanceof org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage)) {
            return true;
        }
        org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage newMsg2 = (org.telegram.tgnet.tl.TL_update.TL_updateNewChannelMessage) update;
        return true ^ shadowbanFilterHideInGroups(newMsg2.message);
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

    private void processEditedMessage(TLRPC.Message message, int currentAccount) {
        long did = MessageUtils.getDialogIdFromMessage(message);
        MessageObject oldObj = MessageUtils.getMessage(currentAccount, did, message.id);
        if (oldObj != null && !oldObj.isOut()) {
            if (!this.redb.messageHasSavedEdits(did, message.id)) {
                this.redb.saveOriginalMessageAsync(did, message.id, oldObj.messageOwner);
            }
            this.redb.saveNewVersionMessageAsync(did, message.id, message);
        }
    }

    private void processDeleteMessages(org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages update, final int currentAccount) {
        if (update.messages == null) {
            return;
        }
        MessagesController controller = MessagesController.getInstance(currentAccount);
        LongSparseArray<ArrayList<Integer>> toUpdateGrouped = new LongSparseArray<>();
        synchronized (controller) {
            Iterator it = update.messages.iterator();
            while (it.hasNext()) {
                int id = ((Integer) it.next()).intValue();
                MessageObject obj = MessageUtils.getMessage(currentAccount, 0L, id);
                if (obj != null) {
                    if (obj.messageOwner != null && obj.messageOwner.peer_id instanceof TLRPC.TL_peerChannel) {
                        continue;
                    }
                    long did = obj.getDialogId();
                    ArrayList<Integer> list = (ArrayList) toUpdateGrouped.get(did);
                    if (list == null) {
                        list = new ArrayList<>();
                        toUpdateGrouped.put(did, list);
                    }
                    list.add(Integer.valueOf(obj.getId()));
                }
            }
            for (int i = 0; i < toUpdateGrouped.size(); i++) {
                final long did2 = toUpdateGrouped.keyAt(i);
                final ArrayList<Integer> ids = (ArrayList) toUpdateGrouped.valueAt(i);
                if (ids != null && !ids.isEmpty()) {
                    this.redb.batchPutDeletedMessagesAsync(did2, ids);
                    this.redb.postToDbThread(new Runnable() {
                        @Override
                        public void run() {
                            MessageUtils.forceUpdateViews(currentAccount, did2, ids);
                        }
                    });
                }
            }
        }
    }

    private void processDeleteScheduledMessages(org.telegram.tgnet.tl.TL_update.TL_updateDeleteScheduledMessages update, int currentAccount) {
        long dialogId = DialogObject.getPeerDialogId(update.peer);
        ArrayList<Integer> all = new ArrayList<>();
        if (update.sent_messages != null) {
            all.addAll(update.sent_messages);
        }
        if (update.messages != null) {
            all.addAll(update.messages);
        }
        if (all.isEmpty()) {
            return;
        }
        InternalUtils.deleteMessages(currentAccount, dialogId, all, true);
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
