# View-once media (photo + video)

Keep view-once ("self-destruct" / once-view) photos AND videos from disappearing
after viewing. Single toggle `Settings.getSaveOneTimeMessages()` — labelled
**"Save view-once media"** on the Spy screen (independent, not gated by the deleted
master). Helper `getSaveOneTimeMessages()` reads key `save_one_time_messages`.

## How it works
1. **On receipt** — `hooks/messagescontroller/ProcessUpdates` zeroes `message.ttl`
   and `media.ttl_seconds` for incoming `TL_updateNewMessage` /
   `TL_updateNewChannelMessage` when the toggle is on, so cloud view-once media
   becomes normal media (viewable/savable, no self-destruct). Media-type agnostic.
2. **Destroy interception (ChatActivity)** — `sendSecretMediaDelete(MessageObject)` and
   `sendSecretMessageRead(MessageObject, boolean)` are hooked to no-op
   (`hooks/chatactivity/secretmedia/*`). This covers view-once **photo** (destroys on
   close).
3. **Video-specific** — a view-once VIDEO self-destructs after ONE playthrough via
   `SecretMediaViewer`'s own logic, which the ChatActivity hook doesn't cover. Fix:
   `hooks/secretmediaviewer/OpenMedia.java` hooks
   `SecretMediaViewer.openMedia(MessageObject, PhotoViewer$PhotoViewerProvider,
   Runnable, Runnable)` (after) and forces the private `ignoreDelete` field `true` —
   the viewer gates every deletion on it, so photo AND video are preserved.
   Registered via `Class.forName` for `SecretMediaViewer` +
   `PhotoViewer$PhotoViewerProvider`.
4. **Deleting task** — `MessagesController.checkDeletingTask` hooked to return false
   (blocks the pending TTL-delete task).

## Key facts (from draft/extragram)
- Once-view sentinel TTL = `0x7fffffff` (Integer.MAX_VALUE).
- `SecretMediaViewer.openMedia` @ line 8002; `ignoreDelete:Z` field @ 150;
  `closePhoto(Z,Z)Z` @ 5867. `isVoiceOnce()` @ 66795, `isRoundOnce()` @ 65529;
  no `isPhotoOnce()` (photo once = inline `messageOwner.ttl == 0x7fffffff`).
- `isSecretMedia()` is true only for `TL_message_secret` (secret chats), not cloud
  once-view — that's why the ttl-zeroing on receipt is the primary mechanism.

## History note
A photo/video split (two toggles) was briefly implemented then reverted — the user
wanted a single enable/disable toggle for the whole view-once hook. Keep it single
unless asked otherwise.

## Files
`hooks/messagescontroller/ProcessUpdates.java` (ttl zero),
`hooks/chatactivity/secretmedia/SendSecretMediaDelete.java` + `SendSecretMessageRead.java`,
`hooks/secretmediaviewer/OpenMedia.java`, `hooks/messagescontroller/CheckDeletingTask.java`.
