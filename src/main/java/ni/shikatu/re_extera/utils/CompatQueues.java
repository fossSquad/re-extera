package ni.shikatu.re_extera.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.AndroidUtilities;

/**
 * Telegram 13.2.x changed {@code Utilities.stageQueue} and {@code Utilities.globalQueue}
 * from {@code DispatchQueue} to {@code DispatchQueue2}. Referencing those fields directly
 * makes the plugin fail with {@code NoSuchFieldError} on older clients (for example 13.1.1),
 * even though the queue API is the same. Resolve the fields reflectively at runtime instead.
 */
public final class CompatQueues {
    private static volatile Handle stageHandle;
    private static volatile Handle globalHandle;

    private CompatQueues() {
    }

    public static void postStage(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        Handle handle = stageHandle;
        if (handle == null) {
            handle = resolve("stageQueue");
            stageHandle = handle;
        }
        if (!handle.post(runnable, 0)) {
            AndroidUtilities.runOnUIThread(runnable);
        }
    }

    public static void postGlobal(Runnable runnable) {
        postGlobal(runnable, 0);
    }

    public static void postGlobal(Runnable runnable, long delay) {
        if (runnable == null) {
            return;
        }
        Handle handle = globalHandle;
        if (handle == null) {
            handle = resolve("globalQueue");
            globalHandle = handle;
        }
        if (!handle.post(runnable, delay)) {
            if (delay > 0) {
                AndroidUtilities.runOnUIThread(runnable, delay);
            } else {
                AndroidUtilities.runOnUIThread(runnable);
            }
        }
    }

    private static Handle resolve(String fieldName) {
        try {
            Field field = Class.forName("org.telegram.messenger.Utilities").getField(fieldName);
            Object queue = field.get(null);
            if (queue == null) {
                return Handle.EMPTY;
            }
            Class<?> queueClass = queue.getClass();
            Method post = queueClass.getMethod("postRunnable", Runnable.class);
            Method postDelayed = queueClass.getMethod("postRunnable", Runnable.class, Long.TYPE);
            return new Handle(queue, post, postDelayed);
        } catch (Throwable t) {
            Main.log("CompatQueues: %s not available: %s", fieldName, t.getMessage());
            return Handle.EMPTY;
        }
    }

    private static final class Handle {
        static final Handle EMPTY = new Handle(null, null, null);

        private final Object queue;
        private final Method post;
        private final Method postDelayed;

        Handle(Object queue, Method post, Method postDelayed) {
            this.queue = queue;
            this.post = post;
            this.postDelayed = postDelayed;
        }

        boolean post(Runnable runnable, long delay) {
            if (this.queue == null) {
                return false;
            }
            try {
                if (delay > 0) {
                    this.postDelayed.invoke(this.queue, runnable, delay);
                } else {
                    this.post.invoke(this.queue, runnable);
                }
                return true;
            } catch (Throwable t) {
                Main.log("CompatQueues: post failed: %s", t.getMessage());
                return false;
            }
        }
    }
}
