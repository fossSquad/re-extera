package ni.shikatu.re_extera.hooks.launchactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.NotificationCenter;

/**
 * Suppresses the "TL scheme parse exception" error dialog.
 *
 * <p>Hooks {@code LaunchActivity.didReceivedNotification(int, int, Object[])} and
 * swallows the {@code tlSchemeParseException} notification so the disruptive error
 * popup never appears.
 */
public class HideTlError extends XC_MethodHook {

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getHideTlError()) {
            return;
        }
        if (param.args.length > 0
                && param.args[0] instanceof Integer
                && ((Integer) param.args[0]).intValue() == NotificationCenter.tlSchemeParseException) {
            param.setResult(null);
        }
    }
}
