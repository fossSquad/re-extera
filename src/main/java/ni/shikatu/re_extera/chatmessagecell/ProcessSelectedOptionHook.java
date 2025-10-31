package ni.shikatu.re_extera.chatmessagecell;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.chatactivity.ProcessDeletedMessages;
import ni.shikatu.re_extera.messageobject.MarkMessagesAsDeletedInternalHook;
import ni.shikatu.re_extera.messageobject.MessageHistoryFragment;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ChatActivity;

public class ProcessSelectedOptionHook extends XC_MethodHook {
    public static MessageObject selectedObject;
    private static Field selectedObjectField;

    static {
        selectedObjectField = null;
        try {
            selectedObjectField = ChatActivity.class.getDeclaredField("selectedObject");
            selectedObjectField.setAccessible(true);
        } catch (Exception e) {
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        int option = ((Integer) param.args[0]).intValue();
        Global.log(String.format("Hooked ProcessSelectedOptionHook with option %s", Integer.valueOf(option)));
        ChatActivity thisObj = (ChatActivity) param.thisObject;
        if (thisObj == null) {
            return;
        }
        MessageObject messageObject = (MessageObject) selectedObjectField.get(thisObj);
        if (option == 6363) {
            Global.log("Hooked 6363 match");
            if (messageObject != null) {
                Global.log("messageObject is not null");
                MessageHistoryFragment historyFragment = MessageHistoryFragment.newInstance(messageObject.getDialogId(), messageObject.getId());
                thisObj.presentFragment(historyFragment);
            } else if (selectedObject != null) {
                Global.log("selectedObject is not null");
                MessageHistoryFragment historyFragment2 = MessageHistoryFragment.newInstance(selectedObject.getDialogId(), selectedObject.getId());
                thisObj.presentFragment(historyFragment2);
            }
        }
        if (option == 1 && (DbDeletedStore.get().exists(messageObject.getDialogId(), messageObject.getId()) || Main.cachedDeleted.contains(messageObject.getDialogId() + "_" + messageObject.getId()))) {
            ProcessDeletedMessages.onRequestToDelete.add(Integer.valueOf(messageObject.getId()));
            MarkMessagesAsDeletedInternalHook.removeMessages(new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), messageObject.getDialogId(), Long.valueOf(messageObject.getChannelId()), true);
            param.setResult((Object) null);
        }
        if (option == 24) {
            if (DbDeletedStore.get().exists(messageObject.getDialogId(), messageObject.getId()) || Main.cachedDeleted.contains(messageObject.getDialogId() + "_" + messageObject.getId())) {
                ProcessDeletedMessages.onRequestToDelete.add(Integer.valueOf(messageObject.getId()));
                SendMessagesHelper.getInstance(UserConfig.selectedAccount).cancelSendingMessage(messageObject);
                MarkMessagesAsDeletedInternalHook.removeMessages(new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), messageObject.getDialogId(), Long.valueOf(messageObject.getChannelId()), true);
            }
        }
    }
}
