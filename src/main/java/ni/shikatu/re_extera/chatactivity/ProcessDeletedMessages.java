package ni.shikatu.re_extera.chatactivity;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import ni.shikatu.re_extera.Global;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.RecyclerListView;

public class ProcessDeletedMessages extends XC_MethodHook {
    public static Method measureTimeMethod;
    public static ArrayList<Integer> onRequestToDelete = new ArrayList<>();

    static {
        try {
            measureTimeMethod = ChatMessageCell.class.getDeclaredMethod("measureTime", MessageObject.class);
            measureTimeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Global.log("requesting deleting messages");
        ChatActivity thisObject = (ChatActivity) param.thisObject;
        ArrayList<Integer> ids = (ArrayList) param.args[0];
        ArrayList<Integer> realDelete = new ArrayList<>();
        ArrayList<Integer> deleted = new ArrayList<>();
        thisObject.getDialogId();
        for (Integer id : ids) {
            if (onRequestToDelete.contains(id)) {
                realDelete.add(id);
            } else {
                deleted.add(id);
            }
        }
        onRequestToDelete.clear();
        RecyclerListView chatListView = thisObject.getChatListView();
        RecyclerView.Adapter adapter = chatListView.getAdapter();
        HashMap<Integer, ChatMessageCell> visibleCells = getVisibleCells(chatListView);
        Global.log(visibleCells.toString());
        Iterator<Integer> it = deleted.iterator();
        while (it.hasNext()) {
            ChatMessageCell cell = visibleCells.get(it.next());
            MessageObject cellObject = cell.getMessageObject();
            if (cellObject != null && measureTimeMethod != null && adapter != null) {
                cell.forceResetMessageObject();
                adapter.notifyItemChanged(chatListView.getChildAdapterPosition(cell));
                cell.requestLayout();
                cell.invalidate();
            }
        }
        param.args[0] = realDelete;
    }

    private HashMap<Integer, ChatMessageCell> getVisibleCells(RecyclerListView chatListView) {
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
}
