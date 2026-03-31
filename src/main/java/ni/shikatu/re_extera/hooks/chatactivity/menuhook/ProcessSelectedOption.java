package ni.shikatu.re_extera.hooks.chatactivity.menuhook;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.ui.MessageHistoryFragment;
import ni.shikatu.re_extera.utils.InternalUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.ChatActivity;

public class ProcessSelectedOption extends XC_MethodHook {
    public static MessageObject selectedObject;
    private static Field selectedObjectField;

    static {
        selectedObjectField = null;
        try {
            selectedObjectField = ChatActivity.class.getDeclaredField("selectedObject");
            selectedObjectField.setAccessible(true);
        } catch (Exception e) {
            Main.log("Error on ProcessSelectedOption %s", e.getMessage());
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        int option = ((Integer) param.args[0]).intValue();
        Main.log(String.format("Hooked ProcessSelectedOptionHook with option %s", Integer.valueOf(option)), new Object[0]);
        ChatActivity thisObj = (ChatActivity) param.thisObject;
        if (thisObj == null || selectedObjectField == null) {
            return;
        }
        MessageObject messageObject = (MessageObject) ReflectionUtils.get(selectedObjectField, thisObj);
        if (option == 6363) {
            Main.log("Hooked 6363 match", new Object[0]);
            if (messageObject != null) {
                Main.log("messageObject is not null", new Object[0]);
                MessageHistoryFragment historyFragment = MessageHistoryFragment.newInstance(messageObject.getDialogId(), messageObject.getId());
                thisObj.presentFragment(historyFragment);
            } else if (selectedObject != null) {
                Main.log("selectedObject is not null", new Object[0]);
                MessageHistoryFragment historyFragment2 = MessageHistoryFragment.newInstance(selectedObject.getDialogId(), selectedObject.getId());
                thisObj.presentFragment(historyFragment2);
            }
        }
        if (option == 6565) {
            Main.log("Hooked 6565 match, reading", new Object[0]);
            InternalUtils.sendReadMessage(messageObject, true);
        }
        if (option == 24 && messageObject != null && ReExteraDb.get().messageIsDeleted(messageObject)) {
            SendMessagesHelper.getInstance(messageObject.currentAccount).cancelSendingMessage(messageObject);
            InternalUtils.deleteMessages(messageObject.currentAccount, messageObject.getDialogId(), new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), Long.valueOf(messageObject.getChannelId()), true);
        }
    }
}
