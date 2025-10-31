package ni.shikatu.re_extera.chatmessagecell;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.Settings;
import ni.shikatu.re_extera.Utils;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ColoredImageSpan;

public class MeasureTimeHook extends XC_MethodHook {
    public static Field currentMessageObject;
    public static Field currentTimeString;
    public static Drawable deletedIcon;
    public static String mark = Settings.getCustomPrefix();
    public static Field timeTextWidth;
    public static Field timeWidth;

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
        ChatMessageCell chatMessageCell = (ChatMessageCell) methodHookParam.thisObject;
        MessageObject messageObject = (MessageObject) methodHookParam.args[0];
        if (messageObject == null || (message = messageObject.messageOwner) == null) {
            return;
        }
        long dialogIdFromMessage = Utils.getDialogIdFromMessage(message);
        int i = message.id;
        if ((DbDeletedStore.get().exists(dialogIdFromMessage, i) || Main.cachedDeleted.contains(dialogIdFromMessage + "_" + i)) && (charSequence = (CharSequence) currentTimeString.get(chatMessageCell)) != null && (textPaint = Theme.chat_timePaint) != null) {
            if (mark != null && !mark.isEmpty()) {
                SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(Utils.fullyFormatText(mark));
                spannableStringBuilder2.setSpan(spannableStringBuilder2, 0, spannableStringBuilder2.length(), 33);
                if (Settings.getRedMark()) {
                    spannableStringBuilder2.setSpan(new ForegroundColorSpan(chatMessageCell.getThemedColor(Theme.key_color_red)), 0, spannableStringBuilder2.length(), 33);
                }
                spannableStringBuilder2.append(" ");
                ((SpannableStringBuilder) charSequence).insert(0, (CharSequence) spannableStringBuilder2);
                spannableStringBuilder = spannableStringBuilder2;
            } else {
                SpannableStringBuilder spannableStringBuilder3 = new SpannableStringBuilder("....");
                ColoredImageSpan coloredImageSpan = new ColoredImageSpan(deletedIcon);
                if (Settings.getRedMark()) {
                    coloredImageSpan.setOverrideColor(chatMessageCell.getThemedColor(Theme.key_color_red));
                }
                coloredImageSpan.setRelativeSize(textPaint.getFontMetricsInt());
                spannableStringBuilder3.setSpan(coloredImageSpan, 0, spannableStringBuilder3.length(), 0);
                if (Settings.getRedMark()) {
                    spannableStringBuilder3.setSpan(new ForegroundColorSpan(chatMessageCell.getThemedColor(Theme.key_color_red)), 0, spannableStringBuilder3.length(), 33);
                }
                spannableStringBuilder3.append(" ");
                ((SpannableStringBuilder) charSequence).insert(0, (CharSequence) spannableStringBuilder3);
                spannableStringBuilder = spannableStringBuilder3;
            }
            currentTimeString.set(chatMessageCell, charSequence);
            int iCeil = (int) Math.ceil(textPaint.measureText(spannableStringBuilder, 0, spannableStringBuilder.length()));
            int iIntValue = ((Integer) timeTextWidth.get(chatMessageCell)).intValue();
            int iIntValue2 = ((Integer) timeWidth.get(chatMessageCell)).intValue();
            timeTextWidth.set(chatMessageCell, Integer.valueOf(iCeil + iIntValue));
            timeWidth.set(chatMessageCell, Integer.valueOf(iCeil + iIntValue2));
            chatMessageCell.requestLayout();
            chatMessageCell.invalidate();
        }
    }
}
