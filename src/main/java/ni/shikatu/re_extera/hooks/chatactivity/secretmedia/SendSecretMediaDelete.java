package ni.shikatu.re_extera.hooks.chatactivity.secretmedia;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.MessageObject;

public class SendSecretMediaDelete extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveOneTimeMessages()) {
            MessageObject obj = (MessageObject) param.args[0];
            if (obj != null) {
                obj.forceExpired = false;
            }
            param.setResult(new Runnable() { // from class: ni.shikatu.re_extera.hooks.chatactivity.secretmedia.SendSecretMediaDelete$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SendSecretMediaDelete.lambda$beforeHookedMethod$0();
                }
            });
        }
    }

    static /* synthetic */ void lambda$beforeHookedMethod$0() {
    }
}
