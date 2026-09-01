package ni.shikatu.re_extera.utils;

import org.telegram.messenger.Utilities;
import org.telegram.ui.Components.UItem;
import java.lang.reflect.Method;

public final class UItemUtils {
    private static Method setLinkAliasMethod;
    private static boolean checkedLinkAlias;

    private static Method asSlideViewWithIdMethod;
    private static Method asSlideViewWithoutIdMethod;
    private static boolean checkedSlideView;

    private UItemUtils() {}

    public static UItem asSlideView(int id, String[] choices, int chosen, Utilities.Callback<Integer> whenChose) {
        if (!checkedSlideView) {
            checkedSlideView = true;
            try {
                for (Method m : UItem.class.getMethods()) {
                    if (m.getName().equals("asSlideView")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 4 && params[0] == int.class && params[1] == String[].class && params[2] == int.class && Utilities.Callback.class.isAssignableFrom(params[3])) {
                            asSlideViewWithIdMethod = m;
                        } else if (params.length == 3 && params[0] == String[].class && params[1] == int.class && Utilities.Callback.class.isAssignableFrom(params[2])) {
                            asSlideViewWithoutIdMethod = m;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (asSlideViewWithIdMethod != null) {
            try {
                return (UItem) asSlideViewWithIdMethod.invoke(null, id, choices, chosen, whenChose);
            } catch (Throwable ignored) {}
        }

        if (asSlideViewWithoutIdMethod != null) {
            try {
                UItem item = (UItem) asSlideViewWithoutIdMethod.invoke(null, choices, chosen, whenChose);
                if (item != null) {
                    item.id = id;
                }
                return item;
            } catch (Throwable ignored) {}
        }

        UItem item = UItem.asSlideView(choices, chosen, whenChose);
        item.id = id;
        return item;
    }

    public static UItem setLinkAlias(UItem item, String alias, Object activity) {
        if (item == null) {
            return null;
        }
        if (!checkedLinkAlias) {
            checkedLinkAlias = true;
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

