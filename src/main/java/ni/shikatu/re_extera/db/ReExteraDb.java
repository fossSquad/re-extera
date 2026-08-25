package ni.shikatu.re_extera.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.HandlerThread;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

public final class ReExteraDb {
    private static final int BATCH_IN_SIZE = 500;
    public static final String DB_NAME = "re_extera.db";
    public static final int DB_VERSION = 13;
    private static final long DELETED_KEYS_TTL_MS = TimeUnit.DAYS.toMillis(30);
    private static volatile ReExteraDb instance;
    private final Handler dbHandler;
    private final HandlerThread dbThread;
    private final Helper helper;
    private final java.util.concurrent.ConcurrentHashMap<Long, Integer> lastOnlineCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, Boolean> filterExceptionsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.Set<Integer>> deletedKeysCache = new java.util.concurrent.ConcurrentHashMap<>();

    private java.util.Set<Integer> getOrCreateDeletedSet(long did) {
        java.util.Set<Integer> set = deletedKeysCache.get(did);
        if (set == null) {
            java.util.Set<Integer> newSet = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>());
            java.util.Set<Integer> oldSet = deletedKeysCache.putIfAbsent(did, newSet);
            set = oldSet != null ? oldSet : newSet;
        }
        return set;
    }

    public static synchronized ReExteraDb init(Context context) {
        if (instance == null) {
            instance = new ReExteraDb(context);
        }
        return instance;
    }

    public static ReExteraDb get() {
        return instance;
    }

    private ReExteraDb(Context context) {
        File dbFile = new File(context.getFilesDir(), DB_NAME);
        this.helper = new Helper(context, dbFile.getAbsolutePath());
        this.dbThread = new HandlerThread("re_extera_db");
        this.dbThread.start();
        this.dbHandler = new Handler(this.dbThread.getLooper());
        this.dbHandler.postDelayed(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                pruneStaleEntries();
            }
        }, TimeUnit.SECONDS.toMillis(30L));
        this.dbHandler.post(new Runnable() {
            @Override
            public void run() {
                loadFilterExceptionsCache();
                loadDeletedKeysCache();
            }
        });
    }

    private void loadDeletedKeysCache() {
        android.database.sqlite.SQLiteDatabase db = this.helper.getReadableDatabase();
        android.database.Cursor c = null;
        try {
            c = db.rawQuery("SELECT did, mid FROM deleted_keys", null);
            if (c.moveToFirst()) {
                do {
                    long did = c.getLong(0);
                    int mid = c.getInt(1);
                    getOrCreateDeletedSet(did).add(mid);
                } while (c.moveToNext());
            }
        } catch (Throwable e) {
            ni.shikatu.re_extera.Main.log("Failed to load deletedKeysCache: %s", e.getMessage());
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void loadFilterExceptionsCache() {
        android.database.sqlite.SQLiteDatabase db = this.helper.getReadableDatabase();
        android.database.Cursor c = null;
        try {
            c = db.rawQuery("SELECT did FROM exception_users WHERE exception_filter != 0", null);
            if (c.moveToFirst()) {
                do {
                    filterExceptionsCache.put(c.getLong(0), Boolean.TRUE);
                } while (c.moveToNext());
            }
        } catch (Throwable e) {
            ni.shikatu.re_extera.Main.log("Failed to load filterExceptionsCache", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void postToDbThread(Runnable r) {
        this.dbHandler.post(r);
    }

    /* JADX INFO: renamed from: putDeletedMessage, reason: merged with bridge method [inline-methods] */
    public void lambda$putDeletedMessageAsync$0(long did, int mid) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO deleted_keys(did, mid, ts) VALUES(?, ?, ?)");
            try {
                st.bindLong(1, did);
                st.bindLong(2, mid);
                st.bindLong(3, System.currentTimeMillis());
                st.executeInsert();
                if (st != null) {
                    st.close();
                }
            } catch (Throwable th) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("putDeletedMessage error: %s", e.getMessage());
        }
    }

    public void putDeletedMessageAsync(final long did, final int mid) {
        getOrCreateDeletedSet(did).add(mid);
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$putDeletedMessageAsync$0(did, mid);
            }
        });
    }

    /* JADX INFO: renamed from: batchPutDeletedMessages, reason: merged with bridge method [inline-methods] */
    public void lambda$batchPutDeletedMessagesAsync$1(long did, Collection<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try {
                SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO deleted_keys(did, mid, ts) VALUES(?, ?, ?)");
                try {
                    long now = System.currentTimeMillis();
                    Iterator<Integer> it = mids.iterator();
                    while (it.hasNext()) {
                        int mid = it.next().intValue();
                        st.clearBindings();
                        st.bindLong(1, did);
                        st.bindLong(2, mid);
                        st.bindLong(3, now);
                        st.executeInsert();
                    }
                    db.setTransactionSuccessful();
                    if (st != null) {
                        st.close();
                    }
                    db.endTransaction();
                } catch (Throwable th) {
                    if (st != null) {
                        try {
                            st.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                    }
                    throw th;
                }
            } catch (Exception e) {
                Main.log("batchPutDeletedMessages error: %s", e.getMessage());
            }
        } catch (Throwable th3) {
            db.endTransaction();
            throw th3;
        }
    }

    public void batchPutDeletedMessagesAsync(final long did, Collection<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        getOrCreateDeletedSet(did).addAll(mids);
        final ArrayList<Integer> copy = new ArrayList<>(mids);
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$batchPutDeletedMessagesAsync$1(did, copy);
            }
        });
    }

    public boolean messageIsDeleted(long did, int mid) {
        java.util.Set<Integer> set = deletedKeysCache.get(did);
        if (set != null && set.contains(mid)) {
            return true;
        }
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT 1 FROM deleted_keys WHERE did=? AND mid=? LIMIT 1", new String[]{String.valueOf(did), String.valueOf(mid)});
            try {
                boolean zMoveToFirst = c.moveToFirst();
                if (zMoveToFirst) {
                    getOrCreateDeletedSet(did).add(mid);
                }
                if (c != null) {
                    c.close();
                }
                return zMoveToFirst;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("messageIsDeleted error: %s", e.getMessage());
            return false;
        }
    }

    public boolean messageIsDeleted(MessageObject msg) {
        if (msg == null) {
            return false;
        }
        return messageIsDeleted(msg.getDialogId(), msg.getId());
    }

    /**
     * Did-independent check: is {@code mid} recorded as deleted in ANY dialog?
     *
     * <p>Server delete updates (notably {@code TL_updateDeleteMessages} and channel
     * deletes) do not always give the same dialog id that the message reports at
     * render time, so the strict (did, mid) lookup can miss. This mid-only fallback
     * keeps the deleted mark reliable across sessions. Message ids are large and
     * effectively unique in practice, so cross-dialog collisions are negligible.
     */
    public boolean isMidDeletedAnyDialog(int mid) {
        // Cache-only: deletedKeysCache is fully loaded at startup and kept in sync
        // on every write, so a memory scan is authoritative and avoids per-render DB I/O.
        for (java.util.Set<Integer> set : deletedKeysCache.values()) {
            if (set != null && set.contains(mid)) {
                return true;
            }
        }
        return false;
    }

    public List<Long> getDialogIdWithSavedMessages() {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<Long> result = new ArrayList<>();
        try {
            Cursor c = db.rawQuery("SELECT DISTINCT did FROM deleted_keys", null);
            while (c.moveToNext()) {
                try {
                    result.add(Long.valueOf(c.getLong(0)));
                } catch (Throwable th) {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                    }
                    throw th;
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Main.log("getDialogIdWithSavedMessages error: %s", e.getMessage());
        }
        return result;
    }

    public ArrayList<Integer> allMessageIdsByDid(long did) {
        java.util.Set<Integer> set = deletedKeysCache.get(did);
        if (set != null && !set.isEmpty()) {
            return new ArrayList<>(set);
        }
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<Integer> result = new ArrayList<>();
        try {
            Cursor c = db.rawQuery("SELECT mid FROM deleted_keys WHERE did=?", new String[]{String.valueOf(did)});
            while (c.moveToNext()) {
                try {
                    result.add(Integer.valueOf(c.getInt(0)));
                } catch (Throwable th) {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                    }
                    throw th;
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Main.log("allMessageIdsByDid error: %s", e.getMessage());
        }
        return result;
    }

    public void clearMessages(long did, List<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        java.util.Set<Integer> set = deletedKeysCache.get(did);
        if (set != null) {
            set.removeAll(mids);
        }
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try {
                String didStr = String.valueOf(did);
                for (int from = 0; from < mids.size(); from += BATCH_IN_SIZE) {
                    int to = Math.min(from + BATCH_IN_SIZE, mids.size());
                    List<Integer> slice = mids.subList(from, to);
                    String placeholders = buildPlaceholders(slice.size());
                    String[] args = buildInArgs(didStr, slice);
                    db.execSQL("DELETE FROM deleted_keys WHERE did=? AND mid IN (" + placeholders + ")", args);
                    db.execSQL("DELETE FROM message_edits WHERE did=? AND mid IN (" + placeholders + ")", args);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Main.log("clearMessages error: %s", e.getMessage());
            }
        } finally {
            db.endTransaction();
        }
    }

    public boolean messageHasSavedEdits(long did, long mid) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?", new String[]{String.valueOf(did), String.valueOf(mid)});
            try {
                boolean z = c.moveToFirst() && !c.isNull(0) && c.getInt(0) > 0;
                if (c != null) {
                    c.close();
                }
                return z;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("messageHasSavedEdits error: %s", e.getMessage());
            return false;
        }
    }

    public boolean messageHasSavedEdits(MessageObject obj) {
        return messageHasSavedEdits(obj.getDialogId(), obj.getId());
    }

    /* JADX INFO: renamed from: saveOriginalMessage, reason: merged with bridge method [inline-methods] */
    public void lambda$saveOriginalMessageAsync$2(long did, int mid, TLRPC.Message message) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, 0, ?, ?)");
            try {
                st.bindLong(1, did);
                st.bindLong(2, mid);
                st.bindLong(3, System.currentTimeMillis());
                byte[] blob = serializeMessage(message);
                st.bindBlob(4, blob != null ? blob : new byte[0]);
                st.executeInsert();
                if (st != null) {
                    st.close();
                }
            } catch (Throwable th) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("saveOriginalMessage error: %s", e.getMessage());
        }
    }

    public void saveOriginalMessageAsync(final long did, final int mid, final TLRPC.Message message) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$saveOriginalMessageAsync$2(did, mid, message);
            }
        });
    }

    /* JADX INFO: renamed from: saveNewVersionMessage, reason: merged with bridge method [inline-methods] */
    public void lambda$saveNewVersionMessageAsync$3(long did, int mid, TLRPC.Message msg) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        int nextVer = 1;
        try {
            try {
                Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?", new String[]{String.valueOf(did), String.valueOf(mid)});
                try {
                    if (c.moveToFirst() && !c.isNull(0)) {
                        nextVer = Math.max(1, c.getInt(0) + 1);
                    }
                    if (c != null) {
                        c.close();
                    }
                    SQLiteStatement st = db.compileStatement("INSERT INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, ?, ?, ?)");
                    try {
                        st.bindLong(1, did);
                        st.bindLong(2, mid);
                        st.bindLong(3, nextVer);
                        st.bindLong(4, System.currentTimeMillis());
                        byte[] blob = serializeMessage(msg);
                        st.bindBlob(5, blob != null ? blob : new byte[0]);
                        st.executeInsert();
                        if (st != null) {
                            st.close();
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();
                    } catch (Throwable th) {
                        if (st != null) {
                            try {
                                st.close();
                            } catch (Throwable th2) {
                                Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Throwable th4) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th3, th4);
                        }
                    }
                    throw th3;
                }
            } catch (Exception e) {
                Main.log("saveNewVersionMessage error: %s", e.getMessage());
            }
        } catch (Throwable th5) {
            db.endTransaction();
            throw th5;
        }
    }

    public void saveNewVersionMessageAsync(final long did, final int mid, final TLRPC.Message msg) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$saveNewVersionMessageAsync$3(did, mid, msg);
            }
        });
    }

    public ArrayList<TLRPC.Message> listVersionsOfEditedMessage(long did, int mid) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<TLRPC.Message> out = new ArrayList<>();
        try {
            Cursor c = db.rawQuery("SELECT ver, date, data FROM message_edits WHERE did=? AND mid=? ORDER BY ver ASC", new String[]{String.valueOf(did), String.valueOf(mid)});
            while (c.moveToNext()) {
                try {
                    TLRPC.Message m = deserializeMessage(c.getBlob(2));
                    if (m != null) {
                        out.add(m);
                    }
                } catch (Throwable th) {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                    }
                    throw th;
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Main.log("listVersionsOfEditedMessage error: %s", e.getMessage());
        }
        return out;
    }

    public List<DialogExclusion> getActiveExceptions() {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<DialogExclusion> result = new ArrayList<>();
        try {
            Cursor c = db.rawQuery("SELECT did, exception_reading, exception_typing, exception_filter FROM exception_users WHERE exception_reading != 0 OR exception_typing != 0 OR exception_filter != 0", null);
            while (c.moveToNext()) {
                try {
                    result.add(new DialogExclusion(c.getLong(0), c.getInt(1), c.getInt(2), c.getInt(3)));
                } catch (Throwable th) {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                    }
                    throw th;
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Main.log("getActiveExceptions error: %s", e.getMessage());
        }
        return result;
    }

    public DialogExclusion getException(long dialogId) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT did, exception_reading, exception_typing, exception_filter FROM exception_users WHERE did = ?", new String[]{String.valueOf(dialogId)});
            try {
                if (c.moveToFirst()) {
                    DialogExclusion dialogExclusion = new DialogExclusion(c.getLong(0), c.getInt(1), c.getInt(2), c.getInt(3));
                    if (c != null) {
                        c.close();
                    }
                    return dialogExclusion;
                }
                if (c != null) {
                    c.close();
                    return null;
                }
                return null;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            // removed
            return null;
        }
    }

    public int getDialogReading(long did) {
        return getExceptionColumn(did, "exception_reading");
    }

    public int getDialogTyping(long did) {
        return getExceptionColumn(did, "exception_typing");
    }

    public int getDialogFilter(long did) {
        return getExceptionColumn(did, "exception_filter");
    }

    public boolean isFilterExcluded(long did) {
        return filterExceptionsCache.containsKey(did);
    }

    private int getExceptionColumn(long did, String column) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT " + column + " FROM exception_users WHERE did = ?", new String[]{String.valueOf(did)});
            try {
                int i = c.moveToFirst() ? c.getInt(0) : 0;
                if (c != null) {
                    c.close();
                }
                return i;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("getExceptionColumn(%s) error: %s", column, e.getMessage());
            return 0;
        }
    }

    /* JADX INFO: renamed from: setDialogReading, reason: merged with bridge method [inline-methods] */
    public void lambda$setDialogReadingAsync$4(long did, int value) {
        lambda$upsertExceptionAsync$4(did, "exception_reading", "exception_typing", "exception_filter", value);
    }

    public void setDialogReadingAsync(final long did, final int value) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$setDialogReadingAsync$4(did, value);
            }
        });
    }

    /* JADX INFO: renamed from: setDialogTyping, reason: merged with bridge method [inline-methods] */
    public void lambda$setDialogTypingAsync$5(long did, int value) {
        lambda$upsertExceptionAsync$4(did, "exception_typing", "exception_reading", "exception_filter", value);
    }

    public void setDialogTypingAsync(final long did, final int value) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$setDialogTypingAsync$5(did, value);
            }
        });
    }

    /* JADX INFO: renamed from: setDialogFilter, reason: merged with bridge method [inline-methods] */
    public void lambda$setDialogFilterAsync$6(long did, int value) {
        lambda$upsertExceptionAsync$4(did, "exception_filter", "exception_reading", "exception_typing", value);
    }

    public void setDialogFilterAsync(final long did, final int value) {
        if (value != 0) {
            filterExceptionsCache.put(did, Boolean.TRUE);
        } else {
            filterExceptionsCache.remove(did);
        }
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$setDialogFilterAsync$6(did, value);
            }
        });
    }

    public void lambda$upsertExceptionAsync$4(long did, String column, String siblingColumn1, String siblingColumn2, int value) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try {
                ContentValues cv = new ContentValues();
                cv.put(column, Integer.valueOf(value));
                int upd = db.update("exception_users", cv, "did = ?", new String[]{String.valueOf(did)});
                if (upd == 0) {
                    cv.put("did", Long.valueOf(did));
                    cv.put(siblingColumn1, (Integer) 0);
                    cv.put(siblingColumn2, (Integer) 0);
                    db.insert("exception_users", null, cv);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Main.log("upsertException(%s) error: %s", column, e.getMessage());
            }
        } finally {
            db.endTransaction();
        }
    }

    public void addRegexFilter(String pattern) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO regex_filters(regex) VALUES(?)");
            try {
                st.bindString(1, pattern);
                st.executeInsert();
                if (st != null) {
                    st.close();
                }
            } catch (Throwable th) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("addRegexFilter error: %s", e.getMessage());
        }
    }

    public void updateRegexFilter(String oldPattern, String newPattern) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try {
                db.delete("regex_filters", "regex = ?", new String[]{oldPattern});
                SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO regex_filters(regex) VALUES(?)");
                try {
                    st.bindString(1, newPattern);
                    st.executeInsert();
                    if (st != null) {
                        st.close();
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } catch (Throwable th) {
                    if (st != null) {
                        try {
                            st.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                    }
                    throw th;
                }
            } catch (Exception e) {
                Main.log("updateRegexFilter error: %s", e.getMessage());
            }
        } catch (Throwable th3) {
            db.endTransaction();
            throw th3;
        }
    }

    public void deleteRegexFilter(String pattern) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            db.delete("regex_filters", "regex = ?", new String[]{pattern});
        } catch (Exception e) {
            Main.log("deleteRegexFilter error: %s", e.getMessage());
        }
    }

    public List<String> getAllRegexFilters() {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<String> result = new ArrayList<>();
        try {
            Cursor c = db.rawQuery("SELECT regex FROM regex_filters ORDER BY regex ASC", null);
            while (c.moveToNext()) {
                try {
                    result.add(c.getString(0));
                } catch (Throwable th) {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                    }
                    throw th;
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Main.log("getAllRegexFilters error: %s", e.getMessage());
        }
        return result;
    }

    public boolean hasRegexFilter(String pattern) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT 1 FROM regex_filters WHERE regex = ? LIMIT 1", new String[]{pattern});
            try {
                boolean zMoveToFirst = c.moveToFirst();
                if (c != null) {
                    c.close();
                }
                return zMoveToFirst;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("hasRegexFilter error: %s", e.getMessage());
            return false;
        }
    }

    /* JADX INFO: renamed from: addShadowban, reason: merged with bridge method [inline-methods] */
    public void lambda$addShadowbanAsync$6(long userId, boolean hideDialog, boolean hideInGroups) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            SQLiteStatement st = db.compileStatement("INSERT OR REPLACE INTO shadowban_users(user_id, hide_dialog, hide_in_groups, added_ts) VALUES(?, ?, ?, ?)");
            try {
                st.bindLong(1, userId);
                long j = 1;
                st.bindLong(2, hideDialog ? 1L : 0L);
                if (!hideInGroups) {
                    j = 0;
                }
                st.bindLong(3, j);
                st.bindLong(4, System.currentTimeMillis());
                st.executeInsert();
                if (st != null) {
                    st.close();
                }
            } catch (Throwable th) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("addShadowban error: %s", e.getMessage());
        }
    }

    public void addShadowbanAsync(final long userId, final boolean hideDialog, final boolean hideInGroups) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$addShadowbanAsync$6(userId, hideDialog, hideInGroups);
            }
        });
    }

    /* JADX INFO: renamed from: removeShadowban, reason: merged with bridge method [inline-methods] */
    public void lambda$removeShadowbanAsync$7(long userId) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            db.delete("shadowban_users", "user_id = ?", new String[]{String.valueOf(userId)});
        } catch (Exception e) {
            Main.log("removeShadowban error: %s", e.getMessage());
        }
    }

    public void removeShadowbanAsync(final long userId) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$removeShadowbanAsync$7(userId);
            }
        });
    }

    public boolean isShadowbanned(long userId) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT 1 FROM shadowban_users WHERE user_id = ? LIMIT 1", new String[]{String.valueOf(userId)});
            try {
                boolean zMoveToFirst = c.moveToFirst();
                if (c != null) {
                    c.close();
                }
                return zMoveToFirst;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("isShadowbanned error: %s", e.getMessage());
            return false;
        }
    }

    public ShadowbanEntry getShadowban(long userId) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT user_id, hide_dialog, hide_in_groups, added_ts FROM shadowban_users WHERE user_id = ?", new String[]{String.valueOf(userId)});
            try {
                if (c.moveToFirst()) {
                    ShadowbanEntry shadowbanEntry = new ShadowbanEntry(c.getLong(0), c.getInt(1) == 1, c.getInt(2) == 1, c.getLong(3));
                    if (c != null) {
                        c.close();
                    }
                    return shadowbanEntry;
                }
                if (c != null) {
                    c.close();
                    return null;
                }
                return null;
            } catch (Throwable th) {
                if (c == null) {
                    throw th;
                }
                try {
                    c.close();
                    throw th;
                } catch (Throwable th2) {
                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    throw th;
                }
            }
        } catch (Exception e) {
            // removed
            return null;
        }
    }

    public List<ShadowbanEntry> getAllShadowbanned() {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<ShadowbanEntry> result = new ArrayList<>();
        try {
            Cursor c = db.rawQuery("SELECT user_id, hide_dialog, hide_in_groups, added_ts FROM shadowban_users ORDER BY added_ts DESC", null);
            while (c.moveToNext()) {
                try {
                    result.add(new ShadowbanEntry(c.getLong(0), c.getInt(1) == 1, c.getInt(2) == 1, c.getLong(3)));
                } catch (Throwable th) {
                    if (c == null) {
                        throw th;
                    }
                    try {
                        c.close();
                        throw th;
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        throw th;
                    }
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Main.log("getAllShadowbanned error: %s", e.getMessage());
        }
        return result;
    }

    public void lambda$saveLastOnlineAsync$90(long userId, int ts) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            SQLiteStatement st = db.compileStatement("INSERT OR REPLACE INTO last_online_users(user_id, online_ts) VALUES(?, ?)");
            try {
                st.bindLong(1, userId);
                st.bindLong(2, ts);
                st.executeInsert();
                if (st != null) {
                    st.close();
                }
            } catch (Throwable th) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("saveLastOnline error: %s", e.getMessage());
        }
    }

    public void saveLastOnlineAsync(final long userId, final int ts) {
        lastOnlineCache.put(userId, ts);
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$saveLastOnlineAsync$90(userId, ts);
            }
        });
    }

    public int getLastOnline(long userId) {
        Integer cached = lastOnlineCache.get(userId);
        if (cached != null) {
            return cached;
        }
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT online_ts FROM last_online_users WHERE user_id = ?", new String[]{String.valueOf(userId)});
            try {
                int i = (c.moveToFirst() && !c.isNull(0)) ? c.getInt(0) : 0;
                if (c != null) {
                    c.close();
                }
                if (i > 0) {
                    lastOnlineCache.put(userId, i);
                }
                return i;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("getLastOnline error: %s", e.getMessage());
            return 0;
        }
    }

    public void lambda$saveReadEventAsync$9(long did, int maxId) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO read_events(did, max_id, date) VALUES(?, ?, ?)");
            try {
                st.bindLong(1, did);
                st.bindLong(2, maxId);
                st.bindLong(3, System.currentTimeMillis() / 1000L);
                st.executeInsert();
                if (st != null) {
                    st.close();
                }
            } catch (Throwable th) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("saveReadEvent error: %s", e.getMessage());
        }
    }

    public void saveReadEventAsync(final long did, final int maxId) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$saveReadEventAsync$9(did, maxId);
            }
        });
    }

    public int getReadDate(long did, int mid) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT MIN(date) FROM read_events WHERE did = ? AND max_id >= ?", new String[]{String.valueOf(did), String.valueOf(mid)});
            try {
                int i = (c.moveToFirst() && !c.isNull(0)) ? c.getInt(0) : 0;
                if (c != null) {
                    c.close();
                }
                return i;
            } catch (Throwable th) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("getReadDate error: %s", e.getMessage());
            return 0;
        }
    }

    /* JADX INFO: renamed from: updateShadowban, reason: merged with bridge method [inline-methods] */
    public void lambda$updateShadowbanAsync$10(long userId, boolean hideDialog, boolean hideInGroups) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try {
                ContentValues cv = new ContentValues();
                cv.put("hide_dialog", Integer.valueOf(hideDialog ? 1 : 0));
                cv.put("hide_in_groups", Integer.valueOf(hideInGroups ? 1 : 0));
                db.update("shadowban_users", cv, "user_id = ?", new String[]{String.valueOf(userId)});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Main.log("updateShadowban error: %s", e.getMessage());
            }
        } finally {
            db.endTransaction();
        }
    }

    public void updateShadowbanAsync(final long userId, final boolean hideDialog, final boolean hideInGroups) {
        postToDbThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$updateShadowbanAsync$10(userId, hideDialog, hideInGroups);
            }
        });
    }

    public void clearDatabaseOnly() {
        deletedKeysCache.clear();
        lastOnlineCache.clear();
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try {
                db.delete("deleted_keys", null, null);
                db.delete("message_edits", null, null);
                db.delete("read_events", null, null);
                db.delete("last_online_users", null, null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Main.log("clearDatabaseOnly error: %s", e.getMessage());
            }
        } finally {
            db.endTransaction();
        }
    }

    public void pruneStaleEntries() {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        long cutoff = System.currentTimeMillis() - DELETED_KEYS_TTL_MS;
        try {
            SQLiteStatement st = db.compileStatement("DELETE FROM deleted_keys WHERE ts < ?");
            try {
                st.bindLong(1, cutoff);
                int removed = st.executeUpdateDelete();
                if (removed > 0) {
                    Main.log("pruneStaleEntries: removed %d old deleted_keys entries", Integer.valueOf(removed));
                    deletedKeysCache.clear();
                    loadDeletedKeysCache();
                }
                if (st != null) {
                    st.close();
                }
            } catch (Throwable th) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable th2) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Main.log("pruneStaleEntries error: %s", e.getMessage());
        }
    }

    private static byte[] serializeMessage(TLRPC.Message m) {
        if (m == null) {
            return null;
        }
        SerializedData sd = new SerializedData(m.getObjectSize());
        m.serializeToStream(sd);
        byte[] out = sd.toByteArray();
        sd.cleanup();
        return out;
    }

    private static TLRPC.Message deserializeMessage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        SerializedData sd = new SerializedData(bytes);
        int constructor = sd.readInt32(false);
        TLRPC.Message msg = TLRPC.Message.TLdeserialize(sd, constructor, false);
        sd.cleanup();
        return msg;
    }

    private static String buildPlaceholders(int n) {
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.toString();
    }

    private static String[] buildInArgs(String first, List<Integer> rest) {
        String[] args = new String[rest.size() + 1];
        args[0] = first;
        for (int i = 0; i < rest.size(); i++) {
            args[i + 1] = String.valueOf(rest.get(i));
        }
        return args;
    }

    private static final class Helper extends SQLiteOpenHelper {
        Helper(Context context, String path) {
            super(context, path, (SQLiteDatabase.CursorFactory) null, 9);
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
            try {
                db.enableWriteAheadLogging();
            } catch (Exception e) {
                Main.log("enableWriteAheadLogging error: %s", e.getMessage());
            }
            db.setForeignKeyConstraintsEnabled(true);
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted_keys(did INTEGER NOT NULL,mid INTEGER NOT NULL,ts INTEGER NOT NULL,PRIMARY KEY(did, mid))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_ts ON deleted_keys(ts)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_did ON deleted_keys(did)");
            db.execSQL("CREATE TABLE IF NOT EXISTS message_edits(did INTEGER NOT NULL,mid INTEGER NOT NULL,ver INTEGER NOT NULL,date INTEGER NOT NULL,data BLOB NOT NULL,PRIMARY KEY(did, mid, ver))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
            db.execSQL("CREATE TABLE IF NOT EXISTS exception_users(did INTEGER NOT NULL,exception_reading INTEGER DEFAULT 0 NOT NULL,exception_typing INTEGER DEFAULT 0 NOT NULL,exception_filter INTEGER DEFAULT 0 NOT NULL,PRIMARY KEY(did))");
            db.execSQL("CREATE TABLE IF NOT EXISTS regex_filters(regex TEXT NOT NULL,PRIMARY KEY(regex))");
            db.execSQL("CREATE TABLE IF NOT EXISTS shadowban_users(user_id INTEGER PRIMARY KEY,hide_dialog INTEGER DEFAULT 1,hide_in_groups INTEGER DEFAULT 1,added_ts INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shadowban_added_ts ON shadowban_users(added_ts)");
            db.execSQL("CREATE TABLE IF NOT EXISTS read_events(did INTEGER NOT NULL, max_id INTEGER NOT NULL, date INTEGER NOT NULL, PRIMARY KEY(did, max_id))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_read_events_did ON read_events(did)");
            db.execSQL("CREATE TABLE IF NOT EXISTS last_online_users(user_id INTEGER PRIMARY KEY, online_ts INTEGER NOT NULL)");
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            onCreate(db);
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            if (oldV < 3) {
                db.execSQL("DROP TABLE IF EXISTS message_edits");
                db.execSQL("CREATE TABLE IF NOT EXISTS message_edits(did INTEGER NOT NULL,mid INTEGER NOT NULL,ver INTEGER NOT NULL,date INTEGER NOT NULL,data BLOB NOT NULL,PRIMARY KEY(did, mid, ver))");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
            }
            if (oldV < 4) {
                db.execSQL("CREATE TABLE IF NOT EXISTS exception_users(did INTEGER NOT NULL,exception_reading INTEGER DEFAULT 0 NOT NULL,exception_typing INTEGER DEFAULT 0 NOT NULL,PRIMARY KEY(did))");
            }
            if (oldV < 7) {
                db.execSQL("CREATE TABLE IF NOT EXISTS regex_filters(regex TEXT NOT NULL,PRIMARY KEY(regex))");
            }
            if (oldV < 8) {
                db.execSQL("CREATE TABLE IF NOT EXISTS shadowban_users(user_id INTEGER PRIMARY KEY,hide_dialog INTEGER DEFAULT 1,hide_in_groups INTEGER DEFAULT 1,added_ts INTEGER NOT NULL)");
            }
            if (oldV < 9) {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_ts ON deleted_keys(ts)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_did ON deleted_keys(did)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shadowban_added_ts ON shadowban_users(added_ts)");
            }
            if (oldV < 10) {
                db.execSQL("CREATE TABLE IF NOT EXISTS read_events(did INTEGER NOT NULL, max_id INTEGER NOT NULL, date INTEGER NOT NULL, PRIMARY KEY(did, max_id))");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_read_events_did ON read_events(did)");
            }
            if (oldV < 11) {
                db.execSQL("CREATE TABLE IF NOT EXISTS last_online_users(user_id INTEGER PRIMARY KEY, online_ts INTEGER NOT NULL)");
            }
            if (oldV < 12) {
                try {
                    db.execSQL("ALTER TABLE exception_users ADD COLUMN exception_filter INTEGER DEFAULT 0 NOT NULL");
                } catch (Exception ignore) {}
            }
            if (oldV < 13) {
                try {
                    db.execSQL("ALTER TABLE exception_users ADD COLUMN exception_filter INTEGER DEFAULT 0 NOT NULL");
                } catch (Exception ignore) {}
            }
        }
    }
}
