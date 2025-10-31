package ni.shikatu.re_extera;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import de.robv.android.xposed.XC_MethodHook;

public class SendMessageHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

		Global.log("Intercepting send message");
		SendMessagesHelper.SendMessageParams params = (SendMessagesHelper.SendMessageParams) param.args[0];
		MessageObject r = params.replyToMsg;
		long did = r.getDialogId();

		int mid = r.getId();
		boolean isDeleted = Main.cachedDeleted.contains(did + "_" + mid) || DbDeletedStore.get().exists(did, mid);

		if(isDeleted){
			TLRPC.User user = MessagesController.getInstance(UserConfig.selectedAccount).getUser(params.replyToMsg.getSenderId());
			String senderName = user.first_name;
			String newText = String.format("%s\n%s", senderName, params.replyToMsg.messageText);
			TLRPC.TL_messageEntityBlockquote quote = new TLRPC.TL_messageEntityBlockquote();
			quote.offset = 0;
			quote.length = newText.length();
			params.entities.add(quote);

			TLRPC.TL_inputMessageEntityMentionName urlToUser = new TLRPC.TL_inputMessageEntityMentionName();
			urlToUser.offset = 0;
			urlToUser.length = senderName.length();
			TLRPC.TL_inputUser userment = new TLRPC.TL_inputUser();
			userment.user_id = r.getSenderId();
			urlToUser.user_id = userment;
			/*if(user.username != null && !user.username.isEmpty()){
				urlToUser.url = String.format("https://t.me/%s", user.username);
			}else{
				urlToUser.url = String.format("tg://user?id=%s", params.replyToMsg.getSenderId());
			}*/
			params.entities.add(urlToUser);
			params.message = newText + params.message;
			params.replyToMsg = null;
			param.args[0] = params;
		}
	}



}
