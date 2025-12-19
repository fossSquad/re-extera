package ni.shikatu.re_extera.hooks.connectionsmanager;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class SendRequest extends XC_MethodHook {
    private static int current_reading_status;
    private static int current_typing_status;
    private static final TL_account.updateStatus offlineStatus = new TL_account.updateStatus();

    public SendRequest() {
        offlineStatus.offline = true;
    }

    public static void notifyDialogIdChanged(long dialogId) {
        current_reading_status = ReExteraDb.get().getDialogReading(dialogId);
        current_typing_status = ReExteraDb.get().getDialogTyping(dialogId);
    }

    public static void autoCheckDialogId() {
        ChatActivity lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment instanceof ChatActivity) {
            ChatActivity chatActivity = lastFragment;
            notifyDialogIdChanged(chatActivity.getDialogId());
        }
    }

    public static int getCurrentReadingStatus() {
        return current_reading_status;
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        TL_account.updateStatus updatestatus = (TLObject) param.args[0];
        if ((updatestatus instanceof TL_account.updateStatus) && Settings.getHideOnline()) {
            updatestatus.offline = true;
            return;
        }
        if (Defaults.readingRequests.contains(updatestatus.getClass())) {
            switch (current_reading_status) {
                case Defaults.NEVER /* -1 */:
                    Main.log("Selected NEVER, reading", new Object[0]);
                    param.setResult((Object) null);
                    break;
                case Defaults.GLOBAL_VALUE /* 0 */:
                default:
                    if (Settings.getHideReading()) {
                        Main.log("Selected DEFAULT, not reading", new Object[0]);
                        param.setResult((Object) null);
                    } else {
                        Main.log("Selected DEFAULT, not reading", new Object[0]);
                    }
                    break;
                case Defaults.ALWAYS /* 1 */:
                    Main.log("Selected ALWAYS, reading", new Object[0]);
                    break;
            }
        }
        if (Defaults.typingRequests.contains(updatestatus.getClass())) {
            switch (current_typing_status) {
                case Defaults.NEVER /* -1 */:
                    Main.log("Selected NEVER, not typing", new Object[0]);
                    param.setResult((Object) null);
                    break;
                case Defaults.GLOBAL_VALUE /* 0 */:
                default:
                    if (Settings.getHideTyping()) {
                        Main.log("Selected DEFAULT, not typing", new Object[0]);
                        param.setResult((Object) null);
                    } else {
                        Main.log("Selected DEFAULT, typing", new Object[0]);
                    }
                    break;
                case Defaults.ALWAYS /* 1 */:
                    Main.log("Selected ALWAYS, typing", new Object[0]);
                    break;
            }
        }
        if (Defaults.storiesRequests.contains(updatestatus.getClass()) && Settings.getNoReadStories()) {
            param.setResult((Object) null);
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Defaults.onlineRequests.contains(param.args[0].getClass()) && Settings.getImmediateOffline()) {
            ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(offlineStatus, new RequestDelegate() { // from class: ni.shikatu.re_extera.hooks.connectionsmanager.SendRequest$$ExternalSyntheticLambda0
                public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                    SendRequest.lambda$afterHookedMethod$0(tLObject, tL_error);
                }
            });
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(TLObject __, TLRPC.TL_error ___) {
    }
}
