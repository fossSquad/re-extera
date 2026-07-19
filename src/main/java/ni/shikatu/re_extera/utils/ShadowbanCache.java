package ni.shikatu.re_extera.utils;

import androidx.collection.LongSparseArray;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.db.ShadowbanEntry;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;

public final class ShadowbanCache {
    private static final ConcurrentHashMap<Long, ShadowbanEntry> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private ShadowbanCache() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        reload();
        initialized = true;
    }

    public static void reload() {
        CACHE.clear();
        List<ShadowbanEntry> entries = ReExteraDb.get().getAllShadowbanned();
        for (ShadowbanEntry entry : entries) {
            CACHE.put(Long.valueOf(entry.userId), entry);
        }
    }

    public static boolean isShadowbanned(long userId) {
        return CACHE.containsKey(Long.valueOf(userId));
    }

    public static boolean shouldHideDialog(long userId) {
        ShadowbanEntry entry = CACHE.get(Long.valueOf(userId));
        return entry != null && entry.hideDialog;
    }

    public static boolean shouldHideInGroups(long userId) {
        ShadowbanEntry entry = CACHE.get(Long.valueOf(userId));
        return entry != null && entry.hideInGroups;
    }

    public static ShadowbanEntry get(long userId) {
        return CACHE.get(Long.valueOf(userId));
    }

    public static void add(long userId, boolean hideDialog, boolean hideInGroups) {
        ReExteraDb.get().addShadowbanAsync(userId, hideDialog, hideInGroups);
        CACHE.put(Long.valueOf(userId), new ShadowbanEntry(userId, hideDialog, hideInGroups, System.currentTimeMillis()));
    }

    public static void remove(long userId) {
        ReExteraDb.get().removeShadowbanAsync(userId);
        CACHE.remove(Long.valueOf(userId));
    }

    public static void update(long userId, boolean hideDialog, boolean hideInGroups) {
        ReExteraDb.get().updateShadowbanAsync(userId, hideDialog, hideInGroups);
        ShadowbanEntry existing = CACHE.get(Long.valueOf(userId));
        long addedTs = existing != null ? existing.addedTs : System.currentTimeMillis();
        CACHE.put(Long.valueOf(userId), new ShadowbanEntry(userId, hideDialog, hideInGroups, addedTs));
    }

    public static List<ShadowbanEntry> snapshot() {
        return ReExteraDb.get().getAllShadowbanned();
    }

    public static void notifyDialogsUpdate(final int account) {
        AndroidUtilities.runOnUIThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                ShadowbanCache.lambda$notifyDialogsUpdate$0(account);
            }
        });
    }

    static /* synthetic */ void lambda$notifyDialogsUpdate$0(int account) {
        MessagesController controller = MessagesController.getInstance(account);
        NotificationCenter nc = NotificationCenter.getInstance(account);
        controller.sortDialogs((LongSparseArray) null);
        nc.postNotificationName(NotificationCenter.dialogsNeedReload, new Object[]{true});
        nc.postNotificationName(NotificationCenter.dialogsUnreadCounterChanged, new Object[]{0});
        nc.postNotificationName(NotificationCenter.updateInterfaces, new Object[]{Integer.valueOf(MessagesController.UPDATE_MASK_ALL)});
    }
}
