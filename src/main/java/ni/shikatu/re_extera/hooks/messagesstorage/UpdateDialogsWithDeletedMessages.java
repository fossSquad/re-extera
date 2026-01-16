package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;

public class UpdateDialogsWithDeletedMessages extends XC_MethodHook {
    private ReExteraDb redb = ReExteraDb.get();

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveDeletedMessages()) {
            long uid = ((Long) param.args[0]).longValue();
            long channelId = ((Long) param.args[1]).longValue();
            long did = channelId != 0 ? channelId : uid;
            ArrayList<Integer> ids = (ArrayList) param.args[2];
            this.redb.lambda$batchPutDeletedMessagesAsync$1(did, ids);
            MessageUtils.forceUpdateViews(did, ids);
        }
    }
}
