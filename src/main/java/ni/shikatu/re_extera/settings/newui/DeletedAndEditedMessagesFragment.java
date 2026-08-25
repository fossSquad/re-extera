package ni.shikatu.re_extera.settings.newui;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.exteragram.messenger.preferences.utils.SettingsRegistry;
import com.exteragram.messenger.utils.system.VibratorUtils;
import java.util.ArrayList;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class DeletedAndEditedMessagesFragment extends BasePreferencesActivityExtended {

    private enum DeletedAndEditedIds {
        SAVE_DELETED_MESSAGES_ID,
        SAVE_BOT_CHATS_ID,
        SAVE_READ_DATE_ID,
        SAVE_LAST_ONLINE_ID,
        SAVE_ONE_TIME_MESSAGES_ID,
        SAVE_MESSAGE_HISTORY_ID,
        SAVE_ATTACHMENTS_ID,
        SAVE_ATTACHMENTS_SIZE_ID,
        TRANSPARENT_DELETED_MESSAGES_ID,
        DELETED_MESSAGE_CUSTOMIZATION_ID,
        VIEW_ONCE_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    public String getTitle() {
        return Localization.SPY;
    }

    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(DeletedAndEditedIds.SAVE_ATTACHMENTS_ID.getId(), Localization.SAVE_ATTACHMENTS).setChecked(Settings.getSaveAttachments()).setLinkAlias("reExteraSaveAttachments", this));
        items.add(UItem.asHeader(Localization.SAVE_ATTACHMENTS_SIZE));
        final long[] sizes = new long[]{
            100L * 1024 * 1024,
            500L * 1024 * 1024,
            1024L * 1024 * 1024,
            2L * 1024 * 1024 * 1024,
            5L * 1024 * 1024 * 1024,
            0L // Infinite
        };
        String[] sizeStrings = new String[]{
            "100M",
            "500M",
            "1G",
            "2G",
            "5G",
            "∞"
        };
        int selectedIndex = 5;
        long currentSize = Settings.getAttachmentsMaxSize();
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] == currentSize) {
                selectedIndex = i;
                break;
            }
        }
        items.add(UItem.asSlideView(DeletedAndEditedIds.SAVE_ATTACHMENTS_SIZE_ID.getId(), sizeStrings, selectedIndex, new org.telegram.messenger.Utilities.Callback<Integer>() {
            @Override
            public void run(Integer index) {
                Settings.setAttachmentsMaxSize(sizes[index]);
            }
        }).setLinkAlias("reExteraSaveAttachmentsSize", this));
        items.add(UItem.asShadow(Localization.SAVE_ATTACHMENTS_DESC));

        items.add(UItem.asCheck(DeletedAndEditedIds.SAVE_READ_DATE_ID.getId(), Localization.SAVE_READ_DATE).setChecked(Settings.getSaveReadDate()).setLinkAlias("reExteraSaveReadDate", this));
        items.add(UItem.asCheck(DeletedAndEditedIds.SAVE_LAST_ONLINE_ID.getId(), Localization.SAVE_LAST_ONLINE).setChecked(Settings.getSaveLastOnline()).setLinkAlias("reExteraSaveLastOnline", this));
        items.add(UItem.asShadow());

        items.add(UItem.asCheck(DeletedAndEditedIds.VIEW_ONCE_ID.getId(), Localization.VIEW_ONCE).setChecked(Settings.getSaveOneTimeMessages()).setLinkAlias("reExteraViewOnce", this));
        items.add(UItem.asShadow(Localization.VIEW_ONCE_ABOUT));

        items.add(UItem.asButton(DeletedAndEditedIds.DELETED_MESSAGE_CUSTOMIZATION_ID.getId(), Localization.DELETED_MESSAGE).setLinkAlias("reExteraDeletedMessageCustomization", this));
        items.add(UItem.asShadow());
    }

    public void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > DeletedAndEditedIds.values().length) {
            return;
        }
        DeletedAndEditedIds clicked = DeletedAndEditedIds.values()[item.id - 1];
        if (clicked == DeletedAndEditedIds.DELETED_MESSAGE_CUSTOMIZATION_ID) {
            presentFragment(new CustomizationFragment());
            return;
        }
        if (clicked == DeletedAndEditedIds.VIEW_ONCE_ID) {
            Settings.setSaveOneTimeMessages(!Settings.getSaveOneTimeMessages());
            refreshCheckBox(item, position, Settings.getSaveOneTimeMessages());
            return;
        }
        switch (AnonymousClass2.$SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[clicked.ordinal()]) {
            case Defaults.ALWAYS /* 1 */:
                Settings.setSaveDeletedMessages(!Settings.getSaveDeletedMessages());
                refreshCheckBox(item, position, Settings.getSaveDeletedMessages());
                break;
            case 2:
                Settings.setSaveBotChats(!Settings.getSaveBotChats());
                refreshCheckBox(item, position, Settings.getSaveBotChats());
                break;
            case 3:
                Settings.setSaveReadDate(!Settings.getSaveReadDate());
                refreshCheckBox(item, position, Settings.getSaveReadDate());
                break;
            case 4:
                Settings.setSaveLastOnline(!Settings.getSaveLastOnline());
                refreshCheckBox(item, position, Settings.getSaveLastOnline());
                break;
            case 5:
                Settings.setSaveOneTimeMessages(!Settings.getSaveOneTimeMessages());
                refreshCheckBox(item, position, Settings.getSaveOneTimeMessages());
                break;
            case 6:
                Settings.setSaveEditedMessages(!Settings.getSaveEditedMessages());
                refreshCheckBox(item, position, Settings.getSaveEditedMessages());
                break;
            case 7:
                Settings.setSaveAttachments(!Settings.getSaveAttachments());
                refreshCheckBox(item, position, Settings.getSaveAttachments());
                break;
            case 9:
                Settings.setTransparentDeletedMessages(!Settings.getTransparentDeletedMessages());
                refreshCheckBox(item, position, Settings.getTransparentDeletedMessages());
                break;
        }
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.settings.newui.DeletedAndEditedMessagesFragment$2, reason: invalid class name */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds = new int[DeletedAndEditedIds.values().length];

        static {
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_DELETED_MESSAGES_ID.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_BOT_CHATS_ID.ordinal()] = 2;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_READ_DATE_ID.ordinal()] = 3;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_LAST_ONLINE_ID.ordinal()] = 4;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_ONE_TIME_MESSAGES_ID.ordinal()] = 5;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_MESSAGE_HISTORY_ID.ordinal()] = 6;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_ATTACHMENTS_ID.ordinal()] = 7;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.SAVE_ATTACHMENTS_SIZE_ID.ordinal()] = 8;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[DeletedAndEditedIds.TRANSPARENT_DELETED_MESSAGES_ID.ordinal()] = 9;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    public boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > DeletedAndEditedIds.values().length) {
            return false;
        }
        final String settingLink = SettingsRegistry.getInstance().getFirstSettingLink(getClass(), item);
        if (!TextUtils.isEmpty(settingLink)) {
            view.performHapticFeedback(VibratorUtils.getType(3), 1);
            ItemOptions.makeOptions(this, view).add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), new Runnable() {
                @Override // java.lang.Runnable
                public final void run() {
                    AndroidUtilities.addToClipboard(settingLink);
                }
            }).show();
        }
        return false;
    }


}
