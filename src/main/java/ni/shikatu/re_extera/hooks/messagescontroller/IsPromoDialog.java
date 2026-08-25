package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Hides the proxy/promo sponsored dialog from the chat list.
 *
 * <p>Complements the existing "Disable ads" toggle (which only clears in-chat
 * sponsored messages) by forcing {@code MessagesController.isPromoDialog(long,
 * boolean)} to {@code false}, so the promoted/proxy-sponsor channel is not shown.
 */
public class IsPromoDialog extends XC_MethodHook {

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getDisableAds()) {
            param.setResult(Boolean.FALSE);
        }
    }
}
