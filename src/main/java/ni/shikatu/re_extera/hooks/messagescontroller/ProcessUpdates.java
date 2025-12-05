package ni.shikatu.re_extera.hooks.messagescontroller;

import androidx.collection.LongSparseArray;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.utils.InternalUtils;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

public class ProcessUpdates extends XC_MethodHook {
    private ReExteraDb redb = ReExteraDb.get();

    private static MessagesController getController() {
        return MessagesController.getInstance(UserConfig.selectedAccount);
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        TLRPC.Updates updates = (TLRPC.Updates) param.args[0];
        ArrayList<TLRPC.Update> filtered = new ArrayList<>();
        LongSparseArray<ArrayList<Integer>> channelDeleted = new LongSparseArray<>();
        if (updates.update != null) {
            if (processSingleUpdate(updates.update, channelDeleted)) {
                filtered.add(updates.update);
            }
        } else {
            for (TLRPC.Update update : updates.updates) {
                if (processSingleUpdate(update, channelDeleted)) {
                    filtered.add(update);
                }
            }
            if (!channelDeleted.isEmpty()) {
                for (int i = 0; i < channelDeleted.size(); i++) {
                    long did = channelDeleted.keyAt(i);
                    ArrayList<Integer> ids = (ArrayList) channelDeleted.get(did);
                    if (ids != null && !ids.isEmpty()) {
                        this.redb.batchPutDeletedMessages(did, ids);
                        MessageUtils.forceUpdateViews(did, ids);
                    }
                }
            }
        }
        updates.updates = filtered;
        param.args[0] = updates;
    }

    private boolean processSingleUpdate(TLRPC.Update update, LongSparseArray<ArrayList<Integer>> channelDeleted) throws IllegalAccessException, InvocationTargetException {
        if (update instanceof TLRPC.TL_updateEditMessage) {
            processTL_updateEditMessage((TLRPC.TL_updateEditMessage) update);
            return true;
        }
        if (update instanceof TLRPC.TL_updateEditChannelMessage) {
            processTL_updateEditChannelMessage((TLRPC.TL_updateEditChannelMessage) update);
            return true;
        }
        if (update instanceof TLRPC.TL_updateDeleteMessages) {
            processTL_updateDeleteMessages((TLRPC.TL_updateDeleteMessages) update);
            return true;
        }
        if (update instanceof TLRPC.TL_updateDeleteChannelMessages) {
            processTL_updateDeleteChannelMessages((TLRPC.TL_updateDeleteChannelMessages) update, channelDeleted);
            return true;
        }
        if (!(update instanceof TLRPC.TL_updateDeleteScheduledMessages)) {
            return true;
        }
        processTL_updateDeleteScheduledMessages((TLRPC.TL_updateDeleteScheduledMessages) update);
        return true;
    }

    private void processTL_updateEditMessage(TLRPC.TL_updateEditMessage update) throws IllegalAccessException, InvocationTargetException {
        processEditedMessage(update.message);
    }

    private void processTL_updateEditChannelMessage(TLRPC.TL_updateEditChannelMessage update) throws IllegalAccessException, InvocationTargetException {
        processEditedMessage(update.message);
    }

    private void processEditedMessage(TLRPC.Message message) throws IllegalAccessException, InvocationTargetException {
        long did = MessageUtils.getDialogIdFromMessage(message);
        MessageObject oldObj = MessageUtils.getMessage(did, message.id);
        if (oldObj != null && !oldObj.isOut()) {
            if (!this.redb.messageHasSavedEdits(did, message.id)) {
                this.redb.saveOriginalMessage(did, message.id, oldObj.messageOwner);
            }
            this.redb.saveNewVersionMessage(did, message.id, message);
        }
    }

    private void processTL_updateDeleteMessages(TLRPC.TL_updateDeleteMessages update) {
        MessagesController controller = getController();
        LongSparseArray<ArrayList<Integer>> toUpdateGrouped = new LongSparseArray<>();
        synchronized (controller) {
            if (update.messages != null) {
                Iterator it = update.messages.iterator();
                while (it.hasNext()) {
                    int id = ((Integer) it.next()).intValue();
                    MessageObject obj = MessageUtils.getMessage(0L, id);
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
                        this.redb.batchPutDeletedMessages(did2, ids);
                        MessageUtils.forceUpdateViews(did2, ids);
                    }
                }
            }
        }
    }

    private void processTL_updateDeleteScheduledMessages(TLRPC.TL_updateDeleteScheduledMessages update) {
        long dialogId = DialogObject.getPeerDialogId(update.peer);
        ArrayList<Integer> allMessages = new ArrayList<>();
        allMessages.addAll(update.sent_messages);
        allMessages.addAll(update.messages);
        InternalUtils.deleteMessages(dialogId, allMessages);
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
