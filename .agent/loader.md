# Python loader (`loader/`)

The loader ships as a single `loader.plugin` file, assembled by `loader/build.py`
from the fragments in `loader/__init__.py:BUILD_ORDER`, concatenated in order. The
fragments share ONE namespace (all imports live in `loader/imports.py`); a fragment
may only use names defined by earlier ones. `metadata.py` sets the plugin manifest
dunders: `__id__`, `__name__`, `__version__`, `__min_version__`, a feature-listing
`__description__`, and a multi-line `__author__` (fork lineage: Original author /
First fork maintainer / Current fork maintainer) — keep it in sync with the in-app
Credits card (`Localization.THANKS`, see settings-ui.md) and the README Credits section. Editing metadata only takes effect after
rebuilding + reinstalling `loader.plugin` (not a bare `classes.dex` push).

## Version = single source of truth
`metadata.py __version__` is the ONE place the version lives. The **DEX reads it too**:
`build.gradle getLoaderVersion(project)` regex-parses `__version__` out of
`loader/metadata.py`, and `getVersionName` returns it whenever there is no release git tag,
so `Main.VERSION` (the in-app "Version:" line) always equals the loader's version. Bump the
version by editing `metadata.py __version__` only — never hardcode it in build.gradle. A
release git tag (`v<ver>-<tgver>`) still overrides for CI naming, so keep the tag's base in
sync with `metadata.py`.

## Build
```bash
python3 loader/build.py            # assemble + syntax check + ruff lint
python3 loader/build.py --no-lint  # skip lint
```
Output: `build/plugin/loader.plugin`. The script prints with `[+]/[-]/[*]` markers and
`print("-" * 80)` separators, and runs `ruff check` on the assembled artifact.

## Lint / IDE config
- `ruff.toml` — lints the ASSEMBLED `*.plugin` (via `extend-include`) plus
  `loader/build.py`, and EXCLUDES the raw fragments (they aren't valid standalone
  modules). `isort` `lines-after-imports = 2` to match the 2-blank-line fragment join.
- `pyrightconfig.json` — disables `reportMissingImports` / `reportUndefinedVariable` /
  optional-access for the `loader/` fragments (cross-fragment names + exteraGram
  runtime modules can't be resolved per-file). Real undefined-name bugs are still
  caught by ruff on the assembled artifact.
- `loader/utils.py` `UIRunnable(dynamic_proxy(Runnable))` carries `# type: ignore`
  (Pyrefly can't model the Chaquopy dynamic base).

## Runtime (`dex.py` / `plugin.py`)
Loads the DEX from local path → cache → GitHub download; tries `InMemoryDexClassLoader`
then `DexClassLoader`. Calls `Main.initAndStart()`. **Two full loads under different
classloaders would double-register hooks** — the Java side guards against this (see
architecture.md). Update checks are rate-limited (60s). Channels: Release / Dev.
`_on_export_logs` writes to `Download/ReExtera/logs/`.

## CI
`.github/workflows/build.yml` runs `ruff check .` (Python lint) before the Gradle build.
