package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.exteragram.messenger.utils.text.LocaleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.MessageUtils;
import ni.shikatu.re_extera.utils.UItemUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class RegexFiltersFragment extends BasePreferencesActivity {
    private static final int ID_ENABLE_FILTERS = 1;
    private ArrayList<String> filters = new ArrayList<>();

    public View createView(Context context) {
        View view = super.createView(context);
        if (this.actionBar != null) {
            this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            this.actionBar.setAllowOverlayTitle(true);
            this.actionBar.setTitle(Localization.FILTERS);
            this.actionBar.createMenu().clearItems();
            this.actionBar.createMenu().addItem(1, R.drawable.msg_add);
            org.telegram.ui.ActionBar.ActionBarMenuItem otherItem = this.actionBar.createMenu().addItem(2, R.drawable.ic_ab_other);
            otherItem.addSubItem(10, R.drawable.msg_bot, Localization.TEST_FILTERS);
            otherItem.addSubItem(11, R.drawable.msg_log, Localization.FILTERED_LOG);
            otherItem.addSubItem(12, R.drawable.msg_download, Localization.IMPORT_FILTERS);
            otherItem.addSubItem(13, R.drawable.msg_share, Localization.EXPORT_FILTERS);

            this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // from class: ni.shikatu.re_extera.ui.RegexFiltersFragment.1
                public void onItemClick(int id) {
                    if (id == -1) {
                        RegexFiltersFragment.this.finishFragment();
                    } else if (id == 1) {
                        RegexFiltersFragment.this.showAddFilterDialog();
                    } else if (id == 10) {
                        RegexFiltersFragment.this.showTestFilterDialog();
                    } else if (id == 11) {
                        RegexFiltersFragment.this.presentFragment(new FilteredLogFragment());
                    } else if (id == 12) {
                        RegexFiltersFragment.this.showImportOptions();
                    } else if (id == 13) {
                        RegexFiltersFragment.this.showExportOptions();
                    }
                }
            });
        }
        return view;
    }

    public String getTitle() {
        return Localization.FILTERS;
    }

    public boolean onFragmentCreate() {
        loadFilters();
        return super.onFragmentCreate();
    }

    public void onResume() {
        super.onResume();
        loadFilters();
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
    }

    private void loadFilters() {
        this.filters.clear();
        List<String> dbFilters = ReExteraDb.get().getAllRegexFilters();
        if (dbFilters != null) {
            this.filters.addAll(dbFilters);
        }
    }

    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItemUtils.setLinkAlias(UItem.asCheck(1, Localization.ENABLE_FILTERS).setChecked(Settings.getFiltersEnabled()), "reExteraFiltersEnable", this));
        items.add(UItem.asButton(2, Localization.TEST_FILTERS, Localization.TEST_FILTERS_DESC));
        items.add(UItem.asButton(3, Localization.FILTERED_LOG));
        items.add(UItem.asShadow(LocaleUtils.fullyFormatText(Localization.FILTERS_ABOUT)));
        for (int i = 0; i < this.filters.size(); i++) {
            String filter = this.filters.get(i);
            UItem item = UItem.asButton(i + 100, filter);
            items.add(item);
        }
        items.add(UItem.asShadow((CharSequence) null));
    }

    public void onClick(UItem item, View view, int position, float x, float y) {
        int filterIndex;
        if (item.id == 1) {
            Settings.setFiltersEnabled(!Settings.getFiltersEnabled());
            if (getAdapter() != null) {
                getAdapter().update(true);
            }
            return;
        }
        if (item.id == 2) {
            showTestFilterDialog();
            return;
        }
        if (item.id == 3) {
            presentFragment(new FilteredLogFragment());
            return;
        }
        if (item.id >= 100 && (filterIndex = item.id - 100) >= 0 && filterIndex < this.filters.size()) {
            showOptionsMenu(this.filters.get(filterIndex), filterIndex);
        }
    }

    public boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int index = item.id - 100;
        if (index >= 0 && index < this.filters.size()) {
            showDeleteConfirmation(index);
            return true;
        }
        return false;
    }

    private void showOptionsMenu(final String filter, final int position) {
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(filter);
        CharSequence[] items = {Localization.EDIT_REGEX_FILTER, Localization.COPY_FILTER, Localization.DELETE_FILTER};
        int[] icons = {R.drawable.floating_pencil, R.drawable.msg_copy, R.drawable.msg_delete};
        builder.setItems(items, icons, new DialogInterface.OnClickListener() { 
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                lambda$showOptionsMenu$0(filter, position, dialogInterface, i);
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showOptionsMenu$0(String filter, int position, DialogInterface dialog, int which) {
        switch (which) {
            case Defaults.GLOBAL_VALUE /* 0 */:
                showEditFilterDialog(filter, position);
                break;
            case 1:
                if (AndroidUtilities.addToClipboard(filter)) {
                    BulletinFactory.of(this).createSimpleBulletin(ContextCompat.getDrawable(getContext(), R.drawable.msg_copy), Localization.COPIED).show();
                }
                break;
            case 2:
                showDeleteConfirmation(position);
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAddFilterDialog() {
        showFilterDialog(null, -1, false);
    }

    private void showEditFilterDialog(String existingFilter, int position) {
        showFilterDialog(existingFilter, position, true);
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
        builder.setPositiveButton(isEdit ? Localization.SAVE : Localization.ADD, new AlertDialog.OnButtonClickListener() { 
            public final void onClick(AlertDialog alertDialog, int i) {
                lambda$showFilterDialog$1(editText, context, isEdit, existingFilter, position, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { 
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                RegexFiltersFragment.lambda$showFilterDialog$2(editText, dialogInterface);
            }
        });
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showFilterDialog$1(EditTextBoldCursor editText, Context context, boolean isEdit, String existingFilter, int position, AlertDialog dialog, int which) {
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
            } else {
                ReExteraDb.get().addRegexFilter(regex);
                this.filters.add(regex);
            }
            MessageUtils.updatePatterns();
            if (getAdapter() != null) {
                getAdapter().update(true);
            }
        } catch (PatternSyntaxException e) {
            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(context);
            errorBuilder.setTitle(Localization.PATTERN_ERROR);
            errorBuilder.setMessage(e.getMessage());
            errorBuilder.setPositiveButton(Localization.YES, (AlertDialog.OnButtonClickListener) null);
            errorBuilder.show();
        }
    }

    static /* synthetic */ void lambda$showFilterDialog$2(EditTextBoldCursor editText, DialogInterface d) {
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }

    private void showDeleteConfirmation(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(Localization.DELETE_FILTER);
        builder.setMessage(Localization.DELETE_FILTER_ABOUT);
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() { 
            public final void onClick(AlertDialog alertDialog, int i) {
                lambda$showDeleteConfirmation$3(position, alertDialog, i);
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDeleteConfirmation$3(int position, AlertDialog dialog, int which) {
        String filter = this.filters.get(position);
        ReExteraDb.get().deleteRegexFilter(filter);
        this.filters.remove(position);
        MessageUtils.updatePatterns();
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
    }

    private UniversalAdapter getAdapter() {
        return this.listView.adapter;
    }

    public void showTestFilterDialog() {
        showFilterTesterDialog(getParentActivity(), "");
    }

    public static void showFilterTesterDialog(Context context, String initialText) {
        if (context == null) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(Localization.TEST_FILTERS);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));

        android.widget.EditText input = new android.widget.EditText(context);
        input.setHint(Localization.TEST_FILTERS_HINT);
        input.setText(initialText != null ? initialText : "");
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        input.setMaxLines(6);
        layout.addView(input);

        android.widget.TextView resultView = new android.widget.TextView(context);
        resultView.setTextSize(14);
        resultView.setPadding(0, AndroidUtilities.dp(10), 0, 0);
        resultView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(resultView);

        Runnable checkAction = () -> {
            String text = input.getText().toString();
            if (text.isEmpty()) {
                resultView.setText(Localization.TEST_FILTERS_HINT);
                return;
            }
            List<String> matches = MessageUtils.getMatchingFilters(text);
            if (matches.isEmpty()) {
                resultView.setText(Localization.TEST_FILTERS_NO_MATCH);
                resultView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText));
            } else {
                StringBuilder sb = new StringBuilder();
                for (String m : matches) {
                    sb.append("• ").append(m).append("\n");
                }
                resultView.setText(String.format(Localization.TEST_FILTERS_MATCHED, matches.size(), sb.toString().trim()));
                resultView.setTextColor(Theme.getColor(Theme.key_text_RedBold));
            }
        };

        if (initialText != null && !initialText.isEmpty()) {
            checkAction.run();
        }

        builder.setView(layout);
        builder.setPositiveButton(Localization.TEST_FILTERS, null);
        builder.setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Close), (dialog, which) -> dialog.dismiss());
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> checkAction.run());
    }

    public void showImportOptions() {
        Context context = getParentActivity();
        if (context == null) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(context);
        builder.setTitle(Localization.IMPORT_FILTERS);
        CharSequence[] items = {Localization.FROM_CLIPBOARD, Localization.IMPORT_FILTERS};
        int[] icons = {R.drawable.msg_copy, R.drawable.msg_edit};
        builder.setItems(items, icons, (dialog, which) -> {
            if (which == 0) {
                importFromClipboard();
            } else if (which == 1) {
                showPasteImportDialog();
            }
        });
        builder.show();
    }

    private void importFromClipboard() {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getParentActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip() || clipboard.getPrimaryClip().getItemCount() == 0) {
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, Localization.IMPORT_FILTERS_EMPTY).show();
                return;
            }
            CharSequence clipText = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (clipText == null || clipText.toString().trim().isEmpty()) {
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, Localization.IMPORT_FILTERS_EMPTY).show();
                return;
            }
            processImportContent(clipText.toString());
        } catch (Exception e) {
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, e.getMessage()).show();
        }
    }

    private void showPasteImportDialog() {
        Context context = getParentActivity();
        if (context == null) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(Localization.IMPORT_FILTERS);

        android.widget.FrameLayout container = new android.widget.FrameLayout(context);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        android.widget.EditText input = new android.widget.EditText(context);
        input.setHint(Localization.TEST_FILTERS_HINT);
        input.setMaxLines(10);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        container.addView(input);

        builder.setView(container);
        builder.setPositiveButton(Localization.SAVE, (dialog, which) -> {
            String content = input.getText().toString();
            processImportContent(content);
        });
        builder.setNegativeButton(Localization.CANCEL, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void processImportContent(String content) {
        List<String> parsed = ni.shikatu.re_extera.utils.FilterImportExportUtils.parseFilters(content);
        if (parsed.isEmpty()) {
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, Localization.IMPORT_FILTERS_EMPTY).show();
            return;
        }
        int added = ReExteraDb.get().addRegexFiltersBatch(parsed);
        MessageUtils.updatePatterns();
        loadFilters();
        if (getAdapter() != null) {
            getAdapter().update(true);
        }
        BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, String.format(Localization.IMPORT_FILTERS_SUCCESS, added)).show();
    }

    public void showExportOptions() {
        Context context = getParentActivity();
        if (context == null) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(context);
        builder.setTitle(Localization.EXPORT_FILTERS);
        CharSequence[] items = {"JSON", "TXT"};
        int[] icons = {R.drawable.msg_copy, R.drawable.msg_copy};
        builder.setItems(items, icons, (dialog, which) -> {
            if (which == 0) {
                String json = ni.shikatu.re_extera.utils.FilterImportExportUtils.exportToJson(this.filters);
                AndroidUtilities.addToClipboard(json);
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, Localization.COPIED).show();
            } else if (which == 1) {
                String txt = ni.shikatu.re_extera.utils.FilterImportExportUtils.exportToPlainText(this.filters);
                AndroidUtilities.addToClipboard(txt);
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, Localization.COPIED).show();
            }
        });
        builder.show();
    }
}
