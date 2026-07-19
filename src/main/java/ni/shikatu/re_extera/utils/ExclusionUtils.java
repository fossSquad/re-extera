package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;

public final class ExclusionUtils {

    public enum Kind {
        READING,
        TYPING
    }

    private ExclusionUtils() {
    }

    public static final class ExclusionReadingDialog {
        private final ExclusionDialog dialog;

        public ExclusionReadingDialog(Context context, long dialogId, Runnable onChoice) {
            this.dialog = new ExclusionDialog(context, dialogId, Kind.READING, onChoice);
        }

        public void show() {
            this.dialog.show();
        }
    }

    public static final class ExclusionTypingDialog {
        private final ExclusionDialog dialog;

        public ExclusionTypingDialog(Context context, long dialogId, Runnable onChoice) {
            this.dialog = new ExclusionDialog(context, dialogId, Kind.TYPING, onChoice);
        }

        public void show() {
            this.dialog.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class ExclusionDialog {
        private final RadioColorCell cellAlways;
        private final RadioColorCell cellDefault;
        private final RadioColorCell cellNever;
        private final Context context;
        private final long dialogId;
        private final Kind kind;
        private final LinearLayout layout;
        private final Runnable onChoice;

        ExclusionDialog(Context context, long dialogId, Kind kind, Runnable onChoice) {
            this.context = context;
            this.dialogId = dialogId;
            this.kind = kind;
            this.onChoice = onChoice;
            this.layout = new LinearLayout(context);
            this.layout.setOrientation(1);
            this.cellAlways = buildCell(Localization.ALWAYS + " " + verb());
            this.cellNever = buildCell(Localization.NEVER + Localization.NOT + verb());
            this.cellDefault = buildCell(Localization.BASED_ON_GLOBAL);
            this.cellAlways.setOnClickListener(new View.OnClickListener() { 
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    lambda$new$0(view);
                }
            });
            this.cellNever.setOnClickListener(new View.OnClickListener() { 
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    lambda$new$1(view);
                }
            });
            this.cellDefault.setOnClickListener(new View.OnClickListener() { 
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    lambda$new$2(view);
                }
            });
            this.layout.addView(this.cellAlways);
            this.layout.addView(this.cellNever);
            this.layout.addView(this.cellDefault);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$0(View v) {
            setValue(1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$1(View v) {
            setValue(-1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$2(View v) {
            setValue(0);
        }

        private String verb() {
            return this.kind == Kind.READING ? Localization.READ : Localization.TYPE;
        }

        private RadioColorCell buildCell(String text) {
            RadioColorCell cell = new RadioColorCell(this.context);
            cell.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(text, false);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            return cell;
        }

        private void setValue(int mode) {
            if (this.kind == Kind.READING) {
                ReExteraDb.get().setDialogReadingAsync(this.dialogId, mode);
            } else {
                ReExteraDb.get().setDialogTypingAsync(this.dialogId, mode);
            }
            this.cellAlways.setChecked(mode == 1, true);
            this.cellNever.setChecked(mode == -1, true);
            this.cellDefault.setChecked(mode == 0, true);
            if (this.onChoice != null) {
                this.onChoice.run();
            }
        }

        void show() {
            int current;
            if (this.kind == Kind.READING) {
                current = ReExteraDb.get().getDialogReading(this.dialogId);
            } else {
                current = ReExteraDb.get().getDialogTyping(this.dialogId);
            }
            this.cellAlways.setChecked(current == 1, false);
            this.cellNever.setChecked(current == -1, false);
            this.cellDefault.setChecked(current == 0, false);
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setView(this.layout);
            builder.setTitle(this.kind == Kind.READING ? Localization.EXCEPTION_READING_TEXT : Localization.EXCEPTION_TYPING_TEXT);
            builder.show();
        }
    }
}
