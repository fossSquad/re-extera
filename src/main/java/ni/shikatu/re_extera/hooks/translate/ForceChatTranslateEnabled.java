package ni.shikatu.re_extera.hooks.translate;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Enables exteraGram's chat-level translation so the "Translate" bar can appear.
 *
 * <p>{@code TranslateController.isChatTranslateEnabled()} is
 * {@code isTranslationsAutoEnabled() && pref("translate_chat_button", false)}. exteraGram
 * defaults {@code translate_chat_button} to {@code false}, and it is the gate that lets the
 * language-detection pipeline ({@code checkTranslation}) populate {@code translatableDialogs}.
 * With it off, no dialog is ever marked translatable, so {@code isDialogTranslatable} is
 * always false and the bar (and the 3-dot Translate item) never show. Forcing this to
 * {@code true} turns detection on.
 */
public class ForceChatTranslateEnabled extends XC_MethodHook {

    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        if (Settings.getFixTranslateButton()) {
            param.setResult(Boolean.TRUE);
        }
    }
}
