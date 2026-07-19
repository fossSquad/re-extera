package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class DeleteMessages extends XC_MethodHook {
    private final ReExteraDb redb = ReExteraDb.get();

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        ArrayList<Integer> ids = (ArrayList) param.args[0];
        long did = ((Long) param.args[3]).longValue();
        ArrayList<Integer> alreadyLocallyDeleted = new ArrayList<>();
        Iterator<Integer> it = ids.iterator();
        while (it.hasNext()) {
            int id = it.next().intValue();
            if (this.redb.messageIsDeleted(did, id)) {
                alreadyLocallyDeleted.add(Integer.valueOf(id));
            }
        }
        InternalUtils.deleteMessages(currentAccount, did, alreadyLocallyDeleted, null, true);
        ids.removeAll(alreadyLocallyDeleted);
        if (!Settings.getSaveManuallyDeleted()) {
            InternalUtils.deleteMessages(currentAccount, did, ids, null, true);
        }
        org.telegram.ui.ActionBar.BaseFragment lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment instanceof ChatActivity) {
            final ChatActivity chat = (ChatActivity) lastFragment;
            chat.getClass();
            AndroidUtilities.runOnUIThread(new Runnable() { 
                @Override // java.lang.Runnable
                public final void run() {
                    chat.clearSelectionMode();
                }
            });
        }
    }
}
