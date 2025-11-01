package ni.shikatu.re_extera;

import android.text.SpannableStringBuilder;
import android.text.TextPaint;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;

public class MeasureTimeHook extends XC_MethodHook {
	public static Field currentMessageObject;
	public static Field currentTimeString;
	public static Field timeTextWidth;
	public static Field timeWidth;

	public static String mark = "deleted";
	public static void notifyMarkChanged(String to){
		mark = to;
	}

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
		} catch (Exception e){}
	}
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		ChatMessageCell cell = (ChatMessageCell) param.thisObject;
		MessageObject obj = (MessageObject) param.args[0];
		if(obj == null) return;
		TLRPC.Message message = obj.messageOwner;
		if(message == null) return;
		long did = Utils.getDialogIdFromMessage(message);
		int mid = message.id;

		if(DbDeletedStore.get().exists(did, mid) || Main.cachedDeleted.contains(did + "_" + mid)){
			CharSequence currentTimeStringGot = (CharSequence) currentTimeString.get(cell);
			if(currentTimeStringGot == null) return;
			SpannableStringBuilder builder = new SpannableStringBuilder(mark);
			builder.append(" ");
			((SpannableStringBuilder) currentTimeStringGot).insert(0, builder);
			currentTimeString.set(cell, currentTimeStringGot);
			TextPaint paint = Theme.chat_timePaint;
			if(paint != null){
				int ceil = (int) Math.ceil(paint.measureText(builder, 0, builder.length()));
				int timeTextWidthGoh = (int) timeTextWidth.get(cell);
				int timeWidthGot = (int) timeWidth.get(cell);
				timeTextWidth.set(cell, ceil + timeTextWidthGoh);
				timeWidth.set(cell, ceil + timeWidthGot);
			}
		}
	}
}
