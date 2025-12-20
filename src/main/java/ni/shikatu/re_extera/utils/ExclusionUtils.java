package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import java.lang.reflect.InvocationTargetException;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;

public class ExclusionUtils {

    public static class ExclusionReadingDialog {
        private Context context;
        long dialogId;
        private Runnable onChoice;
        private RadioColorCell reading_cell_Defaults;
        private RadioColorCell reading_cell_always;
        private RadioColorCell reading_cell_never;
        private LinearLayout reading_layout;

        public ExclusionReadingDialog(Context context, long dialogId, Runnable onChoice) {
            this.dialogId = dialogId;
            this.context = context;
            this.onChoice = onChoice;
            this.reading_layout = new LinearLayout(context);
            this.reading_layout.setOrientation(1);
            this.reading_cell_Defaults = new RadioColorCell(context);
            this.reading_cell_always = new RadioColorCell(context);
            this.reading_cell_never = new RadioColorCell(context);
            this.reading_cell_Defaults.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            this.reading_cell_Defaults.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            this.reading_cell_Defaults.setTextAndValue(Localization.BASED_ON_GLOBAL, false);
            this.reading_cell_Defaults.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExclusionUtils$ExclusionReadingDialog$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$0(view);
                }
            });
            this.reading_cell_Defaults.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            this.reading_cell_never.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            this.reading_cell_never.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            this.reading_cell_never.setTextAndValue(Localization.NEVER + Localization.NOT + Localization.READ, false);
            this.reading_cell_never.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExclusionUtils$ExclusionReadingDialog$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$1(view);
                }
            });
            this.reading_cell_never.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            this.reading_cell_always.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            this.reading_cell_always.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            this.reading_cell_always.setTextAndValue(Localization.ALWAYS + " " + Localization.READ, false);
            this.reading_cell_always.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExclusionUtils$ExclusionReadingDialog$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$2(view);
                }
            });
            this.reading_cell_always.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            this.reading_layout.addView(this.reading_cell_always);
            this.reading_layout.addView(this.reading_cell_never);
            this.reading_layout.addView(this.reading_cell_Defaults);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$0(View l) {
            setReadingException(0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$1(View l) {
            setReadingException(-1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$2(View l) {
            setReadingException(1);
        }

        private void setReadingException(int selectedMode) {
            if (selectedMode == 1) {
                ReExteraDb.get().setDialogReading(this.dialogId, 1);
                this.reading_cell_always.setChecked(true, true);
                this.reading_cell_Defaults.setChecked(false, true);
                this.reading_cell_never.setChecked(false, true);
            } else if (selectedMode == -1) {
                ReExteraDb.get().setDialogReading(this.dialogId, -1);
                this.reading_cell_never.setChecked(true, true);
                this.reading_cell_Defaults.setChecked(false, true);
                this.reading_cell_always.setChecked(false, true);
            } else if (selectedMode == 0) {
                ReExteraDb.get().setDialogReading(this.dialogId, 0);
                this.reading_cell_Defaults.setChecked(true, true);
                this.reading_cell_never.setChecked(false, true);
                this.reading_cell_always.setChecked(false, true);
            }
            if (this.onChoice != null) {
                this.onChoice.run();
            }
        }

        public void show() throws IllegalAccessException, InvocationTargetException {
            int selectedModeReading = ReExteraDb.get().getDialogReading(this.dialogId);
            this.reading_cell_always.setChecked(selectedModeReading == 1, false);
            this.reading_cell_never.setChecked(selectedModeReading == -1, false);
            this.reading_cell_Defaults.setChecked(selectedModeReading == 0, false);
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setView(this.reading_layout);
            builder.setTitle(Localization.EXCEPTION_READING_TEXT);
            builder.show();
        }
    }

    public static class ExclusionTypingDialog {
        private Context context;
        long dialogId;
        private Runnable onChoice;
        private RadioColorCell typing_cell_Defaults;
        private RadioColorCell typing_cell_always;
        private RadioColorCell typing_cell_never;
        private LinearLayout typing_layout;

        public ExclusionTypingDialog(Context context, long dialogId, Runnable onChoice) {
            this.dialogId = dialogId;
            this.context = context;
            this.onChoice = onChoice;
            this.typing_layout = new LinearLayout(context);
            this.typing_layout.setOrientation(1);
            this.typing_cell_never = new RadioColorCell(context);
            this.typing_cell_always = new RadioColorCell(context);
            this.typing_cell_Defaults = new RadioColorCell(context);
            this.typing_cell_Defaults.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            this.typing_cell_Defaults.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            this.typing_cell_Defaults.setTextAndValue(Localization.BASED_ON_GLOBAL, false);
            this.typing_cell_Defaults.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExclusionUtils$ExclusionTypingDialog$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$0(view);
                }
            });
            this.typing_cell_Defaults.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            this.typing_cell_never.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            this.typing_cell_never.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            this.typing_cell_never.setTextAndValue(Localization.NEVER + Localization.NOT + Localization.TYPE, false);
            this.typing_cell_never.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExclusionUtils$ExclusionTypingDialog$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$1(view);
                }
            });
            this.typing_cell_never.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            this.typing_cell_always.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            this.typing_cell_always.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            this.typing_cell_always.setTextAndValue(Localization.ALWAYS + " " + Localization.TYPE, false);
            this.typing_cell_always.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExclusionUtils$ExclusionTypingDialog$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$2(view);
                }
            });
            this.typing_cell_always.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            this.typing_layout.addView(this.typing_cell_always);
            this.typing_layout.addView(this.typing_cell_never);
            this.typing_layout.addView(this.typing_cell_Defaults);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$0(View l) {
            setTypingException(0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$1(View l) {
            setTypingException(-1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$2(View l) {
            setTypingException(1);
        }

        private void setTypingException(int selectedMode) {
            if (selectedMode == 1) {
                ReExteraDb.get().setDialogTyping(this.dialogId, 1);
                this.typing_cell_always.setChecked(true, true);
                this.typing_cell_Defaults.setChecked(false, true);
                this.typing_cell_never.setChecked(false, true);
            } else if (selectedMode == -1) {
                ReExteraDb.get().setDialogTyping(this.dialogId, -1);
                this.typing_cell_never.setChecked(true, true);
                this.typing_cell_Defaults.setChecked(false, true);
                this.typing_cell_always.setChecked(false, true);
            } else if (selectedMode == 0) {
                ReExteraDb.get().setDialogTyping(this.dialogId, 0);
                this.typing_cell_Defaults.setChecked(true, true);
                this.typing_cell_never.setChecked(false, true);
                this.typing_cell_always.setChecked(false, true);
            }
            if (this.onChoice != null) {
                this.onChoice.run();
            }
        }

        public void show() throws IllegalAccessException, InvocationTargetException {
            int selectedModeTyping = ReExteraDb.get().getDialogTyping(this.dialogId);
            this.typing_cell_always.setChecked(selectedModeTyping == 1, false);
            this.typing_cell_never.setChecked(selectedModeTyping == -1, false);
            this.typing_cell_Defaults.setChecked(selectedModeTyping == 0, false);
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setView(this.typing_layout);
            builder.setTitle(Localization.EXCEPTION_TYPING_TEXT);
            builder.show();
        }
    }
}
