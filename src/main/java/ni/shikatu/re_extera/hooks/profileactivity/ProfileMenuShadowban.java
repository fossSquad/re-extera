package ni.shikatu.re_extera.hooks.profileactivity;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.ui.ShadowbanDialog;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ProfileActivity;

public class ProfileMenuShadowban extends XC_MethodHook {
    private static Field otherItemField;
    private static Field userIdField;

    static {
        try {
            otherItemField = ProfileActivity.class.getDeclaredField("otherItem");
            otherItemField.setAccessible(true);
            userIdField = ProfileActivity.class.getDeclaredField("userId");
            userIdField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("ProfileMenuShadowban: field not found: %s", e.getMessage());
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (otherItemField == null || userIdField == null) {
            return;
        }
        final ProfileActivity activity = (ProfileActivity) param.thisObject;
        final ActionBarMenuItem otherItem = (ActionBarMenuItem) otherItemField.get(activity);
        final long userId = userIdField.getLong(activity);
        if (otherItem == null || userId <= 0) {
            return;
        }
        if (ShadowbanCache.isShadowbanned(userId)) {
            ActionBarMenuSubItem subItem = otherItem.addSubItem(0, R.drawable.msg_block2, Localization.REMOVE_FROM_SHADOWBAN);
            subItem.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.hooks.profileactivity.ProfileMenuShadowban$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ProfileMenuShadowban.lambda$afterHookedMethod$0(userId, otherItem, activity, view);
                }
            });
        } else {
            ActionBarMenuSubItem subItem2 = otherItem.addSubItem(0, R.drawable.msg_block, Localization.ADD_TO_SHADOWBAN);
            subItem2.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.hooks.profileactivity.ProfileMenuShadowban$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ProfileMenuShadowban.lambda$afterHookedMethod$2(otherItem, activity, userId, view);
                }
            });
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(long userId, ActionBarMenuItem otherItem, ProfileActivity activity, View v) {
        ShadowbanCache.remove(userId);
        otherItem.closeSubMenu();
        ShadowbanCache.notifyDialogsUpdate();
        activity.finishFragment();
    }

    static /* synthetic */ void lambda$afterHookedMethod$2(ActionBarMenuItem otherItem, final ProfileActivity activity, long userId, View v) {
        otherItem.closeSubMenu();
        ShadowbanDialog.showAddAndSave(activity.getParentActivity(), userId, new Runnable() { // from class: ni.shikatu.re_extera.hooks.profileactivity.ProfileMenuShadowban$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ProfileMenuShadowban.lambda$afterHookedMethod$1(activity);
            }
        });
    }

    static /* synthetic */ void lambda$afterHookedMethod$1(ProfileActivity activity) {
        ShadowbanCache.notifyDialogsUpdate();
        activity.finishFragment();
    }
}
