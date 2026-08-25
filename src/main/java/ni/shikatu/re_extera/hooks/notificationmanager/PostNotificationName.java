package ni.shikatu.re_extera.hooks.notificationmanager;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.hooks.messagescontroller.DeletionState;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.NotificationCenter;

/**
 * Suppresses the {@code messagesDeleted} broadcast for server-side deletions.
 *
 * <p>re:extera already keeps deleted messages in Telegram's own storage (via the
 * {@code markMessagesAsDeleted} hooks) and records them in its cache, but it did
 * not stop the {@code messagesDeleted} notification. That broadcast reaches every
 * consumer — the open chat, the dialog list, shared media, search — and each one
 * removes the message from view, which is why deleted messages "sometimes"
 * vanished. Swallowing the broadcast at its single source (as TeleVip does) keeps
 * the message visible everywhere; a user's own delete still passes through because
 * {@link DeletionState} flags it.
 *
 * <p>Hooks {@code NotificationCenter.postNotificationName(int, Object[])} — a very
 * hot path, so the body stays a cheap int compare until it matches.
 */
public class PostNotificationName extends XC_MethodHook {

    private boolean isMessagesDeleted(XC_MethodHook.MethodHookParam param) {
        return param.args.length > 0
                && param.args[0] instanceof Integer
                && ((Integer) param.args[0]).intValue() == NotificationCenter.messagesDeleted;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!isMessagesDeleted(param)) {
            return;
        }
        // Swallow only server-pushed deletions while the feature is on; a real
        // user delete (flagged by DeletionState) must still remove the message.
        if (Settings.getSaveDeletedMessages() && !DeletionState.isUserDelete()) {
            param.setResult((Object) null);
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        // The user delete has now emitted its messagesDeleted broadcast; reset so
        // subsequent server deletions are intercepted again.
        if (isMessagesDeleted(param)) {
            DeletionState.clearUserDelete();
        }
    }
}
