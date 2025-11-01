package ni.shikatu.re_extera;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

public class SendMessageHook extends XC_MethodHook {

	private XC_MethodHook.Unhook hooked = null;

	private static Method updateBottomOverlay;

	static {
		try {
			updateBottomOverlay = ChatActivity.class.getDeclaredMethod("updateBottomOverlay");
			updateBottomOverlay.setAccessible(true);
		} catch (NoSuchMethodException e) {}
	}

	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

		Global.log("Intercepting send message");
		SendMessagesHelper.SendMessageParams params = (SendMessagesHelper.SendMessageParams) param.args[0];
		MessageObject r = params.replyToMsg;
		if(r != null){
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
		Global.log(String.format("Disabled scheduled messages for now %s", params.scheduleDate));
		if(Settings.getUseSchedule() && params.scheduleDate == 0 && false){
			Global.log("Using schedule");
			hooked = XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("openScheduledMessages", int.class, boolean.class), new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					return null;
				}
			});
			((SendMessagesHelper.SendMessageParams) param.args[0]).scheduleDate = (int) getScheduleTime(params.photo, params.document);
			if(updateBottomOverlay != null){
				updateBottomOverlay.invoke(LaunchActivity.getLastFragment());
			}
		}

	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		if(hooked != null){
			hooked.unhook();
		}
	}

	public static double getScheduleTime(TLRPC.TL_photo photo, TLRPC.TL_document document){
		double time = ConnectionsManager.getInstance(UserConfig.selectedAccount).getCurrentTime() + 12f;
		if(document != null && document.access_hash != 0 && (MessageObject.isStickerDocument(document) || MessageObject.isAnimatedStickerDocument(document, true))){
			return (int) Math.ceil(time);
		}
		if(document != null && document.access_hash != 0 && MessageObject.isGifDocument(document)){
			return (int) Math.ceil(time);
		}
		int photoFileSize = 0;
		if(photo != null){
			TLRPC.PhotoSize mbsize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize());
			if(mbsize != null){
				photoFileSize = mbsize.size;
			}
		}
		long documentFileSize = 0;
		if(document != null){
			documentFileSize = document.size;
		}
		if(photoFileSize != 0){
			time += Double.max(6, Math.ceil(photoFileSize / 1024f / 1024f * 4.5f));
		}
		if(documentFileSize != 0){
			time += Double.max(6, Math.ceil(documentFileSize / 1024f / 1024f * 4.5f));
		}
		return Math.ceil(time);
	}
}
