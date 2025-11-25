package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import ni.shikatu.re_extera.localization.Localization;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.PopupSwipeBackLayout;

public class ExceptionsPopupWrapper {
    public TextView textView;
    public final ActionBarPopupWindow.ActionBarPopupWindowLayout windowLayout;

    public interface Callback {
        void dismiss();

        void finallyRemoveAllDeletedMessages();

        void openReading();

        void openTyping();

        void showDeletedMessages();
    }

    private Drawable getResizedDelete(Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.msg_delete_filled);
        drawable.setBounds(0, 0, 20, 20);
        return drawable;
    }

    public ExceptionsPopupWrapper(Context context, final PopupSwipeBackLayout swipeBackLayout, final Callback callback, Theme.ResourcesProvider resourcesProvider) {
        this.windowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, resourcesProvider);
        this.windowLayout.setFitItems(true);
        if (swipeBackLayout != null) {
            ActionBarMenuItem.addItem(this.windowLayout, R.drawable.msg_arrow_back, LocaleController.getString(R.string.Back), false, resourcesProvider).setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExceptionsPopupWrapper$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    swipeBackLayout.closeForeground();
                }
            });
        }
        ActionBarMenuSubItem itemReading = ActionBarMenuItem.addItem(this.windowLayout, R.drawable.msg_archive_hide, Localization.EXCEPTION_READING_TEXT, false, resourcesProvider);
        itemReading.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExceptionsPopupWrapper$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ExceptionsPopupWrapper.lambda$new$1(callback, view);
            }
        });
        ActionBarMenuSubItem itemTyping = ActionBarMenuItem.addItem(this.windowLayout, R.drawable.floating_pencil, Localization.EXCEPTION_TYPING_TEXT, false, resourcesProvider);
        itemTyping.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExceptionsPopupWrapper$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ExceptionsPopupWrapper.lambda$new$2(callback, view);
            }
        });
        ActionBarMenuSubItem itemFinallyDelete = ActionBarMenuItem.addItem(this.windowLayout, R.drawable.msg_remove, Localization.CLEAR_DELETED, false, resourcesProvider);
        itemFinallyDelete.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.utils.ExceptionsPopupWrapper$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ExceptionsPopupWrapper.lambda$new$3(callback, view);
            }
        });
    }

    static /* synthetic */ void lambda$new$1(Callback callback, View v) {
        callback.dismiss();
        callback.openReading();
    }

    static /* synthetic */ void lambda$new$2(Callback callback, View v) {
        callback.dismiss();
        callback.openTyping();
    }

    static /* synthetic */ void lambda$new$3(Callback callback, View v) {
        callback.dismiss();
        callback.finallyRemoveAllDeletedMessages();
    }
}
