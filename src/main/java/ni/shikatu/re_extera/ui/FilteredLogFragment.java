package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import java.util.ArrayList;
import java.util.List;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.utils.FilteredLogManager;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class FilteredLogFragment extends BasePreferencesActivity {
    private final ArrayList<FilteredLogManager.Entry> entries = new ArrayList<>();

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        if (this.actionBar != null) {
            this.actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            this.actionBar.setAllowOverlayTitle(true);
            this.actionBar.setTitle(Localization.FILTERED_LOG_TITLE);
            this.actionBar.createMenu().clearItems();
            this.actionBar.createMenu().addItem(1, R.drawable.msg_delete);
            this.actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == 1) {
                        showClearConfirmation();
                    }
                }
            });
        }
        return view;
    }

    @Override
    public String getTitle() {
        return Localization.FILTERED_LOG_TITLE;
    }

    @Override
    public boolean onFragmentCreate() {
        loadEntries();
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEntries();
        if (this.listView != null && this.listView.adapter != null) {
            this.listView.adapter.update(true);
        }
    }

    private void loadEntries() {
        this.entries.clear();
        this.entries.addAll(FilteredLogManager.getEntries());
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (this.entries.isEmpty()) {
            items.add(UItem.asShadow(Localization.FILTERED_LOG_EMPTY));
            return;
        }
        for (int i = 0; i < this.entries.size(); i++) {
            FilteredLogManager.Entry entry = this.entries.get(i);
            String snippet = entry.text;
            if (snippet.length() > 60) {
                snippet = snippet.substring(0, 57) + "...";
            }
            snippet = snippet.replace("\n", " ");

            StringBuilder filtersSub = new StringBuilder();
            for (int k = 0; k < entry.matchedPatterns.size(); k++) {
                if (k > 0) {
                    filtersSub.append(", ");
                }
                filtersSub.append(entry.matchedPatterns.get(k));
            }

            items.add(UItem.asButton(100 + i, snippet, filtersSub.toString()));
        }
        items.add(UItem.asShadow((CharSequence) null));
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        int idx = item.id - 100;
        if (idx >= 0 && idx < this.entries.size()) {
            showEntryDetails(this.entries.get(idx));
        }
    }

    private void showEntryDetails(final FilteredLogManager.Entry entry) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.FILTERED_LOG_TITLE);

        StringBuilder sb = new StringBuilder();
        sb.append(Localization.MATCHED_PATTERNS).append("\n");
        for (String p : entry.matchedPatterns) {
            sb.append("• ").append(p).append("\n");
        }
        sb.append("\n").append(entry.text);

        builder.setMessage(sb.toString());
        builder.setPositiveButton(LocaleController.getString(R.string.Copy), new AlertDialog.OnButtonClickListener() {
            @Override
            public void onClick(AlertDialog alertDialog, int i) {
                AndroidUtilities.addToClipboard(entry.text);
                BulletinFactory.of(FilteredLogFragment.this).createSimpleBulletin(
                        ContextCompat.getDrawable(getContext(), R.drawable.msg_copy),
                        Localization.COPIED
                ).show();
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }

    private void showClearConfirmation() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.CLEAR_LOG);
        builder.setMessage(Localization.CLEAR_LOG + "?");
        builder.setPositiveButton(Localization.YES, new AlertDialog.OnButtonClickListener() {
            @Override
            public void onClick(AlertDialog alertDialog, int i) {
                FilteredLogManager.clear();
                loadEntries();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        });
        builder.setNegativeButton(Localization.CANCEL, (AlertDialog.OnButtonClickListener) null);
        builder.show();
    }
}
