package ni.shikatu.re_extera.hooks.pluginsengine;

import com.exteragram.messenger.plugins.Plugin;
import de.robv.android.xposed.XC_MethodHook;
import java.util.Set;
import java.util.function.Predicate;
import ni.shikatu.re_extera.settings.newui.SettingsFragmentNew;
import org.telegram.ui.ActionBar.BaseFragment;

public class OpenSettingsHook extends XC_MethodHook {
    private static final Set<String> RE_EXTERA_PLUGIN_IDS = OpenSettingsHook$$ExternalSyntheticBackport0.m(new Object[]{"re_extera_dex", "re_extera_dex_local_debug", "re_extera_dex_unstable"});

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        final Plugin plugin = (Plugin) param.args[0];
        BaseFragment fragment = (BaseFragment) param.args[1];
        if (plugin != null && fragment != null && RE_EXTERA_PLUGIN_IDS.stream().anyMatch(new Predicate() { // from class: ni.shikatu.re_extera.hooks.pluginsengine.OpenSettingsHook$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OpenSettingsHook$$ExternalSyntheticBackport2.m(plugin.getId(), (String) obj);
            }
        })) {
            fragment.presentFragment(new SettingsFragmentNew());
            param.setResult((Object) null);
        }
    }
}
