package ni.shikatu.re_extera;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;


import androidx.collection.LongSparseArray;

import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.QuickAckDelegate;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.RequestDelegateTimestamp;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.WriteToSocketDelegate;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;
import ni.shikatu.re_extera.flagsecure.FlagSecureReasonHook;
import ni.shikatu.re_extera.flagsecure.WindowHook;
import ni.shikatu.re_extera.flagsecure.WindowManagerImplHook;

public class Main {

	public static Main instance = null;

	public static ArrayList<String> cachedDeleted = new ArrayList<>();
	private Main() {}
	public static synchronized Main getInstance(){
		if(instance == null){
			instance = new Main();
		}
		return instance;
	}
	public void start() throws NoSuchMethodException, ClassNotFoundException {
		DbDeletedStore.init(LaunchActivity.instance.getApplicationContext());
		setupHooks();
		Localization.updateStrings();
		//MeasureTimeHook.notifyMarkChanged(Settings.getCustomPrefix());
	}

	public void setupHooks() throws NoSuchMethodException, ClassNotFoundException {
		XposedBridge.hookMethod(ConnectionsManager.class.getDeclaredMethod("sendRequestInternal",
				TLObject.class,
				RequestDelegate.class,
				RequestDelegateTimestamp.class,
				QuickAckDelegate.class,
				WriteToSocketDelegate.class,
				int.class,
				int.class,
				int.class,
				boolean.class,
				int.class
				), new InterceptOnlineHook());


		XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("processUpdateArray", ArrayList.class, ArrayList.class, ArrayList.class, boolean.class, int.class),
				new ProcessUpdateArrayHook());
		XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("processUpdates", TLRPC.Updates.class, boolean.class), new ProcessUpdatesHook());
		//XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("putMessagesInternal", ArrayList.class, boolean.class, boolean.class, int.class, boolean.class, int.class, long.class), new PutMessagesInternalHook());
		XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("markMessagesAsDeletedInternal", long.class, ArrayList.class, boolean.class, int.class, int.class),
				new MarkMessagesAsDeletedInternalHook()
				);
		try {
			Class<?> clazz = Class.forName("android.view.WindowManagerImpl");
			XposedBridge.hookMethod(clazz.getDeclaredMethod("addView", View.class, ViewGroup.LayoutParams.class), new WindowManagerImplHook());
		} catch (Exception e){
			Global.log("The FLAG_SECURE must not work on this device");
		}

		XposedBridge.hookMethod(Window.class.getDeclaredMethod("setFlags", int.class, int.class), new WindowHook());
		XposedBridge.hookMethod(FlagSecureReason.class.getDeclaredMethod("attach"), new FlagSecureReasonHook());
		XposedBridge.hookMethod(SendMessagesHelper.class.getDeclaredMethod("sendMessage", SendMessagesHelper.SendMessageParams.class), new SendMessageHook());
		XposedBridge.hookMethod(ChatMessageCell.class.getDeclaredMethod("measureTime", MessageObject.class), new MeasureTimeHook());
		XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessages", long.class, long.class, ArrayList.class, ArrayList.class, boolean.class), new UpdateDialogsWithDeletedHook());
		XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessagesInternal", long.class, long.class, ArrayList.class, ArrayList.class), new UpdateDialogsWithDeletedHook());
		XposedBridge.hookMethod(NotificationsController.class.getDeclaredMethod("removeDeletedMessagesFromNotifications", LongSparseArray.class, boolean.class), new NotificationsRemoveDeletedHook());

		XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("fillMessageMenu", MessageObject.class, ArrayList.class, ArrayList.class, ArrayList.class), new FillMessageMenuHook());
		XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("processSelectedOption", int.class), new ProcessSelectedOptionHook());

	}

	public void showSettings(){
		Global.log("Opening settings");
		LaunchActivity.instance.presentFragment(new SettingsFragment());
	}
}
