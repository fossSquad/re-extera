package ni.shikatu.re_extera.hooks.userconfig;

import android.util.Base64;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

public class isPremium extends XC_MethodHook {

    private static final Set<Long> fakePremiumUsers = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
    private static final ThreadLocal<ArrayList<TLRPC.User>> strippedUsers = new ThreadLocal<>();
    private static final ThreadLocal<TLRPC.User> strippedUser = new ThreadLocal<>();

    @Override
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (!Settings.getLocalPremium()) {
            return;
        }
        int account = AccountUtils.getCurrentAccount(param.thisObject);
        if (Settings.getRealPremium(account) == 1) {
            return;
        }
        try {
            patchUser(account, UserConfig.getInstance(account).getCurrentUser());
        } catch (Throwable ignored) {
        }
        param.setResult(true);
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

    private static void recordRealPremium(int account, TLRPC.User user) {
        if (user == null || user.min) {
            return;
        }
        try {
            if (user.id != UserConfig.getInstance(account).getClientUserId()) {
                return;
            }
            int value = user.premium ? 1 : 0;
            if (Settings.getRealPremium(account) != value) {
                Settings.setRealPremium(account, value == 1);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void patchUser(int account, TLRPC.User user) {
        if (user == null || !Settings.getLocalPremium()) {
            return;
        }
        if (Settings.getRealPremium(account) == 1) {
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

        if (!user.premium) {
            rememberFieldsIfPresent(account, user);
            user.premium = true;
            fakePremiumUsers.add(user.id);
        }

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

    public static void refresh(int account) {
        try {
            patchUser(account, UserConfig.getInstance(account).getCurrentUser());
        } catch (Throwable e) {
            Main.log("LocalPremium.refresh: %s", e.getMessage());
        }
    }

    public static void restore(int account) {
        try {
            UserConfig cfg = UserConfig.getInstance(account);
            TLRPC.User user = cfg.getCurrentUser();
            if (user != null && fakePremiumUsers.remove(user.id)) {
                user.premium = false;
                cfg.saveConfig(false);
            }
        } catch (Throwable e) {
            Main.log("LocalPremium.restore: %s", e.getMessage());
        }
    }

    public static Method[] findSaveConfigLambdas() {
        ArrayList<Method> methods = new ArrayList<>();
        try {
            for (Method method : UserConfig.class.getDeclaredMethods()) {
                if (!method.getName().startsWith("lambda$saveConfig$")) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || parameterTypes[0] != boolean.class) {
                    continue;
                }
                method.setAccessible(true);
                methods.add(method);
            }
        } catch (Throwable e) {
            Main.log("LocalPremium findSaveConfigLambdas: %s", e.getMessage());
        }
        return methods.toArray(new Method[0]);
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
            try {
                TLRPC.User user = (TLRPC.User) param.args[0];
                int account = AccountUtils.getCurrentAccount(param.thisObject);
                if (param.args.length > 1 && Boolean.FALSE.equals(param.args[1])) {
                    recordRealPremium(account, user);
                }
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
            try {
                ArrayList<TLRPC.User> users = (ArrayList<TLRPC.User>) param.args[0];
                if (users == null || users.isEmpty()) {
                    return;
                }
                int account = AccountUtils.getCurrentAccount(param.thisObject);
                boolean fromServer = param.args.length > 1 && Boolean.FALSE.equals(param.args[1]);
                for (TLRPC.User user : users) {
                    if (fromServer) {
                        recordRealPremium(account, user);
                    }
                    patchUser(account, user);
                }
            } catch (Throwable e) {
                Main.log("LocalPremium.PutUsersHook: %s", e.getMessage());
            }
        }
    }

    public static class PutUsersInternalHook extends XC_MethodHook {
        @Override
        @SuppressWarnings("unchecked")
        public void beforeHookedMethod(MethodHookParam param) {
            try {
                List<TLRPC.User> users = (List<TLRPC.User>) param.args[0];
                if (users == null || users.isEmpty()) {
                    return;
                }
                ArrayList<TLRPC.User> stripped = null;
                for (TLRPC.User user : users) {
                    if (user != null && user.premium && fakePremiumUsers.contains(user.id)) {
                        user.premium = false;
                        if (stripped == null) {
                            stripped = new ArrayList<>();
                        }
                        stripped.add(user);
                    }
                }
                if (stripped != null) {
                    strippedUsers.set(stripped);
                }
            } catch (Throwable e) {
                Main.log("LocalPremium.PutUsersInternalHook: %s", e.getMessage());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void afterHookedMethod(MethodHookParam param) {
            try {
                ArrayList<TLRPC.User> stripped = strippedUsers.get();
                if (stripped == null) {
                    return;
                }
                strippedUsers.remove();
                for (TLRPC.User user : stripped) {
                    user.premium = true;
                }
            } catch (Throwable e) {
                Main.log("LocalPremium.PutUsersInternalHook restore: %s", e.getMessage());
            }
        }
    }

    public static class SaveConfigHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(MethodHookParam param) {
            try {
                UserConfig cfg = (UserConfig) param.thisObject;
                TLRPC.User user = cfg.getCurrentUser();
                if (user != null && user.premium && fakePremiumUsers.contains(user.id)) {
                    user.premium = false;
                    strippedUser.set(user);
                }
            } catch (Throwable e) {
                Main.log("LocalPremium.SaveConfigHook: %s", e.getMessage());
            }
        }

        @Override
        public void afterHookedMethod(MethodHookParam param) {
            TLRPC.User user = strippedUser.get();
            if (user != null) {
                strippedUser.remove();
                user.premium = true;
            }
        }
    }
}
