package ni.shikatu.re_extera.hooks.translate;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Wraps {@code ChatActivity.updateTopPanel(boolean)} so that {@link TranslateScope} is
 * active for the duration of the call. While active, the {@code UserConfig.isPremium()}
 * hook returns {@code true}, which clears the inline premium gate that otherwise hides
 * the translate bar for non-Premium DMs. The scope is always cleared in
 * {@code afterHookedMethod} (even on exceptions) so premium is never spoofed outside
 * this method.
 */
public class UpdateTopPanelScope extends XC_MethodHook {

    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        if (Settings.getFixTranslateButton()) {
            TranslateScope.enter();
        }
    }

    @Override
    public void afterHookedMethod(MethodHookParam param) {
        TranslateScope.exit();
    }
}
