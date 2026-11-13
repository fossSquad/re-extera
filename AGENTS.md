# re:extera — Agent Guide

## What this is

Android plugin for exteraGram and Inugram (Telegram forks) loaded at runtime via DEX injection. Three deliverable artifacts:
- `classes.dex` (the core plugin)
- `loader.plugin` (the Python loader for exteraGram)
- `loader.inu.js` (the TypeScript loader for Inugram)

### Reference documentation
- [exteraGram Python SDK Repository](https://github.com/exteraSquad/plugins-pysdk-builds)
- [exteraGram Plugins Documentation](https://plugins.exteragram.app/docs)
- [Inugram Repository](https://github.com/teidesu/inugram)
- [Inugram Plugins Documentation](https://github.com/teidesu/inugram/tree/main/sdk/docs)

## Build commands (exact)

**Requirements**: JDK 21, Android SDK (compileSdk 35, build-tools with d8, Android NDK), Python 3.x, Node.js + pnpm.

```bash
# Initialize Telegram submodule (required for buildDex):
git submodule update --init --recursive --depth=1

# (Optional) Install TypeScript loader dependencies (required once for Inugram before building loader):
pnpm --dir loader/typescript install

# Build the DEX plugin (assembleRelease AAR → d8 → classes.dex)
./gradlew buildDex

# Build all loader plugins (both Python and TypeScript)
./gradlew buildPlugin

# Or build individually:
./gradlew buildPlugin -Ptarget=python      # or python3 loader/python/build.py
./gradlew buildPlugin -Ptarget=typescript  # or pnpm --dir loader/typescript run build
```

**Output**: `build/dex/classes.dex`, `build/plugin/loader.plugin`, and `build/plugin/loader.inu.js`.

## Project structure

```
re-extera/
├── build.gradle                  # Android library build script
├── libs/
│   ├── Telegram/                 # Submodule: TMessagesProj source (required for buildDex)
│   └── exteragram.jar           # compileOnly dependency (exteraGram SDK stubs)
├── loader/
│   ├── python/                   # Python loader for exteraGram
│   │   ├── build.py              # Concatenation and syntax-checking script
│   │   ├── config.py             # Stores cached versions and rate-limiting data
│   │   ├── constants.py          # Download URLs, cache paths, tags
│   │   ├── dex.py                # GitHub releases fetching and DEX loading engine
│   │   ├── imports.py            # Dynamic import fallbacks
│   │   ├── metadata.py           # Plugin metadata (__version__, __id__, __min_version__)
│   │   ├── plugin.py             # Main exteraGram BasePlugin implementation & UI dialogs
│   │   └── utils.py              # Toast notifications and client helper functions
│   └── typescript/               # TypeScript loader for Inugram (built with @inugram/cli)
│       ├── inu.config.ts         # Inugram plugin config & manifest
│       ├── package.json          # pnpm workspace config
│       ├── pnpm-workspace.yaml   # Workspace packages definition
│       ├── tsconfig.json         # TypeScript compiler configuration
│       └── src/
│           ├── bulletin.ts       # In-app notifications and alerts
│           ├── config.ts         # Persistent config manager
│           ├── constants.ts      # URLs, cache paths, tags
│           ├── dex.ts            # DEX fetcher & JVM loader via QuickJS
│           ├── i18n.ts           # Loader translations (uk, ru, en)
│           ├── index.ts          # Entry point for the TypeScript loader
│           └── ui.ts             # Settings page registration
└── src/main/java/ni/shikatu/re_extera/
    ├── Main.java                 # Entry point: initAndStart() → DB init & hooks
    ├── Defaults.java             # Constants for Ghost mode (typing, reading, etc.)
    ├── db/                       # Custom SQLite implementation (re_extera.db)
    │   ├── DialogExclusion.java  # Entity: chat exclusion from ghost/read rules
    │   ├── ReExteraDb.java       # Database helper and CRUD operations
    │   └── ShadowbanEntry.java   # Entity: shadowbanned user/dialog records
    ├── hooks/                    # ~50+ Xposed hooks across Telegram classes
    │   ├── HookInit.java         # Central hook registry via Aliuhook / XposedBridge
    │   ├── chatactivity/         # Chat UI hooks (menus, message processing)
    │   ├── chatmessagecell/      # Message cell hooks (deleted message transparency)
    │   ├── connectionsmanager/   # Network hooks (Ghost mode sendRequest interception)
    │   ├── dialogsactivity/      # Dialog list hooks
    │   ├── messagescontroller/   # Core logic hooks (filtering, shadowbans, update loop)
    │   └── messagesstorage/      # SQLite hooks (intercepting markMessagesAsDeleted)
    ├── localization/
    │   └── Localization.java     # Translations for DEX settings (ru, uk, en)
    ├── settings/
    │   ├── Settings.java         # SharedPreferences abstraction ("re_extera")
    │   └── newui/                # Settings screen fragments (GhostFragment, etc.)
    ├── ui/                       # Additional UI dialogs and filter fragments
    │   ├── DeletedMessagesInChatFragment.java
    │   ├── ExclusionsFragment.java
    │   ├── RegexFiltersFragment.java
    │   └── ShadowbanDialog.java
    └── utils/                    # Utility classes
        ├── ForkCompat.java       # Compatibility layer between exteraGram and Inugram
        ├── GhostMenuHelper.java  # Helper for long-press ghost mode menus
        ├── MessageForwarder.java # Logic for redirecting deleted/edited messages
        ├── ReflectionUtils.java  # Reflection helpers for accessing obfuscated fields
        └── TextUtils.java        # String and regex helpers
```

## Architecture

- **Core concept**: `re-extera` is an Android library compiled into an AAR, unpacked, and converted to a standalone `classes.dex` using Android SDK's `d8` tool.
- **Runtime injection**: The loader (`loader.plugin` or `loader.inu.js`) is installed into the client app (exteraGram or Inugram). On client startup, the loader downloads/loads `classes.dex` into memory and invokes `ni.shikatu.re_extera.Main.initAndStart()`.
- **Hooks**: Uses Aliuhook (`com.aliucord:Aliuhook`) for runtime method hooking into Telegram's Java classes.
- **Database**: Custom SQLite (`re_extera.db`, version 14) with 7 tables: `deleted_keys`, `message_edits`, `exception_users`, `regex_filters`, `shadowban_users`, `read_events`, `last_online_users`. All writes go through a dedicated HandlerThread.
- **Settings**: SharedPreferences file `"re_extera"` via `Settings.java`.
- **Ghost mode**: Intercepts `ConnectionsManager.sendRequestInternal` to block typing/reading/online/stories requests. Request types defined in `Defaults.java`.
- **Dependencies**:
  - `libs/Telegram`: git submodule providing `org.telegram:TMessagesProj` source, included via Gradle composite build (`includeBuild 'libs/Telegram'`). Required to compile against Telegram internals.
  - `libs/exteragram.jar`: `compileOnly` dependency containing exteraGram SDK stubs.
  - `Aliuhook`: runtime hook engine.
- **Versioning**:
  - Tagged builds: tag name stripped of `v` prefix (e.g. `2.9.2-13.2.1` or `2.9.2`).
  - Dev builds: `${yyyyMMddHHmmss}-${commit}` (e.g. `20261113125234-de8e75a`), derived from latest git commit timestamp (`git log -1 --format=%cd --date=format:%Y%m%d%H%M%S`) for reproducible builds, falling back to current date.
  - Tag format `v<plugin_ver>-<tg_ver>` (e.g. `v2.9.2-13.2.1`).

## CI & releasing

- GitHub Actions on push to master/main or `v*` tags
- Artifacts: `classes.dex`, `loader.plugin`, and `loader.inu.js` per commit (dev artifacts on nightly.link)
- Release tags: `v<plugin_ver>-<tg_ver>` → release named `v<plugin_ver> for <tg_ver>` (e.g. `v2.9.2 for 13.2.1`)
- Loader channels: "Release" (stable GitHub releases) and "Dev" (latest CI artifact)

## Loader behavior

- **Python loader (`loader.plugin`)**: Runs inside exteraGram's plugin engine (`__id__ = "re_extera_loader"`, `__version__ = "2.9.2"`, min exteraGram version: `12.8.1` from `loader/python/metadata.py`). On load: checks local path → cache → downloads fresh DEX. Tries `InMemoryDexClassLoader` first, falls back to `DexClassLoader` from file.
- **TypeScript loader (`loader.inu.js`)**: Runs inside Inugram's plugin engine (`id: "re_extera_loader"`, `version: "2.9.2"` from `loader/typescript/inu.config.ts`). Loads DEX via QuickJS JVM bridge.
- **Update logic (both loaders)**:
  1. Check configured update channel (Release vs Dev)
  2. Query GitHub API for latest release (`/releases/tags/{tag}`) or CI artifact (nightly.link)
  3. Compare remote version against local cache
  4. Download new `classes.dex` if update available
  5. Load `classes.dex` via DexClassLoader and call `ni.shikatu.re_extera.Main.initAndStart()`

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
chore(release): bump version to 2.9.2
```

Types observed: `fix`, `feat`, `refactor`, `ci`, `chore`, `build`, `docs`. Scoped by affected module when relevant (`online-status`, `loader`, `settings`, `i18n`, `hooks`, `ghost-mode`, `messages`). No trailing dot.

## Testing

- Tests are minimal: JUnit 4, AndroidX Test, Espresso 3.7.0
- `DummyTest.java` in `settings/newui/` is a placeholder
- Don't expect meaningful test coverage

## Critical invariants & Gotchas

- Never change package name `ni.shikatu.re_extera` or class name `Main` — loaders look for this exact entry point
- `Main.initAndStart()` signature must not change
- Keep `classes.dex` self-contained — no external runtime dependencies beyond what Telegram/exteraGram already provides
- `Main.VERSION` comes from `BuildConfig.RE_EXTERA_VERSION` (buildConfig enabled)
- `Main.VERSION_CODE` is a hardcoded integer (currently 13) — bump on significant releases
- `anyAccountIsPremium()` in HookInit disables Local Premium if any account has real premium
- ProGuard keeps `ni.shikatu.re_extera.Main` entirely (`-keep class` in `proguard-rules.pro`)
- Local DEX path (for sideloading): `/storage/emulated/0/Android/media/com.exteragram.messenger/classes.dex`

## Debugging

To debug the plugin locally, you must have an Android device connected via ADB with the exteraGram client (`com.exteragram.messenger`) installed.

### Pushing builds (DEX)

**Requirements**: device connected via ADB, built .dex file

1. Build the DEX file using instructions in `Build commands` section

2. Push the compiled DEX directly to the device's exteraGram media folder:
   ```bash
   adb push build/dex/classes.dex /storage/emulated/0/Android/media/com.exteragram.messenger/classes.dex
   ```
3. Go to plugin settings -> tap `Install from file`/`Установить из файла`/`Встановити з файлу` and restart the app _(do not use force stop via adb like `am force-stop`, as this will not apply changes)_

### Pushing builds (.plugin)

**Requirements**: device connected via ADB, Python 3.x, `Developer mode` enabled on phone in "Plugins" section _(`exteraGram Settings` -> `Plugins` -> Settings icon top right -> `Developer mode`)_

1. Build the .plugin using instructions in `Build commands` section

There are two ways to push the plugin to the device:

#### Method 1: exteragram-utils
1. Ensure `exteragram-utils` is installed (`pip install exteragram-utils`).
2. Run `extera loader.plugin`.
3. `extera` handles port forwarding, pushing, and reloading automatically.

#### Method 2: Python script (without exteragram-utils)
1. Forward port: `adb forward tcp:42690 tcp:42690`.
2. Run the push script:
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
- Fallback: ask the user to copy logs and send them: plugin settings -> `Copy logs`/`Скопировать логи`/`Скопіювати логи` (copies logs to clipboard).
