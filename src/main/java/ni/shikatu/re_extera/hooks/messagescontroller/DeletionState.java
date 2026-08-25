package ni.shikatu.re_extera.hooks.messagescontroller;

/**
 * Shared signal that tells the deleted-messages pipeline whether an in-flight
 * deletion was triggered by the local user (a real delete the user wants to go
 * through) or pushed by the server (a delete we want to intercept and keep).
 *
 * <p>{@code MessagesController.deleteMessages} marks a user-initiated delete via
 * {@link #markUserDelete()}. {@code NotificationCenter.postNotificationName}
 * consults {@link #isUserDelete()} to decide whether to swallow the
 * {@code messagesDeleted} broadcast (server deletes are swallowed so the message
 * stays visible; user deletes pass through) and clears the flag afterwards.
 *
 * <p>This mirrors TeleVip's {@code isDeleteMessage} design: the flag is set when
 * the user deletes and reset by the {@code messagesDeleted} notification that the
 * same delete triggers. It is a plain volatile flag on purpose — the two hooks
 * run on the same synchronous delete flow.
 */
public final class DeletionState {
    private static volatile boolean userDelete = false;

    private DeletionState() {
    }

    public static void markUserDelete() {
        userDelete = true;
    }

    public static void clearUserDelete() {
        userDelete = false;
    }

    public static boolean isUserDelete() {
        return userDelete;
    }
}
