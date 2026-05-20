package ni.shikatu.re_extera.utils;

import java.lang.reflect.Field;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;

public final class AccountUtils {
    private static final Field CURRENT_ACCOUNT_FIELD;

    static {
        Field f = null;
        try {
            f = BaseController.class.getDeclaredField("currentAccount");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("AccountUtils: currentAccount field not found: %s", e.getMessage());
        }
        CURRENT_ACCOUNT_FIELD = f;
    }

    private AccountUtils() {
    }

    public static int getCurrentAccount(Object source) {
        Integer account;
        if (source instanceof Integer) {
            return ((Integer) source).intValue();
        }
        if (source instanceof BaseFragment) {
            BaseFragment fragment = (BaseFragment) source;
            return fragment.getCurrentAccount();
        }
        if (source instanceof MessageObject) {
            MessageObject messageObject = (MessageObject) source;
            return messageObject.currentAccount;
        }
        if (source instanceof AccountInstance) {
            AccountInstance accountInstance = (AccountInstance) source;
            return accountInstance.getCurrentAccount();
        }
        if (CURRENT_ACCOUNT_FIELD != null && (source instanceof BaseController) && (account = (Integer) ReflectionUtils.get(CURRENT_ACCOUNT_FIELD, source)) != null) {
            return account.intValue();
        }
        return UserConfig.selectedAccount;
    }
}
