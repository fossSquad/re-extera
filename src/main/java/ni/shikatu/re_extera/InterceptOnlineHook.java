package ni.shikatu.re_extera;

import de.robv.android.xposed.XC_MethodHook;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

public class InterceptOnlineHook extends XC_MethodHook {
    private static final TL_account.updateStatus offlineStatus = new TL_account.updateStatus();
    private static int exception_reading = 0;
    private static int exception_typing = 0;

    public static void notifyDialogIdChanged(long to) {
        exception_reading = DbDeletedStore.get().getDialogReading(to);
        exception_typing = DbDeletedStore.get().getDialogTyping(to);
    }

    InterceptOnlineHook() {
        offlineStatus.offline = true;
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        TLObject obj = (TLObject) param.args[0];
        if ((obj instanceof TLRPC.TL_updateUserStatus) && Settings.getHideOnline()) {
            param.setResult((Object) null);
            return;
        }
        if (Global.readingRequests.contains(obj.getClass())) {
            switch (exception_reading) {
                case Global.NEVER /* -1 */:
                    param.setResult((Object) null);
                    return;
                case Global.GLOBAL_VALUE /* 0 */:
                default:
                    if (Settings.getHideReading()) {
                        param.setResult((Object) null);
                        return;
                    }
                    break;
                case Global.ALWAYS /* 1 */:
                    break;
            }
        }
        if (Global.typingRequests.contains(obj.getClass())) {
            switch (exception_typing) {
                case Global.NEVER /* -1 */:
                    param.setResult((Object) null);
                    return;
                case Global.GLOBAL_VALUE /* 0 */:
                default:
                    if (Settings.getHideTyping()) {
                        param.setResult((Object) null);
                        return;
                    }
                    break;
                case Global.ALWAYS /* 1 */:
                    break;
            }
        }
        if (Global.storiesRequests.contains(obj.getClass()) && Settings.getNoReadStories()) {
            param.setResult((Object) null);
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        Global.log("sending offline");
        if (Global.onlineRequests.contains(param.args[0].getClass())) {
            ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(offlineStatus, new RequestDelegate() { // from class: ni.shikatu.re_extera.InterceptOnlineHook$$ExternalSyntheticLambda0
                public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                    InterceptOnlineHook.lambda$afterHookedMethod$0(tLObject, tL_error);
                }
            });
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(TLObject unused, TLRPC.TL_error unused2) {
    }
}
