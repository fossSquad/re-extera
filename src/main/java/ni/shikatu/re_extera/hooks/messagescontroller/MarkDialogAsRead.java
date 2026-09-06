package ni.shikatu.re_extera.hooks.messagescontroller;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;

public class MarkDialogAsRead extends XC_MethodHook {
    private boolean isExcludedGlobally(int currentAccount, long dialogId) {
        if (dialogId == 0) return false;
        if (dialogId > 0) {
            return Settings.getGhostExcludePMs();
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(Long.valueOf(-dialogId));
            if (chat != null) {
                boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                if (isChannel) {
                    return Settings.getGhostExcludeChannels();
                } else {
                    return Settings.getGhostExcludeGroups();
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        long dialogId = (Long) param.args[0];
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        int status = ReExteraDb.get().getDialogReading(dialogId);
        boolean shouldBlock = false;
        switch (status) {
            case Defaults.NEVER /* -1 */:
                shouldBlock = true;
                break;
            case Defaults.GLOBAL_VALUE /* 0 */:
            default:
                if (!isExcludedGlobally(currentAccount, dialogId) && Settings.getHideReadingWithGhost()) {
                    shouldBlock = true;
                }
                break;
            case Defaults.ALWAYS /* 1 */:
                break;
        }

        if (shouldBlock) {
            // Prevent ChatActivity from locally updating dialogs_read_inbox_max and marking dialog read in SQLite
            param.setResult((Object) null);
        }
    }
}
