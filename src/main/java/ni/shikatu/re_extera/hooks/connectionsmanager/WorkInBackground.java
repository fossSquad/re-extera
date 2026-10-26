package ni.shikatu.re_extera.hooks.connectionsmanager;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.LaunchActivity;

public class WorkInBackground extends XC_MethodHook {
    private static Field switchingAccountField;

    static {
        try {
            switchingAccountField = LaunchActivity.class.getDeclaredField("switchingAccount");
            switchingAccountField.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSwitchingAccount() {
        if (switchingAccountField != null && LaunchActivity.instance != null) {
            try {
                return switchingAccountField.getBoolean(LaunchActivity.instance);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        if (Settings.getWorkInBackground()) {
            if (isSwitchingAccount()) {
                return;
            }
            int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
            if (currentAccount == UserConfig.selectedAccount && ApplicationLoader.mainInterfacePaused) {
                param.args[0] = false;
            }
        }
    }
}
