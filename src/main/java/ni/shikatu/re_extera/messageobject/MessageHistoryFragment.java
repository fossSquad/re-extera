package ni.shikatu.re_extera.messageobject;

import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Localization;
import ni.shikatu.re_extera.Utils;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BulletinFactory;

public class MessageHistoryFragment extends BaseFragment {
    private HistoryAdapter adapter;
    private long did;
    private RecyclerView list;
    private int mid;

    public static MessageHistoryFragment newInstance(long did, int mid) {
        MessageHistoryFragment f = new MessageHistoryFragment();
        Bundle b = new Bundle();
        b.putLong("did", did);
        b.putInt("mid", mid);
        f.arguments = b;
        return f;
    }

    public boolean onFragmentCreate() {
        Bundle args = getArguments();
        if (args != null) {
            this.did = args.getLong("did");
            this.mid = args.getInt("mid");
        }
        return super.onFragmentCreate();
    }

    public View createView(final Context context) {
        this.actionBar.setTitle(Localization.MESSAGE_HISTORY_TITLE);
        RecyclerView rv = new RecyclerView(context);
        rv.setLayoutManager(new LinearLayoutManager(context));
        this.adapter = new HistoryAdapter(context, this.did, this.mid, getAccountInstance());
        rv.setAdapter(this.adapter);
        this.list = rv;
        rv.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() { // from class: ni.shikatu.re_extera.messageobject.MessageHistoryFragment.1
            private final GestureDetector gestureDetector;

            {
                this.gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() { // from class: ni.shikatu.re_extera.messageobject.MessageHistoryFragment.1.1
                    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
                    public void onLongPress(MotionEvent e) {
                        int position;
                        MessageObject messageObject;
                        View childView = MessageHistoryFragment.this.list.findChildViewUnder(e.getX(), e.getY());
                        if ((childView instanceof ChatMessageCell) && (position = MessageHistoryFragment.this.list.getChildAdapterPosition(childView)) != -1 && (messageObject = MessageHistoryFragment.this.adapter.getItem(position)) != null) {
                            childView.performHapticFeedback(0);
                            long savedMessagesDid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                            Utils.sendMessageCopy(MessageHistoryFragment.this.getAccountInstance(), new ArrayList(Collections.singletonList(messageObject)), savedMessagesDid, true, 0, null);
                            BulletinFactory.of(MessageHistoryFragment.this).createSimpleBulletin(ContextCompat.getDrawable(context, R.drawable.chats_saved), Localization.FORWARDED_TO_SAVED_MESSAGES).show();
                        }
                    }
                });
            }

            public boolean onInterceptTouchEvent(RecyclerView rv2, MotionEvent e) {
                this.gestureDetector.onTouchEvent(e);
                return false;
            }

            public void onTouchEvent(RecyclerView rv2, MotionEvent e) {
            }

            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });
        this.fragmentView = rv;
        this.fragmentView.post(new Runnable() { // from class: ni.shikatu.re_extera.messageobject.MessageHistoryFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.m13x5598aac();
            }
        });
        return this.fragmentView;
    }

    /* JADX INFO: renamed from: lambda$createView$0$ni-shikatu-re_extera-messageobject-MessageHistoryFragment, reason: not valid java name */
    /* synthetic */ void m13x5598aac() {
        this.adapter.reload();
    }

    public void onResume() {
        super.onResume();
        if (this.fragmentView != null) {
            this.fragmentView.post(new Runnable() { // from class: ni.shikatu.re_extera.messageobject.MessageHistoryFragment$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.m14x2647b558();
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$onResume$1$ni-shikatu-re_extera-messageobject-MessageHistoryFragment, reason: not valid java name */
    /* synthetic */ void m14x2647b558() {
        this.adapter.reload();
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<CellVH> {
        private final Context ctx;
        private final long did;
        private final int mid;
        private final ArrayList<MessageObject> versionRows = new ArrayList<>();

        HistoryAdapter(Context ctx, long did, int mid, AccountInstance accountInstance) {
            this.ctx = ctx;
            this.did = did;
            this.mid = mid;
        }

        public MessageObject getItem(int position) {
            if (this.versionRows != null && position >= 0 && position < this.versionRows.size()) {
                return this.versionRows.get(position);
            }
            return null;
        }

        void reload() {
            this.versionRows.clear();
            ArrayList<TLRPC.Message> versions = DbDeletedStore.get().listEdits(this.did, this.mid);
            if (!versions.isEmpty()) {
                for (TLRPC.Message m : versions) {
                    if (m.edit_date != 0) {
                        m.date = m.edit_date;
                    }
                    this.versionRows.add(new MessageObject(UserConfig.selectedAccount, m, false, false));
                }
            }
            notifyDataSetChanged();
        }

        public CellVH onCreateViewHolder(ViewGroup parent, int viewType) {
            ChatMessageCell cell = new ChatMessageCell(this.ctx, UserConfig.selectedAccount);
            cell.setAllowAssistant(false);
            return new CellVH(cell);
        }

        public void onBindViewHolder(CellVH holder, int position) {
            MessageObject mo = this.versionRows.get(position);
            holder.bind(mo);
        }

        public int getItemCount() {
            return this.versionRows.size();
        }

        static class CellVH extends RecyclerView.ViewHolder {
            private final ChatMessageCell cell;

            CellVH(View itemView) {
                super(itemView);
                this.cell = (ChatMessageCell) itemView;
            }

            void bind(MessageObject mo) {
                try {
                    mo.resetLayout();
                    try {
                        this.cell.setMessageObject(mo, (MessageObject.GroupedMessages) null, false, false, false);
                        this.cell.invalidate();
                        this.cell.requestLayout();
                    } catch (Throwable th) {
                    }
                } catch (Throwable th2) {
                }
            }
        }
    }
}
