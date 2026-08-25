# re:extera — Agent Guide

> **This file is the entry point.** For any hook/patch you're touching, open the
> matching detailed file under [`.agent/`](.agent/README.md) — one file per feature
> area, fully documented. Read [`.agent/architecture.md`](.agent/architecture.md)
> first (how hooks register, the settings API, the fragment UI, the double-injection
> guard), then the area file.

## What this is

Android plugin for exteraGram (Telegram fork) loaded at runtime via DEX injection. Two deliverable artifacts: `classes.dex` (the plugin) and `loader.plugin` (the Python loader that downloads/loads the DEX).

## Hook & patch reference — `.agent/`

| Area | File |
|---|---|
| Hook system, settings, UI, lifecycle, **double-injection guard** | [.agent/architecture.md](.agent/architecture.md) |
| Save/show deleted messages | [.agent/deleted-messages.md](.agent/deleted-messages.md) |
| View-once photo + video | [.agent/view-once.md](.agent/view-once.md) |
| Long-press menu (Edit / Save to Saved / Details) | [.agent/message-menu.md](.agent/message-menu.md) |
| Quick side buttons (edit / cloud-save) | [.agent/quick-buttons.md](.agent/quick-buttons.md) |
| Show message id / profile-menu id | [.agent/id-display.md](.agent/id-display.md) |
| Hide pinned, disable swipes, save stories, hide TL error, promo | [.agent/gap-features.md](.agent/gap-features.md) |
| Liquid glass tab bar (LiteMode flags + shader amplify + sliders) | [.agent/liquid-glass.md](.agent/liquid-glass.md) |
| Fix translate button not showing (non-Premium) | [.agent/translate-button.md](.agent/translate-button.md) |
| Custom SVG icons (`PathIconDrawable`) | [.agent/custom-icons.md](.agent/custom-icons.md) |
| Settings screen tree, credits card, master switch | [.agent/settings-ui.md](.agent/settings-ui.md) |
| DB + `Download/ReExtera/` paths | [.agent/storage-and-paths.md](.agent/storage-and-paths.md) |
| Python loader, ruff/pyright, build | [.agent/loader.md](.agent/loader.md) |

Before adding a Telegram-mod feature, check `com.exteragram.messenger.ExteraConfig` —
the host already has many toggles natively (Hide Phone, Number Rounding, Download
Boost, Disable Stories, Show ID/DC, ad block…). Don't duplicate them.

## Build commands (exact)

```bash
# Build the DEX plugin (assembleRelease AAR → d8 → classes.dex)
./gradlew buildDex

# Build the Python loader plugin (concatenates loader/*.py → loader.plugin)
python3 loader/build.py
```

**Requirements**: JDK 17 (JDK 21 also builds fine; source/target is Java 11), Android SDK (compileSdk 35, build-tools 36.0.0), Python 3.x. `python3 loader/build.py` also runs `ruff` on the assembled plugin (see [.agent/loader.md](.agent/loader.md)).

**Output**: `build/dex/classes.dex` and `build/plugin/loader.plugin`.

## Project structure

```
re-extera/
├── build.gradle                  # Android library build script
├── libs/exteragram.jar           # compileOnly dependency (exteraGram SDK stubs)
├── loader/                       # Python loader (concatenated by build.py)
│   ├── build.py                  # Concatenation and syntax-checking script
│   ├── config.py                 # Stores cached versions and rate-limiting data
│   ├── dex.py                    # GitHub releases fetching and DEX loading engine
│   ├── plugin.py                 # Main exteraGram BasePlugin implementation & UI dialogs
│   ├── metadata.py               # Plugin metadata (__version__, __id__, __min_version__)
│   └── (utils.py, constants.py, imports.py)
└── src/main/java/ni/shikatu/re_extera/
    ├── Main.java                 # Entry point: initAndStart() → DB init & hooks
    ├── Defaults.java             # Constants for Ghost mode (typing, reading, etc.)
    ├── db/                       # Custom SQLite implementation (re_extera.db)
    │   ├── ReExteraDb.java       # Database helper and CRUD operations (HandlerThread)
    │   └── (Entities: DialogExclusion, ShadowbanEntry)
    ├── hooks/                    # ~50+ runtime method hooks across Telegram classes
    │   ├── HookInit.java         # Central hook registry (Aliuhook)
    │   ├── chatactivity/         # Chat UI hooks (menus, message processing)
    │   ├── chatmessagecell/      # Message cell hooks (deleted message transparency)
    │   ├── connectionsmanager/   # Network hooks (Ghost mode sendRequestInternal interception)
    │   ├── dialogsactivity/      # Dialog list hooks
    │   ├── messagescontroller/   # Core logic hooks (filtering, shadowbans, update loop)
    │   ├── messagesstorage/      # SQLite hooks (intercepting markMessagesAsDeleted)
    │   ├── maintabs/             # Liquid glass tab bar (ForceLiquidGlass, LiquidGlassAmplify)
    │   ├── translate/            # Fix translate button (dialog-translatable/feature/premium-scope)
    │   └── (other hook subpackages: notificationmanager, profileactivity, sendmessageshelper, etc.)
    ├── localization/
    │   └── Localization.java     # Translations for DEX settings
    ├── settings/
    │   ├── Settings.java         # SharedPreferences abstraction ("re_extera")
    │   └── newui/                # Settings screen fragments (GhostFragment, CustomizationFragment, etc.)
    ├── ui/                       # Additional UI components
    │   ├── DeletedMessagesInChatFragment.java
    │   ├── RegexFiltersFragment.java
    │   └── (ShadowbanDialog, ExclusionsFragment)
    └── utils/                    # 14 utility classes
        ├── MessageForwarder.java # Logic for redirecting deleted/edited messages to Saved Messages
        ├── ReflectionUtils.java  # Reflection helpers for accessing obfuscated Telegram fields
        ├── GhostMenuHelper.java  # Helper for long-press ghost mode menus
        └── (AccountUtils, DrawableUtils, ExclusionUtils, ShadowbanCache, etc.)
```

## Core concepts (one-liners — details in `.agent/`)

- **Entry point**: `Main.initAndStart()`, called by the loader after DEX injection; has a
  process-wide guard against double-registration → [.agent/architecture.md](.agent/architecture.md).
- **Hooking**: this is an exteraGram plugin, **not an Xposed/LSPosed module** —
  runtime method hooking is done with **Aliuhook** (`com.aliucord:Aliuhook`), which
  provides the `de.robv.android.xposed.*` API (`XC_MethodHook`, `XposedBridge`)
  in-process. All registration goes through the single registry `hooks/HookInit.java`
  (`tryHook(...)`); each hook is an `XC_MethodHook` gated on a `Settings.getX()`. How
  to add one, resolve signatures, and handle decompiled fragments →
  [.agent/architecture.md](.agent/architecture.md).
- **Settings**: `SharedPreferences("re_extera")` via `settings/Settings.java`; the UI
  fragment tree and the "Save deleted messages" master switch →
  [.agent/settings-ui.md](.agent/settings-ui.md).
- **Database**: custom SQLite `re_extera.db` with an in-memory `deleted_keys` cache →
  [.agent/storage-and-paths.md](.agent/storage-and-paths.md).
- **Ghost mode**: intercepts `ConnectionsManager.sendRequestInternal` to drop
  typing/reading/online/stories requests (types in `Defaults.java`).
- **Versioning**: from git tag `v<plugin_ver>-<tg_ver>`; no tag → `Main.VERSION`
  falls back to `"1.9.0"` (build.gradle). `VERSION_CODE` = 13.

Everything a feature touches is documented per-area under [`.agent/`](.agent/README.md);
this file stays a lean map + the practical build/push/debug steps below.

## CI & releasing

- GitHub Actions on push to master/main or `v*` tags
- Artifacts: `classes.dex` + `loader.plugin` per commit (dev artifacts on nightly.link)
- Release tags: `v<plugin_ver>-<tg_ver>` → release named `v<plugin_ver> for <tg_ver>`
- Loader channels: "Release" (stable GitHub releases) and "Dev" (latest CI artifact)

## Loader (Python)

The `loader.plugin` runs inside exteraGram's plugin engine; on load it resolves the
DEX from local path → cache → GitHub download (`InMemoryDexClassLoader`, then
`DexClassLoader`), rate-limits update checks (60s), and calls `Main.initAndStart()`.
Min exteraGram `12.8.1`; `__id__ = "re_extera_loader"`, `__version__ = "2.8.5"`. Build,
concatenation model, ruff/pyright setup → [.agent/loader.md](.agent/loader.md).

## Hooks troubleshooting

`HookInit` wraps every registration in try/catch with per-hook logging, so a missing
signature logs and continues. Add multiple `tryHook` lines with different signatures
for version drift; use `Class.forName` for classes absent from the stub jar. Full
recipe and the decompiled-fragment gotchas → [.agent/architecture.md](.agent/architecture.md).

## Commit style

Conventional Commits with lowercase scope. Examples from history:

```
fix(online-status): debounce UI redraws to avoid DialogsActivity crashes
feat(settings): extract customization settings into new CustomizationFragment
feat(i18n): add ukrainian localization for both python loader and java hooks
refactor: modularize loader into distinct components and introduce build script
ci: update build workflow for modular plugin compilation
chore(loader): bump version to 2.8.2
```

Types observed: `fix`, `feat`, `refactor`, `ci`, `chore`, `build`, `docs`. Scoped by affected module when relevant (`online-status`, `loader`, `settings`, `i18n`, `hooks`, `ghost-mode`, `messages`). No trailing dot.

## Testing

There is no test suite — the placeholder `src/test` and `src/androidTest` (and their
JUnit/Espresso deps) were removed. Verify changes by building the DEX and testing on a
device (see Debugging below).

## Gotchas

- `Main.VERSION` comes from `BuildConfig.RE_EXTERA_VERSION` (buildConfig enabled)
- `Main.VERSION_CODE` is a hardcoded integer (currently 13) — bump on significant releases. `Main.VERSION` fallback is `"1.9.0"` when there's no git tag (build.gradle)
- `Main.initAndStart()` has a process-wide guard (`re_extera_hooked` system property) so the DEX loading under two classloaders can't double-register hooks — see [.agent/architecture.md](.agent/architecture.md)
- `anyAccountIsPremium()` in HookInit disables Local Premium if any account has real premium
- ProGuard keeps `ni.shikatu.re_extera.Main` entirely (`-keep class` in `proguard-rules.pro`)
- Local DEX path (for sideloading): `/storage/emulated/0/Android/media/com.exteragram.messenger/classes.dex`

## Debugging

To debug the plugin locally, you must have an Android device connected via ADB with the exteraGram client (`com.exteragram.messenger`) installed. 

### Pushing builds (DEX)

**Requirements**: device connected via ADB, builded .dex file

1. Build the DEX file using instructions in `Build commands` section

2. Push the compiled DEX directly to the device's exteraGram media folder:
   ```bash
   adb push build/dex/classes.dex /storage/emulated/0/Android/media/com.exteragram.messenger/classes.dex
   ```
3. User need go to plugin settings -> press `Install from file`/`Установить из файла`/`Встановити з файлу` and restart app _(don't use force stop via adb like am stop etc. this will not work)_

### Pushing builds (.plugin)

**Requirements**: device connected via ADB, Python 3.x, `Developer mode` enabled on phone in "Plugins" section _(`exteraGram Settings` -> `Plugins` -> Settings icon top right -> `Developer mode`)_

1. Build the .plugin using instructions in `Build commands` section

Now, there is 2 ways how to push plugin to device

**Method 1:** via exteragram-utils
1. Make sure pip package `exteragram-utils` is installed
2. run `extera loader.plugin`
3. `extera` made all work _(port forward, push plugin etc.)_

**Method 2:** via python script _(without `exteragram-utils`)_
1. forward port: `adb forward tcp:42690 tcp:42690`
2. run this script:
```bash
python3 -c "
import socket, json
with open('build/plugin/loader.plugin', 'r', encoding='utf-8') as f: content = f.read()

def send_cmd(cmd):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect(('127.0.0.1', 42690))
        s.sendall(json.dumps(cmd).encode('utf-8'))

send_cmd({'@': 'write_plugin', '#': 1, 'plugin_id': 're_extera_loader', 'content': content})
send_cmd({'@': 'reload_plugin', '#': 2, 'plugin_id': 're_extera_loader'})
print('Plugin pushed and reloaded successfully!')
"
```

### Debug errors
- View logs via ADB: `adb logcat -d | grep -iE 're_extera|re:extera|chaquopy'`
- Fallback: ask the user to export logs: plugin settings -> `Export logs`/`Экспортировать логи`/`Експортувати логи` (writes a file to `Download/ReExtera/logs/re_extera_logs_<timestamp>.txt`)