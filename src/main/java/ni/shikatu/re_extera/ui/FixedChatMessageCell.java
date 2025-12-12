package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatMessageSharedResources;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;

public class FixedChatMessageCell extends ChatMessageCell {
    private static final int AVATAR_ZONE_WIDTH = 54;
    private GestureDetector avatarGestureDetector;
    private final Handler handler;
    private boolean isAvatarTouch;
    private boolean isLongPressHandled;
    private final Runnable longPressRunnable;
    private final int longPressTimeout;
    private boolean passedToParent;
    private boolean shouldShowAvatar;
    private float startX;
    private float startY;
    private final int touchSlop;
    private boolean wasScrolling;

    public FixedChatMessageCell(Context context, int currentAccount, boolean needAvatar, ChatMessageSharedResources sharedResources, Theme.ResourcesProvider resourcesProvider) {
        super(context, currentAccount, needAvatar, sharedResources, resourcesProvider);
        this.isLongPressHandled = false;
        this.isAvatarTouch = false;
        this.shouldShowAvatar = false;
        this.wasScrolling = false;
        this.passedToParent = false;
        this.handler = new Handler(Looper.getMainLooper());
        this.longPressRunnable = new Runnable() { // from class: ni.shikatu.re_extera.ui.FixedChatMessageCell.1
            @Override // java.lang.Runnable
            public void run() {
                if (!FixedChatMessageCell.this.wasScrolling && !FixedChatMessageCell.this.isAvatarTouch && FixedChatMessageCell.this.startX >= AndroidUtilities.dp(54.0f)) {
                    FixedChatMessageCell.this.isLongPressHandled = true;
                    FixedChatMessageCell.this.performHapticFeedback(0);
                    if (FixedChatMessageCell.this.getDelegate() != null) {
                        FixedChatMessageCell.this.getDelegate().didLongPress(FixedChatMessageCell.this, FixedChatMessageCell.this.startX, FixedChatMessageCell.this.startY);
                    }
                }
            }
        };
        ViewConfiguration config = ViewConfiguration.get(context);
        this.touchSlop = config.getScaledTouchSlop();
        this.longPressTimeout = ViewConfiguration.getLongPressTimeout();
        initGestureDetector(context);
        setWillNotDraw(false);
    }

    private void initGestureDetector(Context context) {
        this.avatarGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() { // from class: ni.shikatu.re_extera.ui.FixedChatMessageCell.2
            @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
            public boolean onSingleTapUp(MotionEvent e) {
                if (!FixedChatMessageCell.this.wasScrolling && FixedChatMessageCell.this.getDelegate() != null && FixedChatMessageCell.this.getMessageObject() != null) {
                    long senderId = FixedChatMessageCell.this.getMessageObject().getSenderId();
                    if (senderId > 0) {
                        TLRPC.User user = MessagesController.getInstance(FixedChatMessageCell.this.currentAccount).getUser(Long.valueOf(senderId));
                        if (user != null) {
                            FixedChatMessageCell.this.getDelegate().didPressUserAvatar(FixedChatMessageCell.this, user, e.getX(), e.getY(), false);
                            return true;
                        }
                    } else {
                        TLRPC.Chat chat = MessagesController.getInstance(FixedChatMessageCell.this.currentAccount).getChat(Long.valueOf(-senderId));
                        if (chat != null) {
                            FixedChatMessageCell.this.getDelegate().didPressChannelAvatar(FixedChatMessageCell.this, chat, 0, e.getX(), e.getY(), false);
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }

    private boolean isInAvatarBounds(float x, float y) {
        ImageReceiver avatar = getAvatarImage();
        if (avatar == null || !avatar.getVisible()) {
            return false;
        }
        float avatarX = avatar.getImageX();
        float avatarY = avatar.getImageY();
        float avatarSize = avatar.getImageWidth();
        return x >= avatarX && x <= avatarX + avatarSize && y >= avatarY && y <= avatarY + avatarSize;
    }

    public void setShouldShowAvatar(boolean show) {
        this.shouldShowAvatar = show;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == 0) {
            this.startX = event.getX();
            this.startY = event.getY();
            this.isLongPressHandled = false;
            this.wasScrolling = false;
            this.passedToParent = false;
            this.isAvatarTouch = isInAvatarBounds(event.getX(), event.getY());
            if (this.isAvatarTouch) {
                this.avatarGestureDetector.onTouchEvent(event);
                return true;
            }
            this.handler.postDelayed(this.longPressRunnable, this.longPressTimeout);
            return true;
        }
        if (this.isAvatarTouch) {
            float dx = Math.abs(event.getX() - this.startX);
            float dy = Math.abs(event.getY() - this.startY);
            if (dx > this.touchSlop || dy > this.touchSlop) {
                this.wasScrolling = true;
            }
            boolean handled = this.avatarGestureDetector.onTouchEvent(event);
            if (event.getAction() == 1 || event.getAction() == 3) {
                this.isAvatarTouch = false;
                if (event.getAction() == 1 && handled && !this.wasScrolling) {
                    return true;
                }
            }
            return !this.wasScrolling;
        }
        switch (event.getAction()) {
            case Defaults.ALWAYS /* 1 */:
                this.handler.removeCallbacks(this.longPressRunnable);
                if (this.isLongPressHandled) {
                    this.isLongPressHandled = false;
                    return true;
                }
                break;
            case 2:
                float dx2 = Math.abs(event.getX() - this.startX);
                float dy2 = Math.abs(event.getY() - this.startY);
                if ((dx2 > this.touchSlop || dy2 > this.touchSlop) && !this.wasScrolling) {
                    this.wasScrolling = true;
                    this.handler.removeCallbacks(this.longPressRunnable);
                    if (!this.passedToParent) {
                        this.passedToParent = true;
                        MotionEvent down = MotionEvent.obtain(event);
                        down.setAction(0);
                        down.setLocation(this.startX, this.startY);
                        super.onTouchEvent(down);
                        down.recycle();
                    }
                }
                break;
            case Main.VERSION_CODE /* 3 */:
                this.handler.removeCallbacks(this.longPressRunnable);
                this.isLongPressHandled = false;
                this.wasScrolling = false;
                this.passedToParent = false;
                return super.onTouchEvent(event);
        }
        if (this.wasScrolling || event.getAction() == 1) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ImageReceiver avatar = getAvatarImage();
        if (avatar != null && avatar.getVisible() && this.shouldShowAvatar) {
            canvas.save();
            canvas.clipRect(-avatar.getImageWidth(), (-avatar.getImageHeight()) * 2.0f, getWidth() + avatar.getImageWidth(), getHeight() + (avatar.getImageHeight() * 2.0f));
            avatar.draw(canvas);
            canvas.restore();
        }
    }
}
