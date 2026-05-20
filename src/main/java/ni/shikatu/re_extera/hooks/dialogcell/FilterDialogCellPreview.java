package ni.shikatu.re_extera.hooks.dialogcell;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.Cells.DialogCell;

public class FilterDialogCellPreview extends XC_MethodHook {
    private static final Field MESSAGE_FIELD;

    static {
        Field f = null;
        try {
            f = DialogCell.class.getDeclaredField("message");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("FilterDialogCellPreview: 'message' field not found: %s", e.getMessage());
        }
        MESSAGE_FIELD = f;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (MESSAGE_FIELD == null) {
            return;
        }
        try {
            DialogCell cell = (DialogCell) param.thisObject;
            MessageObject message = (MessageObject) ReflectionUtils.get(MESSAGE_FIELD, cell);
            if (message != null && !message.isOut()) {
                boolean shouldFilter = false;
                long fromId = message.getFromChatId();
                if (fromId > 0 && ShadowbanCache.shouldHideInGroups(fromId)) {
                    shouldFilter = true;
                }
                if (!shouldFilter && Settings.getFiltersEnabled() && MessageUtils.shouldFilterMessage(message)) {
                    shouldFilter = true;
                }
                if (shouldFilter) {
                    String filteredText = Localization.FILTERED_MESSAGE != null ? Localization.FILTERED_MESSAGE : "Filtered";
                    message.messageText = filteredText;
                    if (message.messageOwner != null) {
                        message.messageOwner.message = filteredText;
                    }
                }
            }
        } catch (Exception e) {
            Main.log("FilterDialogCellPreview: %s", e.getMessage());
        }
    }
}
