package ni.shikatu.re_extera.settings.components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

@SuppressLint("ViewConstructor")
public class AltSeekbar extends FrameLayout {

    public interface OnDrag {
        void run(float value);
    }

    private final OnDrag onDrag;

    protected final AnimatedTextView headerValue;
    protected final TextView leftTextView;
    protected final TextView rightTextView;
    public final SeekBarView seekBarView;

    protected final int min;
    protected final int max;

    protected float currentValue;
    protected int roundedValue;

    private int vibro = -1;

    public AltSeekbar(Context context, OnDrag onDrag, int min, int max,
                      String title, String left, String right) {
        super(context);
        this.onDrag = onDrag;
        this.min = min;
        this.max = max;

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);

        TextView headerTextView = new TextView(context);
        headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        headerTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        headerTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        headerTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        headerTextView.setText(title);
        headerLayout.addView(headerTextView, LayoutHelper.createLinear(
                -2, -2, Gravity.CENTER_VERTICAL));

        headerValue = new AnimatedTextView(context, false, true, true) {
            final Drawable backgroundDrawable = Theme.createRoundRectDrawable(dp(4),
                    Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader), 0.15f));

            @Override
            public void onDraw(Canvas canvas) {
                backgroundDrawable.setBounds(0, 0,
                        (int) (getPaddingLeft() + getDrawable().getCurrentWidth() + getPaddingRight()),
                        getMeasuredHeight());
                backgroundDrawable.draw(canvas);
                super.onDraw(canvas);
            }
        };
        headerValue.setAnimationProperties(.45f, 0, 240, CubicBezierInterpolator.EASE_OUT_QUINT);
        headerValue.setAllowCancel(true);
        headerValue.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        headerValue.setPadding(dp(5.33f), dp(2), dp(5.33f), dp(2));
        headerValue.setTextSize(dp(12));
        headerValue.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        headerLayout.addView(headerValue, LayoutHelper.createLinear(
                -2, 17, Gravity.CENTER_VERTICAL, 6, 1, 0, 0));

        addView(headerLayout, LayoutHelper.createFrame(-1, -2,
                Gravity.TOP | Gravity.FILL_HORIZONTAL, 21, 17, 21, 0));

        FrameLayout valuesView = new FrameLayout(context);

        leftTextView = new TextView(context);
        leftTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        leftTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        leftTextView.setGravity(Gravity.LEFT);
        leftTextView.setText(left);
        valuesView.addView(leftTextView, LayoutHelper.createFrame(
                -2, -2, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        rightTextView = new TextView(context);
        rightTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        rightTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        rightTextView.setGravity(Gravity.RIGHT);
        rightTextView.setText(right);
        valuesView.addView(rightTextView, LayoutHelper.createFrame(
                -2, -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        addView(valuesView, LayoutHelper.createFrame(-1, -2,
                Gravity.TOP | Gravity.FILL_HORIZONTAL, 21, 52, 21, 0));

        seekBarView = new SeekBarView(context, true, null);
        seekBarView.setReportChanges(true);
        seekBarView.setDelegate((stop, progress) -> {
            float value = this.min + (this.max - this.min) * progress;
            if (this.onDrag != null) {
                this.onDrag.run(value);
            }
            if (Math.round(value) != roundedValue) {
                setProgress(value);
            }
        });

        addView(seekBarView, LayoutHelper.createFrame(-1, 38,
                Gravity.TOP | Gravity.FILL_HORIZONTAL, 11, 65, 11, 0));
    }

    public void setProgress(float value) {
        currentValue = value;
        roundedValue = Math.round(value);
        seekBarView.setProgress((value - min) / (max - min));
        headerValue.setText(String.format(LocaleController.getInstance().getCurrentLocale(), "%d%%", roundedValue), true);

        int newVibro;
        if (roundedValue <= min) {
            newVibro = 1;
        } else if (roundedValue >= max) {
            newVibro = 2;
        } else {
            newVibro = 0;
        }
        if (newVibro != 0 && newVibro != vibro) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        vibro = newVibro;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(110), MeasureSpec.EXACTLY)
        );
    }
}
