package ni.shikatu.re_extera.hooks.chatactivity;

import android.view.View;
import android.widget.FrameLayout;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Hides the pinned-message bar at the top of a chat.
 *
 * <p>Hooks {@code ChatActivity.updatePinnedMessageView(boolean, int)} and forces
 * the private {@code pinnedMessageView} FrameLayout to {@code GONE} after Telegram
 * has (re)built it, so the header never shows.
 */
public class HidePinnedMessages extends XC_MethodHook {
    private Field pinnedViewField;

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getHidePinnedMessages()) {
            return;
        }
        try {
            if (pinnedViewField == null) {
                pinnedViewField = param.thisObject.getClass().getDeclaredField("pinnedMessageView");
                pinnedViewField.setAccessible(true);
            }
            Object view = pinnedViewField.get(param.thisObject);
            if (view instanceof FrameLayout) {
                ((FrameLayout) view).setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {
        }
    }
}
