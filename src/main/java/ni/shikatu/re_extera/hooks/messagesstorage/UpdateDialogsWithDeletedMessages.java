package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.MessageUtils;

public class UpdateDialogsWithDeletedMessages extends XC_MethodHook {
    private final ReExteraDb redb = ReExteraDb.get();

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveDeletedMessages()) {
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
            long uid = ((Long) param.args[0]).longValue();
            long channelId = ((Long) param.args[1]).longValue();
            long did = channelId != 0 ? channelId : uid;
            ArrayList<Integer> ids = (ArrayList) param.args[2];
            if (ids == null || ids.isEmpty()) {
                return;
            }
            this.redb.lambda$batchPutDeletedMessagesAsync$1(did, ids);
            MessageUtils.forceUpdateViews(currentAccount, did, ids);
        }
    }
}
