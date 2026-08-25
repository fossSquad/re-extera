# Gap features (not native in exteraGram, not previously in re:extera)

Small toggles ported from TeleVip. Each = one hook (before → `setResult`) + one
toggle in OTHER (AdditionalFragment) + localization. Verified against
`draft/extragram` (unobfuscated). First check `ExteraConfig` before adding anything —
the host already has Hide Phone, Number Rounding, Download Boost, Disable Stories,
Show ID/DC, ad block, etc. natively; don't duplicate.

| Feature | Toggle key | Hook (before → result) |
|---|---|---|
| Hide pinned messages | `hide_pinned_messages` | `ChatActivity.updatePinnedMessageView(boolean,int)` after → set private `pinnedMessageView` FrameLayout `GONE` (`hooks/chatactivity/HidePinnedMessages.java`) |
| Disable chat swipe-back | `disable_chat_swipe_back` | `ChatActivity.canBeginSlide()` → `false` (`hooks/chatactivity/CanBeginSlide.java`) |
| Disable profile swipe | `disable_profile_swipe` | `ProfileGalleryView.onInterceptTouchEvent(MotionEvent)` → `false`, via `Class.forName` (`hooks/profileactivity/DisableProfileSwipe.java`) |
| Save protected stories | `save_protected_stories` | `PeerStoriesView$StoryItemHolder.allowScreenshots()` → `true`, via `Class.forName` (`hooks/stories/AllowScreenshots.java`) |
| Hide TL error | `hide_tl_error` | `LaunchActivity.didReceivedNotification(int,int,Object[])` → swallow when `id == NotificationCenter.tlSchemeParseException` (`hooks/launchactivity/HideTlError.java`) |
| Hide promo/proxy sponsor | (folds into `disable_ads`) | `MessagesController.isPromoDialog(long,boolean)` → `false` when `getDisableAds()` (`hooks/messagescontroller/IsPromoDialog.java`) |

UI: rows + `onClick` cases in `settings/newui/AdditionalFragment.java` (its `onClick`
handles NEW enum ids in a clean `switch(clicked)` that `return`s before the decompiled
`$SwitchMap`; new constants appended at the END of the enum). Labels in
`Localization.java` (`HIDE_PINNED_MESSAGES`, `DISABLE_CHAT_SWIPE_BACK`,
`DISABLE_PROFILE_SWIPE`, `SAVE_PROTECTED_STORIES`, `HIDE_TL_ERROR`).

## Removed dead hooks (clean logs)
On TG 12.9.0 these methods don't exist — registrations removed from `HookInit`:
`MessagesStorage.updateDialogsWithDeletedMessages` (+boolean `_old` overload) and
legacy `DialogsActivity.addMainMenuConfiguredItem(s)` (current path is
`MainMenuHelper.addConfiguredItemOptions`, plural only).
