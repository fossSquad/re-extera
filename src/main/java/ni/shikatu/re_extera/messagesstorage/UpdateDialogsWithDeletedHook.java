package ni.shikatu.re_extera.messagesstorage;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.Iterator;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.Settings;

public class UpdateDialogsWithDeletedHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveDeletedMessages()) {
            long uid = ((Long) param.args[0]).longValue();
            long channelId = ((Long) param.args[1]).longValue();
            ArrayList<Integer> ids = (ArrayList) param.args[2];
            if (ids == null || ids.isEmpty()) {
                return;
            }
            long did = channelId != 0 ? -channelId : uid;
            DbDeletedStore.get().batchPut(did, ids);
            Iterator<Integer> it = ids.iterator();
            while (it.hasNext()) {
                int mid = it.next().intValue();
                Main.cachedDeleted.add(did + "_" + mid);
            }
            param.setResult((Object) null);
        }
    }
}
