package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
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

        // 1. Ignore if scheduled messages being sent (sent == true)
        if (param.args.length > 2 && param.args[2] instanceof Boolean && ((Boolean) param.args[2]).booleanValue()) {
            return;
        }

        // 2. Ignore if ChatActivity is currently displaying scheduled messages
        if (thisObject.getChatMode() == ChatActivity.MODE_SCHEDULED) {
            return;
        }

        // 3. Verify that the deletion event actually matches this ChatActivity
        long channelId = param.args.length > 1 && param.args[1] instanceof Long ? ((Long) param.args[1]).longValue() : 0;
        TLRPC.Chat currentChat = thisObject.getCurrentChat();
        if (ChatObject.isChannel(currentChat)) {
            if (channelId == 0 && thisObject.getMergeDialogId() != 0) {
                // allowed for merged dialog
            } else if (channelId != -dialogId) {
                return; // Not for this channel
            }
        } else if (channelId != 0) {
            return; // Not for this non-channel chat
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
                // Only save if the message actually belongs to this chat's loaded messages
                boolean hasMsg = false;
                try {
                    if (thisObject.messages != null) {
                        for (int i = 0; i < thisObject.messages.size(); i++) {
                            MessageObject obj = thisObject.messages.get(i);
                            if (obj != null && obj.getId() == id.intValue()) {
                                hasMsg = true;
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                if (hasMsg) {
                    validIds.add(id);
                }
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
