package ni.shikatu.re_extera.hooks.stories;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Allows saving/screenshotting of protected stories.
 *
 * <p>Hooks {@code PeerStoriesView$StoryItemHolder.allowScreenshots()} and returns
 * {@code true}, which unlocks the story download/save path that Telegram otherwise
 * blocks for {@code noforwards} stories.
 */
public class AllowScreenshots extends XC_MethodHook {

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveProtectedStories()) {
            param.setResult(Boolean.TRUE);
        }
    }
}
