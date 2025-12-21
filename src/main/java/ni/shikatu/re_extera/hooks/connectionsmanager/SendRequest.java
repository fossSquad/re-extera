package ni.shikatu.re_extera.hooks.connectionsmanager;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

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

    public static int getCurrentReadingStatus() {
        return current_reading_status;
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        TL_account.updateStatus updatestatus = (TLObject) param.args[0];
        if (Main.ignoredRequests.remove(updatestatus)) {
            return;
        }
        if ((updatestatus instanceof TL_account.updateStatus) && Settings.getHideOnlineWithGhost()) {
            updatestatus.offline = true;
        }
        if ((updatestatus instanceof TLRPC.TL_messages_sendReaction) || (updatestatus instanceof TLRPC.TL_messages_sendVote) || (updatestatus instanceof TLRPC.TL_messages_readMentions) || (updatestatus instanceof TLRPC.TL_messages_readReactions) || (updatestatus instanceof TLRPC.TL_messages_sendMessage) || (updatestatus instanceof TLRPC.TL_messages_forwardMessage) || (updatestatus instanceof TLRPC.TL_messages_forwardMessages)) {
            Main.log("Sending onInteract request", new Object[0]);
            if (Settings.getReadOnInteract()) {
                if (updatestatus instanceof TLRPC.TL_messages_sendReaction) {
                    InternalUtils.sendReadMessage(((TLRPC.TL_messages_sendReaction) updatestatus).peer, ((TLRPC.TL_messages_sendReaction) updatestatus).msg_id, false);
                }
                if (updatestatus instanceof TLRPC.TL_messages_sendVote) {
                    InternalUtils.sendReadMessage(((TLRPC.TL_messages_sendVote) updatestatus).peer, ((TLRPC.TL_messages_sendVote) updatestatus).msg_id, false);
                }
                if (updatestatus instanceof TLRPC.TL_messages_sendMessage) {
                    InternalUtils.sendReadMessage(((TLRPC.TL_messages_sendMessage) updatestatus).peer, 0, false);
                }
                if (updatestatus instanceof TLRPC.TL_messages_forwardMessage) {
                    InternalUtils.sendReadMessage(((TLRPC.TL_messages_forwardMessage) updatestatus).peer, 0, false);
                }
                if (updatestatus instanceof TLRPC.TL_messages_forwardMessages) {
                    InternalUtils.sendReadMessage(((TLRPC.TL_messages_forwardMessages) updatestatus).to_peer, 0, false);
                }
            }
        }
        if (Defaults.readingRequests.contains(updatestatus.getClass())) {
            switch (current_reading_status) {
                case Defaults.NEVER /* -1 */:
                    Main.log("Selected NEVER, reading", new Object[0]);
                    param.setResult((Object) null);
                    break;
                case Defaults.GLOBAL_VALUE /* 0 */:
                default:
                    if (Settings.getHideReadingWithGhost()) {
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
            return;
        }
        if (Defaults.typingRequests.contains(updatestatus.getClass())) {
            switch (current_typing_status) {
                case Defaults.NEVER /* -1 */:
                    Main.log("Selected NEVER, not typing", new Object[0]);
                    param.setResult((Object) null);
                    break;
                case Defaults.GLOBAL_VALUE /* 0 */:
                default:
                    if (Settings.getHideTypingWithGhost()) {
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
            return;
        }
        if (Defaults.storiesRequests.contains(updatestatus.getClass()) && Settings.getNoReadStoriesWithGhost()) {
            param.setResult((Object) null);
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Defaults.onlineRequests.contains(param.args[0].getClass()) && Settings.getImmediateOfflineWithGhost()) {
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
