package ni.shikatu.re_extera.hooks.chatmessagecell;

import android.view.View;
import java.lang.reflect.Method;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.Cells.ChatMessageCell;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;

public class HideFilteredCell extends XC_MethodHook {
    private static Method setMeasuredDimensionMethod;

    static {
        try {
            setMeasuredDimensionMethod = View.class.getDeclaredMethod("setMeasuredDimension", Integer.TYPE, Integer.TYPE);
            setMeasuredDimensionMethod.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getFiltersEnabled()) {
            return;
        }

        ChatMessageCell cell = (ChatMessageCell) param.thisObject;
        MessageObject messageObject = cell.getMessageObject();
        if (messageObject == null) {
            return;
        }

        if (MessageUtils.shouldFilterMessage(messageObject)) {
            cell.setVisibility(View.GONE);
            if (setMeasuredDimensionMethod != null) {
                try {
                    setMeasuredDimensionMethod.invoke(cell, 0, 0);
                } catch (Throwable ignored) {
                }
            }
            param.setResult((Object) null);
        }
    }
}
