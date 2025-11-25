package ni.shikatu.re_extera.hooks.profileactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ProfileActivity;

public class UpdateProfileData extends XC_MethodHook {
    private static Method needLayout;
    private static Field onlineTextView;
    private static Field userId;

    static {
        try {
            userId = ProfileActivity.class.getDeclaredField("userId");
            onlineTextView = ProfileActivity.class.getDeclaredField("onlineTextView");
            needLayout = ProfileActivity.class.getDeclaredMethod("needLayout", Boolean.TYPE);
            needLayout.setAccessible(true);
            userId.setAccessible(true);
            onlineTextView.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("Not found field: %s", e.getMessage());
        } catch (NoSuchMethodException e2) {
            Main.log("Not found method: %s", e2.getMessage());
        }
    }

    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
        if (userId == null || onlineTextView == null) {
            return;
        }
        long user = ((Long) ReflectionUtils.get(userId, param.thisObject)).longValue();
        final SimpleTextView[] onlineView = (SimpleTextView[]) ReflectionUtils.get(onlineTextView, param.thisObject);
        if (user == UserConfig.getInstance(UserConfig.selectedAccount).clientUserId && Settings.getHideOnlineWithGhost()) {
            AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UpdateProfileData.lambda$afterHookedMethod$0(onlineView, param);
                }
            });
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(SimpleTextView[] onlineView, XC_MethodHook.MethodHookParam param) {
        for (SimpleTextView view : onlineView) {
            view.setText(Localization.OFFLINE);
        }
        ReflectionUtils.invoke(needLayout, param.thisObject, true);
    }
}
