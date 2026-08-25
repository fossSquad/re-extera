package ni.shikatu.re_extera.hooks.translate;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Forces {@code TranslateController.isDialogTranslatable(id)} to {@code true}.
 *
 * <p>On this exteraGram build the language-detection pipeline never adds the dialog to
 * {@code translatableDialogs} (it stays empty), so the stock method returns false and the
 * bar/menu never appear. Rather than fight the detection gate, we mark every real dialog
 * translatable directly. Telegram's translate flow then offers the bar; if the chat is
 * already in the UI language the user just won't use it. Self/encrypted chats are left to
 * the caller's other guards.
 */
public class ForceDialogTranslatable extends XC_MethodHook {
    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        if (Settings.getFixTranslateButton()) {
            param.setResult(Boolean.TRUE);
        }
    }
}
