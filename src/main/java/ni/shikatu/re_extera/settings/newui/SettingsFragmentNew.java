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

    // User-supplied SVG path data (viewBox 16 for ghost, 507.965 for spy).
    private static final String GHOST_ICON_PATH =
            "M2 6C2 2.68629 4.68629 0 8 0C11.3137 0 14 2.68629 14 6V16H12L10 14L8 16L6 14L4 16H2V6ZM7 6C7 6.55228 6.55228 7 6 7C5.44772 7 5 6.55228 5 6C5 5.44772 5.44772 5 6 5C6.55228 5 7 5.44772 7 6ZM10 7C10.5523 7 11 6.55228 11 6C11 5.44772 10.5523 5 10 5C9.44772 5 9 5.44772 9 6C9 6.55228 9.44772 7 10 7Z";
    private static final String SPY_ICON_PATH_1 =
            "M507.083,238.166c-2.7-7.3-10.8-11-18.1-8.3c-20,7.4-40.4,13.7-61,19.1l-35.7-172.3c-8-40.8-50.9-67.6-93.8-52.7"
            + "c-28.7,10.3-60.3,10.3-89,0c-40.6-13.9-84,8.8-93.9,52.7l-35.6,172.3c-20.7-5.4-41-11.7-61-19.1c-7.3-2.7-15.4,1-18.1,8.3"
            + "c-2.7,7.3,1,15.4,8.3,18.1c160.5,57.6,328.9,57.8,489.6,0C506.083,253.566,509.783,245.466,507.083,238.166z M107.483,255.566"
            + "l13.1-63.2h35.6c7.8,0,14.1-6.3,14.1-14.1c0-7.8-6.3-14.1-14.1-14.1h-29.7l5.8-28.2h59.2c7.8,0,14.1-6.3,14.1-14.1"
            + "s-6.3-14.1-14.1-14.1h-53.3l5.2-25.3c4.4-22.1,29-41.2,56.7-31.8c34.8,12.5,73.2,12.5,108,0c29.3-9.5,52.5,10.7,56.7,31.8"
            + "l35.8,173.1C304.483,276.466,203.483,276.466,107.483,255.566z";
    private static final String SPY_ICON_PATH_2 =
            "M450.183,399.566c-8.2-34.8-46.9-61.3-93.3-61.3c-44.3,0-81.4,24-91.9,56.4c-7.7-1.8-14.1-1.8-21.8,0"
            + "c-10.5-32.4-47.7-56.5-92-56.5c-46.5,0-85.2,26.5-93.3,61.3c-6.2,1.5-10.9,7-10.9,13.7s4.7,12.2,10.9,13.7"
            + "c8.2,34.8,46.9,61.3,93.3,61.3c48,0,87.7-28.3,94-64.8c7-2.4,10.7-2.5,17.6-0.1c6.2,36.6,46,64.9,94.1,64.9"
            + "c46.5,0,85.1-26.5,93.3-61.3c6.2-1.5,10.9-7,10.9-13.7C461.083,406.666,456.383,401.066,450.183,399.566z M151.183,460.066"
            + "c-36.2,0-66.8-21.4-66.8-46.7c0-25.3,30.6-46.8,66.8-46.8s66.8,21.4,66.8,46.8C217.983,438.766,187.383,460.066,151.183,460.066z"
            + "M356.783,460.066c-36.2,0-66.8-21.4-66.8-46.7c0-25.3,30.6-46.8,66.8-46.8c36.2,0,66.8,21.4,66.8,46.8"
            + "C423.583,438.766,392.983,460.066,356.783,460.066z";
    private Drawable additionalIcon;
    private Drawable deletedIcon;
    private Drawable ghostIcon;
    private Drawable customizationIcon;

    public enum IDs {
        STICKER_ID,
        THANKS_ID,
        GHOST_MODE_BTN_ID,
        DELETED_AND_EDITED_MESSAGES_BTN_ID,
        CUSTOMIZATION_BTN_ID,
        ADDITIONAL_BTN_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    public View createView(Context context) {
        int sizeDp = AndroidUtilities.dp(28.0f);
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon);
        // Custom user-supplied icons, rendered from SVG path data (no resources in a DEX).
        this.ghostIcon = new ni.shikatu.re_extera.utils.PathIconDrawable(16f, 16f, true, 28, GHOST_ICON_PATH).withTint(accent);
        this.deletedIcon = new ni.shikatu.re_extera.utils.PathIconDrawable(507.965f, 507.965f, false, 28, SPY_ICON_PATH_1, SPY_ICON_PATH_2).withTint(accent);
        this.customizationIcon = DrawableUtils.resize(context.getResources(), ContextCompat.getDrawable(context, R.drawable.msg_theme), sizeDp, sizeDp);
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

    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.clear();
        items.add(UItem.asCustom(IDs.STICKER_ID.getId(), createStickerView()).setTransparent(true));
        items.add(UItem.asCustom(IDs.THANKS_ID.getId(), createThanksView()).setTransparent(true));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.GHOST_MODE_BTN_ID.getId(), this.ghostIcon, Localization.GHOST_MODE));
        items.add(UItem.asShadow());
        items.add(UItem.asButton(IDs.DELETED_AND_EDITED_MESSAGES_BTN_ID.getId(), this.deletedIcon, Localization.SPY));
        items.add(UItem.asShadow());
        // Customization moved into Spy → "Deleted Message" (it only holds deleted-message settings).
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
            try {
                $SwitchMap$ni$shikatu$re_extera$settings$newui$SettingsFragmentNew$IDs[IDs.CUSTOMIZATION_BTN_ID.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public void onClick(UItem item, View view, int position, float x, float y) {
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
            case 4:
                presentFragment(new CustomizationFragment());
                break;
        }
    }

    public boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
