package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.ToIntFunction;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.RestrictedMessageUtils;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatMessageSharedResources;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.TextSelectionHelper;
import org.telegram.ui.PinchToZoomHelper;
import org.telegram.ui.ProfileActivity;

public class DeletedMessagesInChatFragment extends BaseFragment implements ChatMessageCell.ChatMessageCellDelegate {
    private DeletedAdapter adapter;
    private long did;

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

    public View createView(Context context) {
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(true);
        this.actionBar.setTitle(Localization.DELETED_MESSAGES_TITLE);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.1
            public void onItemClick(int id) {
                if (id == -1) {
                    DeletedMessagesInChatFragment.this.finishFragment();
                }
            }
        });
        FrameLayout frameLayout = new FrameLayout(context);
        RecyclerView rv = new RecyclerView(context);
        rv.setClipChildren(false);
        rv.setClipToPadding(false);
        rv.setPadding(0, 0, 0, AndroidUtilities.dp(2.0f));
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rv.setLayoutManager(layoutManager);
        this.adapter = new DeletedAdapter(context, this.did, getAccountInstance(), this, this.resourceProvider);
        rv.setAdapter(this.adapter);
        frameLayout.addView(rv);
        this.fragmentView = frameLayout;
        this.fragmentView.post(new Runnable() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$createView$0();
            }
        });
        return this.fragmentView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createView$0() {
        this.adapter.reload();
    }

    public void videoTimerReached() {
    }

    public boolean shouldRepeatSticker(MessageObject message) {
        return true;
    }

    public void didPressUserAvatar(ChatMessageCell cell, TLRPC.User user, float touchX, float touchY, boolean asForward) {
        if (user != null) {
            Bundle args = new Bundle();
            args.putLong("user_id", user.id);
            presentFragment(new ProfileActivity(args));
        }
    }

    public void didPressChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId, float touchX, float touchY, boolean asForward) {
        if (chat != null) {
            Bundle args = new Bundle();
            args.putLong("chat_id", chat.id);
            presentFragment(new ProfileActivity(args));
        }
    }

    public boolean canPerformReply() {
        return false;
    }

    public void didPressReplyMessage(ChatMessageCell cell, int id, float x, float y, boolean longpress) {
    }

    public String getAdminRank(long uid) {
        return null;
    }

    public TextSelectionHelper.ChatListTextSelectionHelper getTextSelectionHelper() {
        return null;
    }

    public boolean didLongPressUserAvatar(ChatMessageCell cell, TLRPC.User user, float touchX, float touchY) {
        return false;
    }

    public boolean didLongPressChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId, float touchX, float touchY) {
        return false;
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
        RestrictedMessageUtils.createMenu(this, cell, cell.getMessageObject());
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class DeletedAdapter extends RecyclerView.Adapter<CellVH> {
        private final Context ctx;
        private final ChatMessageCell.ChatMessageCellDelegate delegate;
        private final long did;
        private final ArrayList<MessageObject> items = new ArrayList<>();
        private final Theme.ResourcesProvider resourcesProvider;
        private final ChatMessageSharedResources sharedResources;

        DeletedAdapter(Context ctx, long did, AccountInstance accountInstance, ChatMessageCell.ChatMessageCellDelegate delegate, Theme.ResourcesProvider provider) {
            this.ctx = ctx;
            this.did = did;
            this.delegate = delegate;
            this.sharedResources = new ChatMessageSharedResources(ctx);
            this.resourcesProvider = provider;
        }

        void reload() {
            this.items.clear();
            ArrayList<Integer> mids = ReExteraDb.get().allMessageIdsByDid(this.did);
            if (mids != null) {
                for (Integer mid : mids) {
                    MessageObject msgObj = MessageUtils.getMessage(this.did, mid.intValue());
                    if (msgObj != null) {
                        msgObj.messageOwner.out = false;
                        this.items.add(msgObj);
                    }
                }
            }
            Collections.sort(this.items, Comparator.comparingInt(new ToIntFunction() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment$DeletedAdapter$$ExternalSyntheticLambda0
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    return ((MessageObject) obj).messageOwner.date;
                }
            }));
            notifyDataSetChanged();
        }

        public int getItemCount() {
            return this.items.size();
        }

        /* JADX WARN: Type inference failed for: r0v0, types: [android.view.View, ni.shikatu.re_extera.ui.FixedChatMessageCell] */
        public CellVH onCreateViewHolder(ViewGroup parent, int viewType) {
            ?? fixedChatMessageCell = new FixedChatMessageCell(this.ctx, UserConfig.selectedAccount, true, this.sharedResources, this.resourcesProvider);
            ((FixedChatMessageCell) fixedChatMessageCell).isChat = true;
            fixedChatMessageCell.setDelegate(this.delegate);
            return new CellVH(fixedChatMessageCell);
        }

        public void onBindViewHolder(CellVH holder, int position) {
            MessageObject current = this.items.get(position);
            boolean sameAsPrev = false;
            boolean sameAsNext = false;
            if (position > 0) {
                MessageObject prev = this.items.get(position - 1);
                sameAsPrev = isSameGroup(current, prev);
            }
            if (position < this.items.size() - 1) {
                MessageObject next = this.items.get(position + 1);
                sameAsNext = isSameGroup(current, next);
            }
            holder.bind(current, sameAsNext, sameAsPrev);
        }

        private boolean isSameGroup(MessageObject m1, MessageObject m2) {
            return m1 != null && m2 != null && m1.getSenderId() == m2.getSenderId() && Math.abs(m1.messageOwner.date - m2.messageOwner.date) < 300;
        }

        static class CellVH extends RecyclerView.ViewHolder {
            FixedChatMessageCell cell;

            /* JADX WARN: Multi-variable type inference failed */
            CellVH(View view) {
                super(view);
                this.cell = (FixedChatMessageCell) view;
            }

            void bind(MessageObject mo, boolean bottomNear, boolean topNear) {
                mo.resetLayout();
                mo.messageOwner.out = false;
                final boolean shouldShowAvatar = !bottomNear;
                this.cell.setShouldShowAvatar(shouldShowAvatar);
                this.cell.setMessageObject(mo, null, bottomNear, topNear, false);
                this.cell.post(new Runnable() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment$DeletedAdapter$CellVH$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.lambda$bind$0(shouldShowAvatar);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: private */
            public /* synthetic */ void lambda$bind$0(boolean shouldShowAvatar) {
                ImageReceiver avatar = this.cell.getAvatarImage();
                if (avatar != null) {
                    if (shouldShowAvatar) {
                        avatar.setVisible(true, false);
                        int avatarSize = AndroidUtilities.dp(42.0f);
                        int avatarX = AndroidUtilities.dp(6.0f);
                        int height = this.cell.getMeasuredHeight();
                        int avatarY = (height - avatarSize) - AndroidUtilities.dp(4.0f);
                        avatar.setImageCoords(avatarX, avatarY, avatarSize, avatarSize);
                    } else {
                        avatar.setVisible(false, false);
                        avatar.setImageCoords(0.0f, 0.0f, 0.0f, 0.0f);
                    }
                    this.cell.invalidate();
                }
            }
        }
    }
}
