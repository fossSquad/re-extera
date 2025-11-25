package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;

public class MarkMessagesAsDeletedInternal extends XC_MethodHook {
    private ReExteraDb redb = ReExteraDb.get();

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Main.log("Calling MarkMessagesAsDeletedInternal", new Object[0]);
        if (Settings.getSaveDeletedMessages()) {
            long did = ((Long) param.args[0]).longValue();
            ArrayList<Integer> originalMessages = (ArrayList) param.args[1];
            this.redb.batchPutDeletedMessages(did, originalMessages);
            MessageUtils.forceUpdateViews(did, originalMessages);
            param.setResult((Object) null);
        }
    }
}
