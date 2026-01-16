package ni.shikatu.re_extera.hooks.chatactivity;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;

public class ProcessNewMessages extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        char c;
        char c2;
        ArrayList<MessageObject> messages = (ArrayList) param.args[0];
        if (messages == null || messages.isEmpty()) {
            return;
        }
        ChatActivity chatActivity = (ChatActivity) param.thisObject;
        long dialogId = chatActivity.getDialogId();
        char c3 = 1;
        long j = 0;
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
        ArrayList<MessageObject> filteredMessages = new ArrayList<>(messages.size());
        for (MessageObject message2 : messages) {
            if (message2.isOut()) {
                filteredMessages.add(message2);
            } else {
                if (isGroup) {
                    long fromId = message2.getFromChatId();
                    if (fromId > j && ShadowbanCache.shouldHideInGroups(fromId)) {
                        Integer numValueOf = Integer.valueOf(message2.getId());
                        Long lValueOf = Long.valueOf(fromId);
                        Object[] objArr = new Object[2];
                        objArr[c] = numValueOf;
                        objArr[c3] = lValueOf;
                        Main.log("Filtered message (shadowban): id=%s, fromId=%s", objArr);
                    }
                }
                if (filtersEnabled) {
                    boolean byRegex = MessageUtils.shouldFilterMessage(message2);
                    long groupId2 = message2.getGroupId();
                    if (groupId2 != j) {
                        c2 = 1;
                        boolean byGroup = bannedGroupIds.contains(Long.valueOf(groupId2));
                        if (!byRegex || byGroup) {
                            Integer numValueOf2 = Integer.valueOf(message2.getId());
                            Long lValueOf2 = Long.valueOf(groupId2);
                            Object[] objArr2 = new Object[2];
                            objArr2[c] = numValueOf2;
                            objArr2[c2] = lValueOf2;
                            Main.log("Filtered message (regex): id=%s, groupId=%s", objArr2);
                            c3 = 1;
                            j = 0;
                        }
                    } else {
                        c2 = 1;
                    }
                    if (!byRegex) {
                    }
                    Integer numValueOf3 = Integer.valueOf(message2.getId());
                    Long lValueOf3 = Long.valueOf(groupId2);
                    Object[] objArr3 = new Object[2];
                    objArr3[c] = numValueOf3;
                    objArr3[c2] = lValueOf3;
                    Main.log("Filtered message (regex): id=%s, groupId=%s", objArr3);
                    c3 = 1;
                    j = 0;
                }
                filteredMessages.add(message2);
                c3 = 1;
                j = 0;
            }
        }
        param.args[c] = filteredMessages;
    }
}
