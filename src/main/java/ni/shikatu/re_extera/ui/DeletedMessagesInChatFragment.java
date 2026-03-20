package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.function.ToIntFunction;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.RestrictedMessageUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BotInlineKeyboard;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ArticleViewer;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.TextSelectionHelper;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.EmbedBottomSheet;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.LocationActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.PinchToZoomHelper;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.ThemePreviewActivity;

public class DeletedMessagesInChatFragment extends BaseFragment implements ChatMessageCell.ChatMessageCellDelegate, NotificationCenter.NotificationCenterDelegate {
    private DeletedAdapter adapter;
    private ChatAvatarContainer avatarContainer;
    private LinearLayoutManager chatLayoutManager;
    private RecyclerListView chatListView;
    private SizeNotifierFrameLayout contentView;
    private TLRPC.Chat currentChat;
    private TLRPC.User currentUser;
    private long did;
    private TextView emptyView;
    private final PhotoViewer.PhotoViewerProvider provider = new PhotoViewer.EmptyPhotoViewerProvider() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.1
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview, boolean closing) {
            ChatMessageCell cell;
            MessageObject message;
            ImageReceiver imageReceiver;
            if (DeletedMessagesInChatFragment.this.chatListView == null) {
                return null;
            }
            int count = DeletedMessagesInChatFragment.this.chatListView.getChildCount();
            for (int i = 0; i < count; i++) {
                ChatMessageCell cell2 = DeletedMessagesInChatFragment.this.chatListView.getChildAt(i);
                if ((cell2 instanceof ChatMessageCell) && (message = (cell = cell2).getMessageObject()) != null && messageObject != null && message.getId() == messageObject.getId() && (imageReceiver = cell.getPhotoImage()) != null) {
                    int[] coords = new int[2];
                    cell2.getLocationInWindow(coords);
                    PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                    object.viewX = coords[0];
                    object.viewY = coords[1];
                    object.parentView = DeletedMessagesInChatFragment.this.chatListView;
                    object.imageReceiver = imageReceiver;
                    object.thumb = imageReceiver.getBitmapSafe();
                    object.radius = imageReceiver.getRoundRadius(true);
                    return object;
                }
            }
            return null;
        }
    };
    private TextSelectionHelper.ChatListTextSelectionHelper textSelectionHelper;

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
        resolveDialogPeer();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewWallpapper);
        NotificationCenter.getInstance(getCurrentAccount()).addObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(getCurrentAccount()).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(getCurrentAccount()).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(getCurrentAccount()).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        return super.onFragmentCreate();
    }

    public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewWallpapper);
        NotificationCenter.getInstance(getCurrentAccount()).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(getCurrentAccount()).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(getCurrentAccount()).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(getCurrentAccount()).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        super.onFragmentDestroy();
    }

    public View createView(Context context) {
        this.hasOwnBackground = true;
        Theme.createChatResources(context, false);
        this.actionBar.setAddToContainer(false);
        this.actionBar.setOccupyStatusBar(!AndroidUtilities.isTablet());
        this.actionBar.setBackButtonDrawable(new BackDrawable(false));
        this.actionBar.setAllowOverlayTitle(false);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.2
            public void onItemClick(int id) {
                if (id == -1) {
                    DeletedMessagesInChatFragment.this.finishFragment();
                }
            }
        });
        this.actionBar.setTitle("");
        this.avatarContainer = new ChatAvatarContainer(context, (BaseFragment) null, false, this.resourceProvider);
        this.avatarContainer.setOccupyStatusBar(!AndroidUtilities.isTablet());
        this.avatarContainer.setEnabled(false);
        this.actionBar.addView(this.avatarContainer, 0, LayoutHelper.createFrame(-2, -1.0f, 51, 56.0f, 0.0f, 40.0f, 0.0f));
        updateHeader(0);
        this.contentView = new SizeNotifierFrameLayout(context) { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.3
            public boolean dispatchTouchEvent(MotionEvent ev) {
                TextSelectionHelper.TextSelectionOverlay selectionOverlay;
                if (DeletedMessagesInChatFragment.this.textSelectionHelper != null && (selectionOverlay = DeletedMessagesInChatFragment.this.textSelectionHelper.getOverlayView(getContext())) != null) {
                    float overlayX = selectionOverlay.getX();
                    float overlayY = selectionOverlay.getY();
                    ev.offsetLocation(-overlayX, -overlayY);
                    if (DeletedMessagesInChatFragment.this.textSelectionHelper.isInSelectionMode() && selectionOverlay.onTouchEvent(ev)) {
                        ev.offsetLocation(overlayX, overlayY);
                        return true;
                    }
                    ev.offsetLocation(overlayX, overlayY);
                    if (selectionOverlay.checkOnTap(ev)) {
                        return true;
                    }
                    if (ev.getAction() == 0 && DeletedMessagesInChatFragment.this.textSelectionHelper.isInSelectionMode() && DeletedMessagesInChatFragment.this.chatListView != null && (ev.getY() < DeletedMessagesInChatFragment.this.chatListView.getTop() || ev.getY() > DeletedMessagesInChatFragment.this.chatListView.getBottom())) {
                        DeletedMessagesInChatFragment.this.textSelectionHelper.clear();
                        return true;
                    }
                }
                return super.dispatchTouchEvent(ev);
            }
        };
        this.contentView.setOccupyStatusBar(true ^ AndroidUtilities.isTablet());
        this.contentView.setBackgroundImage(Theme.getCachedWallpaper(), Theme.isWallpaperMotion());
        this.textSelectionHelper = new TextSelectionHelper.ChatListTextSelectionHelper() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.4
            {
                this.resourcesProvider = DeletedMessagesInChatFragment.this.resourceProvider;
            }

            protected Theme.ResourcesProvider getResourcesProvider() {
                return this.resourcesProvider;
            }

            public void invalidate() {
                super.invalidate();
                if (DeletedMessagesInChatFragment.this.chatListView != null) {
                    DeletedMessagesInChatFragment.this.chatListView.invalidate();
                }
            }
        };
        this.textSelectionHelper.setCallback(new TextSelectionHelper.Callback() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.5
            public void onStateChanged(boolean isSelected) {
                if (DeletedMessagesInChatFragment.this.chatListView != null) {
                    DeletedMessagesInChatFragment.this.chatListView.invalidate();
                }
            }

            public void onTextCopied() {
                BulletinFactory.of(DeletedMessagesInChatFragment.this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
            }
        });
        int topInset = ActionBar.getCurrentActionBarHeight() + (this.actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        this.chatListView = new RecyclerListView(context, this.resourceProvider);
        this.chatListView.setClipChildren(false);
        this.chatListView.setClipToPadding(false);
        this.chatListView.setPadding(0, AndroidUtilities.dp(8.0f) + topInset, 0, AndroidUtilities.dp(8.0f));
        this.chatLayoutManager = new LinearLayoutManager(context);
        this.chatListView.setLayoutManager(this.chatLayoutManager);
        this.chatListView.setItemAnimator((RecyclerView.ItemAnimator) null);
        this.chatListView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListenerExtended() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.6
            public boolean onItemClick(View view, int position, float x, float y) {
                if (DeletedMessagesInChatFragment.this.textSelectionHelper != null && (DeletedMessagesInChatFragment.this.textSelectionHelper.isTryingSelect() || DeletedMessagesInChatFragment.this.textSelectionHelper.isInSelectionMode())) {
                    return false;
                }
                return DeletedMessagesInChatFragment.this.showMessageMenu(view, x, y);
            }
        });
        this.chatListView.addOnScrollListener(new RecyclerView.OnScrollListener() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment.7
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (DeletedMessagesInChatFragment.this.textSelectionHelper != null && newState == 0) {
                    DeletedMessagesInChatFragment.this.textSelectionHelper.stopScrolling();
                }
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (DeletedMessagesInChatFragment.this.textSelectionHelper != null) {
                    DeletedMessagesInChatFragment.this.textSelectionHelper.onParentScrolled();
                }
            }
        });
        this.adapter = new DeletedAdapter(context, this, this.currentChat, this.currentUser, this.resourceProvider, getCurrentAccount());
        this.chatListView.setAdapter(this.adapter);
        this.contentView.addView(this.chatListView, LayoutHelper.createFrame(-1, -1.0f));
        View textSelectionOverlay = this.textSelectionHelper.getOverlayView(context);
        if (textSelectionOverlay != null) {
            ViewParent parent = textSelectionOverlay.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup parent2 = (ViewGroup) parent;
                parent2.removeView(textSelectionOverlay);
            }
            this.contentView.addView(textSelectionOverlay);
        }
        this.textSelectionHelper.setParentView(this.chatListView);
        this.emptyView = new TextView(context);
        this.emptyView.setText(LocaleController.getString(R.string.NoResult));
        this.emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        this.emptyView.setTextSize(16.0f);
        this.emptyView.setVisibility(8);
        this.contentView.addView(this.emptyView, LayoutHelper.createFrame(-2, -2, 17));
        this.contentView.addView(this.actionBar);
        this.fragmentView = this.contentView;
        this.fragmentView.post(new DeletedMessagesInChatFragment$$ExternalSyntheticLambda0(this));
        return this.fragmentView;
    }

    public boolean isSwipeBackEnabled(MotionEvent event) {
        if (this.textSelectionHelper != null && (this.textSelectionHelper.isTryingSelect() || this.textSelectionHelper.isInSelectionMode() || this.textSelectionHelper.isTouched())) {
            return false;
        }
        return super.isSwipeBackEnabled(event);
    }

    public void onResume() {
        super.onResume();
        if (this.fragmentView != null) {
            this.fragmentView.post(new DeletedMessagesInChatFragment$$ExternalSyntheticLambda0(this));
        }
    }

    private void resolveDialogPeer() {
        if (this.did > 0) {
            this.currentUser = getMessagesController().getUser(Long.valueOf(this.did));
            this.currentChat = null;
        } else if (this.did < 0) {
            this.currentChat = getMessagesController().getChat(Long.valueOf(-this.did));
            this.currentUser = null;
        } else {
            this.currentChat = null;
            this.currentUser = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadMessages() {
        if (this.adapter == null) {
            return;
        }
        this.adapter.reload(this.did);
        updateHeader(this.adapter.getMessageCount());
        if (this.emptyView != null) {
            this.emptyView.setVisibility(this.adapter.getItemCount() == 0 ? 0 : 8);
        }
    }

    private void updateHeader(int messageCount) {
        if (this.avatarContainer == null) {
            return;
        }
        if (this.currentChat != null) {
            this.avatarContainer.setTitle(this.currentChat.title);
            this.avatarContainer.setChatAvatar(this.currentChat);
        } else if (this.currentUser != null) {
            this.avatarContainer.setTitle(ContactsController.formatName(this.currentUser.first_name, this.currentUser.last_name));
            this.avatarContainer.setUserAvatar(this.currentUser);
        } else {
            this.avatarContainer.setTitle(Localization.DELETED_MESSAGES_TITLE);
        }
        if (messageCount <= 0) {
            this.avatarContainer.setSubtitle(LocaleController.getString(R.string.NoResult));
        } else {
            this.avatarContainer.setSubtitle(LocaleController.formatPluralString("MessagesDeletedHint", messageCount, new Object[0]));
        }
    }

    private void openProfile(long peerId) {
        Bundle args = new Bundle();
        if (peerId > 0) {
            args.putLong("user_id", peerId);
        } else if (peerId < 0) {
            args.putLong("chat_id", -peerId);
        } else {
            return;
        }
        presentFragment(new ProfileActivity(args));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean showMessageMenu(View view, float x, float y) {
        ChatMessageCell cell;
        MessageObject messageObject;
        if ((view instanceof ChatMessageCell) && (messageObject = (cell = (ChatMessageCell) view).getMessageObject()) != null) {
            RestrictedMessageUtils.createMenu(this, cell, messageObject);
            return true;
        }
        return false;
    }

    private void openReplyMessage(int id) {
        int position;
        if (this.adapter != null && this.chatListView != null && (position = this.adapter.findPositionByMessageId(id)) >= 0) {
            this.chatLayoutManager.scrollToPositionWithOffset(position, AndroidUtilities.dp(56.0f));
        }
    }

    private void alertUserOpenError(MessageObject message) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.AppName));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (AlertDialog.OnButtonClickListener) null);
        if (message.type == 3) {
            builder.setMessage(LocaleController.getString(R.string.NoPlayerInstalled));
        } else if (message.getDocument() != null) {
            builder.setMessage(LocaleController.formatString(R.string.NoHandleAppInstalled, new Object[]{message.getDocument().mime_type}));
        } else {
            builder.setMessage(LocaleController.getString(R.string.UnknownError));
        }
        showDialog(builder.create());
    }

    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.emojiLoaded) {
            if (this.chatListView != null) {
                this.chatListView.invalidate();
                return;
            }
            return;
        }
        if (id == NotificationCenter.didSetNewWallpapper) {
            if (this.contentView != null) {
                this.contentView.setBackgroundImage(Theme.getCachedWallpaper(), Theme.isWallpaperMotion());
                this.contentView.invalidate();
            }
            if (this.chatListView != null) {
                this.chatListView.invalidate();
                return;
            }
            return;
        }
        if (id == NotificationCenter.messagePlayingDidStart) {
            updateVisiblePlaybackState(true);
            return;
        }
        if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingPlayStateChanged) {
            updateVisiblePlaybackState(false);
        } else if (id == NotificationCenter.messagePlayingProgressDidChanged) {
            Integer mid = (Integer) args[0];
            updateVisiblePlaybackProgress(mid);
        }
    }

    private void updateVisiblePlaybackState(boolean started) {
        ChatMessageCell cell;
        MessageObject messageObject;
        if (this.chatListView == null) {
            return;
        }
        int count = this.chatListView.getChildCount();
        for (int i = 0; i < count; i++) {
            ChatMessageCell cell2 = this.chatListView.getChildAt(i);
            if ((cell2 instanceof ChatMessageCell) && (messageObject = (cell = cell2).getMessageObject()) != null) {
                if (messageObject.isVoice() || messageObject.isMusic()) {
                    cell.updateButtonState(false, true, false);
                } else if (messageObject.isRoundVideo()) {
                    if (started) {
                        cell.checkVideoPlayback(false, (Bitmap) null);
                    } else if (!MediaController.getInstance().isPlayingMessage(messageObject)) {
                        cell.checkVideoPlayback(true, (Bitmap) null);
                    }
                }
            }
        }
    }

    private void updateVisiblePlaybackProgress(Integer mid) {
        ChatMessageCell cell;
        MessageObject playing;
        if (this.chatListView == null || mid == null) {
            return;
        }
        int count = this.chatListView.getChildCount();
        for (int i = 0; i < count; i++) {
            ChatMessageCell cell2 = this.chatListView.getChildAt(i);
            if ((cell2 instanceof ChatMessageCell) && (playing = (cell = cell2).getMessageObject()) != null && playing.getId() == mid.intValue()) {
                MessageObject player = MediaController.getInstance().getPlayingMessageObject();
                if (player != null) {
                    playing.audioProgress = player.audioProgress;
                    playing.audioProgressSec = player.audioProgressSec;
                    playing.audioPlayerDuration = player.audioPlayerDuration;
                    cell.updatePlayingMessageProgress();
                    return;
                }
                return;
            }
        }
    }

    public boolean isReplyOrSelf() {
        return false;
    }

    public void didPressExtendedMediaPreview(ChatMessageCell cell, TLRPC.KeyboardButton button) {
    }

    public void didPressUserStatus(ChatMessageCell cell, TLRPC.User user, TLRPC.Document document, String giftSlug) {
    }

    public void videoTimerReached() {
    }

    public boolean shouldRepeatSticker(MessageObject message) {
        return true;
    }

    public void didPressUserAvatar(ChatMessageCell cell, TLRPC.User user, float touchX, float touchY, boolean asForward) {
        if (user != null) {
            openProfile(user.id);
        }
    }

    public void didPressChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId, float touchX, float touchY, boolean asForward) {
        if (chat != null) {
            openProfile(-chat.id);
        }
    }

    public boolean didLongPressUserAvatar(ChatMessageCell cell, TLRPC.User user, float touchX, float touchY) {
        return false;
    }

    public void didPressHiddenForward(ChatMessageCell cell) {
    }

    public void didPressViaBotNotInline(ChatMessageCell cell, long botId) {
    }

    public void didPressViaBot(ChatMessageCell cell, String username) {
    }

    public void didPressBoostCounter(ChatMessageCell cell) {
    }

    public boolean didLongPressChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId, float touchX, float touchY) {
        return false;
    }

    public void didPressCancelSendButton(ChatMessageCell cell) {
    }

    public boolean canPerformReply() {
        return false;
    }

    public void didPressReplyMessage(ChatMessageCell cell, int id, float x, float y, boolean longpress) {
        openReplyMessage(id);
    }

    public String getAdminRank(long uid) {
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

    public String getProgressLoadingBotButtonUrl(ChatMessageCell cell) {
        return null;
    }

    public CharacterStyle getProgressLoadingLink(ChatMessageCell cell) {
        return null;
    }

    public void didPressUrl(ChatMessageCell cell, CharacterStyle url, boolean longPress) {
    }

    public void didPressCodeCopy(ChatMessageCell cell, MessageObject.TextLayoutBlock block) {
    }

    public void didPressChannelRecommendation(ChatMessageCell cell, TLObject chat, boolean longPress) {
    }

    public void didPressMoreChannelRecommendations(ChatMessageCell cell) {
    }

    public void didPressChannelRecommendationsClose(ChatMessageCell cell) {
    }

    public boolean doNotShowLoadingReply(MessageObject msg) {
        return true;
    }

    public void didPressWebPage(ChatMessageCell cell, TLRPC.WebPage webpage, String url, boolean safe) {
    }

    public void didLongPress(ChatMessageCell cell, float x, float y) {
        RestrictedMessageUtils.createMenu(this, cell, cell.getMessageObject());
    }

    public void didPressOther(ChatMessageCell cell, float otherX, float otherY) {
        RestrictedMessageUtils.createMenu(this, cell, cell.getMessageObject());
    }

    public void didPressGroupImage(ChatMessageCell cell, ImageReceiver imageReceiver, TLRPC.MessageExtendedMedia media, float x, float y) {
    }

    public void didPressSideButton(ChatMessageCell cell) {
    }

    public void didQuickShareStart(ChatMessageCell cell, float x, float y) {
    }

    public void didQuickShareMove(ChatMessageCell cell, float x, float y) {
    }

    public void didQuickShareEnd(ChatMessageCell cell, float x, float y) {
    }

    public void didPressSponsoredClose(ChatMessageCell cell) {
    }

    public void didPressSummarize(ChatMessageCell cell, boolean byReply) {
    }

    public void didPressSponsoredInfo(ChatMessageCell cell, float x, float y) {
    }

    public void didPressTime(ChatMessageCell cell) {
    }

    public void didPressBotButton(ChatMessageCell cell, TLRPC.KeyboardButton button) {
    }

    public void didLongPressBotButton(ChatMessageCell cell, TLRPC.KeyboardButton button) {
    }

    public void didPressCustomBotButton(ChatMessageCell cell, BotInlineKeyboard.ButtonCustom button) {
    }

    public void didLongPressCustomBotButton(ChatMessageCell cell, BotInlineKeyboard.ButtonCustom button) {
    }

    public void didPressReaction(ChatMessageCell cell, TLRPC.ReactionCount reaction, boolean longpress, float x, float y) {
    }

    public void didPressVoteButtons(ChatMessageCell cell, ArrayList<TLRPC.PollAnswer> buttons, int showCount, int x, int y) {
    }

    public boolean didPressToDoButton(ChatMessageCell cell, TLRPC.TodoItem task, boolean enable) {
        return false;
    }

    public boolean didLongPressToDoButton(ChatMessageCell cell, TLRPC.TodoItem task) {
        return false;
    }

    public boolean needPlayMessage(ChatMessageCell cell, MessageObject messageObject, boolean muted) {
        if (messageObject.isVoice() || messageObject.isRoundVideo()) {
            boolean result = MediaController.getInstance().playMessage(messageObject, muted);
            MediaController.getInstance().setVoiceMessagesPlaylist((ArrayList) null, false);
            return result;
        }
        if (messageObject.isMusic()) {
            return MediaController.getInstance().setPlaylist(this.adapter.getItems(), messageObject, 0L);
        }
        return false;
    }

    public void needOpenWebView(MessageObject message, String url, String title, String description, String originalUrl, int w, int h) {
        EmbedBottomSheet.show(this, message, this.provider, title, description, originalUrl, url, w, h, false);
    }

    public void didPressImage(ChatMessageCell cell, float x, float y, boolean fullPreview) {
        Theme.ThemeInfo themeInfo;
        MessageObject message = cell.getMessageObject();
        if (message != null && getParentActivity() != null) {
            if (message.getInputStickerSet() != null) {
                showDialog(new StickersAlert(getParentActivity(), this, message.getInputStickerSet(), (TLRPC.TL_messages_stickerSet) null, (StickersAlert.StickersAlertDelegate) null, false));
                return;
            }
            if (message.isVideo() || message.type == 1 || ((message.type == 0 && !message.isWebpageDocument()) || message.isGif())) {
                PhotoViewer.getInstance().setParentActivity(this);
                PhotoViewer.getInstance().openPhoto(message, (ChatActivity) null, 0L, 0L, 0L, this.provider);
                return;
            }
            if (message.type == 4) {
                if (!AndroidUtilities.isMapsInstalled(this)) {
                    return;
                }
                LocationActivity fragment = new LocationActivity(0);
                fragment.setMessageObject(message);
                presentFragment(fragment);
                return;
            }
            if (message.type == 9 || message.type == 0) {
                String documentName = message.getDocumentName();
                if (!TextUtils.isEmpty(documentName) && documentName.toLowerCase().endsWith("attheme")) {
                    File locFile = null;
                    if (!TextUtils.isEmpty(message.messageOwner.attachPath)) {
                        File attachFile = new File(message.messageOwner.attachPath);
                        if (attachFile.exists()) {
                            locFile = attachFile;
                        }
                    }
                    if (locFile == null) {
                        File file = getFileLoader().getPathToMessage(message.messageOwner);
                        if (file.exists()) {
                            locFile = file;
                        }
                    }
                    if (locFile != null && (themeInfo = Theme.applyThemeFile(locFile, documentName, (TLRPC.TL_theme) null, true)) != null) {
                        presentFragment(new ThemePreviewActivity(themeInfo));
                        return;
                    }
                }
                try {
                    AndroidUtilities.openForView(message, getParentActivity(), (Theme.ResourcesProvider) null, false);
                    return;
                } catch (Exception e) {
                    alertUserOpenError(message);
                    return;
                }
            }
            if (message.type == 3) {
                File file2 = null;
                try {
                    if (!TextUtils.isEmpty(message.messageOwner.attachPath)) {
                        file2 = new File(message.messageOwner.attachPath);
                    }
                    if (file2 == null || !file2.exists()) {
                        file2 = getFileLoader().getPathToMessage(message.messageOwner);
                    }
                    Intent intent = new Intent("android.intent.action.VIEW");
                    if (Build.VERSION.SDK_INT >= 24) {
                        intent.setFlags(1);
                        intent.setDataAndType(FileProvider.getUriForFile(getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", file2), "video/mp4");
                    } else {
                        intent.setDataAndType(Uri.fromFile(file2), "video/mp4");
                    }
                    getParentActivity().startActivityForResult(intent, 500);
                } catch (Exception e2) {
                    alertUserOpenError(message);
                }
            }
        }
    }

    public void didPressInstantButton(ChatMessageCell cell, int type) {
        MessageObject messageObject = cell.getMessageObject();
        if (messageObject == null || getParentActivity() == null) {
            return;
        }
        if (type == 0) {
            if (messageObject.messageOwner.media != null && messageObject.messageOwner.media.webpage != null && messageObject.messageOwner.media.webpage.cached_page != null) {
                ArticleViewer.getInstance().setParentActivity(getParentActivity(), this);
                ArticleViewer.getInstance().open(messageObject);
                return;
            }
            return;
        }
        if (messageObject.messageOwner.media != null && messageObject.messageOwner.media.webpage != null) {
            Browser.openUrl(getParentActivity(), messageObject.messageOwner.media.webpage.url);
        }
    }

    public void didPressGiveawayChatButton(ChatMessageCell cell, int pressedPos) {
    }

    public void didPressCommentButton(ChatMessageCell cell) {
    }

    public void didPressHint(ChatMessageCell cell, int type) {
    }

    public void needShowPremiumFeatures(String source) {
    }

    public void needShowPremiumBulletin(int type) {
    }

    public boolean isAdmin(long uid) {
        return false;
    }

    public boolean isOwner(long uid) {
        return false;
    }

    public boolean drawingVideoPlayerContainer() {
        return false;
    }

    public boolean onAccessibilityAction(int action, Bundle arguments) {
        return false;
    }

    public void didStartVideoStream(MessageObject message) {
    }

    public void setShouldNotRepeatSticker(MessageObject message) {
    }

    public TextSelectionHelper.ChatListTextSelectionHelper getTextSelectionHelper() {
        return this.textSelectionHelper;
    }

    public boolean hasSelectedMessages() {
        return this.textSelectionHelper != null && this.textSelectionHelper.isInSelectionMode();
    }

    public void needReloadPolls() {
    }

    public void onDiceFinished() {
    }

    public boolean shouldDrawThreadProgress(ChatMessageCell cell, boolean delayed) {
        return false;
    }

    public PinchToZoomHelper getPinchToZoomHelper() {
        return null;
    }

    public boolean keyboardIsOpened() {
        return false;
    }

    public boolean shouldDrawAvatarOnlineStatus(ChatMessageCell cell) {
        return true;
    }

    public boolean isLandscape() {
        return false;
    }

    public void invalidateBlur() {
    }

    public boolean didPressAnimatedEmoji(ChatMessageCell cell, AnimatedEmojiSpan span) {
        return false;
    }

    public void didPressTopicButton(ChatMessageCell cell) {
    }

    public boolean shouldShowTopicButton(ChatMessageCell cell) {
        return false;
    }

    public void didPressDialogButton(ChatMessageCell cell) {
    }

    public boolean shouldShowDialogButton(ChatMessageCell cell) {
        return false;
    }

    public void didPressEmojiStatus() {
    }

    public void didPressAboutRevenueSharingAds() {
    }

    public void didPressRevealSensitiveContent(ChatMessageCell cell) {
    }

    public void didPressEffect(ChatMessageCell cell) {
    }

    public void didPressFactCheckWhat(ChatMessageCell cell, int cx, int cy) {
    }

    public void didPressFactCheck(ChatMessageCell cell) {
    }

    public void forceUpdate(ChatMessageCell cell, boolean anchorScroll) {
    }

    public void forceUpdateNoAnimation(ChatMessageCell cell, boolean anchorScroll) {
    }

    public void didPressAdmin(ChatMessageCell cell) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class DeletedAdapter extends RecyclerView.Adapter<CellVH> {
        private static final int VIEW_TYPE_DATE = 1;
        private static final int VIEW_TYPE_MESSAGE = 0;
        private final Context context;
        private final int currentAccount;
        private final TLRPC.Chat currentChat;
        private final TLRPC.User currentUser;
        private final ChatMessageCell.ChatMessageCellDelegate delegate;
        private final Theme.ResourcesProvider resourcesProvider;
        private final ArrayList<MessageObject> items = new ArrayList<>();
        private final ArrayList<MessageObject> rows = new ArrayList<>();

        DeletedAdapter(Context context, ChatMessageCell.ChatMessageCellDelegate delegate, TLRPC.Chat currentChat, TLRPC.User currentUser, Theme.ResourcesProvider resourcesProvider, int currentAccount) {
            this.context = context;
            this.delegate = delegate;
            this.currentChat = currentChat;
            this.currentUser = currentUser;
            this.resourcesProvider = resourcesProvider;
            this.currentAccount = currentAccount;
            setHasStableIds(true);
        }

        ArrayList<MessageObject> getItems() {
            return this.items;
        }

        int getMessageCount() {
            return this.items.size();
        }

        int findPositionByMessageId(int messageId) {
            for (int i = 0; i < this.rows.size(); i++) {
                MessageObject row = this.rows.get(i);
                if (!row.isDateObject && row.getId() == messageId) {
                    return i;
                }
            }
            return -1;
        }

        void reload(long did) {
            this.items.clear();
            ArrayList<Integer> mids = ReExteraDb.get().allMessageIdsByDid(did);
            if (mids != null) {
                for (Integer mid : mids) {
                    MessageObject msgObj = MessageUtils.getMessage(did, mid.intValue());
                    if (msgObj != null) {
                        this.items.add(msgObj);
                    }
                }
            }
            this.items.sort(Comparator.comparingInt(new ToIntFunction() { // from class: ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment$DeletedAdapter$$ExternalSyntheticLambda0
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    return ((MessageObject) obj).messageOwner.date;
                }
            }));
            rebuildRows();
            notifyDataSetChanged();
        }

        private void rebuildRows() {
            this.rows.clear();
            String previousKey = null;
            for (MessageObject item : this.items) {
                String key = getDateKey(item);
                if (!key.equals(previousKey)) {
                    this.rows.add(createDateObject(item.messageOwner.date));
                    previousKey = key;
                }
                this.rows.add(item);
            }
        }

        private String getDateKey(MessageObject messageObject) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(((long) messageObject.messageOwner.date) * 1000);
            return calendar.get(1) + "_" + calendar.get(6);
        }

        private MessageObject createDateObject(int date) {
            TLRPC.TL_message dateMsg = new TLRPC.TL_message();
            dateMsg.message = LocaleController.formatDateChat(date);
            dateMsg.id = 0;
            dateMsg.date = date;
            MessageObject dateObject = new MessageObject(this.currentAccount, dateMsg, false, false);
            dateObject.type = 10;
            dateObject.contentType = 1;
            dateObject.isDateObject = true;
            return dateObject;
        }

        public long getItemId(int position) {
            MessageObject row = this.rows.get(position);
            if (row.isDateObject) {
                return (-1000000000) - ((long) row.messageOwner.date);
            }
            return row.getId();
        }

        public int getItemCount() {
            return this.rows.size();
        }

        public int getItemViewType(int i) {
            return this.rows.get(i).isDateObject ? 1 : 0;
        }

        public CellVH onCreateViewHolder(ViewGroup parent, int viewType) {
            ChatActionCell chatMessageCell;
            if (viewType == 1) {
                chatMessageCell = new ChatActionCell(this.context, false, this.resourcesProvider);
            } else {
                chatMessageCell = new ChatMessageCell(this.context, this.currentAccount);
                chatMessageCell.setDelegate(this.delegate);
                chatMessageCell.setAllowAssistant(false);
            }
            chatMessageCell.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
            return new CellVH(chatMessageCell);
        }

        public void onBindViewHolder(CellVH holder, int position) {
            boolean pinnedBottom;
            boolean pinnedTop;
            MessageObject current = this.rows.get(position);
            if (current.isDateObject) {
                ChatActionCell chatActionCell = holder.itemView;
                if (chatActionCell instanceof ChatActionCell) {
                    ChatActionCell actionCell = chatActionCell;
                    actionCell.setMessageObject(current);
                    actionCell.setAlpha(1.0f);
                    return;
                }
            }
            if (position >= this.rows.size() - 1) {
                pinnedBottom = false;
            } else {
                MessageObject next = this.rows.get(position + 1);
                if (next.isDateObject) {
                    next = null;
                }
                boolean pinnedBottom2 = isSameGroup(current, next);
                pinnedBottom = pinnedBottom2;
            }
            if (position <= 0) {
                pinnedTop = false;
            } else {
                MessageObject prev = this.rows.get(position - 1);
                if (prev.isDateObject) {
                    prev = null;
                }
                boolean pinnedTop2 = isSameGroup(current, prev);
                pinnedTop = pinnedTop2;
            }
            holder.bind(current, this.currentChat != null, this.currentUser != null && this.currentUser.bot, pinnedBottom, pinnedTop);
        }

        private boolean isSameGroup(MessageObject first, MessageObject second) {
            return first != null && second != null && first.isOutOwner() == second.isOutOwner() && first.getFromChatId() == second.getFromChatId() && Math.abs(first.messageOwner.date - second.messageOwner.date) <= 300;
        }

        static class CellVH extends RecyclerView.ViewHolder {
            private final ChatMessageCell cell;

            CellVH(View itemView) {
                super(itemView);
                this.cell = itemView instanceof ChatMessageCell ? (ChatMessageCell) itemView : null;
            }

            void bind(MessageObject message, boolean isChat, boolean isBot, boolean pinnedBottom, boolean pinnedTop) {
                if (this.cell == null) {
                    return;
                }
                message.resetLayout();
                this.cell.isChat = isChat;
                this.cell.isBot = isBot;
                this.cell.setDrawSelectionBackground(true);
                this.cell.setMessageObject(message, (MessageObject.GroupedMessages) null, pinnedBottom, pinnedTop, false);
                this.cell.setHighlighted(false);
                this.cell.setHighlightedText((String) null);
            }
        }
    }
}
