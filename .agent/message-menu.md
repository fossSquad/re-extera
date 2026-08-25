# Message context-menu additions

The long-press message menu is built by `ChatActivity.fillMessageMenu(MessageObject,
icons, items, options)`. re:extera hooks it (after) via
`hooks/chatactivity/menuhook/FillMessageMenu.java`, appending items to the three
parallel lists, and handles the option ids in
`hooks/chatactivity/menuhook/ProcessSelectedOption.java`
(before-hook on `ChatActivity.processSelectedOption(int)`).

`ProcessSelectedOption.selectedObject` caches the long-pressed `MessageObject`
(set in `FillMessageMenu`); the real selection is also read via reflection on
`ChatActivity.selectedObject`.

## Option ids
| Const | id | Item | Condition |
|---|---|---|---|
| `OPT_EDIT` | 7070 | Edit (top of menu) | `isOut() && canEditMessage(null) && !isSponsored()` |
| `OPT_MESSAGE_HISTORY` | 6363 | Message History (re:extera edit history) | `getSaveEditedMessages() && messageHasSavedEdits` |
| `OPT_READ_MESSAGE` | 6565 | Read | ghost/one-time, `!isOut()` |
| `OPT_READ_AT` | 6767 | Read at HH:mm | `isOut() && getSaveReadDate()` |
| `OPT_MESSAGE_DETAILS` | 6868 | Message Details | `!isSponsored() && messageOwner != null` (default) |
| `OPT_SAVE_TO_SAVED` | 6969 | Save to Saved Messages | `!isOut() && !isSponsored()` (default, opponent only) |
| `OPT_DELETE` | 24 | (re-delete a kept message) | — |

## Handlers (`ProcessSelectedOption`)
- **Edit (7070):** reflectively invoke private `ChatActivity.startEditingMessageObject(
  MessageObject, boolean=false)` on `thisObject`.
- **Message Details (6868):** `MessageDetailsDialog.show(thisObj, target)` — see below.
- **Save to Saved (6969):** `SendMessagesHelper.getInstance(account).sendMessage(list,
  ownId, false, false, true, 0, 0L)`, `ownId = UserConfig.getClientUserId()`.
- **History / Read / Read-at:** existing (edit-history fragment, mark-read, read-date).

## MessageDetailsDialog (`ui/MessageDetailsDialog.java`)
Lightweight `AlertDialog` with a selectable, scrollable text listing: ID, Dialog ID,
From ID, Date, Edited, Forwards, Via bot, DC, and file name/type/size — read from
`msg.messageOwner` / `msg.getDocument()` with per-field try/catch. Buttons: Copy
(`R.string.Copy` → `AndroidUtilities.addToClipboard`), Close (`R.string.Close`).
Telegraph opens a full `MessageDetailsActivity`; we use a dialog reading the same fields.

## Duplication with native (important)
exteraGram natively adds **Save** (`getShowSaveMessageButton`), **Details**
(`getShowDetailsButton`, opens `MessageDetailsPopupWrapper`) and **History**
(`getShowHistoryButton`, a sender-search) — but all three default **OFF**, so they
collide with our items only if the user enabled them. The doubled menu the user saw
was NOT native duplication — it was the **double-injection** bug (two classloaders,
every hook twice); fixed by the process-wide guard (see architecture.md). Native
items enabled by default are only Copy Photo and Report.

## Files
`hooks/chatactivity/menuhook/FillMessageMenu.java`,
`hooks/chatactivity/menuhook/ProcessSelectedOption.java`,
`ui/MessageDetailsDialog.java`, `localization/Localization.java`
(`MESSAGE_DETAILS`, `SAVE_TO_SAVED_MESSAGES`).
