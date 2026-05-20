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
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.DrawableUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EffectsTextView;
import org.telegram.ui.Components.StickerImageView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class SettingsFragmentNew extends BasePreferencesActivityExtended {
    private static final int ICON_SIZE = 28;
    private Drawable additionalIcon;
    private Drawable deletedIcon;
    private Drawable ghostIcon;

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
        int sizeDp = AndroidUtilities.dp(28.0f);
        this.ghostIcon = DrawableUtils.resize(context.getResources(), ContextCompat.getDrawable(context, R.drawable.ghost), sizeDp, sizeDp);
        this.deletedIcon = DrawableUtils.resize(context.getResources(), ContextCompat.getDrawable(context, R.drawable.menu_hide_gift), sizeDp, sizeDp);
        this.additionalIcon = DrawableUtils.resize(context.getResources(), ContextCompat.getDrawable(context, R.drawable.msg_list), sizeDp, sizeDp);
        return super.createView(context);
    }

    public String getTitle() {
        return "re:extera";
    }

    private FrameLayout createStickerView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        StickerImageView stickerImageView = new StickerImageView(getContext(), getCurrentAccount());
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
        EffectsTextView title = new EffectsTextView(getContext());
        title.setGravity(17);
        title.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setText("re:extera");
        title.setTextSize(18.0f);
        textLayout.addView(title);
        EffectsTextView thanks = new EffectsTextView(getContext());
        thanks.setGravity(17);
        thanks.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        thanks.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        thanks.setText(LocaleUtils.fullyFormatText(Localization.THANKS));
        thanks.setTextSize(12.0f);
        textLayout.addView(thanks);
        frameLayout.addView(textLayout);
        frameLayout.setPadding(-1, -1, -1, AndroidUtilities.dp(8.0f));
        return frameLayout;
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.clear();
        items.add(UItem.asCustom(IDs.STICKER_ID.getId(), createStickerView()).setTransparent(true));
        items.add(UItem.asCustom(IDs.THANKS_ID.getId(), createThanksView()).setTransparent(true));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.GHOST_MODE_BTN_ID.getId(), this.ghostIcon, Localization.GHOST_MODE));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.DELETED_AND_EDITED_MESSAGES_BTN_ID.getId(), this.deletedIcon, Localization.SPY));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.ADDITIONAL_BTN_ID.getId(), this.additionalIcon, Localization.OTHER));
        items.add(UItem.asShadow());
        items.add(UItem.asShadow(LocaleUtils.fullyFormatText(String.format("**Version: %s**", Main.VERSION))));
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.settings.newui.SettingsFragmentNew$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$ni$shikatu$re_extera$settings$newui$SettingsFragmentNew$IDs = new int[IDs.values().length];

        static {
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$SettingsFragmentNew$IDs[IDs.GHOST_MODE_BTN_ID.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$SettingsFragmentNew$IDs[IDs.DELETED_AND_EDITED_MESSAGES_BTN_ID.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$SettingsFragmentNew$IDs[IDs.ADDITIONAL_BTN_ID.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > IDs.values().length) {
            return;
        }
        switch (AnonymousClass1.$SwitchMap$ni$shikatu$re_extera$settings$newui$SettingsFragmentNew$IDs[IDs.values()[item.id - 1].ordinal()]) {
            case Defaults.ALWAYS /* 1 */:
                presentFragment(new GhostFragment());
                break;
            case 2:
                presentFragment(new DeletedAndEditedMessagesFragment());
                break;
            case 3:
                presentFragment(new AdditionalFragment());
                break;
        }
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
