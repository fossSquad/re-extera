package ni.shikatu.re_extera.hooks.profileactivity;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.ui.ShadowbanDialog;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ProfileActivity;

public class ProfileMenuShadowban extends XC_MethodHook {
    private static final Field OTHER_ITEM_FIELD = field("otherItem");
    private static final Field USER_ID_FIELD = field("userId");

    private static Field field(String name) {
        try {
            Field f = ProfileActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            Main.log("ProfileMenuShadowban: field '%s' not found: %s", name, e.getMessage());
            return null;
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (OTHER_ITEM_FIELD == null || USER_ID_FIELD == null) {
            return;
        }
        try {
            final ProfileActivity activity = (ProfileActivity) param.thisObject;
            final int currentAccount = activity.getCurrentAccount();
            final ActionBarMenuItem otherItem = (ActionBarMenuItem) ReflectionUtils.get(OTHER_ITEM_FIELD, activity);
            final long userId = ((Long) ReflectionUtils.get(USER_ID_FIELD, activity)).longValue();
            if (otherItem != null && userId > 0) {
                if (ShadowbanCache.isShadowbanned(userId)) {
                    ActionBarMenuSubItem subItem = otherItem.addSubItem(0, R.drawable.msg_block2, Localization.REMOVE_FROM_SHADOWBAN);
                    subItem.setOnClickListener(new View.OnClickListener() { 
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            ProfileMenuShadowban.lambda$afterHookedMethod$0(userId, otherItem, currentAccount, activity, view);
                        }
                    });
                } else {
                    ActionBarMenuSubItem subItem2 = otherItem.addSubItem(0, R.drawable.msg_block, Localization.ADD_TO_SHADOWBAN);
                    subItem2.setOnClickListener(new View.OnClickListener() { 
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            ProfileMenuShadowban.lambda$afterHookedMethod$2(otherItem, activity, userId, currentAccount, view);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Main.log("ProfileMenuShadowban: %s", e.getMessage());
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(long userId, ActionBarMenuItem otherItem, int currentAccount, ProfileActivity activity, View v) {
        ShadowbanCache.remove(userId);
        otherItem.closeSubMenu();
        ShadowbanCache.notifyDialogsUpdate(currentAccount);
        activity.finishFragment();
    }

    static /* synthetic */ void lambda$afterHookedMethod$2(ActionBarMenuItem otherItem, final ProfileActivity activity, long userId, final int currentAccount, View v) {
        otherItem.closeSubMenu();
        ShadowbanDialog.showAddAndSave(activity.getParentActivity(), userId, new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                ProfileMenuShadowban.lambda$afterHookedMethod$1(currentAccount, activity);
            }
        });
    }

    static /* synthetic */ void lambda$afterHookedMethod$1(int currentAccount, ProfileActivity activity) {
        ShadowbanCache.notifyDialogsUpdate(currentAccount);
        activity.finishFragment();
    }
}
