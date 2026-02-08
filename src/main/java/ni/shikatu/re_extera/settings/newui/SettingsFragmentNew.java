package ni.shikatu.re_extera.settings.newui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import com.exteragram.messenger.utils.text.LocaleUtils;
import java.util.ArrayList;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.DrawableUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EffectsTextView;
import org.telegram.ui.Components.StickerImageView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class SettingsFragmentNew extends BasePreferencesActivityExtended {
    private static Drawable ADDITIONAL_ICON = null;
    private static Drawable DELETED_ICON = null;
    private static Drawable GHOST_ICON = null;
    private static final int iconSize = 28;
    private boolean isGhostExpanded;

    public enum IDs {
        STICKER_ID,
        THANKS_ID,
        GHOST_MODE_BTN_ID,
        DELETED_AND_EDITED_MESSAGES_BTN_ID,
        ADDITIONAL_BTN_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    public View createView(Context context) {
        GHOST_ICON = DrawableUtils.resize(context.getResources(), ContextCompat.getDrawable(context, R.drawable.ghost), AndroidUtilities.dp(28.0f), AndroidUtilities.dp(28.0f));
        DELETED_ICON = DrawableUtils.resize(context.getResources(), ContextCompat.getDrawable(context, R.drawable.menu_hide_gift), AndroidUtilities.dp(28.0f), AndroidUtilities.dp(28.0f));
        ADDITIONAL_ICON = DrawableUtils.resize(context.getResources(), ContextCompat.getDrawable(context, R.drawable.msg_list), AndroidUtilities.dp(28.0f), AndroidUtilities.dp(28.0f));
        return super.createView(context);
    }

    public String getTitle() {
        return "re:extera";
    }

    private FrameLayout createStickerView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        StickerImageView stickerImageView = new StickerImageView(getContext(), UserConfig.selectedAccount);
        stickerImageView.setStickerPackName("fuki_dum_pjsk_pack");
        stickerImageView.setStickerNum(3);
        stickerImageView.setAspectFit(true);
        frameLayout.addView((View) stickerImageView, (ViewGroup.LayoutParams) new FrameLayout.LayoutParams(AndroidUtilities.dp(130.0f), AndroidUtilities.dp(130.0f), 17));
        frameLayout.setMinimumHeight(AndroidUtilities.dp(150.0f));
        return frameLayout;
    }

    private FrameLayout createThanksView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(1);
        EffectsTextView textView1 = new EffectsTextView(getContext());
        textView1.setGravity(17);
        textView1.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView1.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView1.setText("re:extera");
        textView1.setTextSize(18.0f);
        textLayout.addView(textView1);
        EffectsTextView textView2 = new EffectsTextView(getContext());
        textView2.setGravity(17);
        textView2.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView2.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView2.setText(LocaleUtils.fullyFormatText(Localization.THANKS));
        textView2.setTextSize(12.0f);
        textLayout.addView(textView2);
        frameLayout.addView(textLayout);
        frameLayout.setPadding(-1, -1, -1, AndroidUtilities.dp(8.0f));
        return frameLayout;
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.clear();
        items.add(UItem.asCustom(IDs.STICKER_ID.getId(), createStickerView()).setTransparent(true));
        items.add(UItem.asCustom(IDs.THANKS_ID.getId(), createThanksView()).setTransparent(true));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.GHOST_MODE_BTN_ID.getId(), GHOST_ICON, Localization.GHOST_MODE));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.DELETED_AND_EDITED_MESSAGES_BTN_ID.getId(), DELETED_ICON, Localization.SPY));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.ADDITIONAL_BTN_ID.getId(), ADDITIONAL_ICON, Localization.OTHER));
        items.add(UItem.asShadow());
        items.add(UItem.asShadow(LocaleUtils.fullyFormatText(String.format("**Version: %s**", Main.VERSION))));
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > IDs.values().length) {
            return;
        }
        IDs clicked = IDs.values()[item.id - 1];
        switch (clicked.ordinal()) {
            case 2:
                presentFragment(new GhostFragment());
                break;
            case 3:
                presentFragment(new DeletedAndEditedMessagesFragment());
                break;
            case 4:
                presentFragment(new AdditionalFragment());
                break;
        }
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
