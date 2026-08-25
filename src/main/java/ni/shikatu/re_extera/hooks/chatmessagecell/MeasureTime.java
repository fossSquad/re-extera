package ni.shikatu.re_extera.hooks.chatmessagecell;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import com.exteragram.messenger.utils.text.LocaleUtils;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ColoredImageSpan;

/**
 * Renders the deleted-message mark and (optionally) the message id inside the
 * time area of a {@link ChatMessageCell}.
 *
 * <p>Both additions are done in this single hook so they compose correctly and
 * share one width adjustment. {@code measureTime} rebuilds {@code currentTimeString}
 * on its main path but reuses it on early-return paths; to stay idempotent across
 * those repeated calls we prepend a zero-width sentinel and skip if it is already
 * present (a reused string), reprocessing only freshly rebuilt strings.
 */
public class MeasureTime extends XC_MethodHook {
    public static Drawable deletedIcon;
    private static final char SENTINEL = '\u200B';
    private final ReExteraDb redb = ReExteraDb.get();
    private static final Field CURRENT_TIME_STRING = field(ChatMessageCell.class, "currentTimeString");
    private static final Field TIME_TEXT_WIDTH = field(ChatMessageCell.class, "timeTextWidth");
    private static final Field TIME_WIDTH = field(ChatMessageCell.class, "timeWidth");
    private static final Field CHAT_TIME_PAINT = field(Theme.class, "chat_timePaint");
    public static String mark = Settings.getCustomPrefix();

    private static Field field(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    public static void notifyMarkChanged(String to) {
        mark = to;
    }

    private boolean computeDeleted(ChatMessageCell cell, MessageObject obj, long did, int mid) {
        if (!Settings.getSaveDeletedMessages()) {
            return false;
        }
        if (did == -999999999L) {
            return true;
        }
        try {
            MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
            if (group != null && group.messages != null && !group.messages.isEmpty()) {
                for (MessageObject m : group.messages) {
                    if (m != null && !m.deleted && !isMessageDeleted(m)) {
                        return false;
                    }
                }
                return true;
            }
            return obj.deleted || isMessageDeleted(obj);
        } catch (Throwable e) {
            return obj.deleted || isMessageDeleted(obj);
        }
    }

    private boolean isMessageDeleted(MessageObject m) {
        // Strict (did, mid) first, then a did-independent fallback so channel/group
        // and peer-less delete updates still mark reliably (matches TeleVip behaviour).
        return this.redb.messageIsDeleted(m) || this.redb.isMidDeletedAnyDialog(m.getId());
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!ni.shikatu.re_extera.hooks.HookInit.isActive) return;
        if (CURRENT_TIME_STRING == null || TIME_TEXT_WIDTH == null || TIME_WIDTH == null) {
            return;
        }
        ChatMessageCell cell = (ChatMessageCell) param.thisObject;
        MessageObject obj = (MessageObject) param.args[0];
        TLRPC.Message message;
        if (obj == null || (message = obj.messageOwner) == null) {
            return;
        }
        long did = obj.getDialogId();
        if (did == 0) {
            did = MessageUtils.getDialogIdFromMessage(message);
        }
        int mid = message.id;

        boolean isDeleted = computeDeleted(cell, obj, did, mid);
        boolean showId = Settings.getShowMessageId() && mid != 0;
        if (!isDeleted && !showId) {
            return;
        }

        CharSequence currentTimeString = (CharSequence) ReflectionUtils.get(CURRENT_TIME_STRING, cell);
        if (currentTimeString == null) {
            return;
        }
        SpannableStringBuilder builderTime = currentTimeString instanceof SpannableStringBuilder
                ? (SpannableStringBuilder) currentTimeString
                : new SpannableStringBuilder(currentTimeString);
        // Already decorated this (reused) string on an earlier measureTime call.
        if (builderTime.length() > 0 && builderTime.charAt(0) == SENTINEL) {
            return;
        }

        TextPaint paint = null;
        if (CHAT_TIME_PAINT != null) {
            try {
                paint = (TextPaint) CHAT_TIME_PAINT.get(null);
            } catch (Exception e) {
                // Ignore
            }
        }
        if (paint == null) {
            return;
        }

        int extraWidth = 0;
        // Deleted mark sits next to the timestamp.
        if (isDeleted) {
            SpannableStringBuilder markPrefix = buildPrefix(cell, paint);
            if (markPrefix != null) {
                markPrefix.append((CharSequence) " ");
                builderTime.insert(0, (CharSequence) markPrefix);
                if (deletedIcon != null && markPrefix.toString().contains("....")) {
                    extraWidth += AndroidUtilities.dp(16) + (int) Math.ceil(paint.measureText(" "));
                } else {
                    extraWidth += (int) Math.ceil(paint.measureText(markPrefix, 0, markPrefix.length()));
                }
            }
        }
        // Message id sits to the far left of the time block.
        if (showId) {
            SpannableStringBuilder idPrefix = new SpannableStringBuilder("ID " + mid + " ");
            builderTime.insert(0, (CharSequence) idPrefix);
            extraWidth += (int) Math.ceil(paint.measureText(idPrefix, 0, idPrefix.length()));
        }
        if (extraWidth == 0) {
            return;
        }
        // Sentinel marks this string as processed (zero width, no layout impact).
        builderTime.insert(0, (CharSequence) String.valueOf(SENTINEL));
        ReflectionUtils.set(CURRENT_TIME_STRING, cell, builderTime);

        Integer timeTextWidthGot = (Integer) ReflectionUtils.get(TIME_TEXT_WIDTH, cell);
        Integer timeWidthGot = (Integer) ReflectionUtils.get(TIME_WIDTH, cell);
        if (timeTextWidthGot != null) {
            ReflectionUtils.set(TIME_TEXT_WIDTH, cell, Integer.valueOf(timeTextWidthGot.intValue() + extraWidth));
        }
        if (timeWidthGot != null) {
            ReflectionUtils.set(TIME_WIDTH, cell, Integer.valueOf(timeWidthGot.intValue() + extraWidth));
        }
    }

    private static SpannableStringBuilder buildPrefix(ChatMessageCell cell, TextPaint paint) {
        SpannableStringBuilder builder;
        if (mark != null && !mark.isEmpty()) {
            builder = new SpannableStringBuilder(LocaleUtils.fullyFormatText(mark));
        } else if (deletedIcon != null) {
            builder = new SpannableStringBuilder("....");
            ColoredImageSpan span = new ColoredImageSpan(deletedIcon);
            if (Settings.getDeletedMarkColor() != 0) {
                span.setOverrideColor(Settings.getDeletedMarkColor());
            }
            span.setRelativeSize(paint.getFontMetricsInt());
            builder.setSpan(span, 0, builder.length(), 33);
        } else {
            return null;
        }
        if (Settings.getDeletedMarkColor() != 0) {
            builder.setSpan(new ForegroundColorSpan(Settings.getDeletedMarkColor()), 0, builder.length(), 33);
        }
        return builder;
    }
}
