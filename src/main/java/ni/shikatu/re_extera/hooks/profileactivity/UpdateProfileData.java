package ni.shikatu.re_extera.hooks.profileactivity;

import android.util.SparseArray;
import android.util.SparseLongArray;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ProfileActivity;

public class UpdateProfileData extends XC_MethodHook {
    private static final long REQUEST_COOLDOWN_MS = 5000;
    private static final Field USER_ID = field("userId");
    private static final Field ONLINE_TEXT_VIEW = field("onlineTextView");
    private static final Method NEED_LAYOUT = method("needLayout", Boolean.TYPE);
    private static final Set<SimpleTextView> TRACKED_VIEWS = Collections.newSetFromMap(new WeakHashMap());
    private static volatile boolean blockUpdates = false;
    private static final SparseArray<String> cachedTextByAccount = new SparseArray<>();
    private static final SparseLongArray lastRequestTimeByAccount = new SparseLongArray();
    private static final XC_MethodHook BLOCK_HOOK = new XC_MethodHook() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData.1
        public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
            if (UpdateProfileData.blockUpdates && UpdateProfileData.TRACKED_VIEWS.contains(param.thisObject)) {
                param.setResult(false);
            }
        }
    };

    interface TimeCallback {
        void onTime(int i);
    }

    static {
        hookSetText();
    }

    private static Field field(String name) {
        try {
            Field f = ProfileActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            Main.log("UpdateProfileData: field '%s' not found: %s", name, e.getMessage());
            return null;
        }
    }

    private static Method method(String name, Class<?>... params) {
        try {
            Method m = ProfileActivity.class.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            Main.log("UpdateProfileData: method '%s' not found: %s", name, e.getMessage());
            return null;
        }
    }

    private static void hookSetText() {
        try {
            XposedBridge.hookMethod(SimpleTextView.class.getDeclaredMethod("setText", CharSequence.class), BLOCK_HOOK);
            XposedBridge.hookMethod(SimpleTextView.class.getDeclaredMethod("setText", CharSequence.class, Boolean.TYPE), BLOCK_HOOK);
        } catch (Exception e) {
            Main.log("UpdateProfileData: failed to hook setText: %s", e.getMessage());
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (USER_ID != null && ONLINE_TEXT_VIEW != null) {
            final ProfileActivity activity = (ProfileActivity) param.thisObject;
            final int currentAccount = activity.getCurrentAccount();
            long user = ((Long) ReflectionUtils.get(USER_ID, activity)).longValue();
            final SimpleTextView[] onlineView = (SimpleTextView[]) ReflectionUtils.get(ONLINE_TEXT_VIEW, activity);
            if (onlineView == null || onlineView.length == 0 || user != UserConfig.getInstance(currentAccount).clientUserId) {
                return;
            }
            if (!Settings.getHideOnlineWithGhost()) {
                blockUpdates = false;
                TRACKED_VIEWS.clear();
                cachedTextByAccount.remove(currentAccount);
                lastRequestTimeByAccount.delete(currentAccount);
                return;
            }
            TRACKED_VIEWS.clear();
            for (SimpleTextView view : onlineView) {
                if (view != null) {
                    TRACKED_VIEWS.add(view);
                }
            }
            blockUpdates = true;
            String cachedText = cachedTextByAccount.get(currentAccount);
            if (cachedText != null) {
                lambda$afterHookedMethod$0(onlineView, cachedText, activity);
            }
            long now = System.currentTimeMillis();
            long last = lastRequestTimeByAccount.get(currentAccount, 0L);
            if (now - last > REQUEST_COOLDOWN_MS) {
                lastRequestTimeByAccount.put(currentAccount, now);
                getRealLastSeenTime(currentAccount, user, new TimeCallback() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData$$ExternalSyntheticLambda1
                    @Override // ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData.TimeCallback
                    public final void onTime(int i) {
                        this.f$0.lambda$afterHookedMethod$1(currentAccount, onlineView, activity, i);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$afterHookedMethod$1(int currentAccount, final SimpleTextView[] onlineView, final ProfileActivity activity, int timestamp) {
        final String formatted = formatLastSeen(timestamp);
        cachedTextByAccount.put(currentAccount, formatted);
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$afterHookedMethod$0(onlineView, formatted, activity);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: setTextSafely, reason: merged with bridge method [inline-methods] */
    public void lambda$afterHookedMethod$0(SimpleTextView[] views, String text, ProfileActivity profileActivity) {
        blockUpdates = false;
        for (SimpleTextView view : views) {
            if (view != null) {
                view.setText(text);
            }
        }
        blockUpdates = true;
        ReflectionUtils.invoke(NEED_LAYOUT, profileActivity, true);
    }

    private void getRealLastSeenTime(int currentAccount, long userId, final TimeCallback callback) {
        try {
            TLRPC.TL_users_getUsers request = new TLRPC.TL_users_getUsers();
            TLRPC.InputUser inputUser = MessagesController.getInstance(currentAccount).getInputUser(userId);
            if (inputUser == null) {
                callback.onTime(0);
            } else {
                request.id.add(inputUser);
                ConnectionsManager.getInstance(currentAccount).sendRequest(request, new RequestDelegate() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData$$ExternalSyntheticLambda0
                    public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                        this.f$0.lambda$getRealLastSeenTime$2(callback, tLObject, tL_error);
                    }
                });
            }
        } catch (Exception e) {
            Main.log("UpdateProfileData: error getting last seen: %s", e.getMessage());
            callback.onTime(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$getRealLastSeenTime$2(TimeCallback callback, TLObject response, TLRPC.TL_error error) {
        if (response instanceof Vector) {
            Vector<?> vector = (Vector) response;
            if (!vector.objects.isEmpty()) {
                Object patt5524$temp = vector.objects.get(0);
                if (patt5524$temp instanceof TLRPC.User) {
                    TLRPC.User user = (TLRPC.User) patt5524$temp;
                    if (user.status != null) {
                        callback.onTime(getTimeFromStatus(user.status));
                        return;
                    }
                }
            }
        }
        callback.onTime(0);
    }

    private int getTimeFromStatus(TLRPC.UserStatus status) {
        if (status instanceof TLRPC.TL_userStatusOnline) {
            return 0;
        }
        return status.expires;
    }

    private String formatLastSeen(int timestamp) {
        if (timestamp == 0) {
            return LocaleController.getString(R.string.Online);
        }
        return LocaleController.formatDateTime(timestamp, true);
    }
}
