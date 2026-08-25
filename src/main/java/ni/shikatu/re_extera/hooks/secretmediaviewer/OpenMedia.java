package ni.shikatu.re_extera.hooks.secretmediaviewer;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Keeps view-once media alive inside {@code SecretMediaViewer} — for video too.
 *
 * <p>A view-once photo self-destructs when the viewer closes (already blocked via
 * {@code ChatActivity.sendSecretMediaDelete}), but a view-once VIDEO self-destructs
 * after a single playthrough through the viewer's own logic, which that ChatActivity
 * hook does not cover. The viewer gates every deletion on its private
 * {@code ignoreDelete} flag, so forcing it {@code true} at open time preserves both
 * photo and video.
 *
 * <p>Hooks {@code SecretMediaViewer.openMedia(MessageObject, PhotoViewerProvider,
 * Runnable, Runnable)} (after).
 */
public class OpenMedia extends XC_MethodHook {
    private Field ignoreDeleteField;

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getSaveOneTimeMessages()) {
            return;
        }
        try {
            if (ignoreDeleteField == null) {
                ignoreDeleteField = param.thisObject.getClass().getDeclaredField("ignoreDelete");
                ignoreDeleteField.setAccessible(true);
            }
            ignoreDeleteField.setBoolean(param.thisObject, true);
        } catch (Throwable e) {
            Main.log("SecretMediaViewer.ignoreDelete set failed: %s", e.getMessage());
        }
    }
}
