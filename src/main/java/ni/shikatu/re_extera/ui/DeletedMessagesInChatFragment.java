package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.TextSelectionHelper;
import org.telegram.ui.PinchToZoomHelper;
import org.telegram.ui.ProfileActivity;

public class DeletedMessagesInChatFragment extends BaseFragment implements ChatMessageCell.ChatMessageCellDelegate {
    private DeletedAdapter adapter;
    private long did;
    private RecyclerView list;

    public static DeletedMessagesInChatFragment newInstance(long did) {
        DeletedMessagesInChatFragment f = new DeletedMessagesInChatFragment();
        Bundle b = new Bundle();
        b.putLong("did", did);
        f.arguments = b;
        return f;
    }

    public boolean onFragmentCreate() {
        Bundle args = getArguments();
        if (args != null) {
            this.did = args.getLong("did");
        }
        return super.onFragmentCreate();
    }

    public void didPressUserAvatar(ChatMessageCell cell, TLRPC.User user, float touchX, float touchY, boolean asForward) {
        if (user != null && user.id != UserConfig.getInstance(this.currentAccount).getClientUserId()) {
            Bundle args = new Bundle();
            args.putLong("user_id", user.id);
            presentFragment(new ProfileActivity(args));
        }
    }

    public String getAdminRank(long uid) {
        return null;
    }

    public TextSelectionHelper.ChatListTextSelectionHelper getTextSelectionHelper() {
        return null;
    }

    public boolean canPerformActions() {
        return true;
    }

    public boolean canDrawOutboundsContent() {
        return true;
    }

    public boolean isProgressLoading(ChatMessageCell cell, int type) {
        return false;
    }

    public PinchToZoomHelper getPinchToZoomHelper() {
        return null;
    }

    public boolean doNotShowLoadingReply(MessageObject msg) {
        return true;
    }

    public void didLongPress(ChatMessageCell cell, float x, float y) {
    }

    public View createView(Context context) {
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(true);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.1
            public void onItemClick(int id) {
                if (id == -1) {
                    DeletedMessagesInChatFragment.this.finishFragment();
                }
            }
        });
        this.actionBar.setTitle(Localization.MESSAGE_HISTORY_TITLE);
        FrameLayout frameLayout = new FrameLayout(context);
        RecyclerView rv = new RecyclerView(context);
        rv.setLayoutManager(new LinearLayoutManager(context));
        this.adapter = new DeletedAdapter(context, this.did, getAccountInstance(), this);
        rv.setAdapter(this.adapter);
        frameLayout.addView(rv);
        this.list = rv;
        this.fragmentView = frameLayout;
        this.fragmentView.post(new Runnable() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment$$ExternalSyntheticLambda0
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

    private static class DeletedAdapter extends RecyclerView.Adapter<CellVH> {
        private final Context ctx;
        private final ChatMessageCell.ChatMessageCellDelegate delegate;
        private final long did;
        private final ArrayList<MessageObject> versionRows = new ArrayList<>();

        DeletedAdapter(Context ctx, long did, AccountInstance accountInstance, ChatMessageCell.ChatMessageCellDelegate delegate) {
            this.ctx = ctx;
            this.did = did;
            this.delegate = delegate;
        }

        public void onViewAttachedToWindow(CellVH holder) {
            if (holder.itemView instanceof FixedChatMessageCell) {
                ((FixedChatMessageCell) holder.itemView).setCellAttachedToWindow(true);
            }
        }

        public void onViewDetachedFromWindow(CellVH holder) {
            if (holder.itemView instanceof FixedChatMessageCell) {
                ((FixedChatMessageCell) holder.itemView).setCellAttachedToWindow(false);
            }
        }

        public MessageObject getItem(int position) {
            if (this.versionRows != null && position >= 0 && position < this.versionRows.size()) {
                return this.versionRows.get(position);
            }
            return null;
        }

        void reload() throws IllegalAccessException, InvocationTargetException {
            this.versionRows.clear();
            ArrayList<Integer> mids = ReExteraDb.get().allMessageIdsByDid(this.did);
            if (!mids.isEmpty()) {
                for (Integer mid : mids) {
                    MessageObject msgObj = MessageUtils.getMessage(this.did, mid.intValue());
                    this.versionRows.add(msgObj);
                }
            }
            notifyDataSetChanged();
        }

        /* JADX WARN: Type inference failed for: r0v0, types: [android.view.View, ni.shikatu.re_extera.ui.FixedChatMessageCell] */
        public CellVH onCreateViewHolder(ViewGroup parent, int viewType) {
            ?? fixedChatMessageCell = new FixedChatMessageCell(this.ctx, UserConfig.selectedAccount);
            fixedChatMessageCell.setAllowAssistant(false);
            fixedChatMessageCell.setDelegate(this.delegate);
            return new CellVH(fixedChatMessageCell);
        }

        public void onBindViewHolder(CellVH holder, int position) {
            MessageObject mo = this.versionRows.get(position);
            holder.bind(mo);
        }

        public int getItemCount() {
            return this.versionRows.size();
        }

        static class CellVH extends RecyclerView.ViewHolder {
            private final FixedChatMessageCell cell;

            /* JADX WARN: Multi-variable type inference failed */
            CellVH(View view) {
                super(view);
                this.cell = (FixedChatMessageCell) view;
            }

            void bind(MessageObject mo) {
                try {
                    mo.resetLayout();
                    mo.forceAvatar = true;
                    mo.messageOwner.out = false;
                    try {
                        this.cell.setMessageObject(mo, null, false, false, false);
                        this.cell.forceRenderAvatar(mo);
                    } catch (Throwable th) {
                    }
                } catch (Throwable th2) {
                }
            }
        }
    }
}
