package ni.shikatu.re_extera;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;

public class ProcessUpdatesHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		MessagesController controller = (MessagesController) param.thisObject;
		TLRPC.Updates updates = (TLRPC.Updates) param.args[0];
		if(updates.update != null){
			parseUpdate(controller,updates.update);
		}
		for(TLRPC.Update update: updates.updates){
			if(update != null){
				parseUpdate(controller,update);
			}
		}

	}

	void parseUpdate(MessagesController controller, TLRPC.Update update){

		if(update instanceof TLRPC.TL_updateEditMessage){
			TLRPC.Message msg = ((TLRPC.TL_updateEditMessage) update).message;
			long did = getDialogIdFromMessage(msg);
			MessageObject oldObj = getMessage(controller, did, msg.id);

			if(oldObj != null){
				Global.log("Old object is not null");
				if(!DbDeletedStore.get().hasEdits(did, msg.id)){
					Global.log("message does not have edits");
					DbDeletedStore.get().saveOriginalIfAbsent(did, msg.id, oldObj.messageText.toString(), System.currentTimeMillis());
				}
				Global.log("saving");
				DbDeletedStore.get().appendEdit(did, msg.id, msg.message, System.currentTimeMillis());
			}
		}
		if(update instanceof TLRPC.TL_updateEditChannelMessage){
			TLRPC.Message msg = ((TLRPC.TL_updateEditChannelMessage) update).message;
			long did = getDialogIdFromMessage(msg);
			MessageObject oldObj = getMessage(controller, did, msg.id);
			if(oldObj != null){
				if(!DbDeletedStore.get().hasEdits(did, msg.id)){
					DbDeletedStore.get().saveOriginalIfAbsent(did, msg.id, oldObj.messageText.toString(), System.currentTimeMillis());
				}
				DbDeletedStore.get().appendEdit(did, msg.id, msg.message, System.currentTimeMillis());
			}
		}
	}
	MessageObject getMessage(MessagesController controller, long did, int mid){
		MessageObject obj = null;
		if(obj == null && did == 0){
			obj = controller.dialogMessagesByIds.get(mid);
		}
		if(obj == null){
			ArrayList<MessageObject> list = controller.dialogMessage.get(did);
			if(list != null && !list.isEmpty()){
				for(var obj1: list){
					if(obj1.getId() == mid){
						obj = obj1;
					}
				}
			}

		}
		if(obj == null){
			TLRPC.Message msg = MessagesStorage.getInstance(UserConfig.selectedAccount).getMessage(did, mid);
			if (msg != null) {
				obj = new MessageObject(UserConfig.selectedAccount, msg, false, false);
			}
		}
		if(obj == null){
			var lastFragment = LaunchActivity.getLastFragment();
			if (lastFragment instanceof ChatActivity ) {
				ChatActivity chatActivity = (ChatActivity) lastFragment;
				if (chatActivity.getDialogId() == did || did == 0 && chatActivity.getCurrentUser() != null) {
					for (var msg : chatActivity.messages) {
						if (msg != null && msg.getId() == mid) {
							obj = msg;
							break;
						}
					}
				}
			}
		}
		return obj;
	}


	private long getDialogIdFromMessage(TLRPC.Message msg) {
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
