package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.exteragram.messenger.utils.text.LocaleUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class RegexFiltersFragment extends BaseFragment {
    private FiltersAdapter adapter;
    private ArrayList<String> filters = new ArrayList<>();
    private RecyclerListView listView;

    public boolean onFragmentCreate() throws IllegalAccessException, InvocationTargetException {
        loadFilters();
        return super.onFragmentCreate();
    }

    public View createView(Context context) {
        this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        this.actionBar.setAllowOverlayTitle(true);
        this.actionBar.setTitle(Localization.FILTERS);
        this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment.1
            public void onItemClick(int id) {
                if (id == -1) {
                    RegexFiltersFragment.this.finishFragment();
                } else if (id == 1) {
                    RegexFiltersFragment.this.showAddFilterDialog();
                }
            }
        });
        this.actionBar.createMenu().addItemWithWidth(1, R.drawable.msg_add, AndroidUtilities.dp(56.0f));
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        this.listView = new RecyclerListView(context);
        this.listView.setLayoutManager(new LinearLayoutManager(context));
        this.adapter = new FiltersAdapter(context);
        this.listView.setAdapter(this.adapter);
        this.listView.setVerticalScrollBarEnabled(false);
        this.listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment$$ExternalSyntheticLambda3
            public final void onItemClick(View view, int i) {
                this.f$0.lambda$createView$0(view, i);
            }
        });
        this.listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment$$ExternalSyntheticLambda4
            public final boolean onItemClick(View view, int i) {
                return this.f$0.lambda$createView$1(view, i);
            }
        });
        frameLayout.addView((View) this.listView, (ViewGroup.LayoutParams) LayoutHelper.createFrame(-1, -1.0f));
        this.fragmentView = frameLayout;
        return this.fragmentView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createView$0(View view, int position) {
        int index = position - 2;
        if (index >= 0 && index < this.filters.size()) {
            String filter = this.filters.get(index);
            showOptionsMenu(filter, index);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$createView$1(View view, int position) {
        int index = position - 2;
        if (index >= 0 && index < this.filters.size()) {
            showDeleteConfirmation(index);
            return true;
        }
        return false;
    }

    public void onResume() throws IllegalAccessException, InvocationTargetException {
        super.onResume();
        loadFilters();
        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        }
    }

    private void loadFilters() throws IllegalAccessException, InvocationTargetException {
        this.filters.clear();
        List<String> dbFilters = ReExteraDb.get().getAllRegexFilters();
        if (dbFilters != null) {
            this.filters.addAll(dbFilters);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAddFilterDialog() {
        showFilterDialog(null, -1, false);
    }

    private void showEditFilterDialog(String existingFilter, int position) {
        showFilterDialog(existingFilter, position, true);
    }

    private void showOptionsMenu(final String filter, final int position) {
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(filter);
        CharSequence[] items = {Localization.EDIT_REGEX_FILTER, Localization.COPY_FILTER, Localization.DELETE_FILTER};
        int[] icons = {R.drawable.floating_pencil, R.drawable.msg_copy, R.drawable.msg_delete};
        builder.setItems(items, icons, new DialogInterface.OnClickListener() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment$$ExternalSyntheticLambda5
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.lambda$showOptionsMenu$2(filter, position, dialogInterface, i);
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOptionsMenu$2(String filter, int position, DialogInterface dialog, int which) {
        switch (which) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                showEditFilterDialog(filter, position);
                break;
            case Defaults.ALWAYS /* 1 */:
                if (AndroidUtilities.addToClipboard(filter)) {
                    BulletinFactory.of(this).createSimpleBulletin(ContextCompat.getDrawable(getContext(), R.drawable.msg_copy), Localization.COPIED).show();
                }
                break;
            case 2:
                showDeleteConfirmation(position);
                break;
        }
    }

    private void showFilterDialog(final String existingFilter, final int position, final boolean isEdit) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(isEdit ? Localization.EDIT_REGEX_FILTER : Localization.ADD_REGEX_FILTER);
        FrameLayout container = new FrameLayout(context);
        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setBackground(Theme.createEditTextDrawable(context, false));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setTextSize(18.0f);
        editText.setHint(Localization.EDIT_REGEX_PATTERN);
        editText.setInputType(1);
        editText.setPadding(AndroidUtilities.dp(16.0f), AndroidUtilities.dp(8.0f), AndroidUtilities.dp(16.0f), AndroidUtilities.dp(8.0f));
        if (existingFilter != null) {
            editText.setText(existingFilter);
            editText.setSelection(existingFilter.length());
        }
        container.addView((View) editText, (ViewGroup.LayoutParams) LayoutHelper.createFrame(-1, -2.0f, 0, 24.0f, 12.0f, 24.0f, 0.0f));
        builder.setView(container);
        builder.setPositiveButton(isEdit ? Localization.SAVE : Localization.ADD, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment$$ExternalSyntheticLambda1
            public final void onClick(AlertDialog alertDialog, int i) throws IllegalAccessException, InvocationTargetException {
                this.f$0.lambda$showFilterDialog$3(editText, context, isEdit, existingFilter, position, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                RegexFiltersFragment.lambda$showFilterDialog$4(editText, dialogInterface);
            }
        });
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showFilterDialog$3(EditTextBoldCursor editText, Context context, boolean isEdit, String existingFilter, int position, AlertDialog dialog, int which) throws IllegalAccessException, InvocationTargetException {
        String regex = editText.getText().toString().trim();
        if (regex.isEmpty()) {
            AndroidUtilities.shakeView(editText);
            return;
        }
        try {
            Pattern.compile(regex);
            if (isEdit) {
                ReExteraDb.get().updateRegexFilter(existingFilter, regex);
                this.filters.set(position, regex);
                this.adapter.notifyItemChanged(position + 2);
            } else {
                ReExteraDb.get().addRegexFilter(regex);
                this.filters.add(regex);
                this.adapter.notifyItemInserted((this.filters.size() - 1) + 2);
            }
        } catch (PatternSyntaxException e) {
            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(context);
            errorBuilder.setTitle(Localization.PATTERN_ERROR);
            errorBuilder.setMessage(e.getMessage());
            errorBuilder.setPositiveButton(Localization.YES, (AlertDialog.OnButtonClickListener) null);
            errorBuilder.show();
        }
    }

    static /* synthetic */ void lambda$showFilterDialog$4(EditTextBoldCursor editText, DialogInterface d) {
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }

    private void showDeleteConfirmation(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(Localization.DELETE_FILTER);
        builder.setMessage(Localization.DELETE_FILTER_ABOUT);
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment$$ExternalSyntheticLambda0
            public final void onClick(AlertDialog alertDialog, int i) {
                this.f$0.lambda$showDeleteConfirmation$5(position, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteConfirmation$5(int position, AlertDialog dialog, int which) {
        String filter = this.filters.get(position);
        ReExteraDb.get().deleteRegexFilter(filter);
        this.filters.remove(position);
        this.adapter.notifyItemRemoved(position + 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    class FiltersAdapter extends RecyclerListView.SelectionAdapter {
        private static final int VIEW_TYPE_ENABLE = 0;
        private static final int VIEW_TYPE_FILTER = 2;
        private static final int VIEW_TYPE_INFO = 1;
        private final Context context;

        FiltersAdapter(Context context) {
            this.context = context;
        }

        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == 0 || position >= VIEW_TYPE_FILTER;
        }

        public int getItemViewType(int position) {
            if (position == 0) {
                return 0;
            }
            if (position == 1) {
                return 1;
            }
            return VIEW_TYPE_FILTER;
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextCheckCell cell;
            if (viewType == 0) {
                cell = new TextCheckCell(this.context);
                cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == 1) {
                cell = new TextInfoPrivacyCell(this.context);
            } else {
                cell = new TextSettingsCell(this.context);
                cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            return new RecyclerListView.Holder(cell);
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == 0) {
                final TextCheckCell cell = holder.itemView;
                cell.setTextAndCheck(Localization.ENABLE_FILTERS, Settings.getFiltersEnabled(), false);
                cell.setOnClickListener(new View.OnClickListener() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment$FiltersAdapter$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        RegexFiltersFragment.FiltersAdapter.lambda$onBindViewHolder$0(cell, view);
                    }
                });
            } else {
                if (viewType == 1) {
                    TextInfoPrivacyCell cell2 = holder.itemView;
                    cell2.setText(LocaleUtils.fullyFormatText(Localization.FILTERS_ABOUT));
                    return;
                }
                int index = position - 2;
                if (index >= 0 && index < RegexFiltersFragment.this.filters.size()) {
                    TextSettingsCell cell3 = holder.itemView;
                    String filter = (String) RegexFiltersFragment.this.filters.get(index);
                    boolean divider = index < RegexFiltersFragment.this.filters.size() - 1;
                    cell3.setText(filter, divider);
                }
            }
        }

        static /* synthetic */ void lambda$onBindViewHolder$0(TextCheckCell cell, View v) {
            Settings.setFiltersEnabled(!Settings.getFiltersEnabled());
            cell.setChecked(Settings.getFiltersEnabled());
        }

        public int getItemCount() {
            return RegexFiltersFragment.this.filters.size() + VIEW_TYPE_FILTER;
        }
    }
}
