package ni.shikatu.re_extera;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;


public class FillMessageMenuHook extends XC_MethodHook {
	@Override
	@SuppressWarnings("unchecked")
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		if(!Settings.getSaveEditedMessages()) return;
		Global.log("adding a messagemenu fill");
		ProcessSelectedOptionHook.selectedObject = (MessageObject) param.args[0];
		if(DbDeletedStore.get().hasEdits(ProcessSelectedOptionHook.selectedObject.getDialogId(), ProcessSelectedOptionHook.selectedObject.getId())){
			((ArrayList<Integer>) param.args[1]).add(0, R.drawable.msg2_trending);
			((ArrayList<CharSequence>) param.args[2]).add(0, Localization.MESSAGE_HISTORY);
			((ArrayList<Integer>) param.args[3]).add(0, 6363);
		}

	}
}
