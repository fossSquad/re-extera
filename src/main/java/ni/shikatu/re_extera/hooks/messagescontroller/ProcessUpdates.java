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
    private ReExteraDb redb = ReExteraDb.get();

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        TLRPC.Updates updates = (TLRPC.Updates) param.args[0];
        ArrayList<TLRPC.Update> filtered = new ArrayList<>();
        LongSparseArray<ArrayList<Integer>> channelDeleted = new LongSparseArray<>();
        if (updates.update != null) {
            if (processSingleUpdate(updates.update, channelDeleted, currentAccount)) {
                filtered.add(updates.update);
            }
        } else {
            for (TLRPC.Update update : updates.updates) {
                if (processSingleUpdate(update, channelDeleted, currentAccount)) {
                    filtered.add(update);
                }
            }
            if (!channelDeleted.isEmpty()) {
                for (int i = 0; i < channelDeleted.size(); i++) {
                    long did = channelDeleted.keyAt(i);
                    ArrayList<Integer> ids = (ArrayList) channelDeleted.get(did);
                    if (ids != null && !ids.isEmpty()) {
                        this.redb.batchPutDeletedMessagesAsync(did, ids);
                        MessageUtils.forceUpdateViews(currentAccount, did, ids);
                    }
                }
            }
        }
        updates.updates = filtered;
        param.args[0] = updates;
    }

    private boolean processSingleUpdate(TLRPC.Update update, LongSparseArray<ArrayList<Integer>> channelDeleted, int currentAccount) {
        if (update instanceof TLRPC.TL_updateEditMessage) {
            processTL_updateEditMessage((TLRPC.TL_updateEditMessage) update, currentAccount);
            return true;
        }
        if (update instanceof TLRPC.TL_updateEditChannelMessage) {
            processTL_updateEditChannelMessage((TLRPC.TL_updateEditChannelMessage) update, currentAccount);
            return true;
        }
        if (update instanceof TLRPC.TL_updateDeleteMessages) {
            processTL_updateDeleteMessages((TLRPC.TL_updateDeleteMessages) update, currentAccount);
            return true;
        }
        if (update instanceof TLRPC.TL_updateDeleteChannelMessages) {
            processTL_updateDeleteChannelMessages((TLRPC.TL_updateDeleteChannelMessages) update, channelDeleted);
            return true;
        }
        if (update instanceof TLRPC.TL_updateDeleteScheduledMessages) {
            processTL_updateDeleteScheduledMessages((TLRPC.TL_updateDeleteScheduledMessages) update, currentAccount);
            return true;
        }
        if (update instanceof TLRPC.TL_updateNewMessage) {
            return processTL_updateNewMessage((TLRPC.TL_updateNewMessage) update);
        }
        if (update instanceof TLRPC.TL_updateNewChannelMessage) {
            return processTL_updateNewChannelMessage((TLRPC.TL_updateNewChannelMessage) update);
        }
        return true;
    }

    private boolean processTL_updateNewMessage(TLRPC.TL_updateNewMessage update) {
        if (update.message == null) {
            return true;
        }
        long fromId = getFromId(update.message);
        if (fromId <= 0 || !ShadowbanCache.shouldHideDialog(fromId)) {
            return true;
        }
        Main.log("ProcessUpdates: Filtered new message from shadowbanned user %d", Long.valueOf(fromId));
        return false;
    }

    private boolean processTL_updateNewChannelMessage(TLRPC.TL_updateNewChannelMessage update) {
        if (update.message == null) {
            return true;
        }
        long fromId = getFromId(update.message);
        if (fromId <= 0 || !ShadowbanCache.shouldHideInGroups(fromId)) {
            return true;
        }
        Main.log("ProcessUpdates: Filtered channel message from shadowbanned user %d", Long.valueOf(fromId));
        return false;
    }

    private long getFromId(TLRPC.Message message) {
        if (message.from_id != null && (message.from_id instanceof TLRPC.TL_peerUser)) {
            return message.from_id.user_id;
        }
        return 0L;
    }

    private void processTL_updateEditMessage(TLRPC.TL_updateEditMessage update, int currentAccount) {
        processEditedMessage(update.message, currentAccount);
    }

    private void processTL_updateEditChannelMessage(TLRPC.TL_updateEditChannelMessage update, int currentAccount) {
        processEditedMessage(update.message, currentAccount);
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

    private void processTL_updateDeleteMessages(TLRPC.TL_updateDeleteMessages update, int currentAccount) {
        MessagesController controller = MessagesController.getInstance(currentAccount);
        LongSparseArray<ArrayList<Integer>> toUpdateGrouped = new LongSparseArray<>();
        synchronized (controller) {
            if (update.messages != null) {
                Iterator it = update.messages.iterator();
                while (it.hasNext()) {
                    int id = ((Integer) it.next()).intValue();
                    MessageObject obj = MessageUtils.getMessage(currentAccount, 0L, id);
                    if (obj != null) {
                        long did = obj.getDialogId();
                        ArrayList<Integer> listx = (ArrayList) toUpdateGrouped.get(did);
                        if (listx == null) {
                            listx = new ArrayList<>();
                            toUpdateGrouped.put(did, listx);
                        }
                        listx.add(Integer.valueOf(obj.getId()));
                    }
                }
                for (int i = 0; i < toUpdateGrouped.size(); i++) {
                    long did2 = toUpdateGrouped.keyAt(i);
                    ArrayList<Integer> ids = (ArrayList) toUpdateGrouped.valueAt(i);
                    if (ids != null && !ids.isEmpty()) {
                        this.redb.batchPutDeletedMessagesAsync(did2, ids);
                        MessageUtils.forceUpdateViews(currentAccount, did2, ids);
                    }
                }
            }
        }
    }

    private void processTL_updateDeleteScheduledMessages(TLRPC.TL_updateDeleteScheduledMessages update, int currentAccount) {
        long dialogId = DialogObject.getPeerDialogId(update.peer);
        ArrayList<Integer> allMessages = new ArrayList<>();
        allMessages.addAll(update.sent_messages);
        allMessages.addAll(update.messages);
        InternalUtils.deleteMessages(currentAccount, dialogId, allMessages, null, true);
    }

    private void processTL_updateDeleteChannelMessages(TLRPC.TL_updateDeleteChannelMessages update, LongSparseArray<ArrayList<Integer>> channelDeleted) {
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
