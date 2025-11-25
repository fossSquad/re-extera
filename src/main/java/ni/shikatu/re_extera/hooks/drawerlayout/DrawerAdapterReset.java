package ni.shikatu.re_extera.hooks.drawerlayout;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.DrawerLayoutAdapter;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

public class DrawerAdapterReset extends XC_MethodHook {
    private static Field itemsField;

    static {
        try {
            itemsField = DrawerLayoutAdapter.class.getDeclaredField("items");
            itemsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("NoSuchFieldException", e.getMessage());
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getAddGhostToDrawer()) {
            DrawerLayoutAdapter thisObject = (DrawerLayoutAdapter) param.thisObject;
            ArrayList<DrawerLayoutAdapter.Item> items = (ArrayList) itemsField.get(thisObject);
            DrawerLayoutAdapter.Item ghostItem = new DrawerLayoutAdapter.Item(7799, Settings.getGhostModeEnabledGlobal() ? Localization.GHOST_MODE_DISABLE : Localization.GHOST_MODE_ENABLE, R.drawable.ghost);
            ghostItem.onClick(new View.OnClickListener() { // from class: ni.shikatu.re_extera.hooks.drawerlayout.DrawerAdapterReset$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    DrawerAdapterReset.lambda$afterHookedMethod$0(view);
                }
            });
            if (items != null) {
                items.add(0, null);
                items.add(0, ghostItem);
            }
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(View v) {
        InternalUtils.createShortVibration();
        Settings.setGhostModeEnabledGlobal(!Settings.getGhostModeEnabledGlobal());
        BaseFragment lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment != null) {
            if (Settings.getGhostModeEnabledGlobal()) {
                BulletinFactory.of(lastFragment).createEmojiBulletin("👻", Localization.GHOST_MODE_ENABLED).show();
            } else {
                BulletinFactory.of(lastFragment).createEmojiBulletin("👻", Localization.GHOST_MODE_DISABLED).show();
            }
        }
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
    }
}
