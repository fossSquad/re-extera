package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.RestrictedMessageUtils;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SavedMessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.Bulletin;

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
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(true);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment.1
            public void onItemClick(int id) {
                if (id == -1) {
                    MessageHistoryFragment.this.finishFragment();
                }
            }
        });
        this.actionBar.setTitle(Localization.MESSAGE_HISTORY_TITLE);
        FrameLayout frameLayout = new FrameLayout(context);
        RecyclerView rv = new RecyclerView(context);
        rv.setLayoutManager(new LinearLayoutManager(context));
        this.adapter = new HistoryAdapter(context, this.did, this.mid, getAccountInstance());
        rv.setAdapter(this.adapter);
        frameLayout.addView(rv);
        this.list = rv;
        rv.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment.2
            private final GestureDetector gestureDetector;

            {
                this.gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment.2.1
                    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
                    public void onLongPress(MotionEvent e) {
                        int position;
                        MessageObject messageObject;
                        View childView = MessageHistoryFragment.this.list.findChildViewUnder(e.getX(), e.getY());
                        if ((childView instanceof ChatMessageCell) && (position = MessageHistoryFragment.this.list.getChildAdapterPosition(childView)) != -1 && (messageObject = MessageHistoryFragment.this.adapter.getItem(position)) != null) {
                            childView.performHapticFeedback(0);
                            RestrictedMessageUtils.createMenu(MessageHistoryFragment.this, childView, messageObject);
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
        this.fragmentView = frameLayout;
        this.fragmentView.post(new Runnable() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() throws IllegalAccessException, InvocationTargetException {
                this.f$0.lambda$createView$0();
            }
        });
        return this.fragmentView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createView$0() throws IllegalAccessException, InvocationTargetException {
        this.adapter.reload();
    }

    public static Bulletin createSavedMessagesBulletin(Context context, BaseFragment fragment, FrameLayout containerLayout, int messagesCount, Runnable undoAction, final Runnable delayedAction) {
        CharSequence text;
        final Bulletin.LottieLayout layout = new Bulletin.LottieLayout(context, fragment != null ? fragment.getResourceProvider() : null);
        final boolean[] isCanceled = {false};
        Runnable delayedActionOnce = delayedAction != null ? new Runnable() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MessageHistoryFragment.lambda$createSavedMessagesBulletin$1(isCanceled, delayedAction);
            }
        } : null;
        if (messagesCount <= 1) {
            text = AndroidUtilities.replaceSingleTag(LocaleController.getString(R.string.FwdMessageToSavedMessages), -1, 2, new Runnable() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SavedMessagesController.openSavedMessages();
                }
            });
        } else {
            text = AndroidUtilities.replaceSingleTag(LocaleController.getString(R.string.FwdMessagesToSavedMessages), -1, 2, new Runnable() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SavedMessagesController.openSavedMessages();
                }
            });
        }
        layout.setAnimation(R.raw.saved_messages, 30, 30, new String[0]);
        layout.textView.setText(text);
        if (undoAction != null || delayedAction != null) {
            layout.setButton(new Bulletin.UndoButton(context, true, true, fragment != null ? fragment.getResourceProvider() : null).setUndoAction(undoAction).setDelayedAction(delayedActionOnce));
        }
        layout.postDelayed(new Runnable() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                layout.performHapticFeedback(3, 2);
            }
        }, 300L);
        if (containerLayout != null) {
            return Bulletin.make(containerLayout, layout, 2000);
        }
        if (fragment != null) {
            return Bulletin.make(fragment, layout, 2000);
        }
        throw new IllegalArgumentException("At least fragment or containerLayout must be not null");
    }

    static /* synthetic */ void lambda$createSavedMessagesBulletin$1(boolean[] isCanceled, Runnable delayedAction) {
        if (!isCanceled[0]) {
            isCanceled[0] = true;
            delayedAction.run();
        }
    }

    public void onResume() {
        super.onResume();
        if (this.fragmentView != null) {
            this.fragmentView.post(new Runnable() { // from class: ni.shikatu.re_extera.ui.MessageHistoryFragment$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() throws IllegalAccessException, InvocationTargetException {
                    this.f$0.lambda$onResume$3();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onResume$3() throws IllegalAccessException, InvocationTargetException {
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

        void reload() throws IllegalAccessException, InvocationTargetException {
            this.versionRows.clear();
            ArrayList<TLRPC.Message> versions = ReExteraDb.get().listVersionsOfEditedMessage(this.did, this.mid);
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
