package ni.shikatu.re_extera;

import static ni.shikatu.re_extera.PutMessagesInternalHook.extractText;
import static ni.shikatu.re_extera.PutMessagesInternalHook.resolveDidFromMessage;
import static ni.shikatu.re_extera.PutMessagesInternalHook.safeMsgText;

import android.util.SparseArray;
import androidx.collection.LongSparseArray;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.HashSet;

import de.robv.android.xposed.XC_MethodHook;

public class ProcessUpdateArrayHook extends XC_MethodHook {

	@Override
	@SuppressWarnings("unchecked")
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		ArrayList<TLRPC.Update> updates = (ArrayList<TLRPC.Update>) param.args[0];
		if (updates == null || updates.isEmpty()) return;

		MessagesController mc = (MessagesController) param.thisObject;
		SparseArray<MessageObject> byIds = mc.dialogMessagesByIds;
		LongSparseArray<ArrayList<MessageObject>> dialogMessage = mc.dialogMessage;

		ArrayList<TLRPC.Update> filtered = new ArrayList<>(updates.size());
		LongSparseArray<ArrayList<Integer>> channelDeleted = new LongSparseArray<>();

		long now = System.currentTimeMillis();

		for (TLRPC.Update u : updates) {
			if (u instanceof TLRPC.TL_updateDeleteMessages) {
				TLRPC.TL_updateDeleteMessages del = (TLRPC.TL_updateDeleteMessages) u;
				if (byIds != null && del.messages != null) {
					for (int id : del.messages) {
						MessageObject mo = byIds.get(id);
						if (mo != null) {
							long did = mo.getDialogId();
							Main.cachedDeleted.add(did + "_" + mo.getId());
							try { DbDeletedStore.get().put(did, mo.getId()); } catch (Throwable ignored) {}
						}
					}
				}
				continue;
			}

			if (u instanceof TLRPC.TL_updateDeleteChannelMessages) {
				TLRPC.TL_updateDeleteChannelMessages delc = (TLRPC.TL_updateDeleteChannelMessages) u;
				if (delc.messages == null || delc.messages.isEmpty()) continue;

				long did = -delc.channel_id;

				ArrayList<MessageObject> list = dialogMessage != null ? dialogMessage.get(did) : null;
				if (list != null && !list.isEmpty()) {
					HashSet<Integer> idsSet = new HashSet<>(delc.messages);
					for (MessageObject mo : list) {
						if (mo != null && idsSet.contains(mo.getId())) {
							Main.cachedDeleted.add(did + "_" + mo.getId());
						}
					}
				}

				ArrayList<Integer> acc = channelDeleted.get(did);
				if (acc == null) {
					acc = new ArrayList<>();
					channelDeleted.put(did, acc);
				}
				acc.addAll(delc.messages);
				continue;
			}
			filtered.add(u);
		}

		if (channelDeleted.size() > 0) {
			for (int i = 0; i < channelDeleted.size(); i++) {
				long did = channelDeleted.keyAt(i);
				ArrayList<Integer> ids = channelDeleted.valueAt(i);
				if (ids != null && !ids.isEmpty()) {
					try { DbDeletedStore.get().batchPut(did, ids); } catch (Throwable ignored) {}
				}
			}
		}

		((ArrayList<TLRPC.Update>) param.args[0]).clear();
		((ArrayList<TLRPC.Update>) param.args[0]).addAll(filtered);
	}

}
