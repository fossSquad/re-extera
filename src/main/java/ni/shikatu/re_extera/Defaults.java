package ni.shikatu.re_extera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_forum;
import org.telegram.tgnet.tl.TL_phone;
import org.telegram.tgnet.tl.TL_stories;

public final class Defaults {
    public static final int ALWAYS = 1;
    public static final int GLOBAL_VALUE = 0;
    public static final int NEVER = -1;
    public static final List<Class<?>> readingRequests = java.util.Arrays.asList(TLRPC.TL_messages_readHistory.class, TLRPC.TL_messages_readEncryptedHistory.class, TLRPC.TL_messages_readDiscussion.class, TLRPC.TL_channels_readHistory.class, TLRPC.TL_messages_markDialogUnread.class, TLRPC.TL_messages_readMessageContents.class, TLRPC.TL_channels_readMessageContents.class);
    public static final List<Class<?>> typingRequests = java.util.Arrays.asList(TLRPC.TL_messages_setTyping.class, TLRPC.TL_messages_setEncryptedTyping.class);
    public static final List<Class<?>> storiesRequests = java.util.Arrays.asList(TL_stories.TL_stories_readStories.class, TL_stories.TL_stories_incrementStoryViews.class);
    public static final List<Class<?>> sendMessageRequests = java.util.Arrays.asList(TLRPC.TL_messages_sendMessage.class, TLRPC.TL_messages_sendMedia.class, TLRPC.TL_messages_sendMultiMedia.class, TLRPC.TL_messages_sendInlineBotResult.class, TLRPC.TL_messages_sendEncrypted.class, TLRPC.TL_messages_sendEncryptedFile.class, TLRPC.TL_messages_sendEncryptedMultiMedia.class, TLRPC.TL_messages_sendEncryptedService.class, TLRPC.TL_messages_sendReaction.class);
    public static final List<Class<?>> onlineRequests = combineOnline();

    private Defaults() {
    }

    private static List<Class<?>> combineOnline() {
        ArrayList<Class<?>> list = new ArrayList<>();
        list.addAll(sendMessageRequests);
        list.addAll(readingRequests);
        list.addAll(typingRequests);
        list.addAll(storiesRequests);
        list.add(TLRPC.TL_messages_editMessage.class);
        list.add(TLRPC.TL_messages_createChat.class);
        list.add(TLRPC.TL_channels_createChannel.class);
        list.add(TL_forum.TL_messages_createForumTopic.class);
        list.add(TLRPC.TL_channels_leaveChannel.class);
        list.add(TL_forum.TL_messages_deleteTopicHistory.class);
        list.add(TL_forum.TL_messages_editForumTopic.class);
        list.add(TLRPC.TL_messages_updatePinnedMessage.class);
        list.add(TL_stories.TL_stories_sendStory.class);
        list.add(TL_phone.requestCall.class);
        list.add(TL_phone.acceptCall.class);
        list.add(TL_phone.confirmCall.class);
        return Collections.unmodifiableList(list);
    }
}
