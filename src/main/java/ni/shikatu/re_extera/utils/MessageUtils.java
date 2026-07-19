package ni.shikatu.re_extera.utils;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.LaunchActivity;

public final class MessageUtils {
    private static volatile List<Pattern> compiledPatterns = Collections.emptyList();

    private MessageUtils() {
    }

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

    public static MessageObject getMessage(int currentAccount, long did, int mid) {
        MessageObject obj;
        MessagesController controller = MessagesController.getInstance(currentAccount);
        if (did == 0 && (obj = (MessageObject) controller.dialogMessagesByIds.get(mid)) != null) {
            return obj;
        }
        ArrayList<MessageObject> list = (ArrayList) controller.dialogMessage.get(did);
        if (list != null) {
            for (MessageObject obj2 : list) {
                if (obj2 != null && obj2.getId() == mid) {
                    return obj2;
                }
            }
        }
        TLRPC.Message stored = MessagesStorage.getInstance(currentAccount).getMessage(did, mid);
        if (stored != null) {
            return new MessageObject(currentAccount, stored, false, false);
        }
        ChatActivity lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment instanceof ChatActivity) {
            ChatActivity chatActivity = lastFragment;
            if (chatActivity.getCurrentAccount() != currentAccount) {
                return null;
            }
            if (chatActivity.getDialogId() == did || (did == 0 && chatActivity.getCurrentUser() != null)) {
                for (MessageObject msg : chatActivity.messages) {
                    if (msg != null && msg.getId() == mid) {
                        return msg;
                    }
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public static void forceUpdateViews(int currentAccount, long did, Collection<Integer> mids) {
        final RecyclerListView chatListView;
        final RecyclerView.Adapter<?> adapter;
        if (mids.isEmpty()) {
            return;
        }
        ChatActivity lastFragment = LaunchActivity.getLastFragment();
        if (!(lastFragment instanceof ChatActivity)) {
            return;
        }
        ChatActivity activity = lastFragment;
        if (activity.getCurrentAccount() == currentAccount && activity.getDialogId() == did && (adapter = (chatListView = activity.getChatListView()).getAdapter()) != null) {
            HashMap<Integer, ChatMessageCell> visibleCells = getVisibleCells(chatListView);
            Iterator<Integer> it = mids.iterator();
            while (it.hasNext()) {
                int id = it.next().intValue();
                final ChatMessageCell cell = visibleCells.get(Integer.valueOf(id));
                if (cell != null && cell.getMessageObject() != null) {
                    AndroidUtilities.runOnUIThread(new Runnable() { 
                        @Override // java.lang.Runnable
                        public final void run() {
                            MessageUtils.lambda$forceUpdateViews$0(cell, adapter, chatListView);
                        }
                    });
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
        HashMap<Integer, ChatMessageCell> visible = new HashMap<>();
        int count = chatListView.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = chatListView.getChildAt(i);
            if ((view instanceof ChatMessageCell) && (messageObject = (cell = (ChatMessageCell) view).getMessageObject()) != null) {
                visible.put(Integer.valueOf(messageObject.getId()), cell);
            }
        }
        return visible;
    }

    public static void updatePatterns() {
        List<String> regexFilters = ReExteraDb.get().getAllRegexFilters();
        ArrayList<Pattern> compiled = new ArrayList<>(regexFilters.size());
        for (String regex : regexFilters) {
            try {
                compiled.add(Pattern.compile(regex, 40));
            } catch (PatternSyntaxException e) {
                Main.log("Invalid regex pattern: %s - %s", regex, e.getMessage());
            }
        }
        compiledPatterns = Collections.unmodifiableList(compiled);
    }

    public static boolean shouldFilterMessage(MessageObject message) {
        if (message == null) {
            return false;
        }
        List<Pattern> patterns = compiledPatterns;
        if (patterns.isEmpty()) {
            return false;
        }
        String text = null;
        if (message.messageOwner != null && message.messageOwner.message != null && !message.messageOwner.message.isEmpty()) {
            text = message.messageOwner.message;
        } else if (message.messageText != null) {
            String asString = message.messageText.toString();
            if (!asString.isEmpty()) {
                text = asString;
            }
        }
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (Pattern pattern : patterns) {
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

    public static int getRealPosition(ArrayList<MessageObject> messages, int visiblePosition) {
        int currentVisible = 0;
        for (int i = 0; i < messages.size(); i++) {
            if (!shouldFilterMessage(messages.get(i))) {
                if (currentVisible == visiblePosition) {
                    return i;
                }
                currentVisible++;
            }
        }
        int i2 = messages.size();
        return i2 - 1;
    }

    public static double getScheduleTime(int currentAccount, TLRPC.TL_photo photo, TLRPC.TL_document document) {
        double time = ConnectionsManager.getInstance(currentAccount).getCurrentTime() + 12;
        if (document != null && document.access_hash != 0 && (MessageObject.isStickerDocument(document) || MessageObject.isAnimatedStickerDocument(document, true) || MessageObject.isGifDocument(document))) {
            return Math.ceil(time);
        }
        int photoFileSize = 0;
        long documentFileSize = 0;
        if (photo != null) {
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize());
            photoFileSize = photoSize != null ? photoSize.size : 0;
        }
        if (document != null) {
            documentFileSize = document.size;
        }
        double dMax = documentFileSize != 0 ? Math.max(6, (int) Math.ceil(((documentFileSize / 1024.0f) / 1024.0f) * 4.5f)) : 0.0d;
        Double.isNaN(time);
        return time + dMax + (photoFileSize != 0 ? Math.max(6, (int) Math.ceil(((photoFileSize / 1024.0f) / 1024.0f) * 4.5f)) : 0.0d);
    }
}
