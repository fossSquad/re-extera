# re:extera — Agent Guide

## What this is

Android plugin for exteraGram (Telegram fork) loaded at runtime via DEX injection. Two deliverable artifacts: `classes.dex` (the Java hooking plugin) and `loader.elyx` (the Python Elyx loader that downloads and loads the DEX).

## Build commands (exact)

```bash
# build .dex (output: build/dex/classes.dex)
./gradlew buildDex

# build plugin (output: build/plugin/loader.elyx)
./gradlew buildPlugin

# build both
./gradlew buildAll
```
*Note: `buildAll` automatically creates a `.venv` internally, installs `ElyxBuilder`, and runs `elyb build -nf` to package the Elyx plugin.*

**Requirements**: JDK 17, Android SDK (compileSdk 35, build-tools 36.0.0), Python 3.x, `python3-venv` package (for Linux).

**Output**: `build/dex/classes.dex` and `build/plugin/loader.elyx`.

## Project structure

```
re-extera/
├── build.gradle                  # Android library build script
├── libs/exteragram.jar           # compileOnly dependency (exteraGram SDK stubs)
├── loader/                       # Elyx-structured Python loader
│   ├── refmap.yml                # Elyx map pointing to main, strings, and metadata
│   ├── metainfo.yml              # Elyx metadata (__id__, __version__, etc.)
│   ├── requirements.txt          # Python requirements for ElyxBuilder
│   └── plugin/                   # Source files for Elyx builder
│       ├── locales/              # strings_en.yml, strings_ru.yml, etc.
│       └── src/
│           ├── main.py           # Thin Elyx entry point (inherits Mixins)
│           ├── core.py           # Core initialization and lifecycle
│           ├── dex/              # Modulazied DEX updating and downloading engine
│           ├── ui/               # Modularized Elyx settings menus and actions
│           └── utils/            # Proxies, localization wrappers, download listeners
└── src/main/java/ni/shikatu/re_extera/
    ├── Main.java                 # Entry point: initAndStart() → DB init & hooks
    ├── Defaults.java             # Constants for Ghost mode (typing, reading, etc.)
    ├── db/                       # Custom SQLite implementation (re_extera.db)
    │   ├── ReExteraDb.java       # Database helper and CRUD operations (HandlerThread)
    │   └── (Entities: DialogExclusion, ShadowbanEntry)
    ├── hooks/                    # ~50+ Xposed hooks across Telegram classes
    │   ├── HookInit.java         # Central hook registry via XposedBridge
    │   ├── chatactivity/         # Chat UI hooks (menus, message processing)
    │   ├── chatmessagecell/      # Message cell hooks (deleted message transparency)
    │   ├── connectionsmanager/   # Network hooks (Ghost mode sendRequestInternal interception)
    │   ├── dialogsactivity/      # Dialog list hooks
    │   ├── messagescontroller/   # Core logic hooks (filtering, shadowbans, update loop)
    │   ├── messagesstorage/      # SQLite hooks (intercepting markMessagesAsDeleted)
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

## Architecture

- **Entry point**: `ni.shikatu.re_extera.Main.initAndStart()` — called by the Python loader after DEX injection
- **Hooking**: Uses `de.robv.android.xposed.XposedBridge` to hook ~50+ exteraGram/Telegram methods at runtime
- **Settings**: `SharedPreferences("re_extera")` — boolean/int/float/string key-value store
- **Database**: Custom SQLite (`re_extera.db`, version 11) with 7 tables: `deleted_keys`, `message_edits`, `exception_users`, `regex_filters`, `shadowban_users`, `read_events`, `last_online_users`. All writes go through a dedicated HandlerThread.
- **Ghost mode**: Intercepts `ConnectionsManager.sendRequestInternal` to block typing/reading/online/stories requests. Request types defined in `Defaults.java`.
- **Versioning**: Auto-generated from git tags. Tag format `v<plugin_ver>-<tg_ver>` (e.g. `v2.8.3-12.9.0`). Dev builds use `{yyyyMMddHHmmss}-{commit}`.

## CI & releasing

- GitHub Actions on push to master/main or `v*` tags
- Artifacts: `classes.dex` + `loader.plugin` per commit (dev artifacts on nightly.link)
- Release tags: `v<plugin_ver>-<tg_ver>` → release named `v<plugin_ver> for <tg_ver>`
- Loader channels: "Release" (stable GitHub releases) and "Dev" (latest CI artifact)

## Loader behavior

- The Python loader is now an **Elyx** archive (`loader.elyx`).
- On load: checks local path → cache → downloads fresh DEX
- DEX loading: tries `InMemoryDexClassLoader` first, falls back to `DexClassLoader` from file
- Updates: checks `loader.elyx` assets first on GitHub, falls back to legacy `.plugin` assets if `.elyx` is absent.
- Update checks rate-limited (60s cooldown)
- Plugin metadata: defined in `loader/metainfo.yml` (`__id__ = "re_extera_loader"`).

## Hooks troubleshooting

- `HookInit` wraps every hook registration in try/catch with per-hook name logging
- If `SettingsRegistry.initiateFragment` reflection fails, `ReflectionUtils.hookError()` shows a crash dialog and unloads
- Multiple hook overloads exist for compatibility across Telegram versions (e.g., `markMessagesAsDeleted` has 3 signature variants)
- After `initAndStart()`, further calls are no-ops (static `hooks` field guard)

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

- Tests are minimal: JUnit 4, AndroidX Test, Espresso 3.7.0
- `DummyTest.java` in `settings/newui/` is a placeholder
- Don't expect meaningful test coverage

## Gotchas

- `Main.VERSION` comes from `BuildConfig.RE_EXTERA_VERSION` (buildConfig enabled)
- `Main.VERSION_CODE` is a hardcoded integer (currently 12) — bump on significant releases
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

### Pushing builds (.elyx)

**Requirements**: device connected via ADB.

1. Build the project (`./gradlew buildAll`)

Now, there are 2 main ways how to push the Elyx plugin to device:

**Method 1: ADB Push + Manual Install**
1. Push file to phone: `adb push build/plugin/loader.elyx /sdcard/Download/loader.elyx`
2. Open exteraGram -> Settings -> Plugins -> "Install from file" (or + icon)
3. Select `loader.elyx` from the Downloads folder.

**Method 2: via exteragram-utils (extera CLI)**
If your `exteragram-utils` supports Elyx archives:
1. run `extera build/plugin/loader.elyx`

*(Note: The old python TCP script with `@write_plugin` only works for plain `.plugin` strings and will NOT work for `.elyx` zip archives. The new Live Reload Elyx protocol uses `elyx_changes` with base64 encoded JSON arrays, which requires specialized tools like `elyx_dev_client.py`).*

### Debug errors
- View logs via ADB: `adb logcat -d | grep -iE 're_extera|re:extera|chaquopy'`
- Fallback: ask user copy logs and send to you: plugin settings -> `Copy logs`/`Скопировать логи`/`Скопіювати логи` (button will copy logs to phone's clipboard)