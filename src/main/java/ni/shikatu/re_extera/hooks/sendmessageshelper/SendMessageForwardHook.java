package ni.shikatu.re_extera.hooks.sendmessageshelper;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.utils.MessageForwarder;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

public class SendMessageForwardHook extends XC_MethodHook {
    private final Method canForwardMessageMethod;
    private final ReExteraDb redb = ReExteraDb.get();

    public SendMessageForwardHook() {
        Method method;
        try {
            method = MessageObject.class.getDeclaredMethod("canForwardMessage", new Class[0]);
            method.setAccessible(true);
        } catch (Throwable th) {
            method = null;
        }
        this.canForwardMessageMethod = method;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        boolean needsCopy;
        ArrayList<MessageObject> objects = (ArrayList) param.args[0];
        if (objects == null || objects.isEmpty()) {
            return;
        }
        long peer = ((Long) param.args[1]).longValue();
        boolean notify = ((Boolean) param.args[4]).booleanValue();
        int scheduleDate = ((Integer) param.args[5]).intValue();
        MessageObject replyToTopMsg = (MessageObject) param.args[7];
        int currentAccount = objects.get(0).currentAccount;
        Iterator<MessageObject> it = objects.iterator();
        while (true) {
            if (!it.hasNext()) {
                needsCopy = false;
                break;
            }
            MessageObject obj = it.next();
            if (isRestrictedSourceMessage(obj)) {
                needsCopy = true;
                break;
            }
        }
        if (needsCopy) {
            AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
            MessageForwarder.sendMessageCopy(accountInstance, objects, peer, notify, scheduleDate, replyToTopMsg);
            param.setResult(0);
        }
    }

    private boolean isRestrictedSourceMessage(MessageObject obj) {
        if (obj == null || obj.messageOwner == null) {
            return false;
        }
        boolean canForwardMessage = Boolean.TRUE.equals(ReflectionUtils.invokeOriginalMethod(this.canForwardMessageMethod, obj, new Object[0]));
        return !canForwardMessage || obj.messageOwner.noforwards || obj.hasRevealedExtendedMedia() || isSourcePeerNoForwardsRaw(obj) || obj.getDialogId() == 489000 || this.redb.messageIsDeleted(obj) || obj.isSecret() || obj.isSecretMedia() || obj.isVoiceOnce() || obj.isRoundOnce();
    }

    private boolean isSourcePeerNoForwardsRaw(MessageObject obj) {
        TLRPC.UserFull userFull;
        TLRPC.Chat migratedTo;
        long dialogId = obj.getDialogId();
        MessagesController controller = MessagesController.getInstance(obj.currentAccount);
        if (dialogId >= 0) {
            return dialogId > 0 && (userFull = controller.getUserFull(dialogId)) != null && (userFull.noforwards_peer_enabled || userFull.noforwards_my_enabled);
        }
        TLRPC.Chat chat = controller.getChat(Long.valueOf(-dialogId));
        if (chat == null) {
            return false;
        }
        if (chat.noforwards) {
            return true;
        }
        return (chat.migrated_to == null || (migratedTo = controller.getChat(Long.valueOf(chat.migrated_to.channel_id))) == null || !migratedTo.noforwards) ? false : true;
        return false;
    }
}
