package ni.shikatu.re_extera.hooks.sendmessageshelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.TextUtils;
import ni.shikatu.re_extera.utils.UserUtils;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class SendMessage extends XC_MethodHook {
    private static Method openScheduledMessages;
    private static final XC_MethodHook returnNullHook = new XC_MethodReplacement() { // from class: ni.shikatu.re_extera.hooks.sendmessageshelper.SendMessage.1
        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            return null;
        }
    };
    private static XC_MethodHook.Unhook unhook;
    private static Method updateBottomOverlay;
    private ReExteraDb redb = ReExteraDb.get();

    public SendMessage() {
        try {
            openScheduledMessages = ChatActivity.class.getDeclaredMethod("openScheduledMessages", Integer.TYPE, Boolean.TYPE);
            updateBottomOverlay = ChatActivity.class.getDeclaredMethod("updateBottomOverlay", new Class[0]);
        } catch (NoSuchMethodException e) {
            Main.log("No such method", e.getMessage());
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        SendMessagesHelper.SendMessageParams params = (SendMessagesHelper.SendMessageParams) param.args[0];
        MessageObject replyToTopMsg = params.replyToTopMsg;
        MessageObject replyToMsg = params.replyToMsg;
        ChatActivity.ReplyQuote replyQuote = params.replyQuote;
        if (replyQuote != null || replyToMsg != null) {
            long did = replyToMsg != null ? replyToMsg.getDialogId() : replyQuote.message.getDialogId();
            int mid = replyToMsg != null ? replyToMsg.getId() : replyQuote.message.getId();
            boolean isDeleted = this.redb.messageIsDeleted(did, mid);
            if (replyQuote != null && isDeleted) {
                long peer = replyQuote.peerId;
                TLRPC.User sender = UserUtils.getUser(peer);
                String newText = String.format("%s\n%s", sender.first_name, replyQuote.getText());
                params.entities.add(TextUtils.createNewBlockQuote(newText));
                params.entities.add(TextUtils.createNewMentionQuote(sender, sender.first_name.length()));
                params.message = newText + params.message;
                params.replyToMsg = replyToTopMsg;
                params.replyToTopMsg = replyToTopMsg;
                params.replyQuote = null;
            } else if (replyToMsg != null && isDeleted) {
                TLRPC.User sender2 = UserUtils.getUser(replyToMsg.getSenderId());
                String newText2 = String.format("%s\n%s", sender2.first_name, replyToMsg.messageText);
                params.entities.add(TextUtils.createNewBlockQuote(newText2));
                params.entities.add(TextUtils.createNewMentionQuote(sender2, sender2.first_name.length()));
                params.message = newText2 + params.message;
                params.replyToMsg = replyToTopMsg;
                params.replyToTopMsg = replyToTopMsg;
                params.replyQuote = null;
            }
        }
        if (params.scheduleDate == 0 && Settings.getUseSchedule()) {
            unhook = XposedBridge.hookMethod(openScheduledMessages, returnNullHook);
            params.scheduleDate = (int) MessageUtils.getScheduleTime(params.photo, params.document);
            ChatActivity lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment instanceof ChatActivity) {
                ChatActivity chatActivity = lastFragment;
                ReflectionUtils.invoke(updateBottomOverlay, chatActivity, new Object[0]);
            }
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (unhook != null) {
            unhook.unhook();
            unhook = null;
        }
    }
}
