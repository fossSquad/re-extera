package ni.shikatu.re_extera.hooks.navigation;

import com.exteragram.messenger.drawer.DrawerMenuView;
import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.utils.GhostMenuHelper;
import org.telegram.ui.ActionBar.BaseFragment;

public class DrawerMenuGhostHook extends XC_MethodHook {
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Object obj = param.thisObject;
        if (!(obj instanceof DrawerMenuView)) {
            return;
        }
        DrawerMenuView drawerMenuView = (DrawerMenuView) obj;
        int currentAccount = ((Integer) param.args[0]).intValue();
        BaseFragment fragment = (BaseFragment) param.args[1];
        GhostMenuHelper.injectIntoDrawer(drawerMenuView, currentAccount, fragment);
    }
}
