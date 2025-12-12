package ni.shikatu.re_extera.settings.newui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import com.exteragram.messenger.preferences.utils.SettingsRegistry;
import com.exteragram.messenger.utils.system.VibratorUtils;
import java.util.ArrayList;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.ui.ExclusionsFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class GhostFragment extends BasePreferencesActivityExtended {
    private boolean isGhostExpanded;

    private enum GhostIds {
        GHOST_ID,
        GHOST_HIDE_ONLINE_ID,
        GHOST_IMMEDIATE_OFFLINE_ID,
        GHOST_HIDE_TYPING_ID,
        GHOST_HIDE_READING_ID,
        GHOST_NO_READ_STORIES_ID,
        SEND_SILENCE_ID,
        ADD_GHOST_TO_DRAWER_ID,
        USE_SCHEDULE_ID,
        EXCLUSIONS_BUTTON_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    private UItem ghostUItem() {
        UItem ghostItem = UItem.asExteraExpandableSwitch(GhostIds.GHOST_ID.getId(), Localization.GHOST_MODE, String.format("%d/5", Integer.valueOf(Settings.countOfGhost())), new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$ghostUItem$0(view);
            }
        });
        ghostItem.setChecked(Settings.getGhostModeEnabledGlobal());
        ghostItem.setCollapsed(!this.isGhostExpanded);
        ghostItem.clickCallback = new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$ghostUItem$1(view);
            }
        };
        return ghostItem;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$ghostUItem$0(View v) {
        Settings.setGhostModeEnabledGlobal(!Settings.getGhostModeEnabledGlobal());
        this.listView.adapter.update(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$ghostUItem$1(View v) {
        this.isGhostExpanded = !this.isGhostExpanded;
        this.listView.adapter.update(true);
    }

    public String getTitle() {
        return Localization.GHOST_MODE;
    }

    public static String getSilenceString() {
        switch (Settings.getSendSilence()) {
            case Defaults.ALWAYS /* 1 */:
                return Localization.ALWAYS;
            case Main.VERSION_CODE /* 2 */:
                return Localization.ONLY_WITH_GHOST;
            default:
                return Localization.NEVER;
        }
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(ghostUItem().setLinkAlias("reExteraGhostMode", this));
        if (this.isGhostExpanded) {
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_HIDE_ONLINE_ID.getId(), Localization.HIDE_ONLINE_STATUS).setChecked(Settings.getHideOnline()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_IMMEDIATE_OFFLINE_ID.getId(), Localization.IMMEDIATE_OFFLINE).setChecked(Settings.getImmediateOffline()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_HIDE_TYPING_ID.getId(), Localization.HIDE_TYPING_STATUS).setChecked(Settings.getHideTyping()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_HIDE_READING_ID.getId(), Localization.HIDE_READING_MESSAGE).setChecked(Settings.getHideReading()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_NO_READ_STORIES_ID.getId(), Localization.NO_READ_STORIES).setChecked(Settings.getNoReadStories()).pad());
        }
        items.add(UItem.asCheck(GhostIds.USE_SCHEDULE_ID.getId(), Localization.USE_SCHEDULE).setChecked(Settings.getUseSchedule()).setLinkAlias("reExteraUseSchedule", this));
        items.add(UItem.asButton(GhostIds.SEND_SILENCE_ID.getId(), Localization.SEND_SILENCE, getSilenceString()).setLinkAlias("reExteraSendSilence", this));
        items.add(UItem.asCheck(GhostIds.ADD_GHOST_TO_DRAWER_ID.getId(), Localization.GHOST_IN_DRAWER).setChecked(Settings.getAddGhostToDrawer()).setLinkAlias("reExteraAddGhostToDrawer", this));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(GhostIds.EXCLUSIONS_BUTTON_ID.getId(), Localization.EXCLUSIONS).setLinkAlias("reExteraExclusions", this));
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > GhostIds.values().length) {
            return;
        }
        GhostIds clicked = GhostIds.values()[item.id - 1];
        switch (clicked.ordinal()) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                this.isGhostExpanded = !this.isGhostExpanded;
                this.listView.adapter.update(true);
                break;
            case Defaults.ALWAYS /* 1 */:
                Settings.setHideOnline(!Settings.getHideOnline());
                refreshCheckBox(item, position, Settings.getHideOnline(), true);
                break;
            case Main.VERSION_CODE /* 2 */:
                Settings.setImmediateOffline(!Settings.getImmediateOffline());
                refreshCheckBox(item, position, Settings.getImmediateOffline(), true);
                break;
            case 3:
                Settings.setHideTyping(!Settings.getHideTyping());
                refreshCheckBox(item, position, Settings.getHideTyping(), true);
                break;
            case 4:
                Settings.setHideReading(!Settings.getHideReading());
                refreshCheckBox(item, position, Settings.getHideReading(), true);
                break;
            case 5:
                Settings.setNoReadStories(!Settings.getNoReadStories());
                refreshCheckBox(item, position, Settings.getNoReadStories(), true);
                break;
            case 6:
                new SendSilenceDialog(getParentActivity(), new Runnable() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.lambda$onClick$2();
                    }
                }).show();
                break;
            case ReExteraDb.DB_VERSION /* 7 */:
                Settings.setAddGhostToDrawer(!Settings.getAddGhostToDrawer());
                refreshCheckBox(item, position, Settings.getAddGhostToDrawer());
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
                break;
            case 8:
                Settings.setUseSchedule(!Settings.getUseSchedule());
                refreshCheckBox(item, position, Settings.getUseSchedule());
                break;
            case 9:
                presentFragment(new ExclusionsFragment());
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onClick$2() {
        this.listView.adapter.update(true);
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        final String settingLink = SettingsRegistry.getInstance().getFirstSettingLink(getClass(), item);
        if (TextUtils.isEmpty(settingLink)) {
            return false;
        }
        view.performHapticFeedback(VibratorUtils.getType(3), 1);
        ItemOptions.makeOptions(this, view).add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), new Runnable() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AndroidUtilities.addToClipboard(settingLink);
            }
        }).show();
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class SendSilenceDialog {
        private RadioColorCell always;
        private Context context;
        private LinearLayout layout;
        private RadioColorCell never;
        private Runnable onSelect;
        private RadioColorCell onlyWithGhost;

        SendSilenceDialog(Context context, Runnable onSelect) {
            this.context = context;
            this.onSelect = onSelect;
            prepare();
        }

        private void prepare() {
            this.layout = new LinearLayout(this.context);
            this.layout.setOrientation(1);
            this.always = new RadioColorCell(this.context);
            this.always.setTextAndValue(Localization.ALWAYS, Settings.getSendSilence() == Settings.SendSilence.YES.getType());
            this.always.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$SendSilenceDialog$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$prepare$0(view);
                }
            });
            this.onlyWithGhost = new RadioColorCell(this.context);
            this.onlyWithGhost.setTextAndValue(Localization.ONLY_WITH_GHOST, Settings.getSendSilence() == Settings.SendSilence.ONLY_WITH_GHOST.getType());
            this.onlyWithGhost.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$SendSilenceDialog$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$prepare$1(view);
                }
            });
            this.never = new RadioColorCell(this.context);
            this.never.setTextAndValue(Localization.NEVER, Settings.getSendSilence() == Settings.SendSilence.NO.getType());
            this.never.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$SendSilenceDialog$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$prepare$2(view);
                }
            });
            this.layout.addView(this.always);
            this.layout.addView(this.onlyWithGhost);
            this.layout.addView(this.never);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$prepare$0(View v) {
            Settings.setSendSilence(Settings.SendSilence.YES);
            this.always.setChecked(true, true);
            this.onlyWithGhost.setChecked(false, true);
            this.never.setChecked(false, true);
            this.onSelect.run();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$prepare$1(View v) {
            Settings.setSendSilence(Settings.SendSilence.ONLY_WITH_GHOST);
            this.always.setChecked(false, true);
            this.onlyWithGhost.setChecked(true, true);
            this.never.setChecked(false, true);
            this.onSelect.run();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$prepare$2(View v) {
            Settings.setSendSilence(Settings.SendSilence.NO);
            this.always.setChecked(false, true);
            this.onlyWithGhost.setChecked(false, true);
            this.never.setChecked(true, true);
            this.onSelect.run();
        }

        public void show() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setView(this.layout);
            builder.setTitle(Localization.SEND_SILENCE);
            builder.show();
        }
    }
}
