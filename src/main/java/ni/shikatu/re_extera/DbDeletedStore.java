package ni.shikatu.re_extera;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public final class DbDeletedStore {

	private static final String DB_NAME = "re_extera.db";
	private static final int DB_VER = 3;

	private static DbDeletedStore sInstance;

	public static synchronized DbDeletedStore init(Context ctx) {
		if (sInstance == null) {
			sInstance = new DbDeletedStore(ctx.getApplicationContext());
		}
		return sInstance;
	}

	public static DbDeletedStore get() {
		return sInstance;
	}

	private final Helper helper;

	private DbDeletedStore(Context appCtx) {
		File dbFile = new File(appCtx.getFilesDir(), DB_NAME);
		this.helper = new Helper(appCtx, dbFile.getAbsolutePath());
	}

	public void put(long did, int mid) {
		SQLiteDatabase db = helper.getWritableDatabase();
		SQLiteStatement st = db.compileStatement(
				"INSERT OR IGNORE INTO deleted_keys(did, mid, ts) VALUES(?, ?, ?)");
		try {
			st.bindLong(1, did);
			st.bindLong(2, mid);
			st.bindLong(3, System.currentTimeMillis());
			st.executeInsert();
		} finally {
			st.close();
		}
	}

	public void batchPut(long did, Collection<Integer> mids) {
		if (mids == null || mids.isEmpty()) return;
		SQLiteDatabase db = helper.getWritableDatabase();
		SQLiteStatement st = db.compileStatement(
				"INSERT OR IGNORE INTO deleted_keys(did, mid, ts) VALUES(?, ?, ?)");
		db.beginTransaction();
		try {
			long now = System.currentTimeMillis();
			for (int mid : mids) {
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
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?",
				new String[]{ String.valueOf(did), String.valueOf(mid) });
		try { return c.moveToFirst() && !c.isNull(0) && c.getInt(0) > 0; }
		finally { c.close(); }
	}

	public boolean exists(long did, int mid) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.rawQuery(
				"SELECT 1 FROM deleted_keys WHERE did=? AND mid=? LIMIT 1",
				new String[]{ String.valueOf(did), String.valueOf(mid) });
		try {
			return c.moveToFirst();
		} finally {
			c.close();
		}
	}

	public ArrayList<Integer> listForDialog(long did) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.rawQuery(
				"SELECT mid FROM deleted_keys WHERE did=?",
				new String[]{ String.valueOf(did) });
		ArrayList<Integer> out = new ArrayList<>();
		try {
			while (c.moveToNext()) {
				out.add(c.getInt(0));
			}
		} finally {
			c.close();
		}
		return out;
	}

	public int cleanupOlderThanDays(int days) {
		long cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L;
		SQLiteDatabase db = helper.getWritableDatabase();
		return db.delete("deleted_keys", "ts < ?", new String[]{ String.valueOf(cutoff) });
	}

	public void saveOriginalIfAbsent(long did, int mid, TLRPC.Message msg, long when) {
		SQLiteDatabase db = helper.getWritableDatabase();
		SQLiteStatement st = db.compileStatement(
				"INSERT OR IGNORE INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, 0, ?, ?)");
		try {
			st.bindLong(1, did);
			st.bindLong(2, mid);
			st.bindLong(3, when);
			byte[] blob = serializeMessage(msg);
			if (blob == null) blob = new byte[0];
			st.bindBlob(4, blob);
			st.executeInsert();
		} finally {
			st.close();
		}
	}

	public void appendEdit(long did, int mid, TLRPC.Message msg, long when) {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			int nextVer = 1;
			Cursor c = db.rawQuery("SELECT MAX(ver) FROM message_edits WHERE did=? AND mid=?",
					new String[]{ String.valueOf(did), String.valueOf(mid) });
			try {
				if (c.moveToFirst()) {
					int cur = c.isNull(0) ? -1 : c.getInt(0);
					nextVer = Math.max(1, cur + 1);
				}
			} finally {
				c.close();
			}
			SQLiteStatement st = db.compileStatement(
					"INSERT INTO message_edits(did, mid, ver, date, data) VALUES(?, ?, ?, ?, ?)");
			try {
				st.bindLong(1, did);
				st.bindLong(2, mid);
				st.bindLong(3, nextVer);
				st.bindLong(4, when);
				byte[] blob = serializeMessage(msg);
				if (blob == null) blob = new byte[0];
				st.bindBlob(5, blob);
				st.executeInsert();
			} finally {
				st.close();
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public ArrayList<TLRPC.Message> listEdits(long did, int mid) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.rawQuery(
				"SELECT ver, date, data FROM message_edits WHERE did=? AND mid=? ORDER BY ver ASC",
				new String[]{ String.valueOf(did), String.valueOf(mid) });
		ArrayList<TLRPC.Message> out = new ArrayList<>();
		try {
			while (c.moveToNext()) {
				byte[] blob = c.getBlob(2);
				TLRPC.Message m = deserializeMessage(blob);
				if (m != null) out.add(m);
			}
		} finally {
			c.close();
		}
		return out;
	}

	public void clearAll() {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete("deleted_keys", null, null);
			db.delete("message_edits", null, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private static byte[] serializeMessage(TLRPC.Message m) {
		if (m == null) return null;
		SerializedData sd = new SerializedData(m.getObjectSize());
		m.serializeToStream(sd); // пишет конструктор и поля
		byte[] out = sd.toByteArray();
		sd.cleanup();
		return out;
	}

	private static TLRPC.Message deserializeMessage(byte[] bytes) {
		if (bytes == null) return null;
		SerializedData sd = new SerializedData(bytes);
		int constructor = sd.readInt32(false);
		TLRPC.Message msg = TLRPC.Message.TLdeserialize(sd, constructor, false);
		sd.cleanup();
		return msg;
	}


	private static final class Helper extends SQLiteOpenHelper {
		Helper(Context ctx, String path) {
			super(ctx, path, null, DB_VER);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS deleted_keys(" +
					"did INTEGER NOT NULL," +
					"mid INTEGER NOT NULL," +
					"ts INTEGER NOT NULL," +
					"PRIMARY KEY(did, mid))");
			db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_ts ON deleted_keys(ts)");
			db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_did ON deleted_keys(did)");
			// новое: message_edits с BLOB
			db.execSQL("CREATE TABLE IF NOT EXISTS message_edits(" +
					"did INTEGER NOT NULL," +
					"mid INTEGER NOT NULL," +
					"ver INTEGER NOT NULL," +
					"date INTEGER NOT NULL," +
					"data BLOB NOT NULL," +
					"PRIMARY KEY(did, mid, ver))");
			db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
			if (oldV < 3) {
				db.execSQL("DROP TABLE IF EXISTS message_edits");
				db.execSQL("CREATE TABLE IF NOT EXISTS message_edits(" +
						"did INTEGER NOT NULL," +
						"mid INTEGER NOT NULL," +
						"ver INTEGER NOT NULL," +
						"date INTEGER NOT NULL," +
						"data BLOB NOT NULL," +
						"PRIMARY KEY(did, mid, ver))");
				db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_did_mid ON message_edits(did, mid)");
			}
		}

	}
}
