package ni.shikatu.re_extera.utils;

import androidx.collection.LongSparseArray;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.db.ShadowbanEntry;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

public class ShadowbanCache {
    private static final ConcurrentHashMap<Long, ShadowbanEntry> cache = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        reload();
        initialized = true;
    }

    public static void reload() {
        cache.clear();
        List<ShadowbanEntry> entries = ReExteraDb.get().getAllShadowbanned();
        for (ShadowbanEntry entry : entries) {
            cache.put(Long.valueOf(entry.userId), entry);
        }
    }

    public static boolean isShadowbanned(long userId) {
        return cache.containsKey(Long.valueOf(userId));
    }

    public static boolean shouldHideDialog(long userId) {
        ShadowbanEntry entry = cache.get(Long.valueOf(userId));
        return entry != null && entry.hideDialog;
    }

    public static boolean shouldHideInGroups(long userId) {
        ShadowbanEntry entry = cache.get(Long.valueOf(userId));
        return entry != null && entry.hideInGroups;
    }

    public static ShadowbanEntry get(long userId) {
        return cache.get(Long.valueOf(userId));
    }

    public static void add(long userId, boolean hideDialog, boolean hideInGroups) {
        ReExteraDb.get().addShadowbanAsync(userId, hideDialog, hideInGroups);
        cache.put(Long.valueOf(userId), new ShadowbanEntry(userId, hideDialog, hideInGroups, System.currentTimeMillis()));
    }

    public static void remove(long userId) {
        ReExteraDb.get().removeShadowbanAsync(userId);
        cache.remove(Long.valueOf(userId));
    }

    public static void update(long userId, boolean hideDialog, boolean hideInGroups) {
        ReExteraDb.get().updateShadowbanAsync(userId, hideDialog, hideInGroups);
        ShadowbanEntry existing = cache.get(Long.valueOf(userId));
        if (existing != null) {
            cache.put(Long.valueOf(userId), new ShadowbanEntry(userId, hideDialog, hideInGroups, existing.addedTs));
        }
    }

    public static void notifyDialogsUpdate() {
        notifyDialogsUpdate(UserConfig.selectedAccount);
    }

    public static void notifyDialogsUpdate(final int account) {
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.utils.ShadowbanCache$$ExternalSyntheticLambda0
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
