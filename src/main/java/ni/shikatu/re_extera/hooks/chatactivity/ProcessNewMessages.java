package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import org.telegram.messenger.MessageObject;

public class ProcessNewMessages extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        ArrayList<MessageObject> messages;
        if (!Settings.getFiltersEnabled() || (messages = (ArrayList) param.args[0]) == null || messages.isEmpty()) {
            return;
        }
        Set<Long> bannedGroupIds = new HashSet<>();
        for (MessageObject message : messages) {
            if (!message.isOut() && MessageUtils.shouldFilterMessage(message)) {
                long groupId = message.getGroupId();
                if (groupId != 0) {
                    bannedGroupIds.add(Long.valueOf(groupId));
                }
            }
        }
        ArrayList<MessageObject> filteredMessages = new ArrayList<>(messages.size());
        for (MessageObject message2 : messages) {
            if (message2.isOut()) {
                filteredMessages.add(message2);
            } else {
                boolean byRegex = MessageUtils.shouldFilterMessage(message2);
                long groupId2 = message2.getGroupId();
                boolean byGroup = groupId2 != 0 && bannedGroupIds.contains(Long.valueOf(groupId2));
                if (byRegex || byGroup) {
                    Main.log("Filtered message (new): id=%s, groupId=%s", Integer.valueOf(message2.getId()), Long.valueOf(groupId2));
                } else {
                    filteredMessages.add(message2);
                }
            }
        }
        param.args[0] = filteredMessages;
    }
}
