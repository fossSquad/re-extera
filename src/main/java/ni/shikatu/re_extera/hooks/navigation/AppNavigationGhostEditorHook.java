package ni.shikatu.re_extera.hooks.navigation;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.preferences.appearance.AppNavigationPreferencesActivity;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.GhostMenuHelper;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class AppNavigationGhostEditorHook extends XC_MethodHook {
    private static Method addMenuSectionMethod;
    private static Field itemDetailsField;
    private static Constructor<?> itemInfoConstructor;
    private static Method saveAndNotifyMethod;
    private static Field stableDividerIdsField;
    private final Mode mode;

    public enum Mode {
        INIT_ITEM_DETAILS,
        ADD_MENU_SECTION,
        FILL_ITEMS,
        ON_CLICK,
        UPDATE_REORDER,
        RESET_TO_DEFAULT
    }

    public AppNavigationGhostEditorHook(Mode mode) {
        this.mode = mode;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        switch (AnonymousClass1.$SwitchMap$ni$shikatu$re_extera$hooks$navigation$AppNavigationGhostEditorHook$Mode[this.mode.ordinal()]) {
            case Defaults.ALWAYS /* 1 */:
                onBeforeAddMenuSection(param);
                break;
            case 2:
                onBeforeClick(param);
                break;
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        switch (this.mode) {
            case INIT_ITEM_DETAILS:
                onAfterInitItemDetails(param);
                break;
            case FILL_ITEMS:
                onAfterFillItems(param);
                break;
            case UPDATE_REORDER:
                onAfterUpdateReorder(param);
                break;
            case RESET_TO_DEFAULT:
                GhostMenuHelper.clampGhostMenuIndex();
                break;
        }
    }

    private void onAfterInitItemDetails(XC_MethodHook.MethodHookParam param) {
        try {
            HashMap<Integer, Object> itemDetails = (HashMap) getItemDetailsField(param.thisObject.getClass()).get(param.thisObject);
            if (itemDetails.containsKey(Integer.valueOf(GhostMenuHelper.GHOST_MENU_ITEM_ID))) {
                return;
            }
            itemDetails.put(Integer.valueOf(GhostMenuHelper.GHOST_MENU_ITEM_ID), getItemInfoConstructor().newInstance(Localization.GHOST_MODE, Integer.valueOf(R.drawable.ghost)));
        } catch (Exception e) {
            Main.log("Failed to inject ghost menu item into editor: %s", e.getMessage());
        }
    }

    private void onBeforeAddMenuSection(XC_MethodHook.MethodHookParam param) {
        ArrayList<Integer> ids = (ArrayList) param.args[3];
        boolean mainSection = ((Boolean) param.args[4]).booleanValue();
        param.args[3] = GhostMenuHelper.withGhostMenuItem(ids, mainSection);
    }

    private void onAfterFillItems(XC_MethodHook.MethodHookParam param) {
        if (GhostMenuHelper.isGhostMenuVisible() || !ExteraConfig.getMainMenuHiddenItems().isEmpty()) {
            return;
        }
        try {
            ArrayList<UItem> items = (ArrayList) param.args[0];
            UniversalAdapter adapter = (UniversalAdapter) param.args[1];
            ReflectionUtils.invoke(getAddMenuSectionMethod(param.thisObject.getClass()), param.thisObject, items, adapter, LocaleController.getString(R.string.MainMenuHiddenItems), new ArrayList(), false);
            items.add(UItem.asShadow((CharSequence) null));
        } catch (Exception e) {
            Main.log("Failed to append hidden ghost editor section: %s", e.getMessage());
        }
    }

    private void onBeforeClick(XC_MethodHook.MethodHookParam param) {
        UItem item = (UItem) param.args[0];
        if (item == null) {
            return;
        }
        if (item.id == 910001) {
            GhostMenuHelper.setGhostMenuVisible(!GhostMenuHelper.isGhostMenuVisible());
            notifyMenuChanged(param.thisObject);
            param.setResult((Object) null);
        } else {
            if (!GhostMenuHelper.isGhostMenuVisible()) {
                return;
            }
            List<Integer> displayedIds = GhostMenuHelper.buildMainSectionDisplayedIds(getStableDividerIds(param.thisObject));
            int itemIndex = displayedIds.indexOf(Integer.valueOf(item.id));
            if (itemIndex >= 0 && itemIndex < GhostMenuHelper.getGhostMenuIndex()) {
                GhostMenuHelper.setGhostMenuIndex(GhostMenuHelper.getGhostMenuIndex() - 1);
            }
        }
    }

    private void onAfterUpdateReorder(XC_MethodHook.MethodHookParam param) {
        int sectionId = ((Integer) param.args[0]).intValue();
        ArrayList<UItem> items = (ArrayList) param.args[1];
        boolean changed = GhostMenuHelper.applyReorderedSection(sectionId, items);
        if (changed | GhostMenuHelper.scrubGhostFromConfig()) {
            notifyMenuChanged(param.thisObject);
        }
    }

    private List<Integer> getStableDividerIds(Object activity) {
        try {
            return (List) getStableDividerIdsField(activity.getClass()).get(activity);
        } catch (Exception e) {
            Main.log("Failed to read stable divider ids: %s", e.getMessage());
            return Collections.EMPTY_LIST;
        }
    }

    private void notifyMenuChanged(Object activity) {
        try {
            GhostMenuHelper.scrubGhostFromConfig();
            ReflectionUtils.invoke(getSaveAndNotifyMethod(activity.getClass()), activity, new Object[0]);
        } catch (Exception e) {
            Main.log("Failed to refresh app navigation editor: %s", e.getMessage());
        }
    }

    private static Field getItemDetailsField(Class<?> clazz) throws Exception {
        if (itemDetailsField == null) {
            itemDetailsField = clazz.getDeclaredField("itemDetails");
            itemDetailsField.setAccessible(true);
        }
        return itemDetailsField;
    }

    private static Field getStableDividerIdsField(Class<?> clazz) throws Exception {
        if (stableDividerIdsField == null) {
            stableDividerIdsField = clazz.getDeclaredField("stableDividerIds");
            stableDividerIdsField.setAccessible(true);
        }
        return stableDividerIdsField;
    }

    private static Constructor<?> getItemInfoConstructor() throws Exception {
        if (itemInfoConstructor == null) {
            Class<?> itemInfoClass = null;
            for (Class<?> declaredClass : AppNavigationPreferencesActivity.class.getDeclaredClasses()) {
                if ("ItemInfo".equals(declaredClass.getSimpleName())) {
                    itemInfoClass = declaredClass;
                    break;
                }
            }
            if (itemInfoClass == null) {
                throw new ClassNotFoundException("AppNavigationPreferencesActivity$ItemInfo");
            }
            itemInfoConstructor = itemInfoClass.getDeclaredConstructor(CharSequence.class, Integer.TYPE);
            itemInfoConstructor.setAccessible(true);
        }
        return itemInfoConstructor;
    }

    private static Method getAddMenuSectionMethod(Class<?> clazz) throws Exception {
        if (addMenuSectionMethod == null) {
            addMenuSectionMethod = clazz.getDeclaredMethod("addMenuSection", ArrayList.class, UniversalAdapter.class, String.class, ArrayList.class, Boolean.TYPE);
            addMenuSectionMethod.setAccessible(true);
        }
        return addMenuSectionMethod;
    }

    private static Method getSaveAndNotifyMethod(Class<?> clazz) throws Exception {
        if (saveAndNotifyMethod == null) {
            saveAndNotifyMethod = clazz.getDeclaredMethod("saveAndNotify", new Class[0]);
            saveAndNotifyMethod.setAccessible(true);
        }
        return saveAndNotifyMethod;
    }
}
