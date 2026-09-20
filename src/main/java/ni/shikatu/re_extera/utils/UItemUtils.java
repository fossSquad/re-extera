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
                return item;
            } catch (Throwable ignored) {}
        }

        if (activity instanceof org.telegram.ui.ActionBar.BaseFragment) {
            SettingsRegistryHelper.addLinkAliasForOption(alias, (org.telegram.ui.ActionBar.BaseFragment) activity, item);
        }
        return item;
    }

    private static Method asShadowMethod;
    private static boolean checkedAsShadow;

    private static Method asExteraExpandableSwitchMethod;
    private static boolean checkedExpandableSwitch;
    private static final java.util.HashMap<Integer, android.view.View.OnClickListener> switchListeners = new java.util.HashMap<>();

    private static Method asCheckWithSubtitleMethod;
    private static boolean checkedAsCheckWithSubtitle;

    /**
     * exteraGram's asCheck() can carry a subtitle and an initial checked state; the plain client's
     * asCheck() only takes the title, so the extra values are applied to the item's public fields.
     */
    public static UItem asCheck(int id, CharSequence text, CharSequence subtitle, boolean checked) {
        if (!checkedAsCheckWithSubtitle) {
            checkedAsCheckWithSubtitle = true;
            try {
                for (Method m : UItem.class.getMethods()) {
                    Class<?>[] types = m.getParameterTypes();
                    if (m.getName().equals("asCheck") && types.length == 4 && types[0] == int.class
                            && types[1] == CharSequence.class && types[2] == CharSequence.class
                            && types[3] == boolean.class) {
                        asCheckWithSubtitleMethod = m;
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (asCheckWithSubtitleMethod != null) {
            try {
                return (UItem) asCheckWithSubtitleMethod.invoke(null, id, text, subtitle, checked);
            } catch (Throwable ignored) {}
        }

        UItem item = UItem.asCheck(id, text);
        item.subtext = subtitle;
        item.checked = checked;
        return item;
    }

    /**
     * exteraGram exposes UItem.asShadow() without arguments; the plain client only has the
     * CharSequence overload, which produces the same spacer row when passed null.
     */
    public static UItem asShadow() {
        if (!checkedAsShadow) {
            checkedAsShadow = true;
            try {
                asShadowMethod = UItem.class.getMethod("asShadow");
            } catch (Throwable ignored) {}
        }

        if (asShadowMethod != null) {
            try {
                return (UItem) asShadowMethod.invoke(null);
            } catch (Throwable ignored) {}
        }

        return UItem.asShadow(null);
    }

    /**
     * exteraGram's asExteraExpandableSwitch() takes the switch listener as an argument; the plain
     * client's asExpandableSwitch() has no switch of its own, so the listener is parked here and
     * attached to the row's switch view by {@link #bindSwitch(int, View)} once the row exists.
     */
    public static UItem asExpandableSwitch(int id, CharSequence text, CharSequence subtitle, android.view.View.OnClickListener onSwitch) {
        if (!checkedExpandableSwitch) {
            checkedExpandableSwitch = true;
            try {
                for (Method m : UItem.class.getMethods()) {
                    if (m.getName().equals("asExteraExpandableSwitch")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 4 && params[0] == int.class && CharSequence.class.isAssignableFrom(params[1])
                                && CharSequence.class.isAssignableFrom(params[2]) && android.view.View.OnClickListener.class.isAssignableFrom(params[3])) {
                            asExteraExpandableSwitchMethod = m;
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (asExteraExpandableSwitchMethod != null) {
            try {
                return (UItem) asExteraExpandableSwitchMethod.invoke(null, id, text, subtitle, onSwitch);
            } catch (Throwable ignored) {}
        }

        switchListeners.put(id, onSwitch);
        return UItem.asExpandableSwitch(id, text, subtitle);
    }

    /**
     * Attaches the listener parked by {@link #asExpandableSwitch} to the switch view of the row,
     * so that tapping the switch toggles the option while tapping the rest of the row expands or
     * collapses it. Safe to call more than once; the listener is consumed on the first call.
     */
    public static void bindSwitch(int id, android.view.View row) {
        android.view.View.OnClickListener listener = switchListeners.remove(id);
        if (listener == null) {
            return;
        }

        if (row instanceof org.telegram.ui.Cells.TextCheckCell2) {
            org.telegram.ui.Components.Switch checkBox = ((org.telegram.ui.Cells.TextCheckCell2) row).getCheckBox();
            if (checkBox != null) {
                checkBox.setOnClickListener(listener);
                return;
            }
        }

        row.setOnClickListener(listener);
    }
}

