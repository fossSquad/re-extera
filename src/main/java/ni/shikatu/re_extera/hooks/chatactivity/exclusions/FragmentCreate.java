package ni.shikatu.re_extera.hooks.chatactivity.exclusions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.hooks.connectionsmanager.SendRequest;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.ui.DeletedMessagesInChatFragment;
import ni.shikatu.re_extera.utils.ExceptionsPopupWrapper;
import ni.shikatu.re_extera.utils.ExclusionUtils;
import ni.shikatu.re_extera.utils.InternalUtils;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.ReflectionUtils;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class FragmentCreate extends XC_MethodHook {
    private static final int MENU_EXCEPTIONS = 6961;
    private static final int MENU_EXCEPTION_READING = 6364;
    private static final int MENU_EXCEPTION_TYPING = 6365;
    private static Field argumentsField;
    private static Field headerItemField;
    private ActionBarMenuSubItem exceptionsItem;
    private ExceptionsPopupWrapper exceptionsPopupWrapper;

    static {
        try {
            headerItemField = ChatActivity.class.getDeclaredField("headerItem");
            headerItemField.setAccessible(true);
            argumentsField = BaseFragment.class.getDeclaredField("arguments");
            argumentsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("No headerItem field found", e.getMessage());
        }
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Main.log("Notifying dialog id change", new Object[0]);
        ChatActivity thisObject = (ChatActivity) param.thisObject;
        Bundle arguments = (Bundle) ReflectionUtils.get(argumentsField, thisObject);
        long chat_id = arguments.getLong("chat_id", 0L);
        long user_id = arguments.getLong("user_id", 0L);
        int enc_id = arguments.getInt("enc_id", 0);
        long dialog_id = 0;
        if (chat_id != 0) {
            dialog_id = -chat_id;
        } else if (user_id != 0) {
            dialog_id = user_id;
        } else if (enc_id != 0) {
            dialog_id = DialogObject.makeEncryptedDialogId(enc_id);
        }
        MessageUtils.updatePatterns();
        SendRequest.notifyDialogIdChanged(dialog_id);
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Main.log("Creating menu", new Object[0]);
        ChatActivity thisObject = (ChatActivity) param.thisObject;
        long dialog_id = thisObject.getDialogId();
        ActionBarMenuItem headerItem = (ActionBarMenuItem) headerItemField.get(thisObject);
        if (headerItem == null) {
            return;
        }
        Context context = thisObject.getContext();
        this.exceptionsPopupWrapper = new ExceptionsPopupWrapper(context, headerItem.getPopupLayout().getSwipeBack(), new AnonymousClass1(headerItem, context, dialog_id), thisObject.getResourceProvider());
        this.exceptionsItem = headerItem.addSwipeBackItem(R.drawable.filled_giveaway_premium, (Drawable) null, "re:extera", this.exceptionsPopupWrapper.windowLayout);
        this.exceptionsItem.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$afterHookedMethod$0(view);
            }
        });
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate$1, reason: invalid class name */
    class AnonymousClass1 implements ExceptionsPopupWrapper.Callback {
        final /* synthetic */ Context val$context;
        final /* synthetic */ long val$dialog_id;
        final /* synthetic */ ActionBarMenuItem val$headerItem;

        AnonymousClass1(ActionBarMenuItem actionBarMenuItem, Context context, long j) {
            this.val$headerItem = actionBarMenuItem;
            this.val$context = context;
            this.val$dialog_id = j;
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void dismiss() {
            this.val$headerItem.toggleSubMenu();
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void finallyRemoveAllDeletedMessages() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.val$context);
            builder.setTitle(Localization.DELETE);
            builder.setSubtitle(Localization.FINALLY_REMOVE_ALL_DELETED_MESSAGES);
            String str = Localization.YES;
            final long j = this.val$dialog_id;
            builder.setPositiveButton(str, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate$1$$ExternalSyntheticLambda0
                public final void onClick(AlertDialog alertDialog, int i) {
                    FragmentCreate.AnonymousClass1.lambda$finallyRemoveAllDeletedMessages$0(j, alertDialog, i);
                }
            });
            builder.setNegativeButton(Localization.NO, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate$1$$ExternalSyntheticLambda1
                public final void onClick(AlertDialog alertDialog, int i) {
                    alertDialog.dismiss();
                }
            });
            builder.show();
        }

        static /* synthetic */ void lambda$finallyRemoveAllDeletedMessages$0(long dialog_id, AlertDialog dialog, int which) {
            InternalUtils.deleteAllMessages(dialog_id);
            dialog.dismiss();
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void showDeletedMessages() {
            DeletedMessagesInChatFragment fragment = DeletedMessagesInChatFragment.newInstance(this.val$dialog_id);
            LaunchActivity.getLastFragment().presentFragment(fragment);
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void openReading() throws IllegalAccessException, InvocationTargetException {
            Context context = this.val$context;
            long j = this.val$dialog_id;
            final long j2 = this.val$dialog_id;
            new ExclusionUtils.ExclusionReadingDialog(context, j, new Runnable() { // from class: ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    SendRequest.notifyDialogIdChanged(j2);
                }
            }).show();
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void openTyping() throws IllegalAccessException, InvocationTargetException {
            Context context = this.val$context;
            long j = this.val$dialog_id;
            final long j2 = this.val$dialog_id;
            new ExclusionUtils.ExclusionTypingDialog(context, j, new Runnable() { // from class: ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    SendRequest.notifyDialogIdChanged(j2);
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$afterHookedMethod$0(View v) {
        this.exceptionsItem.openSwipeBack();
    }
}
