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
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class NotificationCenterDidLoad extends XC_MethodHook {
    private static Field forwardEndReachedField;

    static {
        try {
            forwardEndReachedField = ChatActivity.class.getDeclaredField("forwardEndReached");
            forwardEndReachedField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("Failed to get forwardEndReached field", e.getMessage());
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getFiltersEnabled()) {
            int id = ((Integer) param.args[0]).intValue();
            Object[] args = (Object[]) param.args[2];
            if (id == NotificationCenter.messagesDidLoad && args != null && args.length > 2) {
                ArrayList<MessageObject> messArr = (ArrayList) args[2];
                int originalSize = messArr.size();
                int originalCount = ((Integer) args[1]).intValue();
                boolean isCache = ((Boolean) args[3]).booleanValue();
                Main.log("BEFORE: count=%d, messArr.size=%d, isCache=%b", Integer.valueOf(originalCount), Integer.valueOf(originalSize), Boolean.valueOf(isCache));
                filterMessagesList(messArr);
                int filteredSize = messArr.size();
                if (filteredSize != originalSize) {
                    args[1] = Integer.valueOf(filteredSize);
                    Main.log("AFTER: count=%d, messArr.size=%d (removed %d)", Integer.valueOf(filteredSize), Integer.valueOf(filteredSize), Integer.valueOf(originalSize - filteredSize));
                }
            }
        }
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        if (Settings.getFiltersEnabled()) {
            int id = ((Integer) param.args[0]).intValue();
            Object[] args = (Object[]) param.args[2];
            if (id == NotificationCenter.messagesDidLoad || id == NotificationCenter.messagesDidLoadWithoutProcess) {
                ChatActivity lastFragment = LaunchActivity.getLastFragment();
                if (lastFragment instanceof ChatActivity) {
                    final ChatActivity chatActivity = lastFragment;
                    if (args != null) {
                        try {
                            if (args.length > 9) {
                                int loadtype = ((Integer) args[8]).intValue();
                                if (loadtype == 1) {
                                    boolean[] forwardEndReached = (boolean[]) forwardEndReachedField.get(chatActivity);
                                    if (forwardEndReached != null && ((Boolean) args[9]).booleanValue()) {
                                        forwardEndReached[0] = true;
                                    }
                                    Field loadingField = ChatActivity.class.getDeclaredField("loading");
                                    loadingField.setAccessible(true);
                                    loadingField.setBoolean(chatActivity, false);
                                    Field loadingForwardField = ChatActivity.class.getDeclaredField("loadingForward");
                                    loadingForwardField.setAccessible(true);
                                    loadingForwardField.setBoolean(chatActivity, false);
                                    Main.log("Set forwardEndReached=true, loading=false, loadingForward=false", new Object[0]);
                                    AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.hooks.chatactivity.NotificationCenterDidLoad$$ExternalSyntheticLambda0
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            NotificationCenterDidLoad.lambda$afterHookedMethod$0(chatActivity);
                                        }
                                    }, 50L);
                                }
                            }
                        } catch (Exception e) {
                            Main.log("Error in afterHookedMethod", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    static /* synthetic */ void lambda$afterHookedMethod$0(ChatActivity chatActivity) {
        try {
            Field progressViewField = ChatActivity.class.getDeclaredField("progressView");
            progressViewField.setAccessible(true);
            View progressView = (View) progressViewField.get(chatActivity);
            if (progressView != null && progressView.getVisibility() == 0) {
                progressView.setVisibility(4);
                Main.log("Hidden progressView", new Object[0]);
            }
        } catch (Exception e) {
            Main.log("Failed to hide progressView", e.getMessage());
        }
    }

    private void filterMessagesList(ArrayList<MessageObject> messagesList) {
        if (messagesList == null || messagesList.isEmpty()) {
            return;
        }
        Set<Long> bannedGroupIds = new HashSet<>();
        for (MessageObject messageObject : messagesList) {
            if (!messageObject.isOut() && MessageUtils.shouldFilterMessage(messageObject)) {
                long groupId = messageObject.getGroupId();
                if (groupId != 0) {
                    bannedGroupIds.add(Long.valueOf(groupId));
                }
            }
        }
        Iterator<MessageObject> iterator = messagesList.iterator();
        while (iterator.hasNext()) {
            MessageObject messageObject2 = iterator.next();
            try {
                if (!messageObject2.isOut()) {
                    boolean byRegex = MessageUtils.shouldFilterMessage(messageObject2);
                    long groupId2 = messageObject2.getGroupId();
                    boolean byGroup = groupId2 != 0 && bannedGroupIds.contains(Long.valueOf(groupId2));
                    if (byRegex || byGroup) {
                        if (groupId2 != 0) {
                            Main.log("Filtered message (load): id=%s, groupId=%s", Integer.valueOf(messageObject2.getId()), Long.valueOf(groupId2));
                        } else {
                            Main.log("Filtered message (load): id=%s, no group", Integer.valueOf(messageObject2.getId()));
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                Main.log("Error", e.getMessage());
            }
        }
    }
}
