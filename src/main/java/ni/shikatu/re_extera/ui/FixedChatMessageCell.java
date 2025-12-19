package ni.shikatu.re_extera.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import java.lang.reflect.Field;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.AvatarDrawable;

public class FixedChatMessageCell extends ChatMessageCell {
    FixedChatMessageCell(Context context, int selectedAccount) {
        super(context, selectedAccount);
    }

    public void setCellAttachedToWindow(boolean attached) {
        if (attached) {
            super.onAttachedToWindow();
        } else {
            super.onDetachedFromWindow();
        }
    }

    public void forceRenderAvatar(MessageObject mo) {
        Drawable avatarDrawable;
        try {
            Field field = ChatMessageCell.class.getDeclaredField("photoImage");
            field.setAccessible(true);
            ImageReceiver avatarImage = (ImageReceiver) field.get(this);
            long senderId = mo.getSenderId();
            MessagesController controller = MessagesController.getInstance(UserConfig.selectedAccount);
            if (mo.isFromUser()) {
                avatarDrawable = new AvatarDrawable(controller.getUser(Long.valueOf(senderId)));
            } else {
                avatarDrawable = new AvatarDrawable(controller.getChat(Long.valueOf(senderId)));
            }
            avatarImage.setForUserOrChat(mo.messageOwner, (Drawable) null, mo);
            avatarImage.setImage((ImageLocation) null, (String) null, avatarDrawable, (String) null, (Object) null, 0);
            invalidate();
        } catch (Exception e) {
        }
    }
}
