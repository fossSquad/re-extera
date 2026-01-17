package ni.shikatu.re_extera.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.localization.Localization;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

public class MessageForwarder {
    private static AlertDialog alertForward;
    private static final SendQueue sendQueue = new SendQueue();

    /* JADX INFO: Access modifiers changed from: private */
    static class SendQueue {
        private boolean isProcessing;
        private final LinkedList<Runnable> queue;

        private SendQueue() {
            this.queue = new LinkedList<>();
            this.isProcessing = false;
        }

        public synchronized void enqueue(Runnable task) {
            this.queue.add(task);
            Main.log("SendQueue: Task enqueued, queue size: " + this.queue.size(), new Object[0]);
            if (!this.isProcessing) {
                processNext();
            }
        }

        private synchronized void processNext() {
            if (this.queue.isEmpty()) {
                this.isProcessing = false;
                Main.log("SendQueue: Queue empty, stopping", new Object[0]);
            } else {
                this.isProcessing = true;
                final Runnable task = this.queue.poll();
                Main.log("SendQueue: Processing next task, remaining: " + this.queue.size(), new Object[0]);
                AndroidUtilities.runOnUIThread(new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$SendQueue$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        task.run();
                    }
                });
            }
        }

        public synchronized void signalComplete() {
            Main.log("SendQueue: Task completed, processing next", new Object[0]);
            processNext();
        }
    }

    public static void sendMessageCopy(final AccountInstance accountInstance, ArrayList<MessageObject> messages, final long peer, final boolean notify, final int scheduleDate, final MessageObject replyToTopMsg) {
        if (messages.isEmpty()) {
            return;
        }
        Main.log("NoforwardsHook: sendMessageCopy called with " + messages.size() + " messages", new Object[0]);
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
        for (Map.Entry<Long, ArrayList<MessageObject>> entry : groupedMessages.entrySet()) {
            final ArrayList<MessageObject> group2 = entry.getValue();
            sendQueue.enqueue(new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    MessageForwarder.lambda$sendMessageCopy$1(group2, accountInstance, peer, notify, scheduleDate, replyToTopMsg);
                }
            });
        }
        for (MessageObject msgObj2 : singleMessages) {
            final ArrayList<MessageObject> single = new ArrayList<>();
            single.add(msgObj2);
            sendQueue.enqueue(new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    MessageForwarder.lambda$sendMessageCopy$3(accountInstance, single, peer, notify, scheduleDate, replyToTopMsg);
                }
            });
        }
    }

    static /* synthetic */ void lambda$sendMessageCopy$1(final ArrayList group, final AccountInstance accountInstance, final long peer, final boolean notify, final int scheduleDate, final MessageObject replyToTopMsg) {
        Main.log("NoforwardsHook: Processing album with " + group.size() + " items", new Object[0]);
        ensureMediaDownloadedForGroup(accountInstance, group, new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MessageForwarder.lambda$sendMessageCopy$0(accountInstance, group, peer, notify, scheduleDate, replyToTopMsg);
            }
        });
    }

    static /* synthetic */ void lambda$sendMessageCopy$0(AccountInstance accountInstance, ArrayList group, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        sendGroup(accountInstance, group, peer, notify, scheduleDate, replyToTopMsg);
        sendQueue.signalComplete();
    }

    static /* synthetic */ void lambda$sendMessageCopy$3(final AccountInstance accountInstance, final ArrayList single, final long peer, final boolean notify, final int scheduleDate, final MessageObject replyToTopMsg) {
        Main.log("NoforwardsHook: Processing single message", new Object[0]);
        ensureMediaDownloadedForGroup(accountInstance, single, new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                MessageForwarder.lambda$sendMessageCopy$2(accountInstance, single, peer, notify, scheduleDate, replyToTopMsg);
            }
        });
    }

    static /* synthetic */ void lambda$sendMessageCopy$2(AccountInstance accountInstance, ArrayList single, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        sendGroup(accountInstance, single, peer, notify, scheduleDate, replyToTopMsg);
        sendQueue.signalComplete();
    }

    private static File getMediaFile(AccountInstance accountInstance, TLRPC.Message message) {
        FileLoader fileLoader = FileLoader.getInstance(accountInstance.getCurrentAccount());
        if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.TL_messageMediaPhoto mediaPhoto = message.media;
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(mediaPhoto.photo.sizes, AndroidUtilities.getPhotoSize());
            if (photoSize == null) {
                return null;
            }
            File file = fileLoader.getPathToAttach(photoSize, false);
            if (file != null && file.exists()) {
                Main.log("NoforwardsHook: Found photo at: " + file.getAbsolutePath(), new Object[0]);
                return file;
            }
            File file2 = fileLoader.getPathToAttach(photoSize, true);
            if (file2 != null && file2.exists()) {
                Main.log("NoforwardsHook: Found photo in cache: " + file2.getAbsolutePath(), new Object[0]);
                return file2;
            }
            String fileName = FileLoader.getAttachFileName(photoSize);
            File cacheDir = FileLoader.getDirectory(4);
            File file3 = new File(cacheDir, fileName);
            if (file3.exists()) {
                Main.log("NoforwardsHook: Found photo in MEDIA_DIR_CACHE: " + file3.getAbsolutePath(), new Object[0]);
                return file3;
            }
            File imageDir = FileLoader.getDirectory(0);
            File file4 = new File(imageDir, fileName);
            if (file4.exists()) {
                Main.log("NoforwardsHook: Found photo in MEDIA_DIR_IMAGE: " + file4.getAbsolutePath(), new Object[0]);
                return file4;
            }
            File file5 = extractPhotoFromImageCache(mediaPhoto.photo, photoSize, fileName);
            if (file5 == null || !file5.exists()) {
                Main.log("NoforwardsHook: Photo not found anywhere for: " + fileName, new Object[0]);
                return null;
            }
            Main.log("NoforwardsHook: Extracted photo from ImageLoader cache: " + file5.getAbsolutePath(), new Object[0]);
            return file5;
        }
        if (message.media instanceof TLRPC.TL_messageMediaDocument) {
            return fileLoader.getPathToMessage(message);
        }
        return null;
    }

    private static File extractPhotoFromImageCache(TLRPC.Photo photo, TLRPC.PhotoSize photoSize, String fileName) {
        File file;
        final String baseKey;
        File[] files;
        File[] files2;
        File file2 = null;
        try {
            if (photoSize.location != null) {
                baseKey = photoSize.location.volume_id + "_" + photoSize.location.local_id;
            } else {
                baseKey = null;
            }
            if (baseKey == null) {
                Main.log("NoforwardsHook: Cannot generate base key for photo search", new Object[0]);
                return null;
            }
            Main.log("NoforwardsHook: Searching for photo with base key: " + baseKey, new Object[0]);
            File cacheDir = FileLoader.getDirectory(4);
            File encFile = null;
            if (cacheDir == null || !cacheDir.exists() || (files2 = cacheDir.listFiles(new FilenameFilter() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda1
                @Override // java.io.FilenameFilter
                public final boolean accept(File file3, String str) {
                    return str.contains(baseKey);
                }
            })) == null || files2.length <= 0) {
                file = null;
            } else {
                int length = files2.length;
                int i = 0;
                while (i < length) {
                    File f = files2[i];
                    file = file2;
                    try {
                        Main.log("NoforwardsHook: Found matching file in cache: " + f.getName() + " size: " + f.length(), new Object[0]);
                        if (!f.getName().endsWith(".jpg") && !f.getName().endsWith(".png")) {
                            if (f.getName().endsWith(".enc")) {
                                encFile = f;
                            }
                            i++;
                            file2 = file;
                        }
                        return f;
                    } catch (Exception e) {
                        e = e;
                        Main.log("NoforwardsHook: Error searching photo in cache: " + e.getMessage(), new Object[0]);
                        return file;
                    }
                }
                file = file2;
            }
            File imageDir = FileLoader.getDirectory(0);
            if (imageDir != null && imageDir.exists() && (files = imageDir.listFiles(new FilenameFilter() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda2
                @Override // java.io.FilenameFilter
                public final boolean accept(File file3, String str) {
                    return str.contains(baseKey);
                }
            })) != null && files.length > 0) {
                int length2 = files.length;
                int i2 = 0;
                while (i2 < length2) {
                    File f2 = files[i2];
                    File imageDir2 = imageDir;
                    File[] files3 = files;
                    Main.log("NoforwardsHook: Found matching file in image dir: " + f2.getName() + " size: " + f2.length(), new Object[0]);
                    if (!f2.getName().endsWith(".jpg") && !f2.getName().endsWith(".png")) {
                        if (f2.getName().endsWith(".enc") && encFile == null) {
                            encFile = f2;
                        }
                        i2++;
                        imageDir = imageDir2;
                        files = files3;
                    }
                    return f2;
                }
            }
            if (encFile != null) {
                Main.log("NoforwardsHook: Only encrypted file found, trying to decrypt: " + encFile.getName(), new Object[0]);
                try {
                    File decrypted = decryptEncFile(encFile, fileName, photoSize);
                    if (decrypted != null && decrypted.exists()) {
                        return decrypted;
                    }
                } catch (Exception e2) {
                    e = e2;
                    Main.log("NoforwardsHook: Error searching photo in cache: " + e.getMessage(), new Object[0]);
                    return file;
                }
            }
            Main.log("NoforwardsHook: No matching files found for key: " + baseKey, new Object[0]);
            return file;
        } catch (Exception e3) {
            e = e3;
            file = null;
        }
    }

    private static File decryptEncFile(File encFile, String outputFileName, TLRPC.PhotoSize photoSize) {
        if (photoSize != null) {
            try {
                if (photoSize.location != null) {
                    byte[] key = photoSize.location.key;
                    byte[] iv = photoSize.location.iv;
                    if (key != null && iv != null) {
                        Main.log("NoforwardsHook: Decrypting file with key length: " + key.length, new Object[0]);
                        RandomAccessFile raf = new RandomAccessFile(encFile, "r");
                        byte[] encryptedData = new byte[(int) raf.length()];
                        raf.readFully(encryptedData);
                        raf.close();
                        byte[] ivCopy = new byte[iv.length];
                        System.arraycopy(iv, 0, ivCopy, 0, iv.length);
                        Utilities.aesIgeEncryption(ByteBuffer.wrap(encryptedData), key, ivCopy, false, true, 0, encryptedData.length);
                        File cacheDir = FileLoader.getDirectory(4);
                        File outputFile = new File(cacheDir, outputFileName);
                        FileOutputStream fos = new FileOutputStream(outputFile);
                        fos.write(encryptedData);
                        fos.close();
                        Main.log("NoforwardsHook: Decrypted file saved to: " + outputFile.getAbsolutePath(), new Object[0]);
                        return outputFile;
                    }
                    Main.log("NoforwardsHook: Cannot decrypt - no key/iv in location", new Object[0]);
                    return null;
                }
            } catch (Exception e) {
                Main.log("NoforwardsHook: Error decrypting file: " + e.getMessage(), new Object[0]);
                return null;
            }
        }
        Main.log("NoforwardsHook: Cannot decrypt - no location info", new Object[0]);
        return null;
    }

    private static void ensureMediaDownloadedForGroup(AccountInstance accountInstance, ArrayList<MessageObject> messages, final Runnable onComplete) {
        final AtomicInteger downloadCount = new AtomicInteger(0);
        Iterator<MessageObject> it = messages.iterator();
        while (it.hasNext()) {
            TLRPC.Message original = it.next().messageOwner;
            File file = getMediaFile(accountInstance, original);
            if (file == null || !file.exists()) {
                if ((original.media instanceof TLRPC.TL_messageMediaPhoto) || (original.media instanceof TLRPC.TL_messageMediaDocument)) {
                    downloadCount.incrementAndGet();
                }
            }
        }
        Main.log("NoforwardsHook: Files to download: " + downloadCount.get(), new Object[0]);
        if (downloadCount.get() == 0) {
            Main.log("NoforwardsHook: All files already downloaded, calling onComplete", new Object[0]);
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(onComplete);
                return;
            }
            return;
        }
        for (MessageObject msgObj : messages) {
            TLRPC.Message original2 = msgObj.messageOwner;
            File file2 = getMediaFile(accountInstance, original2);
            if (file2 == null || !file2.exists()) {
                if ((original2.media instanceof TLRPC.TL_messageMediaPhoto) || (original2.media instanceof TLRPC.TL_messageMediaDocument)) {
                    ensureMediaDownloaded(accountInstance, msgObj, new Runnable() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            MessageForwarder.lambda$ensureMediaDownloadedForGroup$6(downloadCount, onComplete);
                        }
                    });
                }
            }
        }
    }

    static /* synthetic */ void lambda$ensureMediaDownloadedForGroup$6(AtomicInteger downloadCount, Runnable onComplete) {
        int remaining = downloadCount.decrementAndGet();
        Main.log("NoforwardsHook: File downloaded, remaining: " + remaining, new Object[0]);
        if (remaining == 0) {
            Main.log("NoforwardsHook: All files downloaded, calling onComplete", new Object[0]);
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(onComplete);
            }
        }
    }

    private static void ensureMediaDownloaded(AccountInstance accountInstance, MessageObject msgObj, Runnable onComplete) {
        String fileName;
        Main.log(msgObj.toString(), new Object[0]);
        TLRPC.Message original = msgObj.messageOwner;
        FileLoader fileLoader = FileLoader.getInstance(accountInstance.getCurrentAccount());
        File file = getMediaFile(accountInstance, original);
        if (file != null && file.exists()) {
            Main.log("NoforwardsHook: File already exists: " + file.getAbsolutePath(), new Object[0]);
            if (onComplete != null) {
                onComplete.run();
                return;
            }
            return;
        }
        if (original.media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.Document document = original.media.document;
            Main.log("NoforwardsHook: Doc is: %s", document);
            String fileName2 = FileLoader.getAttachFileName(document);
            fileName = fileName2;
        } else if (original.media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(original.media.photo.sizes, AndroidUtilities.getPhotoSize());
            Main.log("NoforwardsHook: Photo size is: %s", photoSize);
            String fileName3 = FileLoader.getAttachFileName(photoSize);
            fileName = fileName3;
        } else {
            Main.log("NoforwardsHook: Unknown media type: %s", original.media);
            if (onComplete != null) {
                onComplete.run();
                return;
            }
            return;
        }
        Main.log("NoforwardsHook: Starting download for: " + fileName, new Object[0]);
        NotificationCenter.NotificationCenterDelegate observer = new AnonymousClass1(fileName, original, onComplete);
        NotificationCenter nc = NotificationCenter.getInstance(accountInstance.getCurrentAccount());
        nc.addObserver(observer, NotificationCenter.fileLoaded);
        nc.addObserver(observer, NotificationCenter.fileLoadFailed);
        nc.addObserver(observer, NotificationCenter.fileLoadProgressChanged);
        if (original.media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.TL_messageMediaPhoto mediaPhoto = original.media;
            TLRPC.PhotoSize photoSize2 = FileLoader.getClosestPhotoSizeWithSize(mediaPhoto.photo.sizes, AndroidUtilities.getPhotoSize());
            if (photoSize2 != null) {
                Main.log("NoforwardsHook: Loading photo, photoSize type: " + photoSize2.getClass().getSimpleName(), new Object[0]);
                Main.log("NoforwardsHook: Photo location: " + photoSize2.location, new Object[0]);
                fileLoader.loadFile(ImageLocation.getForPhoto(photoSize2, mediaPhoto.photo), msgObj, (String) null, 3, 1);
                return;
            }
            Main.log("NoforwardsHook: PhotoSize is null!", new Object[0]);
            return;
        }
        if (original.media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.TL_messageMediaDocument mediaDoc = original.media;
            Main.log("NoforwardsHook: Loading document...", new Object[0]);
            fileLoader.loadFile(mediaDoc.document, msgObj, 3, 0);
        }
    }

    /* JADX INFO: renamed from: ni.shikatu.re_extera.utils.MessageForwarder$1, reason: invalid class name */
    class AnonymousClass1 implements NotificationCenter.NotificationCenterDelegate {
        final /* synthetic */ String val$fileName;
        final /* synthetic */ Runnable val$onComplete;
        final /* synthetic */ TLRPC.Message val$original;

        AnonymousClass1(String str, TLRPC.Message message, Runnable runnable) {
            this.val$fileName = str;
            this.val$original = message;
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
                        if (MessageForwarder.alertForward != null) {
                            MessageForwarder.alertForward.setProgress(progress);
                        } else {
                            Context context = LaunchActivity.getLastFragment().getContext();
                            MessageForwarder.alertForward = new AlertDialog(context, 2);
                            MessageForwarder.alertForward.setMessage(Localization.FORWARDED);
                            MessageForwarder.alertForward.setProgress(progress);
                            MessageForwarder.alertForward.setCanCancel(false);
                            MessageForwarder.alertForward.setCancelDialog(false);
                            MessageForwarder.alertForward.setCanceledOnTouchOutside(false);
                            MessageForwarder.alertForward.setCancelable(false);
                            AlertDialog alertDialog = MessageForwarder.alertForward;
                            String str = Localization.CANCEL;
                            final TLRPC.Message message = this.val$original;
                            alertDialog.setNegativeButton(str, new AlertDialog.OnButtonClickListener() { // from class: ni.shikatu.re_extera.utils.MessageForwarder$1$$ExternalSyntheticLambda0
                                public final void onClick(AlertDialog alertDialog2, int i) {
                                    this.f$0.lambda$didReceivedNotification$0(message, nc, alertDialog2, i);
                                }
                            });
                            MessageForwarder.alertForward.show();
                        }
                        Main.log("NoforwardsHook: Download progress: " + progress, new Object[0]);
                        return;
                    }
                    return;
                }
                if (id == NotificationCenter.fileLoaded) {
                    Main.log("NoforwardsHook: File loaded: " + loadedFileName, new Object[0]);
                    if (MessageForwarder.alertForward != null) {
                        MessageForwarder.alertForward.cancel();
                        MessageForwarder.alertForward = null;
                    }
                    nc.removeObserver(this, NotificationCenter.fileLoaded);
                    nc.removeObserver(this, NotificationCenter.fileLoadFailed);
                    nc.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
                    if (this.val$onComplete != null) {
                        Main.log("NoforwardsHook: Calling onComplete callback", new Object[0]);
                        AndroidUtilities.runOnUIThread(this.val$onComplete);
                        return;
                    }
                    return;
                }
                if (id == NotificationCenter.fileLoadFailed) {
                    Main.log("NoforwardsHook: File load failed: " + loadedFileName, new Object[0]);
                    nc.removeObserver(this, NotificationCenter.fileLoaded);
                    nc.removeObserver(this, NotificationCenter.fileLoadFailed);
                    nc.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
                    if (MessageForwarder.alertForward != null) {
                        MessageForwarder.alertForward.cancel();
                        MessageForwarder.alertForward = null;
                    }
                    if (this.val$onComplete != null) {
                        Main.log("NoforwardsHook: Calling onComplete after failure", new Object[0]);
                        AndroidUtilities.runOnUIThread(this.val$onComplete);
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$didReceivedNotification$0(TLRPC.Message original, NotificationCenter nc, AlertDialog dialog, int which) {
            if (dialog != null) {
                dialog.cancel();
            }
            if (original.media instanceof TLRPC.TL_messageMediaDocument) {
                FileLoader.getInstance(UserConfig.selectedAccount).cancelLoadFile(original.media.document, false);
            } else if (original.media instanceof TLRPC.TL_messageMediaPhoto) {
                TLRPC.TL_messageMediaPhoto mp = original.media;
                TLRPC.PhotoSize ps = FileLoader.getClosestPhotoSizeWithSize(mp.photo.sizes, AndroidUtilities.getPhotoSize());
                if (ps != null) {
                    FileLoader.getInstance(UserConfig.selectedAccount).cancelLoadFile(ps, false);
                }
            }
            nc.removeObserver(this, NotificationCenter.fileLoaded);
            nc.removeObserver(this, NotificationCenter.fileLoadFailed);
            nc.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v18 */
    /* JADX WARN: Type inference failed for: r1v3 */
    private static void sendGroup(AccountInstance accountInstance, ArrayList<MessageObject> messages, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        int duration;
        int height;
        if (messages.isEmpty()) {
            return;
        }
        int i = 0;
        Main.log("NoforwardsHook: sendGroup called with " + messages.size() + " messages", new Object[0]);
        boolean isGrouped = messages.size() > 1;
        ArrayList<SendMessagesHelper.SendingMediaInfo> mediaInfos = new ArrayList<>();
        boolean hasMedia = false;
        int i2 = 0;
        while (i2 < messages.size()) {
            MessageObject msgObj = messages.get(i2);
            TLRPC.Message original = msgObj.messageOwner;
            if ((original.media instanceof TLRPC.TL_messageMediaPhoto) || ((original.media instanceof TLRPC.TL_messageMediaDocument) && MessageObject.isVideoDocument(original.media.document) && !MessageObject.isVideoSticker(original.media.document))) {
                SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                File mediaFile = getMediaFile(accountInstance, original);
                if (mediaFile == null || !mediaFile.exists()) {
                    Main.log("NoforwardsHook: Media file not found, skipping", new Object[0]);
                } else {
                    String path = mediaFile.getAbsolutePath();
                    Main.log("NoforwardsHook: Media path: " + path, new Object[i]);
                    info.path = path;
                    if (i2 == 0 && original.message != null) {
                        info.caption = original.message;
                        info.entities = original.entities;
                    }
                    if (original.media instanceof TLRPC.TL_messageMediaPhoto) {
                        info.isVideo = i;
                    } else {
                        TLRPC.TL_messageMediaDocument mediaDoc = original.media;
                        info.isVideo = MessageObject.isVideoDocument(mediaDoc.document) && !MessageObject.isVideoSticker(mediaDoc.document);
                        info.ttl = i;
                        TLRPC.Document document = original.media.document;
                        if (info.isVideo) {
                            int width = -1;
                            Iterator it = document.attributes.iterator();
                            while (true) {
                                if (!it.hasNext()) {
                                    duration = -1;
                                    height = -1;
                                    break;
                                }
                                TLRPC.TL_documentAttributeVideo tL_documentAttributeVideo = (TLRPC.DocumentAttribute) it.next();
                                boolean hasMedia2 = hasMedia;
                                boolean hasMedia3 = tL_documentAttributeVideo instanceof TLRPC.TL_documentAttributeVideo;
                                if (hasMedia3) {
                                    TLRPC.TL_documentAttributeVideo videoAttr = tL_documentAttributeVideo;
                                    width = videoAttr.w;
                                    int height2 = videoAttr.h;
                                    duration = (int) videoAttr.duration;
                                    height = height2;
                                    break;
                                }
                                hasMedia = hasMedia2;
                            }
                            VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
                            videoEditedInfo.roundVideo = MessageObject.isRoundVideoDocument(mediaDoc.document);
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
                }
                i2++;
                i = 0;
            } else {
                sendNonMediaMessage(accountInstance, msgObj, peer, notify, scheduleDate, replyToTopMsg);
                hasMedia = hasMedia;
                i2 = i2;
            }
            hasMedia = hasMedia;
            i2++;
            i = 0;
        }
        if (hasMedia && !mediaInfos.isEmpty()) {
            Main.log("NoforwardsHook: Calling prepareSendingMedia with " + mediaInfos.size() + " items, grouped=" + isGrouped, new Object[0]);
            SendMessagesHelper.prepareSendingMedia(accountInstance, mediaInfos, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, false, isGrouped, (MessageObject) null, notify, scheduleDate, 0, 0, false, (InputContentInfoCompat) null, (String) null, 0, 0L, false, 0L, 0L, (MessageSuggestionParams) null);
        }
    }

    private static void sendNonMediaMessage(AccountInstance accountInstance, MessageObject msgObj, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        String path;
        TLRPC.Message original;
        TLRPC.TL_document document;
        TLRPC.Message original2 = msgObj.messageOwner;
        if (original2.media == null || (document = original2.media.document) == null) {
            path = "";
        } else {
            File file = FileLoader.getInstance(accountInstance.getCurrentAccount()).getPathToAttach(document, false);
            if (!file.exists()) {
                file = FileLoader.getInstance(accountInstance.getCurrentAccount()).getPathToAttach(document, true);
            }
            if (!file.exists()) {
                String fileName = FileLoader.getAttachFileName(document);
                File cacheDir = FileLoader.getDirectory(4);
                file = new File(cacheDir, fileName);
            }
            if (!file.exists() || !file.canRead()) {
                FileLog.e("[re:extera] File not found after download: " + document.id);
                return;
            } else {
                FileLog.d("[re:extera] File found at: " + file.getAbsolutePath());
                String path2 = file.getAbsolutePath();
                path = path2;
            }
        }
        if (original2.media instanceof TLRPC.TL_messageMediaGeo) {
            TLRPC.TL_messageMediaGeo mediaGeo = original2.media;
            SendMessagesHelper.SendMessageParams sendParams = SendMessagesHelper.SendMessageParams.of(mediaGeo, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams);
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaVenue) {
            TLRPC.TL_messageMediaVenue venue = original2.media;
            SendMessagesHelper.SendMessageParams sendParams2 = SendMessagesHelper.SendMessageParams.of(venue, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams2);
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaContact) {
            TLRPC.TL_messageMediaContact contact = original2.media;
            TLRPC.TL_user tL_user = new TLRPC.TL_user();
            ((TLRPC.User) tL_user).id = contact.user_id;
            ((TLRPC.User) tL_user).first_name = contact.first_name;
            ((TLRPC.User) tL_user).last_name = contact.last_name;
            ((TLRPC.User) tL_user).phone = contact.phone_number;
            SendMessagesHelper.SendMessageParams sendParams3 = SendMessagesHelper.SendMessageParams.of(tL_user, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams3);
            return;
        }
        if (original2.media instanceof TLRPC.TL_messageMediaPoll) {
            TLRPC.TL_messageMediaPoll mediaPoll = original2.media;
            SendMessagesHelper.SendMessageParams sendParams4 = SendMessagesHelper.SendMessageParams.of(mediaPoll, peer, replyToTopMsg, replyToTopMsg, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams4);
            return;
        }
        if (!(original2.media instanceof TLRPC.TL_messageMediaDocument)) {
            if (original2.message != null && !original2.message.isEmpty()) {
                Main.log("Message is a text", new Object[0]);
                SendMessagesHelper.SendMessageParams sendParams5 = SendMessagesHelper.SendMessageParams.of(original2.message, peer, replyToTopMsg, replyToTopMsg, (TLRPC.WebPage) null, true, original2.entities, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, (MessageObject.SendAnimationData) null, false);
                accountInstance.getSendMessagesHelper().sendMessage(sendParams5);
                return;
            }
            return;
        }
        TLRPC.TL_messageMediaDocument mediaDoc = original2.media;
        if (MessageObject.isStickerMessage(original2) || MessageObject.isAnimatedStickerMessage(original2) || MessageObject.isVideoSticker(mediaDoc.document)) {
            Main.log("Message is sticker", new Object[0]);
            original = original2;
            accountInstance.getSendMessagesHelper().sendSticker(mediaDoc.document, (String) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject.SendAnimationData) null, notify, scheduleDate, 0, false, msgObj, (String) null, 0, 0L, 0L, (MessageSuggestionParams) null);
        } else if (MessageObject.isVoiceMessage(original2)) {
            Main.log("Message is voice", new Object[0]);
            SendMessagesHelper.SendMessageParams sendParams6 = SendMessagesHelper.SendMessageParams.of(msgObj.getDocument(), (VideoEditedInfo) null, path, peer, replyToTopMsg, replyToTopMsg, (String) null, (ArrayList) null, (TLRPC.ReplyMarkup) null, (HashMap) null, notify, scheduleDate, 0, 0, msgObj, (MessageObject.SendAnimationData) null, false, false);
            accountInstance.getSendMessagesHelper().sendMessage(sendParams6);
            original = original2;
        } else if (MessageObject.isRoundVideoMessage(original2)) {
            Main.log("Message is round video", new Object[0]);
            int width = -1;
            int height = -1;
            int duration = -1;
            for (TLRPC.TL_documentAttributeVideo tL_documentAttributeVideo : msgObj.getDocument().attributes) {
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
            SendMessagesHelper.prepareSendingVideo(accountInstance, path, videoEditedInfo, (String) null, (TLRPC.Photo) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (ArrayList) null, 0, (MessageObject) null, notify, 0, 0, false, false, (CharSequence) null, (String) null, 0, 0L, 0L);
            original = original2;
        } else {
            Main.log("Message is a document", new Object[0]);
            SendMessagesHelper.prepareSendingDocument(accountInstance, path, path, (Uri) null, original2.message, (String) null, peer, replyToTopMsg, replyToTopMsg, (TL_stories.StoryItem) null, (ChatActivity.ReplyQuote) null, (MessageObject) null, notify, scheduleDate, (InputContentInfoCompat) null, (String) null, 0, false);
            original = original2;
        }
    }
}
