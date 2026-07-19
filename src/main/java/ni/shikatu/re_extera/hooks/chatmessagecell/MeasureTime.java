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

public class MeasureTime extends XC_MethodHook {
    public static Drawable deletedIcon;
    private final ReExteraDb redb = ReExteraDb.get();
    private static final Field CURRENT_TIME_STRING = field("currentTimeString");
    private static final Field TIME_TEXT_WIDTH = field("timeTextWidth");
    private static final Field TIME_WIDTH = field("timeWidth");
    public static String mark = Settings.getCustomPrefix();

    private static Field field(String name) {
        try {
            Field f = ChatMessageCell.class.getDeclaredField(name);
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
        long did = MessageUtils.getDialogIdFromMessage(message);
        int mid = message.id;
        if (this.redb.messageIsDeleted(did, mid)) {
            CharSequence currentTimeString = (CharSequence) ReflectionUtils.get(CURRENT_TIME_STRING, cell);
            if (!(currentTimeString instanceof SpannableStringBuilder)) {
                return;
            }
            SpannableStringBuilder builderTime = (SpannableStringBuilder) currentTimeString;
            TextPaint paint = Theme.chat_timePaint;
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
            cell.requestLayout();
            cell.invalidate();
        }
    }

    private static SpannableStringBuilder buildPrefix(ChatMessageCell cell, TextPaint paint) {
        SpannableStringBuilder builder;
        if (mark != null && !mark.isEmpty()) {
            builder = new SpannableStringBuilder(LocaleUtils.fullyFormatText(mark));
        } else if (deletedIcon != null) {
            builder = new SpannableStringBuilder("....");
            ColoredImageSpan span = new ColoredImageSpan(deletedIcon);
            if (Settings.getRedMark()) {
                span.setOverrideColor(cell.getThemedColor(Theme.key_color_red));
            }
            span.setRelativeSize(paint.getFontMetricsInt());
            builder.setSpan(span, 0, builder.length(), 33);
        } else {
            return null;
        }
        if (Settings.getRedMark()) {
            builder.setSpan(new ForegroundColorSpan(cell.getThemedColor(Theme.key_color_red)), 0, builder.length(), 33);
        }
        return builder;
    }
}
