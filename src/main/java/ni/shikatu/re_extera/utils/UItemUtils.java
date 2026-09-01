package ni.shikatu.re_extera.utils;

import org.telegram.ui.Components.UItem;
import java.lang.reflect.Method;

public final class UItemUtils {
    private static Method setLinkAliasMethod;
    private static boolean checked;

    private UItemUtils() {}

    public static UItem setLinkAlias(UItem item, String alias, Object activity) {
        if (!checked) {
            checked = true;
            try {
                for (Method m : UItem.class.getMethods()) {
                    if (m.getName().equals("setLinkAlias")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 2 && params[0] == String.class) {
                            setLinkAliasMethod = m;
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (setLinkAliasMethod != null) {
            try {
                setLinkAliasMethod.invoke(item, alias, activity);
            } catch (Throwable ignored) {}
        }
        return item;
    }
}
