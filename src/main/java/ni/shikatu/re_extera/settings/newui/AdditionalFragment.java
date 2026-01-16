package ni.shikatu.re_extera.settings.newui;

import android.text.TextUtils;
import android.view.View;
import com.exteragram.messenger.preferences.utils.SettingsRegistry;
import com.exteragram.messenger.utils.system.VibratorUtils;
import java.util.ArrayList;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.ui.RegexFiltersFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class AdditionalFragment extends BasePreferencesActivityExtended {

    private enum AdditionalIds {
        IGNORE_FLAG_SECURE_ID,
        NO_FORWARD_ID,
        LOCAL_PREMIUM_ID,
        ADD_SETTINGS_TO_DRAWER,
        FILTERS_ID,
        CLEAR_DB_ID,
        UNLOAD_HOOKS;

        public int getId() {
            return ordinal() + 1;
        }
    }

    public String getTitle() {
        return Localization.OTHER;
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(AdditionalIds.IGNORE_FLAG_SECURE_ID.getId(), Localization.REMOVE_FLAG_SECURE).setChecked(Settings.getRemoveFlagSecure()).setLinkAlias("reExteraIgnoreFlagSecure", this));
        items.add(UItem.asCheck(AdditionalIds.LOCAL_PREMIUM_ID.getId(), Localization.LOCAL_PREMIUM).setChecked(Settings.getLocalPremium()).setLinkAlias("reExteraLocalPremium", this));
        items.add(UItem.asCheck(AdditionalIds.NO_FORWARD_ID.getId(), Localization.NO_FORWARD).setChecked(Settings.noForward()).setLinkAlias("reExteraNoForward", this));
        items.add(UItem.asShadow(Localization.NO_FORWARD_ABOUT));
        items.add(UItem.asCheck(AdditionalIds.ADD_SETTINGS_TO_DRAWER.getId(), Localization.ADD_SETTINGS_TO_DRAWER).setChecked(Settings.getShowSettingsInDrawer()).setLinkAlias("reExteraAddSettingsToDrawer", this));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(AdditionalIds.FILTERS_ID.getId(), Localization.FILTERS).setLinkAlias("reExteraFiltersEnter", this));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(AdditionalIds.CLEAR_DB_ID.getId(), Localization.CLEAR_DB).setLinkAlias("reExteraClearDb", this));
        items.add(UItem.asButton(AdditionalIds.UNLOAD_HOOKS.getId(), Localization.UNLOAD_REEXTERA).setLinkAlias("reExteraUnloadHooks", this));
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
                if (UserConfig.getInstance(UserConfig.selectedAccount).isPremium() && !Settings.getLocalPremium()) {
                    BulletinFactory.of(this).createEmojiBulletin("❌", Localization.CANT_USE_WITH_PREMIUM).show();
                } else {
                    Settings.setLocalPremium(!Settings.getLocalPremium());
                    refreshCheckBox(item, position, Settings.getLocalPremium());
                    getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
                }
                break;
            case 3:
                Settings.setShowSettingsInDrawer(!Settings.getShowSettingsInDrawer());
                refreshCheckBox(item, position, Settings.getShowSettingsInDrawer());
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
                break;
            case 4:
                presentFragment(new RegexFiltersFragment());
                break;
            case 5:
                showClearDbDialog();
                break;
            case Main.VERSION_CODE /* 6 */:
                Main.getInstance().onUnload();
                BulletinFactory.of(this).createSuccessBulletin(Localization.UNLOAD_SUCCESSFULL).show();
                break;
        }
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        final String settingLink = SettingsRegistry.getInstance().getFirstSettingLink(getClass(), item);
        if (TextUtils.isEmpty(settingLink)) {
            return false;
        }
        view.performHapticFeedback(VibratorUtils.getType(3), 1);
        ItemOptions.makeOptions(this, view).add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), new Runnable() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                AndroidUtilities.addToClipboard(settingLink);
            }
        }).show();
        return false;
    }

    private void showClearDbDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(Localization.CLEAR_DB + "?");
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda1
            public final void onClick(AlertDialog alertDialog, int i) {
                this.f$0.lambda$showClearDbDialog$2(alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.NO, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda2
            public final void onClick(AlertDialog alertDialog, int i) {
                alertDialog.dismiss();
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showClearDbDialog$2(AlertDialog dialog, int which) {
        final AlertDialog progressDialog = new AlertDialog(getContext(), 0);
        progressDialog.setTitle(Localization.CLEARING_NOW);
        progressDialog.setNegativeButton("", (AlertDialog.OnButtonClickListener) null);
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread(new Runnable() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                AdditionalFragment.lambda$showClearDbDialog$1(progressDialog);
            }
        }).start();
    }

    static /* synthetic */ void lambda$showClearDbDialog$1(final AlertDialog progressDialog) {
        ReExteraDb.get().clearDatabaseWithInternal();
        progressDialog.getClass();
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.settings.newui.AdditionalFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                progressDialog.dismiss();
            }
        });
    }
}
