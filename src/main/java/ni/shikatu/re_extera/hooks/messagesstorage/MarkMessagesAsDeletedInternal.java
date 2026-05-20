package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.MessageUtils;

public class MarkMessagesAsDeletedInternal extends XC_MethodHook {
    private final ReExteraDb redb = ReExteraDb.get();

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveDeletedMessages()) {
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
            long did = ((Long) param.args[0]).longValue();
            ArrayList<Integer> originalMessages = (ArrayList) param.args[1];
            this.redb.lambda$batchPutDeletedMessagesAsync$1(did, originalMessages);
            MessageUtils.forceUpdateViews(currentAccount, did, originalMessages);
            param.setResult((Object) null);
        }
    }
}
