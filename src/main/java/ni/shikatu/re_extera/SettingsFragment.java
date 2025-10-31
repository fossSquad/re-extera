package ni.shikatu.re_extera;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EffectsTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.StickerImageView;

import java.lang.reflect.InvocationTargetException;

public class SettingsFragment extends BaseFragment {

    private CharSequence fullyFormatText(String text){
        try{
            Class<?> clazz = Class.forName("com.exteragram.messenger.utils.text.LocaleUtils");
            return (CharSequence) clazz.getMethod("fullyFormatText", CharSequence.class).invoke(null, text);
        } catch(Exception e){
            return text;
        }
	}

    @Override
    public View createView(Context context) {
        Localization.updateStrings();
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(Localization.RE_EXTERA_SETTINGS);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        ScrollView scrollView = new ScrollView(context);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout linearLayout = new LinearLayout(context);

        FrameLayout stickerContainer = new FrameLayout(context);

        LinearLayout stickerLayout = new LinearLayout(context);
        stickerLayout.setOrientation(LinearLayout.VERTICAL);

        StickerImageView stickerImageView = new StickerImageView(context, UserConfig.selectedAccount);
        stickerImageView.setStickerPackName("fuki_dum_pjsk_pack");
        stickerImageView.setStickerNum(3);
        stickerImageView.setAspectFit(true);

        stickerLayout.addView(stickerImageView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        EffectsTextView textView = new EffectsTextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setText("re:extera");
        textView.setTextSize(18);
        stickerLayout.addView(textView);

        EffectsTextView textView2 = new EffectsTextView(context);
        textView2.setGravity(Gravity.CENTER);
        textView2.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView2.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView2.setText(fullyFormatText(Localization.THANKS));
        textView2.setTextSize(12);
        stickerLayout.addView(textView2);

        stickerContainer.addView(stickerLayout, LayoutHelper.createFrame(200, 200, Gravity.CENTER));
        linearLayout.addView(stickerContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 200));

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.NO_GRAVITY));
        HeaderCell generalHeader = new HeaderCell(context);
        generalHeader.setText(Localization.GENERAL);
        linearLayout.addView(generalHeader);
        TextCheckCell hideOnlineCell = new TextCheckCell(context);
        hideOnlineCell.setTextAndCheck(Localization.HIDE_ONLINE_STATUS, Settings.getHideOnline(), false);
        hideOnlineCell.setOnClickListener(v -> {
            Settings.setHideOnline(!Settings.getHideOnline());
            hideOnlineCell.setChecked(Settings.getHideOnline());
        });
        linearLayout.addView(hideOnlineCell);

        TextCheckCell hideTypingCell = new TextCheckCell(context);
        hideTypingCell.setTextAndCheck(Localization.HIDE_TYPING_STATUS, Settings.getHideTyping(), false);
        hideTypingCell.setOnClickListener(v -> {
            Settings.setHideTyping(!Settings.getHideTyping());
            hideTypingCell.setChecked(Settings.getHideTyping());
        });
        linearLayout.addView(hideTypingCell);

        TextCheckCell hideReadingCell = new TextCheckCell(context);
        hideReadingCell.setTextAndCheck(Localization.HIDE_READING_MESSAGE, Settings.getHideReading(), false);
        hideReadingCell.setOnClickListener(v -> {
            Settings.setHideReading(!Settings.getHideReading());
            hideReadingCell.setChecked(Settings.getHideReading());
        });
        linearLayout.addView(hideReadingCell);

        TextCheckCell noReadStoriesCell = new TextCheckCell(context);
        noReadStoriesCell.setTextAndCheck(Localization.NO_READ_STORIES, Settings.getNoReadStories(), false);
        noReadStoriesCell.setOnClickListener(v -> {
            Settings.setNoReadStories(!Settings.getNoReadStories());
            noReadStoriesCell.setChecked(Settings.getNoReadStories());
        });
        linearLayout.addView(noReadStoriesCell);

        /*TextCheckCell enableHighRequireFeaturesCell = new TextCheckCell(context);
        enableHighRequireFeaturesCell.setTextAndCheck(Localization.ENABLE_ALPHA, Settings.getEnableAlpha(), false);
        enableHighRequireFeaturesCell.setOnClickListener(v -> {
            Settings.setEnableAlpha(!Settings.getEnableAlpha());
            enableHighRequireFeaturesCell.setChecked(Settings.getEnableAlpha());
            if(Settings.getEnableAlpha()){
                BulletinFactory.of(this).createSimpleBulletin(Localization.ALPHA_WARNING, "").show();
            }
        });
        linearLayout.addView(enableHighRequireFeaturesCell);*/
        TextCheckCell saveDeletedMessages = new TextCheckCell(context);
        saveDeletedMessages.setTextAndCheck(Localization.SAVE_DELETED_MESSAGES, Settings.getSaveDeletedMessages(), false);
        saveDeletedMessages.setOnClickListener(v -> {
            Settings.setSaveDeletedMessages(!Settings.getSaveDeletedMessages());
            saveDeletedMessages.setChecked(Settings.getSaveDeletedMessages());
        });
        linearLayout.addView(saveDeletedMessages);

        TextCheckCell saveEditedMessages = new TextCheckCell(context);
        saveEditedMessages.setTextAndCheck(Localization.MESSAGE_HISTORY_TOGGLE, Settings.getSaveEditedMessages(), false);
        saveEditedMessages.setOnClickListener(v -> {
            Settings.setSaveEditedMessages(!Settings.getSaveEditedMessages());
            saveEditedMessages.setChecked(Settings.getSaveEditedMessages());
        });
        linearLayout.addView(saveEditedMessages);

        TextCheckCell removeFlagSecure = new TextCheckCell(context);
        removeFlagSecure.setTextAndCheck(Localization.REMOVE_FLAG_SECURE, Settings.getRemoveFlagSecure(), false);
        removeFlagSecure.setOnClickListener(v -> {
            Settings.setRemoveFlagSecure(!Settings.getRemoveFlagSecure());
            removeFlagSecure.setChecked(Settings.getRemoveFlagSecure());
        });
        linearLayout.addView(removeFlagSecure);
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(Gravity.CENTER);
        EffectsTextView about = new EffectsTextView(context);
        about.setGravity(Gravity.CENTER);
        about.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        about.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        about.setText(fullyFormatText("**Version: 1.0.1**"));
        textLayout.addView(about);
        linearLayout.addView(textLayout);
        return fragmentView;
    }
}
