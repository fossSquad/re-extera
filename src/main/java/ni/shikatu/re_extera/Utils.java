package ni.shikatu.re_extera;

import org.telegram.tgnet.TLRPC;

public class Utils {
	public static long getDialogIdFromMessage(TLRPC.Message msg) {
		if (msg.peer_id instanceof TLRPC.TL_peerUser) {
			return msg.peer_id.user_id;
		} else if (msg.peer_id instanceof TLRPC.TL_peerChat) {
			return -msg.peer_id.chat_id;
		} else if (msg.peer_id instanceof TLRPC.TL_peerChannel) {
			return -msg.peer_id.channel_id;
		}
		return 0;
	}
}
