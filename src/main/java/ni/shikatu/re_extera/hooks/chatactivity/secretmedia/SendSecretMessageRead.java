package ni.shikatu.re_extera.hooks.chatactivity.secretmedia;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

public class SendSecretMessageRead extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveOneTimeMessages()) {
            param.setResult(new Runnable() { // from class: ni.shikatu.re_extera.hooks.chatactivity.secretmedia.SendSecretMessageRead$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SendSecretMessageRead.lambda$beforeHookedMethod$0();
                }
            });
        }
    }

    static /* synthetic */ void lambda$beforeHookedMethod$0() {
    }
}
