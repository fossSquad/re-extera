package ni.shikatu.re_extera.hooks.storyviewer;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;
import java.util.ArrayList;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.AlertDialog;

public class StoryViewerHook {
    private static volatile boolean storyGhostSession = false;
    private static final ThreadLocal<Boolean> isPassingThrough = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean isStoryGhostSession() {
        return storyGhostSession;
    }

    public static void setStoryGhostSession(boolean active) {
        storyGhostSession = active;
    }

    public static class OpenHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(MethodHookParam param) {
            if (isPassingThrough.get()) {
                return;
            }
            int mode = Settings.getStoryGhostMode();
            if (mode == 0) {
                return;
            }
            if (Settings.getNoReadStoriesWithGhost()) {
                return;
            }
            if (storyGhostSession) {
                return;
            }

            int account = UserConfig.selectedAccount;
            Context context = null;
            TL_stories.StoryItem storyItem = null;
            ArrayList<Long> peerIds = null;
            int position = 0;

            if (param.args.length >= 9) {
                if (param.args[0] instanceof Integer) account = (Integer) param.args[0];
                if (param.args[1] instanceof Context) context = (Context) param.args[1];
                if (param.args[2] instanceof TL_stories.StoryItem) storyItem = (TL_stories.StoryItem) param.args[2];
                if (param.args[3] instanceof ArrayList) peerIds = (ArrayList<Long>) param.args[3];
                if (param.args[4] instanceof Integer) position = (Integer) param.args[4];
            } else if (param.args.length >= 8) {
                if (param.args[0] instanceof Context) context = (Context) param.args[0];
                if (param.args[1] instanceof TL_stories.StoryItem) storyItem = (TL_stories.StoryItem) param.args[1];
                if (param.args[2] instanceof ArrayList) peerIds = (ArrayList<Long>) param.args[2];
                if (param.args[3] instanceof Integer) position = (Integer) param.args[3];
            }

            if (context == null) {
                return;
            }

            long clientUserId = UserConfig.getInstance(account).getClientUserId();
            long targetDialogId = 0;
            if (storyItem != null && storyItem.dialogId != 0) {
                targetDialogId = storyItem.dialogId;
            } else if (peerIds != null && position >= 0 && position < peerIds.size()) {
                targetDialogId = peerIds.get(position);
            }
            if (targetDialogId != 0 && targetDialogId == clientUserId) {
                return;
            }

            if (mode == 2) {
                storyGhostSession = true;
                return;
            }

            param.setResult(null);

            final Object storyViewer = param.thisObject;
            final Method method = (Method) param.method;
            final Object[] originalArgs = param.args;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(Localization.ASK_BEFORE_OPENING_STORY_TITLE);
            CharSequence[] items = new CharSequence[]{
                    Localization.ASK_BEFORE_OPENING_STORY_GHOST,
                    Localization.ASK_BEFORE_OPENING_STORY_NORMAL
            };
            builder.setItems(items, (dialog, which) -> {
                if (which == 0) {
                    storyGhostSession = true;
                }
                isPassingThrough.set(true);
                try {
                    ReflectionUtils.invokeOriginalMethod(method, storyViewer, originalArgs);
                } catch (Throwable t) {
                    Main.log("StoryViewer open retry error: %s", t.getMessage());
                } finally {
                    isPassingThrough.set(false);
                }
            });
            builder.setNegativeButton(Localization.CANCEL, null);
            builder.show();
        }
    }

    public static class CloseHook extends XC_MethodHook {
        @Override
        public void beforeHookedMethod(MethodHookParam param) {
            storyGhostSession = false;
        }
    }
}
