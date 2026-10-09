package com.exteragram.messenger.preferences;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

import ni.shikatu.re_extera.utils.Res;

/**
 * Runtime shim for exteraGram's preferences screen base class.
 *
 * The DEX is loaded through a parent-first class loader: on exteraGram the app's own
 * class wins and this implementation is dead code, while forks without the extera SDK
 * (Inugram) fall back to it. It reproduces the parts of the original API that the
 * re:extera preference fragments rely on, using only stock Telegram classes.
 */
public abstract class BasePreferencesActivity extends BaseFragment {

    protected LinearLayoutManager layoutManager;
    protected UniversalRecyclerView listView;

    public abstract void fillItems(ArrayList<UItem> items, UniversalAdapter adapter);

    public abstract String getTitle();

    public abstract void onClick(UItem item, View view, int position, float x, float y);

    public boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    public boolean needHideTitle() {
        return false;
    }

    public int getListTopPadding(int topPadding) {
        return topPadding;
    }

    @Override
    public View createView(Context context) {
        if (this.actionBar == null) {
            this.actionBar = createActionBar(context);
        }
        actionBar.setBackButtonImage(Res.drawable("ic_ab_back", R.drawable.ic_ab_back));
        actionBar.setAllowOverlayTitle(false);
        actionBar.setTitle(getTitle());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);

        FrameLayout container = new FrameLayout(context);
        container.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this,
                (items, adapter) -> fillItems(items, adapter),
                this::onClick,
                this::onLongClick);
        listView.setClipToPadding(false);
        listView.setPadding(0, getListTopPadding(AndroidUtilities.statusBarHeight), 0, 0);
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        listView.setLayoutManager(layoutManager);

        container.addView(listView, LayoutHelper.createFrame(-1, -1f));
        container.addView(actionBar, LayoutHelper.createFrame(-1, -2, 48));

        fragmentView = container;
        return container;
    }
}
