# Python loader (`loader/`)

The loader ships as a single `loader.plugin` file, assembled by `loader/build.py`
from the fragments in `loader/__init__.py:BUILD_ORDER`, concatenated in order. The
fragments share ONE namespace (all imports live in `loader/imports.py`); a fragment
may only use names defined by earlier ones. `metadata.py` sets the plugin manifest
dunders (`__id__`, `__name__`, `__version__` = 2.8.5, `__author__` includes @SHAJON).

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
