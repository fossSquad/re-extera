package ni.shikatu.re_extera.utils;

import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.settings.Settings;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;

public class AttachmentSaver {
    private static final Object saveLock = new Object();

    private static File getAttachmentsDir() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dir = new File(downloads, "ReExteraAttachments");
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
                String digest = getSha256(sourceFile);
                if (digest == null) {
                    Main.log("Failed to hash attachment: " + sourceFile.getAbsolutePath());
                    return;
                }
                synchronized (saveLock) {
                    File destFile = new File(dir, "att_" + digest + ext);
                    if (destFile.exists() || findFileByDigest(dir, digest) != null) {
                        return;
                    }

                    File tempFile = new File(dir, ".att_" + digest + ".tmp");
                    copyFile(sourceFile, tempFile);
                    if (!tempFile.renameTo(destFile)) {
                        tempFile.delete();
                        throw new IllegalStateException("Cannot move saved attachment into place");
                    }
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
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[32768];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
    }

    private static String getSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[32768];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder output = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            output.append(String.format(java.util.Locale.US, "%02x", value & 255));
        }
        return output.toString();
    }

    private static File findFileByDigest(File dir, String digest) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (!file.isFile() || file.getName().startsWith(".")) {
                continue;
            }
            if (digest.equals(getSha256(file))) {
                return file;
            }
        }
        return null;
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
            if (f.isFile() && !f.getName().startsWith(".")) {
                currentSize += f.length();
            }
        }

        if (currentSize > maxSize) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });

            for (File f : files) {
                if (!f.isFile() || f.getName().startsWith(".")) continue;
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
