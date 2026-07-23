package ni.shikatu.re_extera.hooks.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.Main;
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
            long did = channelId != 0 ? -channelId : uid;
            ArrayList<Integer> ids = (ArrayList) param.args[2];
            if (ids == null || ids.isEmpty()) {
                return;
            }
            // Defensive copy — caller may mutate the list after we return
            final ArrayList<Integer> savedIds = new ArrayList<>(ids);
            Main.log("UpdateDialogsWithDeletedMessages: intercepting %d ids for did=%d (args=%d)", savedIds.size(), did, param.args.length);
            this.redb.lambda$batchPutDeletedMessagesAsync$1(did, savedIds);
            MessageUtils.forceUpdateViews(currentAccount, did, savedIds);
            // Only cancel the *internal* 4-arg variant. The public 5-arg method
            // (updateDialogsWithDeletedMessages) also refreshes the dialog list
            // preview (last message, unread count) and must NOT be skipped —
            // otherwise the dialog list goes stale after a delete.
            // The 5th arg (index 4) is present only in the public method.
            boolean isInternalVariant = param.args.length == 4;
            if (isInternalVariant) {
                param.setResult((Object) null);
            }
        }
    }
}
