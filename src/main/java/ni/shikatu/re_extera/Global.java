package ni.shikatu.re_extera;

import android.util.Log;

import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_forum;
import org.telegram.tgnet.tl.TL_stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Global {
	public static final List<Class<?>> readingRequests = Arrays.asList(
			TLRPC.TL_messages_readHistory.class,
			TLRPC.TL_messages_readEncryptedHistory.class,
			TLRPC.TL_messages_readDiscussion.class,
			TLRPC.TL_channels_readHistory.class,
			TLRPC.TL_channels_readMessageContents.class,
			TLRPC.TL_messages_markDialogUnread.class
	);

	public static final List<Class<?>> typingRequests = Arrays.asList(
			TLRPC.TL_messages_setTyping.class,
			TLRPC.TL_messages_setEncryptedTyping.class
	);

	public static final List<Class<?>> storiesRequests = Arrays.asList(
			TL_stories.TL_stories_readStories.class,
			TL_stories.TL_stories_incrementStoryViews.class
	);

	public static final List<Class<?>> sendMessageRequests = Arrays.asList(
			TLRPC.TL_messages_sendMessage.class,
			TLRPC.TL_messages_sendMedia.class,
			TLRPC.TL_messages_sendMultiMedia.class,
			// TLRPC.TL_messages_forwardMessage.class,
			// TLRPC.TL_messages_forwardMessages.class,
			TLRPC.TL_messages_sendInlineBotResult.class,
			TLRPC.TL_messages_sendEncrypted.class,
			TLRPC.TL_messages_sendEncryptedFile.class,
			TLRPC.TL_messages_sendEncryptedMultiMedia.class,
			TLRPC.TL_messages_sendEncryptedService.class
	);

	public static final List<Class<?>> onlineRequests = combineOnline();

	private static List<Class<?>> combineOnline() {
		// Эквивалент spread-оператора: собрать все в один список
		ArrayList<Class<?>> list = new java.util.ArrayList<>();
		list.addAll(sendMessageRequests);
		list.addAll(readingRequests);
		list.add(TLRPC.TL_messages_editMessage.class);
		list.add(TLRPC.TL_messages_createChat.class);
		list.add(TLRPC.TL_channels_createChannel.class);
		list.add(TL_forum.TL_messages_createForumTopic.class);
		list.add(TLRPC.TL_channels_leaveChannel.class);
		list.add(TL_forum.TL_messages_deleteTopicHistory.class);
		list.add(TL_forum.TL_messages_editForumTopic.class);
		list.add(TLRPC.TL_messages_updatePinnedMessage.class);
		// "requestCall", "acceptCall", "confirmCall" пропущены как строки в Kotlin-комментариях
		list.add(TL_stories.TL_stories_sendStory.class);
		list.add(TL_stories.TL_stories_readStories.class);
		return list;
	}
	public static void log(String msg){
		Log.w("re:extera", msg);
	}
}
