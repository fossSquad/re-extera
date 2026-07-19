package ni.shikatu.re_extera.settings.newui;

import android.text.TextUtils;
import android.view.View;
import com.exteragram.messenger.preferences.utils.SettingsRegistry;
import com.exteragram.messenger.utils.system.VibratorUtils;
import java.util.ArrayList;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.ui.RegexFiltersFragment;
import ni.shikatu.re_extera.ui.ShadowbanFragment;
import ni.shikatu.re_extera.utils.InternalUtils;
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
        FILTERS_ID,
        SHADOWBAN_ID,
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
        items.add(UItem.asButton(AdditionalIds.FILTERS_ID.getId(), Localization.FILTERS).setLinkAlias("reExteraFiltersEnter", this));
        items.add(UItem.asButton(AdditionalIds.SHADOWBAN_ID.getId(), Localization.SHADOWBAN).setLinkAlias("reExteraShadowban", this));
        items.add(UItem.asShadow(Localization.SHADOWBAN_ABOUT));
        items.add(UItem.asButton(AdditionalIds.CLEAR_DB_ID.getId(), Localization.CLEAR_DB).setLinkAlias("reExteraClearDb", this));
        items.add(UItem.asButton(AdditionalIds.UNLOAD_HOOKS.getId(), Localization.UNLOAD_REEXTERA).setLinkAlias("reExteraUnloadHooks", this));
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.settings.newui.AdditionalFragment$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds = new int[AdditionalIds.values().length];

        static {
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.IGNORE_FLAG_SECURE_ID.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.NO_FORWARD_ID.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.FILTERS_ID.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.SHADOWBAN_ID.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.LOCAL_PREMIUM_ID.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.CLEAR_DB_ID.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.UNLOAD_HOOKS.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > AdditionalIds.values().length) {
            return;
        }
        switch (AnonymousClass1.$SwitchMap$ni$shikatu$re_extera$settings$newui$AdditionalFragment$AdditionalIds[AdditionalIds.values()[item.id - 1].ordinal()]) {
            case Defaults.ALWAYS /* 1 */:
                Settings.setRemoveFlagSecure(!Settings.getRemoveFlagSecure());
                refreshCheckBox(item, position, Settings.getRemoveFlagSecure());
                break;
            case 2:
                Settings.setNoForward(!Settings.noForward());
                refreshCheckBox(item, position, Settings.noForward());
                break;
            case 3:
                presentFragment(new RegexFiltersFragment());
                break;
            case 4:
                presentFragment(new ShadowbanFragment());
                break;
            case 5:
                if (UserConfig.getInstance(getCurrentAccount()).isPremium() && !Settings.getLocalPremium()) {
                    BulletinFactory.of(this).createEmojiBulletin("❌", Localization.CANT_USE_WITH_PREMIUM).show();
                } else {
                    Settings.setLocalPremium(!Settings.getLocalPremium());
                    refreshCheckBox(item, position, Settings.getLocalPremium());
                    getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
                }
                break;
            case 6:
                showClearDbDialog();
                break;
            case 7:
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
        ItemOptions.makeOptions(this, view).add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), new Runnable() { 
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
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { 
            public final void onClick(AlertDialog alertDialog, int i) {
                this.f$0.lambda$showClearDbDialog$2(alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.NO, new AlertDialog.OnButtonClickListener() { 
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
        final int currentAccount = getCurrentAccount();
        new Thread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                AdditionalFragment.lambda$showClearDbDialog$1(currentAccount, progressDialog);
            }
        }).start();
    }

    static /* synthetic */ void lambda$showClearDbDialog$1(int currentAccount, final AlertDialog progressDialog) {
        InternalUtils.clearSavedMessages(currentAccount);
        progressDialog.getClass();
        AndroidUtilities.runOnUIThread(new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                progressDialog.dismiss();
            }
        });
    }
}
