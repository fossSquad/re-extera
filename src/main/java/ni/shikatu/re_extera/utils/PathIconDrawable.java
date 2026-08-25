package ni.shikatu.re_extera.utils;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.PathParser;
import org.telegram.messenger.AndroidUtilities;

/**
 * A tintable {@link Drawable} that renders one or more SVG path strings.
 *
 * <p>re:extera ships as a DEX (no resource table), so custom vector icons can't be
 * added under res/drawable; instead the SVG path data is parsed at runtime and
 * drawn directly, scaled to the drawable bounds and centered.
 */
public class PathIconDrawable extends Drawable {
    private final Path path = new Path();
    private final float viewportW;
    private final float viewportH;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int intrinsicPx;

    /** Fill icon. */
    public PathIconDrawable(float viewportW, float viewportH, boolean evenOdd, int intrinsicDp, String... pathData) {
        this(viewportW, viewportH, evenOdd, intrinsicDp, 0f, pathData);
    }

    /** {@code strokeWidth > 0} (in viewport units) renders as a stroked (outline) icon. */
    public PathIconDrawable(float viewportW, float viewportH, boolean evenOdd, int intrinsicDp, float strokeWidth, String... pathData) {
        this.viewportW = viewportW;
        this.viewportH = viewportH;
        this.intrinsicPx = AndroidUtilities.dp(intrinsicDp);
        if (strokeWidth > 0f) {
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(strokeWidth);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
        } else {
            this.paint.setStyle(Paint.Style.FILL);
        }
        this.path.setFillType(evenOdd ? Path.FillType.EVEN_ODD : Path.FillType.WINDING);
        for (String d : pathData) {
            try {
                Path p = PathParser.createPathFromPathData(d);
                if (p != null) {
                    this.path.addPath(p);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public PathIconDrawable withTint(int color) {
        paint.setColor(color);
        return this;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0) {
            return;
        }
        float scale = Math.min(b.width() / viewportW, b.height() / viewportH);
        int save = canvas.save();
        canvas.translate(b.left + (b.width() - viewportW * scale) / 2f,
                b.top + (b.height() - viewportH * scale) / 2f);
        canvas.scale(scale, scale);
        canvas.drawPath(path, paint);
        canvas.restoreToCount(save);
    }

    @Override
    public int getIntrinsicWidth() {
        return intrinsicPx;
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicPx;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    public void setTintColorFilter(int color) {
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
