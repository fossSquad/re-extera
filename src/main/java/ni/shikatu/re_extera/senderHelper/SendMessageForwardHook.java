package ni.shikatu.re_extera.senderHelper;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.Utils;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;

public class SendMessageForwardHook extends XC_MethodHook {
    /* JADX WARN: Code duplicated, block: B:24:0x00c8  */
    /* JADX WARN: Code duplicated, block: B:25:0x00cc  */
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Iterator<MessageObject> it;
        boolean canNotBeForward;
        ArrayList<MessageObject> objects = (ArrayList) param.args[0];
        long peer = ((Long) param.args[1]).longValue();
        ((Boolean) param.args[2]).booleanValue();
        ((Boolean) param.args[3]).booleanValue();
        boolean notify = ((Boolean) param.args[4]).booleanValue();
        int scheduleDate = ((Integer) param.args[5]).intValue();
        MessageObject replyToTopMsg = (MessageObject) param.args[6];
        ArrayList<MessageObject> noforwardsMessages = new ArrayList<>();
        ArrayList<MessageObject> normalMessages = new ArrayList<>();
        Iterator<MessageObject> it2 = objects.iterator();
        while (it2.hasNext()) {
            MessageObject msgObj = it2.next();
            if (msgObj.isNoforwards()) {
                it = it2;
            } else {
                it = it2;
                if (!Main.cachedDeleted.contains(msgObj.getDialogId() + "_" + msgObj.getId()) && !DbDeletedStore.get().exists(msgObj.getDialogId(), msgObj.getId()) && !msgObj.isSecret() && !msgObj.isSecretMedia() && !msgObj.isVoiceOnce() && !msgObj.isRoundOnce()) {
                    canNotBeForward = false;
                }
                if (canNotBeForward) {
                    noforwardsMessages.add(msgObj);
                } else {
                    normalMessages.add(msgObj);
                }
                it2 = it;
            }
            canNotBeForward = true;
            if (canNotBeForward) {
                noforwardsMessages.add(msgObj);
            } else {
                normalMessages.add(msgObj);
            }
            it2 = it;
        }
        if (!noforwardsMessages.isEmpty()) {
            AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
            Utils.sendMessageCopy(accountInstance, noforwardsMessages, peer, notify, scheduleDate, replyToTopMsg);
            if (normalMessages.isEmpty()) {
                param.setResult(0);
            } else {
                param.args[0] = normalMessages;
            }
        }
    }
}
