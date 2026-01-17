package ni.shikatu.re_extera.hooks;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.collection.LongSparseArray;
import com.exteragram.messenger.plugins.Plugin;
import com.exteragram.messenger.plugins.PythonPluginsEngine;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.util.ArrayList;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.hooks.chatactivity.HasSelectedNoForwardsMessage;
import ni.shikatu.re_extera.hooks.chatactivity.NotificationCenterDidLoad;
import ni.shikatu.re_extera.hooks.chatactivity.ProcessDeletedMessages;
import ni.shikatu.re_extera.hooks.chatactivity.ProcessNewMessages;
import ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate;
import ni.shikatu.re_extera.hooks.chatactivity.menuhook.FillMessageMenu;
import ni.shikatu.re_extera.hooks.chatactivity.menuhook.ProcessSelectedOption;
import ni.shikatu.re_extera.hooks.chatactivity.secretmedia.SendSecretMediaDelete;
import ni.shikatu.re_extera.hooks.chatactivity.secretmedia.SendSecretMessageRead;
import ni.shikatu.re_extera.hooks.chatmessagecell.DidPressButton;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.hooks.chatmessagecell.SecretVoicePlayerDismiss;
import ni.shikatu.re_extera.hooks.connectionsmanager.SendRequest;
import ni.shikatu.re_extera.hooks.dialogcell.FilterDialogCellPreview;
import ni.shikatu.re_extera.hooks.dialogsactivity.GetDialogsArray;
import ni.shikatu.re_extera.hooks.drawerlayout.DrawerAdapterReset;
import ni.shikatu.re_extera.hooks.flagsecure.FlagSecureReasonAttach;
import ni.shikatu.re_extera.hooks.flagsecure.WindowManagerImpl;
import ni.shikatu.re_extera.hooks.flagsecure.WindowSetFlags;
import ni.shikatu.re_extera.hooks.messageobject.CanDeleteMessage;
import ni.shikatu.re_extera.hooks.messageobject.CanForwardMessage;
import ni.shikatu.re_extera.hooks.messagescontroller.CheckDeletingTask;
import ni.shikatu.re_extera.hooks.messagescontroller.DeleteMessages;
import ni.shikatu.re_extera.hooks.messagescontroller.FilterShadowbannedDialogs;
import ni.shikatu.re_extera.hooks.messagescontroller.IsChatNoForwards;
import ni.shikatu.re_extera.hooks.messagescontroller.ProcessLoadedDialogs;
import ni.shikatu.re_extera.hooks.messagescontroller.ProcessUpdates;
import ni.shikatu.re_extera.hooks.messagescontroller.SortDialogsHook;
import ni.shikatu.re_extera.hooks.messagesstorage.MarkMessagesAsDeletedInternal;
import ni.shikatu.re_extera.hooks.messagesstorage.UpdateDialogsWithDeletedMessages;
import ni.shikatu.re_extera.hooks.notificationmanager.RemoveDeletedMessagesFromNotification;
import ni.shikatu.re_extera.hooks.pluginsengine.OpenSettingsHook;
import ni.shikatu.re_extera.hooks.profileactivity.ProfileMenuShadowban;
import ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData;
import ni.shikatu.re_extera.hooks.sendmessageshelper.SendMessage;
import ni.shikatu.re_extera.hooks.sendmessageshelper.SendMessageForwardHook;
import ni.shikatu.re_extera.hooks.userconfig.isPremium;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.QuickAckDelegate;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.RequestDelegateTimestamp;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.WriteToSocketDelegate;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.DrawerLayoutAdapter;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.SecretVoicePlayer;

public class HookInit {
    private ArrayList<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    public XC_MethodHook.Unhook sendRequestHook;

    public void init() {
        try {
            startIntercepting();
        } catch (Exception e) {
            Main.log("Fail on startIntercepting, no such method: %s", e.getMessage());
        }
    }

    public void addHook(XC_MethodHook.Unhook hook) {
        this.hooks.add(hook);
    }

    public void startSendRequestHook() {
        try {
            this.sendRequestHook = XposedBridge.hookMethod(ConnectionsManager.class.getDeclaredMethod("sendRequestInternal", TLObject.class, RequestDelegate.class, RequestDelegateTimestamp.class, QuickAckDelegate.class, WriteToSocketDelegate.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE), new SendRequest());
        } catch (Exception e) {
            ReflectionUtils.hookError();
        }
    }

    public void startIntercepting() {
        try {
            startSendRequestHook();
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("processUpdates", TLRPC.Updates.class, Boolean.TYPE), new ProcessUpdates()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("isChatNoForwards", TLRPC.Chat.class), new IsChatNoForwards()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("isChatNoForwards", Long.TYPE), new IsChatNoForwards()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("isChatNoForwards", TLRPC.Chat.class), new IsChatNoForwards()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("checkDeletingTask", Boolean.TYPE), new CheckDeletingTask()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("deleteMessages", ArrayList.class, ArrayList.class, TLRPC.EncryptedChat.class, Long.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE), new DeleteMessages()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("getDialogs", Integer.TYPE), new FilterShadowbannedDialogs()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("sortDialogs", LongSparseArray.class), new SortDialogsHook()));
            addHook(XposedBridge.hookMethod(MessagesController.class.getDeclaredMethod("processLoadedDialogs", TLRPC.messages_Dialogs.class, ArrayList.class, ArrayList.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE), new ProcessLoadedDialogs()));
            addHook(XposedBridge.hookMethod(SecretVoicePlayer.class.getDeclaredMethod("dismiss", new Class[0]), new SecretVoicePlayerDismiss()));
            addHook(XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("markMessagesAsDeletedInternal", Long.TYPE, ArrayList.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE), new MarkMessagesAsDeletedInternal()));
            addHook(XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessages", Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class, Boolean.TYPE), new UpdateDialogsWithDeletedMessages()));
            addHook(XposedBridge.hookMethod(MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessagesInternal", Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class), new UpdateDialogsWithDeletedMessages()));
            addHook(XposedBridge.hookMethod(ChatMessageCell.class.getDeclaredMethod("didPressButton", Boolean.TYPE, Boolean.TYPE), new DidPressButton()));
            addHook(XposedBridge.hookMethod(ChatMessageCell.class.getDeclaredMethod("measureTime", MessageObject.class), new MeasureTime()));
            if (UserConfig.getInstance(UserConfig.selectedAccount).isPremium()) {
                Settings.setLocalPremium(false);
            }
            addHook(XposedBridge.hookMethod(UserConfig.class.getDeclaredMethod("isPremium", new Class[0]), new isPremium()));
            try {
                Class<?> clazz = Class.forName("android.view.WindowManagerImpl");
                addHook(XposedBridge.hookMethod(clazz.getDeclaredMethod("addView", View.class, ViewGroup.LayoutParams.class), new WindowManagerImpl()));
            } catch (ClassNotFoundException e) {
                Main.log("WindowManagerImpl not found: %s", e.getMessage());
            }
            addHook(XposedBridge.hookMethod(Window.class.getDeclaredMethod("setFlags", Integer.TYPE, Integer.TYPE), new WindowSetFlags()));
            addHook(XposedBridge.hookMethod(FlagSecureReason.class.getDeclaredMethod("attach", new Class[0]), new FlagSecureReasonAttach()));
            addHook(XposedBridge.hookMethod(SendMessagesHelper.class.getDeclaredMethod("sendMessage", SendMessagesHelper.SendMessageParams.class), new SendMessage()));
            addHook(XposedBridge.hookMethod(SendMessagesHelper.class.getDeclaredMethod("sendMessage", ArrayList.class, Long.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Integer.TYPE, Integer.TYPE, MessageObject.class, Integer.TYPE, Long.TYPE, Long.TYPE, MessageSuggestionParams.class), new SendMessageForwardHook()));
            addHook(XposedBridge.hookMethod(NotificationsController.class.getDeclaredMethod("removeDeletedMessagesFromNotifications", LongSparseArray.class, Boolean.TYPE), new RemoveDeletedMessagesFromNotification()));
            addHook(XposedBridge.hookMethod(MessageObject.class.getDeclaredMethod("canDeleteMessage", Boolean.TYPE, TLRPC.Chat.class), new CanDeleteMessage()));
            addHook(XposedBridge.hookMethod(MessageObject.class.getDeclaredMethod("canForwardMessage", new Class[0]), new CanForwardMessage()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("fillMessageMenu", MessageObject.class, ArrayList.class, ArrayList.class, ArrayList.class), new FillMessageMenu()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("processSelectedOption", Integer.TYPE), new ProcessSelectedOption()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("createView", Context.class), new FragmentCreate()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("hasSelectedNoforwardsMessage", new Class[0]), new HasSelectedNoForwardsMessage()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("sendSecretMediaDelete", MessageObject.class), new SendSecretMediaDelete()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("sendSecretMessageRead", MessageObject.class, Boolean.TYPE), new SendSecretMessageRead()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("processDeletedMessages", ArrayList.class, Long.TYPE, Boolean.TYPE, Boolean.TYPE), new ProcessDeletedMessages()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("processNewMessages", ArrayList.class, Boolean.TYPE), new ProcessNewMessages()));
            addHook(XposedBridge.hookMethod(ChatActivity.class.getDeclaredMethod("didReceivedNotification", Integer.TYPE, Integer.TYPE, Object[].class), new NotificationCenterDidLoad()));
            addHook(XposedBridge.hookMethod(DialogCell.class.getDeclaredMethod("update", Integer.TYPE, Boolean.TYPE), new FilterDialogCellPreview()));
            addHook(XposedBridge.hookMethod(DialogsActivity.class.getDeclaredMethod("getDialogsArray", Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE), new GetDialogsArray()));
            addHook(XposedBridge.hookMethod(DrawerLayoutAdapter.class.getDeclaredMethod("resetItems", new Class[0]), new DrawerAdapterReset()));
            addHook(XposedBridge.hookMethod(ProfileActivity.class.getDeclaredMethod("updateProfileData", Boolean.TYPE), new UpdateProfileData()));
            addHook(XposedBridge.hookMethod(ProfileActivity.class.getDeclaredMethod("createActionBarMenu", Boolean.TYPE), new ProfileMenuShadowban()));
            addHook(XposedBridge.hookMethod(PythonPluginsEngine.class.getDeclaredMethod("openPluginSettings", Plugin.class, BaseFragment.class), new OpenSettingsHook()));
        } catch (Exception e2) {
            ReflectionUtils.hookError();
        }
    }

    public void onUnload() {
        this.sendRequestHook.unhook();
        for (XC_MethodHook.Unhook hook : this.hooks) {
            hook.unhook();
        }
    }
}
