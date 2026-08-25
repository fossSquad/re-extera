# Deleted messages — save, keep visible, mark

Goal: intercept server-side deletions, keep the message in Telegram's own storage,
and mark it in the chat. Toggle: `Settings.getSaveDeletedMessages()`
("Save deleted messages" — master switch on the Deleted-Message screen).

## Why it used to be flaky (root cause)
re:extera keeps messages by rewriting `MessagesStorage.markMessagesAsDeleted` args
(so server ids aren't purged) and records them, BUT it did **not** stop the
`messagesDeleted` NotificationCenter broadcast. That broadcast reaches every
consumer — the open chat, dialog list, shared media, search — and each removes the
message from view, so deleted messages "sometimes" disappeared.

## The fix (this session)
1. **Swallow the broadcast** — `hooks/notificationmanager/PostNotificationName.java`
   hooks `NotificationCenter.postNotificationName(int, Object[])`; when
   `id == NotificationCenter.messagesDeleted`, `getSaveDeletedMessages()` is on, and
   it is NOT a user-initiated delete → `param.setResult(null)` (swallow). Hot path,
   so the body is a cheap int compare first.
2. **User-delete flag** — `hooks/messagescontroller/DeletionState.java` (volatile
   flag). `DeleteMessages.beforeHookedMethod` calls `markUserDelete()` only when
   `!getSaveManuallyDeleted()` (a real delete that should disappear);
   `PostNotificationName.afterHookedMethod` clears it after the broadcast.
   Mirrors TeleVip's `isDeleteMessage`.

## Detection at render (did-independent)
`TL_updateDeleteMessages` (non-channel) carries NO dialog id, and channel deletes
record a `did` that differs from the render-time `obj.getDialogId()` — so the strict
`(did, mid)` lookup misses (mark only appeared after restart).
- `db/ReExteraDb.isMidDeletedAnyDialog(int mid)` — cache-only scan of
  `deletedKeysCache` (fully loaded at startup, kept in sync), did-independent.
- `hooks/chatmessagecell/MeasureTime.computeDeleted` → `isMessageDeleted(m)` =
  `redb.messageIsDeleted(m) || redb.isMidDeletedAnyDialog(m.getId())`. Also returns
  false early when `!getSaveDeletedMessages()` (master off → no mark).

## Immediate live refresh
`utils/MessageUtils.forceUpdateViews` matches visible cells by **message id and
account only** (dropped the `activity.getDialogId() != did` guard) — the recorded
`did` doesn't always equal the open chat's, and that guard skipped the live refresh,
so the mark only showed after a restart. It calls `cell.forceResetMessageObject()` +
`adapter.notifyItemChanged(...)` to re-run `measureTime`.

## Rendering the mark (`hooks/chatmessagecell/MeasureTime.java`)
- After-hook on `ChatMessageCell.measureTime(MessageObject)`. Edits
  `currentTimeString` + `timeTextWidth`/`timeWidth`; `onLayout` then builds the drawn
  `timeLayout` StaticLayout from `currentTimeString`, so the post-hook edit reaches
  the screen. Same mechanism TeleVip uses.
- Draws BOTH the deleted mark (icon/custom prefix, `Settings.getCustomPrefix()` /
  `getDeletedMarkColor()`) AND, when `getShowMessageId()`, `"ID <n>"` — one hook, one
  width adjustment (see id-display.md).
- **Idempotency:** `measureTime` rebuilds `currentTimeString` on its main path but
  REUSES it on early-return paths. A zero-width sentinel `'​'` at index 0 marks a
  processed string; skip if present. Without it the id/mark doubles up. Do NOT add a
  second hook on `measureTime` — keep it one.
- Deleted icon: `MeasureTime.deletedIcon`, initialized in `Main.start()` from host
  drawable `msg_delete_filled`/`msg_delete`.

## Related existing hooks (kept)
`messagesstorage/MarkMessagesAsDeletedInternal` (records + keeps), `ProcessUpdates`
(records server-deleted ids, zeroes ttl), `ChatActivity.processDeletedMessages`,
`NotificationsController.removeDeletedMessagesFromNotifications`. Menu options
(View deleted / Read / Read-at / History) live in `menuhook/` (see message-menu.md).

## Files
`hooks/notificationmanager/PostNotificationName.java`,
`hooks/messagescontroller/DeletionState.java`, `hooks/messagescontroller/DeleteMessages.java`,
`hooks/chatmessagecell/MeasureTime.java`, `db/ReExteraDb.java`, `utils/MessageUtils.java`,
registered in `HookInit.java`.
