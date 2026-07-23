package ni.shikatu.re_extera.hooks;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.collection.LongSparseArray;
import com.exteragram.messenger.drawer.DrawerMenuView;
import com.exteragram.messenger.plugins.Plugin;
import com.exteragram.messenger.plugins.PythonPluginsEngine;
import com.exteragram.messenger.preferences.appearance.AppNavigationPreferencesActivity;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
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
import ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook;
import ni.shikatu.re_extera.hooks.dialogsactivity.GetDialogsArray;
import ni.shikatu.re_extera.hooks.flagsecure.FlagSecureReasonAttach;
import ni.shikatu.re_extera.hooks.flagsecure.WindowManagerImpl;
import ni.shikatu.re_extera.hooks.flagsecure.WindowSetFlags;
import ni.shikatu.re_extera.hooks.messageobject.CanDeleteMessage;
import ni.shikatu.re_extera.hooks.messageobject.CanForwardMessage;
import ni.shikatu.re_extera.hooks.messagescontroller.CheckDeletingTask;
import ni.shikatu.re_extera.hooks.messagescontroller.DeleteMessages;
import ni.shikatu.re_extera.hooks.messagescontroller.FilterShadowbannedDialogs;
import ni.shikatu.re_extera.hooks.messagescontroller.IsChatNoForwards;
import ni.shikatu.re_extera.hooks.messagescontroller.IsUserNoForwards;
import ni.shikatu.re_extera.hooks.messagescontroller.ProcessLoadedDialogs;
import ni.shikatu.re_extera.hooks.messagescontroller.ProcessUpdates;
import ni.shikatu.re_extera.hooks.messagescontroller.SortDialogsHook;
import ni.shikatu.re_extera.hooks.messagesstorage.MarkMessagesAsDeletedInternal;
import ni.shikatu.re_extera.hooks.messagesstorage.UpdateDialogsWithDeletedMessages;
import ni.shikatu.re_extera.hooks.navigation.AppNavigationGhostEditorHook;
import ni.shikatu.re_extera.hooks.navigation.DrawerMenuGhostHook;
import ni.shikatu.re_extera.hooks.notificationmanager.FilterShadowbannedNotifications;
import ni.shikatu.re_extera.hooks.notificationmanager.RemoveDeletedMessagesFromNotification;
import ni.shikatu.re_extera.hooks.pluginsengine.OpenSettingsHook;
import ni.shikatu.re_extera.hooks.profileactivity.ProfileMenuShadowban;
import ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData;
import ni.shikatu.re_extera.hooks.sendmessageshelper.SendMessage;
import ni.shikatu.re_extera.hooks.sendmessageshelper.SendMessageForwardHook;
import ni.shikatu.re_extera.hooks.userconfig.isPremium;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.GhostMenuHelper;
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
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.SecretVoicePlayer;

public final class HookInit {
    private final ArrayList<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    public XC_MethodHook.Unhook sendRequestHook;

    /* JADX INFO: Access modifiers changed from: private */
    @FunctionalInterface
    interface HookRegistrar {
        XC_MethodHook.Unhook register() throws Throwable;
    }

    public void init() {
        try {
            startIntercepting();
        } catch (Exception e) {
            Main.log("Fail on startIntercepting: %s", e.getMessage());
        }
    }

    private void addHook(XC_MethodHook.Unhook hook) {
        if (hook != null) {
            this.hooks.add(hook);
        }
    }

    private void tryAddHook(String name, HookRegistrar registrar) {
        try {
            addHook(registrar.register());
        } catch (Throwable e) {
            Main.log("Failed to hook %s: %s", name, e.getMessage());
        }
    }

    private void tryHook(String name, final Class<?> clazz, final String methodName, final XC_MethodHook hook, final Class<?>... parameterTypes) {
        tryAddHook(name, new HookRegistrar() { 
            @Override // ni.shikatu.re_extera.hooks.HookInit.HookRegistrar
            public final XC_MethodHook.Unhook register() {
                try {
                    return XposedBridge.hookMethod(clazz.getDeclaredMethod(methodName, parameterTypes), hook);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void startSendRequestHook() {
        try {
            this.sendRequestHook = XposedBridge.hookMethod(ConnectionsManager.class.getDeclaredMethod("sendRequestInternal", TLObject.class, RequestDelegate.class, RequestDelegateTimestamp.class, QuickAckDelegate.class, WriteToSocketDelegate.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE), new SendRequest());
        } catch (Throwable e) {
            Main.log("Failed to hook ConnectionsManager.sendRequestInternal: %s", e.getMessage());
        }
    }

    public void startIntercepting() {
        startSendRequestHook();
        tryHook("MessagesController.processUpdates", MessagesController.class, "processUpdates", new ProcessUpdates(), TLRPC.Updates.class, Boolean.TYPE);
        tryHook("MessagesController.isChatNoForwards(Chat)", MessagesController.class, "isChatNoForwards", new IsChatNoForwards(), TLRPC.Chat.class);
        tryHook("MessagesController.isChatNoForwards(long)", MessagesController.class, "isChatNoForwards", new IsChatNoForwards(), Long.TYPE);
        tryHook("MessagesController.isUserNoForwards", MessagesController.class, "isUserNoForwards", new IsUserNoForwards(), TLRPC.UserFull.class);
        tryHook("MessagesController.checkDeletingTask", MessagesController.class, "checkDeletingTask", new CheckDeletingTask(), Boolean.TYPE);
        tryHook("MessagesController.deleteMessages", MessagesController.class, "deleteMessages", new DeleteMessages(), ArrayList.class, ArrayList.class, TLRPC.EncryptedChat.class, Long.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE);
        tryHook("MessagesController.getDialogs", MessagesController.class, "getDialogs", new FilterShadowbannedDialogs(), Integer.TYPE);
        tryHook("MessagesController.sortDialogs", MessagesController.class, "sortDialogs", new SortDialogsHook(), LongSparseArray.class);
        tryHook("MessagesController.processLoadedDialogs", MessagesController.class, "processLoadedDialogs", new ProcessLoadedDialogs(), TLRPC.messages_Dialogs.class, ArrayList.class, ArrayList.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
        tryHook("SecretVoicePlayer.dismiss", SecretVoicePlayer.class, "dismiss", new SecretVoicePlayerDismiss(), new Class[0]);
        tryHook("MessagesStorage.markMessagesAsDeletedInternal", MessagesStorage.class, "markMessagesAsDeletedInternal", new MarkMessagesAsDeletedInternal(), Long.TYPE, ArrayList.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE);
        tryHook("MessagesStorage.updateDialogsWithDeletedMessages", MessagesStorage.class, "updateDialogsWithDeletedMessages", new UpdateDialogsWithDeletedMessages(), Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class, Boolean.TYPE);
        tryHook("MessagesStorage.updateDialogsWithDeletedMessagesInternal", MessagesStorage.class, "updateDialogsWithDeletedMessagesInternal", new UpdateDialogsWithDeletedMessages(), Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class);
        tryHook("ChatMessageCell.didPressButton", ChatMessageCell.class, "didPressButton", new DidPressButton(), Boolean.TYPE, Boolean.TYPE);
        tryHook("ChatMessageCell.measureTime", ChatMessageCell.class, "measureTime", new MeasureTime(), MessageObject.class);
        if (anyAccountIsPremium()) {
            Settings.setLocalPremium(false);
        }
        tryHook("UserConfig.isPremium", UserConfig.class, "isPremium", new isPremium(), new Class[0]);
        try {
            Class<?> clazz = Class.forName("android.view.WindowManagerImpl");
            tryHook("WindowManagerImpl.addView", clazz, "addView", new WindowManagerImpl(), View.class, ViewGroup.LayoutParams.class);
        } catch (ClassNotFoundException e) {
            Main.log("WindowManagerImpl not found: %s", e.getMessage());
        }
        tryHook("Window.setFlags", Window.class, "setFlags", new WindowSetFlags(), Integer.TYPE, Integer.TYPE);
        tryHook("FlagSecureReason.attach", FlagSecureReason.class, "attach", new FlagSecureReasonAttach(), new Class[0]);
        tryHook("SendMessagesHelper.sendMessage(params)", SendMessagesHelper.class, "sendMessage", new SendMessage(), SendMessagesHelper.SendMessageParams.class);
        tryHook("SendMessagesHelper.sendMessage(forwards)", SendMessagesHelper.class, "sendMessage", new SendMessageForwardHook(), ArrayList.class, Long.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Integer.TYPE, Integer.TYPE, MessageObject.class, Integer.TYPE, Long.TYPE, Long.TYPE, MessageSuggestionParams.class);
        tryHook("NotificationsController.removeDeletedMessagesFromNotifications", NotificationsController.class, "removeDeletedMessagesFromNotifications", new RemoveDeletedMessagesFromNotification(), LongSparseArray.class, Boolean.TYPE);
        tryHook("NotificationsController.processNewMessages", NotificationsController.class, "processNewMessages", new FilterShadowbannedNotifications(), ArrayList.class, Boolean.TYPE, Boolean.TYPE, CountDownLatch.class);
        tryHook("MessageObject.canDeleteMessage", MessageObject.class, "canDeleteMessage", new CanDeleteMessage(), Boolean.TYPE, TLRPC.Chat.class);
        tryHook("MessageObject.canForwardMessage", MessageObject.class, "canForwardMessage", new CanForwardMessage(), new Class[0]);
        tryHook("ChatActivity.fillMessageMenu", ChatActivity.class, "fillMessageMenu", new FillMessageMenu(), MessageObject.class, ArrayList.class, ArrayList.class, ArrayList.class);
        tryHook("ChatActivity.processSelectedOption", ChatActivity.class, "processSelectedOption", new ProcessSelectedOption(), Integer.TYPE);
        tryHook("LocaleController.formatUserStatus", LocaleController.class, "formatUserStatus", new ni.shikatu.re_extera.hooks.localecontroller.FormatUserStatus(), Integer.TYPE, TLRPC.User.class, boolean[].class, boolean[].class, boolean[].class);
        tryHook("ChatActivity.createView", ChatActivity.class, "createView", new FragmentCreate(), Context.class);
        tryHook("ChatActivity.hasSelectedNoforwardsMessage", ChatActivity.class, "hasSelectedNoforwardsMessage", new HasSelectedNoForwardsMessage(), new Class[0]);
        tryHook("ChatActivity.sendSecretMediaDelete", ChatActivity.class, "sendSecretMediaDelete", new SendSecretMediaDelete(), MessageObject.class);
        tryHook("ChatActivity.sendSecretMessageRead", ChatActivity.class, "sendSecretMessageRead", new SendSecretMessageRead(), MessageObject.class, Boolean.TYPE);
        tryHook("ChatActivity.processDeletedMessages", ChatActivity.class, "processDeletedMessages", new ProcessDeletedMessages(), ArrayList.class, Long.TYPE, Boolean.TYPE, Boolean.TYPE);
        tryHook("ChatActivity.processNewMessages", ChatActivity.class, "processNewMessages", new ProcessNewMessages(), ArrayList.class, Boolean.TYPE);
        tryHook("ChatActivity.didReceivedNotification", ChatActivity.class, "didReceivedNotification", new NotificationCenterDidLoad(), Integer.TYPE, Integer.TYPE, Object[].class);
        tryHook("DialogCell.update", DialogCell.class, "update", new FilterDialogCellPreview(), Integer.TYPE, Boolean.TYPE);
        tryHook("DialogsActivity.getDialogsArray", DialogsActivity.class, "getDialogsArray", new GetDialogsArray(), Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE);
        tryHook("DialogsActivity.addMainMenuConfiguredItems", DialogsActivity.class, "addMainMenuConfiguredItems", new DialogsActivityHook(DialogsActivityHook.Mode.ADD_ITEMS), ItemOptions.class);
        tryHook("DialogsActivity.addMainMenuConfiguredItem", DialogsActivity.class, "addMainMenuConfiguredItem", new DialogsActivityHook(DialogsActivityHook.Mode.ADD_ITEM), ItemOptions.class, Integer.TYPE);
        tryHook("ProfileActivity.updateProfileData", ProfileActivity.class, "updateProfileData", new UpdateProfileData(), Boolean.TYPE);
        tryHook("ProfileActivity.createActionBarMenu", ProfileActivity.class, "createActionBarMenu", new ProfileMenuShadowban(), Boolean.TYPE);
        tryHook("PythonPluginsEngine.openPluginSettings", PythonPluginsEngine.class, "openPluginSettings", new OpenSettingsHook(), Plugin.class, BaseFragment.class);
        GhostMenuHelper.registerPluginMenuItem();
        tryHook("DrawerMenuView.rebuildMenu", DrawerMenuView.class, "rebuildMenu", new DrawerMenuGhostHook(), Integer.TYPE, BaseFragment.class);
        tryHook("AppNavigationPreferencesActivity.initItemDetails", AppNavigationPreferencesActivity.class, "initItemDetails", new AppNavigationGhostEditorHook(AppNavigationGhostEditorHook.Mode.INIT_ITEM_DETAILS), new Class[0]);
        tryHook("AppNavigationPreferencesActivity.addMenuSection", AppNavigationPreferencesActivity.class, "addMenuSection", new AppNavigationGhostEditorHook(AppNavigationGhostEditorHook.Mode.ADD_MENU_SECTION), ArrayList.class, UniversalAdapter.class, String.class, ArrayList.class, Boolean.TYPE);
        tryHook("AppNavigationPreferencesActivity.fillItems", AppNavigationPreferencesActivity.class, "fillItems", new AppNavigationGhostEditorHook(AppNavigationGhostEditorHook.Mode.FILL_ITEMS), ArrayList.class, UniversalAdapter.class);
        tryHook("AppNavigationPreferencesActivity.onClick", AppNavigationPreferencesActivity.class, "onClick", new AppNavigationGhostEditorHook(AppNavigationGhostEditorHook.Mode.ON_CLICK), UItem.class, View.class, Integer.TYPE, Float.TYPE, Float.TYPE);
        tryHook("AppNavigationPreferencesActivity.updateConfigFromReorder", AppNavigationPreferencesActivity.class, "updateConfigFromReorder", new AppNavigationGhostEditorHook(AppNavigationGhostEditorHook.Mode.UPDATE_REORDER), Integer.TYPE, ArrayList.class);
        tryHook("AppNavigationPreferencesActivity.resetToDefault", AppNavigationPreferencesActivity.class, "resetToDefault", new AppNavigationGhostEditorHook(AppNavigationGhostEditorHook.Mode.RESET_TO_DEFAULT), new Class[0]);
    }

    private static boolean anyAccountIsPremium() {
        TLRPC.User user;
        for (int i = 0; i < 16; i++) {
            UserConfig cfg = UserConfig.getInstance(i);
            if (cfg != null && cfg.isClientActivated() && (user = cfg.getCurrentUser()) != null && user.premium) {
                return true;
            }
        }
        return false;
    }

    public void onUnload() {
        if (this.sendRequestHook != null) {
            this.sendRequestHook.unhook();
            this.sendRequestHook = null;
        }
        for (XC_MethodHook.Unhook hook : this.hooks) {
            hook.unhook();
        }
        this.hooks.clear();
    }
}
