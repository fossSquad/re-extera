package ni.shikatu.re_extera.chatmessagecell;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Collections;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.messageobject.MarkMessagesAsDeletedInternalHook;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Cells.ChatMessageCell;

public class DidPressButton extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        boolean didPress = ((Boolean) param.args[0]).booleanValue();
        Global.log(String.format("Called didPressMiniButton %s", Boolean.valueOf(didPress)));
        if (didPress) {
            ChatMessageCell cell = (ChatMessageCell) param.thisObject;
            MessageObject messageObject = cell.getMessageObject();
            if (messageObject.isOut() && messageObject.isSending()) {
                SendMessagesHelper.getInstance(UserConfig.selectedAccount).cancelSendingMessage(messageObject);
                MarkMessagesAsDeletedInternalHook.removeMessages(new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), messageObject.getDialogId(), Long.valueOf(messageObject.getChannelId()), true);
                messageObject.loadingCancelled = true;
            }
        }
    }
}
