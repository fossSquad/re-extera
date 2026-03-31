package ni.shikatu.re_extera.utils;

import android.os.Bundle;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.TopicsFragment;

public class RestrictedMessageUtils {
    public static void createMenu(final BaseFragment fragment, View view, final MessageObject toForward) {
        ItemOptions.makeOptions(fragment, view).add(R.drawable.msg_forward, LocaleController.getString(R.string.Forward), new Runnable() { // from class: ni.shikatu.re_extera.utils.RestrictedMessageUtils$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RestrictedMessageUtils.forwardMessages(fragment, new ArrayList(Collections.singletonList(toForward)));
            }
        }).add(R.drawable.msg_saved, LocaleController.getString(R.string.SavedMessages), new Runnable() { // from class: ni.shikatu.re_extera.utils.RestrictedMessageUtils$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RestrictedMessageUtils.saveMessagesToSaved(fragment, new ArrayList(Collections.singletonList(toForward)));
            }
        }).setOnTopOfScrim().show();
    }

    public static void forwardMessages(final BaseFragment fragment, final ArrayList<MessageObject> messages) {
        if (fragment == null || messages == null || messages.isEmpty()) {
            return;
        }
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", 3);
        args.putBoolean("allowGroups", true);
        args.putBoolean("allowUsers", true);
        args.putBoolean("allowChannels", true);
        args.putBoolean("allowBots", true);
        DialogsActivity dialogsActivity = new DialogsActivity(args);
        DialogsActivity.DialogsActivityDelegate delegate = new DialogsActivity.DialogsActivityDelegate() { // from class: ni.shikatu.re_extera.utils.RestrictedMessageUtils.1
            public boolean didSelectDialogs(DialogsActivity fragment1, ArrayList<MessagesStorage.TopicKey> dids, CharSequence message, boolean param, boolean notify, int scheduleDate, int scheduleRepeatPeriod, TopicsFragment topicsFragment) {
                TLRPC.TL_forumTopic topic;
                if (dids == null || dids.isEmpty()) {
                    return false;
                }
                for (MessagesStorage.TopicKey topicKey : dids) {
                    long dialogId = topicKey.dialogId;
                    long topicId = topicKey.topicId;
                    MessageObject replyToTopMsg = null;
                    if (topicId != 0 && (topic = MessagesController.getInstance(fragment.getCurrentAccount()).getTopicsController().findTopic(-dialogId, (int) topicId)) != null && topic.top_message != 0) {
                        replyToTopMsg = new MessageObject(fragment.getCurrentAccount(), topic.topMessage, false, false);
                    }
                    MessageForwarder.sendMessageCopy(AccountInstance.getInstance(fragment.getCurrentAccount()), messages, dialogId, true, 0, replyToTopMsg);
                }
                fragment1.finishFragment();
                BulletinFactory bulletin = BulletinFactory.of(LaunchActivity.getLastFragment());
                if (dids.size() == 1) {
                    bulletin.showForwardedBulletinWithTag(dids.get(0).dialogId, messages.size());
                } else {
                    bulletin.createSimpleBulletin(R.raw.forward, LocaleController.formatPluralString("ForwardedMessageCount", messages.size(), new Object[0]), LocaleController.formatPluralString("ForwardedToChatsCount", dids.size(), new Object[0])).show();
                }
                return true;
            }

            public boolean canSelectStories() {
                return false;
            }

            public boolean didSelectStories(DialogsActivity fragment2) {
                return false;
            }
        };
        dialogsActivity.setDelegate(delegate);
        fragment.presentFragment(dialogsActivity);
    }

    public static void saveMessagesToSaved(BaseFragment fragment, ArrayList<MessageObject> messages) {
        if (fragment == null || messages == null) {
            return;
        }
        if (messages.isEmpty()) {
            return;
        }
        MessageForwarder.sendMessageCopy(AccountInstance.getInstance(fragment.getCurrentAccount()), messages, UserConfig.getInstance(fragment.getCurrentAccount()).getClientUserId(), true, 0, null);
        long selfId = UserConfig.getInstance(fragment.getCurrentAccount()).getClientUserId();
        BulletinFactory.of(LaunchActivity.getLastFragment()).showForwardedBulletinWithTag(selfId, messages.size());
    }
}
