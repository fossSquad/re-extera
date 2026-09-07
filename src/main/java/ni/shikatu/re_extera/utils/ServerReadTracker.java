package ni.shikatu.re_extera.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the true server-side inbox read position (per account / dialog).
 *
 * Unlike MessagesController.dialogs_read_inbox_max — which is also bumped
 * locally whenever ChatActivity marks messages as read on screen — this
 * tracker is only advanced from real server signals:
 *   - TL_updateReadHistoryInbox / TL_updateReadChannelInbox
 *   - readHistory requests that actually reach the server
 *   - the persisted read position snapshot taken before any local read
 */
public final class ServerReadTracker {
    private static final Map<Integer, Map<Long, Integer>> byAccount = new HashMap<>();

    private ServerReadTracker() {
    }

    private static synchronized Map<Long, Integer> mapFor(int account) {
        Map<Long, Integer> m = byAccount.get(account);
        if (m == null) {
            m = new HashMap<>();
            byAccount.put(account, m);
        }
        return m;
    }

    public static synchronized int get(int account, long did) {
        Map<Long, Integer> m = byAccount.get(account);
        if (m == null) {
            return 0;
        }
        Integer v = m.get(did);
        return v != null ? v.intValue() : 0;
    }

    public static synchronized void update(int account, long did, int maxId) {
        if (maxId <= 0 || did == 0) {
            return;
        }
        Map<Long, Integer> m = mapFor(account);
        Integer cur = m.get(did);
        if (cur == null || maxId > cur.intValue()) {
            m.put(did, Integer.valueOf(maxId));
        }
    }

    public static synchronized void seed(int account, long did, int maxId) {
        if (maxId <= 0 || did == 0) {
            return;
        }
        Map<Long, Integer> m = mapFor(account);
        if (!m.containsKey(did)) {
            m.put(did, Integer.valueOf(maxId));
        }
    }
}
