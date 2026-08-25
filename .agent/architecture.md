# Architecture — hook system, settings, UI, lifecycle

## Entry & lifecycle (`Main.java`)
- Python loader calls `Main.initAndStart()` after DEX injection.
- `initAndStart()` guards against double init:
  1. `if (hooks != null) return;` — per-classloader static guard.
  2. **Process-wide guard** (added this session): a JVM system property
     `re_extera_hooked`. If the DEX is loaded under a *second* classloader
     (e.g. in-memory load + a later "Install from file"), each copy has its own
     static `hooks`, so the per-classloader check can't see the other → every
     hook registers twice (doubled menu items, doubled quick buttons, etc.).
     `System.getProperty/setProperty` is shared across all classloaders in the
     process and resets on app restart, so it blocks the duplicate load.
     `onUnload()` calls `System.clearProperty(...)` so a real reload can re-hook.
- `start()` initializes: `ReExteraDb`, shadowban cache, the deleted-mark icon,
  localization (`Localization.updateStrings()`), then `hooks = new HookInit()` /
  `hooks.init()` / `initFragments()`.
- `VERSION` = `BuildConfig.RE_EXTERA_VERSION` (git tag, else `"1.9.0"` fallback in
  build.gradle). `VERSION_CODE` = hardcoded int (13).

## Registering a hook (`hooks/HookInit.java`)
> Runtime method hooking uses **Aliuhook** (`com.aliucord:Aliuhook`, a `compileOnly`
> dep), which exposes the `de.robv.android.xposed.*` API (`XC_MethodHook`,
> `XposedBridge.hookMethod`, `Unhook`) in-process. This is an exteraGram **plugin**,
> NOT an Xposed/LSPosed module — there is no Xposed framework at runtime.

- `startIntercepting()` is the single registry. Add:
  ```java
  tryHook("Class.method", Clazz.class, "method", new MyHook(), ArgType.class, Integer.TYPE);
  ```
  `tryHook` resolves `clazz.getDeclaredMethod(name, paramTypes)` and calls
  `XposedBridge.hookMethod`, collecting the `Unhook` for `onUnload()`. A missing
  signature is caught & logged (one broken hook doesn't kill the rest).
- For classes NOT in `libs/exteragram.jar` (inner classes, host-only), wrap in
  `try { Class<?> c = Class.forName("..."); tryHook(...); } catch (Throwable e) {...}`.
- Multiple `tryHook` lines with different signatures = version-drift resilience.
- `isActive` (static) gates UI-drawing hooks; set false in `onUnload()`.

## Hook class shape
```java
public class MyHook extends XC_MethodHook {
    public void beforeHookedMethod(MethodHookParam p) {
        if (!Settings.getMyFeature()) return;
        // p.args[i] read/replace; p.setResult(x) skip original; p.thisObject instance
    }
}
```
No `XposedHelpers` in the stub — use plain reflection or `utils/ReflectionUtils`.
Account: `AccountUtils.getCurrentAccount(param.thisObject)`. Log: `Main.log(...)`.

## Settings API (`settings/Settings.java`)
- One cached `SharedPreferences("re_extera")`. Typed private helpers
  `getBool/putBool`, `getInt/putInt`, `getLong/putLong`, `getString/putString`,
  `getFloat/putFloat`.
- Add a public `getX()/setX()` pair per toggle. For "only under ghost", AND with
  `getGhostModeEnabledGlobal()`. For "only under the deleted master", AND with
  `getSaveDeletedMessages()` (see settings-ui.md).

## Localization (`localization/Localization.java`)
- Declare `public static String X;`, assign in `updateStrings()` under the `ru`,
  `uk`, and English (else) branches. `updateStrings()` runs once from `Main.start()`.

## Settings UI (`settings/newui/*`)
See settings-ui.md. Fragments extend `BasePreferencesActivityExtended`; each has an
`enum Ids { ... }` (`getId()=ordinal()+1`), `fillItems(...)` building `UItem` rows,
and `onClick(...)` flipping the setting + `refreshCheckBox(...)`. New top-level
fragments must be added to `Main.fragments`.

> NOTE: most hook `.java` files are JADX-decompiled (AnonymousClass switch maps,
> `/* synthetic */` lambdas). Prefer adding new small classes over refactoring the
> decompiled control flow. For fragment `onClick` that uses a decompiled
> `$SwitchMap`, handle NEW enum ids in a clean `switch(...)`/`if` that `return`s
> BEFORE the generated switch, and append new enum constants at the END.
