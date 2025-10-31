package ni.shikatu.re_extera;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.HandlerThread;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import ni.shikatu.re_extera.messageobject.MarkMessagesAsDeletedInternalHook;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

public final class DbDeletedStore {
    private static final String DB_NAME = "re_extera.db";
    private static final int DB_VER = 4;
    private static DbDeletedStore sInstance;
    private HandlerThread dbThread = new HandlerThread("reextera_db");
    private final Helper helper;

    public static synchronized DbDeletedStore init(Context ctx) {
        if (sInstance == null) {
            sInstance = new DbDeletedStore(ctx.getApplicationContext());
        }
        return sInstance;
    }

    public static DbDeletedStore get() {
        return sInstance;
    }

    private DbDeletedStore(Context appCtx) {
        File dbFile = new File(appCtx.getFilesDir(), DB_NAME);
        this.helper = new Helper(appCtx, dbFile.getAbsolutePath());
    }

    public void put(long did, int mid) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO deleted_keys(did, mid, ts) VALUES(?, ?, ?)");
        try {
            st.bindLong(1, did);
            st.bindLong(2, mid);
            st.bindLong(3, System.currentTimeMillis());
            st.executeInsert();
        } finally {
            st.close();
        }
    }

    public void clearDeletedMessagesInDialog(long did) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        Global.log("Deleting messages from dialog " + did);
        MarkMessagesAsDeletedInternalHook.removeMessages(getByDid(did), did, 0L, false);
        db.delete("deleted_keys", "did = ?", new String[]{String.valueOf(did)});
    }

    public void batchPut(long did, Collection<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.helper.getWritableDatabase();
        SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO deleted_keys(did, mid, ts) VALUES(?, ?, ?)");
        db.beginTransaction();
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
        } finally {
            db.endTransaction();
            st.close();
        }
    }

    public boolean hasEdits(long did, int mid) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        boolean z = false;
        Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?", new String[]{String.valueOf(did), String.valueOf(mid)});
        try {
            if (c.moveToFirst() && !c.isNull(0) && c.getInt(0) > 0) {
                z = true;
            }
            return z;
        } finally {
            c.close();
        }
    }

    public ArrayList<Long> allDids() {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<Long> result = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT DISTINCT did FROM deleted_keys", new String[0]);
        while (c.moveToNext()) {
            try {
                result.add(Long.valueOf(c.getLong(0)));
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return result;
    }

    public ArrayList<Integer> getByDid(long did) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        ArrayList<Integer> result = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT mid FROM deleted_keys WHERE did=?", new String[]{String.valueOf(did)});
        while (c.moveToNext()) {
            try {
                result.add(Integer.valueOf(c.getInt(0)));
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return result;
    }

    public boolean exists(long did, int mid) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM deleted_keys WHERE did=? AND mid=? LIMIT 1", new String[]{String.valueOf(did), String.valueOf(mid)});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public ArrayList<Integer> listForDialog(long did) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT mid FROM deleted_keys WHERE did=?", new String[]{String.valueOf(did)});
        ArrayList<Integer> out = new ArrayList<>();
        while (c.moveToNext()) {
            try {
                out.add(Integer.valueOf(c.getInt(0)));
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return out;
    }

    public int cleanupOlderThanDays(int days) {
        long cutoff = System.currentTimeMillis() - ((((((long) days) * 24) * 60) * 60) * 1000);
        SQLiteDatabase db = this.helper.getWritableDatabase();
        return db.delete("deleted_keys", "ts < ?", new String[]{String.valueOf(cutoff)});
    }

    public void saveOriginalIfAbsent(long did, int mid, TLRPC.Message msg, long when) {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        SQLiteStatement st = db.compileStatement("INSERT OR IGNORE INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, 0, ?, ?)");
        try {
            st.bindLong(1, did);
            st.bindLong(2, mid);
            st.bindLong(3, when);
            byte[] blob = serializeMessage(msg);
            if (blob == null) {
                blob = new byte[0];
            }
            st.bindBlob(DB_VER, blob);
            st.executeInsert();
        } finally {
            st.close();
        }
    }

    public void appendEdit(long did, int mid, TLRPC.Message msg, long when) {
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
                c.close();
                SQLiteStatement st = db.compileStatement("INSERT INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, ?, ?, ?)");
                try {
                    st.bindLong(1, did);
                    st.bindLong(2, mid);
                    st.bindLong(3, nextVer);
                    st.bindLong(DB_VER, when);
                    byte[] blob = serializeMessage(msg);
                    if (blob == null) {
                        blob = new byte[0];
                    }
                    st.bindBlob(5, blob);
                    st.executeInsert();
                    st.close();
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } catch (Throwable th) {
                    st.close();
                    throw th;
                }
            } catch (Throwable th2) {
                c.close();
                throw th2;
            }
        } catch (Throwable th3) {
            db.endTransaction();
            throw th3;
        }
    }

    public ArrayList<TLRPC.Message> listEdits(long did, int mid) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT ver, date, data FROM message_edits WHERE did=? AND mid=? ORDER BY ver ASC", new String[]{String.valueOf(did), String.valueOf(mid)});
        ArrayList<TLRPC.Message> out = new ArrayList<>();
        while (c.moveToNext()) {
            try {
                byte[] blob = c.getBlob(2);
                TLRPC.Message m = deserializeMessage(blob);
                if (m != null) {
                    out.add(m);
                }
            } finally {
                c.close();
            }
        }
        return out;
    }

    public void clearAll() {
        SQLiteDatabase db = this.helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ArrayList<Long> dids = allDids();
            Iterator<Long> it = dids.iterator();
            while (it.hasNext()) {
                long did = it.next().longValue();
                Global.log("Deleting messages from dialog " + did);
                MarkMessagesAsDeletedInternalHook.removeMessages(getByDid(did), did, 0L, false);
            }
            Global.log("deleting from deleted_keys");
            db.delete("deleted_keys", null, null);
            Global.log("deleting from message_edits");
            db.delete("message_edits", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
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

    public int getDialogReading(long did) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT exception_reading FROM exception_users WHERE did = ?", new String[]{String.valueOf(did)});
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
            return 0;
        } finally {
            if (c != null) {
                c.close();
            }
            db.close();
        }
    }

    public int getDialogTyping(long did) {
        SQLiteDatabase db = this.helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT exception_typing FROM exception_users WHERE did = ?", new String[]{String.valueOf(did)});
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
            return 0;
        } finally {
            if (c != null) {
                c.close();
            }
            db.close();
        }
    }

    public void setDialogReading(long did, int value) {
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
        } finally {
            db.close();
        }
    }

    public void setDialogTyping(long did, int value) {
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
        } finally {
            db.close();
        }
    }

    private static final class Helper extends SQLiteOpenHelper {
        Helper(Context ctx, String path) {
            super(ctx, path, (SQLiteDatabase.CursorFactory) null, DbDeletedStore.DB_VER);
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted_keys(did INTEGER NOT NULL,mid INTEGER NOT NULL,ts INTEGER NOT NULL,PRIMARY KEY(did, mid))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_ts ON deleted_keys(ts)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_did ON deleted_keys(did)");
            db.execSQL("CREATE TABLE IF NOT EXISTS message_edits(did INTEGER NOT NULL,mid INTEGER NOT NULL,ver INTEGER NOT NULL,date INTEGER NOT NULL,data BLOB NOT NULL,PRIMARY KEY(did, mid, ver))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
            db.execSQL("CREATE TABLE IF NOT EXISTS exception_users(did INTEGER NOT NULL,exception_reading INTEGER NOT NULL,exception_typing INTEGER NOT NULL,PRIMARY KEY(did))");
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            if (oldV < 3) {
                db.execSQL("DROP TABLE IF EXISTS message_edits");
                db.execSQL("CREATE TABLE IF NOT EXISTS message_edits(did INTEGER NOT NULL,mid INTEGER NOT NULL,ver INTEGER NOT NULL,date INTEGER NOT NULL,data BLOB NOT NULL,PRIMARY KEY(did, mid, ver))");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
            }
            if (oldV < DbDeletedStore.DB_VER) {
                db.execSQL("CREATE TABLE IF NOT EXISTS exception_users(did INTEGER NOT NULL,exception_reading INTEGER DEFAULT 0 NOT NULL,exception_typing INTEGER DEFAULT 0 NOT NULL,PRIMARY KEY(did))");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_exception_users_did ON exception_users(did)");
            }
        }
    }
}
