package ni.shikatu.re_extera.hooks.usercell;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;

public class HideOwnOnlineStatus extends XC_MethodHook {
    private static final String[] TEXT_FIELD_NAMES = {"statusTextView"};

    @Override
    public void afterHookedMethod(MethodHookParam param) {
        if (!Settings.getHideOnlineWithGhost()) {
            return;
        }
        try {
            Object user = getField(param.thisObject, "currentObject");
            int account = getIntField(param.thisObject, "currentAccount");
            if (!(user instanceof TLRPC.User) || ((TLRPC.User) user).id != UserConfig.getInstance(account).getClientUserId()) {
                return;
            }
            Object textView = getTextView(param.thisObject);
            if (textView instanceof SimpleTextView) {
                SimpleTextView statusView = (SimpleTextView) textView;
                statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                statusView.setText("", true);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object getTextView(Object target) throws NoSuchFieldException, IllegalAccessException {
        for (String name : TEXT_FIELD_NAMES) {
            try {
                Object value = getField(target, name);
                if (value instanceof SimpleTextView) {
                    return value;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Object getField(Object target, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int getIntField(Object target, String name) throws NoSuchFieldException, IllegalAccessException {
        Object value = getField(target, name);
        return value instanceof Integer ? (Integer) value : UserConfig.selectedAccount;
    }
}
