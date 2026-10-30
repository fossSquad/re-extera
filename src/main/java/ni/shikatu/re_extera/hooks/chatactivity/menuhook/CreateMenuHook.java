package ni.shikatu.re_extera.hooks.chatactivity.menuhook;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;

public class CreateMenuHook extends XC_MethodHook {
    private static final Field SELECTED_OBJECT_FIELD;
    private static final Field SELECTED_OBJECT_GROUP_FIELD;
    private final ThreadLocal<List<TLRPC.Message>> modifiedLocal = new ThreadLocal<>();

    static {
        Field selObj = null;
        Field selGroup = null;
        try {
            selObj = ChatActivity.class.getDeclaredField("selectedObject");
            selObj.setAccessible(true);
        } catch (Throwable ignore) {}
        try {
            selGroup = ChatActivity.class.getDeclaredField("selectedObjectGroup");
            selGroup.setAccessible(true);
        } catch (Throwable ignore) {}
        SELECTED_OBJECT_FIELD = selObj;
        SELECTED_OBJECT_GROUP_FIELD = selGroup;
    }

    @Override
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.noForward()) {
            return;
        }
        View v = param.args != null && param.args.length > 0 && param.args[0] instanceof View ? (View) param.args[0] : null;
        List<TLRPC.Message> modified = new ArrayList<>();
        if (v instanceof ChatMessageCell) {
            ChatMessageCell cell = (ChatMessageCell) v;
            checkAndClear(cell.getMessageObject(), modified);
            checkAndClear(cell.getPrimaryMessageObject(), modified);
        } else if (v instanceof ChatActionCell) {
            ChatActionCell cell = (ChatActionCell) v;
            checkAndClear(cell.getMessageObject(), modified);
        }
        if (param.thisObject instanceof ChatActivity) {
            ChatActivity activity = (ChatActivity) param.thisObject;
            if (SELECTED_OBJECT_FIELD != null) {
                MessageObject sel = (MessageObject) ReflectionUtils.get(SELECTED_OBJECT_FIELD, activity);
                checkAndClear(sel, modified);
            }
            if (SELECTED_OBJECT_GROUP_FIELD != null) {
                MessageObject.GroupedMessages group = (MessageObject.GroupedMessages) ReflectionUtils.get(SELECTED_OBJECT_GROUP_FIELD, activity);
                if (group != null && group.messages != null) {
                    for (MessageObject m : group.messages) {
                        checkAndClear(m, modified);
                    }
                }
            }
        }
        if (!modified.isEmpty()) {
            modifiedLocal.set(modified);
        }
    }

    private void checkAndClear(MessageObject obj, List<TLRPC.Message> modified) {
        if (obj != null && obj.messageOwner != null && obj.messageOwner.noforwards) {
            obj.messageOwner.noforwards = false;
            modified.add(obj.messageOwner);
        }
    }

    @Override
    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        List<TLRPC.Message> modified = modifiedLocal.get();
        modifiedLocal.remove();
        if (modified != null) {
            for (TLRPC.Message msg : modified) {
                msg.noforwards = true;
            }
        }
    }
}
