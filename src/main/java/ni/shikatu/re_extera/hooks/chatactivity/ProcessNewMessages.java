package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;

public class ProcessNewMessages extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        char c;
        ArrayList<MessageObject> messages = (ArrayList) param.args[0];
        if (messages == null || messages.isEmpty()) {
            return;
        }
        ChatActivity chatActivity = (ChatActivity) param.thisObject;
        long dialogId = chatActivity.getDialogId();
        boolean isGroup = dialogId < 0;
        boolean filtersEnabled = Settings.getFiltersEnabled();
        Set<Long> bannedGroupIds = new HashSet<>();
        if (!filtersEnabled) {
            c = 0;
        } else {
            for (MessageObject message : messages) {
                if (!message.isOut()) {
                    if (MessageUtils.shouldFilterMessage(message)) {
                        long groupId = message.getGroupId();
                        if (groupId != 0) {
                            bannedGroupIds.add(Long.valueOf(groupId));
                        }
                    }
                }
            }
            c = 0;
        }
        ArrayList<MessageObject> filtered = new ArrayList<>(messages.size());
        for (MessageObject message2 : messages) {
            if (message2.isOut()) {
                filtered.add(message2);
            } else {
                if (isGroup) {
                    long fromId = message2.getFromChatId();
                    if (fromId <= 0 || !ShadowbanCache.shouldHideInGroups(fromId)) {
                    }
                }
                if (filtersEnabled) {
                    long groupId2 = message2.getGroupId();
                    boolean byGroup = groupId2 != 0 && bannedGroupIds.contains(Long.valueOf(groupId2));
                    if (byGroup || MessageUtils.shouldFilterMessage(message2)) {
                    }
                }
                filtered.add(message2);
            }
        }
        param.args[c] = filtered;
    }
}
