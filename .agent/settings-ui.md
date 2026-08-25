# Settings UI structure

Root: `settings/newui/SettingsFragmentNew.java` — the "re:extera" screen with the
sticker, "Thanks", and category buttons. Fragments extend
`BasePreferencesActivityExtended`; new top-level fragments must be added to
`Main.fragments`.

## Screen tree (current)
```
re:extera
├─ Ghost mode   → GhostFragment            (icon: ghost.svg via PathIconDrawable)
├─ Spy          → DeletedAndEditedMessagesFragment  (icon: spy.svg via PathIconDrawable)
│   ├─ Save attachments (+ max folder size slider, Download/ReExtera/ReExteraAttachments)
│   ├─ Save read date / Save last online
│   ├─ Save view-once media           (view-once toggle, independent)
│   └─ Deleted Message  → CustomizationFragment
│        ├─ Save deleted messages     (MASTER switch — off hides & disables the rest)
│        ├─ Save in bot chats / Save message history / Save manually deleted / Use collapsed quote
│        ├─ preview cell
│        └─ Custom mark / color / Disable colored replies / Transparent + alpha slider
└─ Other        → AdditionalFragment
     ├─ Local Premium / Disable ads
     ├─ Work in background / Ignore FLAG_SECURE / re:forward (+ about)
     ├─ Hide pinned / Disable chat swipe / Disable profile swipe / Save protected stories / Hide TL error
     ├─ Show message ID / Show ID in profile menu / Message quick buttons
     └─ Filters / Shadowban / Clear-Export-Import DB / Unload
```
"Customization" was removed as a top-level button; it became **Deleted Message**
inside Spy (it only holds deleted-message settings). Its title = `Localization.DELETED_MESSAGE`.

## Master switch (`CustomizationFragment` = Deleted Message)
"Save deleted messages" is the master. In `fillItems`, after adding it, `if
(!Settings.getSaveDeletedMessages()) return;` hides everything below. `onClick`
toggles it with `refreshCheckBox(item, position, value, true)` (full reload so rows
appear/disappear). The dependent getters are ALSO gated at the source so they're
truly inactive when off — `getSaveBotChats/getSaveEditedMessages/getSaveManuallyDeleted/
getUseExpandableBlockQuote/getTransparentDeletedMessages/getDisableColoredReplies` each
`return getSaveDeletedMessages() && getBool(...)`, and `MeasureTime.computeDeleted`
returns false when the master is off. `getSaveReadDate`/`getSaveLastOnline` are on the
Spy screen and stay independent.

## Decompiled `onClick` caveat
Some fragments' `onClick` use a JADX `$SwitchMap` (`AnonymousClass1/2`). To add a new
toggle: append the enum constant at the END, then handle it in a clean `switch(clicked)`
or `if` that `return`s BEFORE the generated switch (see AdditionalFragment,
DeletedAndEditedMessagesFragment). CustomizationFragment already uses a clean
`switch(clicked)`.

## Files
`settings/newui/{SettingsFragmentNew,DeletedAndEditedMessagesFragment,CustomizationFragment,
AdditionalFragment}.java`, `settings/Settings.java`, `localization/Localization.java`.
