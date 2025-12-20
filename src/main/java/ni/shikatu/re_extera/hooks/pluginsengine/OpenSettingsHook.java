package ni.shikatu.re_extera.hooks.pluginsengine;

import com.exteragram.messenger.plugins.Plugin;
import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.newui.SettingsFragmentNew;
import org.telegram.ui.ActionBar.BaseFragment;

public class OpenSettingsHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Plugin plugin = (Plugin) param.args[0];
        BaseFragment fragment = (BaseFragment) param.args[1];
        if (plugin == null) {
            return;
        }
        if (OpenSettingsHook$$ExternalSyntheticBackport0.m(plugin.getId(), "re_extera_dex") || OpenSettingsHook$$ExternalSyntheticBackport0.m(plugin.getId(), "re_extera_dex_local_debug") || OpenSettingsHook$$ExternalSyntheticBackport0.m(plugin.getId(), "re_extera_dex_unstable")) {
            fragment.presentFragment(new SettingsFragmentNew());
            param.setResult((Object) null);
        }
    }
}
