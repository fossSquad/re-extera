package ni.shikatu.re_extera;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;

import java.util.ArrayList;

public class MessageHistoryFragment extends BaseFragment {

	private long did;
	private int mid;

	private RecyclerView list;
	private HistoryAdapter adapter;

	public static MessageHistoryFragment newInstance(long did, int mid) {
		MessageHistoryFragment f = new MessageHistoryFragment();
		Bundle b = new Bundle();
		b.putLong("did", did);
		b.putInt("mid", mid);
		f.arguments = b;
		return f;
	}

	@Override
	public boolean onFragmentCreate() {
		Bundle args = getArguments();
		if (args != null) {
			did = args.getLong("did");
			mid = args.getInt("mid");
		}
		return super.onFragmentCreate();
	}

	@Override
	public View createView(Context context) {
		actionBar.setTitle(Localization.MESSAGE_HISTORY_TITLE);
		RecyclerView rv = new RecyclerView(context);
		rv.setLayoutManager(new LinearLayoutManager(context));
		adapter = new HistoryAdapter(context, did, mid);
		rv.setAdapter(adapter);
		fragmentView = rv;
		list = rv;
		fragmentView.post(() -> adapter.reload());
		return fragmentView;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (fragmentView != null) {
			fragmentView.post(() -> adapter.reload());
		}
	}

	private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.CellVH> {

		private final Context ctx;
		private final long did;
		private final int mid;

		private final ArrayList<MessageObject> versionRows = new ArrayList<>();

		HistoryAdapter(Context ctx, long did, int mid) {
			this.ctx = ctx;
			this.did = did;
			this.mid = mid;
		}

		void reload() {
			versionRows.clear();
			ArrayList<TLRPC.Message> versions = DbDeletedStore.get().listEdits(did, mid);
			if (!versions.isEmpty()) {
				for (TLRPC.Message m : versions) {
					if(m.edit_date != 0){
						m.date = m.edit_date;
					}
					versionRows.add(new MessageObject(UserConfig.selectedAccount, m, false, false));
				}
			}
			notifyDataSetChanged();
		}

		@Override
		public CellVH onCreateViewHolder(ViewGroup parent, int viewType) {
			ChatMessageCell cell = new ChatMessageCell(ctx, UserConfig.selectedAccount);
			cell.setAllowAssistant(false);
			cell.setDelegate(null);
			//cell.setDrawAvatar(false);
			//cell.setNeedReplyButton(false);
			//cell.setNameVisible(false);
			return new CellVH(cell);
		}

		@Override
		public void onBindViewHolder(CellVH holder, int position) {
			MessageObject mo = versionRows.get(position);
			holder.bind(mo);
		}

		@Override
		public int getItemCount() {
			return versionRows.size();
		}

		static class CellVH extends RecyclerView.ViewHolder {
			private final ChatMessageCell cell;

			CellVH(View itemView) {
				super(itemView);
				this.cell = (ChatMessageCell) itemView;
			}

			void bind(MessageObject mo) {
				try {
					//cell.setDrawAvatar(false);
					//cell.setNeedReplyButton(false);
					//cell.setNameVisible(false);
					mo.resetLayout();
					cell.setMessageObject(mo, null, false, false, false);
					cell.invalidate();
					cell.requestLayout();
				} catch (Throwable ignored) {}
			}
		}

		private static MessageObject findOrBuildBase(long did, int mid) {
			TLRPC.Message restored = buildMinimalMessage(did, mid);
			if (restored != null) {
				return new MessageObject(UserConfig.selectedAccount, restored, false, false);
			}
			return null;
		}

		private static TLRPC.Message buildMinimalMessage(long did, int mid) {
			try {
				TLRPC.TL_message m = new TLRPC.TL_message();
				m.id = mid;
				m.dialog_id = did;
				m.date = (int) (System.currentTimeMillis() / 1000L);
				m.edit_date = 1;
				m.flags = 0;
				m.message = "";
				TLRPC.TL_peerUser pu = new TLRPC.TL_peerUser();
				if (did > 0) {
					pu.user_id = (int) did;
					m.peer_id = pu;
				} else {
					int cid = (int) (-did);
					TLRPC.TL_peerChannel pc = new TLRPC.TL_peerChannel();
					pc.channel_id = cid;
					m.peer_id = pc;
				}
				return m;
			} catch (Throwable t) {
				return null;
			}
		}

		private static MessageObject cloneWithText(MessageObject base, String text, int ver) {
			try {
				TLRPC.Message src = base.messageOwner;
				TLRPC.Message m = new TLRPC.TL_message();
				m.id = src.id;
				m.dialog_id = src.dialog_id;
				m.date = src.date;
				m.edit_date = 1;
				m.from_id = src.from_id;
				m.peer_id = src.peer_id;
				m.fwd_from = src.fwd_from;
				m.via_bot_id = src.via_bot_id;
				m.flags = src.flags;
				m.out = src.out;
				m.mentioned = src.mentioned;
				m.media_unread = src.media_unread;
				m.silent = src.silent;
				m.post = src.post;
				m.legacy = src.legacy;
				m.edit_date = src.edit_date;
				m.message = text;
				m.media = src.media;
				m.reply_to = src.reply_to;
				m.entities = src.entities;
				m.views = src.views;

				MessageObject clone = new MessageObject(UserConfig.selectedAccount, m, false, false);
				clone.resetLayout();
				return clone;
			} catch (Throwable t) {
				return base;
			}
		}
	}
}
