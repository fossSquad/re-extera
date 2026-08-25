package ni.shikatu.re_extera.hooks.chatmessagecell;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import androidx.core.content.ContextCompat;
import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.WeakHashMap;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

/**
 * Telegraph-style quick side buttons drawn in the empty margin beside a message:
 * a pencil (edit) on the user's own editable messages, and a cloud (save to Saved
 * Messages) on incoming messages. Drawn in {@code onDraw} and hit-tested in
 * {@code onTouchEvent}; no layout space is reserved (kept in the existing margin)
 * to avoid touching the cell's measure/layout. Gated by a settings kill-switch.
 */
public final class QuickButtons {
    private static final int ACTION_EDIT = 1;
    private static final int ACTION_SAVE = 2;

    // Per-cell button geometry computed on draw and read on touch: {cx, cy, radius, action}.
    private static final WeakHashMap<Object, float[]> BUTTONS = new WeakHashMap<>();

    private static final Paint BG_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static Drawable editIcon;
    private static Drawable saveIcon;

    // User-supplied cloud icon (stroke, viewBox 24, stroke width 2).
    private static final String CLOUD_ICON_PATH =
            "M12 9.5V15.5M12 9.5L10 11.5M12 9.5L14 11.5M8.4 19C5.41766 19 3 16.6044 3 13.6493C3 11.2001 4.8 8.9375 7.5 8.5"
            + "C8.34694 6.48637 10.3514 5 12.6893 5C15.684 5 18.1317 7.32251 18.3 10.25C19.8893 10.9449 21 12.6503 21 14.4969"
            + "C21 16.9839 18.9853 19 16.5 19L8.4 19Z";

    private QuickButtons() {
    }

    private static Drawable icon(ChatMessageCell cell, boolean edit) {
        try {
            if (edit) {
                if (editIcon == null) {
                    editIcon = ContextCompat.getDrawable(cell.getContext(), R.drawable.msg_edit).mutate();
                }
                return editIcon;
            }
            if (saveIcon == null) {
                saveIcon = new ni.shikatu.re_extera.utils.PathIconDrawable(24f, 24f, false, 20, 2f, CLOUD_ICON_PATH);
            }
            return saveIcon;
        } catch (Throwable e) {
            return null;
        }
    }

    public static final class Draw extends XC_MethodHook {
        public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
            if (!Settings.getMessageQuickButtons()) {
                return;
            }
            try {
                ChatMessageCell cell = (ChatMessageCell) param.thisObject;
                MessageObject mo = cell.getMessageObject();
                if (mo == null || mo.messageOwner == null || mo.isSponsored()) {
                    BUTTONS.remove(cell);
                    return;
                }
                boolean out = mo.isOutOwner();
                int action;
                if (out) {
                    boolean canEdit;
                    try {
                        canEdit = mo.canEditMessage(null);
                    } catch (Throwable e) {
                        canEdit = false;
                    }
                    if (!canEdit) {
                        BUTTONS.remove(cell);
                        return;
                    }
                    action = ACTION_EDIT;
                } else {
                    action = ACTION_SAVE;
                }

                int bubbleLeft = cell.getBackgroundDrawableLeft();
                int bubbleRight = cell.getBackgroundDrawableRight();
                int bubbleBottom = cell.getBackgroundDrawableBottom();

                // 28dp circle, 4dp gap from the bubble edge, anchored near the bubble bottom.
                float radius = AndroidUtilities.dp(14);
                float gap = AndroidUtilities.dp(4);
                float cy = bubbleBottom - AndroidUtilities.dp(20);
                float cx;
                if (out) {
                    cx = bubbleLeft - gap - radius; // left of an outgoing bubble
                    if (cx - radius < 0) {
                        BUTTONS.remove(cell);
                        return;
                    }
                } else {
                    cx = bubbleRight + gap + radius; // right of an incoming bubble
                    if (cx + radius > cell.getWidth()) {
                        BUTTONS.remove(cell);
                        return;
                    }
                }

                Canvas canvas = (Canvas) param.args[0];
                Drawable drawable = icon(cell, action == ACTION_EDIT);
                if (drawable == null) {
                    return;
                }
                // Solid service-background circle — consistent for both buttons everywhere.
                BG_PAINT.setColor(Theme.getColor(Theme.key_chat_serviceBackground));
                canvas.drawCircle(cx, cy, radius, BG_PAINT);
                int half = AndroidUtilities.dp(10);
                drawable.setBounds((int) cx - half, (int) cy - half, (int) cx + half, (int) cy + half);
                drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_serviceText), PorterDuff.Mode.SRC_IN));
                drawable.draw(canvas);

                BUTTONS.put(cell, new float[]{cx, cy, radius, action});
            } catch (Throwable e) {
                Main.log("QuickButtons draw error: %s", e.getMessage());
            }
        }
    }

    public static final class Touch extends XC_MethodHook {
        public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
            if (!Settings.getMessageQuickButtons()) {
                return;
            }
            try {
                ChatMessageCell cell = (ChatMessageCell) param.thisObject;
                float[] btn = BUTTONS.get(cell);
                if (btn == null) {
                    return;
                }
                MotionEvent ev = (MotionEvent) param.args[0];
                int a = ev.getAction();
                if (a != MotionEvent.ACTION_DOWN && a != MotionEvent.ACTION_UP) {
                    return;
                }
                float dx = ev.getX() - btn[0];
                float dy = ev.getY() - btn[1];
                float hit = btn[2] + AndroidUtilities.dp(6);
                if (dx * dx + dy * dy > hit * hit) {
                    return;
                }
                // Inside the button: consume the event so the cell doesn't react,
                // and trigger on UP.
                param.setResult(Boolean.TRUE);
                if (a == MotionEvent.ACTION_UP) {
                    trigger(cell, (int) btn[3]);
                }
            } catch (Throwable e) {
                Main.log("QuickButtons touch error: %s", e.getMessage());
            }
        }
    }

    private static void trigger(ChatMessageCell cell, int action) {
        MessageObject mo = cell.getMessageObject();
        if (mo == null) {
            return;
        }
        if (action == ACTION_EDIT) {
            BaseFragment last = LaunchActivity.getLastFragment();
            if (last instanceof ChatActivity) {
                try {
                    java.lang.reflect.Method m = ChatActivity.class.getDeclaredMethod("startEditingMessageObject", MessageObject.class, Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(last, mo, Boolean.FALSE);
                } catch (Throwable e) {
                    Main.log("QuickButtons edit failed: %s", e.getMessage());
                }
            }
        } else if (action == ACTION_SAVE) {
            try {
                int account = mo.currentAccount;
                long ownId = UserConfig.getInstance(account).getClientUserId();
                ArrayList<MessageObject> list = new ArrayList<>();
                list.add(mo);
                SendMessagesHelper.getInstance(account).sendMessage(list, ownId, false, false, true, 0, 0L);
                // "Forwarded to Saved Messages" toast (clickable to open Saved Messages).
                // Action id 53 (0x35) is what exteraGram's own Save handler uses.
                BaseFragment last = LaunchActivity.getLastFragment();
                if (last instanceof ChatActivity) {
                    try {
                        org.telegram.ui.Components.UndoView undo = ((ChatActivity) last).getUndoView();
                        if (undo != null) {
                            undo.showWithAction(ownId, 53, Integer.valueOf(1));
                        }
                    } catch (Throwable e) {
                        Main.log("QuickButtons save toast failed: %s", e.getMessage());
                    }
                }
            } catch (Throwable e) {
                Main.log("QuickButtons save failed: %s", e.getMessage());
            }
        }
    }
}
