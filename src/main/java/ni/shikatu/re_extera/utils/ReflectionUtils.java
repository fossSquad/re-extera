package ni.shikatu.re_extera.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.StickerImageView;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

public class ReflectionUtils {
    public static <T> T invoke(Method method, Object obj, Object... objArr) {
        if (method == null) {
            hookError();
            return null;
        }
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            Main.log("IllegalAccessException", e.getMessage());
            return null;
        } catch (InvocationTargetException e2) {
            Main.log("InvocationTargetException", e2.getMessage());
            return null;
        } catch (Exception e3) {
            Main.log("Exception", e3.getMessage());
            return null;
        }
    }

    public static <T> T get(Field field, Object obj) {
        if (field == null) {
            hookError();
            return null;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            return (T) field.get(obj);
        } catch (IllegalAccessException e) {
            Main.log("IllegalAccessException", e.getMessage());
            return null;
        }
    }

    public static void set(Field field, Object object, Object value) {
        if (field == null) {
            hookError();
            return;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            Main.log("IllegalAccessException", e.getMessage());
        }
    }

    public static <T> T invokeOriginalMethod(Method method, Object obj, Object[] objArr) {
        if (method == null) {
            hookError();
            return null;
        }
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) XposedBridge.invokeOriginalMethod(method, obj, objArr);
        } catch (IllegalAccessException e) {
            Main.log("Exception", e.getMessage());
            return null;
        } catch (InvocationTargetException e2) {
            Main.log("InvocationTargetException", e2.getMessage());
            return null;
        }
    }

    public static void hookError() {
        Main.getInstance().onUnload();
        final BaseFragment last = LaunchActivity.getLastFragment();
        if (last != null) {
            AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.utils.ReflectionUtils$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ReflectionUtils.lambda$hookError$0(last);
                }
            });
        }
    }

    static /* synthetic */ void lambda$hookError$0(BaseFragment last) {
        HooksInitFailedBottomSheet bottomSheet = new HooksInitFailedBottomSheet(last);
        bottomSheet.show();
    }

    public static class HooksInitFailedBottomSheet extends BottomSheet {
        public HooksInitFailedBottomSheet(BaseFragment fragment) {
            super(fragment.getParentActivity(), false, fragment.getResourceProvider());
            final Activity activity = fragment.getParentActivity();
            fixNavigationBar();
            FrameLayout frameLayout = new FrameLayout(activity);
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(1);
            linearLayout.setClipChildren(false);
            linearLayout.setClipToPadding(false);
            frameLayout.addView(linearLayout);
            FrameLayout stickerLayout = new FrameLayout(getContext());
            StickerImageView stickerImageView = new StickerImageView(getContext(), UserConfig.selectedAccount);
            stickerImageView.setStickerPackName("fuki_dum_pjsk_pack");
            stickerImageView.setStickerNum(22);
            stickerImageView.setAspectFit(true);
            stickerLayout.addView((View) stickerImageView, (ViewGroup.LayoutParams) new FrameLayout.LayoutParams(AndroidUtilities.dp(130.0f), AndroidUtilities.dp(130.0f), 17));
            stickerLayout.setMinimumHeight(AndroidUtilities.dp(150.0f));
            linearLayout.addView(stickerLayout, LayoutHelper.createLinear(120, 120, 1, 0, 20, 0, 0));
            TextView titleView = new TextView(activity);
            titleView.setGravity(1);
            titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setTextSize(1, 20.0f);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setText(Localization.WE_BROKE_SOMETHING);
            linearLayout.addView(titleView, LayoutHelper.createLinear(-1, -2, 0, 12, 16, 12, 0));
            TextView descriptionView = new TextView(activity);
            descriptionView.setGravity(1);
            descriptionView.setTextSize(1, 14.0f);
            descriptionView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            descriptionView.setText(Localization.BROKE_HOOKS_DESCRIPTION);
            linearLayout.addView(descriptionView, LayoutHelper.createLinear(-1, -2, 0, 24, 8, 24, 0));
            ButtonWithCounterView restartButton = new ButtonWithCounterView(activity, true, this.resourcesProvider);
            restartButton.setText(Localization.CLOSE_APPLICATION, false);
            restartButton.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ReflectionUtils$HooksInitFailedBottomSheet$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$0(activity, view);
                }
            });
            linearLayout.addView((View) restartButton, LayoutHelper.createLinear(-1, 48, 0, 16, 28, 16, 12));
            TextView continueButton = new TextView(activity);
            continueButton.setGravity(17);
            continueButton.setTextSize(1, 14.0f);
            continueButton.setTypeface(AndroidUtilities.bold());
            continueButton.setText(Localization.CONTINUE_ANYWAY);
            continueButton.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
            continueButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
            continueButton.setPadding(AndroidUtilities.dp(16.0f), AndroidUtilities.dp(10.0f), AndroidUtilities.dp(16.0f), AndroidUtilities.dp(10.0f));
            continueButton.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ReflectionUtils$HooksInitFailedBottomSheet$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$new$1(view);
                }
            });
            linearLayout.addView(continueButton, LayoutHelper.createLinear(-1, -2, 0, 16, 0, 16, 16));
            ScrollView scrollView = new ScrollView(activity);
            scrollView.addView(frameLayout);
            setCustomView(scrollView);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$0(Activity activity, View view) {
            restartApp(activity);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$new$1(View v) {
            dismiss();
        }

        private void restartApp(Context context) {
            System.exit(0);
        }
    }
}
