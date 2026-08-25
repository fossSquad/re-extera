package ni.shikatu.re_extera.utils;

import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;

public class AttachmentSaver {

    private static File getAttachmentsDir() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dir = new File(new File(downloads, "ReExtera"), "ReExteraAttachments");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File nomedia = new File(dir, ".nomedia");
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile();
            } catch (Exception e) {
                // Ignore
            }
        }
        return dir;
    }

    public static void saveAttachments(final int currentAccount, final long did, final ArrayList<Integer> mids) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Integer mid : mids) {
                    if (mid == null || mid <= 0) continue;
                    MessageObject msg = MessageUtils.getMessage(currentAccount, did, mid);
                    if (msg != null && msg.messageOwner != null) {
                        saveAttachment(currentAccount, msg);
                    }
                }
                checkFolderSize();
            }
        }).start();
    }

    private static void saveAttachment(int currentAccount, MessageObject msg) {
        try {
            File sourceFile = FileLoader.getInstance(currentAccount).getPathToMessage(msg.messageOwner);
            if (sourceFile != null && sourceFile.exists()) {
                File dir = getAttachmentsDir();
                String ext = getExtension(sourceFile.getName());
                String newFileName = "att_" + msg.getDialogId() + "_" + msg.getId() + ext;
                File destFile = new File(dir, newFileName);
                if (!destFile.exists()) {
                    copyFile(sourceFile, destFile);
                    Main.log("Saved attachment to: " + destFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Main.log("Failed to save attachment: " + e.getMessage());
        }
    }

    private static String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx > 0) {
            return name.substring(idx);
        }
        return "";
    }

    private static void copyFile(File sourceFile, File destFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(destFile);
             FileChannel source = fis.getChannel();
             FileChannel destination = fos.getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    private static void checkFolderSize() {
        long maxSize = Settings.getAttachmentsMaxSize();
        if (maxSize <= 0) {
            return; // Infinite
        }
        File dir = getAttachmentsDir();
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;

        long currentSize = 0;
        for (File f : files) {
            currentSize += f.length();
        }

        if (currentSize > maxSize) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });

            for (File f : files) {
                if (".nomedia".equals(f.getName())) continue;
                long size = f.length();
                if (f.delete()) {
                    currentSize -= size;
                    Main.log("Deleted old attachment to free space: " + f.getName());
                    if (currentSize <= maxSize) {
                        break;
                    }
                }
            }
        }
    }
}
