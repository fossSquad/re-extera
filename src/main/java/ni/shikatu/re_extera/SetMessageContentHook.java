package ni.shikatu.re_extera;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.Cells.ChatMessageCell;

import de.robv.android.xposed.XC_MethodHook;

public class SetMessageContentHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getEnableAlpha()) return;

 		ChatMessageCell cell = (ChatMessageCell) param.thisObject;
		cell.setAlpha(1.0f);
	}
}
