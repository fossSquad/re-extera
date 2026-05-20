package ni.shikatu.re_extera.hooks.chatmessagecell;

import android.widget.FrameLayout;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.EarListener;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.SecretVoicePlayer;
import org.telegram.ui.Stories.recorder.HintView2;

public class SecretVoicePlayerDismiss extends XC_MethodHook {
    private static final Method SETUP_TRANSLATION;
    private static final Field BACK_DIALOG = field("backDialog");
    private static final Field HINT_VIEW = field("hintView");
    private static final Field PLAYER = field("player");
    private static final Field WINDOW_VIEW = field("windowView");
    private static final Field EAR_LISTENER = field("earListener");
    private static final Field CLOSE_ACTION = field("closeAction");
    private static final Field THANOS_EFFECT = field("thanosEffect");

    static {
        Method m = null;
        try {
            m = SecretVoicePlayer.class.getDeclaredMethod("setupTranslation", new Class[0]);
            m.setAccessible(true);
        } catch (Exception e) {
            Main.log("SecretVoicePlayerDismiss: %s", e.getMessage());
        }
        SETUP_TRANSLATION = m;
    }

    private static Field field(String name) {
        try {
            Field f = SecretVoicePlayer.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            Main.log("SecretVoicePlayerDismiss: field '%s' not found: %s", name, e.getMessage());
            return null;
        }
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveOneTimeMessages()) {
            if (CLOSE_ACTION != null) {
                ReflectionUtils.set(CLOSE_ACTION, param.thisObject, null);
            }
            if (THANOS_EFFECT != null) {
                ReflectionUtils.set(THANOS_EFFECT, param.thisObject, null);
            }
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getSaveOneTimeMessages() || BACK_DIALOG == null || HINT_VIEW == null || PLAYER == null || WINDOW_VIEW == null || EAR_LISTENER == null || SETUP_TRANSLATION == null) {
            return;
        }
        AlertDialog backDialog = (AlertDialog) ReflectionUtils.get(BACK_DIALOG, param.thisObject);
        HintView2 hintView = (HintView2) ReflectionUtils.get(HINT_VIEW, param.thisObject);
        VideoPlayer player = (VideoPlayer) ReflectionUtils.get(PLAYER, param.thisObject);
        FrameLayout windowView = (FrameLayout) ReflectionUtils.get(WINDOW_VIEW, param.thisObject);
        EarListener earListener = (EarListener) ReflectionUtils.get(EAR_LISTENER, param.thisObject);
        if (backDialog != null) {
            backDialog.dismiss();
            ReflectionUtils.set(BACK_DIALOG, param.thisObject, null);
        }
        if (hintView != null) {
            hintView.hide();
        }
        if (player != null) {
            player.pause();
            player.releasePlayer(true);
            ReflectionUtils.set(PLAYER, param.thisObject, null);
        }
        ReflectionUtils.invoke(SETUP_TRANSLATION, param.thisObject, new Object[0]);
        if (windowView != null) {
            windowView.invalidate();
        }
        if (earListener != null) {
            earListener.detach();
        }
    }
}
