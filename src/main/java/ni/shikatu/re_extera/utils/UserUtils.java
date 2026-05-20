package ni.shikatu.re_extera.utils;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

public final class UserUtils {
    private UserUtils() {
    }

    public static TLRPC.User getUser(int currentAccount, long peer) {
        return MessagesController.getInstance(currentAccount).getUser(Long.valueOf(peer));
    }

    public static TLRPC.User getSelf(int currentAccount) {
        return getUser(currentAccount, UserConfig.getInstance(currentAccount).clientUserId);
    }
}
