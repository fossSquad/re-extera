package ni.shikatu.re_extera.hooks.messagescontroller;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.ServerReadTracker;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

/**
 * Runs before MessagesController.markDialogAsRead.
 *
 * Client-side reading in ghost mode is controlled by Settings.getClientSideRead()
 * (default true): when enabled, messages are marked read locally (the convenient
 * "read, exit, counter still unread" behavior) while the outgoing readHistory RPC
 * is still intercepted by SendRequest. When disabled, local reading is fully
 * blocked in ghost mode (per-dialog "Never read" also always blocks).
 *
 * Before any possible blocking we snapshot the persisted server read position
 * into ServerReadTracker so incoming-message read checkmarks stay accurate.
 */
public class MarkDialogAsRead extends XC_MethodHook {
    @Override
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        long dialogId = (Long) param.args[0];
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        int value = 0;
        try {
            MessagesController mc = MessagesController.getInstance(currentAccount);
            if (mc != null) {
                TLRPC.Dialog d = mc.getDialog(dialogId);
                if (d != null && d.read_inbox_max_id > 0) {
                    value = d.read_inbox_max_id;
                }
                Integer mapValue = mc.dialogs_read_inbox_max.get(dialogId);
                if (mapValue != null && mapValue.intValue() > value) {
                    value = mapValue.intValue();
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
        if (value > 0) {
            ServerReadTracker.seed(currentAccount, dialogId, value);
        }

        int status = ReExteraDb.get().getDialogReading(dialogId);
        boolean shouldBlock = false;
        switch (status) {
            case Defaults.NEVER:
                shouldBlock = true;
                break;
            case Defaults.GLOBAL_VALUE:
            default:
                if (!isExcludedGlobally(currentAccount, dialogId) && Settings.getHideReadingWithGhost()) {
                    shouldBlock = true;
                }
                break;
            case Defaults.ALWAYS:
                break;
        }
        if (shouldBlock && (status == Defaults.NEVER || !Settings.getClientSideRead())) {
            param.setResult(null);
        }
    }

    private boolean isExcludedGlobally(int currentAccount, long dialogId) {
        if (dialogId == 0) return false;
        if (dialogId > 0) {
            return Settings.getGhostExcludePMs();
        }
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(Long.valueOf(-dialogId));
        if (chat != null) {
            boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
            if (isChannel) {
                return Settings.getGhostExcludeChannels();
            }
            return Settings.getGhostExcludeGroups();
        }
        return false;
    }
}
