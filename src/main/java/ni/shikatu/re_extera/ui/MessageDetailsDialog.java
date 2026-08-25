package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import ni.shikatu.re_extera.localization.Localization;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

/**
 * A lightweight "Message Details" dialog (Telegraph-style): lists the technical
 * fields of a message (id, date, sender, dc, file info, …) with a Copy action.
 */
public final class MessageDetailsDialog {

    private MessageDetailsDialog() {
    }

    public static void show(BaseFragment fragment, MessageObject msg) {
        if (fragment == null || msg == null || msg.messageOwner == null) {
            return;
        }
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        final String details = build(msg);

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(8), AndroidUtilities.dp(22), AndroidUtilities.dp(8));
        textView.setTextIsSelectable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(details);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(textView);
        scrollView.addView(container);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Localization.MESSAGE_DETAILS);
        builder.setView(scrollView);
        builder.setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Copy), (dialog, which) -> {
            AndroidUtilities.addToClipboard(details);
        });
        builder.setNegativeButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Close), null);
        builder.show();
    }

    private static String build(MessageObject msg) {
        TLRPC.Message m = msg.messageOwner;
        StringBuilder sb = new StringBuilder();

        row(sb, "ID", String.valueOf(msg.getId()));
        row(sb, "Dialog ID", String.valueOf(msg.getDialogId()));
        try {
            long fromId = DialogObject.getPeerDialogId(m.from_id);
            if (fromId != 0) {
                row(sb, "From ID", String.valueOf(fromId));
            }
        } catch (Throwable ignored) {
        }
        if (m.date > 0) {
            row(sb, "Date", formatDate(m.date));
        }
        if (m.edit_date > 0) {
            row(sb, "Edited", formatDate(m.edit_date));
        }
        try {
            if (m.forwards > 0) {
                row(sb, "Forwards", String.valueOf(m.forwards));
            }
        } catch (Throwable ignored) {
        }
        try {
            if (m.via_bot_id != 0) {
                row(sb, "Via bot ID", String.valueOf(m.via_bot_id));
            }
        } catch (Throwable ignored) {
        }

        int dc = dcId(msg);
        if (dc > 0) {
            row(sb, "DC", String.valueOf(dc));
        }

        try {
            TLRPC.Document doc = msg.getDocument();
            if (doc != null) {
                String name = fileName(doc);
                if (name != null && !name.isEmpty()) {
                    row(sb, "File name", name);
                }
                if (doc.mime_type != null && !doc.mime_type.isEmpty()) {
                    row(sb, "File type", doc.mime_type);
                }
                if (doc.size > 0) {
                    row(sb, "File size", AndroidUtilities.formatFileSize(doc.size));
                }
            }
        } catch (Throwable ignored) {
        }

        return sb.toString().trim();
    }

    private static int dcId(MessageObject msg) {
        try {
            TLRPC.Document doc = msg.getDocument();
            if (doc != null && doc.dc_id > 0) {
                return doc.dc_id;
            }
        } catch (Throwable ignored) {
        }
        try {
            TLRPC.Photo photo = (msg.messageOwner.media != null) ? msg.messageOwner.media.photo : null;
            if (photo != null && photo.dc_id > 0) {
                return photo.dc_id;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static String fileName(TLRPC.Document doc) {
        try {
            for (TLRPC.DocumentAttribute attr : doc.attributes) {
                if (attr instanceof TLRPC.TL_documentAttributeFilename) {
                    return ((TLRPC.TL_documentAttributeFilename) attr).file_name;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String formatDate(int seconds) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(seconds * 1000L));
        } catch (Throwable e) {
            return String.valueOf(seconds);
        }
    }

    private static void row(StringBuilder sb, String label, String value) {
        sb.append(label).append(": ").append(value).append("\n");
    }
}
