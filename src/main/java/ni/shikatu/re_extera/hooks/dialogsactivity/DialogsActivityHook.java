package ni.shikatu.re_extera.hooks.dialogsactivity;

import com.exteragram.messenger.ExteraConfig;
import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.function.Predicate;
import ni.shikatu.re_extera.Defaults;
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

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        switch (this.mode.ordinal()) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                onBeforeAddItems();
                break;
            case Defaults.ALWAYS /* 1 */:
                onBeforeAddItem(param);
                break;
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (this.mode == Mode.ADD_ITEMS) {
            GhostMenuHelper.scrubGhostFromConfig();
        }
    }

    private void onBeforeAddItems() {
        if (ExteraConfig.navigationDrawer || !GhostMenuHelper.isGhostMenuVisible()) {
            GhostMenuHelper.scrubGhostFromConfig();
            return;
        }
        ArrayList<Integer> layoutWithGhost = GhostMenuHelper.withGhostMenuItem(ExteraConfig.mainMenuLayout, true);
        ExteraConfig.mainMenuLayout.clear();
        ExteraConfig.mainMenuLayout.addAll(layoutWithGhost);
        ExteraConfig.mainMenuHiddenItems.removeIf(new Predicate() { // from class: ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DialogsActivityHook.lambda$onBeforeAddItems$0((Integer) obj);
            }
        });
    }

    static /* synthetic */ boolean lambda$onBeforeAddItems$0(Integer id) {
        return id != null && id.intValue() == 910001;
    }

    private void onBeforeAddItem(final XC_MethodHook.MethodHookParam param) {
        if (ExteraConfig.navigationDrawer) {
            return;
        }
        int id = ((Integer) param.args[1]).intValue();
        if (id != 910001) {
            return;
        }
        final ItemOptions io = (ItemOptions) param.args[0];
        boolean enabled = Settings.getGhostModeEnabledGlobal();
        io.add(R.drawable.ghost, enabled ? Localization.GHOST_MODE_DISABLE : Localization.GHOST_MODE_ENABLE, new Runnable() { // from class: ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DialogsActivityHook.lambda$onBeforeAddItem$1(io, param);
            }
        });
        param.setResult(true);
    }

    static /* synthetic */ void lambda$onBeforeAddItem$1(ItemOptions io, XC_MethodHook.MethodHookParam param) {
        io.dismiss();
        GhostMenuHelper.toggleGhostMode((BaseFragment) param.thisObject);
    }
}
