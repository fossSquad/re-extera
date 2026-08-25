package ni.shikatu.re_extera.hooks.profileactivity;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.ProfileActivity;

/**
 * Adds an "ID: &lt;n&gt;" row to the profile/channel/group overflow (3-dot) menu
 * that copies the peer id to the clipboard on tap.
 *
 * <p>Hooks {@code ProfileActivity.createActionBarMenu(boolean)} (after) and appends
 * a sub-item to the existing {@code otherItem} menu, resolving the numeric id from
 * the activity's {@code userId} (users) or {@code chatId} (channels/groups).
 */
public class ProfileMenuShowId extends XC_MethodHook {
    private static final int SUBITEM_ID = 910050;
    private static final Field OTHER_ITEM_FIELD = field("otherItem");
    private static final Field USER_ID_FIELD = field("userId");
    private static final Field CHAT_ID_FIELD = field("chatId");

    private static Field field(String name) {
        try {
            Field f = ProfileActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            Main.log("ProfileMenuShowId: field '%s' not found: %s", name, e.getMessage());
            return null;
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getShowIdInMenu() || OTHER_ITEM_FIELD == null) {
            return;
        }
        try {
            final ProfileActivity activity = (ProfileActivity) param.thisObject;
            final ActionBarMenuItem otherItem = (ActionBarMenuItem) ReflectionUtils.get(OTHER_ITEM_FIELD, activity);
            if (otherItem == null) {
                return;
            }
            long id = 0L;
            if (USER_ID_FIELD != null) {
                Long userId = (Long) ReflectionUtils.get(USER_ID_FIELD, activity);
                if (userId != null && userId.longValue() != 0L) {
                    id = userId.longValue();
                }
            }
            if (id == 0L && CHAT_ID_FIELD != null) {
                Long chatId = (Long) ReflectionUtils.get(CHAT_ID_FIELD, activity);
                if (chatId != null && chatId.longValue() != 0L) {
                    id = chatId.longValue();
                }
            }
            if (id == 0L) {
                return;
            }
            final long finalId = id;
            ActionBarMenuSubItem subItem = otherItem.addSubItem(SUBITEM_ID, R.drawable.msg_info, "ID: " + finalId);
            subItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    otherItem.closeSubMenu();
                    AndroidUtilities.addToClipboard(String.valueOf(finalId));
                    try {
                        BulletinFactory.of(activity).createSuccessBulletin(Localization.ID_COPIED).show();
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Exception e) {
            Main.log("ProfileMenuShowId: %s", e.getMessage());
        }
    }
}
