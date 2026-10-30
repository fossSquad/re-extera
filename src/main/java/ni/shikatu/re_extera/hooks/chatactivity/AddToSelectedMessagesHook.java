package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

public class AddToSelectedMessagesHook extends XC_MethodHook {
    private final ThreadLocal<TLRPC.Message> modifiedLocal = new ThreadLocal<>();

    @Override
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.noForward()) {
            return;
        }
        MessageObject msg = param.args != null && param.args.length > 0 && param.args[0] instanceof MessageObject ? (MessageObject) param.args[0] : null;
        if (msg != null && msg.messageOwner != null && msg.messageOwner.noforwards) {
            msg.messageOwner.noforwards = false;
            modifiedLocal.set(msg.messageOwner);
        }
    }

    @Override
    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        TLRPC.Message msg = modifiedLocal.get();
        modifiedLocal.remove();
        if (msg != null) {
            msg.noforwards = true;
        }
    }
}
