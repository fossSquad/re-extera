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
                onBeforeAddItems();
                break;
            case 2:
                onBeforeAddItem(param);
                break;
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (this.mode == Mode.ADD_ITEMS) {
            GhostMenuHelper.scrubGhostFromConfig();
        }
    }

    private void onBeforeAddItems() {
        if (ExteraConfig.getNavigationDrawer() || !GhostMenuHelper.isGhostMenuVisible()) {
            GhostMenuHelper.scrubGhostFromConfig();
            return;
        }
        ArrayList<Integer> layoutWithGhost = GhostMenuHelper.withGhostMenuItem(ExteraConfig.getMainMenuLayout(), true);
        ExteraConfig.getMainMenuLayout().clear();
        ExteraConfig.getMainMenuLayout().addAll(layoutWithGhost);
        ExteraConfig.getMainMenuHiddenItems().removeIf(new Predicate() { // from class: ni.shikatu.re_extera.hooks.dialogsactivity.DialogsActivityHook$$ExternalSyntheticLambda0
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
        if (ExteraConfig.getNavigationDrawer()) {
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
