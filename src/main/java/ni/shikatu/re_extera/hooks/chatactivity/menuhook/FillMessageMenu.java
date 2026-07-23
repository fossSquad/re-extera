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
    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        MessageObject msgObj = (MessageObject) param.args[0];
        ProcessSelectedOption.selectedObject = msgObj;
        ArrayList<Integer> icons = (ArrayList) param.args[1];
        ArrayList<CharSequence> items = (ArrayList) param.args[2];
        ArrayList<Integer> options = (ArrayList) param.args[3];
        boolean oneTime = msgObj.isSecret() || msgObj.isSecretMedia() || msgObj.isVoiceOnce() || msgObj.isRoundOnce();
        if (Settings.getSaveEditedMessages() && ReExteraDb.get().messageHasSavedEdits(msgObj)) {
            icons.add(0, Integer.valueOf(R.drawable.menu_premium_clock));
            items.add(0, Localization.MESSAGE_HISTORY);
            options.add(0, Integer.valueOf(ProcessSelectedOption.OPT_MESSAGE_HISTORY));
        }
        if (msgObj.isOut() && Settings.getSaveReadDate()) {
            int readDate = ReExteraDb.get().getReadDate(msgObj.getDialogId(), msgObj.getId());
            if (readDate > 0) {
                String timeStr = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(readDate * 1000L));
                icons.add(0, Integer.valueOf(R.drawable.msg_info)); // fallback icon
                items.add(0, String.format(Localization.READ_AT, timeStr));
                options.add(0, Integer.valueOf(ProcessSelectedOption.OPT_READ_AT));
            }
        }
        boolean hideReading = Settings.getHideReadingWithGhost() || SendRequest.getCurrentReadingStatus() == -1;
        boolean alwaysRead = SendRequest.getCurrentReadingStatus() == 1;
        if (((hideReading && !alwaysRead) || oneTime) && !msgObj.isOut()) {
            icons.add(0, Integer.valueOf(R.drawable.msg_markread));
            items.add(0, Localization.READ_MESSAGE);
            options.add(0, Integer.valueOf(ProcessSelectedOption.OPT_READ_MESSAGE));
        }
        if ((Settings.getSaveOneTimeMessages() || Settings.noForward()) && oneTime) {
            appendSaveOptions(msgObj, icons, items, options);
        }
    }

    private static void appendSaveOptions(MessageObject msgObj, ArrayList<Integer> icons, ArrayList<CharSequence> items, ArrayList<Integer> options) {
        items.add(LocaleController.getString(R.string.Forward));
        options.add(2);
        icons.add(Integer.valueOf(R.drawable.msg_forward));
        if (msgObj.isVideo()) {
            addSaveEntry(items, options, icons, R.string.SaveToGallery, 4, R.drawable.msg_gallery);
            addSaveEntry(items, options, icons, R.string.ShareFile, 6, R.drawable.msg_shareout);
            return;
        }
        if (msgObj.isMusic()) {
            addSaveEntry(items, options, icons, R.string.SaveToMusic, 10, R.drawable.msg_download);
            addSaveEntry(items, options, icons, R.string.ShareFile, 6, R.drawable.msg_shareout);
        } else {
            if (msgObj.getDocument() != null) {
                if (MessageObject.isNewGifDocument(msgObj.getDocument())) {
                    addSaveEntry(items, options, icons, R.string.SaveToGIFs, 11, R.drawable.msg_gif);
                }
                addSaveEntry(items, options, icons, R.string.SaveToDownloads, 10, R.drawable.msg_download);
                addSaveEntry(items, options, icons, R.string.ShareFile, 6, R.drawable.msg_shareout);
                return;
            }
            addSaveEntry(items, options, icons, R.string.SaveToGallery, 4, R.drawable.msg_gallery);
        }
    }

    private static void addSaveEntry(ArrayList<CharSequence> items, ArrayList<Integer> options, ArrayList<Integer> icons, int labelRes, int optionId, int iconRes) {
        items.add(LocaleController.getString(labelRes));
        options.add(Integer.valueOf(optionId));
        icons.add(Integer.valueOf(iconRes));
    }
}
