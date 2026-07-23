package ni.shikatu.re_extera.hooks.localecontroller;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.tgnet.TLRPC;

public class FormatUserStatus extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getSaveLastOnline()) {
            return;
        }
        TLRPC.User user = (TLRPC.User) param.args[1];
        if (user == null || user.status == null) {
            return;
        }

        boolean isHidden = (user.status instanceof TLRPC.TL_userStatusRecently) ||
                           (user.status instanceof TLRPC.TL_userStatusLastWeek) ||
                           (user.status instanceof TLRPC.TL_userStatusLastMonth) ||
                           (user.status instanceof TLRPC.TL_userStatusEmpty);

        if (isHidden) {
            int wasOnline = ReExteraDb.get().getLastOnline(user.id);
            if (wasOnline > 0) {
                TLRPC.TL_userStatusOffline exactStatus = new TLRPC.TL_userStatusOffline();
                exactStatus.was_online = wasOnline;
                param.setObjectExtra("orig_status", user.status);
                user.status = exactStatus;
            }
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getSaveLastOnline()) {
            return;
        }
        TLRPC.User user = (TLRPC.User) param.args[1];
        if (user == null) {
            return;
        }
        TLRPC.UserStatus origStatus = (TLRPC.UserStatus) param.getObjectExtra("orig_status");
        if (origStatus != null) {
            user.status = origStatus; // restore original status
            String res = (String) param.getResult();
            if (res != null) {
                param.setResult(res); // you could append a mark here if needed, but keeping it clean for now
            }
        }
    }
}
