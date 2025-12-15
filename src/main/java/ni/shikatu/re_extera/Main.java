package ni.shikatu.re_extera;

import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.exteragram.messenger.preferences.utils.SettingsRegistry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.HookInit;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.newui.AdditionalFragment;
import ni.shikatu.re_extera.settings.newui.DeletedAndEditedMessagesFragment;
import ni.shikatu.re_extera.settings.newui.GhostFragment;
import ni.shikatu.re_extera.settings.newui.SettingsFragmentNew;
import ni.shikatu.re_extera.ui.RegexFiltersFragment;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

public class Main {
    public static final String VERSION = "1.3.1b";
    public static final int VERSION_CODE = 5;
    public static HookInit hooks;
    private static Method initiateFragment;
    public static Main instance = null;
    public static List<Long> blocked = Main$$ExternalSyntheticBackport0.m(new Object[]{6204645839L});
    public static List<Class<? extends BaseFragment>> fragments = Main$$ExternalSyntheticBackport0.m(new Object[]{AdditionalFragment.class, SettingsFragmentNew.class, DeletedAndEditedMessagesFragment.class, GhostFragment.class, RegexFiltersFragment.class});

    static {
        try {
            initiateFragment = SettingsRegistry.class.getDeclaredMethod("initiateFragment", Class.class);
        } catch (NoSuchMethodException e) {
            log("initiateFragment method not found.", new Object[0]);
        }
    }

    public static Context getApplicationContext() {
        return LaunchActivity.instance.getApplicationContext();
    }

    public void start() {
        Context context = getApplicationContext();
        ReExteraDb.init(context);
        MeasureTime.deletedIcon = ContextCompat.getDrawable(context, R.drawable.msg_delete_filled);
        Localization.updateStrings();
        hooks = new HookInit();
        hooks.init();
        initFragments();
        checkBlocked();
    }

    private static void initFragments() {
        if (initiateFragment == null) {
            return;
        }
        final SettingsRegistry instance2 = SettingsRegistry.getInstance();
        fragments.forEach(new Consumer() { // from class: ni.shikatu.re_extera.Main$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ReflectionUtils.invoke(Main.initiateFragment, instance2, (Class) obj);
            }
        });
    }

    public static void requestSendWithUnhook(Runnable request) {
        try {
            hooks.sendRequestHook.unhook();
            request.run();
            hooks.startSendRequestHook();
        } catch (Exception e) {
        }
    }

    private Main() {
    }

    public static synchronized Main getInstance() {
        if (instance == null) {
            instance = new Main();
        }
        return instance;
    }

    public static void log(String log, Object... args) {
        Log.d("re:extera", String.format(log, args));
        FileLog.d(String.format("[re:extera] %s", String.format(log, args)));
    }

    public void showSettings() {
        SettingsFragmentNew settingsFragment = new SettingsFragmentNew();
        LaunchActivity.instance.presentFragment(settingsFragment);
    }

    public static void checkBlocked() {
    }

    public void onUnload() {
        if (hooks != null) {
            hooks.onUnload();
        }
    }
}
