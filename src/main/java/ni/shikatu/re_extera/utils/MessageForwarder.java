package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class MessageForwarder {
    private static AlertDialog progressDialog;

    public static void sendMessageCopy(AccountInstance accountInstance, ArrayList<MessageObject> messages, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        if (messages.isEmpty()) {
            return;
        }
        HashMap<Long, ArrayList<MessageObject>> grouped = new HashMap<>();
        ArrayList<MessageObject> single = new ArrayList<>();
        for (MessageObject msg : messages) {
            long groupId = msg.getGroupId();
            if (groupId != 0) {
                grouped.computeIfAbsent(Long.valueOf(groupId), new Function() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda1
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return MessageForwarder.lambda$sendMessageCopy$0((Long) obj);
                    }
                }).add(msg);
            } else {
                single.add(msg);
            }
        }
        ArrayList<ArrayList<MessageObject>> batches = new ArrayList<>();
        batches.addAll(grouped.values());
        for (MessageObject msg2 : single) {
            ArrayList<MessageObject> batch = new ArrayList<>();
            batch.add(msg2);
            batches.add(batch);
        }
        sendBatchSequentially(accountInstance, batches, 0, peer, notify, scheduleDate, replyToTopMsg);
    }

    static /* synthetic */ ArrayList lambda$sendMessageCopy$0(Long k) {
        return new ArrayList();
    }

    private static void sendBatchSequentially(final AccountInstance accountInstance, final ArrayList<ArrayList<MessageObject>> batches, final int index, final long peer, final boolean notify, final int scheduleDate, final MessageObject replyToTopMsg) {
        if (index >= batches.size()) {
            return;
        }
        final ArrayList<MessageObject> batch = batches.get(index);
        ensureMediaReady(accountInstance, batch, new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MessageForwarder.lambda$sendBatchSequentially$1(accountInstance, batch, peer, notify, scheduleDate, replyToTopMsg, batches, index);
            }
        });
    }

    static /* synthetic */ void lambda$sendBatchSequentially$1(AccountInstance accountInstance, ArrayList batch, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg, ArrayList batches, int index) {
        sendBatch(accountInstance, batch, peer, notify, scheduleDate, replyToTopMsg);
        sendBatchSequentially(accountInstance, batches, index + 1, peer, notify, scheduleDate, replyToTopMsg);
    }

    private static File resolveMediaFile(AccountInstance accountInstance, TLRPC.Message message) {
        TLRPC.Document doc;
        FileLoader fileLoader = FileLoader.getInstance(accountInstance.getCurrentAccount());
        if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.PhotoSize photoSize = getPhotoSize(message);
            if (photoSize == null) {
                return null;
            }
            return resolveAttachFile(fileLoader, photoSize);
        }
        if (!(message.media instanceof TLRPC.TL_messageMediaDocument) || (doc = message.media.document) == null) {
            return null;
        }
        return resolveAttachFile(fileLoader, doc);
    }

    private static File resolveAttachFile(FileLoader fileLoader, TLObject attach) {
        File decrypted;
        File decrypted2;
        File file = fileLoader.getPathToAttach(attach, false);
        if (file != null && file.exists() && file.length() > 0) {
            return file;
        }
        File file2 = fileLoader.getPathToAttach(attach, true);
        if (file2 != null && file2.exists() && file2.length() > 0) {
            return file2;
        }
        String fileName = FileLoader.getAttachFileName(attach);
        File cacheDir = FileLoader.getDirectory(4);
        File encFile = new File(cacheDir, fileName + ".enc");
        if (encFile.exists() && encFile.length() > 0 && (decrypted2 = decryptCacheFile(encFile, fileName)) != null && decrypted2.exists() && decrypted2.length() > 0) {
            return decrypted2;
        }
        File imageDir = FileLoader.getDirectory(0);
        File file3 = new File(imageDir, fileName);
        if (file3.exists() && file3.length() > 0) {
            return file3;
        }
        File encFile2 = new File(imageDir, fileName + ".enc");
        if (!encFile2.exists() || encFile2.length() <= 0 || (decrypted = decryptCacheFile(encFile2, fileName)) == null || !decrypted.exists() || decrypted.length() <= 0) {
            return null;
        }
        return decrypted;
    }

    private static File decryptCacheFile(File encFile, String outputFileName) {
        try {
            File keyFile = new File(FileLoader.getInternalCacheDir(), encFile.getName() + ".key");
            if (!keyFile.exists()) {
                Main.log("ForwardCopy: key file not found: %s", keyFile.getName());
                return null;
            }
            byte[] encryptKey = new byte[32];
            byte[] encryptIv = new byte[16];
            RandomAccessFile keyRaf = new RandomAccessFile(keyFile, "r");
            try {
                if (keyRaf.length() >= 48) {
                    keyRaf.readFully(encryptKey);
                    keyRaf.readFully(encryptIv);
                    keyRaf.close();
                    RandomAccessFile dataRaf = new RandomAccessFile(encFile, "r");
                    try {
                        byte[] data = new byte[(int) dataRaf.length()];
                        dataRaf.readFully(data);
                        dataRaf.close();
                        byte[] ivCopy = Arrays.copyOf(encryptIv, encryptIv.length);
                        ivCopy[12] = 0;
                        ivCopy[13] = 0;
                        ivCopy[14] = 0;
                        ivCopy[15] = 0;
                        Utilities.aesCtrDecryptionByteArray(data, encryptKey, ivCopy, 0, data.length, 0);
                        File outputFile = new File(FileLoader.getDirectory(4), "forward_" + outputFileName);
                        FileOutputStream fos = new FileOutputStream(outputFile);
                        try {
                            fos.write(data);
                            fos.close();
                            return outputFile;
                        } catch (Throwable th) {
                            try {
                                fos.close();
                                throw th;
                            } catch (Throwable th2) {
                                Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                                throw th;
                            }
                        }
                    } catch (Throwable th3) {
                        try {
                            dataRaf.close();
                            throw th3;
                        } catch (Throwable th4) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th3, th4);
                            throw th3;
                        }
                    }
                }
                keyRaf.close();
                return null;
            } catch (Throwable th5) {
                try {
                    keyRaf.close();
                    throw th5;
                } catch (Throwable th6) {
                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th5, th6);
                    throw th5;
                }
            }
            Main.log("ForwardCopy: decrypt error: %s", e.getMessage());
            return null;
        } catch (Exception e) {
            Main.log("ForwardCopy: decrypt error: %s", e.getMessage());
            return null;
        }
    }

    private static void ensureMediaReady(AccountInstance accountInstance, ArrayList<MessageObject> messages, final Runnable onComplete) {
        ArrayList<MessageObject> toDownload = new ArrayList<>();
        for (MessageObject msg : messages) {
            if ((msg.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) || (msg.messageOwner.media instanceof TLRPC.TL_messageMediaDocument)) {
                File file = resolveMediaFile(accountInstance, msg.messageOwner);
                if (file == null || !file.exists()) {
                    toDownload.add(msg);
                }
            }
        }
        if (toDownload.isEmpty()) {
            onComplete.run();
            return;
        }
        final AtomicInteger remaining = new AtomicInteger(toDownload.size());
        Iterator<MessageObject> it = toDownload.iterator();
        while (it.hasNext()) {
            downloadMedia(accountInstance, it.next(), new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    MessageForwarder.lambda$ensureMediaReady$2(remaining, onComplete);
                }
            });
        }
    }

    static /* synthetic */ void lambda$ensureMediaReady$2(AtomicInteger remaining, Runnable onComplete) {
        if (remaining.decrementAndGet() == 0) {
            AndroidUtilities.runOnUIThread(onComplete);
        }
    }

    private static void downloadMedia(AccountInstance accountInstance, MessageObject msgObj, final Runnable onComplete) {
        final String fileName;
        final TLRPC.Message original = msgObj.messageOwner;
        FileLoader fileLoader = FileLoader.getInstance(accountInstance.getCurrentAccount());
        if (original.media instanceof TLRPC.TL_messageMediaDocument) {
            fileName = FileLoader.getAttachFileName(original.media.document);
        } else if (original.media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.PhotoSize ps = getPhotoSize(original);
            if (ps == null) {
                onComplete.run();
                return;
            }
            fileName = FileLoader.getAttachFileName(ps);
        } else {
            onComplete.run();
            return;
        }
        NotificationCenter.NotificationCenterDelegate observer = new NotificationCenter.NotificationCenterDelegate() { // from class: ni.shikatu.re_extera.utils.MessageForwarder.1
            public void didReceivedNotification(int id, int account, Object... args) {
                if (fileName.equals(args[0])) {
                    NotificationCenter nc = NotificationCenter.getInstance(account);
                    if (id == NotificationCenter.fileLoadProgressChanged) {
                        Long loaded = (Long) args[1];
                        Long total = (Long) args[2];
                        int progress = (int) ((loaded.longValue() * 100) / total.longValue());
                        MessageForwarder.updateProgress(original, progress, nc, this);
                        return;
                    }
                    if (id == NotificationCenter.fileLoaded || id == NotificationCenter.fileLoadFailed) {
                        nc.removeObserver(this, NotificationCenter.fileLoaded);
                        nc.removeObserver(this, NotificationCenter.fileLoadFailed);
                        nc.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
                        MessageForwarder.dismissProgress();
                        onComplete.run();
                    }
                }
            }
        };
        NotificationCenter nc = NotificationCenter.getInstance(accountInstance.getCurrentAccount());
        nc.addObserver(observer, NotificationCenter.fileLoaded);
        nc.addObserver(observer, NotificationCenter.fileLoadFailed);
        nc.addObserver(observer, NotificationCenter.fileLoadProgressChanged);
        if (!(original.media instanceof TLRPC.TL_messageMediaPhoto)) {
            if (original.media instanceof TLRPC.TL_messageMediaDocument) {
                fileLoader.loadFile(original.media.document, msgObj, 3, 0);
            }
        } else {
            TLRPC.TL_messageMediaPhoto mp = original.media;
            TLRPC.PhotoSize ps2 = getPhotoSize(original);
            if (ps2 != null) {
                fileLoader.loadFile(ImageLocation.getForPhoto(ps2, mp.photo), msgObj, (String) null, 3, 0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateProgress(final TLRPC.Message original, int progress, final NotificationCenter nc, final NotificationCenter.NotificationCenterDelegate observer) {
        if (progressDialog != null) {
            progressDialog.setProgress(progress);
            return;
        }
        try {
            Context context = LaunchActivity.getLastFragment().getContext();
            progressDialog = new AlertDialog(context, 2);
            progressDialog.setMessage(Localization.FORWARDED);
            progressDialog.setProgress(progress);
            progressDialog.setCanCancel(false);
            progressDialog.setCancelDialog(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.setNegativeButton(Localization.CANCEL, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda0
                public final void onClick(AlertDialog alertDialog, int i) {
                    MessageForwarder.lambda$updateProgress$3(original, nc, observer, alertDialog, i);
                }
            });
            progressDialog.show();
        } catch (Exception e) {
        }
    }

    static /* synthetic */ void lambda$updateProgress$3(TLRPC.Message original, NotificationCenter nc, NotificationCenter.NotificationCenterDelegate observer, AlertDialog dialog, int which) {
        TLRPC.PhotoSize ps;
        dialog.cancel();
        FileLoader fl = FileLoader.getInstance(UserConfig.selectedAccount);
        if (original.media instanceof TLRPC.TL_messageMediaDocument) {
            fl.cancelLoadFile(original.media.document, false);
        } else if ((original.media instanceof TLRPC.TL_messageMediaPhoto) && (ps = getPhotoSize(original)) != null) {
            fl.cancelLoadFile(ps, false);
        }
        nc.removeObserver(observer, NotificationCenter.fileLoaded);
        nc.removeObserver(observer, NotificationCenter.fileLoadFailed);
        nc.removeObserver(observer, NotificationCenter.fileLoadProgressChanged);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void dismissProgress() {
        if (progressDialog != null) {
            try {
                progressDialog.cancel();
            } catch (Exception e) {
            }
            progressDialog = null;
        }
    }

    private static void sendBatch(AccountInstance accountInstance, ArrayList<MessageObject> messages, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        if (messages.isEmpty()) {
            return;
        }
        boolean isGrouped = messages.size() > 1;
        ArrayList<SendMessagesHelper.SendingMediaInfo> mediaInfos = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            MessageObject msgObj = messages.get(i);
            TLRPC.Message original = msgObj.messageOwner;
            boolean isPhoto = original.media instanceof TLRPC.TL_messageMediaPhoto;
            boolean isVideo = (original.media instanceof TLRPC.TL_messageMediaDocument) && MessageObject.isVideoDocument(original.media.document) && !MessageObject.isVideoSticker(original.media.document);
            if (isPhoto || isVideo) {
                File mediaFile = resolveMediaFile(accountInstance, original);
                if (mediaFile != null && mediaFile.exists()) {
                    SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                    info.path = mediaFile.getAbsolutePath();
                    info.isVideo = isVideo;
                    if (i == 0 && original.message != null) {
                        info.caption = original.message;
                        info.entities = original.entities;
                    }
                    mediaInfos.add(info);
                }
            } else {
                sendNonMediaMessage(accountInstance, msgObj, peer, notify, scheduleDate, replyToTopMsg);
            }
        }
        if (!mediaInfos.isEmpty()) {
            SendMessagesHelper.prepareSendingMedia(accountInstance, mediaInfos, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, false, isGrouped, (MessageObject) null, notify, scheduleDate, 0, 0, false, (InputContentInfoCompat) null, (String) null, 0, 0L, false, 0L, 0L, (MessageSuggestionParams) null);
        }
    }

    private static void sendNonMediaMessage(AccountInstance accountInstance, MessageObject msgObj, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        AccountInstance accountInstance2;
        String path;
        TLRPC.Message original;
        TLRPC.Message original2 = msgObj.messageOwner;
        if (original2.media == null || original2.media.document == null) {
            accountInstance2 = accountInstance;
            path = "";
        } else {
            accountInstance2 = accountInstance;
            File file = resolveMediaFile(accountInstance2, original2);
            if (file != null && file.exists()) {
                String path2 = file.getAbsolutePath();
                path = path2;
            } else {
                Main.log("ForwardCopy: document file not found for %s", Long.valueOf(original2.media.document.id));
                return;
            }
        }
        if (original2.media instanceof TLRPC.TL_messageMediaGeo) {
            accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(original2.media, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0));
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaVenue) {
            accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(original2.media, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0));
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaContact) {
            TLRPC.TL_messageMediaContact contact = original2.media;
            TLRPC.TL_user tL_user = new TLRPC.TL_user();
            ((TLRPC.User) tL_user).id = contact.user_id;
            ((TLRPC.User) tL_user).first_name = contact.first_name;
            ((TLRPC.User) tL_user).last_name = contact.last_name;
            ((TLRPC.User) tL_user).phone = contact.phone_number;
            accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(tL_user, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0));
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaPoll) {
            accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(original2.media, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0));
            return;
        }
        if (!(original2.media instanceof TLRPC.TL_messageMediaDocument)) {
            if (original2.message != null && !original2.message.isEmpty()) {
                accountInstance.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(original2.message, peer, replyToTopMsg, replyToTopMsg, (TLRPC.WebPage) null, true, original2.entities, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, (MessageObject.SendAnimationData) null, false));
                return;
            }
            return;
        }
        TLRPC.Document document = original2.media.document;
        if (MessageObject.isStickerMessage(original2) || MessageObject.isAnimatedStickerMessage(original2) || MessageObject.isVideoSticker(document)) {
            original = original2;
            accountInstance.getSendMessagesHelper().sendSticker(document, (String) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject.SendAnimationData) null, notify, scheduleDate, 0, false, msgObj, (String) null, 0, 0L, 0L, (MessageSuggestionParams) null);
        } else if (MessageObject.isVoiceMessage(original2)) {
            accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(msgObj.getDocument(), (VideoEditedInfo) null, path, peer, replyToTopMsg, replyToTopMsg, (String) null, (ArrayList) null, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, 0, msgObj, (MessageObject.SendAnimationData) null, false, false));
            original = original2;
        } else if (!MessageObject.isRoundVideoMessage(original2)) {
            SendMessagesHelper.prepareSendingDocument(accountInstance, path, path, (Uri) null, original2.message, (String) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject) null, notify, scheduleDate, (InputContentInfoCompat) null, (String) null, 0, false);
            original = original2;
        } else {
            VideoEditedInfo videoInfo = buildVideoEditedInfo(document, path, true);
            SendMessagesHelper.prepareSendingVideo(accountInstance, path, videoInfo, (String) null, (TLRPC.Photo) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (ArrayList) null, 0, (MessageObject) null, notify, 0, 0, false, false, (CharSequence) null, (String) null, 0, 0L, 0L);
            original = original2;
        }
    }

    private static TLRPC.PhotoSize getPhotoSize(TLRPC.Message message) {
        if (!(message.media instanceof TLRPC.TL_messageMediaPhoto)) {
            return null;
        }
        TLRPC.TL_messageMediaPhoto mp = message.media;
        return FileLoader.getClosestPhotoSizeWithSize(mp.photo.sizes, AndroidUtilities.getPhotoSize());
    }

    private static VideoEditedInfo buildVideoEditedInfo(TLRPC.Document document, String path, boolean isRound) {
        int width = -1;
        int height = -1;
        int duration = -1;
        for (TLRPC.TL_documentAttributeVideo tL_documentAttributeVideo : document.attributes) {
            if (tL_documentAttributeVideo instanceof TLRPC.TL_documentAttributeVideo) {
                TLRPC.TL_documentAttributeVideo v = tL_documentAttributeVideo;
                width = v.w;
                height = v.h;
                duration = (int) v.duration;
                break;
            }
        }
        VideoEditedInfo info = new VideoEditedInfo();
        info.roundVideo = isRound;
        info.startTime = -1L;
        info.endTime = -1L;
        info.bitrate = -1;
        info.originalPath = path;
        info.estimatedSize = 0L;
        info.estimatedDuration = duration;
        info.resultWidth = width;
        info.resultHeight = height;
        info.originalWidth = width;
        info.originalHeight = height;
        return info;
    }
}
