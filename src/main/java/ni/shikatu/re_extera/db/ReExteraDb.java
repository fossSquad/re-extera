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
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

public class ReExteraDb {
    public static final String DB_NAME = "re_extera.db";
    public static final int DB_VERSION = 7;
    private static ReExteraDb instance = null;
    private final Handler dbHandler;
    private final Object dbLock = new Object();
    private final HandlerThread dbThread;
    private final Helper helper;

    public static synchronized ReExteraDb init(Context context) {
        if (instance == null) {
            instance = new ReExteraDb(context);
        }
        return instance;
    }

    public static ReExteraDb get() {
        return instance;
    }

    public ReExteraDb(Context context) {
        File dbFile = new File(context.getFilesDir(), DB_NAME);
        this.helper = new Helper(context, dbFile.getAbsolutePath());
        this.dbThread = new HandlerThread("re_extera_db");
        this.dbThread.start();
        this.dbHandler = new Handler(this.dbThread.getLooper());
    }

    public void postToDbThread(Runnable r) {
        this.dbHandler.post(r);
    }

    /* JADX INFO: renamed from: putDeletedMessage, reason: merged with bridge method [inline-methods] */
    public void lambda$putDeletedMessageAsync$0(long did, int mid) {
        synchronized (this.dbLock) {
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
    }

    public void putDeletedMessageAsync(final long did, final int mid) {
        postToDbThread(new Runnable() { // from class: ni.shikatu.re_extera.db.ReExteraDb$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$putDeletedMessageAsync$0(did, mid);
            }
        });
    }

    /* JADX INFO: renamed from: batchPutDeletedMessages, reason: merged with bridge method [inline-methods] */
    public void lambda$batchPutDeletedMessagesAsync$1(long did, Collection<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        synchronized (this.dbLock) {
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
                } catch (Throwable th3) {
                    db.endTransaction();
                    throw th3;
                }
            } catch (Exception e) {
                Main.log("batchPutDeletedMessages error: %s", e.getMessage());
                db.endTransaction();
            }
        }
    }

    public void batchPutDeletedMessagesAsync(final long did, Collection<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        final ArrayList<Integer> copy = new ArrayList<>(mids);
        postToDbThread(new Runnable() { // from class: ni.shikatu.re_extera.db.ReExteraDb$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$batchPutDeletedMessagesAsync$1(did, copy);
            }
        });
    }

    public List<DialogExclusion> getActiveExceptions() {
        ArrayList<DialogExclusion> result;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            result = new ArrayList<>();
            try {
                Cursor c = db.rawQuery("SELECT did, exception_reading, exception_typing FROM exception_users WHERE exception_reading != 0 OR exception_typing != 0", new String[0]);
                while (c.moveToNext()) {
                    try {
                        long dialogId = c.getLong(0);
                        int readExclusion = c.getInt(1);
                        int typeExclusion = c.getInt(2);
                        result.add(new DialogExclusion(dialogId, readExclusion, typeExclusion));
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
        }
        return result;
    }

    public DialogExclusion getException(long dialogId) {
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            try {
                Cursor c = db.rawQuery("SELECT did, exception_reading, exception_typing FROM exception_users WHERE did = ?", new String[]{String.valueOf(dialogId)});
                try {
                    if (c.moveToFirst()) {
                        long did = c.getLong(0);
                        int readExclusion = c.getInt(1);
                        int typeExclusion = c.getInt(2);
                        DialogExclusion dialogExclusion = new DialogExclusion(did, readExclusion, typeExclusion);
                        if (c != null) {
                            c.close();
                        }
                        return dialogExclusion;
                    }
                    if (c != null) {
                        c.close();
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
                Main.log("getException error: %s", e.getMessage());
            }
        }
    }

    public boolean messageHasSavedEdits(long did, long mid) {
        boolean z;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            try {
                Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?", new String[]{String.valueOf(did), String.valueOf(mid)});
                try {
                    z = c.moveToFirst() && !c.isNull(0) && c.getInt(0) > 0;
                    if (c != null) {
                        c.close();
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
            } catch (Exception e) {
                Main.log("messageHasSavedEdits error: %s", e.getMessage());
                return false;
            }
        }
        return z;
    }

    public boolean messageHasSavedEdits(MessageObject obj) {
        return messageHasSavedEdits(obj.getDialogId(), obj.getId());
    }

    public List<Long> getDialogIdWithSavedMessages() {
        ArrayList<Long> result;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            result = new ArrayList<>();
            try {
                Cursor c = db.rawQuery("SELECT DISTINCT did FROM deleted_keys", new String[0]);
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
        }
        return result;
    }

    public ArrayList<Integer> allMessageIdsByDid(long did) {
        ArrayList<Integer> result;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            result = new ArrayList<>();
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
        }
        return result;
    }

    public boolean messageIsDeleted(long did, int mid) {
        boolean zMoveToFirst;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            try {
                Cursor c = db.rawQuery("SELECT 1 FROM deleted_keys WHERE did=? AND mid=? LIMIT 1", new String[]{String.valueOf(did), String.valueOf(mid)});
                try {
                    zMoveToFirst = c.moveToFirst();
                    if (c != null) {
                        c.close();
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
            } catch (Exception e) {
                Main.log("messageIsDeleted error: %s", e.getMessage());
                return false;
            }
        }
        return zMoveToFirst;
    }

    public boolean messageIsDeleted(MessageObject msg) {
        return messageIsDeleted(msg.getDialogId(), msg.getId());
    }

    /* JADX INFO: renamed from: saveOriginalMessage, reason: merged with bridge method [inline-methods] */
    public void lambda$saveOriginalMessageAsync$2(long did, int mid, TLRPC.Message message) {
        synchronized (this.dbLock) {
            long when = System.currentTimeMillis();
            SQLiteDatabase db = this.helper.getWritableDatabase();
            try {
                SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, 0, ?, ?)");
                try {
                    st.bindLong(1, did);
                    st.bindLong(2, mid);
                    st.bindLong(3, when);
                    byte[] blob = serializeMessage(message);
                    if (blob == null) {
                        blob = new byte[0];
                    }
                    st.bindBlob(4, blob);
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
    }

    public void saveOriginalMessageAsync(final long did, final int mid, final TLRPC.Message message) {
        postToDbThread(new Runnable() { // from class: ni.shikatu.re_extera.db.ReExteraDb$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$saveOriginalMessageAsync$2(did, mid, message);
            }
        });
    }

    /* JADX INFO: renamed from: saveNewVersionMessage, reason: merged with bridge method [inline-methods] */
    public void lambda$saveNewVersionMessageAsync$3(long did, int mid, TLRPC.Message msg) {
        synchronized (this.dbLock) {
            long when = System.currentTimeMillis();
            SQLiteDatabase db = this.helper.getWritableDatabase();
            db.beginTransaction();
            int nextVer = 1;
            try {
                try {
                    Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?", new String[]{String.valueOf(did), String.valueOf(mid)});
                    try {
                        if (c.moveToFirst()) {
                            int cur = c.isNull(0) ? -1 : c.getInt(0);
                            nextVer = Math.max(1, cur + 1);
                        }
                        if (c != null) {
                            c.close();
                        }
                        SQLiteStatement st = db.compileStatement("INSERT INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, ?, ?, ?)");
                        try {
                            st.bindLong(1, did);
                            st.bindLong(2, mid);
                            st.bindLong(3, nextVer);
                            st.bindLong(4, when);
                            byte[] blob = serializeMessage(msg);
                            if (blob == null) {
                                blob = new byte[0];
                            }
                            st.bindBlob(5, blob);
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
                } catch (Throwable th5) {
                    db.endTransaction();
                    throw th5;
                }
            } catch (Exception e) {
                Main.log("saveNewVersionMessage error: %s", e.getMessage());
                db.endTransaction();
            }
        }
    }

    public void saveNewVersionMessageAsync(final long did, final int mid, final TLRPC.Message msg) {
        postToDbThread(new Runnable() { // from class: ni.shikatu.re_extera.db.ReExteraDb$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$saveNewVersionMessageAsync$3(did, mid, msg);
            }
        });
    }

    public ArrayList<TLRPC.Message> listVersionsOfEditedMessage(long did, int mid) {
        ArrayList<TLRPC.Message> out;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            out = new ArrayList<>();
            try {
                Cursor c = db.rawQuery("SELECT ver, date, data FROM message_edits WHERE did=? AND mid=? ORDER BY ver ASC", new String[]{String.valueOf(did), String.valueOf(mid)});
                while (c.moveToNext()) {
                    try {
                        byte[] blob = c.getBlob(2);
                        TLRPC.Message m = deserializeMessage(blob);
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
        }
        return out;
    }

    public void clearDatabaseOnly() {
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getWritableDatabase();
            db.beginTransaction();
            try {
                try {
                    db.delete("deleted_keys", null, null);
                    db.delete("message_edits", null, null);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } catch (Exception e) {
                    Main.log("clearDatabaseOnly error: %s", e.getMessage());
                    db.endTransaction();
                }
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
    }

    public void clearDatabaseWithInternal() {
        List<Long> dids;
        ArrayList<ArrayList<Integer>> allToDelete = new ArrayList<>();
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            dids = new ArrayList<>();
            try {
                Cursor c = db.rawQuery("SELECT DISTINCT did FROM deleted_keys", new String[0]);
                while (c.moveToNext()) {
                    try {
                        dids.add(Long.valueOf(c.getLong(0)));
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
                Main.log("clearDatabaseWithInternal read error: %s", e.getMessage());
            }
            Iterator<Long> it = dids.iterator();
            while (it.hasNext()) {
                long did = it.next().longValue();
                ArrayList<Integer> midsForDid = new ArrayList<>();
                try {
                    Cursor c2 = db.rawQuery("SELECT mid FROM deleted_keys WHERE did=?", new String[]{String.valueOf(did)});
                    while (c2.moveToNext()) {
                        try {
                            midsForDid.add(Integer.valueOf(c2.getInt(0)));
                        } catch (Throwable th3) {
                            if (c2 != null) {
                                try {
                                    c2.close();
                                } catch (Throwable th4) {
                                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th3, th4);
                                }
                            }
                            throw th3;
                        }
                    }
                    if (c2 != null) {
                        c2.close();
                    }
                } catch (Exception e2) {
                    Main.log("clearDatabaseWithInternal read mids error: %s", e2.getMessage());
                }
                allToDelete.add(midsForDid);
            }
        }
        for (int i = 0; i < dids.size(); i++) {
            long did2 = dids.get(i).longValue();
            ArrayList<Integer> toDelete = allToDelete.get(i);
            if (!toDelete.isEmpty()) {
                InternalUtils.deleteMessages(did2, toDelete, true);
            }
        }
        synchronized (this.dbLock) {
            SQLiteDatabase db2 = this.helper.getWritableDatabase();
            db2.beginTransaction();
            try {
                try {
                    db2.delete("deleted_keys", null, null);
                    db2.delete("message_edits", null, null);
                    db2.setTransactionSuccessful();
                    db2.endTransaction();
                } catch (Exception e3) {
                    Main.log("clearDatabaseWithInternal delete error: %s", e3.getMessage());
                    db2.endTransaction();
                }
            } catch (Throwable th5) {
                db2.endTransaction();
                throw th5;
            }
        }
    }

    public void clearMessages(long did, List<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getWritableDatabase();
            db.beginTransaction();
            try {
                try {
                    Iterator<Integer> it = mids.iterator();
                    while (it.hasNext()) {
                        int mid = it.next().intValue();
                        db.delete("deleted_keys", "did = ? AND mid = ?", new String[]{String.valueOf(did), String.valueOf(mid)});
                    }
                    Iterator<Integer> it2 = mids.iterator();
                    while (it2.hasNext()) {
                        int mid2 = it2.next().intValue();
                        db.delete("message_edits", "did = ? AND mid = ?", new String[]{String.valueOf(did), String.valueOf(mid2)});
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } catch (Throwable th) {
                    db.endTransaction();
                    throw th;
                }
            } catch (Exception e) {
                Main.log("clearMessages error: %s", e.getMessage());
                db.endTransaction();
            }
        }
    }

    public int getDialogReading(long did) {
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            try {
                Cursor c = db.rawQuery("SELECT exception_reading FROM exception_users WHERE did = ?", new String[]{String.valueOf(did)});
                try {
                    if (c.moveToFirst()) {
                        int i = c.getInt(0);
                        if (c != null) {
                            c.close();
                        }
                        return i;
                    }
                    if (c != null) {
                        c.close();
                    }
                    return 0;
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
                Main.log("getDialogReading error: %s", e.getMessage());
                return 0;
            }
        }
    }

    public int getDialogTyping(long did) {
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            try {
                Cursor c = db.rawQuery("SELECT exception_typing FROM exception_users WHERE did = ?", new String[]{String.valueOf(did)});
                try {
                    if (c.moveToFirst()) {
                        int i = c.getInt(0);
                        if (c != null) {
                            c.close();
                        }
                        return i;
                    }
                    if (c != null) {
                        c.close();
                    }
                    return 0;
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
                Main.log("getDialogTyping error: %s", e.getMessage());
                return 0;
            }
        }
    }

    /* JADX INFO: renamed from: setDialogReading, reason: merged with bridge method [inline-methods] */
    public void lambda$setDialogReadingAsync$4(long did, int value) {
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getWritableDatabase();
            db.beginTransaction();
            try {
                try {
                    ContentValues cv = new ContentValues();
                    cv.put("exception_reading", Integer.valueOf(value));
                    int upd = db.update("exception_users", cv, "did = ?", new String[]{String.valueOf(did)});
                    if (upd == 0) {
                        cv.put("did", Long.valueOf(did));
                        cv.put("exception_typing", (Integer) 0);
                        db.insert("exception_users", null, cv);
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } catch (Exception e) {
                    Main.log("setDialogReading error: %s", e.getMessage());
                    db.endTransaction();
                }
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
    }

    public void setDialogReadingAsync(final long did, final int value) {
        postToDbThread(new Runnable() { // from class: ni.shikatu.re_extera.db.ReExteraDb$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$setDialogReadingAsync$4(did, value);
            }
        });
    }

    /* JADX INFO: renamed from: setDialogTyping, reason: merged with bridge method [inline-methods] */
    public void lambda$setDialogTypingAsync$5(long did, int value) {
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getWritableDatabase();
            db.beginTransaction();
            try {
                try {
                    ContentValues cv = new ContentValues();
                    cv.put("exception_typing", Integer.valueOf(value));
                    int upd = db.update("exception_users", cv, "did = ?", new String[]{String.valueOf(did)});
                    if (upd == 0) {
                        cv.put("did", Long.valueOf(did));
                        cv.put("exception_reading", (Integer) 0);
                        db.insert("exception_users", null, cv);
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } catch (Exception e) {
                    Main.log("setDialogTyping error: %s", e.getMessage());
                    db.endTransaction();
                }
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
    }

    public void setDialogTypingAsync(final long did, final int value) {
        postToDbThread(new Runnable() { // from class: ni.shikatu.re_extera.db.ReExteraDb$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$setDialogTypingAsync$5(did, value);
            }
        });
    }

    public void addRegexFilter(String pattern) {
        synchronized (this.dbLock) {
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
    }

    public void updateRegexFilter(String oldPattern, String newPattern) {
        synchronized (this.dbLock) {
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
                    db.endTransaction();
                }
            } catch (Throwable th3) {
                db.endTransaction();
                throw th3;
            }
        }
    }

    public void deleteRegexFilter(String pattern) {
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getWritableDatabase();
            try {
                db.delete("regex_filters", "regex = ?", new String[]{pattern});
            } catch (Exception e) {
                Main.log("deleteRegexFilter error: %s", e.getMessage());
            }
        }
    }

    public List<String> getAllRegexFilters() {
        ArrayList<String> result;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            result = new ArrayList<>();
            try {
                Cursor c = db.rawQuery("SELECT regex FROM regex_filters ORDER BY regex ASC", new String[0]);
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
        }
        return result;
    }

    public boolean hasRegexFilter(String pattern) {
        boolean zMoveToFirst;
        synchronized (this.dbLock) {
            SQLiteDatabase db = this.helper.getReadableDatabase();
            try {
                Cursor c = db.rawQuery("SELECT 1 FROM regex_filters WHERE regex = ? LIMIT 1", new String[]{pattern});
                try {
                    zMoveToFirst = c.moveToFirst();
                    if (c != null) {
                        c.close();
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
            } catch (Exception e) {
                Main.log("hasRegexFilter error: %s", e.getMessage());
                return false;
            }
        }
        return zMoveToFirst;
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
        if (bytes == null) {
            return null;
        }
        SerializedData sd = new SerializedData(bytes);
        int constructor = sd.readInt32(false);
        TLRPC.Message msg = TLRPC.Message.TLdeserialize(sd, constructor, false);
        sd.cleanup();
        return msg;
    }

    private static final class Helper extends SQLiteOpenHelper {
        Helper(Context context, String path) {
            super(context, path, (SQLiteDatabase.CursorFactory) null, 7);
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted_keys(did INTEGER NOT NULL,mid INTEGER NOT NULL,ts INTEGER NOT NULL,PRIMARY KEY(did, mid))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_ts ON deleted_keys(ts)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_did ON deleted_keys(did)");
            db.execSQL("CREATE TABLE IF NOT EXISTS message_edits(did INTEGER NOT NULL,mid INTEGER NOT NULL,ver INTEGER NOT NULL,date INTEGER NOT NULL,data BLOB NOT NULL,PRIMARY KEY(did, mid, ver))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
            db.execSQL("CREATE TABLE IF NOT EXISTS exception_users(did INTEGER NOT NULL,exception_reading INTEGER NOT NULL,exception_typing INTEGER NOT NULL,PRIMARY KEY(did))");
            db.execSQL("CREATE TABLE IF NOT EXISTS regex_filters(regex TEXT NOT NULL,PRIMARY KEY(regex))");
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
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_exception_users_did ON exception_users(did)");
            }
            if (oldV < 7) {
                db.execSQL("CREATE TABLE IF NOT EXISTS regex_filters(regex TEXT NOT NULL,PRIMARY KEY(regex))");
            }
        }
    }
}
