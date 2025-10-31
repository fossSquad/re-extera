package ni.shikatu.re_extera;

import org.telegram.SQLite.SQLiteException;
import org.telegram.messenger.MessagesStorage;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;

public class MarkMessagesAsDeletedInternalHook extends XC_MethodHook {
	@SuppressWarnings("unchecked")
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getSaveDeletedMessages()) return;
		ArrayList<Integer> msgIds = (ArrayList<Integer>) param.args[1];
		if (msgIds == null || msgIds.isEmpty()) {
			return;
		}

		long did = (long) param.args[0];

		try {
			DbDeletedStore.get().batchPut(did, msgIds);
		} catch (Exception e) {
			Global.log("DbDeletedStore.batchPut failed: " + e.getMessage());
		}

		for (int mid : msgIds) {
			Main.cachedDeleted.add(did + "_" + mid);
		}

		param.setResult(null);
	}

	public static void markMessagesDeletedInternal(MessagesStorage storage, long did, ArrayList<Integer> ids)
			throws SQLiteException {
		if(!Settings.getSaveDeletedMessages()) return;

		if (ids == null || ids.isEmpty()) return;
		try {
			DbDeletedStore.get().batchPut(did, ids);
		} catch (Exception e) {
			Global.log("DbDeletedStore.batchPut (direct) failed: " + e.getMessage());
		}
		for (int mid : ids) {
			Main.cachedDeleted.add(did + "_" + mid);
		}
	}
}
