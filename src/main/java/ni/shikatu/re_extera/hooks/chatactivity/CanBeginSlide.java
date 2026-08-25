package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Disables the horizontal swipe-back gesture inside a chat.
 *
 * <p>Hooks {@code ChatActivity.canBeginSlide()} and returns {@code false} so the
 * chat cannot be dismissed (or the next chat revealed) by swiping.
 */
public class CanBeginSlide extends XC_MethodHook {

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getDisableChatSwipeBack()) {
            param.setResult(Boolean.FALSE);
        }
    }
}
