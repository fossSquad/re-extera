package ni.shikatu.re_extera.hooks.messagescontroller;

import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.tgnet.TLRPC;

public class PutUsers extends XC_MethodHook {
    @Override
    @SuppressWarnings("unchecked")
    public void beforeHookedMethod(MethodHookParam param) {
        try {
            if (!Settings.getSaveLastOnline()) {
                return;
            }
            
            boolean fromCache = false;
            if (param.args.length > 1 && param.args[1] instanceof Boolean) {
                fromCache = (Boolean) param.args[1];
            }
            
            if (fromCache) {
                return;
            }
            
            Object usersArg = param.args[0];
            if (!(usersArg instanceof List)) {
                return;
            }
            List<TLRPC.User> copy;
            synchronized (usersArg) {
                copy = new ArrayList<>((List<TLRPC.User>) usersArg);
            }
            if (copy.isEmpty()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis() / 1000L;
            for (TLRPC.User user : copy) {
                if (user != null && user.status != null) {
                    if (user.status instanceof TLRPC.TL_userStatusOffline) {
                        ReExteraDb.get().saveLastOnlineAsync(user.id, ((TLRPC.TL_userStatusOffline) user.status).expires);
                    } else if (user.status instanceof TLRPC.TL_userStatusOnline) {
                        ReExteraDb.get().saveLastOnlineAsync(user.id, (int) currentTime);
                    }
                }
            }
        } catch (Throwable t) {
            Main.log("PutUsers.beforeHookedMethod error: %s", t.getMessage());
        }
    }
}
