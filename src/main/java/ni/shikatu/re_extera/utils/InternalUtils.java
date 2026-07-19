package ni.shikatu.re_extera.utils;

import android.view.View;
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
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public final class InternalUtils {
    private static final Method markMessagesAsDeleted;
    public static final Method sendSecretMessageRead;
    private static final Method updateDialogs;

    private InternalUtils() {
    }

    static {
        Method mark = null;
        Method update = null;
        Method secret = null;
        try {
            mark = MessagesStorage.class.getDeclaredMethod("markMessagesAsDeletedInternal", Long.TYPE, ArrayList.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE);
            update = MessagesStorage.class.getDeclaredMethod("updateDialogsWithDeletedMessagesInternal", Long.TYPE, Long.TYPE, ArrayList.class, ArrayList.class);
            secret = ChatActivity.class.getDeclaredMethod("sendSecretMessageRead", MessageObject.class, Boolean.TYPE);
        } catch (NoSuchMethodException e) {
            Main.log("InternalUtils: method not found: %s", e.getMessage());
        }
        markMessagesAsDeleted = mark;
        updateDialogs = update;
        sendSecretMessageRead = secret;
    }

    public static void deleteMessages(int currentAccount, long did, List<Integer> messagesIds, boolean shouldNotify) {
        deleteMessages(currentAccount, did, messagesIds, null, shouldNotify);
    }

    public static void deleteMessages(final int currentAccount, final long did, final List<Integer> messagesIds, Long channelId, boolean shouldNotify) {
        if (!messagesIds.isEmpty() && markMessagesAsDeleted != null) {
            Main.log("Deleting messages in %d: %s", Long.valueOf(did), messagesIds);
            final long rChannelId = channelId != null ? channelId.longValue() : 0L;
            final MessagesStorage storage = MessagesStorage.getInstance(currentAccount);
            final ArrayList<Integer> copy = new ArrayList<>(messagesIds);
            ReflectionUtils.invokeOriginalMethod(markMessagesAsDeleted, storage, new Object[]{Long.valueOf(did), copy, false, 0, 0});
            if (shouldNotify) {
                ProcessDeletedMessages.onRequestToDelete.addAll(copy);
                AndroidUtilities.runOnUIThread(new Runnable() { 
                    @Override // java.lang.Runnable
                    public final void run() {
                        InternalUtils.lambda$deleteMessages$1(currentAccount, did, copy, storage, rChannelId, messagesIds);
                    }
                });
            }
            ReExteraDb.get().clearMessages(did, copy);
        }
    }

    static /* synthetic */ void lambda$deleteMessages$1(int currentAccount, long did, ArrayList copy, MessagesStorage storage, long rChannelId, List messagesIds) {
        ChatActivity lastFragment = (ChatActivity) LaunchActivity.getLastFragment();
        if (lastFragment instanceof ChatActivity) {
            final ChatActivity chat = lastFragment;
            if (chat.getCurrentAccount() == currentAccount && chat.getDialogId() == did) {
                Iterator it = copy.iterator();
                while (it.hasNext()) {
                    final int mid = ((Integer) it.next()).intValue();
                    Main.log("Removing in %d mid: %d", Long.valueOf(did), Integer.valueOf(mid));
                    Optional optionalFindFirst = chat.messages.stream().filter(new Predicate() { 
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return InternalUtils.lambda$deleteMessages$0(mid, (MessageObject) obj);
                        }
                    }).findFirst();
                    chat.getClass();
                    optionalFindFirst.ifPresent(new Consumer() { 
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            chat.removeMessageWithThanos((MessageObject) obj);
                        }
                    });
                }
            }
        }
        ReflectionUtils.invokeOriginalMethod(updateDialogs, storage, new Object[]{Long.valueOf(did), Long.valueOf(rChannelId), copy, null});
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messagesDeleted, new Object[]{messagesIds, Long.valueOf(rChannelId), false, false, false, 0});
    }

    static /* synthetic */ boolean lambda$deleteMessages$0(int mid, MessageObject m) {
        return m.getId() == mid;
    }

    public static void deleteAllMessages(int currentAccount, long did) {
        deleteMessages(currentAccount, did, ReExteraDb.get().allMessageIdsByDid(did), true);
    }

    public static void clearSavedMessages(int currentAccount) {
        ReExteraDb db = ReExteraDb.get();
        List<Long> dids = db.getDialogIdWithSavedMessages();
        ArrayList<ArrayList<Integer>> allToDelete = new ArrayList<>(dids.size());
        Iterator<Long> it = dids.iterator();
        while (it.hasNext()) {
            long did = it.next().longValue();
            allToDelete.add(db.allMessageIdsByDid(did));
        }
        for (int i = 0; i < dids.size(); i++) {
            ArrayList<Integer> toDelete = allToDelete.get(i);
            if (!toDelete.isEmpty()) {
                deleteMessages(currentAccount, dids.get(i).longValue(), toDelete, true);
            }
        }
        db.clearDatabaseOnly();
    }

    public static void sendSecretMessageRead(MessageObject mo) {
        if (mo == null || mo.getDialogId() == 0) {
            return;
        }
        int currentAccount = mo.currentAccount;
        if (!DialogObject.isEncryptedDialog(mo.getDialogId()) && mo.isSecretMedia()) {
            TLRPC.TL_messages_readMessageContents req = new TLRPC.TL_messages_readMessageContents();
            req.id.add(Integer.valueOf(mo.getId()));
            Main.addIgnoredRequest(req);
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (RequestDelegate) null);
            mo.setContentIsRead();
            if (mo.messageOwner.ttl > 0 && mo.messageOwner.ttl != Integer.MAX_VALUE) {
                mo.messageOwner.destroyTime = mo.messageOwner.ttl + ConnectionsManager.getInstance(currentAccount).getCurrentTime();
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateMessageMedia, new Object[]{mo.messageOwner});
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

    public static void sendReadMessage(int currentAccount, TLRPC.InputPeer peer, int maxId, final boolean vibrate) {
        org.telegram.tgnet.TLObject r;
        if (peer.channel_id != 0) {
            r = new TLRPC.TL_channels_readHistory();
            ((TLRPC.TL_channels_readHistory) r).channel = MessagesController.getInputChannel(peer);
            ((TLRPC.TL_channels_readHistory) r).max_id = maxId;
        } else {
            r = new TLRPC.TL_messages_readHistory();
            ((TLRPC.TL_messages_readHistory) r).peer = peer;
            ((TLRPC.TL_messages_readHistory) r).max_id = maxId;
        }
        Main.addIgnoredRequest(r);
        ConnectionsManager.getInstance(currentAccount).sendRequest(r, new RequestDelegate() { 
            public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                InternalUtils.lambda$sendReadMessage$2(vibrate, tLObject, tL_error);
            }
        });
    }

    static /* synthetic */ void lambda$sendReadMessage$2(boolean vibrate, TLObject resp, TLRPC.TL_error error) {
        if (vibrate) {
            createShortVibration();
        }
    }

    public static void sendReadMessage(MessageObject messageObject, final boolean vibrate) {
        org.telegram.tgnet.TLObject r;
        if (messageObject == null) {
            return;
        }
        int currentAccount = messageObject.currentAccount;
        MessagesController controller = MessagesController.getInstance(currentAccount);
        messageObject.setIsRead();
        messageObject.setContentIsRead();
        if (messageObject.isFromChannel()) {
            r = new TLRPC.TL_channels_readHistory();
            ((TLRPC.TL_channels_readHistory) r).channel = MessagesController.getInputChannel(controller.getInputPeer(messageObject.getDialogId()));
            ((TLRPC.TL_channels_readHistory) r).max_id = messageObject.getId();
        } else {
            r = new TLRPC.TL_messages_readHistory();
            ((TLRPC.TL_messages_readHistory) r).peer = controller.getInputPeer(messageObject.getDialogId());
            ((TLRPC.TL_messages_readHistory) r).max_id = messageObject.getId();
        }
        Main.addIgnoredRequest(r);
        ConnectionsManager.getInstance(currentAccount).sendRequest(r, new RequestDelegate() { 
            public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                InternalUtils.lambda$sendReadMessage$3(vibrate, tLObject, tL_error);
            }
        });
        if (messageObject.isSecret() || messageObject.isSecretMedia() || messageObject.isRoundOnce() || messageObject.isVoiceOnce()) {
            sendSecretMessageRead(messageObject);
            if (vibrate) {
                createShortVibration();
            }
        }
    }

    static /* synthetic */ void lambda$sendReadMessage$3(boolean vibrate, TLObject resp, TLRPC.TL_error error) {
        if (vibrate) {
            createShortVibration();
        }
    }
}
