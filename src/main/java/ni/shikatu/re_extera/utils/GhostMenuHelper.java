package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.MainMenuItem;
import com.exteragram.messenger.drawer.DrawerMenuItemView;
import com.exteragram.messenger.drawer.DrawerMenuView;
import com.exteragram.messenger.plugins.Plugin;
import com.exteragram.messenger.plugins.PluginsConstants;
import com.exteragram.messenger.plugins.PluginsController;
import com.exteragram.messenger.utils.chats.MainMenuHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.LaunchActivity;

public final class GhostMenuHelper {
    private static final float DIVIDER_HEIGHT_DP = 1.0f / AndroidUtilities.density;
    private static final int DIVIDER_HORIZONTAL_MARGIN_DP = 12;
    private static final int DIVIDER_VERTICAL_MARGIN_DP = 8;
    public static final int GHOST_MENU_ITEM_ID = 910001;
    private static final String GHOST_PLUGIN_ID = "__re_extera_native__";
    private static final String GHOST_PLUGIN_MENU_ITEM_ID = "re_extera_ghost_mode";
    private static final int GHOST_PLUGIN_MENU_PRIORITY = 1000;
    private static final String GHOST_PLUGIN_NAME = "re:extera";
    private static Field drawerContainerField;
    private static Field drawerOnItemClickField;
    private static boolean pluginMenuRegistered;
    private static Class<?> pyObjectClass;
    private static Class<?> pythonClass;

    private GhostMenuHelper() {
    }

    public static void ensureInitialized() {
        if (!Settings.isGhostPositionInitialized()) {
            Settings.setGhostInMainMenu(Settings.getAddGhostToDrawer());
            Settings.setGhostMenuIndex(countEditorVisibleItems(ExteraConfig.getMainMenuLayout()));
            Settings.setGhostPositionInitialized(true);
        }
        clampGhostMenuIndex();
        syncLegacyVisibility();
    }

    public static boolean isGhostMenuVisible() {
        ensureInitialized();
        return Settings.getGhostInMainMenu();
    }

    public static void setGhostMenuVisible(boolean visible) {
        ensureInitialized();
        if (visible && Settings.getGhostMenuIndex() < 0) {
            Settings.setGhostMenuIndex(countEditorVisibleItems(ExteraConfig.getMainMenuLayout()));
        }
        Settings.setGhostInMainMenu(visible);
        Settings.setAddGhostToDrawer(visible);
        clampGhostMenuIndex();
        registerPluginMenuItem(true);
    }

    public static int getGhostMenuIndex() {
        ensureInitialized();
        return Settings.getGhostMenuIndex();
    }

    public static void setGhostMenuIndex(int index) {
        ensureInitialized();
        Settings.setGhostMenuIndex(Math.max(0, index));
        clampGhostMenuIndex();
    }

    public static void clampGhostMenuIndex() {
        int max = countEditorVisibleItems(ExteraConfig.getMainMenuLayout());
        int current = Settings.getGhostMenuIndex();
        int clamped = Math.max(0, Math.min(current, max));
        if (current != clamped) {
            Settings.setGhostMenuIndex(clamped);
        }
    }

    public static ArrayList<Integer> withGhostMenuItem(List<Integer> sourceIds, boolean mainSection) {
        ArrayList<Integer> ids = new ArrayList<>(sourceIds);
        ids.removeIf(new Predicate() { 
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GhostMenuHelper.lambda$withGhostMenuItem$0((Integer) obj);
            }
        });
        Integer numValueOf = Integer.valueOf(GHOST_MENU_ITEM_ID);
        if (mainSection) {
            if (isGhostMenuVisible()) {
                ids.add(getRawInsertionIndex(ids, getGhostMenuIndex()), numValueOf);
            }
        } else if (!isGhostMenuVisible()) {
            ids.add(0, numValueOf);
        }
        return ids;
    }

    static /* synthetic */ boolean lambda$withGhostMenuItem$0(Integer id) {
        return id != null && id.intValue() == 910001;
    }

    public static ArrayList<Integer> buildMainSectionDisplayedIds(List<Integer> stableDividerIds) {
        ArrayList<Integer> ids = new ArrayList<>();
        int dividerIndex = 0;
        for (Integer id : ExteraConfig.getMainMenuLayout()) {
            if (id != null) {
                if (id.intValue() == MainMenuItem.DIVIDER.getId()) {
                    if (stableDividerIds != null && dividerIndex < stableDividerIds.size()) {
                        ids.add(stableDividerIds.get(dividerIndex));
                    }
                    dividerIndex++;
                } else if (isEditorVisibleItem(id.intValue())) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public static boolean applyReorderedSection(int sectionId, ArrayList<UItem> items) {
        int ghostIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) != null && items.get(i).id == 910001) {
                ghostIndex = i;
                break;
            }
        }
        if (ghostIndex < 0) {
            return false;
        }
        if (sectionId == 0) {
            setGhostMenuIndex(ghostIndex);
            setGhostMenuVisible(true);
        } else {
            setGhostMenuVisible(false);
        }
        return true;
    }

    static /* synthetic */ boolean lambda$scrubGhostFromConfig$1(Integer id) {
        return id != null && id.intValue() == 910001;
    }

    public static boolean scrubGhostFromConfig() {
        boolean changed = ExteraConfig.getMainMenuLayout().removeIf(new Predicate() { 
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GhostMenuHelper.lambda$scrubGhostFromConfig$1((Integer) obj);
            }
        });
        return changed | ExteraConfig.getMainMenuHiddenItems().removeIf(new Predicate() { 
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GhostMenuHelper.lambda$scrubGhostFromConfig$2((Integer) obj);
            }
        });
    }

    static /* synthetic */ boolean lambda$scrubGhostFromConfig$2(Integer id) {
        return id != null && id.intValue() == 910001;
    }

    public static void injectIntoDrawer(DrawerMenuView drawerMenuView, int currentAccount, final BaseFragment fragment) {
        LinearLayout container;
        ensureInitialized();
        if (registerPluginMenuItem(false) || !isGhostMenuVisible() || (container = getDrawerContainer(drawerMenuView)) == null) {
            return;
        }
        DrawerInsertionInfo insertionInfo = resolveDrawerInsertionInfo(currentAccount, fragment);
        if (!insertionInfo.shouldInsert) {
            return;
        }
        int childIndex = Math.min(insertionInfo.childIndex, container.getChildCount());
        if (insertionInfo.addDivider) {
            container.addView(createDividerView(drawerMenuView.getContext()), childIndex);
            childIndex++;
        }
        DrawerMenuItemView itemView = new DrawerMenuItemView(drawerMenuView.getContext());
        itemView.setMenuItem(GHOST_MENU_ITEM_ID, currentAccount, R.drawable.ghost, getDrawerTitle());
        final Runnable onItemClick = getDrawerOnItemClick(drawerMenuView);
        itemView.setOnClickListener(new View.OnClickListener() { 
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                GhostMenuHelper.lambda$injectIntoDrawer$3(onItemClick, fragment, view);
            }
        });
        container.addView((View) itemView, childIndex);
    }

    static /* synthetic */ void lambda$injectIntoDrawer$3(Runnable onItemClick, BaseFragment fragment, View v) {
        if (onItemClick != null) {
            onItemClick.run();
        }
        toggleGhostMode(fragment);
    }

    public static void toggleGhostMode(BaseFragment fragment) {
        boolean enabled = !Settings.getGhostModeEnabledGlobal();
        Settings.setGhostModeEnabledGlobal(enabled);
        registerPluginMenuItem(true);
        if (fragment != null) {
            BulletinFactory.of(fragment).createSuccessBulletin(enabled ? Localization.GHOST_MODE_ENABLED : Localization.GHOST_MODE_DISABLED).show();
        }
    }

    public static void toggleGhostModeFromPlugin(Object ignoredContextData) {
        Main.log("Ghost mode plugin menu item clicked", new Object[0]);
        toggleGhostMode(LaunchActivity.getSafeLastFragment());
    }

    public static boolean registerPluginMenuItem() {
        return registerPluginMenuItem(false);
    }

    private static boolean registerPluginMenuItem(boolean force) {
        ensureInitialized();
        if (!isGhostMenuVisible()) {
            unregisterPluginMenuItem();
            return false;
        }
        if (pluginMenuRegistered && !force) {
            return true;
        }
        Object python = getStartedPython();
        if (!PluginsController.isPluginEngineAvailable() || python == null) {
            return false;
        }
        try {
            PluginsController controller = PluginsController.getInstance();
            Plugin plugin = (Plugin) controller.getPlugins().get(GHOST_PLUGIN_ID);
            if (plugin == null) {
                plugin = new Plugin(GHOST_PLUGIN_ID, GHOST_PLUGIN_NAME);
                plugin.setEngine(PluginsConstants.PYTHON);
                plugin.setDescription("Native re:extera integration");
                plugin.setAuthor(GHOST_PLUGIN_NAME);
                controller.getPlugins().put(GHOST_PLUGIN_ID, plugin);
            }
            plugin.setError((Throwable) null);
            plugin.setEnabled(true);
            String itemId = addPluginMenuItem(controller, createPluginMenuItemData(python));
            pluginMenuRegistered = GHOST_PLUGIN_MENU_ITEM_ID.equals(itemId);
            return pluginMenuRegistered;
        } catch (Throwable e) {
            Main.log("Failed to register ghost plugin menu item: %s", e.getMessage());
            pluginMenuRegistered = false;
            return false;
        }
    }

    private static void unregisterPluginMenuItem() {
        if (!pluginMenuRegistered || !PluginsController.isPluginEngineSupported()) {
            return;
        }
        try {
            PluginsController.getInstance().removeMenuItem(GHOST_PLUGIN_ID, GHOST_PLUGIN_MENU_ITEM_ID);
        } catch (Throwable e) {
            try {
                Main.log("Failed to unregister ghost plugin menu item: %s", e.getMessage());
            } finally {
                pluginMenuRegistered = false;
            }
        }
    }

    private static Object createPluginMenuItemData(Object python) throws ReflectiveOperationException {
        Object data = pyCall(pyGet(getBuiltins(python), "dict"), new Object[0]);
        Map<Object, Object> map = pyAsMap(data);
        putPy(map, PluginsConstants.MenuItemProperties.MENU_TYPE, PluginsConstants.MenuItemTypes.DRAWER_MENU);
        putPy(map, PluginsConstants.MenuItemProperties.ITEM_ID, GHOST_PLUGIN_MENU_ITEM_ID);
        putPy(map, PluginsConstants.MenuItemProperties.TEXT, getDrawerTitle());
        putPy(map, PluginsConstants.MenuItemProperties.ICON, "ghost");
        putPy(map, PluginsConstants.MenuItemProperties.PRIORITY, Integer.valueOf(GHOST_PLUGIN_MENU_PRIORITY));
        putPy(map, PluginsConstants.MenuItemProperties.ON_CLICK, createPluginClickCallback(python));
        return data;
    }

    private static Object createPluginClickCallback(Object python) throws ReflectiveOperationException {
        Object globals = pyCall(pyGet(getBuiltins(python), "dict"), new Object[0]);
        putPy(pyAsMap(globals), "_callback", new PluginClickCallback());
        pyCall(pyGet(getBuiltins(python), "exec"), "def on_click(context):\n    _callback.onClick(context)\n", globals);
        return pyCallAttr(globals, "get", "on_click");
    }

    private static String addPluginMenuItem(PluginsController controller, Object pyMenuItemData) throws ReflectiveOperationException {
        Method method = PluginsController.class.getMethod("addMenuItem", String.class, getPyObjectClass());
        return (String) method.invoke(controller, GHOST_PLUGIN_ID, pyMenuItemData);
    }

    private static Object getStartedPython() {
        try {
            if (!Boolean.TRUE.equals(getPythonClass().getMethod("isStarted", new Class[0]).invoke(null, new Object[0]))) {
                return null;
            }
            return getPythonClass().getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
        } catch (Throwable e) {
            Main.log("Failed to access Python runtime: %s", e.getMessage());
            return null;
        }
    }

    private static Object getBuiltins(Object python) throws ReflectiveOperationException {
        return getPythonClass().getMethod("getBuiltins", new Class[0]).invoke(python, new Object[0]);
    }

    private static Map<Object, Object> pyAsMap(Object pyObject) throws ReflectiveOperationException {
        return (Map) getPyObjectClass().getMethod("asMap", new Class[0]).invoke(pyObject, new Object[0]);
    }

    private static Object pyGet(Object pyObject, Object key) throws ReflectiveOperationException {
        return getPyObjectClass().getMethod("get", Object.class).invoke(pyObject, key);
    }

    private static Object pyCall(Object pyObject, Object... args) throws ReflectiveOperationException {
        return getPyObjectClass().getMethod("call", Object[].class).invoke(pyObject, args);
    }

    private static Object pyCallAttr(Object pyObject, String key, Object... args) throws ReflectiveOperationException {
        return getPyObjectClass().getMethod("callAttr", String.class, Object[].class).invoke(pyObject, key, args);
    }

    private static Object pyFromJava(Object value) throws ReflectiveOperationException {
        return getPyObjectClass().getMethod("fromJava", Object.class).invoke(null, value);
    }

    private static Class<?> getPythonClass() throws ClassNotFoundException {
        if (pythonClass == null) {
            pythonClass = Class.forName("com.chaquo.python.Python");
        }
        return pythonClass;
    }

    private static Class<?> getPyObjectClass() throws ClassNotFoundException {
        if (pyObjectClass == null) {
            pyObjectClass = Class.forName("com.chaquo.python.PyObject");
        }
        return pyObjectClass;
    }

    private static void putPy(Map<Object, Object> map, String key, Object value) throws ReflectiveOperationException {
        map.put(pyFromJava(key), pyFromJava(value));
    }

    public static final class PluginClickCallback {
        public void onClick(Object contextData) {
            GhostMenuHelper.toggleGhostModeFromPlugin(contextData);
        }
    }

    private static void syncLegacyVisibility() {
        if (Settings.getAddGhostToDrawer() != Settings.getGhostInMainMenu()) {
            Settings.setAddGhostToDrawer(Settings.getGhostInMainMenu());
        }
    }

    private static int countEditorVisibleItems(List<Integer> ids) {
        int count = 0;
        for (Integer id : ids) {
            if (isEditorVisibleRawId(id)) {
                count++;
            }
        }
        return count;
    }

    private static int getRawInsertionIndex(List<Integer> ids, int editorIndex) {
        int safeIndex = Math.max(0, Math.min(editorIndex, countEditorVisibleItems(ids)));
        int visibleIndex = 0;
        for (int rawIndex = 0; rawIndex < ids.size(); rawIndex++) {
            if (isEditorVisibleRawId(ids.get(rawIndex))) {
                if (visibleIndex == safeIndex) {
                    return rawIndex;
                }
                visibleIndex++;
            }
        }
        int rawIndex2 = ids.size();
        return rawIndex2;
    }

    private static boolean isEditorVisibleRawId(Integer id) {
        if (id == null) {
            return false;
        }
        if (id.intValue() == MainMenuItem.DIVIDER.getId()) {
            return true;
        }
        return isEditorVisibleItem(id.intValue());
    }

    private static boolean isEditorVisibleItem(int id) {
        return (id != MainMenuItem.PLUGINS.getId() || PluginsController.isPluginEngineSupported()) && MainMenuItem.getById(id) != null;
    }

    private static DrawerInsertionInfo resolveDrawerInsertionInfo(int currentAccount, BaseFragment fragment) {
        ArrayList<Integer> layout = new ArrayList<>(ExteraConfig.getMainMenuLayout());
        layout.removeIf(new Predicate() { 
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GhostMenuHelper.lambda$resolveDrawerInsertionInfo$4((Integer) obj);
            }
        });
        layout.add(getRawInsertionIndex(layout, getGhostMenuIndex()), Integer.valueOf(GHOST_MENU_ITEM_ID));
        MainMenuHelper.MenuContext menuContext = MainMenuHelper.createMenuContext(currentAccount, fragment);
        boolean hasVisibleItems = false;
        boolean pendingDivider = false;
        int childIndex = 0;
        Iterator<Integer> it = layout.iterator();
        while (true) {
            boolean z = true;
            if (it.hasNext()) {
                Integer id = it.next();
                if (id != null) {
                    if (id.intValue() == MainMenuItem.DIVIDER.getId()) {
                        if (hasVisibleItems) {
                            pendingDivider = true;
                        }
                    } else {
                        if (id.intValue() == 910001) {
                            return new DrawerInsertionInfo(z, childIndex, pendingDivider);
                        }
                        List<MainMenuHelper.MenuItemInfo> items = MainMenuHelper.resolveDrawerMenuItems(id.intValue(), menuContext);
                        if (items != null && !items.isEmpty()) {
                            if (pendingDivider) {
                                childIndex++;
                                pendingDivider = false;
                            }
                            childIndex += items.size();
                            hasVisibleItems = true;
                        }
                    }
                }
            } else {
                return new DrawerInsertionInfo(z, childIndex, pendingDivider);
            }
        }
    }

    static /* synthetic */ boolean lambda$resolveDrawerInsertionInfo$4(Integer id) {
        return id != null && id.intValue() == 910001;
    }

    private static String getDrawerTitle() {
        return Settings.getGhostModeEnabledGlobal() ? Localization.GHOST_MODE_DISABLE : Localization.GHOST_MODE_ENABLE;
    }

    private static LinearLayout getDrawerContainer(DrawerMenuView drawerMenuView) {
        try {
            if (drawerContainerField == null) {
                drawerContainerField = DrawerMenuView.class.getDeclaredField("container");
                drawerContainerField.setAccessible(true);
            }
            return (LinearLayout) drawerContainerField.get(drawerMenuView);
        } catch (Exception e) {
            Main.log("Failed to access drawer container: %s", e.getMessage());
            return null;
        }
    }

    private static Runnable getDrawerOnItemClick(DrawerMenuView drawerMenuView) {
        try {
            if (drawerOnItemClickField == null) {
                drawerOnItemClickField = DrawerMenuView.class.getDeclaredField("onItemClick");
                drawerOnItemClickField.setAccessible(true);
            }
            return (Runnable) drawerOnItemClickField.get(drawerMenuView);
        } catch (Exception e) {
            Main.log("Failed to access drawer item callback: %s", e.getMessage());
            return null;
        }
    }

    private static View createDividerView(Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        divider.setLayoutParams(LayoutHelper.createLinear(-1, DIVIDER_HEIGHT_DP, 87, 12, DIVIDER_VERTICAL_MARGIN_DP, 12, DIVIDER_VERTICAL_MARGIN_DP));
        return divider;
    }

    private static final class DrawerInsertionInfo {
        private final boolean addDivider;
        private final int childIndex;
        private final boolean shouldInsert;

        private DrawerInsertionInfo(boolean shouldInsert, int childIndex, boolean addDivider) {
            this.shouldInsert = shouldInsert;
            this.childIndex = childIndex;
            this.addDivider = addDivider;
        }
    }
}
