package ni.shikatu.re_extera;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import ni.shikatu.re_extera.chatmessagecell.MeasureTimeHook;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EffectsTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.StickerImageView;

public class SettingsFragment extends BaseFragment {
    public View createView(final Context context) {
        Localization.updateStrings();
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(true);
        this.actionBar.setTitle(Localization.RE_EXTERA_SETTINGS);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.SettingsFragment.1
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
        LinearLayout linearLayout = new LinearLayout(context);
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
        textView2.setText(Utils.fullyFormatText(Localization.THANKS));
        textView2.setTextSize(12.0f);
        stickerLayout.addView(textView2);
        stickerContainer.addView(stickerLayout, LayoutHelper.createFrame(200, 200, 17));
        linearLayout.addView(stickerContainer, LayoutHelper.createLinear(-1, 200));
        linearLayout.setOrientation(1);
        scrollView.addView(linearLayout, LayoutHelper.createScroll(-1, -2, 0));
        View ghostModeHeader = new HeaderCell(context);
        ghostModeHeader.setText(Localization.GHOST_MODE);
        linearLayout.addView(ghostModeHeader);
        final View hideOnlineCell = new TextCheckCell(context);
        hideOnlineCell.setTextAndCheck(Localization.HIDE_ONLINE_STATUS, Settings.getHideOnline(), false);
        hideOnlineCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$0(hideOnlineCell, view);
            }
        });
        linearLayout.addView(hideOnlineCell);
        final View hideTypingCell = new TextCheckCell(context);
        hideTypingCell.setTextAndCheck(Localization.HIDE_TYPING_STATUS, Settings.getHideTyping(), false);
        hideTypingCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$1(hideTypingCell, view);
            }
        });
        linearLayout.addView(hideTypingCell);
        final View hideReadingCell = new TextCheckCell(context);
        hideReadingCell.setTextAndCheck(Localization.HIDE_READING_MESSAGE, Settings.getHideReading(), false);
        hideReadingCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$2(hideReadingCell, view);
            }
        });
        linearLayout.addView(hideReadingCell);
        final View noReadStoriesCell = new TextCheckCell(context);
        noReadStoriesCell.setTextAndCheck(Localization.NO_READ_STORIES, Settings.getNoReadStories(), false);
        noReadStoriesCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$3(noReadStoriesCell, view);
            }
        });
        linearLayout.addView(noReadStoriesCell);
        View editedAndDeletedMessagesHeader = new HeaderCell(context);
        editedAndDeletedMessagesHeader.setText(Localization.DELETED_AND_EDITED_MESSAGES);
        linearLayout.addView(editedAndDeletedMessagesHeader);
        final View saveDeletedMessages = new TextCheckCell(context);
        saveDeletedMessages.setTextAndCheck(Localization.SAVE_DELETED_MESSAGES, Settings.getSaveDeletedMessages(), false);
        saveDeletedMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$4(saveDeletedMessages, view);
            }
        });
        linearLayout.addView(saveDeletedMessages);
        final View saveOneTimeMessages = new TextCheckCell(context);
        saveOneTimeMessages.setTextAndCheck(Localization.SAVE_ONE_TIME_MESSAGES, Settings.getSaveOneTimeMessages(), false);
        saveOneTimeMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$5(saveOneTimeMessages, view);
            }
        });
        linearLayout.addView(saveOneTimeMessages);
        final View redDeletedMark = new TextCheckCell(context);
        redDeletedMark.setTextAndCheck(Localization.RED_DELETED_MARK, Settings.getRedMark(), false);
        redDeletedMark.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$6(redDeletedMark, view);
            }
        });
        linearLayout.addView(redDeletedMark);
        final View saveEditedMessages = new TextCheckCell(context);
        saveEditedMessages.setTextAndCheck(Localization.MESSAGE_HISTORY_TOGGLE, Settings.getSaveEditedMessages(), false);
        saveEditedMessages.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$7(saveEditedMessages, view);
            }
        });
        linearLayout.addView(saveEditedMessages);
        View customPrefixHeader = new HeaderCell(context);
        customPrefixHeader.setText(Localization.CUSTOM_PREFIX);
        linearLayout.addView(customPrefixHeader);
        View customPrefix = new EditTextSettingsCell(context);
        customPrefix.setTextAndHint(Settings.getCustomPrefix(), Localization.LEAVE_BLANK_FOR_RECYCLE, false);
        customPrefix.getTextView().addTextChangedListener(new TextWatcher() { // from class: ni.shikatu.re_extera.SettingsFragment.2
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (SettingsFragment$2$$ExternalSyntheticBackport0.m(s.toString())) {
                    Settings.setCustomPrefix("");
                    MeasureTimeHook.notifyMarkChanged(Settings.getCustomPrefix());
                } else {
                    Settings.setCustomPrefix(s.toString());
                    MeasureTimeHook.notifyMarkChanged(Settings.getCustomPrefix());
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
        removeFlagSecure.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$8(removeFlagSecure, view);
            }
        });
        linearLayout.addView(removeFlagSecure);
        final View noForwardCell = new TextCheckCell(context);
        noForwardCell.setTextAndCheck(Localization.NO_FORWARD, Settings.noForward(), false);
        noForwardCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$9(noForwardCell, view);
            }
        });
        linearLayout.addView(noForwardCell);
        View clearDbCell = new TextCell(context);
        clearDbCell.setText(Localization.CLEAR_DB, false);
        clearDbCell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsFragment.lambda$createView$13(context, view);
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
        about.setText(Utils.fullyFormatText(String.format("**Version: %s**", Main.VERSION)));
        textLayout.addView(about);
        linearLayout.addView(textLayout);
        return this.fragmentView;
    }

    static /* synthetic */ void lambda$createView$0(TextCheckCell hideOnlineCell, View v) {
        Settings.setHideOnline(!Settings.getHideOnline());
        hideOnlineCell.setChecked(Settings.getHideOnline());
    }

    static /* synthetic */ void lambda$createView$1(TextCheckCell hideTypingCell, View v) {
        Settings.setHideTyping(!Settings.getHideTyping());
        hideTypingCell.setChecked(Settings.getHideTyping());
    }

    static /* synthetic */ void lambda$createView$2(TextCheckCell hideReadingCell, View v) {
        Settings.setHideReading(!Settings.getHideReading());
        hideReadingCell.setChecked(Settings.getHideReading());
    }

    static /* synthetic */ void lambda$createView$3(TextCheckCell noReadStoriesCell, View v) {
        Settings.setNoReadStories(!Settings.getNoReadStories());
        noReadStoriesCell.setChecked(Settings.getNoReadStories());
    }

    static /* synthetic */ void lambda$createView$4(TextCheckCell saveDeletedMessages, View v) {
        Settings.setSaveDeletedMessages(!Settings.getSaveDeletedMessages());
        saveDeletedMessages.setChecked(Settings.getSaveDeletedMessages());
    }

    static /* synthetic */ void lambda$createView$5(TextCheckCell saveOneTimeMessages, View v) {
        Settings.setSaveOneTimeMessages(!Settings.getSaveOneTimeMessages());
        saveOneTimeMessages.setChecked(Settings.getSaveOneTimeMessages());
    }

    static /* synthetic */ void lambda$createView$6(TextCheckCell redDeletedMark, View v) {
        Settings.setRedMark(!Settings.getRedMark());
        redDeletedMark.setChecked(Settings.getRedMark());
    }

    static /* synthetic */ void lambda$createView$7(TextCheckCell saveEditedMessages, View v) {
        Settings.setSaveEditedMessages(!Settings.getSaveEditedMessages());
        saveEditedMessages.setChecked(Settings.getSaveEditedMessages());
    }

    static /* synthetic */ void lambda$createView$8(TextCheckCell removeFlagSecure, View v) {
        Settings.setRemoveFlagSecure(!Settings.getRemoveFlagSecure());
        removeFlagSecure.setChecked(Settings.getRemoveFlagSecure());
    }

    static /* synthetic */ void lambda$createView$9(TextCheckCell noForwardCell, View v) {
        Settings.setNoForward(!Settings.noForward());
        noForwardCell.setChecked(Settings.noForward());
    }

    static /* synthetic */ void lambda$createView$13(final Context context, View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.CLEAR_DB + "?");
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda1
            public final void onClick(AlertDialog alertDialog, int i) {
                SettingsFragment.lambda$createView$11(context, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.NO, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda2
            public final void onClick(AlertDialog alertDialog, int i) {
                alertDialog.dismiss();
            }
        });
        builder.show();
    }

    static /* synthetic */ void lambda$createView$11(Context context, AlertDialog dialog, int which) {
        final AlertDialog progressDialog = new AlertDialog(context, 0);
        progressDialog.setTitle(Localization.CLEARING_NOW);
        progressDialog.setNegativeButton("", (AlertDialog.OnButtonClickListener) null);
        progressDialog.setCancelable(false);
        progressDialog.show();
        new Thread(new Runnable() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.lambda$createView$10(progressDialog);
            }
        }).start();
    }

    static /* synthetic */ void lambda$createView$10(final AlertDialog progressDialog) {
        DbDeletedStore.get().clearAll();
        progressDialog.getClass();
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.SettingsFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                progressDialog.dismiss();
            }
        });
    }
}
