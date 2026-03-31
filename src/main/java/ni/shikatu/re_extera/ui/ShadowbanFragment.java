package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import java.util.ArrayList;
import java.util.List;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.db.ShadowbanEntry;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.ShadowbanCache;
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

public class ShadowbanFragment extends BasePreferencesActivity {
    private ArrayList<ShadowbanEntry> shadowbanned = new ArrayList<>();

    public View createView(Context context) {
        View view = super.createView(context);
        if (this.actionBar != null) {
            this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            this.actionBar.setTitle(Localization.SHADOWBAN);
            this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.ShadowbanFragment.1
                public void onItemClick(int id) {
                    if (id == -1) {
                        ShadowbanFragment.this.finishFragment();
                    }
                }
            });
        }
        return view;
    }

    public String getTitle() {
        return Localization.SHADOWBAN;
    }

    public boolean onFragmentCreate() {
        loadShadowbanned();
        return super.onFragmentCreate();
    }

    public void onResume() {
        super.onResume();
        loadShadowbanned();
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
    }

    private void loadShadowbanned() {
        this.shadowbanned.clear();
        List<ShadowbanEntry> all = ReExteraDb.get().getAllShadowbanned();
        if (all != null) {
            this.shadowbanned.addAll(all);
        }
    }

    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (this.shadowbanned.isEmpty()) {
            items.add(UItem.asCustom(createEmptyView(getContext())));
            return;
        }
        for (int i = 0; i < this.shadowbanned.size(); i++) {
            ShadowbanEntry entry = this.shadowbanned.get(i);
            View cell = createShadowbanCell(getContext(), entry, i);
            items.add(UItem.asCustom(i, cell));
        }
        items.add(UItem.asShadow((CharSequence) null));
    }

    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private View createShadowbanCell(Context context, final ShadowbanEntry entry, final int position) {
        ProfileSearchCell cell = new ProfileSearchCell(context);
        long userId = entry.userId;
        String statusText = buildStatusText(entry);
        TLRPC.User user = MessagesController.getInstance(this.currentAccount).getUser(Long.valueOf(userId));
        cell.setData(user, (TLRPC.EncryptedChat) null, (CharSequence) null, statusText, false, false);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.ui.ShadowbanFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$createShadowbanCell$0(entry, position, view);
            }
        });
        cell.setBackground(Theme.getSelectorDrawable(true));
        return cell;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createShadowbanCell$0(ShadowbanEntry entry, int position, View v) {
        showOptionsMenu(entry, position);
    }

    private View createEmptyView(Context context) {
        TextInfoPrivacyCell cell = new TextInfoPrivacyCell(context);
        cell.setText(Localization.NO_SHADOWBANNED);
        cell.setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        return cell;
    }

    private void showOptionsMenu(final ShadowbanEntry entry, final int position) {
        String userName;
        TLRPC.User user = MessagesController.getInstance(this.currentAccount).getUser(Long.valueOf(entry.userId));
        if (user != null) {
            userName = user.first_name + (user.last_name != null ? " " + user.last_name : "");
        } else {
            userName = Localization.UNKNOWN_DIALOG;
        }
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(userName);
        CharSequence[] items = {Localization.OPEN_CHAT, Localization.EDIT_SHADOWBAN, Localization.REMOVE_FROM_SHADOWBAN};
        int[] icons = {R.drawable.msg_openprofile, R.drawable.floating_pencil, R.drawable.msg_delete};
        builder.setItems(items, icons, new DialogInterface.OnClickListener() { // from class: ni.shikatu.re_extera.ui.ShadowbanFragment$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.lambda$showOptionsMenu$1(entry, position, dialogInterface, i);
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOptionsMenu$1(ShadowbanEntry entry, int position, DialogInterface dialog, int which) {
        switch (which) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                openChat(entry.userId);
                break;
            case Defaults.ALWAYS /* 1 */:
                showEditDialog(entry, position);
                break;
            case 2:
                showDeleteConfirmation(entry, position);
                break;
        }
    }

    private void openChat(long userId) {
        Bundle args = new Bundle();
        args.putLong("user_id", userId);
        presentFragment(new ChatActivity(args));
    }

    private void showEditDialog(final ShadowbanEntry entry, int position) {
        ShadowbanDialog.showEdit(getContext(), entry, new ShadowbanDialog.OnShadowbanResult() { // from class: ni.shikatu.re_extera.ui.ShadowbanFragment$$ExternalSyntheticLambda0
            @Override // ni.shikatu.re_extera.ui.ShadowbanDialog.OnShadowbanResult
            public final void onResult(boolean z, boolean z2) {
                this.f$0.lambda$showEditDialog$2(entry, z, z2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showEditDialog$2(ShadowbanEntry entry, boolean hideDialog, boolean hideInGroups) {
        ShadowbanCache.update(entry.userId, hideDialog, hideInGroups);
        loadShadowbanned();
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
        ShadowbanCache.notifyDialogsUpdate(getCurrentAccount());
    }

    private void showDeleteConfirmation(final ShadowbanEntry entry, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(Localization.REMOVE_FROM_SHADOWBAN);
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.ui.ShadowbanFragment$$ExternalSyntheticLambda2
            public final void onClick(AlertDialog alertDialog, int i) {
                this.f$0.lambda$showDeleteConfirmation$3(entry, position, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteConfirmation$3(ShadowbanEntry entry, int position, AlertDialog dialog, int which) {
        ShadowbanCache.remove(entry.userId);
        this.shadowbanned.remove(position);
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
        ShadowbanCache.notifyDialogsUpdate(getCurrentAccount());
    }

    private String buildStatusText(ShadowbanEntry entry) {
        ArrayList<String> parts = new ArrayList<>();
        if (entry.hideDialog) {
            parts.add(Localization.HIDE_DIALOG);
        }
        if (entry.hideInGroups) {
            parts.add(Localization.HIDE_IN_GROUPS);
        }
        if (parts.isEmpty()) {
            return "";
        }
        return ExclusionsFragment$$ExternalSyntheticBackport0.m(", ", parts);
    }

    private UniversalAdapter getAdapter() {
        return this.listView.adapter;
    }
}
