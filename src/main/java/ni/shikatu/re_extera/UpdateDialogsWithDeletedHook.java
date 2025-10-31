package ni.shikatu.re_extera;

import java.util.ArrayList;
import de.robv.android.xposed.XC_MethodHook;

public class UpdateDialogsWithDeletedHook extends XC_MethodHook {

	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getSaveDeletedMessages()) return;

		long uid = (long) param.args[0];
		long channelId = (long) param.args[1];
		@SuppressWarnings("unchecked")
		ArrayList<Integer> ids = (ArrayList<Integer>) param.args[2];
		if (ids == null || ids.isEmpty()) return;
		long did = channelId != 0 ? -channelId : uid;
		DbDeletedStore.get().batchPut(did, ids);
		for (int mid : ids) {
			Main.cachedDeleted.add(did + "_" + mid);
		}
		param.setResult(null);
	}
}
