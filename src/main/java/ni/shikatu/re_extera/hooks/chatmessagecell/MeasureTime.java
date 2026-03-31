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
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ColoredImageSpan;

public class MeasureTime extends XC_MethodHook {
    public static Field currentMessageObject;
    public static Field currentTimeString;
    public static Drawable deletedIcon;
    public static String mark = Settings.getCustomPrefix();
    public static Field timeTextWidth;
    public static Field timeWidth;
    public ReExteraDb redb = ReExteraDb.get();

    static {
        try {
            currentMessageObject = ChatMessageCell.class.getDeclaredField("currentMessageObject");
            currentMessageObject.setAccessible(true);
            currentTimeString = ChatMessageCell.class.getDeclaredField("currentTimeString");
            currentTimeString.setAccessible(true);
            timeTextWidth = ChatMessageCell.class.getDeclaredField("timeTextWidth");
            timeTextWidth.setAccessible(true);
            timeWidth = ChatMessageCell.class.getDeclaredField("timeWidth");
            timeWidth.setAccessible(true);
        } catch (Exception e) {
            currentMessageObject = null;
            currentTimeString = null;
            timeTextWidth = null;
            timeWidth = null;
        }
    }

    public static void notifyMarkChanged(String to) {
        mark = to;
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
        TLRPC.Message message;
        CharSequence charSequence;
        TextPaint textPaint;
        SpannableStringBuilder spannableStringBuilder;
        if (currentTimeString == null || timeTextWidth == null || timeWidth == null) {
            return;
        }
        ChatMessageCell chatMessageCell = (ChatMessageCell) methodHookParam.thisObject;
        MessageObject messageObject = (MessageObject) methodHookParam.args[0];
        if (messageObject == null || (message = messageObject.messageOwner) == null) {
            return;
        }
        if (this.redb.messageIsDeleted(MessageUtils.getDialogIdFromMessage(message), message.id) && (charSequence = (CharSequence) ReflectionUtils.get(currentTimeString, chatMessageCell)) != null && (textPaint = Theme.chat_timePaint) != null) {
            if (mark != null && !mark.isEmpty()) {
                SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(LocaleUtils.fullyFormatText(mark));
                spannableStringBuilder2.setSpan(spannableStringBuilder2, 0, spannableStringBuilder2.length(), 33);
                spannableStringBuilder = spannableStringBuilder2;
            } else {
                SpannableStringBuilder spannableStringBuilder3 = new SpannableStringBuilder("....");
                ColoredImageSpan coloredImageSpan = new ColoredImageSpan(deletedIcon);
                if (Settings.getRedMark()) {
                    coloredImageSpan.setOverrideColor(chatMessageCell.getThemedColor(Theme.key_color_red));
                }
                coloredImageSpan.setRelativeSize(textPaint.getFontMetricsInt());
                spannableStringBuilder3.setSpan(coloredImageSpan, 0, spannableStringBuilder3.length(), 0);
                spannableStringBuilder = spannableStringBuilder3;
            }
            if (Settings.getRedMark()) {
                spannableStringBuilder.setSpan(new ForegroundColorSpan(chatMessageCell.getThemedColor(Theme.key_color_red)), 0, spannableStringBuilder.length(), 33);
            }
            spannableStringBuilder.append(" ");
            ((SpannableStringBuilder) charSequence).insert(0, (CharSequence) spannableStringBuilder);
            ReflectionUtils.set(currentTimeString, chatMessageCell, charSequence);
            int iCeil = (int) Math.ceil(textPaint.measureText(spannableStringBuilder, 0, spannableStringBuilder.length()));
            int iIntValue = ((Integer) ReflectionUtils.get(timeTextWidth, chatMessageCell)).intValue();
            int iIntValue2 = ((Integer) ReflectionUtils.get(timeWidth, chatMessageCell)).intValue();
            ReflectionUtils.set(timeTextWidth, chatMessageCell, Integer.valueOf(iCeil + iIntValue));
            ReflectionUtils.set(timeWidth, chatMessageCell, Integer.valueOf(iCeil + iIntValue2));
            chatMessageCell.requestLayout();
            chatMessageCell.invalidate();
        }
    }
}
