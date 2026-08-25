package ni.shikatu.re_extera.hooks.profileactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Disables the swipe gesture on the profile avatar gallery.
 *
 * <p>Hooks {@code ProfileGalleryView.onInterceptTouchEvent(MotionEvent)} and
 * returns {@code false} so horizontal swipes over the profile header are ignored.
 */
public class DisableProfileSwipe extends XC_MethodHook {

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getDisableProfileSwipe()) {
            param.setResult(Boolean.FALSE);
        }
    }
}
