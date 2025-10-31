package ni.shikatu.re_extera;

import androidx.collection.LongSparseArray;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;

public class PutMessagesInternalHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		Global.log("Hook");
		ArrayList<TLRPC.Message> msgs = (ArrayList<TLRPC.Message>) param.args[0];
		MessagesStorage s = (MessagesStorage) param.thisObject;
		long now = System.currentTimeMillis();

		for(TLRPC.Message msg : msgs){
			int mid = msg.id;
			long did = resolveDidFromMessage(msg);

			SQLiteCursor cursor = null;
			try {
				cursor = s.getDatabase().queryFinalized(
						"SELECT data FROM messages_v2 WHERE uid = " + did + " AND mid = " + mid + " LIMIT 1"
				);

				if (cursor.next()) {
					Global.log(String.format("Edit detected: mid=%d did=%d edit_date=%d", mid, did, msg.edit_date));

					NativeByteBuffer data = cursor.byteBufferValue(0);
					if (data != null) {
						TLRPC.Message msgOld = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);

						data.reuse();

						Global.log("Got msg old");

						MessageObject mo = new MessageObject(UserConfig.selectedAccount, msgOld, false, false);
						Global.log(String.format("Old message: %s", mo));

						String oldText = safeMsgText(mo);
						if (oldText != null && !DbDeletedStore.get().hasEdits(did, mid)) {
							try {
								DbDeletedStore.get().saveOriginalIfAbsent(did, mid, oldText, now);
							} catch (Throwable ignored) {}
						}

						String newText = extractText(msg);
						try {
							DbDeletedStore.get().appendEdit(did, mid, newText, now);
							Global.log(String.format("Saved edit: old='%s' new='%s'", oldText, newText));
						} catch (Throwable ignored) {}
					}
				} else if (!cursor.next()) {
					Global.log(String.format("New message: mid=%d did=%d", mid, did));
				}
			} catch (Exception e) {
				Global.log("Error: " + e.getMessage());
				e.printStackTrace();
			} finally {
				if (cursor != null) {
					cursor.dispose();
				}
			}
		}
	}

	static String safeMsgText(MessageObject mo) {
		try {
			CharSequence cs = mo.messageText;
			return cs != null ? cs.toString() : "";
		} catch (Throwable t) {
			return "";
		}
	}

	static String extractText(TLRPC.Message m) {
		try {
			if (m.message != null) return m.message;
		} catch (Throwable ignored) {}
		return "";
	}

	static long resolveDidFromMessage(TLRPC.Message m) {
		if (m == null) return 0L;
		long userOrChat = 0L;
		if (m.peer_id != null) {
			if (m.peer_id.user_id != 0) userOrChat = m.peer_id.user_id;
			else if (m.peer_id.chat_id != 0) userOrChat = -m.peer_id.chat_id;
			else if (m.peer_id.channel_id != 0) userOrChat = -m.peer_id.channel_id;
		}
		return userOrChat;
	}
}


