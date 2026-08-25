package ni.shikatu.re_extera.hooks.translate;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Restores the in-chat translate button that exteraGram hides for non-Premium users.
 *
 * <p>exteraGram's {@code TranslateController.isFeatureAvailable(long)} keeps Telegram's
 * Premium gate ({@code isPremium() || chat.autotranslation}) plus the
 * {@code translate_chat_button} pref (defaulted to {@code false}); vanilla/telegraph
 * strips both. Forcing this method to {@code true} bypasses the pref and premium checks
 * for the inner {@code isDialogTranslatable(...)} test and the 3-dot "Translate" menu
 * item.
 *
 * <p>This does NOT make the bar appear on every chat: {@code isDialogTranslatable} still
 * requires that foreign text was actually detected in the dialog, so the bar shows only
 * for foreign-language chats — exactly like telegraph. The outer premium check inside
 * {@code ChatActivity.updateTopPanel} is handled separately by {@link TranslateScope}.
 */
public class TranslateFeatureAvailable extends XC_MethodHook {

    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        if (Settings.getFixTranslateButton()) {
            long id = param.args.length > 0 && param.args[0] instanceof Long ? (Long) param.args[0] : 0L;
            ni.shikatu.re_extera.Main.log("TL-DBG isFeatureAvailable(%d) forced true", id);
            param.setResult(Boolean.TRUE);
        }
    }
}
