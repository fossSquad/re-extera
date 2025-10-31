package ni.shikatu.re_extera.chatmessagecell;

import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import ni.shikatu.re_extera.DbDeletedStore;
import ni.shikatu.re_extera.Global;
import ni.shikatu.re_extera.Localization;
import ni.shikatu.re_extera.Settings;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;

public class FillMessageMenuHook extends XC_MethodHook {
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Global.log("adding a messagemenu fill");
        MessageObject msgObj = (MessageObject) param.args[0];
        ProcessSelectedOptionHook.selectedObject = msgObj;
        ArrayList<Integer> icons = (ArrayList) param.args[1];
        ArrayList<CharSequence> items = (ArrayList) param.args[2];
        ArrayList<Integer> options = (ArrayList) param.args[3];
        if (Settings.getSaveEditedMessages() && DbDeletedStore.get().hasEdits(msgObj.getDialogId(), msgObj.getId())) {
            icons.add(0, Integer.valueOf(R.drawable.msg2_trending));
            items.add(0, Localization.MESSAGE_HISTORY);
            options.add(0, 6363);
        }
        if (Settings.getSaveOneTimeMessages() || Settings.noForward()) {
            boolean oneTime = msgObj.isSecret() || msgObj.isSecretMedia() || msgObj.isVoiceOnce() || msgObj.isRoundOnce();
            if (oneTime) {
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
}
