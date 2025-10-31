package ni.shikatu.re_extera;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class ProcessUpdatesHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        MessagesController controller = (MessagesController) param.thisObject;
        TLRPC.Updates updates = (TLRPC.Updates) param.args[0];
        if (updates.update != null) {
            parseUpdate(controller, updates.update);
        }
        for (TLRPC.Update update : updates.updates) {
            if (update != null) {
                parseUpdate(controller, update);
            }
        }
    }

    void parseUpdate(MessagesController controller, TLRPC.Update update) {
        TLRPC.Message msg;
        long did;
        MessageObject oldObj;
        TLRPC.Message msg2;
        long did2;
        MessageObject oldObj2;
        if ((update instanceof TLRPC.TL_updateEditMessage) && (oldObj2 = getMessage(controller, (did2 = Utils.getDialogIdFromMessage((msg2 = ((TLRPC.TL_updateEditMessage) update).message))), msg2.id)) != null && !oldObj2.messageOwner.equals(msg2) && !oldObj2.isOut()) {
            if (!DbDeletedStore.get().hasEdits(did2, msg2.id)) {
                DbDeletedStore.get().saveOriginalIfAbsent(did2, msg2.id, oldObj2.messageOwner, System.currentTimeMillis());
            }
            DbDeletedStore.get().appendEdit(did2, msg2.id, msg2, System.currentTimeMillis());
        }
        if ((update instanceof TLRPC.TL_updateEditChannelMessage) && (oldObj = getMessage(controller, (did = Utils.getDialogIdFromMessage((msg = ((TLRPC.TL_updateEditChannelMessage) update).message))), msg.id)) != null && !oldObj.messageOwner.equals(msg) && !oldObj.isOut()) {
            if (!DbDeletedStore.get().hasEdits(did, msg.id)) {
                DbDeletedStore.get().saveOriginalIfAbsent(did, msg.id, oldObj.messageOwner, System.currentTimeMillis());
            }
            DbDeletedStore.get().appendEdit(did, msg.id, msg, System.currentTimeMillis());
        }
    }

    MessageObject getMessage(MessagesController controller, long did, int mid) {
        TLRPC.Message msg;
        ArrayList<MessageObject> list;
        MessageObject obj = null;
        if (0 == 0 && did == 0) {
            obj = (MessageObject) controller.dialogMessagesByIds.get(mid);
        }
        if (obj == null && (list = (ArrayList) controller.dialogMessage.get(did)) != null && !list.isEmpty()) {
            for (MessageObject obj1 : list) {
                if (obj1.getId() == mid) {
                    obj = obj1;
                }
            }
        }
        if (obj == null && (msg = MessagesStorage.getInstance(UserConfig.selectedAccount).getMessage(did, mid)) != null) {
            obj = new MessageObject(UserConfig.selectedAccount, msg, false, false);
        }
        if (obj == null) {
            ChatActivity lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment instanceof ChatActivity) {
                ChatActivity chatActivity = lastFragment;
                if (chatActivity.getDialogId() == did || (did == 0 && chatActivity.getCurrentUser() != null)) {
                    for (MessageObject msg2 : chatActivity.messages) {
                        if (msg2 != null && msg2.getId() == mid) {
                            return msg2;
                        }
                    }
                    return obj;
                }
                return obj;
            }
            return obj;
        }
        return obj;
    }
}
