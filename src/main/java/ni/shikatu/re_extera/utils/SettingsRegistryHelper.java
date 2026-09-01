package ni.shikatu.re_extera.utils;

import java.lang.reflect.Method;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.UItem;

public final class SettingsRegistryHelper {
    private static Object registryInstance;
    private static Method getFirstSettingLinkMethod;
    private static boolean checked;

    private SettingsRegistryHelper() {
    }

    private static void init() {
        if (checked) return;
        checked = true;
        Class<?> clazz = null;
        try {
            clazz = Class.forName("com.exteragram.messenger.preferences.utils.SettingsRegistry");
        } catch (Throwable ignored) {}
        if (clazz == null) {
            try {
                clazz = Class.forName("app.exteraless.settings.utils.SettingsRegistry");
            } catch (Throwable ignored) {}
        }
        if (clazz != null) {
            try {
                Method getInstanceMethod = clazz.getMethod("getInstance");
                registryInstance = getInstanceMethod.invoke(null);
                getFirstSettingLinkMethod = clazz.getMethod("getFirstSettingLink", Class.class, UItem.class);
            } catch (Throwable ignored) {}
        }
    }

    public static String getFirstSettingLink(Class<? extends BaseFragment> owner, UItem item) {
        init();
        if (registryInstance != null && getFirstSettingLinkMethod != null) {
            try {
                return (String) getFirstSettingLinkMethod.invoke(registryInstance, owner, item);
            } catch (Throwable ignored) {}
        }
        return null;
    }
}

