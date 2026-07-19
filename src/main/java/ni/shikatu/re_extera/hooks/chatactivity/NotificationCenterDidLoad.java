package ni.shikatu.re_extera.hooks.chatactivity;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class NotificationCenterDidLoad extends XC_MethodHook {
    private static final Field FORWARD_END_REACHED_FIELD = field("forwardEndReached");
    private static final Field LOADING_FIELD = field("loading");
    private static final Field LOADING_FORWARD_FIELD = field("loadingForward");
    private static final Field PROGRESS_VIEW_FIELD = field("progressView");

    private static Field field(String name) {
        try {
            Field f = ChatActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            Main.log("NotificationCenterDidLoad: field '%s' not found: %s", name, e.getMessage());
            return null;
        }
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int id = ((Integer) param.args[0]).intValue();
        Object[] args = (Object[]) param.args[2];
        if (id == NotificationCenter.messagesDidLoad && args != null && args.length > 3) {
            ArrayList<MessageObject> messArr = (ArrayList) args[2];
            int originalSize = messArr.size();
            long dialogId = ((Long) args[0]).longValue();
            boolean isGroup = dialogId < 0;
            filterMessagesList(messArr, isGroup);
            int filteredSize = messArr.size();
            if (filteredSize != originalSize) {
                args[1] = Integer.valueOf(filteredSize);
                Main.log("messagesDidLoad: filtered %d->%d (%d removed)", Integer.valueOf(originalSize), Integer.valueOf(filteredSize), Integer.valueOf(originalSize - filteredSize));
            }
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        boolean[] forwardEndReached;
        if (Settings.getFiltersEnabled()) {
            int id = ((Integer) param.args[0]).intValue();
            Object[] args = (Object[]) param.args[2];
            if (id != NotificationCenter.messagesDidLoad && id != NotificationCenter.messagesDidLoadWithoutProcess) {
                return;
            }
            ChatActivity lastFragment = (ChatActivity) LaunchActivity.getLastFragment();
            if (!(lastFragment instanceof ChatActivity)) {
                return;
            }
            final ChatActivity chatActivity = lastFragment;
            if (args == null || args.length <= 9 || ((Integer) args[8]).intValue() != 1) {
                return;
            }
            try {
                if (FORWARD_END_REACHED_FIELD != null && (forwardEndReached = (boolean[]) ReflectionUtils.get(FORWARD_END_REACHED_FIELD, chatActivity)) != null && ((Boolean) args[9]).booleanValue()) {
                    forwardEndReached[0] = true;
                }
                if (LOADING_FIELD != null) {
                    ReflectionUtils.set(LOADING_FIELD, chatActivity, false);
                }
                if (LOADING_FORWARD_FIELD != null) {
                    ReflectionUtils.set(LOADING_FORWARD_FIELD, chatActivity, false);
                }
                if (PROGRESS_VIEW_FIELD != null) {
                    AndroidUtilities.runOnUIThread(new Runnable() { 
                        @Override // java.lang.Runnable
                        public final void run() {
                            NotificationCenterDidLoad.lambda$afterHookedMethod$0(chatActivity);
                        }
                    }, 50L);
                }
            } catch (Exception e) {
                Main.log("NotificationCenterDidLoad.after: %s", e.getMessage());
            }
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(ChatActivity chatActivity) {
        View progressView = (View) ReflectionUtils.get(PROGRESS_VIEW_FIELD, chatActivity);
        if (progressView != null && progressView.getVisibility() == 0) {
            progressView.setVisibility(4);
        }
    }

    private void filterMessagesList(ArrayList<MessageObject> messagesList, boolean isGroup) {
        if (messagesList == null || messagesList.isEmpty()) {
            return;
        }
        boolean filtersEnabled = Settings.getFiltersEnabled();
        Set<Long> bannedGroupIds = new HashSet<>();
        if (filtersEnabled) {
            for (MessageObject message : messagesList) {
                if (!message.isOut() && MessageUtils.shouldFilterMessage(message)) {
                    long groupId = message.getGroupId();
                    if (groupId != 0) {
                        bannedGroupIds.add(Long.valueOf(groupId));
                    }
                }
            }
        }
        Iterator<MessageObject> iterator = messagesList.iterator();
        while (iterator.hasNext()) {
            MessageObject message2 = iterator.next();
            try {
                if (!message2.isOut()) {
                    if (isGroup) {
                        long fromId = message2.getFromChatId();
                        if (fromId > 0 && ShadowbanCache.shouldHideInGroups(fromId)) {
                            iterator.remove();
                        }
                    }
                    if (filtersEnabled) {
                        long groupId2 = message2.getGroupId();
                        boolean byGroup = groupId2 != 0 && bannedGroupIds.contains(Long.valueOf(groupId2));
                        if (byGroup || MessageUtils.shouldFilterMessage(message2)) {
                            iterator.remove();
                        }
                    }
                }
            } catch (Exception e) {
                Main.log("Error filtering message: %s", e.getMessage());
            }
        }
    }
}
