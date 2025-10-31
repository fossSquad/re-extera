package ni.shikatu.re_extera.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;
import org.telegram.messenger.MessageObject;

public class SendSecretMediaDeleteHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        MessageObject obj = (MessageObject) param.args[0];
        if (Settings.getSaveOneTimeMessages()) {
            obj.forceExpired = false;
            param.setResult(new Runnable() { // from class: ni.shikatu.re_extera.chatactivity.SendSecretMediaDeleteHook$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SendSecretMediaDeleteHook.lambda$beforeHookedMethod$0();
                }
            });
        }
    }

    static /* synthetic */ void lambda$beforeHookedMethod$0() {
    }
}
