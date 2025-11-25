package ni.shikatu.re_extera.settings;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.exteragram.messenger.utils.text.LocaleUtils;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.ui.ExclusionsFragment;
import ni.shikatu.re_extera.ui.RegexFiltersFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckCell2;
import org.telegram.ui.Components.EffectsTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.StickerImageView;

public class SettingsFragment extends BaseFragment {
    public View createView(final Context context) {
        Localization.updateStrings();
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(true);
        this.actionBar.setTitle(Localization.RE_EXTERA_SETTINGS);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.settings.SettingsFragment.1
            public void onItemClick(int id) {
                if (id == -1) {
                    SettingsFragment.this.finishFragment();
                }
            }
        });
        FrameLayout frameLayout = new FrameLayout(context);
        this.fragmentView = frameLayout;
        ScrollView scrollView = new ScrollView(context);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(-1, -1.0f));
        final LinearLayout linearLayout = new LinearLayout(context);
        FrameLayout stickerContainer = new FrameLayout(context);
        LinearLayout stickerLayout = new LinearLayout(context);
        stickerLayout.setOrientation(1);
        StickerImageView stickerImageView = new StickerImageView(context, UserConfig.selectedAccount);
        stickerImageView.setStickerPackName("fuki_dum_pjsk_pack");
        stickerImageView.setStickerNum(3);
        stickerImageView.setAspectFit(true);
        stickerLayout.addView((View) stickerImageView, (ViewGroup.LayoutParams) new LinearLayout.LayoutParams(-2, -2, 1.0f));
        EffectsTextView textView = new EffectsTextView(context);
        textView.setGravity(17);
        textView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setText("re:extera");
        textView.setTextSize(18.0f);
        stickerLayout.addView(textView);
        EffectsTextView textView2 = new EffectsTextView(context);
        textView2.setGravity(17);
        textView2.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView2.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView2.setText(LocaleUtils.fullyFormatText(Localization.THANKS));
        textView2.setTextSize(12.0f);
        stickerLayout.addView(textView2);
        stickerContainer.addView(stickerLayout, LayoutHelper.createFrame(200, 200, 17));
        linearLayout.addView(stickerContainer, LayoutHelper.createLinear(-1, 200));
        linearLayout.setOrientation(1);
        scrollView.addView(linearLayout, LayoutHelper.createScroll(-1, -2, 0));
        View ghostModeHeader = new HeaderCell(context);
        ghostModeHeader.setText(Localization.GHOST_MODE);
        linearLayout.addView(ghostModeHeader);
        final View checkBoxCell = new CheckBoxCell(context, 4, this.resourceProvider);
        checkBoxCell.getCheckBoxRound().setColor(Theme.key_switch2TrackChecked, Theme.key_radioBackground, Theme.key_checkboxCheck);
        checkBoxCell.setText(Localization.HIDE_ONLINE_STATUS, (String) null, true);
        checkBoxCell.setChecked(Settings.getHideOnline(), true);
        checkBoxCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$0(checkBoxCell, view);
            }
        });
        final View checkBoxCell2 = new CheckBoxCell(context, 4, this.resourceProvider);
        checkBoxCell2.getCheckBoxRound().setColor(Theme.key_switch2TrackChecked, Theme.key_radioBackground, Theme.key_checkboxCheck);
        checkBoxCell2.setText(Localization.IMMEDIATE_OFFLINE, (String) null, true);
        checkBoxCell2.setChecked(Settings.getImmediateOffline(), true);
        checkBoxCell2.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$1(checkBoxCell2, view);
            }
        });
        final View checkBoxCell3 = new CheckBoxCell(context, 4, this.resourceProvider);
        checkBoxCell3.getCheckBoxRound().setColor(Theme.key_switch2TrackChecked, Theme.key_radioBackground, Theme.key_checkboxCheck);
        checkBoxCell3.setText(Localization.HIDE_TYPING_STATUS, (String) null, true);
        checkBoxCell3.setChecked(Settings.getHideTyping(), true);
        checkBoxCell3.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$2(checkBoxCell3, view);
            }
        });
        final View checkBoxCell4 = new CheckBoxCell(context, 4, this.resourceProvider);
        checkBoxCell4.getCheckBoxRound().setColor(Theme.key_switch2TrackChecked, Theme.key_radioBackground, Theme.key_checkboxCheck);
        checkBoxCell4.setText(Localization.HIDE_READING_MESSAGE, (String) null, true);
        checkBoxCell4.setChecked(Settings.getHideReading(), true);
        checkBoxCell4.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$3(checkBoxCell4, view);
            }
        });
        final View checkBoxCell5 = new CheckBoxCell(context, 4, this.resourceProvider);
        checkBoxCell5.getCheckBoxRound().setColor(Theme.key_switch2TrackChecked, Theme.key_radioBackground, Theme.key_checkboxCheck);
        checkBoxCell5.setText(Localization.NO_READ_STORIES, (String) null, true);
        checkBoxCell5.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$4(checkBoxCell5, view);
            }
        });
        final LinearLayout ghostContainer = new LinearLayout(context);
        ghostContainer.setOrientation(1);
        ghostContainer.setVisibility(8);
        ghostContainer.setPadding(AndroidUtilities.dp(20.0f), 0, 0, 0);
        ghostContainer.addView(checkBoxCell);
        ghostContainer.addView(checkBoxCell2);
        ghostContainer.addView(checkBoxCell3);
        ghostContainer.addView(checkBoxCell4);
        ghostContainer.addView(checkBoxCell5);
        final View globalGhostMode = new TextCheckCell2(context);
        globalGhostMode.setTextAndCheck(Localization.GHOST_MODE, Settings.getGhostModeEnabledGlobal(), false);
        globalGhostMode.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$5(ghostContainer, linearLayout, globalGhostMode, view);
            }
        });
        globalGhostMode.setCollapseArrow("", true, new Runnable() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.lambda$createView$6(globalGhostMode);
            }
        });
        linearLayout.addView(globalGhostMode);
        linearLayout.addView(ghostContainer);
        final View useSchedule = new TextCheckCell(context);
        useSchedule.setTextAndCheck(Localization.USE_SCHEDULE, Settings.getUseSchedule(), false);
        useSchedule.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$7(useSchedule, view);
            }
        });
        linearLayout.addView(useSchedule);
        View seeExclusions = new TextCell(context);
        seeExclusions.setText(Localization.EXCLUSIONS, false);
        seeExclusions.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$createView$8(view);
            }
        });
        linearLayout.addView(seeExclusions);
        View editedAndDeletedMessagesHeader = new HeaderCell(context);
        editedAndDeletedMessagesHeader.setText(Localization.DELETED_AND_EDITED_MESSAGES);
        linearLayout.addView(editedAndDeletedMessagesHeader);
        final View saveDeletedMessages = new TextCheckCell(context);
        saveDeletedMessages.setTextAndValueAndCheck(Localization.SAVE_DELETED_MESSAGES, Localization.HOLD_FOR_ADDITIONAL_SETTINGS, Settings.getSaveDeletedMessages(), true, false);
        saveDeletedMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$9(saveDeletedMessages, view);
            }
        });
        linearLayout.addView(saveDeletedMessages);
        saveDeletedMessages.setOnLongClickListener(new View.OnLongClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(View view) {
                return SettingsFragment.lambda$createView$12(context, view);
            }
        });
        final View saveOneTimeMessages = new TextCheckCell(context);
        saveOneTimeMessages.setTextAndCheck(Localization.SAVE_ONE_TIME_MESSAGES, Settings.getSaveOneTimeMessages(), false);
        saveOneTimeMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$13(saveOneTimeMessages, view);
            }
        });
        linearLayout.addView(saveOneTimeMessages);
        final View redDeletedMark = new TextCheckCell(context);
        redDeletedMark.setTextAndCheck(Localization.RED_DELETED_MARK, Settings.getRedMark(), false);
        redDeletedMark.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$14(redDeletedMark, view);
            }
        });
        linearLayout.addView(redDeletedMark);
        final View saveEditedMessages = new TextCheckCell(context);
        saveEditedMessages.setTextAndCheck(Localization.MESSAGE_HISTORY_TOGGLE, Settings.getSaveEditedMessages(), false);
        saveEditedMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$15(saveEditedMessages, view);
            }
        });
        linearLayout.addView(saveEditedMessages);
        View customPrefixHeader = new HeaderCell(context);
        customPrefixHeader.setText(Localization.CUSTOM_PREFIX);
        linearLayout.addView(customPrefixHeader);
        View customPrefix = new EditTextSettingsCell(context);
        customPrefix.setTextAndHint(Settings.getCustomPrefix(), Localization.LEAVE_BLANK_FOR_RECYCLE, false);
        customPrefix.getTextView().addTextChangedListener(new TextWatcher() { // from class: ni.shikatu.re_extera.settings.SettingsFragment.2
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
        linearLayout.addView(customPrefix);
        View otherHeader = new HeaderCell(context);
        otherHeader.setText(Localization.OTHER);
        linearLayout.addView(otherHeader);
        final View removeFlagSecure = new TextCheckCell(context);
        removeFlagSecure.setTextAndCheck(Localization.REMOVE_FLAG_SECURE, Settings.getRemoveFlagSecure(), false);
        removeFlagSecure.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$16(removeFlagSecure, view);
            }
        });
        linearLayout.addView(removeFlagSecure);
        final View noForwardCell = new TextCheckCell(context);
        noForwardCell.setTextAndCheck(Localization.NO_FORWARD, Settings.noForward(), false);
        noForwardCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$17(noForwardCell, view);
            }
        });
        linearLayout.addView(noForwardCell);
        View filtersCell = new TextCell(context);
        filtersCell.setText(Localization.FILTERS, false);
        filtersCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$createView$18(view);
            }
        });
        linearLayout.addView(filtersCell);
        View clearDbCell = new TextCell(context);
        clearDbCell.setText(Localization.CLEAR_DB, false);
        clearDbCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$22(context, view);
            }
        });
        linearLayout.addView(clearDbCell);
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(1);
        textLayout.setGravity(17);
        EffectsTextView about = new EffectsTextView(context);
        about.setGravity(17);
        about.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        about.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        about.setText(LocaleUtils.fullyFormatText(String.format("**Version: %s**", Main.VERSION)));
        textLayout.addView(about);
        linearLayout.addView(textLayout);
        return this.fragmentView;
    }

    static /* synthetic */ void lambda$createView$0(CheckBoxCell hideOnlineCell, View v) {
        Settings.setHideOnline(!Settings.getHideOnline());
        hideOnlineCell.setChecked(Settings.getHideOnline(), true);
    }

    static /* synthetic */ void lambda$createView$1(CheckBoxCell immediateOfflineCell, View v) {
        Settings.setImmediateOffline(!Settings.getImmediateOffline());
        immediateOfflineCell.setChecked(Settings.getImmediateOffline(), true);
    }

    static /* synthetic */ void lambda$createView$2(CheckBoxCell hideTypingCell, View v) {
        Settings.setHideTyping(!Settings.getHideTyping());
        hideTypingCell.setChecked(Settings.getHideTyping(), true);
    }

    static /* synthetic */ void lambda$createView$3(CheckBoxCell hideReadingCell, View v) {
        Settings.setHideReading(!Settings.getHideReading());
        hideReadingCell.setChecked(Settings.getHideReading(), true);
    }

    static /* synthetic */ void lambda$createView$4(CheckBoxCell noReadStoriesCell, View v) {
        Settings.setNoReadStories(!Settings.getNoReadStories());
        noReadStoriesCell.setChecked(Settings.getNoReadStories(), true);
    }

    static /* synthetic */ void lambda$createView$5(LinearLayout ghostContainer, LinearLayout linearLayout, TextCheckCell2 globalGhostMode, View v) {
        boolean isCollapsed = ghostContainer.getVisibility() == 8;
        AutoTransition transition = new AutoTransition();
        transition.setDuration(200L);
        TransitionManager.beginDelayedTransition(linearLayout, transition);
        ghostContainer.setVisibility(isCollapsed ? 0 : 8);
        globalGhostMode.setCollapsed(isCollapsed ? false : true);
    }

    static /* synthetic */ void lambda$createView$6(TextCheckCell2 globalGhostMode) {
        boolean newValue = !Settings.getGhostModeEnabledGlobal();
        Settings.setGhostModeEnabledGlobal(newValue);
        globalGhostMode.setChecked(newValue);
    }

    static /* synthetic */ void lambda$createView$7(TextCheckCell useSchedule, View v) {
        Settings.setUseSchedule(!Settings.getUseSchedule());
        useSchedule.setChecked(Settings.getUseSchedule());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createView$8(View v) {
        presentFragment(new ExclusionsFragment());
    }

    static /* synthetic */ void lambda$createView$9(TextCheckCell saveDeletedMessages, View v) {
        Settings.setSaveDeletedMessages(!Settings.getSaveDeletedMessages());
        saveDeletedMessages.setChecked(Settings.getSaveDeletedMessages());
    }

    static /* synthetic */ boolean lambda$createView$12(Context context, View v) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(1);
        final TextCheckCell saveManuallyDeletedMessages = new TextCheckCell(context);
        saveManuallyDeletedMessages.setTextAndValueAndCheck(Localization.SAVE_SELF_DELETED_MESSAGES, Localization.ABOUT_SAVE_SELF_DELETED_MESSAGES, Settings.getSaveManuallyDeleted(), true, false);
        saveManuallyDeletedMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$10(saveManuallyDeletedMessages, view);
            }
        });
        layout.addView(saveManuallyDeletedMessages);
        final TextCheckCell useExpandableBlockQuote = new TextCheckCell(context);
        useExpandableBlockQuote.setTextAndValueAndCheck(Localization.USE_COLLAPSED_BLOCKQUOTE, Localization.USE_COLLAPSED_BLOCKQUOTE_DESCRIPTION, Settings.getUseExpandableBlockQuote(), true, false);
        useExpandableBlockQuote.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$11(useExpandableBlockQuote, view);
            }
        });
        layout.addView(useExpandableBlockQuote);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.ADDITIONAL_SETTINGS);
        builder.setView(layout);
        builder.show();
        return true;
    }

    static /* synthetic */ void lambda$createView$10(TextCheckCell saveManuallyDeletedMessages, View v1) {
        Settings.setSaveManuallyDeleted(!Settings.getSaveManuallyDeleted());
        saveManuallyDeletedMessages.setChecked(Settings.getSaveManuallyDeleted());
    }

    static /* synthetic */ void lambda$createView$11(TextCheckCell useExpandableBlockQuote, View v1) {
        Settings.setUseExpandableBlockQuote(!Settings.getUseExpandableBlockQuote());
        useExpandableBlockQuote.setChecked(Settings.getUseExpandableBlockQuote());
    }

    static /* synthetic */ void lambda$createView$13(TextCheckCell saveOneTimeMessages, View v) {
        Settings.setSaveOneTimeMessages(!Settings.getSaveOneTimeMessages());
        saveOneTimeMessages.setChecked(Settings.getSaveOneTimeMessages());
    }

    static /* synthetic */ void lambda$createView$14(TextCheckCell redDeletedMark, View v) {
        Settings.setRedMark(!Settings.getRedMark());
        redDeletedMark.setChecked(Settings.getRedMark());
    }

    static /* synthetic */ void lambda$createView$15(TextCheckCell saveEditedMessages, View v) {
        Settings.setSaveEditedMessages(!Settings.getSaveEditedMessages());
        saveEditedMessages.setChecked(Settings.getSaveEditedMessages());
    }

    static /* synthetic */ void lambda$createView$16(TextCheckCell removeFlagSecure, View v) {
        Settings.setRemoveFlagSecure(!Settings.getRemoveFlagSecure());
        removeFlagSecure.setChecked(Settings.getRemoveFlagSecure());
    }

    static /* synthetic */ void lambda$createView$17(TextCheckCell noForwardCell, View v) {
        Settings.setNoForward(!Settings.noForward());
        noForwardCell.setChecked(Settings.noForward());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createView$18(View v) {
        presentFragment(new RegexFiltersFragment());
    }

    static /* synthetic */ void lambda$createView$22(final Context context, View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.CLEAR_DB + "?");
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda22
            public final void onClick(AlertDialog alertDialog, int i) {
                SettingsFragment.lambda$createView$20(context, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.NO, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda23
            public final void onClick(AlertDialog alertDialog, int i) {
                alertDialog.dismiss();
            }
        });
        builder.show();
    }

    static /* synthetic */ void lambda$createView$20(Context context, AlertDialog dialog, int which) {
        final AlertDialog progressDialog = new AlertDialog(context, 0);
        progressDialog.setTitle(Localization.CLEARING_NOW);
        progressDialog.setNegativeButton("", (AlertDialog.OnButtonClickListener) null);
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread(new Runnable() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.lambda$createView$19(progressDialog);
            }
        }).start();
    }

    static /* synthetic */ void lambda$createView$19(final AlertDialog progressDialog) {
        ReExteraDb.get().clearDatabaseWithInternal();
        progressDialog.getClass();
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.settings.SettingsFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                progressDialog.dismiss();
            }
        });
    }
}
