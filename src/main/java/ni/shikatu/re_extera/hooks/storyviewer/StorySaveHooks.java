package ni.shikatu.re_extera.hooks.storyviewer;

import de.robv.android.xposed.XC_MethodHook;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Stories.PeerStoriesView;

/**
 * Unlocks screenshots and saving of stories that are protected by the
 * {@code StoryItem.noforwards} flag.
 */
public final class StorySaveHooks {

    private static final Field CURRENT_ACCOUNT_FIELD = field(PeerStoriesView.class, "currentAccount");
    private static final Map<String, PendingSave> PENDING = new ConcurrentHashMap<>();
    private static final long POLL_INTERVAL_MS = 1_000L;
    private static final int MAX_POLL_ATTEMPTS = 60;

    private StorySaveHooks() {
    }

    private static Field field(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            Main.log("StorySaveHooks: field '%s' not found: %s", name, t.getMessage());
            return null;
        }
    }

    /** StoryViewer.allowScreenshots(boolean) — clears FLAG_SECURE for stories. */
    public static class AllowScreenshotsHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(MethodHookParam param) {
            if (Settings.getRemoveFlagSecure() || Settings.getSaveProtectedStories()) {
                param.args[0] = true;
            }
        }
    }

    /** StoryItemHolder.allowScreenshots() — reveals the "Save to gallery" menu entry. */
    public static class HolderAllowScreenshotsHook extends XC_MethodHook {
        @Override
        public void afterHookedMethod(MethodHookParam param) {
            if (Settings.getSaveProtectedStories()) {
                param.setResult(true);
            }
        }
    }

    /**
     * PeerStoriesView.saveToGallery() — the stock implementation only works when the
     * media file is already on disk, which is not the case for protected stories.
     * Downloads the media into the cache first and saves it once it arrives.
     */
    public static class SaveToGalleryHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(MethodHookParam param) {
            if (!Settings.getSaveProtectedStories() || CURRENT_ACCOUNT_FIELD == null) {
                return;
            }
            try {
                PeerStoriesView view = (PeerStoriesView) param.thisObject;
                PeerStoriesView.StoryItemHolder holder = view.currentStory;
                if (holder == null || holder.storyItem == null || holder.uploadingStory != null) {
                    return;
                }
                TL_stories.StoryItem item = holder.storyItem;
                File existing = holder.getPath();
                if (existing != null && existing.exists()) {
                    return;
                }
                int account = ((Integer) ReflectionUtils.get(CURRENT_ACCOUNT_FIELD, view)).intValue();
                File cached = findCacheFile(account, item);
                if (cached != null && cached.exists()) {
                    save(view, cached, isVideo(item));
                    param.setResult(null);
                    return;
                }
                List<String> keys = startDownload(account, item);
                if (!keys.isEmpty()) {
                    PendingSave pending = new PendingSave(view, cached, isVideo(item), keys);
                    for (String key : keys) {
                        PENDING.put(key, pending);
                    }
                    pending.schedulePoll();
                    param.setResult(null);
                }
            } catch (Throwable t) {
                Main.log("StorySaveHooks: saveToGallery failed: %s", t.getMessage());
            }
        }
    }

    /** NotificationCenter.fileLoaded — finishes deferred saves without waiting for the poll. */
    public static void onFileLoaded(String fileName, File file) {
        if (fileName == null || file == null) {
            return;
        }
        PendingSave pending = PENDING.get(fileName);
        if (pending != null) {
            complete(pending, file);
        }
    }

    private static void complete(PendingSave pending, File file) {
        if (!pending.done.compareAndSet(false, true)) {
            return;
        }
        removePending(pending);
        save(pending.view, file, pending.video);
    }

    private static void removePending(PendingSave pending) {
        for (String key : pending.keys) {
            PENDING.remove(key);
        }
    }

    private static void save(final PeerStoriesView view, File file, boolean video) {
        try {
            MediaController.saveFile(file.getAbsolutePath(), view.getContext(), video ? 1 : 0, null, null, uri -> {
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        BulletinFactory.createSaveToGalleryBulletin(view, video, null).show();
                    } catch (Throwable t) {
                        Main.log("StorySaveHooks: bulletin failed: %s", t.getMessage());
                    }
                });
            });
        } catch (Throwable t) {
            Main.log("StorySaveHooks: MediaController.saveFile failed: %s", t.getMessage());
        }
    }

    private static boolean isVideo(TL_stories.StoryItem item) {
        if (item.media == null || item.media.document == null) {
            return false;
        }
        return MessageObject.isVideoDocument(item.media.document) || "video/mp4".equals(item.media.document.mime_type);
    }

    private static File findCacheFile(int account, TL_stories.StoryItem item) {
        FileLoader loader = FileLoader.getInstance(account);
        if (item.media == null) {
            return null;
        }
        if (item.media.document != null) {
            return loader.getPathToAttach(item.media.document, true);
        }
        if (item.media.photo != null) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(item.media.photo.sizes, Integer.MAX_VALUE);
            return size == null ? null : loader.getPathToAttach(size, true);
        }
        return null;
    }

    private static List<String> startDownload(int account, TL_stories.StoryItem item) {
        List<String> keys = new ArrayList<>();
        FileLoader loader = FileLoader.getInstance(account);
        if (item.media == null) {
            return keys;
        }
        if (item.media.document != null) {
            TLRPC.Document document = item.media.document;
            loader.loadFile(document, item, 1, 1);
            keys.add(FileLoader.getAttachFileName(document));
            ImageLocation documentLocation = ImageLocation.getForDocument(document);
            String imageKey = documentLocation == null ? null : documentLocation.getKey(null, null, false);
            if (imageKey != null && !keys.contains(imageKey)) {
                keys.add(imageKey);
            }
        } else if (item.media.photo != null) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(item.media.photo.sizes, Integer.MAX_VALUE);
            if (size == null) {
                return keys;
            }
            ImageLocation location = ImageLocation.getForPhoto(size, item.media.photo);
            if (location == null) {
                return keys;
            }
            loader.loadFile(location, item, "jpg", 1, 1);
            keys.add(FileLoader.getAttachFileName(size));
            String imageKey = location.getKey(null, null, false);
            if (imageKey != null && !keys.contains(imageKey)) {
                keys.add(imageKey);
            }
        }
        return keys;
    }

    private static final class PendingSave {
        final PeerStoriesView view;
        final File cacheFile;
        final boolean video;
        final List<String> keys = new ArrayList<>();
        final java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean(false);
        private int attempts;

        PendingSave(PeerStoriesView view, File cacheFile, boolean video, List<String> keys) {
            this.view = view;
            this.cacheFile = cacheFile;
            this.video = video;
            this.keys.addAll(keys);
        }

        void schedulePoll() {
            AndroidUtilities.runOnUIThread(() -> {
                if (done.get()) {
                    return;
                }
                File file = cacheFile;
                if (file != null && file.exists()) {
                    complete(this, file);
                    return;
                }
                if (++attempts >= MAX_POLL_ATTEMPTS) {
                    removePending(this);
                    Main.log("StorySaveHooks: download timed out for %s", file);
                    return;
                }
                schedulePoll();
            }, POLL_INTERVAL_MS);
        }
    }
}
