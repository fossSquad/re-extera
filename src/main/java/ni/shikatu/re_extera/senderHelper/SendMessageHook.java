package ni.shikatu.re_extera.senderHelper;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;

public class SendMessageHook extends XC_MethodHook {
    private static Method updateBottomOverlay;
    private XC_MethodHook.Unhook hooked = null;

    static {
        try {
            updateBottomOverlay = ChatActivity.class.getDeclaredMethod("updateBottomOverlay", new Class[0]);
            updateBottomOverlay.setAccessible(true);
        } catch (NoSuchMethodException e) {
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Global.log("Intercepting send message");
        SendMessagesHelper.SendMessageParams params = (SendMessagesHelper.SendMessageParams) param.args[0];
        MessageObject top = params.replyToTopMsg;
        MessageObject r = params.replyToMsg;
        ChatActivity.ReplyQuote replyQuote = params.replyQuote;
        if (replyQuote != null || r != null) {
            long did = r != null ? r.getDialogId() : replyQuote.message.getDialogId();
            int mid = r != null ? r.getId() : replyQuote.message.getId();
            boolean isDeleted = Main.cachedDeleted.contains(new StringBuilder().append(did).append("_").append(mid).toString()) || DbDeletedStore.get().exists(did, mid);
            if (replyQuote != null && isDeleted) {
                long peer = replyQuote.peerId;
                TLRPC.User user = MessagesController.getInstance(UserConfig.selectedAccount).getUser(Long.valueOf(peer));
                String senderName = user.first_name;
                String newText = String.format("%s\n%s", senderName, replyQuote.getText());
                TLRPC.TL_messageEntityBlockquote quote = new TLRPC.TL_messageEntityBlockquote();
                quote.offset = 0;
                quote.length = newText.length();
                params.entities.add(quote);
                TLRPC.TL_inputMessageEntityMentionName userment = new TLRPC.TL_inputMessageEntityMentionName();
                TLRPC.InputUser userInput = MessagesController.getInstance(UserConfig.selectedAccount).getInputUser(peer);
                userInput.user_id = user.id;
                userInput.access_hash = user.access_hash;
                userment.user_id = userInput;
                userment.offset = 0;
                userment.length = senderName.length();
                params.entities.add(userment);
                params.message = newText + params.message;
                params.replyToMsg = top;
                params.replyQuote = null;
                params.replyToTopMsg = top;
                Global.log(String.format("Changed %s", Integer.valueOf(params.replyToTopMsg.getId())));
                param.args[0] = params;
                return;
            }
            if (r != null && isDeleted) {
                TLRPC.User user2 = MessagesController.getInstance(UserConfig.selectedAccount).getUser(Long.valueOf(params.replyToMsg.getSenderId()));
                String senderName2 = user2.first_name;
                String newText2 = String.format("%s\n%s", senderName2, params.replyToMsg.messageText);
                TLRPC.TL_messageEntityBlockquote quote2 = new TLRPC.TL_messageEntityBlockquote();
                quote2.offset = 0;
                quote2.length = newText2.length();
                params.entities.add(quote2);
                TLRPC.TL_inputMessageEntityMentionName userment2 = new TLRPC.TL_inputMessageEntityMentionName();
                TLRPC.TL_inputUser tL_inputUser = new TLRPC.TL_inputUser();
                ((TLRPC.InputUser) tL_inputUser).user_id = user2.id;
                ((TLRPC.InputUser) tL_inputUser).access_hash = user2.access_hash;
                userment2.user_id = tL_inputUser;
                userment2.offset = 0;
                userment2.length = senderName2.length();
                params.entities.add(userment2);
                params.message = newText2 + params.message;
                params.replyToMsg = top;
                params.replyQuote = null;
                params.replyToTopMsg = top;
                Global.log(String.format("Changed %s", Integer.valueOf(params.replyToTopMsg.getId())));
                param.args[0] = params;
            }
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (this.hooked != null) {
            this.hooked.unhook();
        }
    }

    public static double getScheduleTime(TLRPC.TL_photo photo, TLRPC.TL_document document) {
        TLRPC.PhotoSize mbsize;
        double time = ConnectionsManager.getInstance(UserConfig.selectedAccount).getCurrentTime() + 12.0f;
        if (document != null && document.access_hash != 0 && (MessageObject.isStickerDocument(document) || MessageObject.isAnimatedStickerDocument(document, true))) {
            return (int) Math.ceil(time);
        }
        if (document != null && document.access_hash != 0 && MessageObject.isGifDocument(document)) {
            return (int) Math.ceil(time);
        }
        int photoFileSize = 0;
        if (photo != null && (mbsize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize())) != null) {
            photoFileSize = mbsize.size;
        }
        long documentFileSize = 0;
        if (document != null) {
            documentFileSize = document.size;
        }
        if (photoFileSize != 0) {
            double dMax = Math.max(6.0d, Math.ceil(((photoFileSize / 1024.0f) / 1024.0f) * 4.5f));
            Double.isNaN(time);
            time += dMax;
        }
        if (documentFileSize != 0) {
            time += Math.max(6.0d, Math.ceil(((documentFileSize / 1024.0f) / 1024.0f) * 4.5f));
        }
        return Math.ceil(time);
    }
}
