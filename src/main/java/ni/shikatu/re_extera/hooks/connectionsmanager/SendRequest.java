package ni.shikatu.re_extera.hooks.connectionsmanager;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

public class SendRequest extends XC_MethodHook {
    private static final TL_account.updateStatus OFFLINE_STATUS = new TL_account.updateStatus();
    private static volatile int currentReadingStatus;
    private static volatile int currentTypingStatus;

    static {
        OFFLINE_STATUS.offline = true;
    }

    public static void notifyDialogIdChanged(long dialogId) {
        currentReadingStatus = ReExteraDb.get().getDialogReading(dialogId);
        currentTypingStatus = ReExteraDb.get().getDialogTyping(dialogId);
    }

    public static int getCurrentReadingStatus() {
        return currentReadingStatus;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        org.telegram.tgnet.TLObject request = (org.telegram.tgnet.TLObject) param.args[0];
        if (Main.ignoredRequests.remove(request)) {
            return;
        }
        if (request instanceof TL_account.updateStatus) {
            TL_account.updateStatus statusUpdate = (TL_account.updateStatus) request;
            if (Settings.getHideOnlineWithGhost()) {
                statusUpdate.offline = true;
            }
        }
        if (Settings.getReadOnInteract() && isInteractionRequest(request)) {
            dispatchReadOnInteract(currentAccount, request);
            return;
        }
        if (Defaults.readingRequests.contains(request.getClass())) {
            applyReadingPolicy(param);
            return;
        }
        if (Defaults.typingRequests.contains(request.getClass())) {
            applyTypingPolicy(param);
        } else if (Defaults.storiesRequests.contains(request.getClass()) && Settings.getNoReadStoriesWithGhost()) {
            param.setResult((Object) null);
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getImmediateOfflineWithGhost() && !(param.args[0] instanceof TL_account.updateStatus)) {
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
            ConnectionsManager.getInstance(currentAccount).sendRequest(OFFLINE_STATUS, new RequestDelegate() { 
                public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                    SendRequest.lambda$afterHookedMethod$0(tLObject, tL_error);
                }
            });
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(TLObject __, TLRPC.TL_error ___) {
    }

    private static boolean isInteractionRequest(TLObject update) {
        return (update instanceof TLRPC.TL_messages_sendReaction) || (update instanceof TLRPC.TL_messages_sendVote) || (update instanceof TLRPC.TL_messages_readMentions) || (update instanceof TLRPC.TL_messages_readReactions) || (update instanceof TLRPC.TL_messages_sendMessage) || (update instanceof TLRPC.TL_messages_forwardMessages);
    }

    private static void dispatchReadOnInteract(int currentAccount, TLObject update) {
        Main.log("SendRequest: read-on-interact dispatched for %s", update.getClass().getSimpleName());
        if (update instanceof TLRPC.TL_messages_sendReaction) {
            TLRPC.TL_messages_sendReaction r = (TLRPC.TL_messages_sendReaction) update;
            InternalUtils.sendReadMessage(currentAccount, r.peer, r.msg_id, false);
            return;
        }
        if (update instanceof TLRPC.TL_messages_sendVote) {
            TLRPC.TL_messages_sendVote v = (TLRPC.TL_messages_sendVote) update;
            InternalUtils.sendReadMessage(currentAccount, v.peer, v.msg_id, false);
        } else if (update instanceof TLRPC.TL_messages_sendMessage) {
            TLRPC.TL_messages_sendMessage m = (TLRPC.TL_messages_sendMessage) update;
            InternalUtils.sendReadMessage(currentAccount, m.peer, 0, false);
        } else if (update instanceof TLRPC.TL_messages_forwardMessages) {
            TLRPC.TL_messages_forwardMessages f = (TLRPC.TL_messages_forwardMessages) update;
            InternalUtils.sendReadMessage(currentAccount, f.to_peer, 0, false);
        }
    }

    private void applyReadingPolicy(XC_MethodHook.MethodHookParam param) {
        switch (currentReadingStatus) {
            case Defaults.NEVER /* -1 */:
                param.setResult((Object) null);
                break;
            case Defaults.GLOBAL_VALUE /* 0 */:
            default:
                if (Settings.getHideReadingWithGhost()) {
                    param.setResult((Object) null);
                }
                break;
            case Defaults.ALWAYS /* 1 */:
                break;
        }
    }

    private void applyTypingPolicy(XC_MethodHook.MethodHookParam param) {
        switch (currentTypingStatus) {
            case Defaults.NEVER /* -1 */:
                param.setResult((Object) null);
                break;
            case Defaults.GLOBAL_VALUE /* 0 */:
            default:
                if (Settings.getHideTypingWithGhost()) {
                    param.setResult((Object) null);
                }
                break;
            case Defaults.ALWAYS /* 1 */:
                break;
        }
    }
}
