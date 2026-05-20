package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.ui.ChatActivity;

public class ProcessDeletedMessages extends XC_MethodHook {
    public static final ArrayList<Integer> onRequestToDelete = new ArrayList<>();

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getSaveDeletedMessages()) {
            ChatActivity thisObject = (ChatActivity) param.thisObject;
            long dialogId = thisObject.getDialogId();
            MessageUtils.forceUpdateViews(thisObject.getCurrentAccount(), dialogId, new ArrayList());
            param.args[0] = new ArrayList(onRequestToDelete);
            onRequestToDelete.clear();
        }
    }
}
