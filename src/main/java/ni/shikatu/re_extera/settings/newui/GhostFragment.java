package ni.shikatu.re_extera.settings.newui;

import android.view.View;
import java.util.ArrayList;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.ui.ExclusionsFragment;
import org.telegram.messenger.NotificationCenter;
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
        ADD_GHOST_TO_DRAWER_ID,
        USE_SCHEDULE_ID,
        EXCLUSIONS_BUTTON_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    private UItem ghostUItem() {
        UItem ghostItem = UItem.asExteraExpandableSwitch(GhostIds.GHOST_ID.getId(), Localization.GHOST_MODE, String.format("%d/5", Integer.valueOf(Settings.countOfGhost())), new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$ghostUItem$0(view);
            }
        });
        ghostItem.setChecked(Settings.getGhostModeEnabledGlobal());
        ghostItem.setCollapsed(!this.isGhostExpanded);
        ghostItem.clickCallback = new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.GhostFragment$$ExternalSyntheticLambda1
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

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(ghostUItem());
        if (this.isGhostExpanded) {
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_HIDE_ONLINE_ID.getId(), Localization.HIDE_ONLINE_STATUS).setChecked(Settings.getHideOnline()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_IMMEDIATE_OFFLINE_ID.getId(), Localization.IMMEDIATE_OFFLINE).setChecked(Settings.getImmediateOffline()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_HIDE_TYPING_ID.getId(), Localization.HIDE_TYPING_STATUS).setChecked(Settings.getHideTyping()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_HIDE_READING_ID.getId(), Localization.HIDE_READING_MESSAGE).setChecked(Settings.getHideReading()).pad());
            items.add(UItem.asRoundCheckbox(GhostIds.GHOST_NO_READ_STORIES_ID.getId(), Localization.NO_READ_STORIES).setChecked(Settings.getNoReadStories()).pad());
        }
        items.add(UItem.asCheck(GhostIds.USE_SCHEDULE_ID.getId(), Localization.USE_SCHEDULE).setChecked(Settings.getUseSchedule()));
        items.add(UItem.asCheck(GhostIds.ADD_GHOST_TO_DRAWER_ID.getId(), Localization.GHOST_IN_DRAWER).setChecked(Settings.getAddGhostToDrawer()));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(GhostIds.EXCLUSIONS_BUTTON_ID.getId(), Localization.EXCLUSIONS));
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
            case 2:
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
                Settings.setAddGhostToDrawer(!Settings.getAddGhostToDrawer());
                refreshCheckBox(item, position, Settings.getAddGhostToDrawer());
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged, new Object[0]);
                break;
            case ReExteraDb.DB_VERSION /* 7 */:
                Settings.setUseSchedule(!Settings.getUseSchedule());
                refreshCheckBox(item, position, Settings.getUseSchedule());
                break;
            case 8:
                presentFragment(new ExclusionsFragment());
                break;
        }
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
