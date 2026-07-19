package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import java.util.ArrayList;
import java.util.List;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.DialogExclusion;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.ExclusionUtils;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class ExclusionsFragment extends BasePreferencesActivity {
    private ArrayList<DialogExclusion> exceptions = new ArrayList<>();

    public View createView(Context context) {
        View view = super.createView(context);
        if (this.actionBar != null) {
            this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            this.actionBar.setTitle(Localization.EXCLUSIONS);
            this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.ExclusionsFragment.1
                public void onItemClick(int id) {
                    if (id == -1) {
                        ExclusionsFragment.this.finishFragment();
                    }
                }
            });
        }
        return view;
    }

    public String getTitle() {
        return Localization.EXCLUSIONS;
    }

    public boolean onFragmentCreate() {
        loadExceptions();
        return super.onFragmentCreate();
    }

    public void onResume() {
        super.onResume();
        loadExceptions();
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
    }

    private void loadExceptions() {
        this.exceptions.clear();
        List<DialogExclusion> allExceptions = ReExteraDb.get().getActiveExceptions();
        if (allExceptions != null) {
            this.exceptions.addAll(allExceptions);
        }
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (this.exceptions.isEmpty()) {
            items.add(UItem.asCustom(createEmptyView(getContext())));
            return;
        }
        for (int i = 0; i < this.exceptions.size(); i++) {
            DialogExclusion exception = this.exceptions.get(i);
            items.add(UItem.asCustom(i, createExclusionCell(getContext(), exception, i)));
        }
        items.add(UItem.asShadow((CharSequence) null));
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private View createExclusionCell(Context context, final DialogExclusion exception, final int position) {
        ProfileSearchCell cell = new ProfileSearchCell(context);
        long dialogId = exception.dialogId;
        String statusText = buildStatusText(exception);
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(this.currentAccount).getUser(Long.valueOf(dialogId));
            cell.setData(user, (TLRPC.EncryptedChat) null, (CharSequence) null, statusText, false, false);
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(this.currentAccount).getChat(Long.valueOf(-dialogId));
            cell.setData(chat, (TLRPC.EncryptedChat) null, (CharSequence) null, statusText, false, false);
        }
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setOnClickListener(new View.OnClickListener() { 
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                lambda$createExclusionCell$0(exception, position, view);
            }
        });
        cell.setBackground(Theme.getSelectorDrawable(true));
        return cell;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createExclusionCell$0(DialogExclusion exception, int position, View v) {
        showOptionsMenu(exception, position);
    }

    private View createEmptyView(Context context) {
        TextInfoPrivacyCell cell = new TextInfoPrivacyCell(context);
        cell.setText(Localization.NO_EXLCUSIONS);
        cell.setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        return cell;
    }

    private void showOptionsMenu(final DialogExclusion exception, final int position) {
        String dialogName = getDialogName(exception.dialogId);
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(dialogName);
        CharSequence[] items = {Localization.OPEN_CHAT, Localization.EDIT_READ, Localization.EDIT_TYPING, Localization.DELETE_FROM_EXCLUSIONS};
        int[] icons = {R.drawable.msg_openprofile, R.drawable.msg_archive_hide, R.drawable.floating_pencil, R.drawable.msg_delete};
        builder.setItems(items, icons, new DialogInterface.OnClickListener() { 
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                lambda$showOptionsMenu$1(exception, position, dialogInterface, i);
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOptionsMenu$1(DialogExclusion exception, int position, DialogInterface dialog, int which) {
        switch (which) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                openChat(exception.dialogId);
                break;
            case Defaults.ALWAYS /* 1 */:
                showEditReadingDialog(exception, position);
                break;
            case 2:
                showEditTypingDialog(exception, position);
                break;
            case 3:
                showDeleteConfirmation(exception, position);
                break;
        }
    }

    private void openChat(long dialogId) {
        Bundle args = new Bundle();
        if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else {
            args.putLong("chat_id", -dialogId);
        }
        presentFragment(new ChatActivity(args));
    }

    private void showEditReadingDialog(final DialogExclusion exception, final int position) {
        new ExclusionUtils.ExclusionReadingDialog(getContext(), exception.dialogId, new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$showEditReadingDialog$2(exception, position);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditReadingDialog$2(DialogExclusion exception, int position) {
        DialogExclusion updated = ReExteraDb.get().getException(exception.dialogId);
        if (updated != null) {
            this.exceptions.set(position, updated);
            if (getAdapter() != null) {
                getAdapter().update(true);
            }
        }
    }

    private void showEditTypingDialog(final DialogExclusion exception, final int position) {
        new ExclusionUtils.ExclusionTypingDialog(getContext(), exception.dialogId, new Runnable() { 
            @Override // java.lang.Runnable
            public final void run() {
                lambda$showEditTypingDialog$3(exception, position);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditTypingDialog$3(DialogExclusion exception, int position) {
        DialogExclusion updated = ReExteraDb.get().getException(exception.dialogId);
        if (updated != null) {
            this.exceptions.set(position, updated);
            if (getAdapter() != null) {
                getAdapter().update(true);
            }
        }
    }

    private void showDeleteConfirmation(final DialogExclusion exception, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(Localization.DELETE_FROM_EXCLUSIONS);
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { 
            public final void onClick(AlertDialog alertDialog, int i) {
                lambda$showDeleteConfirmation$4(exception, position, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteConfirmation$4(DialogExclusion exception, int position, AlertDialog dialog, int which) {
        ReExteraDb.get().lambda$setDialogReadingAsync$4(exception.dialogId, 0);
        ReExteraDb.get().lambda$setDialogTypingAsync$5(exception.dialogId, 0);
        this.exceptions.remove(position);
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
    }

    private String getDialogName(long dialogId) {
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(this.currentAccount).getUser(Long.valueOf(dialogId));
            if (user != null) {
                return user.first_name + (user.last_name != null ? " " + user.last_name : "");
            }
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(this.currentAccount).getChat(Long.valueOf(-dialogId));
            if (chat != null) {
                return chat.title;
            }
        }
        return Localization.UNKNOWN_DIALOG;
    }

    private String buildStatusText(DialogExclusion exception) {
        ArrayList<String> parts = new ArrayList<>();
        if (exception.readExclusion != 0) {
            parts.add(Localization.READ_TO + getExclusionText(exception.readExclusion));
        }
        if (exception.typeExclusion != 0) {
            parts.add(Localization.TYPE_TO + getExclusionText(exception.typeExclusion));
        }
        if (parts.isEmpty()) {
            return Localization.NO_EXLCUSIONS;
        }
        return android.text.TextUtils.join(", ", parts);
    }

    private String getExclusionText(int exclusion) {
        switch (exclusion) {
            case Defaults.NEVER /* -1 */:
                return Localization.NEVER;
            case Defaults.GLOBAL_VALUE /* 0 */:
                return Localization.BASED_ON_GLOBAL;
            case Defaults.ALWAYS /* 1 */:
                return Localization.ALWAYS;
            default:
                return "(" + exclusion + ")";
        }
    }

    private UniversalAdapter getAdapter() {
        return this.listView.adapter;
    }
}
