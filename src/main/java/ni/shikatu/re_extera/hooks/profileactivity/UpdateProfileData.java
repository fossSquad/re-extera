package ni.shikatu.re_extera.hooks.profileactivity;

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
    private static final long REQUEST_COOLDOWN = 5000;
    private static XC_MethodHook blockHook;
    private static Method needLayout;
    private static Field onlineTextView;
    private static Field userId;
    private static final Set<SimpleTextView> trackedViews = Collections.newSetFromMap(new WeakHashMap());
    private static volatile boolean blockUpdates = false;
    private static String cachedText = null;
    private static long lastRequestTime = 0;

    interface TimeCallback {
        void onTime(int i);
    }

    static {
        try {
            userId = ProfileActivity.class.getDeclaredField("userId");
            onlineTextView = ProfileActivity.class.getDeclaredField("onlineTextView");
            needLayout = ProfileActivity.class.getDeclaredMethod("needLayout", Boolean.TYPE);
            needLayout.setAccessible(true);
            userId.setAccessible(true);
            onlineTextView.setAccessible(true);
            hookSetText();
        } catch (NoSuchFieldException e) {
            Main.log("Not found field: %s", e.getMessage());
        } catch (NoSuchMethodException e2) {
            Main.log("Not found method: %s", e2.getMessage());
        }
        blockHook = new XC_MethodHook() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData.1
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (UpdateProfileData.blockUpdates && UpdateProfileData.trackedViews.contains(param.thisObject)) {
                    param.setResult(false);
                }
            }
        };
    }

    private static void hookSetText() {
        try {
            Method setTextMethod = SimpleTextView.class.getDeclaredMethod("setText", CharSequence.class);
            XposedBridge.hookMethod(setTextMethod, blockHook);
            Method setTextWithBoolean = SimpleTextView.class.getDeclaredMethod("setText", CharSequence.class, Boolean.TYPE);
            XposedBridge.hookMethod(setTextWithBoolean, blockHook);
        } catch (Exception e) {
            Main.log("Failed to hook setText: %s", e.getMessage());
        }
    }

    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
        if (userId == null || onlineTextView == null) {
            return;
        }
        long user = ((Long) ReflectionUtils.get(userId, param.thisObject)).longValue();
        final SimpleTextView[] onlineView = (SimpleTextView[]) ReflectionUtils.get(onlineTextView, param.thisObject);
        if (user != UserConfig.getInstance(UserConfig.selectedAccount).clientUserId) {
            return;
        }
        if (Settings.getHideOnlineWithGhost()) {
            trackedViews.clear();
            for (SimpleTextView view : onlineView) {
                if (view != null) {
                    trackedViews.add(view);
                }
            }
            blockUpdates = true;
            if (cachedText != null) {
                setTextSafely(onlineView, cachedText, param.thisObject);
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime > REQUEST_COOLDOWN) {
                lastRequestTime = currentTime;
                getRealLastSeenTime(user, new TimeCallback() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData$$ExternalSyntheticLambda0
                    @Override // ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData.TimeCallback
                    public final void onTime(int i) {
                        this.f$0.lambda$afterHookedMethod$1(onlineView, param, i);
                    }
                });
                return;
            }
            return;
        }
        blockUpdates = false;
        trackedViews.clear();
        cachedText = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$afterHookedMethod$1(final SimpleTextView[] onlineView, final XC_MethodHook.MethodHookParam param, int timestamp) {
        final String formattedTime = formatLastSeen(timestamp);
        cachedText = formattedTime;
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$afterHookedMethod$0(onlineView, formattedTime, param);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$afterHookedMethod$0(SimpleTextView[] onlineView, String formattedTime, XC_MethodHook.MethodHookParam param) {
        setTextSafely(onlineView, formattedTime, param.thisObject);
    }

    private void setTextSafely(SimpleTextView[] views, String text, Object profileActivity) {
        blockUpdates = false;
        for (SimpleTextView view : views) {
            if (view != null) {
                view.setText(text);
            }
        }
        blockUpdates = true;
        ReflectionUtils.invoke(needLayout, profileActivity, true);
    }

    private void getRealLastSeenTime(long userId2, final TimeCallback callback) {
        try {
            TLRPC.TL_users_getUsers request = new TLRPC.TL_users_getUsers();
            TLRPC.InputUser inputUser = MessagesController.getInstance(UserConfig.selectedAccount).getInputUser(userId2);
            request.id.add(inputUser);
            ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(request, new RequestDelegate() { // from class: ni.shikatu.re_extera.hooks.profileactivity.UpdateProfileData$$ExternalSyntheticLambda2
                public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                    this.f$0.lambda$getRealLastSeenTime$2(callback, tLObject, tL_error);
                }
            });
        } catch (Exception e) {
            Main.log("Error getting real last seen: %s", e.getMessage());
            callback.onTime(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$getRealLastSeenTime$2(TimeCallback callback, TLObject response, TLRPC.TL_error error) {
        if (response instanceof Vector) {
            Vector<?> vector = (Vector) response;
            if (!vector.objects.isEmpty() && (vector.objects.get(0) instanceof TLRPC.User)) {
                TLRPC.User user = (TLRPC.User) vector.objects.get(0);
                if (user.status != null) {
                    int time = getTimeFromStatus(user.status);
                    callback.onTime(time);
                    return;
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
