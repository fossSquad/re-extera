package ni.shikatu.re_extera.hooks.userconfig;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.hooks.translate.TranslateScope;
import ni.shikatu.re_extera.settings.Settings;

public class isPremium extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        // Local Premium spoofs premium everywhere; the translate-button fix spoofs it
        // only while ChatActivity.updateTopPanel runs (see TranslateScope).
        if (Settings.getLocalPremium() || TranslateScope.isActive()) {
            param.setResult(true);
        }
    }
}
