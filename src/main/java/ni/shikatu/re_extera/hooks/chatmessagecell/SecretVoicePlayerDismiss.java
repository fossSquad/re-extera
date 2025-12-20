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
    private static Field backDialog;
    private static Field earListener;
    private static Field hintView;
    private static Field player;
    private static Method setupTranslation;
    private static Field windowView;

    static {
        try {
            backDialog = SecretVoicePlayer.class.getDeclaredField("backDialog");
            backDialog.setAccessible(true);
            hintView = SecretVoicePlayer.class.getDeclaredField("hintView");
            hintView.setAccessible(true);
            player = SecretVoicePlayer.class.getDeclaredField("player");
            player.setAccessible(true);
            windowView = SecretVoicePlayer.class.getDeclaredField("windowView");
            windowView.setAccessible(true);
            earListener = SecretVoicePlayer.class.getDeclaredField("earListener");
            earListener.setAccessible(true);
            setupTranslation = SecretVoicePlayer.class.getDeclaredMethod("setupTranslation", new Class[0]);
            setupTranslation.setAccessible(true);
        } catch (Exception e) {
            ReflectionUtils.hookError();
            Main.log("SecretVoicePlayerDismiss: %s", e.getMessage());
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveOneTimeMessages()) {
            Field closeActionF = SecretVoicePlayer.class.getDeclaredField("closeAction");
            closeActionF.setAccessible(true);
            closeActionF.set(param.thisObject, null);
            Field thanosEffectF = SecretVoicePlayer.class.getDeclaredField("thanosEffect");
            thanosEffectF.setAccessible(true);
            thanosEffectF.set(param.thisObject, null);
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveOneTimeMessages()) {
            AlertDialog backDialogC = (AlertDialog) backDialog.get(param.thisObject);
            HintView2 hintViewC = (HintView2) hintView.get(param.thisObject);
            VideoPlayer playerC = (VideoPlayer) player.get(param.thisObject);
            FrameLayout windowViewC = (FrameLayout) windowView.get(param.thisObject);
            EarListener earListenerC = (EarListener) earListener.get(param.thisObject);
            if (backDialogC != null) {
                backDialogC.dismiss();
                backDialog.set(param.thisObject, null);
            }
            if (hintViewC != null) {
                hintViewC.hide();
            }
            if (playerC != null) {
                playerC.pause();
                playerC.releasePlayer(true);
                player.set(param.thisObject, null);
            }
            ReflectionUtils.invoke(setupTranslation, param.thisObject, new Object[0]);
            if (windowViewC != null) {
                windowViewC.invalidate();
            }
            if (earListenerC != null) {
                earListenerC.detach();
            }
        }
    }
}
