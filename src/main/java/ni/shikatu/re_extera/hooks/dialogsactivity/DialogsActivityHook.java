package ni.shikatu.re_extera.hooks.dialogsactivity;

import com.exteragram.messenger.ExteraConfig;
import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.GhostMenuHelper;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ItemOptions;

public class DialogsActivityHook extends XC_MethodHook {
    private final Mode mode;

    public enum Mode {
        ADD_ITEMS,
        ADD_ITEM
    }

    public DialogsActivityHook(Mode mode) {
        this.mode = mode;
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$ni$shikatu$re_extera$hooks$dialogsactivity$DialogsActivityHook$Mode = new int[Mode.values().length];

        static {
            try {
                $SwitchMap$ni$shikatu$re_extera$hooks$dialogsactivity$DialogsActivityHook$Mode[Mode.ADD_ITEMS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$hooks$dialogsactivity$DialogsActivityHook$Mode[Mode.ADD_ITEM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        switch (AnonymousClass1.$SwitchMap$ni$shikatu$re_extera$hooks$dialogsactivity$DialogsActivityHook$Mode[this.mode.ordinal()]) {
            case Defaults.ALWAYS /* 1 */:
                onBeforeAddItems(param);
                break;
            case 2:
                onBeforeAddItem(param);
                break;
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (this.mode == Mode.ADD_ITEMS) {
            GhostMenuHelper.scrubGhostFromConfig();
            // Append "Close app" item at the bottom of the Settings long-press popup
            if (param.args != null && param.args.length >= 1 && param.args[0] instanceof ItemOptions) {
                ItemOptions io = (ItemOptions) param.args[0];
                io.addGap();
                io.add(R.drawable.msg_cancel, Localization.CLOSE_APP, () -> {
                    android.os.Process.killProcess(android.os.Process.myPid());
                });
            }
        }
    }

    private void onBeforeAddItems(XC_MethodHook.MethodHookParam param) {
        if (param != null && param.args != null && param.args.length == 3 && param.args[2] instanceof java.util.function.IntPredicate) {
            final java.util.function.IntPredicate original = (java.util.function.IntPredicate) param.args[2];
            param.args[2] = new java.util.function.IntPredicate() {
                @Override
                public boolean test(int value) {
                    if (value == 910001) return false;
                    if (value == 910002) return false;
                    return original != null && original.test(value);
                }
            };
        }
        if (!GhostMenuHelper.isGhostMenuVisible()) {
            Main.log("DialogsActivityHook: Not adding ghost menu items. isGhostMenuVisible=%s", GhostMenuHelper.isGhostMenuVisible());
            GhostMenuHelper.scrubGhostFromConfig();
            return;
        }
        ArrayList<Integer> layoutWithGhost = GhostMenuHelper.withGhostMenuItem(ExteraConfig.getMainMenuLayout(), true);
        Main.log("DialogsActivityHook: onBeforeAddItems layoutWithGhost contains 910001: %s", layoutWithGhost.contains(910001));
        ExteraConfig.getMainMenuLayout().clear();
        ExteraConfig.getMainMenuLayout().addAll(layoutWithGhost);
        ExteraConfig.getMainMenuHiddenItems().removeIf(new Predicate<Integer>() { 
            @Override // java.util.function.Predicate
            public final boolean test(Integer obj) {
                return DialogsActivityHook.lambda$onBeforeAddItems$0(obj);
            }
        });
    }

    static /* synthetic */ boolean lambda$onBeforeAddItems$0(Integer id) {
        return id != null && (id.intValue() == 910001 || id.intValue() == 910002);
    }

    private void onBeforeAddItem(final XC_MethodHook.MethodHookParam param) {
        
        int id = -1;
        BaseFragment fragment = null;

        if (param.args.length == 2 && param.args[1] instanceof Integer) {
            // DialogsActivity.addMainMenuConfiguredItem(ItemOptions, int)
            id = ((Integer) param.args[1]).intValue();
            if (param.thisObject instanceof BaseFragment) {
                fragment = (BaseFragment) param.thisObject;
            }
        } else if (param.args.length == 3 && param.args[2] instanceof Integer) {
            // MainMenuHelper.addConfiguredItemOption(ItemOptions, MenuContext, int)
            id = ((Integer) param.args[2]).intValue();
            Object menuContext = param.args[1];
            if (menuContext != null) {
                try {
                    Method fragmentMethod = menuContext.getClass().getMethod("fragment");
                    fragment = (BaseFragment) fragmentMethod.invoke(menuContext);
                } catch (Exception e) {
                    Main.log("Failed to extract fragment from MenuContext: " + e.getMessage());
                }
            }
        }
        
        Main.log("DialogsActivityHook: onBeforeAddItem id=%d", id);
        
        if (id != 910001) {
            return;
        }
        
        final ItemOptions io = (ItemOptions) param.args[0];
        boolean enabled = Settings.getGhostModeEnabledGlobal();
        Main.log("DialogsActivityHook: onBeforeAddItem id 910001! enabled=%s", enabled);
        final BaseFragment finalFragment = fragment;
        
        io.add(R.drawable.ghost, enabled ? Localization.GHOST_MODE_DISABLE : Localization.GHOST_MODE_ENABLE, new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                DialogsActivityHook.lambda$onBeforeAddItem$1(io, finalFragment);
            }
        });
        param.setResult(true);
    }

    static /* synthetic */ void lambda$onBeforeAddItem$1(ItemOptions io, BaseFragment fragment) {
        io.dismiss();
        GhostMenuHelper.toggleGhostMode(fragment);
    }
}

