package ni.shikatu.re_extera.hooks.mainmenu;

import android.view.View;
import com.exteragram.messenger.ExteraConfig;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalRecyclerView;

public class MainMenuPreferencesActivityHook {
    public static final int GHOST_MODE_ID = 150;

    public static void init() {
        try {
            Class<?> activityClass = Class.forName("com.exteragram.messenger.preferences.appearance.MainMenuPreferencesActivity");
            Class<?> baseActivityClass = Class.forName("com.exteragram.messenger.preferences.BasePreferencesActivity");
            final Class<?> itemInfoClass = Class.forName("com.exteragram.messenger.preferences.appearance.MainMenuPreferencesActivity$ItemInfo");
            final Field itemDetailsField = activityClass.getDeclaredField("itemDetails");
            itemDetailsField.setAccessible(true);
            final Field reorderIconField = activityClass.getDeclaredField("reorderIcon");
            reorderIconField.setAccessible(true);
            final Field iconResField = itemInfoClass.getDeclaredField("iconRes");
            iconResField.setAccessible(true);
            final Field nameField = itemInfoClass.getDeclaredField("name");
            nameField.setAccessible(true);
            final Field listViewField = baseActivityClass.getDeclaredField("listView");
            listViewField.setAccessible(true);
            final Method updateResetButtonVisibilityMethod = activityClass.getDeclaredMethod("updateResetButtonVisibility", new Class[0]);
            updateResetButtonVisibilityMethod.setAccessible(true);
            XposedBridge.hookMethod(activityClass.getDeclaredMethod("initItemDetails", new Class[0]), new XC_MethodHook() { // from class: ni.shikatu.re_extera.hooks.mainmenu.MainMenuPreferencesActivityHook.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    HashMap<Integer, Object> itemDetails = (HashMap) itemDetailsField.get(param.thisObject);
                    Constructor<?> constructor = itemInfoClass.getDeclaredConstructor(CharSequence.class, Integer.TYPE);
                    constructor.setAccessible(true);
                    Object ghostInfo = constructor.newInstance(Localization.GHOST_MODE, Integer.valueOf(R.drawable.ghost));
                    itemDetails.put(150, ghostInfo);
                }
            });
            XposedBridge.hookMethod(activityClass.getDeclaredMethod("createMenuItem", Integer.TYPE, itemInfoClass), new XC_MethodHook() { // from class: ni.shikatu.re_extera.hooks.mainmenu.MainMenuPreferencesActivityHook.2
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    int id = ((Integer) param.args[0]).intValue();
                    if (id == 150) {
                        Object info = param.args[1];
                        UItem item = UItem.asButton(id, iconResField.getInt(info), (CharSequence) nameField.get(info));
                        item.object2 = reorderIconField.get(param.thisObject);
                        param.setResult(item);
                    }
                }
            });
            XposedBridge.hookMethod(activityClass.getDeclaredMethod("updateConfigFromReorder", Integer.TYPE, ArrayList.class), new XC_MethodHook() { // from class: ni.shikatu.re_extera.hooks.mainmenu.MainMenuPreferencesActivityHook.3
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    MainMenuPreferencesActivityHook.saveGhostPosition();
                }
            });
            XposedBridge.hookMethod(activityClass.getDeclaredMethod("onClick", UItem.class, View.class, Integer.TYPE, Float.TYPE, Float.TYPE), new XC_MethodHook() { // from class: ni.shikatu.re_extera.hooks.mainmenu.MainMenuPreferencesActivityHook.4
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    UItem item = (UItem) param.args[0];
                    if (item.id == 150) {
                        if (ExteraConfig.mainMenuLayout.contains(150)) {
                            ExteraConfig.mainMenuLayout.remove((Object) 150);
                            if (!ExteraConfig.mainMenuHiddenItems.contains(150)) {
                                ExteraConfig.mainMenuHiddenItems.add(0, 150);
                            }
                        } else if (ExteraConfig.mainMenuHiddenItems.contains(150)) {
                            ExteraConfig.mainMenuHiddenItems.remove((Object) 150);
                            ExteraConfig.mainMenuLayout.add(150);
                        }
                        MainMenuPreferencesActivityHook.saveGhostPosition();
                        ExteraConfig.saveMainMenuLayout();
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
                        UniversalRecyclerView listView = (UniversalRecyclerView) listViewField.get(param.thisObject);
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                        updateResetButtonVisibilityMethod.invoke(param.thisObject, new Object[0]);
                        param.setResult((Object) null);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void saveGhostPosition() {
        int layoutIdx = ExteraConfig.mainMenuLayout.indexOf(150);
        if (layoutIdx != -1) {
            Settings.setGhostInMainMenu(true);
            Settings.setGhostMenuIndex(layoutIdx);
        } else {
            int hiddenIdx = ExteraConfig.mainMenuHiddenItems.indexOf(150);
            if (hiddenIdx != -1) {
                Settings.setGhostInMainMenu(false);
                Settings.setGhostMenuIndex(hiddenIdx);
            }
        }
        Settings.setGhostPositionInitialized(true);
    }
}
