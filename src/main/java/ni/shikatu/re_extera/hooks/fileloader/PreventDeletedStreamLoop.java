package ni.shikatu.re_extera.hooks.fileloader;

import android.net.Uri;
import android.os.SystemClock;
import de.robv.android.xposed.XC_MethodHook;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.HookInit;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.messenger.FileLoadOperation;
import org.telegram.messenger.FileStreamLoadOperation;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

public final class PreventDeletedStreamLoop {
    private static final int STATE_FAILED = 2;
    private static final long FAILED_COOLDOWN_MS = 5000L;
    private static final ConcurrentHashMap<Long, Long> FAILED_DOCS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Field> URI_FIELDS = new ConcurrentHashMap<>();

    private static final Field loadOperationField;
    private static final Field stateField;
    private static final Field NO_URI_FIELD;

    static {
        Field loadOp = null;
        try {
            loadOp = FileStreamLoadOperation.class.getDeclaredField("loadOperation");
            loadOp.setAccessible(true);
        } catch (Throwable t) {
            Main.log("PreventDeletedStreamLoop: field loadOperation not found: %s", t.getMessage());
        }
        loadOperationField = loadOp;

        Field state = null;
        try {
            state = FileLoadOperation.class.getDeclaredField("state");
            state.setAccessible(true);
        } catch (Throwable t) {
            Main.log("PreventDeletedStreamLoop: field state not found: %s", t.getMessage());
        }
        stateField = state;

        Field sentinel = null;
        try {
            sentinel = PreventDeletedStreamLoop.class.getDeclaredField("STATE_FAILED");
        } catch (Throwable ignored) {
        }
        NO_URI_FIELD = sentinel;
    }

    private PreventDeletedStreamLoop() {
    }

    public static void clear() {
        FAILED_DOCS.clear();
        URI_FIELDS.clear();
    }

    public static final class LoadStreamFileHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
            try {
                if (!HookInit.isActive || param.args == null || param.args.length < 4) {
                    return;
                }
                Object streamObj = param.args[0];
                if (!(streamObj instanceof FileStreamLoadOperation)) {
                    return;
                }
                FileLoadOperation loadOp = getLoadOperation((FileStreamLoadOperation) streamObj);
                if (loadOp == null) {
                    return;
                }
                if (getState(loadOp) == STATE_FAILED) {
                    long docId = (param.args[1] instanceof TLRPC.Document)
                            ? ((TLRPC.Document) param.args[1]).id
                            : 0L;
                    if (docId != 0L && isDeletedMessage(param.args[3])) {
                        recordFailedDoc(docId);
                        param.setThrowable(new FileNotFoundException("Deleted stream FileLoadOperation failed (state=2)"));
                    }
                }
            } catch (Throwable t) {
                Main.log("PreventDeletedStreamLoop.LoadStreamFileHook error: %s", t.getMessage());
            }
        }
    }

    public static final class OpenHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
            try {
                if (!HookInit.isActive || param.args == null || param.args.length < 1 || param.args[0] == null) {
                    return;
                }
                Uri uri = getDataSpecUri(param.args[0]);
                if (uri == null || !uri.isHierarchical()) {
                    return;
                }
                String idParam = uri.getQueryParameter("id");
                if (idParam == null) {
                    return;
                }
                String trimmed = idParam.trim();
                if (trimmed.startsWith("+")) {
                    trimmed = trimmed.substring(1).trim();
                }
                if (trimmed.isEmpty()) {
                    return;
                }
                long docId;
                try {
                    docId = trimmed.startsWith("-") ? Long.parseLong(trimmed) : Long.parseUnsignedLong(trimmed);
                } catch (NumberFormatException ignored) {
                    return;
                }
                Long failedAt = FAILED_DOCS.get(docId);
                if (failedAt != null) {
                    long age = SystemClock.elapsedRealtime() - failedAt;
                    if (age >= 0L && age < FAILED_COOLDOWN_MS) {
                        param.setThrowable(new FileNotFoundException("Deleted stream document recently failed: " + docId));
                    } else {
                        FAILED_DOCS.remove(docId, failedAt);
                    }
                }
            } catch (Throwable t) {
                Main.log("PreventDeletedStreamLoop.OpenHook error: %s", t.getMessage());
            }
        }
    }

    private static void recordFailedDoc(long docId) {
        long now = SystemClock.elapsedRealtime();
        if (FAILED_DOCS.size() >= 64) {
            FAILED_DOCS.entrySet().removeIf(entry -> {
                long age = now - entry.getValue();
                return age < 0L || age >= FAILED_COOLDOWN_MS;
            });
            if (FAILED_DOCS.size() >= 64) {
                long oldestTime = Long.MAX_VALUE;
                Long oldestKey = null;
                for (ConcurrentHashMap.Entry<Long, Long> entry : FAILED_DOCS.entrySet()) {
                    if (entry.getValue() < oldestTime) {
                        oldestTime = entry.getValue();
                        oldestKey = entry.getKey();
                    }
                }
                if (oldestKey != null) {
                    FAILED_DOCS.remove(oldestKey);
                }
            }
        }
        FAILED_DOCS.put(docId, now);
    }

    private static boolean isDeletedMessage(Object parentObject) {
        if (parentObject instanceof MessageObject) {
            MessageObject msg = (MessageObject) parentObject;
            if (msg.deleted) {
                return true;
            }
            ReExteraDb db = ReExteraDb.get();
            return db != null && db.messageIsDeleted(msg);
        }
        if (parentObject instanceof TLRPC.Message) {
            TLRPC.Message rawMsg = (TLRPC.Message) parentObject;
            long dialogId = MessageUtils.getDialogIdFromMessage(rawMsg);
            ReExteraDb db = ReExteraDb.get();
            return db != null && db.messageIsDeleted(dialogId, rawMsg.id);
        }
        if (parentObject instanceof MessageObject.GroupedMessages) {
            MessageObject.GroupedMessages grouped = (MessageObject.GroupedMessages) parentObject;
            if (grouped.messages != null) {
                for (MessageObject msg : grouped.messages) {
                    if (isDeletedMessage(msg)) {
                        return true;
                    }
                }
            }
        } else if (parentObject instanceof Collection<?>) {
            for (Object item : (Collection<?>) parentObject) {
                if (isDeletedMessage(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static FileLoadOperation getLoadOperation(FileStreamLoadOperation streamOp) {
        try {
            Field f = loadOperationField;
            if (f != null) {
                Object value = f.get(streamOp);
                return value instanceof FileLoadOperation ? (FileLoadOperation) value : null;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int getState(FileLoadOperation loadOp) {
        try {
            Field f = stateField;
            if (f != null) {
                return f.getInt(loadOp);
            }
            return -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static Uri getDataSpecUri(Object dataSpec) {
        try {
            Class<?> cls = dataSpec.getClass();
            Field f = URI_FIELDS.get(cls);
            if (f == NO_URI_FIELD) {
                return null;
            }
            if (f == null) {
                Field found = null;
                for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                    try {
                        found = c.getDeclaredField("uri");
                        found.setAccessible(true);
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
                if (found == null) {
                    URI_FIELDS.put(cls, NO_URI_FIELD);
                    return null;
                }
                URI_FIELDS.put(cls, found);
                f = found;
            }
            Object value = f.get(dataSpec);
            return value instanceof Uri ? (Uri) value : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
