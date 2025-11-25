package ni.shikatu.re_extera.settings.newui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.settings.SettingsFragment$2$$ExternalSyntheticBackport0;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class DeletedAndEditedMessagesFragment extends BasePreferencesActivityExtended {

    private enum DeletedAndEditedIds {
        SAVE_DELETED_MESSAGES_ID,
        SAVE_ONE_TIME_MESSAGES_ID,
        SAVE_MESSAGE_HISTORY_ID,
        RED_DELETED_MARK_ID,
        CUSTOM_DELETED_MARK_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    public String getTitle() {
        return Localization.SPY;
    }

    private FrameLayout customMarkView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        EditTextSettingsCell customPrefix = new EditTextSettingsCell(getContext());
        customPrefix.setTextAndHint(Settings.getCustomPrefix(), Localization.LEAVE_BLANK_FOR_RECYCLE, false);
        customPrefix.getTextView().addTextChangedListener(new TextWatcher() { // from class: ni.shikatu.re_extera.settings.newui.DeletedAndEditedMessagesFragment.1
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (SettingsFragment$2$$ExternalSyntheticBackport0.m(s.toString())) {
                    Settings.setCustomPrefix("");
                    MeasureTime.notifyMarkChanged(Settings.getCustomPrefix());
                } else {
                    Settings.setCustomPrefix(s.toString());
                    MeasureTime.notifyMarkChanged(Settings.getCustomPrefix());
                }
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        frameLayout.addView(customPrefix);
        return frameLayout;
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(DeletedAndEditedIds.SAVE_DELETED_MESSAGES_ID.getId(), Localization.SAVE_DELETED_MESSAGES, Localization.HOLD_FOR_ADDITIONAL_SETTINGS, true).setChecked(Settings.getSaveDeletedMessages()));
        items.add(UItem.asCheck(DeletedAndEditedIds.SAVE_ONE_TIME_MESSAGES_ID.getId(), Localization.SAVE_ONE_TIME_MESSAGES).setChecked(Settings.getSaveOneTimeMessages()));
        items.add(UItem.asCheck(DeletedAndEditedIds.SAVE_MESSAGE_HISTORY_ID.getId(), Localization.MESSAGE_HISTORY_TOGGLE).setChecked(Settings.getSaveEditedMessages()));
        items.add(UItem.asShadow());
        items.add(UItem.asCheck(DeletedAndEditedIds.RED_DELETED_MARK_ID.getId(), Localization.RED_DELETED_MARK).setChecked(Settings.getRedMark()));
        items.add(UItem.asHeader(Localization.CUSTOM_PREFIX));
        items.add(UItem.asCustom(DeletedAndEditedIds.CUSTOM_DELETED_MARK_ID.getId(), customMarkView()));
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > DeletedAndEditedIds.values().length) {
            return;
        }
        DeletedAndEditedIds clicked = DeletedAndEditedIds.values()[item.id - 1];
        switch (clicked.ordinal()) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                Settings.setSaveDeletedMessages(!Settings.getSaveDeletedMessages());
                refreshCheckBox(item, position, Settings.getSaveDeletedMessages());
                break;
            case Defaults.ALWAYS /* 1 */:
                Settings.setSaveOneTimeMessages(!Settings.getSaveOneTimeMessages());
                refreshCheckBox(item, position, Settings.getSaveOneTimeMessages());
                break;
            case 2:
                Settings.setSaveEditedMessages(!Settings.getSaveEditedMessages());
                refreshCheckBox(item, position, Settings.getSaveEditedMessages());
                break;
            case 3:
                Settings.setRedMark(!Settings.getRedMark());
                refreshCheckBox(item, position, Settings.getRedMark());
                break;
        }
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > DeletedAndEditedIds.values().length) {
            return false;
        }
        DeletedAndEditedIds clicked = DeletedAndEditedIds.values()[item.id - 1];
        switch (clicked.ordinal()) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                showAdditionalDeleted();
                return true;
            default:
                return false;
        }
    }

    private void showAdditionalDeleted() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(1);
        final TextCheckCell saveManuallyDeletedMessages = new TextCheckCell(getContext());
        saveManuallyDeletedMessages.setTextAndValueAndCheck(Localization.SAVE_SELF_DELETED_MESSAGES, Localization.ABOUT_SAVE_SELF_DELETED_MESSAGES, Settings.getSaveManuallyDeleted(), true, false);
        saveManuallyDeletedMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.DeletedAndEditedMessagesFragment$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DeletedAndEditedMessagesFragment.lambda$showAdditionalDeleted$0(saveManuallyDeletedMessages, view);
            }
        });
        layout.addView(saveManuallyDeletedMessages);
        final TextCheckCell useExpandableBlockQuote = new TextCheckCell(getContext());
        useExpandableBlockQuote.setTextAndValueAndCheck(Localization.USE_COLLAPSED_BLOCKQUOTE, Localization.USE_COLLAPSED_BLOCKQUOTE_DESCRIPTION, Settings.getUseExpandableBlockQuote(), true, false);
        useExpandableBlockQuote.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.newui.DeletedAndEditedMessagesFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DeletedAndEditedMessagesFragment.lambda$showAdditionalDeleted$1(useExpandableBlockQuote, view);
            }
        });
        layout.addView(useExpandableBlockQuote);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(Localization.ADDITIONAL_SETTINGS);
        builder.setView(layout);
        builder.show();
    }

    static /* synthetic */ void lambda$showAdditionalDeleted$0(TextCheckCell saveManuallyDeletedMessages, View v1) {
        Settings.setSaveManuallyDeleted(!Settings.getSaveManuallyDeleted());
        saveManuallyDeletedMessages.setChecked(Settings.getSaveManuallyDeleted());
    }

    static /* synthetic */ void lambda$showAdditionalDeleted$1(TextCheckCell useExpandableBlockQuote, View v1) {
        Settings.setUseExpandableBlockQuote(!Settings.getUseExpandableBlockQuote());
        useExpandableBlockQuote.setChecked(Settings.getUseExpandableBlockQuote());
    }
}
