package ni.shikatu.re_extera.hooks.dialogsactivity;

import com.exteragram.messenger.ExteraConfig;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.DialogsActivity;

public class DialogsActivityHook {
    public static final int GHOST_MODE_ID = 150;

    /* JADX INFO: renamed from: ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook$1, reason: invalid class name */
    class AnonymousClass1 extends XC_MethodHook {
        AnonymousClass1() {
        }

        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
            int id = ((Integer) param.args[1]).intValue();
            if (id == 150) {
                ItemOptions io = (ItemOptions) param.args[0];
                final boolean enabled = Settings.getGhostModeEnabledGlobal();
                io.add(R.drawable.ghost, enabled ? Localization.GHOST_MODE_DISABLE : Localization.GHOST_MODE_ENABLE, new Runnable() { // from class: ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        DialogsActivityHook.AnonymousClass1.lambda$beforeHookedMethod$0(enabled, param);
                    }
                });
                param.setResult(true);
            }
        }

        static /* synthetic */ void lambda$beforeHookedMethod$0(boolean enabled, XC_MethodHook.MethodHookParam param) {
            Settings.setGhostModeEnabledGlobal(!enabled);
            try {
                String message = enabled ? Localization.GHOST_MODE_DISABLED : Localization.GHOST_MODE_ENABLED;
                BulletinFactory.of((BaseFragment) param.thisObject).createEmojiBulletin(enabled ? "❌" : "✅", message).show();
            } catch (Exception e) {
            }
        }
    }

    public static void init() {
        try {
            XposedBridge.hookMethod(DialogsActivity.class.getDeclaredMethod("addMainMenuConfiguredItem", ItemOptions.class, Integer.TYPE), new AnonymousClass1());
            XposedBridge.hookMethod(ExteraConfig.class.getDeclaredMethod("sanitizeMenu", new Class[0]), new XC_MethodHook() { // from class: ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook.2
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    DialogsActivityHook.restoreGhostMenuPosition();
                }
            });
            restoreGhostMenuPosition();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void restoreGhostMenuPosition() {
        try {
            if (!ExteraConfig.mainMenuLayout.contains(150) && !ExteraConfig.mainMenuHiddenItems.contains(150)) {
                if (Settings.isGhostPositionInitialized()) {
                    boolean inMain = Settings.getGhostInMainMenu();
                    int savedIdx = Settings.getGhostMenuIndex();
                    if (inMain) {
                        int idx = Math.min(savedIdx, ExteraConfig.mainMenuLayout.size());
                        ExteraConfig.mainMenuLayout.add(idx, 150);
                    } else {
                        int idx2 = Math.min(savedIdx, ExteraConfig.mainMenuHiddenItems.size());
                        ExteraConfig.mainMenuHiddenItems.add(idx2, 150);
                    }
                } else {
                    ExteraConfig.mainMenuLayout.add(150);
                }
                ExteraConfig.saveMainMenuLayout();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
