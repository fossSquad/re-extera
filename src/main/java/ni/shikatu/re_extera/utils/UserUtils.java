package ni.shikatu.re_extera.utils;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

public class UserUtils {
    public static TLRPC.User getUser(long peer) {
        return MessagesController.getInstance(UserConfig.selectedAccount).getUser(Long.valueOf(peer));
    }
}
