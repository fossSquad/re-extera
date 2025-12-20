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
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
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
            ReflectionUtils.hookError();
            Main.log("Error on ProcessSelectedOption %s", e.getMessage());
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        final TLRPC.TL_channels_readHistory tL_messages_readHistory;
        int option = ((Integer) param.args[0]).intValue();
        Main.log(String.format("Hooked ProcessSelectedOptionHook with option %s", Integer.valueOf(option)), new Object[0]);
        ChatActivity thisObj = (ChatActivity) param.thisObject;
        if (thisObj == null) {
            return;
        }
        MessageObject messageObject = (MessageObject) selectedObjectField.get(thisObj);
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
            if (messageObject != null) {
                MessagesController controller = MessagesController.getInstance(UserConfig.selectedAccount);
                messageObject.setIsRead();
                messageObject.setContentIsRead();
                if (messageObject.isFromChannel()) {
                    tL_messages_readHistory = new TLRPC.TL_channels_readHistory();
                    tL_messages_readHistory.channel = MessagesController.getInputChannel(controller.getInputPeer(messageObject.getDialogId()));
                    tL_messages_readHistory.max_id = messageObject.getId();
                } else {
                    tL_messages_readHistory = new TLRPC.TL_messages_readHistory();
                    ((TLRPC.TL_messages_readHistory) tL_messages_readHistory).peer = controller.getInputPeer(messageObject.getDialogId());
                    ((TLRPC.TL_messages_readHistory) tL_messages_readHistory).max_id = messageObject.getId();
                }
                Main.requestSendWithUnhook(new Runnable() { // from class: ni.shikatu.re_extera.hooks.chatactivity.menuhook.ProcessSelectedOption$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(tL_messages_readHistory, new RequestDelegate() { // from class: ni.shikatu.re_extera.hooks.chatactivity.menuhook.ProcessSelectedOption$$ExternalSyntheticLambda1
                            public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                                InternalUtils.createShortVibration();
                            }
                        });
                    }
                });
                if (messageObject.isSecret() || messageObject.isSecretMedia() || messageObject.isRoundOnce() || messageObject.isVoiceOnce()) {
                    InternalUtils.sendSecretMessageRead(messageObject);
                    InternalUtils.createShortVibration();
                }
            }
        }
        if (option == 24 && ReExteraDb.get().messageIsDeleted(messageObject)) {
            SendMessagesHelper.getInstance(UserConfig.selectedAccount).cancelSendingMessage(messageObject);
            InternalUtils.deleteMessages(messageObject.getDialogId(), new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), Long.valueOf(messageObject.getChannelId()), true);
        }
    }
}
