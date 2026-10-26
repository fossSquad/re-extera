package ni.shikatu.re_extera.ui;

import android.content.Context;
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
import ni.shikatu.re_extera.utils.Res;
import ni.shikatu.re_extera.utils.UItemUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class DeletedExclusionsFragment extends BasePreferencesActivity {
    private final ArrayList<DialogExclusion> exclusions = new ArrayList<>();

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        if (actionBar != null) {
            actionBar.setBackButtonImage(Res.drawable("ic_ab_back", R.drawable.ic_ab_back));
            actionBar.setTitle(Localization.EXCLUSIONS);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });
        }
        return view;
    }

    @Override
    public String getTitle() {
        return Localization.EXCLUSIONS;
    }

    @Override
    public boolean onFragmentCreate() {
        loadExclusions();
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExclusions();
        updateItems();
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItemUtils.setLinkAlias(UItem.asButton(1, Localization.ADD), "addDeletedExclusion", this));
        items.add(UItem.asShadow((CharSequence) null));
        if (exclusions.isEmpty()) {
            TextInfoPrivacyCell empty = new TextInfoPrivacyCell(getContext());
            empty.setText(Localization.NO_EXLCUSIONS);
            empty.setBackgroundDrawable(Theme.getThemedDrawable(getContext(), Res.drawable("greydivider_bottom", R.drawable.greydivider_bottom), Theme.key_windowBackgroundGrayShadow));
            items.add(UItem.asCustom(empty));
            return;
        }
        for (int i = 0; i < exclusions.size(); i++) {
            items.add(UItem.asCustom(i + 100, createCell(getContext(), exclusions.get(i), i)));
        }
        items.add(UItem.asShadow((CharSequence) null));
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 1) {
            pickDialog();
        }
    }

    private void pickDialog() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", 3);
        org.telegram.ui.DialogsActivity picker = new org.telegram.ui.DialogsActivity(args);
        picker.setDelegate(new org.telegram.ui.DialogsActivity.DialogsActivityDelegate() {
            @Override
            public boolean didSelectDialogs(org.telegram.ui.DialogsActivity dialogsActivity, ArrayList<MessagesStorage.TopicKey> selected, CharSequence comment, boolean notify, boolean schedule, int scheduleDate, int topicsCount, org.telegram.ui.TopicsFragment topicsFragment) {
                if (selected != null && !selected.isEmpty()) {
                    long did = selected.get(0).dialogId;
                    ReExteraDb.get().setDialogDeletedAsync(did, Defaults.NEVER);
                    AndroidUtilities.runOnUIThread(() -> {
                        loadExclusions();
                        updateItems();
                    }, 200);
                }
                dialogsActivity.finishFragment();
                return true;
            }
        });
        presentFragment(picker);
    }

    private View createCell(Context context, DialogExclusion exclusion, int position) {
        ProfileSearchCell cell = new ProfileSearchCell(context);
        long dialogId = exclusion.dialogId;
        String statusText = getStatusText(exclusion.deletedExclusion);
        if (DialogObject.isUserDialog(dialogId)) {
            cell.setData(MessagesController.getInstance(currentAccount).getUser(dialogId), null, null, statusText, false, false);
        } else {
            cell.setData(MessagesController.getInstance(currentAccount).getChat(-dialogId), null, null, statusText, false, false);
        }
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setBackground(Theme.getSelectorDrawable(true));
        cell.setOnClickListener(v -> showOptions(exclusion, position));
        return cell;
    }

    private String getStatusText(int mode) {
        if (mode == Defaults.NEVER) {
            return Localization.NEVER + Localization.NOT + Localization.SAVE_DELETED;
        } else if (mode == Defaults.ALWAYS) {
            return Localization.ALWAYS + " " + Localization.SAVE_DELETED;
        }
        return Localization.BASED_ON_GLOBAL;
    }

    private void showOptions(DialogExclusion exclusion, int position) {
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(getDialogName(exclusion.dialogId));
        builder.setItems(
            new CharSequence[]{Localization.OPEN_CHAT, Localization.EDIT_DELETED, Localization.DELETE_FROM_EXCLUSIONS},
            new int[]{Res.drawable("msg_openprofile", R.drawable.msg_openprofile), Res.drawable("msg_delete", R.drawable.msg_delete), Res.drawable("msg_delete", R.drawable.msg_delete)},
            (dialog, which) -> handleOption(exclusion, position, which)
        );
        builder.show();
    }

    private void handleOption(DialogExclusion exclusion, int position, int which) {
        if (which == 0) {
            openChat(exclusion.dialogId);
        } else if (which == 1) {
            new ExclusionUtils.ExclusionDeletedDialog(getContext(), exclusion.dialogId, () -> {
                loadExclusions();
                updateItems();
            }).show();
        } else {
            ReExteraDb.get().setDialogDeletedAsync(exclusion.dialogId, Defaults.GLOBAL_VALUE);
            exclusions.remove(position);
            updateItems();
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

    private String getDialogName(long dialogId) {
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user != null) {
                return user.first_name + (user.last_name != null ? " " + user.last_name : "");
            }
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null) {
                return chat.title;
            }
        }
        return Localization.UNKNOWN_DIALOG;
    }

    private void loadExclusions() {
        exclusions.clear();
        List<DialogExclusion> all = ReExteraDb.get().getActiveExceptions();
        if (all == null) {
            return;
        }
        for (DialogExclusion exclusion : all) {
            if (exclusion.deletedExclusion != 0) {
                exclusions.add(exclusion);
            }
        }
    }

    private void updateItems() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
