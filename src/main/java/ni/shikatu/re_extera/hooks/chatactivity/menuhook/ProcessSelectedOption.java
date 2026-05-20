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
    public static final int OPT_DELETE = 24;
    public static final int OPT_MESSAGE_HISTORY = 6363;
    public static final int OPT_READ_MESSAGE = 6565;
    private static final Field SELECTED_OBJECT_FIELD;
    public static MessageObject selectedObject;

    static {
        Field f = null;
        try {
            f = ChatActivity.class.getDeclaredField("selectedObject");
            f.setAccessible(true);
        } catch (Exception e) {
            Main.log("ProcessSelectedOption: %s", e.getMessage());
        }
        SELECTED_OBJECT_FIELD = f;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int option = ((Integer) param.args[0]).intValue();
        ChatActivity thisObj = (ChatActivity) param.thisObject;
        if (thisObj == null || SELECTED_OBJECT_FIELD == null) {
            return;
        }
        MessageObject messageObject = (MessageObject) ReflectionUtils.get(SELECTED_OBJECT_FIELD, thisObj);
        if (option == 6363) {
            MessageObject target = messageObject != null ? messageObject : selectedObject;
            if (target != null) {
                thisObj.presentFragment(MessageHistoryFragment.newInstance(target.getDialogId(), target.getId()));
                return;
            }
            return;
        }
        if (option == 6565) {
            InternalUtils.sendReadMessage(messageObject, true);
            return;
        }
        if (option == 24 && messageObject != null && ReExteraDb.get().messageIsDeleted(messageObject)) {
            int currentAccount = messageObject.currentAccount;
            SendMessagesHelper.getInstance(currentAccount).cancelSendingMessage(messageObject);
            InternalUtils.deleteMessages(currentAccount, messageObject.getDialogId(), new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), Long.valueOf(messageObject.getChannelId()), true);
        }
    }
}
