package ni.shikatu.re_extera.settings.newui;

import com.exteragram.messenger.preferences.BasePreferencesActivity;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.UItem;

public abstract class BasePreferencesActivityExtended extends BasePreferencesActivity {
    protected void refreshCheckBox(UItem item, int position, boolean checked) {
        refreshCheckBox(item, position, checked, false);
    }

    protected void refreshCheckBox(UItem item, int position, boolean checked, boolean fullReload) {
        item.setChecked(checked);
        android.view.View view = this.listView.findViewByItemId(item.id);
        if (view instanceof CheckBoxCell) {
            ((CheckBoxCell) view).setChecked(checked, true);
        } else if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
            ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(checked);
        }
        if (fullReload) {
            this.listView.adapter.update(true);
        } else {
            this.listView.adapter.notifyItemChanged(position);
        }
    }
}
