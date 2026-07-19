package ni.shikatu.re_extera;

import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.exteragram.messenger.preferences.utils.SettingsRegistry;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.HookInit;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.newui.AdditionalFragment;
import ni.shikatu.re_extera.settings.newui.DeletedAndEditedMessagesFragment;
import ni.shikatu.re_extera.settings.newui.GhostFragment;
import ni.shikatu.re_extera.settings.newui.SettingsFragmentNew;
import ni.shikatu.re_extera.ui.RegexFiltersFragment;
import ni.shikatu.re_extera.ui.ShadowbanFragment;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

public final class Main {
    private static final String LOG_TAG = "re:extera";
    public static final String VERSION = "1.7";
    public static final int VERSION_CODE = 12;
    public static HookInit hooks;
    private static final Method initiateFragmentMethod;
    private static volatile Main instance;
    public static final List<Class<? extends BaseFragment>> fragments = java.util.Arrays.asList(AdditionalFragment.class, SettingsFragmentNew.class, DeletedAndEditedMessagesFragment.class, GhostFragment.class, RegexFiltersFragment.class, ShadowbanFragment.class);
    public static final Set<TLObject> ignoredRequests = Collections.newSetFromMap(new ConcurrentHashMap());

    static {
        Method m = null;
        try {
            m = SettingsRegistry.class.getDeclaredMethod("initiateFragment", Class.class);
        } catch (NoSuchMethodException e) {
            ReflectionUtils.hookError();
            log("initiateFragment method not found.", new Object[0]);
        }
        initiateFragmentMethod = m;
    }

    private Main() {
    }

    public static synchronized Main getInstance() {
        if (instance == null) {
            instance = new Main();
        }
        return instance;
    }

    public static Context getApplicationContext() {
        return LaunchActivity.instance.getApplicationContext();
    }

    public void start() {
        log("re:extera init", new Object[0]);
        Context context = getApplicationContext();
        ReExteraDb.init(context);
        ShadowbanCache.init();
        try {
            MeasureTime.deletedIcon = ContextCompat.getDrawable(context, R.drawable.msg_delete_filled);
        } catch (Throwable t) {
            MeasureTime.deletedIcon = null;
        }
        if (MeasureTime.deletedIcon == null) {
            MeasureTime.deletedIcon = ContextCompat.getDrawable(context, R.drawable.msg_delete);
        }
        if (MeasureTime.deletedIcon != null) {
            MeasureTime.deletedIcon = MeasureTime.deletedIcon.mutate();
            MeasureTime.deletedIcon.setBounds(0, 0, MeasureTime.deletedIcon.getIntrinsicWidth(), MeasureTime.deletedIcon.getIntrinsicHeight());
        }
        Localization.updateStrings();
        hooks = new HookInit();
        hooks.init();
        initFragments();
    }

    public void onUnload() {
        if (hooks != null) {
            hooks.onUnload();
        }
    }

    public void showSettings() {
        LaunchActivity.instance.presentFragment(new SettingsFragmentNew());
    }

    public static void showSettingsExternal() {
        if (instance != null) {
            instance.showSettings();
        }
    }

    public static void initAndStart() {
        if (hooks != null) {
            Main.log("Already initialized, skipping", new Object[0]);
            return;
        }
        Main.log("initAndStart", new Object[0]);
        getInstance().start();
    }

    public static void addIgnoredRequest(TLObject request) {
        if (request != null) {
            ignoredRequests.add(request);
        }
    }

    public static void log(String message, Object... args) {
        String formatted = args.length == 0 ? message : String.format(message, args);
        Log.d(LOG_TAG, formatted);
        FileLog.d("[re:extera] " + formatted);
    }

    private static void initFragments() {
        if (initiateFragmentMethod == null) {
            return;
        }
        SettingsRegistry registry = SettingsRegistry.getInstance();
        for (Class<? extends BaseFragment> fragment : fragments) {
            ReflectionUtils.invoke(initiateFragmentMethod, registry, fragment);
        }
    }
}
