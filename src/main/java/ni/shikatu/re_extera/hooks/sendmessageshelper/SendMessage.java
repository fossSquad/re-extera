package ni.shikatu.re_extera.hooks.sendmessageshelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.TextUtils;
import ni.shikatu.re_extera.utils.UserUtils;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class SendMessage extends XC_MethodHook {
    private static Method isPremium;
    private static Method openScheduledMessages;
    private static final XC_MethodHook returnNullHook;
    private static XC_MethodHook.Unhook unhook;
    private static Method updateBottomOverlay;
    private ReExteraDb redb = ReExteraDb.get();

    static {
        isPremium = null;
        try {
            isPremium = UserConfig.class.getDeclaredMethod("isPremium", new Class[0]);
        } catch (NoSuchMethodException e) {
            ReflectionUtils.hookError();
            Main.log("No such method", e.getMessage());
        }
        returnNullHook = new XC_MethodReplacement() { // from class: ni.shikatu.re_extera.hooks.sendmessageshelper.SendMessage.1
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                return null;
            }
        };
    }

    public SendMessage() {
        try {
            openScheduledMessages = ChatActivity.class.getDeclaredMethod("openScheduledMessages", Integer.TYPE, Boolean.TYPE);
            updateBottomOverlay = ChatActivity.class.getDeclaredMethod("updateBottomOverlay", new Class[0]);
        } catch (NoSuchMethodException e) {
            Main.log("No such method", e.getMessage());
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        SendMessage sendMessage;
        TLRPC.User sender;
        SendMessagesHelper.SendMessageParams params = (SendMessagesHelper.SendMessageParams) param.args[0];
        MessageObject replyToTopMsg = params.replyToTopMsg;
        MessageObject replyToMsg = params.replyToMsg;
        ChatActivity.ReplyQuote replyQuote = params.replyQuote;
        if (replyQuote == null && replyToMsg == null) {
            sendMessage = this;
        } else {
            long did = replyToMsg != null ? replyToMsg.getDialogId() : replyQuote.message.getDialogId();
            int mid = replyToMsg != null ? replyToMsg.getId() : replyQuote.message.getId();
            sendMessage = this;
            boolean isDeleted = sendMessage.redb.messageIsDeleted(did, mid);
            if (replyQuote != null && isDeleted) {
                long peer = replyQuote.peerId;
                TLRPC.User sender2 = UserUtils.getUser(peer);
                if (sender2 != null) {
                    sender = sender2;
                } else {
                    sender = UserUtils.getSelf();
                }
                String newText = String.format("%s\n%s", sender.first_name, replyQuote.getText());
                params.entities.add(TextUtils.createNewBlockQuote(newText));
                params.entities.add(TextUtils.createNewMentionQuote(sender, sender.first_name.length()));
                params.message = newText + params.message;
                params.replyToMsg = replyToTopMsg;
                params.replyToTopMsg = replyToTopMsg;
                params.replyQuote = null;
            } else if (replyToMsg != null && isDeleted) {
                TLRPC.User sender3 = UserUtils.getUser(replyToMsg.getSenderId());
                if (sender3 == null) {
                    sender3 = UserUtils.getSelf();
                }
                String newText2 = String.format("%s\n%s", sender3.first_name, replyToMsg.messageText);
                params.entities.add(TextUtils.createNewBlockQuote(newText2));
                params.entities.add(TextUtils.createNewMentionQuote(sender3, sender3.first_name.length()));
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
        if (Settings.getLocalPremium()) {
            sendMessage.replaceCustomEmojisNoCheck(UserConfig.selectedAccount, params.peer, params.entities, false);
        }
        if (Settings.getSendSilence() == Settings.SendSilence.YES.getType() || (Settings.getSendSilence() == Settings.SendSilence.ONLY_WITH_GHOST.getType() && Settings.getGhostModeEnabledGlobal())) {
            params.notify = false;
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (unhook != null) {
            unhook.unhook();
            unhook = null;
        }
    }

    private void replaceCustomEmojisNoCheck(int currentAccount, long dialogId, ArrayList<TLRPC.MessageEntity> entities, boolean replaceOnlyLocal) {
        TLRPC.ChatFull chatInfo;
        TLRPC.TL_messages_stickerSet stickerSet;
        if (!replaceOnlyLocal && dialogId > 0 && dialogId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return;
        }
        HashSet<Long> groupEmoji = new HashSet<>();
        if (!replaceOnlyLocal && dialogId < 0 && (chatInfo = MessagesController.getInstance(currentAccount).getChatFull(-dialogId)) != null && chatInfo.emojiset != null && (stickerSet = MediaDataController.getInstance(currentAccount).getGroupStickerSetById(chatInfo.emojiset)) != null && stickerSet.documents != null) {
            for (TLRPC.Document document : stickerSet.documents) {
                groupEmoji.add(Long.valueOf(document.id));
            }
        }
        for (int i = 0; i < entities.size(); i++) {
            TLRPC.TL_messageEntityCustomEmoji tL_messageEntityCustomEmoji = (TLRPC.MessageEntity) entities.get(i);
            if (tL_messageEntityCustomEmoji instanceof TLRPC.TL_messageEntityCustomEmoji) {
                TLRPC.TL_messageEntityCustomEmoji emoji = tL_messageEntityCustomEmoji;
                if ((!replaceOnlyLocal || emoji.local) && !groupEmoji.contains(Long.valueOf(emoji.document_id))) {
                    TLRPC.TL_messageEntityTextUrl newEntity = new TLRPC.TL_messageEntityTextUrl();
                    newEntity.offset = emoji.offset;
                    newEntity.length = emoji.length;
                    newEntity.url = "tg://emoji?id=" + emoji.document_id;
                    entities.set(i, newEntity);
                }
            }
        }
    }
}
