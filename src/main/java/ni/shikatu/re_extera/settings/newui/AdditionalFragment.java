package ni.shikatu.re_extera.settings.newui;

import android.view.View;
import java.util.ArrayList;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.ui.RegexFiltersFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class AdditionalFragment extends BasePreferencesActivityExtended {

    private enum AdditionalIds {
        IGNORE_FLAG_SECURE_ID,
        NO_FORWARD_ID,
        FILTERS_ID,
        CLEAR_DB_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    public String getTitle() {
        return Localization.OTHER;
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(AdditionalIds.IGNORE_FLAG_SECURE_ID.getId(), Localization.REMOVE_FLAG_SECURE).setChecked(Settings.getRemoveFlagSecure()));
        items.add(UItem.asCheck(AdditionalIds.NO_FORWARD_ID.getId(), Localization.NO_FORWARD).setChecked(Settings.noForward()));
        items.add(UItem.asShadow(Localization.NO_FORWARD_ABOUT));
        items.add(UItem.asButton(AdditionalIds.FILTERS_ID.getId(), Localization.FILTERS));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(AdditionalIds.CLEAR_DB_ID.getId(), Localization.CLEAR_DB));
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > AdditionalIds.values().length) {
            return;
        }
        AdditionalIds clicked = AdditionalIds.values()[item.id - 1];
        switch (clicked.ordinal()) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                Settings.setRemoveFlagSecure(!Settings.getRemoveFlagSecure());
                refreshCheckBox(item, position, Settings.getRemoveFlagSecure());
                break;
            case Defaults.ALWAYS /* 1 */:
                Settings.setNoForward(!Settings.noForward());
                refreshCheckBox(item, position, Settings.noForward());
                break;
            case 2:
                presentFragment(new RegexFiltersFragment());
                break;
            case 3:
                showClearDbDialog();
                break;
        }
    }

    private void showClearDbDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(Localization.CLEAR_DB + "?");
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda0
            public final void onClick(AlertDialog alertDialog, int i) {
                this.f$0.lambda$showClearDbDialog$1(alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.NO, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda1
            public final void onClick(AlertDialog alertDialog, int i) {
                alertDialog.dismiss();
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showClearDbDialog$1(AlertDialog dialog, int which) {
        final AlertDialog progressDialog = new AlertDialog(getContext(), 0);
        progressDialog.setTitle(Localization.CLEARING_NOW);
        progressDialog.setNegativeButton("", (AlertDialog.OnButtonClickListener) null);
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread(new Runnable() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                AdditionalFragment.lambda$showClearDbDialog$0(progressDialog);
            }
        }).start();
    }

    static /* synthetic */ void lambda$showClearDbDialog$0(final AlertDialog progressDialog) {
        ReExteraDb.get().clearDatabaseWithInternal();
        progressDialog.getClass();
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                progressDialog.dismiss();
            }
        });
    }
}
