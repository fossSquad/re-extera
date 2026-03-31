package ni.shikatu.re_extera.hooks.sendmessageshelper;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.utils.MessageForwarder;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;

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

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        ArrayList<MessageObject> objects = (ArrayList) param.args[0];
        if (objects == null || objects.isEmpty()) {
            return;
        }
        long peer = ((Long) param.args[1]).longValue();
        boolean notify = ((Boolean) param.args[4]).booleanValue();
        int scheduleDate = ((Integer) param.args[5]).intValue();
        MessageObject replyToTopMsg = (MessageObject) param.args[7];
        int currentAccount = objects.get(0).currentAccount;
        ArrayList<MessageObject> noForwards = new ArrayList<>();
        ArrayList<MessageObject> forwards = new ArrayList<>();
        for (MessageObject obj : objects) {
            boolean isNoForwards = isRestrictedSourceMessage(obj);
            if (isNoForwards) {
                noForwards.add(obj);
            } else {
                forwards.add(obj);
            }
        }
        if (!noForwards.isEmpty()) {
            AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
            MessageForwarder.sendMessageCopy(accountInstance, noForwards, peer, notify, scheduleDate, replyToTopMsg);
            if (!forwards.isEmpty()) {
                param.args[0] = forwards;
            } else {
                param.setResult(0);
            }
        }
    }

    private boolean isRestrictedSourceMessage(MessageObject obj) {
        boolean canForwardMessage = Boolean.TRUE.equals(ReflectionUtils.invokeOriginalMethod(this.canForwardMessageMethod, obj, new Object[0]));
        boolean sourcePeerNoForwards = MessagesController.getInstance(obj.currentAccount).isPeerNoForwards(obj.getDialogId());
        return !canForwardMessage || sourcePeerNoForwards || obj.getDialogId() == 489000 || this.redb.messageIsDeleted(obj) || obj.isSecret() || obj.isSecretMedia() || obj.isVoiceOnce() || obj.isRoundOnce();
    }
}
