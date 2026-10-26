package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.ui.ChatActivity;

public class ProcessDeletedMessages extends XC_MethodHook {
    // ConcurrentLinkedQueue is thread-safe: addAll from InternalUtils (any thread)
    // and drain here (UI thread) no longer race against each other.
    public static final ConcurrentLinkedQueue<Integer> onRequestToDelete = new ConcurrentLinkedQueue<>();

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        final ChatActivity thisObject = (ChatActivity) param.thisObject;
        final long dialogId = thisObject.getDialogId();
        if (!ni.shikatu.re_extera.db.ReExteraDb.get().shouldSaveDeletedMessages(dialogId)) {
            return;
        }

        @SuppressWarnings("unchecked")
        final ArrayList<Integer> originalMessages =
                param.args[0] instanceof java.util.Collection
                        ? new ArrayList<>((java.util.Collection<Integer>) param.args[0])
                        : new ArrayList<>();

        final ArrayList<Integer> validIds = new ArrayList<>();
        final ArrayList<Integer> tempIds = new ArrayList<>();
        for (Integer id : originalMessages) {
            if (id != null && id > 0) {
                validIds.add(id);
            } else if (id != null) {
                tempIds.add(id);
            }
        }

        if (!validIds.isEmpty()) {
            ni.shikatu.re_extera.db.ReExteraDb.get().batchPutDeletedMessagesAsync(dialogId, validIds);
            MessageUtils.forceUpdateViews(thisObject.getCurrentAccount(), dialogId, validIds);
            if (Settings.getSaveAttachments()) {
                ni.shikatu.re_extera.utils.AttachmentSaver.saveAttachments(thisObject.getCurrentAccount(), dialogId, validIds);
            }
        }

        ArrayList<Integer> drained = new ArrayList<>(tempIds);
        Integer id;
        while ((id = onRequestToDelete.poll()) != null) {
            drained.add(id);
        }
        param.args[0] = drained;
    }
}
