package ni.shikatu.re_extera.hooks.maintabs;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.LiteMode;

/**
 * Forces the iOS-26 style "liquid glass" effect on exteraGram's bottom navigation
 * bar (Chats / Contacts / Settings / Profile).
 *
 * <p>exteraGram already ships the whole glass pipeline
 * ({@code org.telegram.ui.Components.blur3.LiquidGlassEffect}, {@code GlassTabView},
 * the {@code blur3} backdrop-blur sources). The tab bar looks solid because its
 * render node ({@code MainTabsActivity$1.renderNodeUpdateDisplayList}) only captures
 * and blurs the content behind the bar when {@code SharedConfig.chatBlurEnabled()} is
 * true; otherwise it just fills a flat {@code windowBackgroundWhite} color. And
 * {@code chatBlurEnabled()} itself is {@code canBlurChat() && LiteMode.isEnabled(FLAG_CHAT_BLUR)}.
 *
 * <p>So two LiteMode flags — both read through {@link LiteMode#isEnabled(int)} — decide
 * the look:
 * <ul>
 *   <li>{@link LiteMode#FLAG_CHAT_BLUR} (0x100): enables the backdrop blur capture, which
 *       turns the solid pill into frosted glass. This is the flag that was missing.</li>
 *   <li>{@link LiteMode#FLAG_LIQUID_GLASS} (0x40000): enables the AGSL refraction/shine on
 *       top (Android 13+ only).</li>
 * </ul>
 *
 * <p>Forcing {@code FLAG_CHAT_BLUR} is exactly what exteraGram's own "Force Blur" appearance
 * option does (it calls {@code SharedConfig.toggleChatBlur()}); it also enables Telegram's
 * chat-background blur elsewhere, which is the intended cost of the glass aesthetic. The hook
 * is reversible: flip the toggle off and every flag passes through untouched.
 *
 * <p><b>OS limits a hook cannot bypass:</b> the backdrop blur needs a RenderNode
 * ({@code SDK_INT >= 31}, Android 12) and the {@code LiquidGlassEffect} AGSL
 * {@code RuntimeShader} needs {@code SDK_INT >= 33} (Android 13). Below that the source
 * falls back to a solid color regardless of these flags — those APIs don't exist there.
 */
public class ForceLiquidGlass extends XC_MethodHook {

    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        if (!Settings.getLiquidGlassTabs()) {
            return;
        }
        Object flag = param.args[0];
        if (!(flag instanceof Integer)) {
            return;
        }
        int f = (Integer) flag;
        if (f == LiteMode.FLAG_LIQUID_GLASS || f == LiteMode.FLAG_CHAT_BLUR) {
            param.setResult(Boolean.TRUE);
        }
    }
}
