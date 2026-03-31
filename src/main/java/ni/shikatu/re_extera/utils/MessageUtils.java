package ni.shikatu.re_extera.utils;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.LaunchActivity;

public class MessageUtils {
    private static List<Pattern> compiledPatterns = null;

    public static long getDialogIdFromMessage(TLRPC.Message msg) {
        if (msg.peer_id instanceof TLRPC.TL_peerUser) {
            return msg.peer_id.user_id;
        }
        if (msg.peer_id instanceof TLRPC.TL_peerChat) {
            return -msg.peer_id.chat_id;
        }
        if (msg.peer_id instanceof TLRPC.TL_peerChannel) {
            return -msg.peer_id.channel_id;
        }
        return 0L;
    }

    public static MessageObject getMessage(long did, int mid) {
        return getMessage(UserConfig.selectedAccount, did, mid);
    }

    public static MessageObject getMessage(int currentAccount, long did, int mid) {
        TLRPC.Message msg;
        ArrayList<MessageObject> list;
        MessagesController controller = MessagesController.getInstance(currentAccount);
        MessageObject obj = null;
        if (did == 0) {
            obj = (MessageObject) controller.dialogMessagesByIds.get(mid);
        }
        if (obj == null && (list = (ArrayList) controller.dialogMessage.get(did)) != null && !list.isEmpty()) {
            for (MessageObject obj1 : list) {
                if (obj1.getId() == mid) {
                    obj = obj1;
                }
            }
        }
        if (obj == null && (msg = MessagesStorage.getInstance(currentAccount).getMessage(did, mid)) != null) {
            obj = new MessageObject(currentAccount, msg, false, false);
        }
        if (obj == null) {
            ChatActivity lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment instanceof ChatActivity) {
                ChatActivity chatActivity = lastFragment;
                if (chatActivity.getCurrentAccount() != currentAccount) {
                    return obj;
                }
                if (chatActivity.getDialogId() == did || (did == 0 && chatActivity.getCurrentUser() != null)) {
                    for (MessageObject msg2 : chatActivity.messages) {
                        if (msg2 != null && msg2.getId() == mid) {
                            return msg2;
                        }
                    }
                    return obj;
                }
                return obj;
            }
            return obj;
        }
        return obj;
    }

    public static void forceUpdateViews(long did, Collection<Integer> mids) {
        forceUpdateViews(UserConfig.selectedAccount, did, mids);
    }

    public static void forceUpdateViews(int currentAccount, long did, Collection<Integer> mids) {
        if (LaunchActivity.getLastFragment() instanceof ChatActivity) {
            ChatActivity activity = LaunchActivity.getLastFragment();
            if (activity.getCurrentAccount() != currentAccount) {
                return;
            }
            final RecyclerListView chatListView = activity.getChatListView();
            final RecyclerView.Adapter adapter = chatListView.getAdapter();
            HashMap<Integer, ChatMessageCell> visibleCells = getVisibleCells(chatListView);
            for (Integer id : mids) {
                Main.log("Deleting message", new Object[0]);
                final ChatMessageCell cell = visibleCells.get(id);
                if (cell != null) {
                    MessageObject cellObject = cell.getMessageObject();
                    if (cellObject != null && adapter != null) {
                        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageUtils$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                MessageUtils.lambda$forceUpdateViews$0(cell, adapter, chatListView);
                            }
                        });
                    }
                }
            }
        }
    }

    static /* synthetic */ void lambda$forceUpdateViews$0(ChatMessageCell cell, RecyclerView.Adapter adapter, RecyclerListView chatListView) {
        cell.forceResetMessageObject();
        cell.setAlpha(1.0f);
        adapter.notifyItemChanged(chatListView.getChildAdapterPosition(cell));
        cell.requestLayout();
        cell.invalidate();
    }

    public static HashMap<Integer, ChatMessageCell> getVisibleCells(RecyclerListView chatListView) {
        ChatMessageCell cell;
        MessageObject messageObject;
        HashMap<Integer, ChatMessageCell> visibleCells = new HashMap<>();
        int count = chatListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = chatListView.getChildAt(a);
            if ((view instanceof ChatMessageCell) && (messageObject = (cell = (ChatMessageCell) view).getMessageObject()) != null) {
                visibleCells.put(Integer.valueOf(messageObject.getId()), cell);
            }
        }
        return visibleCells;
    }

    public static void updatePatterns() {
        List<String> regexFilters = ReExteraDb.get().getAllRegexFilters();
        compiledPatterns = new ArrayList();
        for (String regex : regexFilters) {
            try {
                Pattern pattern = Pattern.compile(regex, 40);
                compiledPatterns.add(pattern);
            } catch (PatternSyntaxException e) {
                Main.log("Invalid regex pattern: %s - %s", regex, e.getMessage());
            }
        }
    }

    public static boolean shouldFilterMessage(MessageObject message) {
        if (message == null || compiledPatterns == null || compiledPatterns.isEmpty()) {
            return false;
        }
        String text = "";
        if (message.messageOwner != null && message.messageOwner.message != null && !message.messageOwner.message.isEmpty()) {
            text = message.messageOwner.message;
        } else if (message.messageText != null && !message.messageText.toString().isEmpty()) {
            text = message.messageText.toString();
        }
        if (text.isEmpty()) {
            return false;
        }
        for (Pattern pattern : compiledPatterns) {
            try {
                if (pattern.matcher(text).find()) {
                    Main.log("Message filtered by regex", new Object[0]);
                    return true;
                }
                continue;
            } catch (Exception e) {
                Main.log("Regex matching error: %s", e.getMessage());
            }
        }
        return false;
    }

    public static double getScheduleTime(TLRPC.TL_photo photo, TLRPC.TL_document document) {
        return getScheduleTime(UserConfig.selectedAccount, photo, document);
    }

    public static double getScheduleTime(int currentAccount, TLRPC.TL_photo photo, TLRPC.TL_document document) {
        double time = ConnectionsManager.getInstance(currentAccount).getCurrentTime() + 12;
        if (document != null && document.access_hash != 0 && (MessageObject.isStickerDocument(document) || MessageObject.isAnimatedStickerDocument(document, true) || MessageObject.isGifDocument(document))) {
            return Math.ceil(time);
        }
        int photoFileSize = 0;
        long documentFileSize = 0;
        if (photo != null) {
            photoFileSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize()).size;
        }
        if (document != null) {
            documentFileSize = document.size;
        }
        double dMax = documentFileSize != 0 ? Math.max(6, (int) Math.ceil(((documentFileSize / 1024.0f) / 1024.0f) * 4.5f)) : 0.0d;
        Double.isNaN(time);
        return time + dMax + (photoFileSize != 0 ? Math.max(6, (int) Math.ceil(((photoFileSize / 1024.0f) / 1024.0f) * 4.5f)) : 0.0d);
    }

    public static int getRealPosition(ArrayList<MessageObject> messages, int visiblePosition) {
        int currentVisible = 0;
        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (!shouldFilterMessage(msg)) {
                if (currentVisible == visiblePosition) {
                    return i;
                }
                currentVisible++;
            }
        }
        int i2 = messages.size();
        return i2 - 1;
    }
}
