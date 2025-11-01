package ni.shikatu.re_extera;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;

public class ProcessSelectedOptionHook extends XC_MethodHook {

	private static Field selectedObjectField = null;

	public static MessageObject selectedObject;

	static {
		try {
			selectedObjectField = ChatActivity.class.getDeclaredField("selectedObject");
			selectedObjectField.setAccessible(true);
		} catch (NoSuchFieldException ignored) {}
	}


	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		int option = (int) param.args[0];
		Global.log(String.format("Hooked ProcessSelectedOptionHook with option %s", option));
		ChatActivity thisObj = (ChatActivity) param.thisObject;
		MessageObject messageObject = (MessageObject) selectedObjectField.get(thisObj);
		if(option == 6363){
			Global.log("Hooked 6363 match");
			ArrayList<TLRPC.Message> x = DbDeletedStore.get().listEdits(messageObject.getDialogId(), messageObject.getId());
			Global.log(String.format("MSGObj has: %s, lines: %s",!x.isEmpty(), x.size()));
			Global.log(String.format("%s", x.toString()));
			if(messageObject != null){
				Global.log("messageObject is not null");
				MessageHistoryFragment historyFragment = MessageHistoryFragment.newInstance(messageObject.getDialogId(), messageObject.getId());
				thisObj.presentFragment(historyFragment);
			} else if(selectedObject != null) {
				Global.log("selectedObject is not null");
				MessageHistoryFragment historyFragment = MessageHistoryFragment.newInstance(selectedObject.getDialogId(), selectedObject.getId());
				thisObj.presentFragment(historyFragment);
			}
		}

	}
}
