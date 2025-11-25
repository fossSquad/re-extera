package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class DeleteMessages extends XC_MethodHook {
    private ReExteraDb redb = ReExteraDb.get();

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        ArrayList<Integer> ids = (ArrayList) param.args[0];
        long did = ((Long) param.args[3]).longValue();
        ArrayList<Integer> toDeleteAnyWay = new ArrayList<>();
        Iterator<Integer> it = ids.iterator();
        while (it.hasNext()) {
            int id = it.next().intValue();
            if (this.redb.messageIsDeleted(did, id)) {
                toDeleteAnyWay.add(Integer.valueOf(id));
            }
        }
        InternalUtils.deleteMessages(did, toDeleteAnyWay);
        ids.removeAll(toDeleteAnyWay);
        if (!Settings.getSaveManuallyDeleted()) {
            InternalUtils.deleteMessages(did, ids);
        }
        ChatActivity lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment instanceof ChatActivity) {
            final ChatActivity chatActivity = lastFragment;
            chatActivity.getClass();
            AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.hooks.messagescontroller.DeleteMessages$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    chatActivity.clearSelectionMode();
                }
            });
        }
    }
}
