package ni.shikatu.re_extera.hooks.chatactivity.menuhook;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.hooks.connectionsmanager.SendRequest;
import ni.shikatu.re_extera.localization.Localization;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;

public class FillMessageMenu extends XC_MethodHook {
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        MessageObject msgObj = (MessageObject) param.args[0];
        ProcessSelectedOption.selectedObject = msgObj;
        ArrayList<Integer> icons = (ArrayList) param.args[1];
        ArrayList<CharSequence> items = (ArrayList) param.args[2];
        ArrayList<Integer> options = (ArrayList) param.args[3];
        boolean oneTime = msgObj.isSecret() || msgObj.isSecretMedia() || msgObj.isVoiceOnce() || msgObj.isRoundOnce();
        if (Settings.getSaveEditedMessages() && ReExteraDb.get().messageHasSavedEdits(msgObj)) {
            icons.add(0, Integer.valueOf(R.drawable.menu_premium_clock));
            items.add(0, Localization.MESSAGE_HISTORY);
            options.add(0, 6363);
        }
        if ((((Settings.getHideReadingWithGhost() || SendRequest.getCurrentReadingStatus() == -1) && SendRequest.getCurrentReadingStatus() != 1) || oneTime) && !msgObj.isOut()) {
            icons.add(0, Integer.valueOf(R.drawable.msg_markread));
            items.add(0, Localization.READ_MESSAGE);
            options.add(0, 6565);
        }
        if ((Settings.getSaveOneTimeMessages() || Settings.noForward()) && oneTime) {
            items.add(LocaleController.getString(R.string.Forward));
            options.add(2);
            icons.add(Integer.valueOf(R.drawable.msg_forward));
            if (msgObj.isVideo()) {
                items.add(LocaleController.getString(R.string.SaveToGallery));
                options.add(4);
                icons.add(Integer.valueOf(R.drawable.msg_gallery));
                items.add(LocaleController.getString(R.string.ShareFile));
                options.add(6);
                icons.add(Integer.valueOf(R.drawable.msg_shareout));
                return;
            }
            if (msgObj.isMusic()) {
                items.add(LocaleController.getString(R.string.SaveToMusic));
                options.add(10);
                icons.add(Integer.valueOf(R.drawable.msg_download));
                items.add(LocaleController.getString(R.string.ShareFile));
                options.add(6);
                icons.add(Integer.valueOf(R.drawable.msg_shareout));
                return;
            }
            if (msgObj.getDocument() != null) {
                if (MessageObject.isNewGifDocument(msgObj.getDocument())) {
                    items.add(LocaleController.getString(R.string.SaveToGIFs));
                    options.add(11);
                    icons.add(Integer.valueOf(R.drawable.msg_gif));
                }
                items.add(LocaleController.getString(R.string.SaveToDownloads));
                options.add(10);
                icons.add(Integer.valueOf(R.drawable.msg_download));
                items.add(LocaleController.getString(R.string.ShareFile));
                options.add(6);
                icons.add(Integer.valueOf(R.drawable.msg_shareout));
                return;
            }
            items.add(LocaleController.getString(R.string.SaveToGallery));
            options.add(4);
            icons.add(Integer.valueOf(R.drawable.msg_gallery));
        }
    }
}
