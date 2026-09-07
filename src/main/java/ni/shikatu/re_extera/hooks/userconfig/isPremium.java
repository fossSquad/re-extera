package ni.shikatu.re_extera.hooks.userconfig;

import android.util.Base64;
import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

public class isPremium extends XC_MethodHook {

    @Override
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getLocalPremium()) {
            param.setResult(true);
        }
    }

    private static String encodeField(TLRPC.TL_peerColor color) {
        if (color == null) {
            return null;
        }
        try {
            int size = color.getObjectSize();
            NativeByteBuffer buffer = new NativeByteBuffer(size);
            color.serializeToStream(buffer);
            int written = buffer.position();
            byte[] out;
            if (buffer.buffer.hasArray()) {
                out = new byte[written];
                System.arraycopy(buffer.buffer.array(), buffer.buffer.arrayOffset(), out, 0, written);
            } else {
                out = new byte[written];
                buffer.buffer.position(0);
                buffer.buffer.get(out, 0, written);
            }
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Throwable e) {
            Main.log("LocalPremium encode color failed: %s", e.getMessage());
            return null;
        }
    }

    private static TLRPC.TL_peerColor decodeColorField(String base64Value) {
        if (base64Value == null) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(base64Value, Base64.NO_WRAP);
            NativeByteBuffer buffer = new NativeByteBuffer(bytes.length);
            buffer.writeBytes(bytes);
            buffer.position(0);
            TLRPC.TL_peerColor color = new TLRPC.TL_peerColor();
            color.readParams(buffer, true);
            return color;
        } catch (Throwable e) {
            Main.log("LocalPremium decode color failed: %s", e.getMessage());
            return null;
        }
    }

    private static void rememberFieldsIfPresent(int account, TLRPC.User user) {
        try {
            if (user.color instanceof TLRPC.TL_peerColor) {
                String enc = encodeField((TLRPC.TL_peerColor) user.color);
                if (enc != null) {
                    Settings.setCachedPremiumField(account, "color", enc);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (user.profile_color instanceof TLRPC.TL_peerColor) {
                String enc = encodeField((TLRPC.TL_peerColor) user.profile_color);
                if (enc != null) {
                    Settings.setCachedPremiumField(account, "profile_color", enc);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void patchUser(int account, TLRPC.User user) {
        if (user == null || !Settings.getLocalPremium()) {
            return;
        }
        long myId;
        try {
            myId = UserConfig.getInstance(account).getClientUserId();
        } catch (Throwable e) {
            return;
        }
        if (user.id != myId) {
            return;
        }

        rememberFieldsIfPresent(account, user);
        user.premium = true;

        if (user.color == null) {
            TLRPC.TL_peerColor cached = decodeColorField(Settings.getCachedPremiumField(account, "color"));
            if (cached != null) {
                user.color = cached;
            }
        }
        if (user.profile_color == null) {
            TLRPC.TL_peerColor cached = decodeColorField(Settings.getCachedPremiumField(account, "profile_color"));
            if (cached != null) {
                user.profile_color = cached;
            }
        }
    }

    public static class SetCurrentUserHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(MethodHookParam param) {
            if (!Settings.getLocalPremium()) {
                return;
            }
            try {
                TLRPC.User user = (TLRPC.User) param.args[0];
                int account = AccountUtils.getCurrentAccount(param.thisObject);
                patchUser(account, user);
            } catch (Throwable e) {
                Main.log("LocalPremium.SetCurrentUserHook: %s", e.getMessage());
            }
        }
    }

    public static class PutUserHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(MethodHookParam param) {
            if (!Settings.getLocalPremium()) {
                return;
            }
            try {
                TLRPC.User user = (TLRPC.User) param.args[0];
                int account = AccountUtils.getCurrentAccount(param.thisObject);
                patchUser(account, user);
            } catch (Throwable e) {
                Main.log("LocalPremium.PutUserHook: %s", e.getMessage());
            }
        }
    }

    public static class PutUsersHook extends XC_MethodHook {
        @Override
        @SuppressWarnings("unchecked")
        public void beforeHookedMethod(MethodHookParam param) {
            if (!Settings.getLocalPremium()) {
                return;
            }
            try {
                ArrayList<TLRPC.User> users = (ArrayList<TLRPC.User>) param.args[0];
                if (users == null || users.isEmpty()) {
                    return;
                }
                int account = AccountUtils.getCurrentAccount(param.thisObject);
                for (TLRPC.User user : users) {
                    patchUser(account, user);
                }
            } catch (Throwable e) {
                Main.log("LocalPremium.PutUsersHook: %s", e.getMessage());
            }
        }
    }
}
