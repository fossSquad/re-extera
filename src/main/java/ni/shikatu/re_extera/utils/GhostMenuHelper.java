package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.drawer.DrawerMenuItemView;
import com.exteragram.messenger.drawer.DrawerMenuView;
import com.exteragram.messenger.plugins.PluginsController;
import com.exteragram.messenger.utils.chats.MainMenuHelper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

public final class GhostMenuHelper {
    private static final float DIVIDER_HEIGHT_DP = 1.0f / AndroidUtilities.density;
    private static final int DIVIDER_HORIZONTAL_MARGIN_DP = 12;
    private static final int DIVIDER_VERTICAL_MARGIN_DP = 8;
    public static final int GHOST_MENU_ITEM_ID = 910001;
    private static Field drawerContainerField;
    private static Field drawerOnItemClickField;

    private GhostMenuHelper() {
    }

    public static void ensureInitialized() {
        if (!Settings.isGhostPositionInitialized()) {
            Settings.setGhostInMainMenu(Settings.getAddGhostToDrawer());
            Settings.setGhostMenuIndex(countEditorVisibleItems(ExteraConfig.mainMenuLayout));
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
            Settings.setGhostMenuIndex(countEditorVisibleItems(ExteraConfig.mainMenuLayout));
        }
        Settings.setGhostInMainMenu(visible);
        Settings.setAddGhostToDrawer(visible);
        clampGhostMenuIndex();
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
        int max = countEditorVisibleItems(ExteraConfig.mainMenuLayout);
        int current = Settings.getGhostMenuIndex();
        int clamped = Math.max(0, Math.min(current, max));
        if (current != clamped) {
            Settings.setGhostMenuIndex(clamped);
        }
    }

    public static ArrayList<Integer> withGhostMenuItem(List<Integer> sourceIds, boolean mainSection) {
        ArrayList<Integer> ids = new ArrayList<>(sourceIds);
        ids.removeIf(new Predicate() { // from class: ni.shikatu.re_extera.utils.GhostMenuHelper$$ExternalSyntheticLambda1
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
        for (Integer id : ExteraConfig.mainMenuLayout) {
            if (id != null) {
                if (id.intValue() == ExteraConfig.MainMenuItem.DIVIDER.id) {
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
        boolean changed = ExteraConfig.mainMenuLayout.removeIf(new Predicate() { // from class: ni.shikatu.re_extera.utils.GhostMenuHelper$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GhostMenuHelper.lambda$scrubGhostFromConfig$1((Integer) obj);
            }
        });
        return changed | ExteraConfig.mainMenuHiddenItems.removeIf(new Predicate() { // from class: ni.shikatu.re_extera.utils.GhostMenuHelper$$ExternalSyntheticLambda3
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
        if (!isGhostMenuVisible() || (container = getDrawerContainer(drawerMenuView)) == null) {
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
        itemView.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.GhostMenuHelper$$ExternalSyntheticLambda0
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
        if (fragment != null) {
            BulletinFactory.of(fragment).createSuccessBulletin(enabled ? Localization.GHOST_MODE_ENABLED : Localization.GHOST_MODE_DISABLED).show();
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
        if (id.intValue() == ExteraConfig.MainMenuItem.DIVIDER.id) {
            return true;
        }
        return isEditorVisibleItem(id.intValue());
    }

    private static boolean isEditorVisibleItem(int id) {
        return (id != ExteraConfig.MainMenuItem.PLUGINS.id || PluginsController.isPluginEngineSupported()) && ExteraConfig.MainMenuItem.getById(id) != null;
    }

    private static DrawerInsertionInfo resolveDrawerInsertionInfo(int currentAccount, BaseFragment fragment) {
        ArrayList<Integer> layout = new ArrayList<>(ExteraConfig.mainMenuLayout);
        layout.removeIf(new Predicate() { // from class: ni.shikatu.re_extera.utils.GhostMenuHelper$$ExternalSyntheticLambda4
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
                    if (id.intValue() == ExteraConfig.MainMenuItem.DIVIDER.id) {
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
        divider.setLayoutParams(LayoutHelper.createLinear(-1, DIVIDER_HEIGHT_DP, 87, DIVIDER_HORIZONTAL_MARGIN_DP, 8, DIVIDER_HORIZONTAL_MARGIN_DP, 8));
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
