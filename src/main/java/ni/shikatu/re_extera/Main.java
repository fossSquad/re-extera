package ni.shikatu.re_extera;

import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.HookInit;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.newui.SettingsFragmentNew;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

public class Main {
    public static Context applicationContext;
    public static HookInit hooks;
    public static Main instance = null;
    public static String VERSION = "1.2.3";

    public static Context getApplicationContext() {
        return applicationContext != null ? applicationContext : LaunchActivity.instance.getApplicationContext();
    }

    public void start() {
        Context context = getApplicationContext();
        ReExteraDb.init(context);
        MeasureTime.deletedIcon = ContextCompat.getDrawable(context, R.drawable.msg_delete_filled);
        Localization.updateStrings();
        hooks = new HookInit();
        hooks.init();
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
        applicationContext = getApplicationContext();
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

    public void onUnload() {
        if (hooks != null) {
            hooks.onUnload();
        }
    }
}
