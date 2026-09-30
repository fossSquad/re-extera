# re:extera — Agent Guide

## What this is

Android plugin for exteraGram and Inugram (Telegram forks) loaded at runtime via DEX injection. Three deliverable artifacts:
- `classes.dex` (the core plugin)
- `loader.plugin` (the Python loader for exteraGram)
- `re_extera.inu.js` (the TypeScript loader for Inugram)

## Build commands (exact)

```bash
# Build the DEX plugin (assembleRelease AAR → d8 → classes.dex)
./gradlew buildDex

# Build all loader plugins (both Python and TypeScript)
./gradlew buildPlugin

# Or build individually:
./gradlew buildPlugin -Ptarget=python      # or python3 loader/python/build.py
./gradlew buildPlugin -Ptarget=typescript  # or cd loader/typescript && pnpm run build
```

**Requirements**: JDK 17, Android SDK (compileSdk 35, build-tools 36.0.0), Python 3.x, Node.js + pnpm.

**Output**: `build/dex/classes.dex`, `build/plugin/loader.plugin`, and `build/plugin/re_extera.inu.js`.

## Project structure

```
re-extera/
├── build.gradle                  # Android library build script
├── libs/exteragram.jar           # compileOnly dependency (exteraGram SDK stubs)
├── loader/
│   ├── python/                   # Python loader for exteraGram
│   │   ├── build.py              # Concatenation and syntax-checking script
│   │   ├── config.py             # Stores cached versions and rate-limiting data
│   │   ├── dex.py                # GitHub releases fetching and DEX loading engine
│   │   ├── plugin.py             # Main exteraGram BasePlugin implementation & UI dialogs
│   │   ├── metadata.py           # Plugin metadata (__version__, __id__, __min_version__)
│   │   └── (utils.py, constants.py, imports.py)
│   └── typescript/               # TypeScript loader for Inugram
│       ├── package.json          # pnpm workspace config
│       ├── inu.config.ts         # Inugram plugin config & manifest
│       ├── build.mjs             # Inugram CLI / esbuild bundler script
│       └── src/
│           ├── index.ts          # Entry point
│           ├── constants.ts      # URLs, cache paths, tags
│           ├── config.ts         # Persistent config manager
│           ├── dex.ts            # DEX fetcher & JVM loader
│           ├── i18n.ts           # Loader translations (uk, ru, en)
│           └── ui.ts             # Settings page registration
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

- The Python loader (`loader.plugin`) runs inside exteraGram's plugin engine
- On load: checks local path → cache → downloads fresh DEX
- DEX loading: tries `InMemoryDexClassLoader` first, falls back to `DexClassLoader` from file
- Update checks rate-limited (60s cooldown)
- Min exteraGram version: `12.8.1` (from `loader/metadata.py`)
- Plugin metadata: `__id__ = "re_extera_loader"`, `__version__ = "2.8.3"`

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
- Fallback: ask user copy logs and send to you: plugin settings -> `Copy logs`/`Скопировать логи`/`Скопіювати логи` (button will copy logs to phone's clipboard)