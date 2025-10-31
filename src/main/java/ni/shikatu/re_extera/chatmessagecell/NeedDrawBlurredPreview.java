package ni.shikatu.re_extera.chatmessagecell;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;
import org.telegram.messenger.MessageObject;

public class NeedDrawBlurredPreview extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        MessageObject msgObj = (MessageObject) param.thisObject;
        if (Settings.getSaveOneTimeMessages() && Settings.getDisableBlurOnOneTimeMessages() && !msgObj.isPaid()) {
            param.setResult(false);
        }
    }
}
