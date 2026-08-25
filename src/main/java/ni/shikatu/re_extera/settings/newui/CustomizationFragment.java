package ni.shikatu.re_extera.settings.newui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import ni.shikatu.re_extera.hooks.chatmessagecell.MeasureTime;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import com.exteragram.messenger.preferences.components.AltSeekbar;

public class CustomizationFragment extends BasePreferencesActivityExtended {

    private enum CustomizationIds {
        TRANSPARENT_DELETED_MESSAGES_PREVIEW_ID,
        CUSTOM_DELETED_MARK_ID,
        DELETED_MARK_COLOR_HEADER_ID,
        DELETED_MARK_COLOR_ID,
        DISABLE_COLORED_REPLIES_ID,
        TRANSPARENT_DELETED_MESSAGES_ID,
        TRANSPARENT_DELETED_MESSAGES_SLIDER_ID,
        SAVE_DELETED_MESSAGES_ID,
        SAVE_BOT_CHATS_ID,
        SAVE_MESSAGE_HISTORY_ID,
        SAVE_MANUALLY_DELETED_ID,
        USE_COLLAPSED_BLOCKQUOTE_ID;

        public int getId() {
            return ordinal() + 1;
        }
    }

    @Override
    public String getTitle() {
        return Localization.DELETED_MESSAGE;
    }

    private ChatMessageCell previewCell;
    private LinearLayout colorsLayout;

    private void updateColorsSelection() {
        if (colorsLayout == null) return;
        int currentColor = Settings.getDeletedMarkColor();
        int customColor = Settings.getDeletedMarkCustomColor();
        for (int i = 0; i < colorsLayout.getChildCount(); i++) {
            View child = colorsLayout.getChildAt(i);
            if (child instanceof ColorCircle) {
                ColorCircle circle = (ColorCircle) child;
                boolean selected = false;
                if (circle.isDefault) {
                    selected = (currentColor == 0);
                } else if (circle.isCustom) {
                    selected = (currentColor != 0 && currentColor == customColor && !isStandardColor(currentColor));
                } else {
                    selected = (currentColor == circle.color);
                }
                circle.setSelectedColor(selected);
            }
        }
    }

    private boolean isStandardColor(int color) {
        int[] standard = {0xFFE53935, 0xFF1E88E5, 0xFFFFB300, 0xFF43A047, 0xFF8E24AA};
        for (int c : standard) {
            if (c == color) return true;
        }
        return false;
    }

    private class ColorCircle extends View {
        public int color;
        public boolean isCustom;
        public boolean isDefault;
        private boolean isSelectedColor;
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Drawable icon;

        public ColorCircle(Context context, int color, boolean isCustom, boolean isDefault) {
            super(context);
            this.color = color;
            this.isCustom = isCustom;
            this.isDefault = isDefault;
            setWillNotDraw(false);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(AndroidUtilities.dp(2));
            
            setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            
            if (isCustom) {
                icon = ContextCompat.getDrawable(context, org.telegram.messenger.R.drawable.msg_edit);
                if (icon != null) {
                    icon = icon.mutate();
                }
            }
        }
        
        public void setSelectedColor(boolean selected) {
            this.isSelectedColor = selected;
            invalidate();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int radius = AndroidUtilities.dp(16);
            
            if (isDefault) {
                paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                canvas.drawCircle(cx, cy, radius, paint);
            } else if (isCustom && color == 0) {
                android.graphics.Shader shader = new android.graphics.SweepGradient(cx, cy, 
                    new int[]{0xFFE53935, 0xFFFFB300, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA, 0xFFE53935}, null);
                paint.setShader(shader);
                canvas.drawCircle(cx, cy, radius, paint);
                paint.setShader(null);
            } else {
                paint.setColor(color);
                canvas.drawCircle(cx, cy, radius, paint);
            }
            
            if (isSelectedColor) {
                strokePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                canvas.drawCircle(cx, cy, radius + AndroidUtilities.dp(3), strokePaint);
            }
            
            if (isCustom && icon != null) {
                int iconSize = AndroidUtilities.dp(16);
                icon.setBounds(cx - iconSize/2, cy - iconSize/2, cx + iconSize/2, cy + iconSize/2);
                int iconColor = 0xFFFFFFFF;
                if (color != 0) {
                    double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
                    iconColor = luminance > 0.5 ? 0xFF000000 : 0xFFFFFFFF;
                }
                icon.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
                icon.draw(canvas);
            }
        }
    }

    private void showColorPickerDialog(int initialColor, final Runnable onColorSelected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose Color");
        
        final int[] selectedColor = new int[]{initialColor == 0 ? 0xFFFFFFFF : initialColor};
        
        ColorPicker colorPicker = new ColorPicker(getContext(), false, new ColorPicker.ColorPickerDelegate() {
            @Override
            public void setColor(int color, int state, boolean notify) {
                selectedColor[0] = color;
                Settings.setDeletedMarkColor(color);
                if (previewCell != null && previewCell.getMessageObject() != null) {
                    previewCell.getMessageObject().forceUpdate = true;
                    previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
                }
            }
        });
        
        LinearLayout wrapper = new LinearLayout(getContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(colorPicker, LayoutHelper.createLinear(-1, 300));
        builder.setView(wrapper);
        
        builder.setPositiveButton(Localization.SAVE, (dialog, which) -> {
            Settings.setDeletedMarkCustomColor(selectedColor[0]);
            Settings.setDeletedMarkColor(selectedColor[0]);
            onColorSelected.run();
        });
        builder.setNegativeButton(Localization.CANCEL, (dialog, which) -> {
            Settings.setDeletedMarkColor(initialColor);
            if (previewCell != null && previewCell.getMessageObject() != null) {
                previewCell.getMessageObject().forceUpdate = true;
                previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
            }
            onColorSelected.run();
        });
        
        showDialog(builder.create());
    }

    private View colorSelectorView() {
        HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setHorizontalScrollBarEnabled(false);
        
        colorsLayout = new LinearLayout(getContext());
        colorsLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorsLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        
        int[] colors = {0, 0xFFE53935, 0xFF1E88E5, 0xFFFFB300, 0xFF43A047, 0xFF8E24AA};
        
        ColorCircle customCircle = new ColorCircle(getContext(), Settings.getDeletedMarkCustomColor(), true, false);
        colorsLayout.addView(customCircle, LayoutHelper.createLinear(48, 48, 0, 0, 4, 0));
        customCircle.setOnClickListener(v -> {
            int currentCustom = Settings.getDeletedMarkCustomColor();
            if (currentCustom == 0 || Settings.getDeletedMarkColor() == currentCustom) {
                showColorPickerDialog(Settings.getDeletedMarkColor(), () -> {
                    customCircle.color = Settings.getDeletedMarkCustomColor();
                    updateColorsSelection();
                });
            } else {
                Settings.setDeletedMarkColor(currentCustom);
                if (previewCell != null && previewCell.getMessageObject() != null) {
                    previewCell.getMessageObject().forceUpdate = true;
                    previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
                }
                updateColorsSelection();
            }
        });
        
        for (int c : colors) {
            boolean isDefault = (c == 0);
            ColorCircle circle = new ColorCircle(getContext(), c, false, isDefault);
            colorsLayout.addView(circle, LayoutHelper.createLinear(48, 48, 0, 0, 4, 0));
            circle.setOnClickListener(v -> {
                Settings.setDeletedMarkColor(c);
                if (previewCell != null && previewCell.getMessageObject() != null) {
                    previewCell.getMessageObject().forceUpdate = true;
                    previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
                }
                updateColorsSelection();
            });
        }
        
        scrollView.addView(colorsLayout, LayoutHelper.createScroll(-2, -2, 17));
        updateColorsSelection();
        return scrollView;
    }

    private FrameLayout customMarkView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        EditTextSettingsCell customPrefix = new EditTextSettingsCell(getContext());
        customPrefix.setTextAndHint(Settings.getCustomPrefix(), Localization.LEAVE_BLANK_FOR_RECYCLE, false);
        customPrefix.getTextView().addTextChangedListener(new TextWatcher() { 
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Settings.setCustomPrefix(s.toString());
                MeasureTime.notifyMarkChanged(Settings.getCustomPrefix());
                if (previewCell != null && previewCell.getMessageObject() != null) {
                    previewCell.getMessageObject().forceUpdate = true;
                    previewCell.setMessageObject(previewCell.getMessageObject(), null, false, false, false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        frameLayout.addView(customPrefix);
        return frameLayout;
    }

    private FrameLayout previewView() {
        FrameLayout frameLayout = new FrameLayout(getContext());
        
        ChatMessageCell chatMessageCell = new ChatMessageCell(getContext(), UserConfig.selectedAccount);
        
        TLRPC.TL_message replyMsg = new TLRPC.TL_message();
        replyMsg.message = "are you using re:extera?";
        replyMsg.out = true;
        replyMsg.id = 2;
        TLRPC.TL_peerUser replyFromUser = new TLRPC.TL_peerUser();
        replyFromUser.user_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        replyMsg.from_id = replyFromUser;
        MessageObject replyMessageObject = new MessageObject(UserConfig.selectedAccount, replyMsg, true, false);
        
        TLRPC.TL_message msg = new TLRPC.TL_message();
        msg.message = "sure, best plugin ever trust";
        msg.date = (int) (System.currentTimeMillis() / 1000);
        msg.dialog_id = -999999999L;
        msg.flags = 259 | 8;
        
        TLRPC.TL_peerUser fromUser = new TLRPC.TL_peerUser();
        fromUser.user_id = 123456789L;
        msg.from_id = fromUser;
        
        msg.id = 1;
        msg.media = new TLRPC.TL_messageMediaEmpty();
        msg.out = false;
        
        TLRPC.TL_messageReplyHeader replyHeader = new TLRPC.TL_messageReplyHeader();
        replyHeader.reply_to_msg_id = 2;
        msg.reply_to = replyHeader;
        
        TLRPC.TL_peerUser peerUser = new TLRPC.TL_peerUser();
        peerUser.user_id = -999999999L;
        msg.peer_id = peerUser;
        
        MessageObject messageObject = new MessageObject(UserConfig.selectedAccount, msg, true, false);
        messageObject.deleted = true;
        messageObject.eventId = 1;
        messageObject.customName = "lostya";
        messageObject.customReplyName = "aartzz";
        messageObject.replyMessageObject = replyMessageObject;
        messageObject.resetLayout();
        
        chatMessageCell.isChat = true;
        chatMessageCell.setFullyDraw(true);
        chatMessageCell.setMessageObject(messageObject, null, false, false, false);
        
        frameLayout.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        frameLayout.addView(chatMessageCell, LayoutHelper.createFrame(-1, -2));
        
        Drawable wallpaper = Theme.getCachedWallpaperNonBlocking();
        if (wallpaper != null) {
            frameLayout.setBackground(wallpaper);
        } else {
            frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        }
        
        previewCell = chatMessageCell;
        return frameLayout;
    }

    private View alphaSliderView() {
        AltSeekbar slider = new AltSeekbar(getContext(), new AltSeekbar.OnDrag() {
            @Override
            public void run(float value) {
                float alpha = value / 100f;
                Settings.setTransparentDeletedMessagesAlpha(alpha);
                if (previewCell != null) {
                    if (Settings.getTransparentDeletedMessages()) {
                        previewCell.setAlpha(alpha);
                    }
                }
            }
        }, 0, 100, Localization.ENABLE_ALPHA, "0%", "100%");
        slider.setProgress(Settings.getTransparentDeletedMessagesAlpha() * 100f);
        return slider;
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Master switch: when off, everything below is hidden.
        items.add(UItem.asCheck(CustomizationIds.SAVE_DELETED_MESSAGES_ID.getId(), Localization.SAVE_DELETED_MESSAGES).setChecked(Settings.getSaveDeletedMessages()).setLinkAlias("reExteraSaveDeletedMessages", this));
        items.add(UItem.asShadow());
        if (!Settings.getSaveDeletedMessages()) {
            return;
        }

        items.add(UItem.asCheck(CustomizationIds.SAVE_BOT_CHATS_ID.getId(), Localization.SAVE_BOT_CHATS).setChecked(Settings.getSaveBotChats()).setLinkAlias("reExteraSaveBotChats", this));
        items.add(UItem.asCheck(CustomizationIds.SAVE_MESSAGE_HISTORY_ID.getId(), Localization.MESSAGE_HISTORY_TOGGLE).setChecked(Settings.getSaveEditedMessages()).setLinkAlias("reExteraSaveMessageHistory", this));
        items.add(UItem.asCheck(CustomizationIds.SAVE_MANUALLY_DELETED_ID.getId(), Localization.SAVE_SELF_DELETED_MESSAGES).setChecked(Settings.getSaveManuallyDeleted()).setLinkAlias("reExteraSaveManuallyDeleted", this));
        items.add(UItem.asShadow(Localization.ABOUT_SAVE_SELF_DELETED_MESSAGES));

        items.add(UItem.asCheck(CustomizationIds.USE_COLLAPSED_BLOCKQUOTE_ID.getId(), Localization.USE_COLLAPSED_BLOCKQUOTE).setChecked(Settings.getUseExpandableBlockQuote()).setLinkAlias("reExteraUseCollapsedBlockquote", this));
        items.add(UItem.asShadow(Localization.USE_COLLAPSED_BLOCKQUOTE_DESCRIPTION));

        items.add(UItem.asCustom(CustomizationIds.TRANSPARENT_DELETED_MESSAGES_PREVIEW_ID.getId(), previewView()));

        items.add(UItem.asHeader(Localization.CUSTOM_PREFIX));
        items.add(UItem.asCustom(CustomizationIds.CUSTOM_DELETED_MARK_ID.getId(), customMarkView()).setLinkAlias("reExteraCustomDeletedMark", this));
        items.add(UItem.asShadow());
        
        items.add(UItem.asHeader("Deleted Mark Color"));
        items.add(UItem.asCustom(CustomizationIds.DELETED_MARK_COLOR_ID.getId(), colorSelectorView()));
        items.add(UItem.asShadow());

        items.add(UItem.asCheck(CustomizationIds.DISABLE_COLORED_REPLIES_ID.getId(), Localization.DISABLE_COLORED_REPLIES).setChecked(Settings.getDisableColoredReplies()).setLinkAlias("reExteraDisableColoredReplies", this));

        items.add(UItem.asCheck(CustomizationIds.TRANSPARENT_DELETED_MESSAGES_ID.getId(), Localization.ENABLE_ALPHA).setChecked(Settings.getTransparentDeletedMessages()).setLinkAlias("reExteraTransparentDeletedMessages", this));
        
        if (Settings.getTransparentDeletedMessages()) {
            items.add(UItem.asCustom(CustomizationIds.TRANSPARENT_DELETED_MESSAGES_SLIDER_ID.getId(), alphaSliderView()));
        }
        
        items.add(UItem.asShadow());
        
        if (previewCell != null) {
            if (Settings.getTransparentDeletedMessages()) {
                previewCell.setAlpha(Settings.getTransparentDeletedMessagesAlpha());
            } else {
                previewCell.setAlpha(1.0f);
            }
        }
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > CustomizationIds.values().length) {
            return;
        }
        CustomizationIds clicked = CustomizationIds.values()[item.id - 1];
        switch (clicked) {
            case DISABLE_COLORED_REPLIES_ID:
                Settings.setDisableColoredReplies(!Settings.getDisableColoredReplies());
                refreshCheckBox(item, position, Settings.getDisableColoredReplies(), true);
                if (previewCell != null) {
                    previewCell.invalidate();
                }
                break;
            case TRANSPARENT_DELETED_MESSAGES_ID:
                Settings.setTransparentDeletedMessages(!Settings.getTransparentDeletedMessages());
                refreshCheckBox(item, position, Settings.getTransparentDeletedMessages(), true);
                if (previewCell != null) {
                    if (Settings.getTransparentDeletedMessages()) {
                        previewCell.setAlpha(Settings.getTransparentDeletedMessagesAlpha());
                    } else {
                        previewCell.setAlpha(1.0f);
                    }
                }
                break;
            case SAVE_DELETED_MESSAGES_ID:
                Settings.setSaveDeletedMessages(!Settings.getSaveDeletedMessages());
                // Full reload so the dependent rows appear/disappear.
                refreshCheckBox(item, position, Settings.getSaveDeletedMessages(), true);
                break;
            case SAVE_BOT_CHATS_ID:
                Settings.setSaveBotChats(!Settings.getSaveBotChats());
                refreshCheckBox(item, position, Settings.getSaveBotChats());
                break;
            case SAVE_MESSAGE_HISTORY_ID:
                Settings.setSaveEditedMessages(!Settings.getSaveEditedMessages());
                refreshCheckBox(item, position, Settings.getSaveEditedMessages());
                break;
            case SAVE_MANUALLY_DELETED_ID:
                Settings.setSaveManuallyDeleted(!Settings.getSaveManuallyDeleted());
                refreshCheckBox(item, position, Settings.getSaveManuallyDeleted());
                break;
            case USE_COLLAPSED_BLOCKQUOTE_ID:
                Settings.setUseExpandableBlockQuote(!Settings.getUseExpandableBlockQuote());
                refreshCheckBox(item, position, Settings.getUseExpandableBlockQuote());
                break;
        }
    }

    @Override
    public void onFragmentDestroy() {
        if (fragmentView != null) {
            fragmentView.clearFocus();
            AndroidUtilities.hideKeyboard(fragmentView);
        }
        super.onFragmentDestroy();
    }
}
