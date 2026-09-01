package ni.shikatu.re_extera.settings.newui;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import ni.shikatu.re_extera.utils.SettingsRegistryHelper;
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
        TRANSPARENT_DELETED_MESSAGES_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    public String getTitle() {
        return Localization.SPY;
    }

    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(DeletedAndEditedIds.SAVE_DELETED_MESSAGES_ID.getId(), Localization.SAVE_DELETED_MESSAGES, Localization.HOLD_FOR_ADDITIONAL_SETTINGS, true).setChecked(Settings.getSaveDeletedMessages()).setLinkAlias("reExteraSaveDeletedMessages", this));
        items.add(UItem.asShadow());

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
    }

    public void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > DeletedAndEditedIds.values().length) {
            return;
        }
        DeletedAndEditedIds clicked = DeletedAndEditedIds.values()[item.id - 1];
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
        DeletedAndEditedIds clicked = DeletedAndEditedIds.values()[item.id - 1];
        switch (AnonymousClass2.$SwitchMap$ni$shikatu$re_extera$settings$newui$DeletedAndEditedMessagesFragment$DeletedAndEditedIds[clicked.ordinal()]) {
            case Defaults.ALWAYS /* 1 */:
                final String settingLink = SettingsRegistryHelper.getFirstSettingLink(getClass(), item);
                if (!TextUtils.isEmpty(settingLink)) {
                    view.performHapticFeedback(VibratorUtils.getType(3), 1);
                    ItemOptions.makeOptions(this, view).add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), new Runnable() { 
                        @Override // java.lang.Runnable
                        public final void run() {
                            AndroidUtilities.addToClipboard(settingLink);
                        }
                    }).add(R.drawable.msg_settings, Localization.ADDITIONAL_SETTINGS, new Runnable() { 
                        @Override // java.lang.Runnable
                        public final void run() {
                            showAdditionalDeleted();
                        }
                    }).show();
                    break;
                }
                break;
            default:
                final String settingLink2 = SettingsRegistryHelper.getFirstSettingLink(getClass(), item);
                if (!TextUtils.isEmpty(settingLink2)) {
                    view.performHapticFeedback(VibratorUtils.getType(3), 1);
                    ItemOptions.makeOptions(this, view).add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), new Runnable() { 
                        @Override // java.lang.Runnable
                        public final void run() {
                            AndroidUtilities.addToClipboard(settingLink2);
                        }
                    }).show();
                    break;
                }
                break;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAdditionalDeleted() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(1);

        final TextCheckCell saveMessageHistory = new TextCheckCell(getContext());
        setTextAndValueAndCheck(saveMessageHistory, Localization.MESSAGE_HISTORY_TOGGLE, "", Settings.getSaveEditedMessages(), false, true);
        saveMessageHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Settings.setSaveEditedMessages(!Settings.getSaveEditedMessages());
                saveMessageHistory.setChecked(Settings.getSaveEditedMessages());
            }
        });
        layout.addView(saveMessageHistory);

        final TextCheckCell saveOneTimeMessages = new TextCheckCell(getContext());
        setTextAndValueAndCheck(saveOneTimeMessages, Localization.SAVE_ONE_TIME_MESSAGES, "", Settings.getSaveOneTimeMessages(), false, true);
        saveOneTimeMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Settings.setSaveOneTimeMessages(!Settings.getSaveOneTimeMessages());
                saveOneTimeMessages.setChecked(Settings.getSaveOneTimeMessages());
            }
        });
        layout.addView(saveOneTimeMessages);

        final TextCheckCell saveBotChats = new TextCheckCell(getContext());
        setTextAndValueAndCheck(saveBotChats, Localization.SAVE_BOT_CHATS, "", Settings.getSaveBotChats(), false, true);
        saveBotChats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Settings.setSaveBotChats(!Settings.getSaveBotChats());
                saveBotChats.setChecked(Settings.getSaveBotChats());
            }
        });
        layout.addView(saveBotChats);

        final TextCheckCell saveManuallyDeletedMessages = new TextCheckCell(getContext());
        setTextAndValueAndCheck(saveManuallyDeletedMessages, Localization.SAVE_SELF_DELETED_MESSAGES, Localization.ABOUT_SAVE_SELF_DELETED_MESSAGES, Settings.getSaveManuallyDeleted(), true, true);
        saveManuallyDeletedMessages.setOnClickListener(new View.OnClickListener() { 
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DeletedAndEditedMessagesFragment.lambda$showAdditionalDeleted$2(saveManuallyDeletedMessages, view);
            }
        });
        layout.addView(saveManuallyDeletedMessages);
        final TextCheckCell useExpandableBlockQuote = new TextCheckCell(getContext());
        setTextAndValueAndCheck(useExpandableBlockQuote, Localization.USE_COLLAPSED_BLOCKQUOTE, Localization.USE_COLLAPSED_BLOCKQUOTE_DESCRIPTION, Settings.getUseExpandableBlockQuote(), true, false);
        useExpandableBlockQuote.setOnClickListener(new View.OnClickListener() { 
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DeletedAndEditedMessagesFragment.lambda$showAdditionalDeleted$3(useExpandableBlockQuote, view);
            }
        });
        layout.addView(useExpandableBlockQuote);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(Localization.ADDITIONAL_SETTINGS);
        builder.setView(layout);
        builder.show();
    }

    private void setTextAndValueAndCheck(TextCheckCell cell, String text, String value, boolean checked, boolean multiline, boolean divider) {
        try {
            java.lang.reflect.Method m = TextCheckCell.class.getMethod("setTextAndValueAndCheck", CharSequence.class, CharSequence.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
            m.invoke(cell, text, value, checked, multiline, divider);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Method m = TextCheckCell.class.getMethod("setTextAndValueAndCheck", String.class, String.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
                m.invoke(cell, text, value, checked, multiline, divider);
            } catch (Throwable e2) {
                cell.setTextAndCheck(text, checked, divider);
            }
        }
    }

    static /* synthetic */ void lambda$showAdditionalDeleted$2(TextCheckCell saveManuallyDeletedMessages, View v1) {
        Settings.setSaveManuallyDeleted(!Settings.getSaveManuallyDeleted());
        saveManuallyDeletedMessages.setChecked(Settings.getSaveManuallyDeleted());
    }

    static /* synthetic */ void lambda$showAdditionalDeleted$3(TextCheckCell useExpandableBlockQuote, View v1) {
        Settings.setUseExpandableBlockQuote(!Settings.getUseExpandableBlockQuote());
        useExpandableBlockQuote.setChecked(Settings.getUseExpandableBlockQuote());
    }

}
