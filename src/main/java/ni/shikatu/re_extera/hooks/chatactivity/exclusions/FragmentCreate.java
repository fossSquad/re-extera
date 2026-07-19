package ni.shikatu.re_extera.hooks.chatactivity.exclusions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
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

public class FragmentCreate extends XC_MethodHook {
    private static final Field ARGUMENTS_FIELD;
    private static final Field HEADER_ITEM_FIELD;

    static {
        Field headerField = null;
        Field argsField = null;
        try {
            headerField = ChatActivity.class.getDeclaredField("headerItem");
            headerField.setAccessible(true);
            argsField = BaseFragment.class.getDeclaredField("arguments");
            argsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Main.log("FragmentCreate: field not found: %s", e.getMessage());
        }
        HEADER_ITEM_FIELD = headerField;
        ARGUMENTS_FIELD = argsField;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        Bundle arguments;
        ChatActivity thisObject = (ChatActivity) param.thisObject;
        if (ARGUMENTS_FIELD == null || (arguments = (Bundle) ReflectionUtils.get(ARGUMENTS_FIELD, thisObject)) == null) {
            return;
        }
        long dialog_id = 0;
        long chat_id = arguments.getLong("chat_id", 0L);
        long user_id = arguments.getLong("user_id", 0L);
        int enc_id = arguments.getInt("enc_id", 0);
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

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        ActionBarMenuItem headerItem;
        ChatActivity thisObject = (ChatActivity) param.thisObject;
        long dialog_id = thisObject.getDialogId();
        int currentAccount = thisObject.getCurrentAccount();
        if (HEADER_ITEM_FIELD == null || (headerItem = (ActionBarMenuItem) ReflectionUtils.get(HEADER_ITEM_FIELD, thisObject)) == null) {
            return;
        }
        Context context = thisObject.getContext();
        ExceptionsPopupWrapper exceptionsPopupWrapper = new ExceptionsPopupWrapper(context, headerItem.getPopupLayout().getSwipeBack(), new AnonymousClass1(headerItem, context, currentAccount, dialog_id, thisObject), thisObject.getResourceProvider());
        final ActionBarMenuSubItem exceptionsItem = headerItem.addSwipeBackItem(R.drawable.filled_giveaway_premium, (Drawable) null, "re:extera", exceptionsPopupWrapper.windowLayout);
        exceptionsItem.setOnClickListener(new View.OnClickListener() { 
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                exceptionsItem.openSwipeBack();
            }
        });
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.hooks.chatactivity.exclusions.FragmentCreate$1, reason: invalid class name */
    class AnonymousClass1 implements ExceptionsPopupWrapper.Callback {
        final /* synthetic */ Context val$context;
        final /* synthetic */ int val$currentAccount;
        final /* synthetic */ long val$dialog_id;
        final /* synthetic */ ActionBarMenuItem val$headerItem;
        final /* synthetic */ ChatActivity val$thisObject;

        AnonymousClass1(ActionBarMenuItem actionBarMenuItem, Context context, int i, long j, ChatActivity chatActivity) {
            this.val$headerItem = actionBarMenuItem;
            this.val$context = context;
            this.val$currentAccount = i;
            this.val$dialog_id = j;
            this.val$thisObject = chatActivity;
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
            final int i = this.val$currentAccount;
            final long j = this.val$dialog_id;
            builder.setPositiveButton(str, new AlertDialog.OnButtonClickListener() { 
                public final void onClick(AlertDialog alertDialog, int i2) {
                    FragmentCreate.AnonymousClass1.lambda$finallyRemoveAllDeletedMessages$0(i, j, alertDialog, i2);
                }
            });
            builder.setNegativeButton(Localization.NO, new AlertDialog.OnButtonClickListener() { 
                public final void onClick(AlertDialog alertDialog, int i2) {
                    alertDialog.dismiss();
                }
            });
            builder.show();
        }

        static /* synthetic */ void lambda$finallyRemoveAllDeletedMessages$0(int currentAccount, long dialog_id, AlertDialog dialog, int which) {
            InternalUtils.deleteAllMessages(currentAccount, dialog_id);
            dialog.dismiss();
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void showDeletedMessages() {
            this.val$thisObject.presentFragment(DeletedMessagesInChatFragment.newInstance(this.val$dialog_id));
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void openReading() {
            Context context = this.val$context;
            long j = this.val$dialog_id;
            final long j2 = this.val$dialog_id;
            new ExclusionUtils.ExclusionReadingDialog(context, j, new Runnable() { 
                @Override // java.lang.Runnable
                public final void run() {
                    SendRequest.notifyDialogIdChanged(j2);
                }
            }).show();
        }

        @Override // ni.shikatu.re_extera.utils.ExceptionsPopupWrapper.Callback
        public void openTyping() {
            Context context = this.val$context;
            long j = this.val$dialog_id;
            final long j2 = this.val$dialog_id;
            new ExclusionUtils.ExclusionTypingDialog(context, j, new Runnable() { 
                @Override // java.lang.Runnable
                public final void run() {
                    SendRequest.notifyDialogIdChanged(j2);
                }
            }).show();
        }
    }
}
