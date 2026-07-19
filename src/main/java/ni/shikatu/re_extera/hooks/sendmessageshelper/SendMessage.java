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
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.TextUtils;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class SendMessage extends XC_MethodHook {
    private static final Method OPEN_SCHEDULED_MESSAGES;
    private static final XC_MethodHook RETURN_NULL;
    private static final Method UPDATE_BOTTOM_OVERLAY;
    private static XC_MethodHook.Unhook unhook;
    private final ReExteraDb redb = ReExteraDb.get();

    static {
        Method openScheduled = null;
        Method updateOverlay = null;
        try {
            openScheduled = ChatActivity.class.getDeclaredMethod("openScheduledMessages", Integer.TYPE, Boolean.TYPE);
            updateOverlay = ChatActivity.class.getDeclaredMethod("updateBottomOverlay", new Class[0]);
        } catch (NoSuchMethodException e) {
            Main.log("SendMessage init: no such method: %s", e.getMessage());
        }
        OPEN_SCHEDULED_MESSAGES = openScheduled;
        UPDATE_BOTTOM_OVERLAY = updateOverlay;
        RETURN_NULL = new XC_MethodReplacement() { // from class: ni.shikatu.re_extera.hooks.sendmessageshelper.SendMessage.1
            public Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) {
                return null;
            }
        };
    }

    private static final class QuoteAuthor {
        final TLRPC.Chat chat;
        final int messageId;
        final String name;
        final TLRPC.User user;

        QuoteAuthor(String name, TLRPC.User user, TLRPC.Chat chat, int messageId) {
            this.name = name;
            this.user = user;
            this.chat = chat;
            this.messageId = messageId;
        }
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        SendMessagesHelper.SendMessageParams params = (SendMessagesHelper.SendMessageParams) param.args[0];
        rewriteReplyToDeleted(params, currentAccount);
        if (params.scheduleDate == 0 && Settings.getUseSchedule() && OPEN_SCHEDULED_MESSAGES != null) {
            unhook = XposedBridge.hookMethod(OPEN_SCHEDULED_MESSAGES, RETURN_NULL);
            params.scheduleDate = (int) MessageUtils.getScheduleTime(currentAccount, params.photo, params.document);
            if (UPDATE_BOTTOM_OVERLAY != null) {
                ChatActivity lastFragment = (ChatActivity) LaunchActivity.getLastFragment();
                if (lastFragment instanceof ChatActivity) {
                    ChatActivity chatActivity = lastFragment;
                    ReflectionUtils.invoke(UPDATE_BOTTOM_OVERLAY, chatActivity, new Object[0]);
                }
            }
        }
        if (Settings.getLocalPremium()) {
            replaceCustomEmojisNoCheck(currentAccount, params.peer, params.entities, false);
        }
        int silenceMode = Settings.getSendSilence();
        if (silenceMode == Settings.SendSilence.YES.getType() || (silenceMode == Settings.SendSilence.ONLY_WITH_GHOST.getType() && Settings.getGhostModeEnabledGlobal())) {
            params.notify = false;
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (unhook != null) {
            unhook.unhook();
            unhook = null;
        }
    }

    private void rewriteReplyToDeleted(SendMessagesHelper.SendMessageParams params, int currentAccount) {
        String body;
        MessageObject replyToTopMsg = params.replyToTopMsg;
        MessageObject replyToMsg = params.replyToMsg;
        ChatActivity.ReplyQuote replyQuote = params.replyQuote;
        if (replyQuote == null && replyToMsg == null) {
            return;
        }
        MessageObject sourceMessage = replyToMsg != null ? replyToMsg : replyQuote.message;
        if (sourceMessage == null) {
            return;
        }
        long did = sourceMessage.getDialogId();
        int mid = sourceMessage.getId();
        if (this.redb.messageIsDeleted(did, mid)) {
            QuoteAuthor author = resolveQuoteAuthor(currentAccount, sourceMessage);
            if (author == null || author.name == null) {
                return;
            }
            if (author.name.isEmpty()) {
                return;
            }
            if (replyQuote != null) {
                body = replyQuote.getText();
            } else {
                body = sourceMessage.messageText != null ? sourceMessage.messageText.toString() : "";
            }
            if (body == null) {
                body = "";
            }
            String header = author.name;
            String newText = header + "\n" + body;
            if (params.entities == null) {
                params.entities = new ArrayList();
            }
            for (TLRPC.MessageEntity entity : params.entities) {
                MessageObject replyToMsg2 = replyToMsg;
                entity.offset += newText.length();
                replyQuote = replyQuote;
                replyToMsg = replyToMsg2;
            }
            params.entities.add(TextUtils.createNewBlockQuote(newText));
            TLRPC.MessageEntity authorEntity = createAuthorEntity(author, header.length());
            if (authorEntity != null) {
                params.entities.add(authorEntity);
            }
            params.message = newText + (params.message != null ? params.message : "");
            params.replyToMsg = replyToTopMsg;
            params.replyToTopMsg = replyToTopMsg;
            params.replyQuote = null;
        }
    }

    private QuoteAuthor resolveQuoteAuthor(int currentAccount, MessageObject messageObject) {
        QuoteAuthor author;
        TLRPC.Message message = messageObject.messageOwner;
        MessagesController controller = MessagesController.getInstance(currentAccount);
        QuoteAuthor author2 = resolvePeerAuthor(controller, message.from_id, message.id);
        if (author2 != null) {
            return author2;
        }
        if (message.post && (message.peer_id instanceof TLRPC.TL_peerChannel)) {
            TLRPC.Chat channel = controller.getChat(Long.valueOf(message.peer_id.channel_id));
            QuoteAuthor author3 = createChatAuthor(channel, message.id);
            if (author3 != null) {
                return author3;
            }
        }
        long senderId = messageObject.getSenderId();
        if (senderId > 0) {
            TLRPC.User user = controller.getUser(Long.valueOf(senderId));
            QuoteAuthor author4 = createUserAuthor(user, message.id);
            if (author4 != null) {
                return author4;
            }
        } else if (senderId < 0) {
            TLRPC.Chat chat = controller.getChat(Long.valueOf(-senderId));
            QuoteAuthor author5 = createChatAuthor(chat, message.id);
            if (author5 != null) {
                return author5;
            }
        }
        if (message.fwd_from != null) {
            QuoteAuthor author6 = resolvePeerAuthor(controller, message.fwd_from.from_id, message.id);
            if (author6 != null) {
                return author6;
            }
            if (message.fwd_from.saved_from_peer != null && (author = resolvePeerAuthor(controller, message.fwd_from.saved_from_peer, message.id)) != null) {
                return author;
            }
            if (message.fwd_from.from_name != null && !message.fwd_from.from_name.isEmpty()) {
                return new QuoteAuthor(message.fwd_from.from_name, null, null, message.id);
            }
        }
        TLRPC.User self = controller.getUser(Long.valueOf(UserConfig.getInstance(currentAccount).clientUserId));
        return createUserAuthor(self, message.id);
    }

    private QuoteAuthor resolvePeerAuthor(MessagesController controller, TLRPC.Peer peer, int messageId) {
        if (peer instanceof TLRPC.TL_peerUser) {
            return createUserAuthor(controller.getUser(Long.valueOf(peer.user_id)), messageId);
        }
        if (peer instanceof TLRPC.TL_peerChannel) {
            return createChatAuthor(controller.getChat(Long.valueOf(peer.channel_id)), messageId);
        }
        if (peer instanceof TLRPC.TL_peerChat) {
            return createChatAuthor(controller.getChat(Long.valueOf(peer.chat_id)), messageId);
        }
        return null;
    }

    private QuoteAuthor createUserAuthor(TLRPC.User user, int messageId) {
        String name;
        if (user == null || (name = UserObject.getUserName(user)) == null || name.isEmpty()) {
            return null;
        }
        return new QuoteAuthor(name, user, null, messageId);
    }

    private QuoteAuthor createChatAuthor(TLRPC.Chat chat, int messageId) {
        if (chat == null || chat.title == null || chat.title.isEmpty()) {
            return null;
        }
        return new QuoteAuthor(chat.title, null, chat, messageId);
    }

    private TLRPC.MessageEntity createAuthorEntity(QuoteAuthor author, int length) {
        if (author.user != null) {
            return TextUtils.createNewMentionQuote(author.user, length);
        }
        String url = createChatUrl(author.chat, author.messageId);
        if (url == null || url.isEmpty()) {
            return null;
        }
        return TextUtils.createNewTextUrlQuote(url, length);
    }

    private String createChatUrl(TLRPC.Chat chat, int messageId) {
        if (chat == null) {
            return null;
        }
        String username = ChatObject.getPublicUsername(chat);
        if (username != null && !username.isEmpty()) {
            return "tg://resolve?domain=" + username;
        }
        if (!ChatObject.isChannel(chat) || chat.id == 0 || messageId <= 0) {
            return null;
        }
        return "tg://privatepost?channel=" + chat.id + "&post=" + messageId;
    }

    private void replaceCustomEmojisNoCheck(int currentAccount, long dialogId, ArrayList<TLRPC.MessageEntity> entities, boolean replaceOnlyLocal) {
        TLRPC.ChatFull chatInfo;
        TLRPC.TL_messages_stickerSet stickerSet;
        if (entities == null || entities.isEmpty()) {
            return;
        }
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
            TLRPC.TL_messageEntityCustomEmoji tL_messageEntityCustomEmoji = (TLRPC.TL_messageEntityCustomEmoji) entities.get(i);
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
