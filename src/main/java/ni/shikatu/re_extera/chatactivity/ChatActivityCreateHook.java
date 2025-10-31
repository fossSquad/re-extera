package ni.shikatu.re_extera.chatactivity;

import android.view.View;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.InterceptOnlineHook;
import ni.shikatu.re_extera.Localization;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class ChatActivityCreateHook extends XC_MethodHook {
    private static final int MENU_EXCEPTION_READING = 6364;
    private static final int MENU_EXCEPTION_TYPING = 6365;
    private static long SELECTED_DIALOG_ID = 0;
    private static Field headerItemField;
    private RadioColorCell reading_cell_always;
    private RadioColorCell reading_cell_global;
    private RadioColorCell reading_cell_never;
    private LinearLayout reading_layout;
    private RadioColorCell typing_cell_always;
    private RadioColorCell typing_cell_global;
    private RadioColorCell typing_cell_never;
    private LinearLayout typing_layout;

    static {
        try {
            headerItemField = ChatActivity.class.getDeclaredField("headerItem");
            headerItemField.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
    }

    public static long getSelectedDialogId() {
        return SELECTED_DIALOG_ID;
    }

    public ChatActivityCreateHook() {
        LaunchActivity launchActivity = LaunchActivity.instance;
        this.reading_layout = new LinearLayout(launchActivity);
        this.reading_layout.setOrientation(1);
        this.typing_layout = new LinearLayout(launchActivity);
        this.typing_layout.setOrientation(1);
        this.reading_cell_global = new RadioColorCell(launchActivity);
        this.reading_cell_always = new RadioColorCell(launchActivity);
        this.reading_cell_never = new RadioColorCell(launchActivity);
        this.typing_cell_never = new RadioColorCell(launchActivity);
        this.typing_cell_always = new RadioColorCell(launchActivity);
        this.typing_cell_global = new RadioColorCell(launchActivity);
        this.reading_cell_global.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
        this.reading_cell_global.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
        this.reading_cell_global.setTextAndValue(Localization.BASED_ON_GLOBAL, false);
        this.reading_cell_global.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m5xac428cdf(view);
            }
        });
        this.reading_cell_global.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        this.reading_cell_never.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
        this.reading_cell_never.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
        this.reading_cell_never.setTextAndValue(Localization.NEVER + " " + Localization.READ, false);
        this.reading_cell_never.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m6x392fa3fe(view);
            }
        });
        this.reading_cell_never.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        this.reading_cell_always.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
        this.reading_cell_always.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
        this.reading_cell_always.setTextAndValue(Localization.ALWAYS + " " + Localization.READ, false);
        this.reading_cell_always.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m7xc61cbb1d(view);
            }
        });
        this.reading_cell_always.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        this.reading_layout.addView(this.reading_cell_always);
        this.reading_layout.addView(this.reading_cell_never);
        this.reading_layout.addView(this.reading_cell_global);
        this.typing_cell_global.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
        this.typing_cell_global.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
        this.typing_cell_global.setTextAndValue(Localization.BASED_ON_GLOBAL, false);
        this.typing_cell_global.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m8x5309d23c(view);
            }
        });
        this.typing_cell_global.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        this.typing_cell_never.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
        this.typing_cell_never.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
        this.typing_cell_never.setTextAndValue(Localization.NEVER + " " + Localization.TYPE, false);
        this.typing_cell_never.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m9xdff6e95b(view);
            }
        });
        this.typing_cell_never.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        this.typing_cell_always.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
        this.typing_cell_always.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
        this.typing_cell_always.setTextAndValue(Localization.ALWAYS + " " + Localization.TYPE, false);
        this.typing_cell_always.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.m10x6ce4007a(view);
            }
        });
        this.typing_cell_always.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        this.typing_layout.addView(this.typing_cell_always);
        this.typing_layout.addView(this.typing_cell_never);
        this.typing_layout.addView(this.typing_cell_global);
    }

    /* JADX INFO: renamed from: lambda$new$0$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m5xac428cdf(View l) {
        setReadingException(0);
    }

    /* JADX INFO: renamed from: lambda$new$1$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m6x392fa3fe(View l) {
        setReadingException(-1);
    }

    /* JADX INFO: renamed from: lambda$new$2$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m7xc61cbb1d(View l) {
        setReadingException(1);
    }

    /* JADX INFO: renamed from: lambda$new$3$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m8x5309d23c(View l) {
        setTypingException(0);
    }

    /* JADX INFO: renamed from: lambda$new$4$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m9xdff6e95b(View l) {
        setTypingException(-1);
    }

    /* JADX INFO: renamed from: lambda$new$5$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m10x6ce4007a(View l) {
        setTypingException(1);
    }

    private void setReadingException(int selectedMode) {
        if (selectedMode == 1) {
            DbDeletedStore.get().setDialogReading(SELECTED_DIALOG_ID, 1);
            this.reading_cell_always.setChecked(true, true);
            this.reading_cell_global.setChecked(false, true);
            this.reading_cell_never.setChecked(false, true);
        } else if (selectedMode == -1) {
            DbDeletedStore.get().setDialogReading(SELECTED_DIALOG_ID, -1);
            this.reading_cell_never.setChecked(true, true);
            this.reading_cell_global.setChecked(false, true);
            this.reading_cell_always.setChecked(false, true);
        } else if (selectedMode == 0) {
            DbDeletedStore.get().setDialogReading(SELECTED_DIALOG_ID, 0);
            this.reading_cell_global.setChecked(true, true);
            this.reading_cell_never.setChecked(false, true);
            this.reading_cell_always.setChecked(false, true);
        }
        InterceptOnlineHook.notifyDialogIdChanged(SELECTED_DIALOG_ID);
    }

    private void setTypingException(int selectedMode) {
        if (selectedMode == 1) {
            DbDeletedStore.get().setDialogTyping(SELECTED_DIALOG_ID, 1);
            this.typing_cell_always.setChecked(true, true);
            this.typing_cell_global.setChecked(false, true);
            this.typing_cell_never.setChecked(false, true);
        } else if (selectedMode == -1) {
            DbDeletedStore.get().setDialogTyping(SELECTED_DIALOG_ID, -1);
            this.typing_cell_never.setChecked(true, true);
            this.typing_cell_global.setChecked(false, true);
            this.typing_cell_always.setChecked(false, true);
        } else if (selectedMode == 0) {
            DbDeletedStore.get().setDialogTyping(SELECTED_DIALOG_ID, 0);
            this.typing_cell_global.setChecked(true, true);
            this.typing_cell_never.setChecked(false, true);
            this.typing_cell_always.setChecked(false, true);
        }
        InterceptOnlineHook.notifyDialogIdChanged(SELECTED_DIALOG_ID);
    }

    private void setChecks() {
        int selectedModeReading = DbDeletedStore.get().getDialogReading(SELECTED_DIALOG_ID);
        int selectedModeTyping = DbDeletedStore.get().getDialogTyping(SELECTED_DIALOG_ID);
        this.reading_cell_always.setChecked(selectedModeReading == 1, false);
        this.reading_cell_never.setChecked(selectedModeReading == -1, false);
        this.reading_cell_global.setChecked(selectedModeReading == 0, false);
        this.typing_cell_always.setChecked(selectedModeTyping == 1, false);
        this.typing_cell_never.setChecked(selectedModeTyping == -1, false);
        this.typing_cell_global.setChecked(selectedModeTyping == 0, false);
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        final ChatActivity thisObject = (ChatActivity) param.thisObject;
        long dialog_id = thisObject.getDialogId();
        SELECTED_DIALOG_ID = dialog_id;
        setChecks();
        InterceptOnlineHook.notifyDialogIdChanged(dialog_id);
        ActionBarMenuItem headerItem = (ActionBarMenuItem) headerItemField.get(thisObject);
        if (headerItem == null) {
            return;
        }
        Global.log(String.format("Adding exception menu on: %s", headerItem));
        if (!headerItem.hasSubItem(MENU_EXCEPTION_READING)) {
            ActionBarMenuItem.Item item_reading = headerItem.lazilyAddSubItem(MENU_EXCEPTION_READING, R.drawable.msg_archive_hide, Localization.EXCEPTION_READING_TEXT);
            item_reading.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m3x5622e29a(thisObject, view);
                }
            });
        }
        if (!headerItem.hasSubItem(MENU_EXCEPTION_TYPING)) {
            ActionBarMenuItem.Item item_typing = headerItem.lazilyAddSubItem(MENU_EXCEPTION_TYPING, R.drawable.floating_pencil, Localization.EXCEPTION_TYPING_TEXT);
            item_typing.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.chatactivity.ChatActivityCreateHook$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.m4xe30ff9b9(thisObject, view);
                }
            });
        }
    }

    /* JADX INFO: renamed from: lambda$afterHookedMethod$6$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m3x5622e29a(ChatActivity thisObject, View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(thisObject.getContext());
        builder.setView(this.reading_layout);
        builder.setTitle(Localization.EXCEPTION_READING_TEXT);
        builder.show();
    }

    /* JADX INFO: renamed from: lambda$afterHookedMethod$7$ni-shikatu-re_extera-chatactivity-ChatActivityCreateHook, reason: not valid java name */
    /* synthetic */ void m4xe30ff9b9(ChatActivity thisObject, View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(thisObject.getContext());
        builder.setView(this.typing_layout);
        builder.setTitle(Localization.EXCEPTION_TYPING_TEXT);
        builder.show();
    }
}
