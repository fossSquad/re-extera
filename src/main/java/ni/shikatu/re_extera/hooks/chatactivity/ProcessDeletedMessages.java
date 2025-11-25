package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.ui.ChatActivity;

public class ProcessDeletedMessages extends XC_MethodHook {
    public static ArrayList<Integer> onRequestToDelete = new ArrayList<>();

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getSaveDeletedMessages()) {
            ChatActivity thisObject = (ChatActivity) param.thisObject;
            ArrayList<Integer> deleted = new ArrayList<>();
            long dialogid = thisObject.getDialogId();
            MessageUtils.forceUpdateViews(dialogid, deleted);
            param.args[0] = onRequestToDelete;
            onRequestToDelete.clear();
        }
    }
}
