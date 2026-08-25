# Fix translate button not showing

Restores the in-chat "Translate to English" bar (and the 3-dot **Translate** item) that
exteraGram hides for non-Premium users, even with "Show translate button" enabled. Telegraph
(vanilla-ish fork) shows it for everyone; exteraGram kept Telegram's Premium gate.

## Root cause (traced on-device with logcat)
The bar is drawn in `ChatActivity.updateTopPanel(boolean)` and gated by:
`(isPremium() || currentChat.autotranslation) && isDialogTranslatable(id) && !isTranslateDialogHidden(id)`.
For a non-Premium DM (`currentChat == null`) the outer gate fails immediately.

Deeper, `TranslateController.isDialogTranslatable(id)` was returning **false** because
`translatableDialogs` was **empty**: the detection pipeline (`checkTranslation`) early-returns on
`isFeatureAvailable(id)`, and `isFeatureAvailable` keeps the Premium gate + the `translate_chat_button`
pref (defaulted false). So nothing ever gets marked translatable. (Confirmed: `isFeatureAvailable`
fired 0 times, `isDialogTranslatable(...) = false`.)

## Hooks (`hooks/translate/`, all gated on `Settings.getFixTranslateButton()`)
- **`ForceDialogTranslatable`** → `TranslateController.isDialogTranslatable(long)` = `true`. The
  decisive fix: bypasses the empty `translatableDialogs` set entirely. (Trade-off: the bar can then
  appear on same-language chats too — acceptable "always allow translate" behaviour.)
- **`TranslateFeatureAvailable`** → `TranslateController.isFeatureAvailable()` **and**
  `isFeatureAvailable(long)` = `true`. Clears the Premium + `translate_chat_button` gate for the
  3-dot menu item and any inner check.
- **`ForceChatTranslateEnabled`** → `TranslateController.isChatTranslateEnabled()` = `true`.
- **`UpdateTopPanelScope`** + **`TranslateScope`** → wraps `ChatActivity.updateTopPanel(boolean)`;
  `beforeHookedMethod` raises a thread-local scope, `afterHookedMethod` clears it. The existing
  `hooks/userconfig/isPremium` hook returns `true` while `TranslateScope.isActive()`, so the outer
  `getUserConfig().isPremium()` check inside `updateTopPanel` passes **only** for the duration of
  that method — no app-wide Premium spoof, no collision with the Local Premium feature.

All registered in `HookInit.startIntercepting()`; `TranslateController`, `ChatActivity`,
`UserConfig` are in the stub jar.

## Settings & UI
- `Settings.getFixTranslateButton()` bool, **default true**.
- Toggle in `AdditionalFragment` ("General"): `FIX_TRANSLATE_BUTTON_ID` check row. Localized
  `FIX_TRANSLATE_BUTTON` / `FIX_TRANSLATE_BUTTON_ABOUT`.

## Notes
- `ChatActivity.updateTopPanel` on the running 12.9.0 build may not always fire; the decisive lever
  is `ForceDialogTranslatable`, which is why the bar shows even if the scope hook is a no-op.
- Decompiled refs: `TranslateController.{isDialogTranslatable,isFeatureAvailable,isChatTranslateEnabled,
  checkTranslation}`, `ChatActivity.updateTopPanel`, pref `translate_chat_button`.
