package ni.shikatu.re_extera.hooks.connectionsmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

/**
 * Detects a connection that is stuck in the "Connecting" state (seen on some
 * OEM ROMs, e.g. ColorOS) and nudges the native connection handler to retry.
 */
public class ConnectionWatchdog implements NotificationCenter.NotificationCenterDelegate {
    private static final long TIMEOUT_MS = 12_000L;
    private static final int MAX_ATTEMPTS = 3;

    private final Map<Integer, Runnable> scheduled = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> attempts = new ConcurrentHashMap<>();
    private boolean registered;

    public void init() {
        if (registered) {
            return;
        }
        try {
            for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
                if (!UserConfig.getInstance(account).isClientActivated()) {
                    continue;
                }
                try {
                    NotificationCenter.getInstance(account).addObserver(this, NotificationCenter.didUpdateConnectionState);
                } catch (Throwable t) {
                    Main.log("ConnectionWatchdog: observer %d failed: %s", account, t.getMessage());
                }
            }
            registered = true;
            schedule(UserConfig.selectedAccount);
        } catch (Throwable t) {
            Main.log("ConnectionWatchdog.init error: %s", t.getMessage());
        }
    }

    public void stop() {
        if (!registered) {
            return;
        }
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (!UserConfig.getInstance(account).isClientActivated()) {
                continue;
            }
            try {
                NotificationCenter.getInstance(account).removeObserver(this, NotificationCenter.didUpdateConnectionState);
            } catch (Throwable ignored) {
            }
        }
        registered = false;
        for (Runnable runnable : scheduled.values()) {
            AndroidUtilities.cancelRunOnUIThread(runnable);
        }
        scheduled.clear();
        attempts.clear();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id != NotificationCenter.didUpdateConnectionState) {
            return;
        }
        try {
            ConnectionsManager manager = ConnectionsManager.getInstance(account);
            if (manager == null) {
                return;
            }
            int state = manager.getConnectionState();
            if (isConnecting(state)) {
                schedule(account);
            } else {
                cancel(account);
            }
        } catch (Throwable t) {
            Main.log("ConnectionWatchdog: state check failed: %s", t.getMessage());
        }
    }

    private static boolean isConnecting(int state) {
        return state == ConnectionsManager.ConnectionStateConnecting
                || state == ConnectionsManager.ConnectionStateConnectingToProxy;
    }

    private void schedule(final int account) {
        if (scheduled.containsKey(account)) {
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                scheduled.remove(account);
                onTimeout(account);
            }
        };
        scheduled.put(account, runnable);
        AndroidUtilities.runOnUIThread(runnable, TIMEOUT_MS);
    }

    private void cancel(int account) {
        Runnable runnable = scheduled.remove(account);
        attempts.remove(account);
        if (runnable != null) {
            AndroidUtilities.cancelRunOnUIThread(runnable);
        }
    }

    private void onTimeout(int account) {
        try {
            ConnectionsManager manager = ConnectionsManager.getInstance(account);
            if (manager == null || !isConnecting(manager.getConnectionState())) {
                attempts.remove(account);
                return;
            }
            int attempt = attempts.containsKey(account) ? attempts.get(account) : 0;
            if (attempt >= MAX_ATTEMPTS) {
                attempts.remove(account);
                Main.log("ConnectionWatchdog: giving up on account %d after %d attempts", account, MAX_ATTEMPTS);
                return;
            }
            attempts.put(account, attempt + 1);
            Main.log("ConnectionWatchdog: stuck connecting on account %d, retry %d", account, attempt + 1);
            manager.checkConnection();
            manager.resumeNetworkMaybe();
            schedule(account);
        } catch (Throwable t) {
            Main.log("ConnectionWatchdog.onTimeout error: %s", t.getMessage());
        }
    }
}
