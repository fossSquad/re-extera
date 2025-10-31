package ni.shikatu.re_extera;

import android.content.Context;
import android.net.Uri;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.LaunchActivity;

public class Utils {
    private static AlertDialog alertForward;
    private static final Field chatActivityEnterTopViewField;
    private static final Field fieldPanelShownField;
    private static final Field forwardingMessage;
    private static final Field forwardingMessageGroup;

    static {
        try {
            forwardingMessage = ChatActivity.class.getDeclaredField("forwardingMessage");
            forwardingMessage.setAccessible(true);
            forwardingMessageGroup = ChatActivity.class.getDeclaredField("forwardingMessageGroup");
            forwardingMessageGroup.setAccessible(true);
            fieldPanelShownField = ChatActivity.class.getDeclaredField("fieldPanelShown");
            fieldPanelShownField.setAccessible(true);
            chatActivityEnterTopViewField = ChatActivity.class.getDeclaredField("chatActivityEnterTopView");
            chatActivityEnterTopViewField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static Bulletin.LottieLayout getNewLayout(Context context, Theme.ResourcesProvider provider, long loadedSize, long totalSize) {
        Bulletin.LottieLayout layout = new Bulletin.LottieLayout(context, provider);
        int progress = (int) ((100 * loadedSize) / totalSize);
        layout.textView.setText(String.format(Localization.FORWARDED, Integer.valueOf(progress)));
        layout.textView.setSingleLine(false);
        layout.textView.setMaxLines(2);
        return layout;
    }

    public static long getDialogIdFromMessage(TLRPC.Message msg) {
        if (msg.peer_id instanceof TLRPC.TL_peerUser) {
            return msg.peer_id.user_id;
        }
        if (msg.peer_id instanceof TLRPC.TL_peerChat) {
            return -msg.peer_id.chat_id;
        }
        if (msg.peer_id instanceof TLRPC.TL_peerChannel) {
            return -msg.peer_id.channel_id;
        }
        return 0L;
    }

    public static CharSequence fullyFormatText(String text) {
        try {
            Class<?> clazz = Class.forName("com.exteragram.messenger.utils.text.LocaleUtils");
            return (CharSequence) clazz.getMethod("fullyFormatText", CharSequence.class).invoke(null, text);
        } catch (Exception e) {
            return text;
        }
    }

    public static void sendMessageCopy(final AccountInstance accountInstance, ArrayList<MessageObject> messages, final long peer, final boolean notify, final int scheduleDate, final MessageObject replyToTopMsg) {
        if (messages.isEmpty()) {
            return;
        }
        HashMap<Long, ArrayList<MessageObject>> groupedMessages = new HashMap<>();
        ArrayList<MessageObject> singleMessages = new ArrayList<>();
        for (MessageObject msgObj : messages) {
            long groupId = msgObj.getGroupId();
            if (groupId != 0) {
                ArrayList<MessageObject> group = groupedMessages.get(Long.valueOf(groupId));
                if (group == null) {
                    group = new ArrayList<>();
                    groupedMessages.put(Long.valueOf(groupId), group);
                }
                group.add(msgObj);
            } else {
                singleMessages.add(msgObj);
            }
        }
        for (final Map.Entry<Long, ArrayList<MessageObject>> entry : groupedMessages.entrySet()) {
            ensureMediaDownloadedForGroup(accountInstance, entry.getValue(), new Runnable() { // from class: ni.shikatu.re_extera.Utils$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Utils.sendGroup(accountInstance, (ArrayList) entry.getValue(), peer, notify, scheduleDate, replyToTopMsg);
                }
            });
        }
        for (MessageObject msgObj2 : singleMessages) {
            final ArrayList<MessageObject> single = new ArrayList<>();
            single.add(msgObj2);
            ensureMediaDownloadedForGroup(accountInstance, single, new Runnable() { // from class: ni.shikatu.re_extera.Utils$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    Utils.sendGroup(accountInstance, single, peer, notify, scheduleDate, replyToTopMsg);
                }
            });
        }
    }

    private static void ensureMediaDownloadedForGroup(AccountInstance accountInstance, ArrayList<MessageObject> messages, final Runnable onComplete) {
        final AtomicInteger downloadCount = new AtomicInteger(0);
        Iterator<MessageObject> it = messages.iterator();
        while (it.hasNext()) {
            TLRPC.Message original = it.next().messageOwner;
            File file = FileLoader.getInstance(accountInstance.getCurrentAccount()).getPathToMessage(original);
            if (file == null || !file.exists()) {
                if ((original.media instanceof TLRPC.TL_messageMediaPhoto) || (original.media instanceof TLRPC.TL_messageMediaDocument)) {
                    downloadCount.incrementAndGet();
                }
            }
        }
        Global.log("NoforwardsHook: Files to download: " + downloadCount.get());
        if (downloadCount.get() == 0) {
            Global.log("NoforwardsHook: All files already downloaded, calling onComplete");
            if (onComplete != null) {
                onComplete.run();
                return;
            }
            return;
        }
        for (MessageObject msgObj : messages) {
            TLRPC.Message original2 = msgObj.messageOwner;
            File file2 = FileLoader.getInstance(accountInstance.getCurrentAccount()).getPathToMessage(original2);
            if (file2 == null || !file2.exists()) {
                if ((original2.media instanceof TLRPC.TL_messageMediaPhoto) || (original2.media instanceof TLRPC.TL_messageMediaDocument)) {
                    ensureMediaDownloaded(accountInstance, msgObj, new Runnable() { // from class: ni.shikatu.re_extera.Utils$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            Utils.lambda$ensureMediaDownloadedForGroup$2(downloadCount, onComplete);
                        }
                    });
                }
            }
        }
    }

    static /* synthetic */ void lambda$ensureMediaDownloadedForGroup$2(AtomicInteger downloadCount, Runnable onComplete) {
        int remaining = downloadCount.decrementAndGet();
        Global.log("NoforwardsHook: File downloaded, remaining: " + remaining);
        if (remaining == 0) {
            Global.log("NoforwardsHook: All files downloaded, calling onComplete");
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private static void ensureMediaDownloaded(AccountInstance accountInstance, MessageObject msgObj, Runnable onComplete) {
        TLRPC.Message original = msgObj.messageOwner;
        FileLoader fileLoader = FileLoader.getInstance(accountInstance.getCurrentAccount());
        File file = fileLoader.getPathToMessage(original);
        if (file != null && file.exists()) {
            Global.log("NoforwardsHook: File already exists: " + file.getAbsolutePath());
            if (onComplete != null) {
                onComplete.run();
                return;
            }
            return;
        }
        String fileName = FileLoader.getAttachFileName(msgObj.getDocument());
        Global.log("NoforwardsHook: Starting download for: " + fileName);
        NotificationCenter.NotificationCenterDelegate observer = new AnonymousClass1(fileName, msgObj, onComplete);
        NotificationCenter nc = NotificationCenter.getInstance(accountInstance.getCurrentAccount());
        nc.addObserver(observer, NotificationCenter.fileLoaded);
        nc.addObserver(observer, NotificationCenter.fileLoadFailed);
        nc.addObserver(observer, NotificationCenter.fileLoadProgressChanged);
        if (!(original.media instanceof TLRPC.TL_messageMediaPhoto)) {
            if (original.media instanceof TLRPC.TL_messageMediaDocument) {
                TLRPC.TL_messageMediaDocument mediaDoc = original.media;
                Global.log("NoforwardsHook: Loading document...");
                fileLoader.loadFile(mediaDoc.document, msgObj, 3, 0);
                return;
            }
            return;
        }
        TLRPC.TL_messageMediaPhoto mediaPhoto = original.media;
        TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(mediaPhoto.photo.sizes, AndroidUtilities.getPhotoSize());
        if (photoSize != null) {
            Global.log("NoforwardsHook: Loading photo...");
            fileLoader.loadFile(ImageLocation.getForPhoto(photoSize, mediaPhoto.photo), msgObj, (String) null, 3, 0);
        } else {
            Global.log("NoforwardsHook: PhotoSize is null!");
        }
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.Utils$1, reason: invalid class name */
    class AnonymousClass1 implements NotificationCenter.NotificationCenterDelegate {
        final /* synthetic */ String val$fileName;
        final /* synthetic */ MessageObject val$msgObj;
        final /* synthetic */ Runnable val$onComplete;

        AnonymousClass1(String str, MessageObject messageObject, Runnable runnable) {
            this.val$fileName = str;
            this.val$msgObj = messageObject;
            this.val$onComplete = runnable;
        }

        public void didReceivedNotification(int id, int account, Object... args) {
            String loadedFileName = (String) args[0];
            if (this.val$fileName.equals(loadedFileName)) {
                final NotificationCenter nc = NotificationCenter.getInstance(account);
                if (id == NotificationCenter.fileLoadProgressChanged) {
                    Long loadedSize = (Long) args[1];
                    Long totalSize = (Long) args[2];
                    if (loadedSize != null && totalSize != null) {
                        int progress = (int) ((loadedSize.longValue() * 100) / totalSize.longValue());
                        if (Utils.alertForward != null) {
                            Utils.alertForward.setProgress(progress);
                        } else {
                            Context context = LaunchActivity.getLastFragment().getContext();
                            Utils.alertForward = new AlertDialog(context, 2);
                            Utils.alertForward.setMessage(Localization.FORWARDED);
                            Utils.alertForward.setProgress(progress);
                            Utils.alertForward.setCanCancel(false);
                            Utils.alertForward.setCancelDialog(false);
                            Utils.alertForward.setCanceledOnTouchOutside(false);
                            Utils.alertForward.setCancelable(false);
                            AlertDialog alertDialog = Utils.alertForward;
                            String str = Localization.CANCEL;
                            final MessageObject messageObject = this.val$msgObj;
                            alertDialog.setNegativeButton(str, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.Utils$1$$ExternalSyntheticLambda0
                                public final void onClick(AlertDialog alertDialog2, int i) {
                                    this.f$0.m2lambda$didReceivedNotification$0$nishikature_exteraUtils$1(messageObject, nc, alertDialog2, i);
                                }
                            });
                            Utils.alertForward.show();
                        }
                        Global.log("NoforwardsHook: Download progress: " + progress + "%");
                        return;
                    }
                    return;
                }
                if (id == NotificationCenter.fileLoaded) {
                    Global.log("NoforwardsHook: File loaded: " + loadedFileName);
                    if (Utils.alertForward != null) {
                        Utils.alertForward.cancel();
                        Utils.alertForward = null;
                    }
                    nc.removeObserver(this, NotificationCenter.fileLoaded);
                    nc.removeObserver(this, NotificationCenter.fileLoadFailed);
                    nc.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
                    if (this.val$onComplete != null) {
                        Global.log("NoforwardsHook: Calling onComplete callback");
                        AndroidUtilities.runOnUIThread(this.val$onComplete);
                        return;
                    }
                    return;
                }
                if (id == NotificationCenter.fileLoadFailed) {
                    Global.log("NoforwardsHook: File load failed: " + loadedFileName);
                    nc.removeObserver(this, NotificationCenter.fileLoaded);
                    nc.removeObserver(this, NotificationCenter.fileLoadFailed);
                    nc.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
                    if (Utils.alertForward != null) {
                        Utils.alertForward.cancel();
                        Utils.alertForward = null;
                    }
                    if (this.val$onComplete != null) {
                        Global.log("NoforwardsHook: Calling onComplete after failure");
                        AndroidUtilities.runOnUIThread(this.val$onComplete);
                    }
                }
            }
        }

        /* JADX INFO: renamed from: lambda$didReceivedNotification$0$ni-shikatu-re_extera-Utils$1, reason: not valid java name */
        /* synthetic */ void m2lambda$didReceivedNotification$0$nishikature_exteraUtils$1(MessageObject msgObj, NotificationCenter nc, AlertDialog dialog, int which) {
            if (dialog != null) {
                dialog.cancel();
            }
            FileLoader.getInstance(UserConfig.selectedAccount).cancelLoadFile(msgObj.getDocument(), false);
            nc.removeObserver(this, NotificationCenter.fileLoaded);
            nc.removeObserver(this, NotificationCenter.fileLoadFailed);
            nc.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference failed for: r1v16 */
    /* JADX WARN: Type inference failed for: r1v3 */
    /* JADX WARN: Type inference failed for: r1v4, types: [boolean, int] */
    public static void sendGroup(AccountInstance accountInstance, ArrayList<MessageObject> messages, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        if (messages.isEmpty()) {
            return;
        }
        Global.log("NoforwardsHook: sendGroup called with " + messages.size() + " messages");
        ?? r1 = 0;
        boolean isGrouped = messages.size() > 1;
        ArrayList<SendMessagesHelper.SendingMediaInfo> mediaInfos = new ArrayList<>();
        boolean hasMedia = false;
        int i = 0;
        while (i < messages.size()) {
            MessageObject msgObj = messages.get(i);
            TLRPC.Message original = msgObj.messageOwner;
            if ((original.media instanceof TLRPC.TL_messageMediaPhoto) || ((original.media instanceof TLRPC.TL_messageMediaDocument) && MessageObject.isVideoDocument(original.media.document))) {
                SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                String path = FileLoader.getInstance(accountInstance.getCurrentAccount()).getPathToMessage(original).getAbsolutePath();
                Global.log("NoforwardsHook: Media path: " + path);
                info.path = path;
                if (i == 0 && original.message != null) {
                    info.caption = original.message;
                    info.entities = original.entities;
                }
                if (original.media instanceof TLRPC.TL_messageMediaPhoto) {
                    info.isVideo = r1;
                } else {
                    TLRPC.TL_messageMediaDocument mediaDoc = original.media;
                    info.isVideo = MessageObject.isVideoDocument(mediaDoc.document);
                    info.ttl = r1;
                    TLRPC.Document document = original.media.document;
                    if (info.isVideo) {
                        int width = -1;
                        int height = -1;
                        int duration = -1;
                        for (TLRPC.TL_documentAttributeVideo tL_documentAttributeVideo : document.attributes) {
                            boolean hasMedia2 = hasMedia;
                            boolean hasMedia3 = tL_documentAttributeVideo instanceof TLRPC.TL_documentAttributeVideo;
                            if (hasMedia3) {
                                TLRPC.TL_documentAttributeVideo videoAttr = tL_documentAttributeVideo;
                                width = videoAttr.w;
                                height = videoAttr.h;
                                duration = (int) videoAttr.duration;
                                break;
                            }
                            hasMedia = hasMedia2;
                        }
                        VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
                        videoEditedInfo.roundVideo = true;
                        videoEditedInfo.startTime = -1L;
                        videoEditedInfo.endTime = -1L;
                        videoEditedInfo.bitrate = -1;
                        videoEditedInfo.originalPath = path;
                        videoEditedInfo.estimatedSize = 0L;
                        videoEditedInfo.estimatedDuration = duration;
                        videoEditedInfo.resultWidth = width;
                        videoEditedInfo.resultHeight = height;
                        videoEditedInfo.originalWidth = width;
                        videoEditedInfo.originalHeight = height;
                    }
                }
                mediaInfos.add(info);
                hasMedia = true;
            } else {
                sendNonMediaMessage(accountInstance, msgObj, peer, notify, scheduleDate, replyToTopMsg);
            }
            i++;
            r1 = 0;
        }
        if (hasMedia && !mediaInfos.isEmpty()) {
            Global.log("NoforwardsHook: Calling prepareSendingMedia with " + mediaInfos.size() + " items, grouped=" + isGrouped);
            SendMessagesHelper.prepareSendingMedia(accountInstance, mediaInfos, peer, (MessageObject) null, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, false, isGrouped, (MessageObject) null, notify, scheduleDate, 0, false, (InputContentInfoCompat) null, (String) null, 0, 0L, false, 0L, 0L, (MessageSuggestionParams) null);
        }
    }

    private static void sendNonMediaMessage(AccountInstance accountInstance, MessageObject msgObj, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        TLRPC.Message original = msgObj.messageOwner;
        if (original.media instanceof TLRPC.TL_messageMediaGeo) {
            TLRPC.TL_messageMediaGeo mediaGeo = original.media;
            SendMessagesHelper.SendMessageParams sendParams = SendMessagesHelper.SendMessageParams.of(mediaGeo, peer, (MessageObject) null, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams);
            return;
        }
        if (original.media instanceof TLRPC.TL_messageMediaVenue) {
            TLRPC.TL_messageMediaVenue venue = original.media;
            SendMessagesHelper.SendMessageParams sendParams2 = SendMessagesHelper.SendMessageParams.of(venue, peer, (MessageObject) null, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams2);
            return;
        }
        if (original.media instanceof TLRPC.TL_messageMediaContact) {
            TLRPC.TL_messageMediaContact contact = original.media;
            TLRPC.TL_user tL_user = new TLRPC.TL_user();
            ((TLRPC.User) tL_user).id = contact.user_id;
            ((TLRPC.User) tL_user).first_name = contact.first_name;
            ((TLRPC.User) tL_user).last_name = contact.last_name;
            ((TLRPC.User) tL_user).phone = contact.phone_number;
            SendMessagesHelper.SendMessageParams sendParams3 = SendMessagesHelper.SendMessageParams.of(tL_user, peer, (MessageObject) null, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams3);
            return;
        }
        if (original.media instanceof TLRPC.TL_messageMediaPoll) {
            TLRPC.TL_messageMediaPoll mediaPoll = original.media;
            SendMessagesHelper.SendMessageParams sendParams4 = SendMessagesHelper.SendMessageParams.of(mediaPoll, peer, (MessageObject) null, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams4);
            return;
        }
        if (original.media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.TL_messageMediaDocument mediaDoc = original.media;
            String path = FileLoader.getInstance(accountInstance.getCurrentAccount()).getPathToMessage(original).getAbsolutePath();
            if (!MessageObject.isStickerMessage(original)) {
                if (!MessageObject.isVoiceMessage(original)) {
                    if (MessageObject.isRoundVideoMessage(original)) {
                        TLRPC.TL_document document = msgObj.getDocument();
                        int width = 0;
                        int height = 0;
                        int duration = 0;
                        for (TLRPC.TL_documentAttributeVideo tL_documentAttributeVideo : document.attributes) {
                            if (tL_documentAttributeVideo instanceof TLRPC.TL_documentAttributeVideo) {
                                TLRPC.TL_documentAttributeVideo videoAttr = tL_documentAttributeVideo;
                                width = videoAttr.w;
                                height = videoAttr.h;
                                duration = (int) videoAttr.duration;
                                break;
                            }
                        }
                        VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
                        videoEditedInfo.roundVideo = true;
                        videoEditedInfo.startTime = -1L;
                        videoEditedInfo.endTime = -1L;
                        videoEditedInfo.bitrate = -1;
                        videoEditedInfo.originalPath = path;
                        videoEditedInfo.estimatedSize = 0L;
                        videoEditedInfo.estimatedDuration = duration;
                        videoEditedInfo.resultWidth = width;
                        videoEditedInfo.resultHeight = height;
                        videoEditedInfo.originalWidth = width;
                        videoEditedInfo.originalHeight = height;
                        SendMessagesHelper.SendMessageParams sendParams5 = SendMessagesHelper.SendMessageParams.of(document, videoEditedInfo, path, peer, (MessageObject) null, replyToTopMsg, (String) null, (ArrayList) null, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, msgObj, (MessageObject.SendAnimationData) null, false, false);
                        accountInstance.getSendMessagesHelper().sendMessage(sendParams5);
                        return;
                    }
                    SendMessagesHelper.prepareSendingDocument(accountInstance, path, path, (Uri) null, original.message, (String) null, peer, (MessageObject) null, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject) null, notify, scheduleDate, (InputContentInfoCompat) null, (String) null, 0, false);
                    return;
                }
                SendMessagesHelper.SendMessageParams sendParams6 = SendMessagesHelper.SendMessageParams.of(msgObj.getDocument(), (VideoEditedInfo) null, path, peer, (MessageObject) null, replyToTopMsg, (String) null, (ArrayList) null, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, msgObj, (MessageObject.SendAnimationData) null, false, false);
                accountInstance.getSendMessagesHelper().sendMessage(sendParams6);
                return;
            }
            accountInstance.getSendMessagesHelper().sendSticker(mediaDoc.document, (String) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject.SendAnimationData) null, notify, scheduleDate, false, msgObj, (String) null, 0, 0L, 0L, (MessageSuggestionParams) null);
            return;
        }
        if (original.message != null && !original.message.isEmpty()) {
            SendMessagesHelper.SendMessageParams sendParams7 = SendMessagesHelper.SendMessageParams.of(original.message, peer, (MessageObject) null, replyToTopMsg, (TLRPC.WebPage) null, true, original.entities, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, (MessageObject.SendAnimationData) null, false);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams7);
        }
    }
}
