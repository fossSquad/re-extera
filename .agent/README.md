# .agent — re:extera hook & patch reference

This folder documents every hook/patch group added to the re:extera DEX plugin, one
file per area, in full detail. [AGENTS.md](../AGENTS.md) is the entry point; start there,
then open the file below for the area you're touching.

## Index

| File | Covers |
|---|---|
| [architecture.md](architecture.md) | How hooks are registered/unloaded, the settings API, the fragment UI system, `Main` lifecycle + the **double-injection guard** |
| [deleted-messages.md](deleted-messages.md) | Save/show deleted messages: source interception, `messagesDeleted` swallow, did-independent detection, the render mark |
| [view-once.md](view-once.md) | Keeping view-once photo **and** video (SecretMediaViewer) — single "Save view-once media" toggle |
| [message-menu.md](message-menu.md) | Long-press menu additions: Edit, Save to Saved Messages, Message Details (+ `MessageDetailsDialog`) |
| [quick-buttons.md](quick-buttons.md) | Telegraph-style per-message side buttons (edit pencil / cloud save) drawn on `ChatMessageCell` |
| [id-display.md](id-display.md) | Show message id in the time area + "ID" row in the profile/channel/group menu |
| [gap-features.md](gap-features.md) | Hide pinned, disable chat/profile swipe, save protected stories, hide TL error, hide promo sponsor |
| [custom-icons.md](custom-icons.md) | `PathIconDrawable` — rendering SVG-path icons from code (no resources in a DEX) |
| [settings-ui.md](settings-ui.md) | Settings screen tree (Ghost / Spy / Other), Deleted-Message sub-screen, master switch, gating |
| [storage-and-paths.md](storage-and-paths.md) | `ReExteraDb` (deleted_keys cache), attachments/logs under `Download/ReExtera/` |
| [loader.md](loader.md) | Python `loader/` (concatenated `loader.plugin`), ruff/pyright setup, build script |

## Conventions used across all areas
- Every hook is an `XC_MethodHook` gated on a `Settings.getX()` read at call time.
- Register in `hooks/HookInit.java → startIntercepting()` via `tryHook(...)`; use `Class.forName`
  in a try/catch for classes not in the compileOnly stub (inner classes, host-only classes).
- Decompiled classes at `draft/extragram/` (exteraGram) and `draft/telegraph/` (Telegraph) are the
  reference for method signatures; exteraGram class names are **un-obfuscated**, Telegraph's are not.
- Before porting a feature, check `com.exteragram.messenger.ExteraConfig` — the host has many
  toggles natively (don't duplicate). See gap-features.md.
- Build: `./gradlew buildDex` (JDK 21 ok) → `build/dex/classes.dex`.
