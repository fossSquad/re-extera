package ni.shikatu.re_extera.hooks.connectionsmanager;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;
import ni.shikatu.re_extera.db.ReExteraDb;
import ni.shikatu.re_extera.settings.Settings;
import ni.shikatu.re_extera.utils.AccountUtils;
import ni.shikatu.re_extera.utils.InternalUtils;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

public class SendRequest extends XC_MethodHook {
    private static final TL_account.updateStatus OFFLINE_STATUS = new TL_account.updateStatus();
    private static volatile int currentReadingStatus;
    private static volatile int currentTypingStatus;

    static {
        OFFLINE_STATUS.offline = true;
    }

    public static void notifyDialogIdChanged(long dialogId) {
        currentReadingStatus = ReExteraDb.get().getDialogReading(dialogId);
        currentTypingStatus = ReExteraDb.get().getDialogTyping(dialogId);
    }

    public static int getCurrentReadingStatus() {
        return currentReadingStatus;
    }

    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
        org.telegram.tgnet.TLObject request = (org.telegram.tgnet.TLObject) param.args[0];
        if (Main.ignoredRequests.remove(request)) {
            return;
        }
        if (request instanceof TL_account.updateStatus) {
            TL_account.updateStatus statusUpdate = (TL_account.updateStatus) request;
            if (Settings.getHideOnlineWithGhost()) {
                statusUpdate.offline = true;
            }
        }
        if (Settings.getReadOnInteract() && isInteractionRequest(request)) {
            dispatchReadOnInteract(currentAccount, request);
            return;
        }
        if (Defaults.readingRequests.contains(request.getClass())) {
            applyReadingPolicy(param, currentAccount, request);
            return;
        }
        if (Defaults.typingRequests.contains(request.getClass())) {
            applyTypingPolicy(param, currentAccount, request);
        } else if (Defaults.storiesRequests.contains(request.getClass()) && Settings.getNoReadStoriesWithGhost()) {
            RequestDelegate onCompleteOrig = (RequestDelegate) param.args[1];
            if (onCompleteOrig != null) {
                try {
                    org.telegram.messenger.Utilities.stageQueue.postRunnable(() -> {
                        try {
                            onCompleteOrig.run(new TLRPC.TL_boolTrue(), null);
                        } catch (Exception ignored) {}
                    });
                } catch (Throwable ignored) {}
            }
            param.setResult((Object) null);
        }
    }

    public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
        if (Settings.getImmediateOfflineWithGhost()) {
            TLObject request = (TLObject) param.args[0];
            if (isMessageSendRequest(request)) {
                int currentAccount = AccountUtils.getCurrentAccount(param.thisObject);
                try {
                    org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                        ConnectionsManager.getInstance(currentAccount).sendRequest(OFFLINE_STATUS, (tLObject, tL_error) -> {});
                    }, 1000);
                } catch (Throwable t) {
                    ConnectionsManager.getInstance(currentAccount).sendRequest(OFFLINE_STATUS, (tLObject, tL_error) -> {});
                }
            }
        }
    }

    private static boolean isMessageSendRequest(TLObject object) {
        return (object instanceof TLRPC.TL_messages_sendMessage) ||
               (object instanceof TLRPC.TL_messages_sendMedia) ||
               (object instanceof TLRPC.TL_messages_sendMultiMedia) ||
               (object instanceof TLRPC.TL_messages_sendInlineBotResult) ||
               (object instanceof TLRPC.TL_messages_sendReaction) ||
               (object instanceof TLRPC.TL_messages_editMessage);
    }

    private static boolean isInteractionRequest(TLObject update) {
        return (update instanceof TLRPC.TL_messages_sendReaction) || (update instanceof TLRPC.TL_messages_sendVote) || (update instanceof TLRPC.TL_messages_readMentions) || (update instanceof TLRPC.TL_messages_readReactions) || (update instanceof TLRPC.TL_messages_sendMessage) || (update instanceof TLRPC.TL_messages_forwardMessages);
    }

    private static void dispatchReadOnInteract(int currentAccount, TLObject update) {
        Main.log("SendRequest: read-on-interact dispatched for %s", update.getClass().getSimpleName());
        if (update instanceof TLRPC.TL_messages_sendReaction) {
            TLRPC.TL_messages_sendReaction r = (TLRPC.TL_messages_sendReaction) update;
            InternalUtils.sendReadMessage(currentAccount, r.peer, r.msg_id, false);
            return;
        }
        if (update instanceof TLRPC.TL_messages_sendVote) {
            TLRPC.TL_messages_sendVote v = (TLRPC.TL_messages_sendVote) update;
            InternalUtils.sendReadMessage(currentAccount, v.peer, v.msg_id, false);
        } else if (update instanceof TLRPC.TL_messages_sendMessage) {
            TLRPC.TL_messages_sendMessage m = (TLRPC.TL_messages_sendMessage) update;
            InternalUtils.sendReadMessage(currentAccount, m.peer, 0, false);
        } else if (update instanceof TLRPC.TL_messages_forwardMessages) {
            TLRPC.TL_messages_forwardMessages f = (TLRPC.TL_messages_forwardMessages) update;
            InternalUtils.sendReadMessage(currentAccount, f.to_peer, 0, false);
        }
    }

    private boolean isExcludedGlobally(int currentAccount, long dialogId) {
        if (dialogId == 0) return false;
        if (dialogId > 0) {
            return Settings.getGhostExcludePMs();
        } else {
            org.telegram.tgnet.TLRPC.Chat chat = org.telegram.messenger.MessagesController.getInstance(currentAccount).getChat(Long.valueOf(-dialogId));
            if (chat != null) {
                boolean isChannel = org.telegram.messenger.ChatObject.isChannel(chat) && !chat.megagroup;
                if (isChannel) {
                    return Settings.getGhostExcludeChannels();
                } else {
                    return Settings.getGhostExcludeGroups();
                }
            } else {
                return false;
            }
        }
    }

    private static long getDialogIdFromRequest(TLObject request) {
        try {
            java.lang.reflect.Field peerField = request.getClass().getField("peer");
            if (peerField != null) {
                Object peer = peerField.get(request);
                if (peer instanceof TLRPC.TL_inputPeerUser) return ((TLRPC.TL_inputPeerUser) peer).user_id;
                if (peer instanceof TLRPC.TL_inputPeerChat) return -((TLRPC.TL_inputPeerChat) peer).chat_id;
                if (peer instanceof TLRPC.TL_inputPeerChannel) return -((TLRPC.TL_inputPeerChannel) peer).channel_id;
            }
        } catch (Exception e) {}
        try {
            java.lang.reflect.Field channelField = request.getClass().getField("channel");
            if (channelField != null) {
                Object channel = channelField.get(request);
                if (channel instanceof TLRPC.TL_inputChannel) return -((TLRPC.TL_inputChannel) channel).channel_id;
            }
        } catch (Exception e) {}
        return 0;
    }

    private void applyReadingPolicy(XC_MethodHook.MethodHookParam param, int currentAccount, TLObject request) {
        long dialogId = getDialogIdFromRequest(request);
        int status = dialogId != 0 ? ReExteraDb.get().getDialogReading(dialogId) : currentReadingStatus;
        boolean shouldBlock = false;
        switch (status) {
            case Defaults.NEVER /* -1 */:
                shouldBlock = true;
                break;
            case Defaults.GLOBAL_VALUE /* 0 */:
            default:
                if (!isExcludedGlobally(currentAccount, dialogId) && Settings.getHideReadingWithGhost()) {
                    shouldBlock = true;
                }
                break;
            case Defaults.ALWAYS /* 1 */:
                break;
        }

        if (shouldBlock) {
            RequestDelegate onCompleteOrig = (RequestDelegate) param.args[1];
            if (onCompleteOrig != null) {
                TLRPC.TL_messages_affectedMessages fakeRes = new TLRPC.TL_messages_affectedMessages();
                fakeRes.pts = -1;
                fakeRes.pts_count = 0;
                try {
                    org.telegram.messenger.Utilities.stageQueue.postRunnable(() -> {
                        try {
                            onCompleteOrig.run(fakeRes, null);
                        } catch (Exception ignored) {}
                    });
                } catch (Throwable ignored) {}
            }
            param.setResult((Object) null);
        }
    }

    private void applyTypingPolicy(XC_MethodHook.MethodHookParam param, int currentAccount, TLObject request) {
        long dialogId = getDialogIdFromRequest(request);
        int status = dialogId != 0 ? ReExteraDb.get().getDialogTyping(dialogId) : currentTypingStatus;
        switch (status) {
            case Defaults.NEVER /* -1 */:
                param.setResult((Object) null);
                break;
            case Defaults.GLOBAL_VALUE /* 0 */:
            default:
                if (!isExcludedGlobally(currentAccount, dialogId) && Settings.getHideTypingWithGhost()) {
                    param.setResult((Object) null);
                }
                break;
            case Defaults.ALWAYS /* 1 */:
                break;
        }
    }
}
