package ni.shikatu.re_extera.utils;

import android.view.View;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.chatactivity.ProcessDeletedMessages;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class InternalUtils {
    public static Method markMessagesAsDeleted;
    public static Method sendSecretMessageRead;
    public static Method updateDialogs;

    static {
        markMessagesAsDeleted = null;
        updateDialogs = null;
        sendSecretMessageRead = null;
        try {
            markMessagesAsDeleted = MessagesStorage.class.getDeclaredMethod("markMessagesAsDeletedInternal", Long.TYPE, ArrayList.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE);
            updateDialogs = MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessagesInternal", Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class);
            sendSecretMessageRead = ChatActivity.class.getDeclaredMethod("sendSecretMessageRead", MessageObject.class, Boolean.TYPE);
        } catch (NoSuchMethodException e) {
            Main.log("No such method", e.getMessage());
        }
    }

    public static void deleteMessages(long did, List<Integer> messagesIds, boolean shouldNotify) {
        deleteMessages(did, messagesIds, null, shouldNotify);
    }

    public static void deleteMessages(long did, List<Integer> messagesIds) {
        deleteMessages(did, messagesIds, null, true);
    }

    public static void deleteMessages(final long did, final List<Integer> messagesIds, Long channelId, boolean shouldNotify) {
        Main.log("Deletings messages in %s, as %s", Long.valueOf(did), messagesIds.toString());
        final long rChannelId = channelId != null ? channelId.longValue() : 0L;
        if (markMessagesAsDeleted != null) {
            final MessagesStorage storage = MessagesStorage.getInstance(UserConfig.selectedAccount);
            final ArrayList<Integer> messagesIdsParsed = new ArrayList<>(messagesIds);
            Object[] args = {Long.valueOf(did), messagesIdsParsed, false, 0, 0};
            ReflectionUtils.invokeOriginalMethod(markMessagesAsDeleted, storage, args);
            if (shouldNotify) {
                ProcessDeletedMessages.onRequestToDelete.addAll(messagesIdsParsed);
                AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.utils.InternalUtils$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        InternalUtils.lambda$deleteMessages$1(messagesIdsParsed, did, storage, rChannelId, messagesIds);
                    }
                });
                ReExteraDb.get().clearMessages(did, messagesIdsParsed);
                return;
            }
            Main.log("do not notifying", new Object[0]);
        }
    }

    static /* synthetic */ void lambda$deleteMessages$1(ArrayList messagesIdsParsed, long did, MessagesStorage storage, long rChannelId, List messagesIds) {
        ChatActivity lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment instanceof ChatActivity) {
            final ChatActivity chatActivity = lastFragment;
            Iterator it = messagesIdsParsed.iterator();
            while (it.hasNext()) {
                final int mid = ((Integer) it.next()).intValue();
                Main.log("Deleting in %s mid: %s", Long.valueOf(did), Integer.valueOf(mid));
                Optional optionalFindFirst = chatActivity.messages.stream().filter(new Predicate() { // from class: ni.shikatu.re_extera.utils.InternalUtils$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return InternalUtils.lambda$deleteMessages$0(mid, (MessageObject) obj);
                    }
                }).findFirst();
                chatActivity.getClass();
                optionalFindFirst.ifPresent(new Consumer() { // from class: ni.shikatu.re_extera.utils.InternalUtils$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        chatActivity.removeMessageWithThanos((MessageObject) obj);
                    }
                });
            }
        }
        ReflectionUtils.invokeOriginalMethod(updateDialogs, storage, new Object[]{Long.valueOf(did), Long.valueOf(rChannelId), messagesIdsParsed, null});
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.messagesDeleted, new Object[]{messagesIds, Long.valueOf(rChannelId), false, false, false, 0});
    }

    static /* synthetic */ boolean lambda$deleteMessages$0(int mid, MessageObject objn) {
        return objn.getId() == mid;
    }

    public static void deleteAllMessages(long did) {
        deleteMessages(did, ReExteraDb.get().allMessageIdsByDid(did), true);
    }

    public static void sendSecretMessageRead(MessageObject mo) {
        if (sendSecretMessageRead != null) {
            ChatActivity lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment instanceof ChatActivity) {
                ChatActivity chatActivity = lastFragment;
                try {
                    Runnable result = (Runnable) XposedBridge.invokeOriginalMethod(sendSecretMessageRead, chatActivity, new Object[]{mo, true});
                    Main.requestSendWithUnhook(result);
                } catch (Exception e) {
                    Main.log("sendSecretMessageRead failed", e.getMessage());
                }
            }
        }
    }

    public static void createShortVibration() {
        try {
            if (LaunchActivity.instance != null) {
                View view = LaunchActivity.instance.getWindow().getDecorView();
                view.performHapticFeedback(3, 1);
            }
        } catch (Exception e) {
        }
    }
}
