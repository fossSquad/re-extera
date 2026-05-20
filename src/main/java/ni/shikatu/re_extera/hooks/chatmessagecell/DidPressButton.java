package ni.shikatu.re_extera.hooks.chatmessagecell;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Collections;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.Cells.ChatMessageCell;

public class DidPressButton extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        boolean didPress = ((Boolean) param.args[0]).booleanValue();
        if (didPress) {
            ChatMessageCell cell = (ChatMessageCell) param.thisObject;
            MessageObject messageObject = cell.getMessageObject();
            if (messageObject == null || !messageObject.isOut() || !messageObject.isSending()) {
                return;
            }
            int currentAccount = messageObject.currentAccount;
            SendMessagesHelper.getInstance(currentAccount).cancelSendingMessage(messageObject);
            InternalUtils.deleteMessages(currentAccount, messageObject.getDialogId(), new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), Long.valueOf(messageObject.getChannelId()), true);
            messageObject.loadingCancelled = true;
        }
    }
}
