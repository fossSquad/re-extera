# Settings UI structure

Root: `settings/newui/SettingsFragmentNew.java` — the "re:extera" screen with the
sticker, the title + `Version: {Main.VERSION}` line, a **Credits card**, and the category
buttons. Fragments extend `BasePreferencesActivityExtended`; new top-level fragments must be
added to `Main.fragments`.

## Screen tree (current)
```
re:extera   (sticker · re:extera · Version: x.y.z · [Credits card])
├─ Spy          → DeletedAndEditedMessagesFragment  (icon: spy.svg via PathIconDrawable)
│   ├─ Save attachments (+ max folder size slider, Download/ReExtera/ReExteraAttachments)
│   ├─ Save read date / Save last online
│   ├─ Save view-once media           (view-once toggle, independent)
│   └─ Deleted Message  → CustomizationFragment
│        ├─ Save deleted messages     (MASTER switch — off hides & disables the rest)
│        ├─ Save in bot chats / Save message history / Save manually deleted / Use collapsed quote
│        ├─ preview cell
│        └─ Custom mark / color / Disable colored replies / Transparent + alpha slider
├─ General      → AdditionalFragment   (icon: general.svg via PathIconDrawable)
│    ├─ Local Premium / Disable ads
│    ├─ Work in background / Ignore FLAG_SECURE / re:forward (+ about)
│    ├─ Hide pinned / Disable chat swipe / Disable profile swipe / Save protected stories / Hide TL error
│    ├─ Show message ID / Show ID in profile menu / Message quick buttons
│    ├─ Liquid glass tab bar (+ Opacity & Strength sliders, shown when on)  → liquid-glass.md
│    ├─ Fix Translate Button Not Showing                                    → translate-button.md
│    └─ Filters / Shadowban / Clear-Export-Import DB / Unload
└─ Ghost mode   → GhostFragment            (icon: ghost.svg via PathIconDrawable)
```
Button order is **Spy → General → Ghost mode** (set by the `fillItems` add-order in
`SettingsFragmentNew`, not the enum). "Other" was renamed **General** — button label and
`AdditionalFragment.getTitle()` both use `Localization.GENERAL`; the icon is `general.svg`
(three rounded bars) rendered via `PathIconDrawable`. "Customization" was removed as a
top-level button; it became **Deleted Message** inside Spy. Its title = `Localization.DELETED_MESSAGE`.

## Credits card (`SettingsFragmentNew.createCreditsCard()`)
A dedicated rounded card (`GradientDrawable`, `key_windowBackgroundWhite`, dp16 radius) holding a
centered **"Credits"** header (`Localization.CREDITS`) with a neon `GlowUnderline` (a `View` that
draws a rounded bar with a `BlurMaskFilter` glow — needs `LAYER_TYPE_SOFTWARE`), then the linked
credit lines (`Localization.THANKS`, multi-line: Maintained by / Original author / FOSS Recovery /
Plugin Channel). The credit handles mirror `loader/metadata.py __author__`.

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

## Sliders & conditional rows (General)
Sliders use `UItem.asSlideView(id, String[] labels, selectedIndex, Utilities.Callback<Integer>)`
— discrete, so #labels = #steps; the callback writes the setting (no `onClick` case needed). The
Liquid-glass Opacity/Strength sliders live in `AdditionalFragment` and are added **only when**
`Settings.getLiquidGlassTabs()` is true, so its check-row `onClick` uses
`refreshCheckBox(item, position, value, true)` (full reload) to show/hide them — same master-switch
pattern as the Deleted-Message screen. Keep labels few (6) so they don't overlap on a phone track.

## Files
`settings/newui/{SettingsFragmentNew,DeletedAndEditedMessagesFragment,CustomizationFragment,
AdditionalFragment}.java`, `settings/Settings.java`, `localization/Localization.java`.
