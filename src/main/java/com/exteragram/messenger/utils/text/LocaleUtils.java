package com.exteragram.messenger.utils.text;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

/**
 * Runtime shim for exteraGram's locale helpers.
 *
 * Only {@link #fullyFormatText(CharSequence)} is used by re:extera code, to turn
 * the light markdown exteraGram accepts (`**bold**`, `__italic__`) into spans.
 * Plain strings pass through unchanged.
 */
public abstract class LocaleUtils {

    public static CharSequence fullyFormatText(CharSequence text) {
        if (text == null) {
            return "";
        }
        String source = text.toString();
        if (source.indexOf("**") == -1 && source.indexOf("__") == -1) {
            return text;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(source);
        applyMarkup(builder, "**", Typeface.BOLD);
        applyMarkup(builder, "__", Typeface.ITALIC);
        return builder;
    }

    private static void applyMarkup(SpannableStringBuilder builder, String marker, int style) {
        int from = 0;
        while (true) {
            String current = builder.toString();
            int start = current.indexOf(marker, from);
            if (start == -1) {
                return;
            }
            int end = current.indexOf(marker, start + marker.length());
            if (end == -1) {
                return;
            }
            builder.delete(end, end + marker.length());
            builder.delete(start, start + marker.length());
            int spanEnd = end - marker.length();
            builder.setSpan(new StyleSpan(style), start, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            from = spanEnd;
        }
    }
}
