package ni.shikatu.re_extera.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Settings;

public class SendSecretMediaReadHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveOneTimeMessages()) {
            param.setResult(new Runnable() { // from class: ni.shikatu.re_extera.chatactivity.SendSecretMediaReadHook$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SendSecretMediaReadHook.lambda$beforeHookedMethod$0();
                }
            });
        }
    }

    static /* synthetic */ void lambda$beforeHookedMethod$0() {
    }
}
