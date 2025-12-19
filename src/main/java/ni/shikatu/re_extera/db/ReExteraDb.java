package ni.shikatu.re_extera.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.HandlerThread;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

public class ReExteraDb {
    public static final String DB_NAME = "re_extera.db";
    public static final int DB_VERSION = 7;
    private static ReExteraDb instance = null;
    private HandlerThread dbThread = new HandlerThread("re_extera_db");
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
    }

    public void putDeletedMessage(long did, int mid) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getWritableDatabase();
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
    }

    public void batchPutDeletedMessages(long did, Collection<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO deleted_keys(did, mid, ts) VALUES(?, ?, ?)");
            try {
                db.beginTransaction();
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
    }

    public List<DialogExclusion> getActiveExceptions() throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<DialogExclusion> result = new ArrayList<>();
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
        return result;
    }

    public DialogExclusion getException(long dialogId) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
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
    }

    public boolean messageHasSavedEdits(long did, long mid) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        boolean z = false;
        Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?", new String[]{String.valueOf(did), String.valueOf(mid)});
        try {
            if (c.moveToFirst() && !c.isNull(0) && c.getInt(0) > 0) {
                z = true;
            }
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
    }

    public boolean messageHasSavedEdits(MessageObject obj) {
        return messageHasSavedEdits(obj.getDialogId(), obj.getId());
    }

    public List<Long> getDialogIdWithSavedMessages() throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<Long> result = new ArrayList<>();
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
        return result;
    }

    public ArrayList<Integer> allMessageIdsByDid(long did) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<Integer> result = new ArrayList<>();
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
        return result;
    }

    public boolean messageIsDeleted(long did, int mid) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM deleted_keys WHERE did=? AND mid=? LIMIT 1", new String[]{String.valueOf(did), String.valueOf(mid)});
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
    }

    public boolean messageIsDeleted(MessageObject msg) {
        return messageIsDeleted(msg.getDialogId(), msg.getId());
    }

    public void saveOriginalMessage(long did, int mid, TLRPC.Message message) throws IllegalAccessException, InvocationTargetException {
        long when = System.currentTimeMillis();
        SQLiteDatabase db = this.helper.getWritableDatabase();
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
    }

    public void saveNewVersionMessage(long did, int mid, TLRPC.Message msg) {
        long when = System.currentTimeMillis();
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        int nextVer = 1;
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
    }

    public ArrayList<TLRPC.Message> listVersionsOfEditedMessage(long did, int mid) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<TLRPC.Message> out = new ArrayList<>();
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
        return out;
    }

    public void clearDatabaseOnly() {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("deleted_keys", null, null);
            db.delete("message_edits", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearDatabaseWithInternal() {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            List<Long> dids = getDialogIdWithSavedMessages();
            Iterator<Long> it = dids.iterator();
            while (it.hasNext()) {
                long did = it.next().longValue();
                ArrayList<Integer> toDelete = allMessageIdsByDid(did);
                InternalUtils.deleteMessages(did, toDelete, true);
            }
            db.delete("deleted_keys", null, null);
            db.delete("message_edits", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearMessages(long did, List<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
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
        } finally {
            db.endTransaction();
        }
    }

    /* JADX WARN: Code duplicated, block: B:35:0x0046 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    public int getDialogReading(long did) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT exception_reading FROM exception_users WHERE did = ?", new String[]{String.valueOf(did)});
            try {
                if (c.moveToFirst()) {
                    int i = c.getInt(0);
                    if (c != null) {
                        c.close();
                    }
                    if (db != null) {
                        db.close();
                    }
                    return i;
                }
                if (c != null) {
                    c.close();
                }
                if (db != null) {
                    db.close();
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
        } catch (Throwable th3) {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable th4) {
                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th3, th4);
                }
            }
            throw th3;
        }
        if (db != null) {
            db.close();
        }
        throw th3;
    }

    /* JADX WARN: Code duplicated, block: B:35:0x0046 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    public int getDialogTyping(long did) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT exception_typing FROM exception_users WHERE did = ?", new String[]{String.valueOf(did)});
            try {
                if (c.moveToFirst()) {
                    int i = c.getInt(0);
                    if (c != null) {
                        c.close();
                    }
                    if (db != null) {
                        db.close();
                    }
                    return i;
                }
                if (c != null) {
                    c.close();
                }
                if (db != null) {
                    db.close();
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
        } catch (Throwable th3) {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable th4) {
                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th3, th4);
                }
            }
            throw th3;
        }
        if (db != null) {
            db.close();
        }
        throw th3;
    }

    public void setDialogReading(long did, int value) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("exception_reading", Integer.valueOf(value));
            int upd = db.update("exception_users", cv, "did = ?", new String[]{String.valueOf(did)});
            if (upd == 0) {
                cv.put("did", Long.valueOf(did));
                cv.put("exception_typing", (Integer) 0);
                db.insert("exception_users", null, cv);
            }
            if (db != null) {
                db.close();
            }
        } catch (Throwable th) {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable th2) {
                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                }
            }
            throw th;
        }
    }

    public void setDialogTyping(long did, int value) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put("exception_typing", Integer.valueOf(value));
            int upd = db.update("exception_users", cv, "did = ?", new String[]{String.valueOf(did)});
            if (upd == 0) {
                cv.put("did", Long.valueOf(did));
                cv.put("exception_reading", (Integer) 0);
                db.insert("exception_users", null, cv);
            }
            if (db != null) {
                db.close();
            }
        } catch (Throwable th) {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable th2) {
                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                }
            }
            throw th;
        }
    }

    public void addRegexFilter(String pattern) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getWritableDatabase();
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
    }

    public void updateRegexFilter(String oldPattern, String newPattern) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
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
        } catch (Throwable th3) {
            db.endTransaction();
            throw th3;
        }
    }

    public void deleteRegexFilter(String pattern) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.delete("regex_filters", "regex = ?", new String[]{pattern});
    }

    public List<String> getAllRegexFilters() throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<String> result = new ArrayList<>();
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
        return result;
    }

    public boolean hasRegexFilter(String pattern) throws IllegalAccessException, InvocationTargetException {
        SQLiteDatabase db = this.helper.getReadableDatabase();
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
