package ni.shikatu.re_extera.hooks.chatactivity.menuhook;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.ui.MessageHistoryFragment;
import ni.shikatu.re_extera.utils.InternalUtils;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import android.widget.FrameLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.LayoutHelper;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ProcessSelectedOption extends XC_MethodHook {
    public static final int OPT_DELETE = 24;
    public static final int OPT_MESSAGE_HISTORY = 6363;
    public static final int OPT_READ_MESSAGE = 6565;
    public static final int OPT_READ_AT = 6767;
    public static final int OPT_CHECK_FILTERS = 6969;
    public static final int OPT_ADD_TO_FILTERS = 7070;
    private static final Field SELECTED_OBJECT_FIELD;
    public static MessageObject selectedObject;

    static {
        Field f = null;
        try {
            f = ChatActivity.class.getDeclaredField("selectedObject");
            f.setAccessible(true);
        } catch (Exception e) {
            Main.log("ProcessSelectedOption: %s", e.getMessage());
        }
        SELECTED_OBJECT_FIELD = f;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int option = ((Integer) param.args[0]).intValue();
        ChatActivity thisObj = (ChatActivity) param.thisObject;
        if (thisObj == null || SELECTED_OBJECT_FIELD == null) {
            return;
        }
        MessageObject messageObject = (MessageObject) ReflectionUtils.get(SELECTED_OBJECT_FIELD, thisObj);
        if (option == 6363) {
            MessageObject target = messageObject != null ? messageObject : selectedObject;
            if (target != null) {
                thisObj.presentFragment(MessageHistoryFragment.newInstance(target.getDialogId(), target.getId()));
                return;
            }
            return;
        }
        if (option == 6565) {
            InternalUtils.sendReadMessage(messageObject, true);
            return;
        }
        if (option == OPT_CHECK_FILTERS) {
            MessageObject target = messageObject != null ? messageObject : selectedObject;
            if (target != null) {
                String text = target.messageOwner != null && target.messageOwner.message != null ? target.messageOwner.message : (target.messageText != null ? target.messageText.toString() : "");
                ni.shikatu.re_extera.ui.RegexFiltersFragment.showFilterTesterDialog(thisObj.getParentActivity(), text);
                return;
            }
            return;
        }
        if (option == OPT_ADD_TO_FILTERS) {
            MessageObject target = messageObject != null ? messageObject : selectedObject;
            if (target != null) {
                String text = target.messageOwner != null && target.messageOwner.message != null ? target.messageOwner.message : (target.messageText != null ? target.messageText.toString() : "");
                showAddToFiltersDialog(thisObj, text);
                return;
            }
            return;
        }
        if (option == 24 && messageObject != null && ReExteraDb.get().messageIsDeleted(messageObject)) {
            int currentAccount = messageObject.currentAccount;
            SendMessagesHelper.getInstance(currentAccount).cancelSendingMessage(messageObject);
            InternalUtils.deleteMessages(currentAccount, messageObject.getDialogId(), new ArrayList(Collections.singletonList(Integer.valueOf(messageObject.getId()))), Long.valueOf(messageObject.getChannelId()), true);
        }
    }

    private static void showAddToFiltersDialog(ChatActivity activity, String messageText) {
        android.content.Context context = activity.getParentActivity();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.ADD_TO_FILTERS);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);

        if (messageText != null && !messageText.trim().isEmpty()) {
            android.widget.TextView previewLabel = new android.widget.TextView(context);
            previewLabel.setText(messageText.length() > 150 ? messageText.substring(0, 147) + "..." : messageText);
            previewLabel.setTextSize(13.0f);
            previewLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            previewLabel.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), AndroidUtilities.dp(8.0f));
            layout.addView(previewLabel, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 0));
        }

        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setBackground(Theme.createEditTextDrawable(context, false));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setTextSize(16.0f);
        editText.setHint(Localization.ADD_TO_FILTERS_HINT);
        editText.setInputType(1);
        editText.setPadding(AndroidUtilities.dp(16.0f), AndroidUtilities.dp(8.0f), AndroidUtilities.dp(16.0f), AndroidUtilities.dp(8.0f));

        if (messageText != null && !messageText.trim().isEmpty()) {
            String initial = Pattern.quote(messageText.trim());
            editText.setText(initial);
            editText.setSelection(initial.length());
        }

        layout.addView(editText, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 0));

        FrameLayout container = new FrameLayout(context);
        container.addView(layout, LayoutHelper.createFrame(-1, -2.0f, 0, 24.0f, 12.0f, 24.0f, 0.0f));
        builder.setView(container);
        builder.setPositiveButton(Localization.ADD, (dialog, which) -> {
            String regex = editText.getText().toString().trim();
            if (regex.isEmpty()) {
                AndroidUtilities.shakeView(editText);
                return;
            }
            try {
                Pattern.compile(regex);
                ReExteraDb.get().addRegexFilter(regex);
                MessageUtils.updatePatterns();
            } catch (PatternSyntaxException e) {
                AlertDialog.Builder errorBuilder = new AlertDialog.Builder(context);
                errorBuilder.setTitle(Localization.PATTERN_ERROR);
                errorBuilder.setMessage(e.getMessage());
                errorBuilder.setPositiveButton(Localization.YES, (AlertDialog.OnButtonClickListener) null);
                errorBuilder.show();
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });
        dialog.show();
    }
}
