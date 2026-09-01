package ni.shikatu.re_extera.utils;

import com.exteragram.messenger.preferences.utils.SettingsRegistry;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.UItem;

public final class SettingsRegistryHelper {
    private SettingsRegistryHelper() {
    }

    public static String getFirstSettingLink(Class<? extends BaseFragment> owner, UItem item) {
        try {
            return SettingsRegistry.getInstance().getFirstSettingLink(owner, item);
        } catch (Throwable t1) {
            try {
                Class<?> exteralessRegistry = Class.forName("app.exteraless.settings.utils.SettingsRegistry");
                Object registry = exteralessRegistry.getMethod("getInstance").invoke(null);
                if (registry != null) {
                    return (String) exteralessRegistry.getMethod("getFirstSettingLink", Class.class, UItem.class).invoke(registry, owner, item);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
