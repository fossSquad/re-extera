# ID display — message id + profile menu id

## 1. Show message id in the time area
Toggle: `Settings.getShowMessageId()` (OTHER → "Show message ID").
Rendered by `hooks/chatmessagecell/MeasureTime.java` (the SAME hook that draws the
deleted mark — see deleted-messages.md). When on and `mid != 0`, it prepends
`"ID <n>"` to `currentTimeString` and widens `timeTextWidth`/`timeWidth`.

Do NOT add a second hook on `measureTime` — it is called repeatedly and reuses the
string on early-return paths; the zero-width sentinel makes the single hook
idempotent (otherwise the id doubles). This is why a previously-separate
`ShowMessageId` hook was merged into `MeasureTime`.

## 2. Show ID in the profile/channel/group 3-dot menu
Toggle: `Settings.getShowIdInMenu()` (OTHER → "Show ID in profile menu").
`hooks/profileactivity/ProfileMenuShowId.java` hooks
`ProfileActivity.createActionBarMenu(boolean)` (after), reads the activity's private
`otherItem` (`ActionBarMenuItem`) and `userId`/`chatId` fields via reflection, and
appends `otherItem.addSubItem(id, R.drawable.msg_info, "ID: " + resolvedId)`. Tap →
`AndroidUtilities.addToClipboard` + `BulletinFactory.createSuccessBulletin(
Localization.ID_COPIED)`. Modeled on the existing `ProfileMenuShadowban` pattern.

`id` resolves to `userId` (users) or `chatId` (channels/groups), whichever is nonzero.

## Note vs native
exteraGram has `getShowIdAndDc()` which adds an ID row in the profile *list* (not the
dropdown menu) — a different UI element. An earlier attempt that force-enabled that
config was replaced by the dropdown sub-item above (what the user actually wanted).

## Files
`hooks/chatmessagecell/MeasureTime.java`, `hooks/profileactivity/ProfileMenuShowId.java`,
`localization/Localization.java` (`SHOW_MESSAGE_ID`, `SHOW_ID_IN_MENU`, `ID_COPIED`).
