package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import ni.shikatu.re_extera.db.ShadowbanEntry;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.ShadowbanCache;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;

public class ShadowbanDialog {

    public interface OnShadowbanResult {
        void onResult(boolean z, boolean z2);
    }

    public static void showAdd(Context context, long userId, OnShadowbanResult callback) {
        show(context, userId, true, true, callback);
    }

    public static void showEdit(Context context, ShadowbanEntry entry, OnShadowbanResult callback) {
        show(context, entry.userId, entry.hideDialog, entry.hideInGroups, callback);
    }

    private static void show(Context context, long userId, boolean initialHideDialog, boolean initialHideInGroups, final OnShadowbanResult callback) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(1);
        layout.setPadding(0, AndroidUtilities.dp(8.0f), 0, 0);
        final boolean[] hideDialog = {initialHideDialog};
        final boolean[] hideInGroups = {initialHideInGroups};
        final TextCheckCell hideDialogCell = new TextCheckCell(context);
        hideDialogCell.setTextAndCheck(Localization.HIDE_DIALOG, hideDialog[0], true);
        hideDialogCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        hideDialogCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.ui.ShadowbanDialog$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ShadowbanDialog.lambda$show$0(hideDialog, hideDialogCell, view);
            }
        });
        layout.addView(hideDialogCell);
        final TextCheckCell hideInGroupsCell = new TextCheckCell(context);
        hideInGroupsCell.setTextAndCheck(Localization.HIDE_IN_GROUPS, hideInGroups[0], false);
        hideInGroupsCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        hideInGroupsCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.ui.ShadowbanDialog$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ShadowbanDialog.lambda$show$1(hideInGroups, hideInGroupsCell, view);
            }
        });
        layout.addView(hideInGroupsCell);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.SHADOWBAN);
        builder.setView(layout);
        builder.setPositiveButton(Localization.SAVE, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.ui.ShadowbanDialog$$ExternalSyntheticLambda3
            public final void onClick(AlertDialog alertDialog, int i) {
                ShadowbanDialog.lambda$show$2(callback, hideDialog, hideInGroups, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }

    static /* synthetic */ void lambda$show$0(boolean[] hideDialog, TextCheckCell hideDialogCell, View v) {
        hideDialog[0] = !hideDialog[0];
        hideDialogCell.setChecked(hideDialog[0]);
    }

    static /* synthetic */ void lambda$show$1(boolean[] hideInGroups, TextCheckCell hideInGroupsCell, View v) {
        hideInGroups[0] = !hideInGroups[0];
        hideInGroupsCell.setChecked(hideInGroups[0]);
    }

    static /* synthetic */ void lambda$show$2(OnShadowbanResult callback, boolean[] hideDialog, boolean[] hideInGroups, AlertDialog dialog, int which) {
        if (callback != null) {
            callback.onResult(hideDialog[0], hideInGroups[0]);
        }
    }

    public static void showAddAndSave(Context context, final long userId, final Runnable onComplete) {
        show(context, userId, true, true, new OnShadowbanResult() { // from class: ni.shikatu.re_extera.ui.ShadowbanDialog$$ExternalSyntheticLambda0
            @Override // ni.shikatu.re_extera.ui.ShadowbanDialog.OnShadowbanResult
            public final void onResult(boolean z, boolean z2) {
                ShadowbanDialog.lambda$showAddAndSave$3(userId, onComplete, z, z2);
            }
        });
    }

    static /* synthetic */ void lambda$showAddAndSave$3(long userId, Runnable onComplete, boolean hideDialog, boolean hideInGroups) {
        ShadowbanCache.add(userId, hideDialog, hideInGroups);
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
