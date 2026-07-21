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
            final ChatActivity thisObject = (ChatActivity) param.thisObject;
            final long dialogId = thisObject.getDialogId();
            final ArrayList<Integer> originalMessages = new ArrayList<>((java.util.Collection<Integer>) param.args[0]);
            ni.shikatu.re_extera.db.ReExteraDb.get().postToDbThread(new Runnable() {
                @Override
                public void run() {
                    MessageUtils.forceUpdateViews(thisObject.getCurrentAccount(), dialogId, originalMessages);
                }
            });
            param.args[0] = new ArrayList<>(onRequestToDelete);
            onRequestToDelete.clear();
        }
    }
}
