package ni.shikatu.re_extera;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import de.robv.android.xposed.XposedBridge;
import java.util.ArrayList;
import ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook;
import ni.shikatu.re_extera.chatactivity.HasSelectedNoForwardsHook;
import ni.shikatu.re_extera.chatactivity.ProcessDeletedMessages;
import ni.shikatu.re_extera.chatactivity.RemoveMessageObjectHook;
import ni.shikatu.re_extera.chatactivity.SendSecretMediaDeleteHook;
import ni.shikatu.re_extera.chatactivity.SendSecretMediaReadHook;
import ni.shikatu.re_extera.chatmessagecell.DidPressButton;
import ni.shikatu.re_extera.chatmessagecell.FillMessageMenuHook;
import ni.shikatu.re_extera.chatmessagecell.MeasureTimeHook;
import ni.shikatu.re_extera.chatmessagecell.ProcessSelectedOptionHook;
import ni.shikatu.re_extera.flagsecure.FlagSecureReasonHook;
import ni.shikatu.re_extera.flagsecure.WindowHook;
import ni.shikatu.re_extera.flagsecure.WindowManagerImplHook;
import ni.shikatu.re_extera.messageobject.CanDeleteMessageHook;
import ni.shikatu.re_extera.messageobject.MarkMessagesAsDeletedInternalHook;
import ni.shikatu.re_extera.messagescontroller.CanForwardMessageHook;
import ni.shikatu.re_extera.messagescontroller.CheckDeletingTaskHook;
import ni.shikatu.re_extera.messagescontroller.IsChatNoForwardsHook;
import ni.shikatu.re_extera.messagesstorage.UpdateDialogsWithDeletedHook;
import ni.shikatu.re_extera.senderHelper.SendMessageForwardHook;
import ni.shikatu.re_extera.senderHelper.SendMessageHook;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
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
import org.telegram.ui.LaunchActivity;

public class Main {
    public static final String VERSION = "1.0.10";
    public static Main instance = null;
    public static ArrayList<String> cachedDeleted = new ArrayList<>();

    public static boolean exists(long dialogId, int messageId) {
        return cachedDeleted.contains(dialogId + "_" + messageId);
    }

    private Main() {
    }

    public static synchronized Main getInstance() {
        if (instance == null) {
            instance = new Main();
        }
        return instance;
    }

    public void start() throws NoSuchMethodException, ClassNotFoundException {
        Context ctx = LaunchActivity.instance.getApplicationContext();
        DbDeletedStore.init(ctx);
        MeasureTimeHook.deletedIcon = ContextCompat.getDrawable(ctx, R.drawable.msg_delete_filled);
        Localization.updateStrings();
        setupHooks();
    }

    public void setupHooks() throws NoSuchMethodException, ClassNotFoundException {
        XposedBridge.hookMethod(ConnectionsManager.class.getDeclaredMethod("sendRequestInternal", TLObject.class, RequestDelegate.class, RequestDelegateTimestamp.class, QuickAckDelegate.class, WriteToSocketDelegate.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE), new InterceptOnlineHook());
        XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("processUpdateArray", ArrayList.class, ArrayList.class, ArrayList.class, Boolean.TYPE, Integer.TYPE), new ProcessUpdateArrayHook());
        XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("processUpdates", TLRPC.Updates.class, Boolean.TYPE), new ProcessUpdatesHook());
        XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("markMessagesAsDeletedInternal", Long.TYPE, ArrayList.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE), new MarkMessagesAsDeletedInternalHook());
        XposedBridge.hookMethod(ChatMessageCell.class.getDeclaredMethod("didPressButton", Boolean.TYPE, Boolean.TYPE), new DidPressButton());
        try {
            Class<?> clazz = Class.forName("android.view.WindowManagerImpl");
            XposedBridge.hookMethod(clazz.getDeclaredMethod("addView", View.class, ViewGroup.LayoutParams.class), new WindowManagerImplHook());
        } catch (Exception e) {
            Global.log("The FLAG_SECURE must not work on this device");
        }
        XposedBridge.hookMethod(MessageObject.class.getDeclaredMethod("canDeleteMessage", Boolean.TYPE, TLRPC.Chat.class), new CanDeleteMessageHook());
        XposedBridge.hookMethod(Window.class.getDeclaredMethod("setFlags", Integer.TYPE, Integer.TYPE), new WindowHook());
        XposedBridge.hookMethod(FlagSecureReason.class.getDeclaredMethod("attach", new Class[0]), new FlagSecureReasonHook());
        XposedBridge.hookMethod(SendMessagesHelper.class.getDeclaredMethod("sendMessage", SendMessagesHelper.SendMessageParams.class), new SendMessageHook());
        XposedBridge.hookMethod(SendMessagesHelper.class.getDeclaredMethod("sendMessage", ArrayList.class, Long.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Integer.TYPE, MessageObject.class, Integer.TYPE, Long.TYPE, Long.TYPE, MessageSuggestionParams.class), new SendMessageForwardHook());
        XposedBridge.hookMethod(ChatMessageCell.class.getDeclaredMethod("measureTime", MessageObject.class), new MeasureTimeHook());
        XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessages", Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class, Boolean.TYPE), new UpdateDialogsWithDeletedHook());
        XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessagesInternal", Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class), new UpdateDialogsWithDeletedHook());
        XposedBridge.hookMethod(NotificationsController.class.getDeclaredMethod("removeDeletedMessagesFromNotifications", LongSparseArray.class, Boolean.TYPE), new NotificationsRemoveDeletedHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("fillMessageMenu", MessageObject.class, ArrayList.class, ArrayList.class, ArrayList.class), new FillMessageMenuHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("processSelectedOption", Integer.TYPE), new ProcessSelectedOptionHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("onFragmentCreate", new Class[0]), new ChatActivityCreateHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("onResume", new Class[0]), new ChatActivityCreateHook());
        XposedBridge.hookMethod(MessageObject.class.getDeclaredMethod("canForwardMessage", new Class[0]), new CanForwardMessageHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("hasSelectedNoforwardsMessage", new Class[0]), new HasSelectedNoForwardsHook());
        XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("isChatNoForwards", TLRPC.Chat.class), new IsChatNoForwardsHook());
        XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("isChatNoForwards", Long.TYPE), new IsChatNoForwardsHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("sendSecretMediaDelete", MessageObject.class), new SendSecretMediaDeleteHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("sendSecretMessageRead", MessageObject.class, Boolean.TYPE), new SendSecretMediaReadHook());
        XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("checkDeletingTask", Boolean.TYPE), new CheckDeletingTaskHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("removeMessageObject", MessageObject.class), new RemoveMessageObjectHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("removeMessageWithThanos", MessageObject.class), new RemoveMessageObjectHook());
        XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("processDeletedMessages", ArrayList.class, Long.TYPE, Boolean.TYPE, Boolean.TYPE), new ProcessDeletedMessages());
    }

    public void showSettings() {
        Global.log("Opening settings");
        LaunchActivity.instance.presentFragment(new SettingsFragment());
    }
}
