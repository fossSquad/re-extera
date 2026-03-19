package ni.shikatu.re_extera.hooks.sendmessageshelper;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.utils.MessageForwarder;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;

public class SendMessageForwardHook extends XC_MethodHook {
    private ReExteraDb redb = ReExteraDb.get();

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        ArrayList<MessageObject> objects = (ArrayList) param.args[0];
        long peer = ((Long) param.args[1]).longValue();
        boolean notify = ((Boolean) param.args[4]).booleanValue();
        int scheduleDate = ((Integer) param.args[5]).intValue();
        MessageObject replyToTopMsg = (MessageObject) param.args[7];
        ArrayList<MessageObject> noForwards = new ArrayList<>();
        ArrayList<MessageObject> forwards = new ArrayList<>();
        for (MessageObject obj : objects) {
            boolean isNoForwards = !obj.canForwardMessage() || this.redb.messageIsDeleted(obj) || obj.isSecret() || obj.isSecretMedia() || obj.isVoiceOnce() || obj.isRoundOnce();
            if (isNoForwards) {
                noForwards.add(obj);
            } else {
                forwards.add(obj);
            }
        }
        if (!noForwards.isEmpty()) {
            AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
            MessageForwarder.sendMessageCopy(accountInstance, noForwards, peer, notify, scheduleDate, replyToTopMsg);
            if (!forwards.isEmpty()) {
                param.args[0] = forwards;
            } else {
                param.setResult(0);
            }
        }
    }
}
