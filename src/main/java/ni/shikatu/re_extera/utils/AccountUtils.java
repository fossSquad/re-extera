package ni.shikatu.re_extera.utils;

import java.lang.reflect.Field;
import ni.shikatu.re_extera.Main;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;

public class AccountUtils {
    private static Field currentAccountField;

    static {
        try {
            currentAccountField = BaseController.class.getDeclaredField("currentAccount");
            currentAccountField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("AccountUtils: currentAccount field not found: %s", e.getMessage());
        }
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
        if (currentAccountField != null && (source instanceof BaseController) && (account = (Integer) ReflectionUtils.get(currentAccountField, source)) != null) {
            return account.intValue();
        }
        return UserConfig.selectedAccount;
    }
}
