package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.reflect.InvocationTargetException;
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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class ExclusionsFragment extends BaseFragment {
    private ExceptionsAdapter adapter;
    private ArrayList<DialogExclusion> exceptions = new ArrayList<>();
    private RecyclerListView listView;

    public boolean onFragmentCreate() throws IllegalAccessException, InvocationTargetException {
        loadExceptions();
        return super.onFragmentCreate();
    }

    public View createView(Context context) {
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(true);
        this.actionBar.setTitle(Localization.EXCLUSIONS);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.ExclusionsFragment.1
            public void onItemClick(int id) {
                if (id == -1) {
                    ExclusionsFragment.this.finishFragment();
                }
            }
        });
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        this.listView = new RecyclerListView(context);
        this.listView.setLayoutManager(new LinearLayoutManager(context));
        this.adapter = new ExceptionsAdapter(context);
        this.listView.setAdapter(this.adapter);
        this.listView.setVerticalScrollBarEnabled(false);
        this.listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() { // from class: ni.shikatu.re_extera.ui.ExclusionsFragment$$ExternalSyntheticLambda0
            public final void onItemClick(View view, int i) {
                this.f$0.lambda$createView$0(view, i);
            }
        });
        frameLayout.addView((View) this.listView, (ViewGroup.LayoutParams) LayoutHelper.createFrame(-1, -1.0f));
        this.fragmentView = frameLayout;
        return this.fragmentView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createView$0(View view, int position) {
        if (position >= 0 && position < this.exceptions.size()) {
            DialogExclusion exception = this.exceptions.get(position);
            showOptionsMenu(exception, position);
        }
    }

    public void onResume() throws IllegalAccessException, InvocationTargetException {
        super.onResume();
        loadExceptions();
        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        }
    }

    private void loadExceptions() throws IllegalAccessException, InvocationTargetException {
        this.exceptions.clear();
        List<DialogExclusion> allExceptions = ReExteraDb.get().getActiveExceptions();
        if (allExceptions != null) {
            this.exceptions.addAll(allExceptions);
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

    private void showOptionsMenu(final DialogExclusion exception, final int position) {
        String dialogName = getDialogName(exception.dialogId);
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(dialogName);
        CharSequence[] items = {Localization.OPEN_CHAT, Localization.EDIT_READ, Localization.EDIT_TYPING, Localization.DELETE_FROM_EXCLUSIONS};
        int[] icons = {R.drawable.msg_openprofile, R.drawable.msg_archive_hide, R.drawable.floating_pencil, R.drawable.msg_delete};
        builder.setItems(items, icons, new DialogInterface.OnClickListener() { // from class: ni.shikatu.re_extera.ui.ExclusionsFragment$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) throws IllegalAccessException, InvocationTargetException {
                this.f$0.lambda$showOptionsMenu$1(exception, position, dialogInterface, i);
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOptionsMenu$1(DialogExclusion exception, int position, DialogInterface dialog, int which) throws IllegalAccessException, InvocationTargetException {
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

    private void showEditReadingDialog(final DialogExclusion exception, final int position) throws IllegalAccessException, InvocationTargetException {
        new ExclusionUtils.ExclusionReadingDialog(getContext(), exception.dialogId, new Runnable() { // from class: ni.shikatu.re_extera.ui.ExclusionsFragment$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() throws IllegalAccessException, InvocationTargetException {
                this.f$0.lambda$showEditReadingDialog$2(exception, position);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditReadingDialog$2(DialogExclusion exception, int position) throws IllegalAccessException, InvocationTargetException {
        DialogExclusion updated = ReExteraDb.get().getException(exception.dialogId);
        if (updated != null) {
            this.exceptions.set(position, updated);
            this.adapter.notifyItemChanged(position);
        }
    }

    private void showEditTypingDialog(final DialogExclusion exception, final int position) throws IllegalAccessException, InvocationTargetException {
        new ExclusionUtils.ExclusionTypingDialog(getContext(), exception.dialogId, new Runnable() { // from class: ni.shikatu.re_extera.ui.ExclusionsFragment$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() throws IllegalAccessException, InvocationTargetException {
                this.f$0.lambda$showEditTypingDialog$3(exception, position);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditTypingDialog$3(DialogExclusion exception, int position) throws IllegalAccessException, InvocationTargetException {
        DialogExclusion updated = ReExteraDb.get().getException(exception.dialogId);
        if (updated != null) {
            this.exceptions.set(position, updated);
            this.adapter.notifyItemChanged(position);
        }
    }

    private void showDeleteConfirmation(final DialogExclusion exception, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(Localization.DELETE_FROM_EXCLUSIONS);
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.ui.ExclusionsFragment$$ExternalSyntheticLambda3
            public final void onClick(AlertDialog alertDialog, int i) throws IllegalAccessException, InvocationTargetException {
                this.f$0.lambda$showDeleteConfirmation$4(exception, position, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteConfirmation$4(DialogExclusion exception, int position, AlertDialog dialog, int which) throws IllegalAccessException, InvocationTargetException {
        ReExteraDb.get().setDialogReading(exception.dialogId, 0);
        ReExteraDb.get().setDialogTyping(exception.dialogId, 0);
        this.exceptions.remove(position);
        this.adapter.notifyItemRemoved(position);
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

    private class ExceptionsAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        ExceptionsAdapter(Context context) {
            this.context = context;
        }

        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            UserCell userCell = new UserCell(this.context, 0, 0, false);
            userCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            return new RecyclerListView.Holder(userCell);
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position < ExclusionsFragment.this.exceptions.size()) {
                UserCell cell = holder.itemView;
                DialogExclusion exception = (DialogExclusion) ExclusionsFragment.this.exceptions.get(position);
                long dialogId = exception.dialogId;
                String statusText = buildStatusText(exception);
                boolean divider = position < ExclusionsFragment.this.exceptions.size() - 1;
                if (DialogObject.isUserDialog(dialogId)) {
                    TLRPC.User user = MessagesController.getInstance(ExclusionsFragment.this.currentAccount).getUser(Long.valueOf(dialogId));
                    if (user != null) {
                        cell.setData(user, (CharSequence) null, statusText, 0, divider);
                        return;
                    }
                    return;
                }
                TLRPC.Chat chat = MessagesController.getInstance(ExclusionsFragment.this.currentAccount).getChat(Long.valueOf(-dialogId));
                if (chat != null) {
                    cell.setData(chat, (CharSequence) null, statusText, 0, divider);
                }
            }
        }

        public int getItemCount() {
            return ExclusionsFragment.this.exceptions.size();
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
            return ExclusionsFragment$ExceptionsAdapter$$ExternalSyntheticBackport0.m(", ", parts);
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
    }
}
