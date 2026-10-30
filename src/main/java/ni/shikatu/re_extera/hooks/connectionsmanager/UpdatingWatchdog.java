package ni.shikatu.re_extera.hooks.connectionsmanager;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.AccountUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;

public class UpdatingWatchdog extends XC_MethodHook {
    private static final long TIMEOUT_MS = 20_000L;
    private static final ConcurrentHashMap<Integer, Runnable> timeoutRunnables = new ConcurrentHashMap<>();
    private static Field gettingDifferenceField = null;

    static {
        try {
            gettingDifferenceField = MessagesController.class.getDeclaredField("gettingDifference");
            gettingDifferenceField.setAccessible(true);
        } catch (Throwable t) {
            Main.log("UpdatingWatchdog: gettingDifference field not found: %s", t.getMessage());
        }
    }

    @Override
    public void afterHookedMethod(MethodHookParam param) {
        try {
            boolean isUpdating = (Boolean) param.args[0];
            int account = AccountUtils.getCurrentAccount(param.thisObject);
            final ConnectionsManager cm = (param.thisObject instanceof ConnectionsManager)
                    ? (ConnectionsManager) param.thisObject
                    : ConnectionsManager.getInstance(account);

            Runnable prev = timeoutRunnables.remove(account);
            if (prev != null) {
                AndroidUtilities.cancelRunOnUIThread(prev);
            }

            if (isUpdating && cm != null) {
                Runnable watchdog = new Runnable() {
                    @Override
                    public void run() {
                        timeoutRunnables.remove(account);
                        try {
                            if (cm.getConnectionState() == ConnectionsManager.ConnectionStateUpdating) {
                                Main.log("UpdatingWatchdog: stuck in updating state for >%dms on account %d, recovering", TIMEOUT_MS, account);
                                MessagesController mc = MessagesController.getInstance(account);
                                if (mc != null && gettingDifferenceField != null) {
                                    try {
                                        gettingDifferenceField.setBoolean(mc, false);
                                    } catch (Throwable ignored) {
                                    }
                                }
                                cm.setIsUpdating(false);
                            }
                        } catch (Throwable t) {
                            Main.log("UpdatingWatchdog error: %s", t.getMessage());
                        }
                    }
                };
                timeoutRunnables.put(account, watchdog);
                AndroidUtilities.runOnUIThread(watchdog, TIMEOUT_MS);
            }
        } catch (Throwable t) {
            Main.log("UpdatingWatchdog.afterHookedMethod error: %s", t.getMessage());
        }
    }
}
