package ni.shikatu.re_extera.hooks.chatmessagecell;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import com.exteragram.messenger.utils.text.LocaleUtils;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.connectionsmanager.SendRequest;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ColoredImageSpan;

public class MeasureTime extends XC_MethodHook {
    public static Drawable deletedIcon;
    public static Drawable halfCheckIcon;
    public static Drawable fullCheckIcon;
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

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!ni.shikatu.re_extera.hooks.HookInit.isActive) return;
        TLRPC.Message message;
        SpannableStringBuilder prefix;
        if (CURRENT_TIME_STRING == null || TIME_TEXT_WIDTH == null) {
            return;
        }
        if (TIME_WIDTH == null) {
            return;
        }
        ChatMessageCell cell = (ChatMessageCell) param.thisObject;
        MessageObject obj = (MessageObject) param.args[0];
        if (obj == null || (message = obj.messageOwner) == null) {
            return;
        }
        long did = obj.getDialogId();
        if (did == 0 && message != null) {
            did = MessageUtils.getDialogIdFromMessage(message);
        }
        int mid = message.id;
        boolean isDeleted = did == -999999999L;
        if (!isDeleted) {
            try {
                org.telegram.messenger.MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
                if (group != null && group.messages != null && !group.messages.isEmpty()) {
                    isDeleted = true;
                    for (MessageObject m : group.messages) {
                        if (m != null && !m.deleted && !this.redb.messageIsDeleted(m)) {
                            isDeleted = false;
                            break;
                        }
                    }
                } else {
                    isDeleted = obj.deleted || this.redb.messageIsDeleted(did, mid);
                }
            } catch (Throwable e) {
                isDeleted = obj.deleted || this.redb.messageIsDeleted(did, mid);
            }
        }
        if (isDeleted) {
            CharSequence currentTimeString = (CharSequence) ReflectionUtils.get(CURRENT_TIME_STRING, cell);
            if (currentTimeString == null) {
                return;
            }
            if (currentTimeString instanceof SpannableStringBuilder) {
                SpannableStringBuilder ssb = (SpannableStringBuilder) currentTimeString;
                ColoredImageSpan[] spans = ssb.getSpans(0, ssb.length(), ColoredImageSpan.class);
                if (spans != null && spans.length > 0) {
                    return;
                }
            }
            if (mark != null && !mark.isEmpty() && currentTimeString.toString().startsWith(LocaleUtils.fullyFormatText(mark).toString())) {
                return;
            }
            SpannableStringBuilder builderTime;
            if (currentTimeString instanceof SpannableStringBuilder) {
                builderTime = (SpannableStringBuilder) currentTimeString;
            } else {
                builderTime = new SpannableStringBuilder(currentTimeString);
            }
            
            TextPaint paint = null;
            if (CHAT_TIME_PAINT != null) {
                try {
                    paint = (TextPaint) CHAT_TIME_PAINT.get(null);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            if (paint == null || (prefix = buildPrefix(cell, paint)) == null) {
                return;
            }
            prefix.append((CharSequence) " ");
            builderTime.insert(0, (CharSequence) prefix);
            ReflectionUtils.set(CURRENT_TIME_STRING, cell, builderTime);
            int extraWidth;
            if (deletedIcon != null && prefix.toString().contains("....")) {
                extraWidth = AndroidUtilities.dp(16) + (int) Math.ceil(paint.measureText(" "));
            } else {
                extraWidth = (int) Math.ceil(paint.measureText(prefix, 0, prefix.length()));
            }
            Integer timeTextWidthGot = (Integer) ReflectionUtils.get(TIME_TEXT_WIDTH, cell);
            Integer timeWidthGot = (Integer) ReflectionUtils.get(TIME_WIDTH, cell);
            if (timeTextWidthGot != null) {
                ReflectionUtils.set(TIME_TEXT_WIDTH, cell, Integer.valueOf(timeTextWidthGot.intValue() + extraWidth));
            }
            if (timeWidthGot != null) {
                ReflectionUtils.set(TIME_WIDTH, cell, Integer.valueOf(timeWidthGot.intValue() + extraWidth));
            }
            return;
        }

        handleIncomingReadStatus(cell, obj, did, mid);
    }

    private static void handleIncomingReadStatus(ChatMessageCell cell, MessageObject obj, long did, int mid) {
        if (obj == null || obj.isOutOwner() || obj.isOut() || mid <= 0 || did == 0) {
            return;
        }
        int readStatusPref = Settings.getShowServerReadStatus();
        if (readStatusPref == 0) {
            return;
        }
        if (readStatusPref == 1) { // Only in Ghost mode (when Hide Reading is active)
            boolean hideReading = Settings.getHideReadingWithGhost() || SendRequest.getCurrentReadingStatus() == -1;
            boolean alwaysRead = SendRequest.getCurrentReadingStatus() == 1;
            if (!hideReading || alwaysRead) {
                return;
            }
        }

        CharSequence currentTimeString = (CharSequence) ReflectionUtils.get(CURRENT_TIME_STRING, cell);
        if (currentTimeString == null || currentTimeString.length() == 0) {
            return;
        }
        if (currentTimeString instanceof SpannableStringBuilder) {
            SpannableStringBuilder ssb = (SpannableStringBuilder) currentTimeString;
            IncomingCheckSpan[] existing = ssb.getSpans(0, ssb.length(), IncomingCheckSpan.class);
            if (existing != null && existing.length > 0) {
                return;
            }
        }

        int maxReadInboxId = getServerReadInboxMaxId(obj.currentAccount, did, obj);
        boolean isReadOnServer = maxReadInboxId > 0 && mid <= maxReadInboxId;
        int style = Settings.getServerReadStatusStyle(); // 0: Only unread (single check), 1: Both unread & read (single or double check)
        if (isReadOnServer && style == 0) {
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

        SpannableStringBuilder checkSpan = buildIncomingCheckSpan(isReadOnServer, paint);
        if (checkSpan == null) {
            return;
        }

        SpannableStringBuilder builderTime;
        if (currentTimeString instanceof SpannableStringBuilder) {
            builderTime = (SpannableStringBuilder) currentTimeString;
        } else {
            builderTime = new SpannableStringBuilder(currentTimeString);
        }

        builderTime.append((CharSequence) " ");
        builderTime.append(checkSpan);
        ReflectionUtils.set(CURRENT_TIME_STRING, cell, builderTime);

        int extraWidth = (isReadOnServer ? AndroidUtilities.dp(14) : AndroidUtilities.dp(10)) + (int) Math.ceil(paint.measureText(" "));
        Integer timeTextWidthGot = (Integer) ReflectionUtils.get(TIME_TEXT_WIDTH, cell);
        Integer timeWidthGot = (Integer) ReflectionUtils.get(TIME_WIDTH, cell);
        if (timeTextWidthGot != null) {
            ReflectionUtils.set(TIME_TEXT_WIDTH, cell, Integer.valueOf(timeTextWidthGot.intValue() + extraWidth));
        }
        if (timeWidthGot != null) {
            ReflectionUtils.set(TIME_WIDTH, cell, Integer.valueOf(timeWidthGot.intValue() + extraWidth));
        }
    }

    private static int getServerReadInboxMaxId(int currentAccount, long did, MessageObject obj) {
        try {
            MessagesController mc = MessagesController.getInstance(currentAccount);
            if (mc == null) return 0;

            // Check forum topic first if applicable
            if (obj != null && obj.messageOwner != null && obj.messageOwner.reply_to != null) {
                int replyToMsgId = obj.messageOwner.reply_to.reply_to_msg_id;
                if (replyToMsgId != 0 && mc.getTopicsController() != null) {
                    TLRPC.TL_forumTopic topic = mc.getTopicsController().findTopic(-did, replyToMsgId);
                    if (topic != null && topic.read_inbox_max_id > 0) {
                        return topic.read_inbox_max_id;
                    }
                }
            }

            Integer cachedMax = mc.dialogs_read_inbox_max.get(did);
            if (cachedMax != null && cachedMax > 0) {
                return cachedMax;
            }

            TLRPC.Dialog dialog = mc.getDialog(did);
            if (dialog != null && dialog.read_inbox_max_id > 0) {
                return dialog.read_inbox_max_id;
            }
        } catch (Throwable t) {
            // Ignore
        }
        return 0;
    }

    private static SpannableStringBuilder buildIncomingCheckSpan(boolean isReadOnServer, TextPaint paint) {
        SpannableStringBuilder builder;
        Drawable icon = isReadOnServer ? fullCheckIcon : halfCheckIcon;
        if (icon != null) {
            builder = new SpannableStringBuilder("..");
            IncomingCheckSpan span = new IncomingCheckSpan(icon);
            span.setRelativeSize(paint.getFontMetricsInt());
            builder.setSpan(span, 0, builder.length(), 33);
        } else {
            builder = new SpannableStringBuilder(isReadOnServer ? "✓✓" : "✓");
            builder.setSpan(new IncomingCheckSpan(null), 0, builder.length(), 33);
        }
        return builder;
    }

    private static class IncomingCheckSpan extends ColoredImageSpan {
        public IncomingCheckSpan(Drawable drawable) {
            super(drawable != null ? drawable : Theme.chat_msgOutHalfCheckDrawable);
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
