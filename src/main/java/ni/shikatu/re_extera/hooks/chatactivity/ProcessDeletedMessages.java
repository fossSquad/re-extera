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
        if (Settings.getSaveDeletedMessages()) {
            final ChatActivity thisObject = (ChatActivity) param.thisObject;
            final long dialogId = thisObject.getDialogId();

            // Capture and nullify param.args[0] defensively
            @SuppressWarnings("unchecked")
            final ArrayList<Integer> originalMessages =
                    param.args[0] instanceof java.util.Collection
                            ? new ArrayList<>((java.util.Collection<Integer>) param.args[0])
                            : new ArrayList<>();

            ni.shikatu.re_extera.db.ReExteraDb.get().postToDbThread(new Runnable() {
                @Override
                public void run() {
                    MessageUtils.forceUpdateViews(thisObject.getCurrentAccount(), dialogId, originalMessages);
                }
            });

            // Atomically drain the queue — avoids the race condition where another
            // thread's addAll() interleaved with the old clear() on the ArrayList.
            ArrayList<Integer> drained = new ArrayList<>();
            Integer id;
            while ((id = onRequestToDelete.poll()) != null) {
                drained.add(id);
            }
            param.args[0] = drained;
        }
    }
}
