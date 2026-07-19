package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class MessageForwarder {
    private static AlertDialog progressDialog;

    private static final class RequiredMedia {
        final TLObject attach;
        final TLRPC.Photo photoParent;

        RequiredMedia(TLObject attach, TLRPC.Photo photoParent) {
            this.attach = attach;
            this.photoParent = photoParent;
        }
    }

    private static final class LivePhotoFile {
        final File file;
        final long videoOffset;

        LivePhotoFile(File file, long videoOffset) {
            this.file = file;
            this.videoOffset = videoOffset;
        }
    }

    public static void sendMessageCopy(AccountInstance accountInstance, ArrayList<MessageObject> messages, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        if (messages.isEmpty()) {
            return;
        }
        HashMap<Long, ArrayList<MessageObject>> grouped = new HashMap<>();
        ArrayList<MessageObject> single = new ArrayList<>();
        for (MessageObject msg : messages) {
            long groupId = msg.getGroupId();
            if (groupId != 0) {
                grouped.computeIfAbsent(Long.valueOf(groupId), new Function() { 
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
        ensureMediaReady(accountInstance, batch, new Runnable() { 
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

    private static File resolveLivePhotoVideoFile(AccountInstance accountInstance, TLRPC.Message message) {
        RequiredMedia video = getLivePhotoVideoMedia(message);
        if (video == null) {
            return null;
        }
        return resolveAttachFile(FileLoader.getInstance(accountInstance.getCurrentAccount()), video.attach);
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
            if (!getRequiredMedia(msg.messageOwner).isEmpty() && !isMediaReady(accountInstance, msg.messageOwner)) {
                toDownload.add(msg);
            }
        }
        if (toDownload.isEmpty()) {
            onComplete.run();
            return;
        }
        final AtomicInteger remaining = new AtomicInteger(toDownload.size());
        Iterator<MessageObject> it = toDownload.iterator();
        while (it.hasNext()) {
            downloadMedia(accountInstance, it.next(), new Runnable() { 
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

    private static void downloadMedia(final AccountInstance accountInstance, MessageObject msgObj, final Runnable onComplete) {
        TLRPC.Message original = msgObj.messageOwner;
        FileLoader fileLoader = FileLoader.getInstance(accountInstance.getCurrentAccount());
        ArrayList<RequiredMedia> missing = getMissingMedia(accountInstance, original);
        if (missing.isEmpty()) {
            onComplete.run();
            return;
        }
        final HashSet<String> pendingFileNames = new HashSet<>();
        for (RequiredMedia media : missing) {
            String fileName = FileLoader.getAttachFileName(media.attach);
            if (fileName != null) {
                pendingFileNames.add(fileName);
            }
        }
        if (pendingFileNames.isEmpty()) {
            onComplete.run();
            return;
        }
        final AtomicInteger remaining = new AtomicInteger(pendingFileNames.size());
        NotificationCenter.NotificationCenterDelegate observer = new NotificationCenter.NotificationCenterDelegate() { // from class: ni.shikatu.re_extera.utils.MessageForwarder.1
            public void didReceivedNotification(int id, int account, Object... args) {
                if (args.length == 0 || !(args[0] instanceof String)) {
                    return;
                }
                String fileName2 = (String) args[0];
                if (pendingFileNames.contains(fileName2)) {
                    NotificationCenter nc = NotificationCenter.getInstance(account);
                    if (id == NotificationCenter.fileLoadProgressChanged) {
                        if (args.length < 3 || !(args[1] instanceof Long) || !(args[2] instanceof Long)) {
                            return;
                        }
                        Long loaded = (Long) args[1];
                        Long total = (Long) args[2];
                        if (total.longValue() <= 0) {
                            return;
                        }
                        int progress = (int) ((loaded.longValue() * 100) / total.longValue());
                        MessageForwarder.updateProgress(progress, nc, this, accountInstance.getCurrentAccount(), pendingFileNames);
                        return;
                    }
                    if ((id == NotificationCenter.fileLoaded || id == NotificationCenter.fileLoadFailed) && pendingFileNames.remove(fileName2) && remaining.decrementAndGet() == 0) {
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
        for (RequiredMedia media2 : missing) {
            startMediaDownload(fileLoader, msgObj, media2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateProgress(int progress, final NotificationCenter nc, final NotificationCenter.NotificationCenterDelegate observer, final int currentAccount, final Collection<String> cancelFileNames) {
        if (progressDialog != null) {
            progressDialog.setProgress(progress);
            return;
        }
        try {
            BaseFragment lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment != null && lastFragment.getContext() != null) {
                Context context = lastFragment.getContext();
                progressDialog = new AlertDialog(context, 2);
                progressDialog.setMessage(Localization.FORWARDED);
                progressDialog.setProgress(progress);
                progressDialog.setCanCancel(false);
                progressDialog.setCancelDialog(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setCancelable(false);
                progressDialog.setNegativeButton(Localization.CANCEL, new AlertDialog.OnButtonClickListener() { 
                    public final void onClick(AlertDialog alertDialog, int i) {
                        MessageForwarder.lambda$updateProgress$3(currentAccount, cancelFileNames, nc, observer, alertDialog, i);
                    }
                });
                progressDialog.show();
            }
        } catch (Exception e) {
        }
    }

    static /* synthetic */ void lambda$updateProgress$3(int currentAccount, Collection cancelFileNames, NotificationCenter nc, NotificationCenter.NotificationCenterDelegate observer, AlertDialog dialog, int which) {
        dialog.cancel();
        progressDialog = null;
        FileLoader fl = FileLoader.getInstance(currentAccount);
        for (String fileName : new ArrayList(cancelFileNames)) {
            fl.cancelLoadFile(fileName);
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
            boolean isVideo = (original.media instanceof TLRPC.TL_messageMediaDocument) && !isGifLike(original) && MessageObject.isVideoDocument(original.media.document) && !MessageObject.isVideoSticker(original.media.document);
            if (isPhoto || isVideo) {
                SendMessagesHelper.SendingMediaInfo info = createMediaInfo(accountInstance, original, isVideo);
                if (info != null) {
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
        String message;
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
        if (original2.media instanceof TLRPC.TL_messageMediaToDo) {
            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of((TLRPC.TL_messageMediaPoll) null, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0);
            params.todo = original2.media;
            accountInstance2.getSendMessagesHelper().sendMessage(params);
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaDice) {
            String emoji = msgObj.getDiceEmoji(original2.media);
            if (emoji == null || emoji.isEmpty()) {
                emoji = "🎲";
            }
            accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(emoji, peer, replyToTopMsg, replyToTopMsg, (TLRPC.WebPage) null, false, (ArrayList) null, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, (MessageObject.SendAnimationData) null, false));
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaWebPage) {
            TLRPC.WebPage webPage = original2.media.webpage;
            String message2 = original2.message;
            if ((message2 == null || message2.isEmpty()) && webPage != null) {
                message = webPage.url;
            } else {
                message = message2;
            }
            if (message != null && !message.isEmpty()) {
                accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(message, peer, replyToTopMsg, replyToTopMsg, webPage, true, original2.entities, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, (MessageObject.SendAnimationData) null, false));
            }
            return;
        }
        if ((original2.media instanceof TLRPC.TL_messageMediaGame) && original2.media.game != null) {
            accountInstance2.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(original2.media.game, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0));
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaInvoice) {
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
        if (isGifLike(original2)) {
            sendDocumentCopy(accountInstance2, msgObj, document, path, peer, notify, scheduleDate, replyToTopMsg);
            original = original2;
        } else if (MessageObject.isStickerMessage(original2) || MessageObject.isAnimatedStickerMessage(original2) || MessageObject.isVideoSticker(document)) {
            original = original2;
            accountInstance.getSendMessagesHelper().sendSticker(document, (String) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject.SendAnimationData) null, notify, scheduleDate, 0, false, msgObj, (String) null, 0, 0L, 0L, (MessageSuggestionParams) null);
        } else if (MessageObject.isVoiceMessage(original2)) {
            accountInstance.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(msgObj.getDocument(), (VideoEditedInfo) null, path, peer, replyToTopMsg, replyToTopMsg, (String) null, (ArrayList) null, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, 0, msgObj, (MessageObject.SendAnimationData) null, false, false));
            original = original2;
        } else if (MessageObject.isRoundVideoMessage(original2)) {
            VideoEditedInfo videoInfo = buildVideoEditedInfo(document, path, true);
            SendMessagesHelper.prepareSendingVideo(accountInstance, path, videoInfo, (String) null, (TLRPC.Photo) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (ArrayList) null, 0, (MessageObject) null, notify, 0, 0, false, false, (CharSequence) null, (String) null, 0, 0L, 0L);
            original = original2;
        } else {
            SendMessagesHelper.prepareSendingDocument(accountInstance, path, path, (Uri) null, original2.message, (String) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject) null, notify, scheduleDate, (InputContentInfoCompat) null, (String) null, 0, false);
            original = original2;
        }
    }

    private static void sendDocumentCopy(AccountInstance accountInstance, MessageObject msgObj, TLRPC.Document document, String path, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        if (document == null || path == null || path.isEmpty()) {
            return;
        }
        File mediaFile = new File(path);
        if (mediaFile.exists() && mediaFile.length() > 0) {
            TLRPC.TL_document uploadDocument = copyDocumentForUpload(accountInstance, document, mediaFile);
            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(uploadDocument, (VideoEditedInfo) null, path, peer, replyToTopMsg, replyToTopMsg, msgObj.messageOwner.message, msgObj.messageOwner.entities, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, msgObj.messageOwner.media != null ? msgObj.messageOwner.media.ttl_seconds : 0, msgObj, (MessageObject.SendAnimationData) null, false, msgObj.messageOwner.media != null && msgObj.messageOwner.media.spoiler);
            accountInstance.getSendMessagesHelper().sendMessage(params);
        }
    }

    private static TLRPC.TL_document copyDocumentForUpload(AccountInstance accountInstance, TLRPC.Document source, File file) {
        TLRPC.TL_document copy = new TLRPC.TL_document();
        copy.id = 0L;
        copy.access_hash = 0L;
        copy.file_reference = new byte[0];
        copy.date = accountInstance.getConnectionsManager().getCurrentTime();
        copy.mime_type = source.mime_type != null ? source.mime_type : "application/octet-stream";
        copy.size = file.length();
        copy.dc_id = 0;
        copy.attributes.addAll(source.attributes);
        copy.thumbs.addAll(source.thumbs);
        copy.video_thumbs.addAll(source.video_thumbs);
        if (!copy.thumbs.isEmpty()) {
            copy.flags |= 1;
        }
        if (!copy.video_thumbs.isEmpty()) {
            copy.flags |= 2;
        }
        copy.file_name_fixed = FileLoader.getDocumentFileName(source);
        return copy;
    }

    private static boolean isGifLike(TLRPC.Message message) {
        if (message == null || !(message.media instanceof TLRPC.TL_messageMediaDocument)) {
            return false;
        }
        TLRPC.Document document = message.media.document;
        if (!MessageObject.isGifMessage(message)) {
            if (!MessageObject.isGifDocument(document, message.grouped_id != 0) && !MessageObject.isNewGifDocument(document) && !hasAnimatedDocumentAttribute(document)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAnimatedDocumentAttribute(TLRPC.Document document) {
        if (document == null || document.attributes == null) {
            return false;
        }
        for (TLRPC.DocumentAttribute attribute : document.attributes) {
            if (attribute instanceof TLRPC.TL_documentAttributeAnimated) {
                return true;
            }
        }
        return false;
    }

    private static ArrayList<RequiredMedia> getRequiredMedia(TLRPC.Message message) {
        TLRPC.Document doc;
        ArrayList<RequiredMedia> media = new ArrayList<>();
        if (message == null || message.media == null) {
            return media;
        }
        if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.TL_messageMediaPhoto mp = message.media;
            TLRPC.PhotoSize photoSize = getPhotoSize(message);
            if (photoSize != null) {
                media.add(new RequiredMedia(photoSize, mp.photo));
            }
            RequiredMedia liveVideo = getLivePhotoVideoMedia(message);
            if (liveVideo != null) {
                media.add(liveVideo);
            }
        } else if ((message.media instanceof TLRPC.TL_messageMediaDocument) && (doc = message.media.document) != null) {
            media.add(new RequiredMedia(doc, null));
        }
        return media;
    }

    private static ArrayList<RequiredMedia> getMissingMedia(AccountInstance accountInstance, TLRPC.Message message) {
        ArrayList<RequiredMedia> missing = new ArrayList<>();
        FileLoader fileLoader = FileLoader.getInstance(accountInstance.getCurrentAccount());
        for (RequiredMedia media : getRequiredMedia(message)) {
            File file = resolveAttachFile(fileLoader, media.attach);
            if (file == null || !file.exists() || file.length() <= 0) {
                missing.add(media);
            }
        }
        return missing;
    }

    private static boolean isMediaReady(AccountInstance accountInstance, TLRPC.Message message) {
        return getMissingMedia(accountInstance, message).isEmpty();
    }

    private static void startMediaDownload(FileLoader fileLoader, MessageObject msgObj, RequiredMedia media) {
        if (media.attach instanceof TLRPC.Document) {
            fileLoader.loadFile(media.attach, msgObj, 3, 0);
            return;
        }
        if ((media.attach instanceof TLRPC.PhotoSize) && media.photoParent != null) {
            fileLoader.loadFile(ImageLocation.getForPhoto(media.attach, media.photoParent), msgObj, (String) null, 3, 0);
        } else if ((media.attach instanceof TLRPC.VideoSize) && media.photoParent != null) {
            fileLoader.loadFile(ImageLocation.getForPhoto(media.attach, media.photoParent), msgObj, (String) null, 3, 0);
        }
    }

    private static SendMessagesHelper.SendingMediaInfo createMediaInfo(AccountInstance accountInstance, TLRPC.Message original, boolean isVideo) {
        if (isLivePhoto(original)) {
            return createLivePhotoMediaInfo(accountInstance, original);
        }
        File mediaFile = resolveMediaFile(accountInstance, original);
        if (mediaFile == null || !mediaFile.exists()) {
            return null;
        }
        SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
        info.path = mediaFile.getAbsolutePath();
        info.isVideo = isVideo;
        return info;
    }

    private static SendMessagesHelper.SendingMediaInfo createLivePhotoMediaInfo(AccountInstance accountInstance, TLRPC.Message original) {
        LivePhotoFile combined;
        File imageFile = resolveMediaFile(accountInstance, original);
        File videoFile = resolveLivePhotoVideoFile(accountInstance, original);
        if (imageFile == null || videoFile == null || !imageFile.exists() || !videoFile.exists() || (combined = combineLivePhotoFiles(imageFile, videoFile, original)) == null) {
            return null;
        }
        SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
        info.path = combined.file.getAbsolutePath();
        info.imagePath = imageFile.getAbsolutePath();
        info.isVideo = true;
        info.isLivePhoto = true;
        info.livePhotoVideoOffset = combined.videoOffset;
        if (original.media != null && original.media.video_timestamp > 0) {
            info.livePhotoTimestampUs = ((long) original.media.video_timestamp) * 1000000;
        }
        return info;
    }

    private static LivePhotoFile combineLivePhotoFiles(File imageFile, File videoFile, TLRPC.Message original) {
        File output = new File(FileLoader.getDirectory(4), "re_extera_live_" + original.id + "_" + System.currentTimeMillis() + ".jpg");
        try {
            FileOutputStream out = new FileOutputStream(output);
            try {
                FileInputStream imageIn = new FileInputStream(imageFile);
                try {
                    FileInputStream videoIn = new FileInputStream(videoFile);
                    try {
                        long videoOffset = copyStream(imageIn, out);
                        copyStream(videoIn, out);
                        if (videoOffset > 0 && output.length() > videoOffset) {
                            LivePhotoFile livePhotoFile = new LivePhotoFile(output, videoOffset);
                            videoIn.close();
                            imageIn.close();
                            out.close();
                            return livePhotoFile;
                        }
                        videoIn.close();
                        imageIn.close();
                        out.close();
                        return null;
                    } catch (Throwable th) {
                        try {
                            videoIn.close();
                        } catch (Throwable th2) {
                            Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th, th2);
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    try {
                        imageIn.close();
                    } catch (Throwable th4) {
                        Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th3, th4);
                    }
                    throw th3;
                }
            } catch (Throwable th5) {
                try {
                    out.close();
                } catch (Throwable th6) {
                    Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(th5, th6);
                }
                throw th5;
            }
        } catch (Exception e) {
            Main.log("ForwardCopy: live photo combine error: %s", e.getMessage());
            return null;
        }
    }

    private static long copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[65536];
        long copied = 0;
        while (true) {
            int read = in.read(buffer);
            if (read != -1) {
                out.write(buffer, 0, read);
                copied += (long) read;
            } else {
                return copied;
            }
        }
    }

    private static boolean isLivePhoto(TLRPC.Message message) {
        return message != null && (message.media instanceof TLRPC.TL_messageMediaPhoto) && message.media.live_photo;
    }

    private static RequiredMedia getLivePhotoVideoMedia(TLRPC.Message message) {
        TLRPC.VideoSize videoSize;
        if (!isLivePhoto(message)) {
            return null;
        }
        TLRPC.Document document = message.media.document;
        if (document != null && !(document instanceof TLRPC.TL_documentEmpty)) {
            return new RequiredMedia(document, null);
        }
        TLRPC.TL_messageMediaPhoto mp = message.media;
        if (mp.photo == null || mp.photo.video_sizes == null || mp.photo.video_sizes.isEmpty() || (videoSize = FileLoader.getClosestVideoSizeWithSize(mp.photo.video_sizes, 1000)) == null) {
            return null;
        }
        return new RequiredMedia(videoSize, mp.photo);
    }

    private static TLRPC.PhotoSize getPhotoSize(TLRPC.Message message) {
        if (!(message.media instanceof TLRPC.TL_messageMediaPhoto)) {
            return null;
        }
        TLRPC.TL_messageMediaPhoto mp = message.media;
        if (mp.photo == null || mp.photo.sizes == null || mp.photo.sizes.isEmpty()) {
            return null;
        }
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
