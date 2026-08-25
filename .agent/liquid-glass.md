# Liquid glass tab bar

iOS-26 "liquid glass" on exteraGram's bottom tab bar (Chats / Contacts / Settings /
Profile) and the other glass surfaces (top pinned/translate panel, message field).

## Key insight — exteraGram already ships the glass
The host already has the whole pipeline: `org.telegram.ui.Components.blur3.LiquidGlassEffect`
(an AGSL `RuntimeShader`), `GlassTabView`, and the `blur3` backdrop-blur sources. The bar
renders as a **solid pill** only because the effect is gated off, not because it's missing.

The tab render node (`MainTabsActivity$1.renderNodeUpdateDisplayList`, decompiled) only
captures + blurs the content behind the bar when `SharedConfig.chatBlurEnabled()` is true —
otherwise it fills a flat `windowBackgroundWhite`. And
`chatBlurEnabled() == canBlurChat() && LiteMode.isEnabled(FLAG_CHAT_BLUR)`. The refraction
shine on top is separately gated by `LiteMode.isEnabled(FLAG_LIQUID_GLASS)`
(`MainTabsActivity.createView → setLiquidGlassEffectAllowed(...)`).

So two `LiteMode` flags — both read through `LiteMode.isEnabled(int)` — decide the look.

## Hooks (`hooks/maintabs/`)
- **`ForceLiquidGlass`** → `LiteMode.isEnabled(int)`. When `Settings.getLiquidGlassTabs()`,
  forces the result to `true` **only** for `FLAG_CHAT_BLUR` (0x100, the missing backdrop-blur
  capture) and `FLAG_LIQUID_GLASS` (0x40000, the AGSL shine). Every other flag passes through
  untouched. This mirrors exteraGram's own "Force Blur" option (which toggles chat blur), so it
  also enables Telegram's chat-background blur — the intended cost of the glass aesthetic.
- **`LiquidGlassAmplify`** → `LiquidGlassEffect.update(FFFFFFFFFFFI)`. The default effect is far
  too subtle, so it rewrites the shader args every frame from the sliders:
  - `args[8]` thickness (edge/bevel) — scaled up.
  - `args[9]` refract_intensity — set from the **Strength** slider (0..100 → 0..3.0).
  - `args[10]` refract_index — nudged up with Strength.
  - `args[11]` tint colour (ARGB) — its alpha set from the **Opacity** slider so the glass is
    see-through enough to reveal the blur (the tab tint ships ~85% opaque, which hid everything).
  The shader is shared by all glass surfaces, so the sliders balance them together.

Both are registered in `HookInit.startIntercepting()`; `LiquidGlassEffect`, `LiteMode` are in
the compileOnly stub so no `Class.forName` is needed.

## Hard OS gates a hook cannot bypass
- Backdrop blur needs a RenderNode → `SDK_INT >= 31` (Android 12).
- `LiquidGlassEffect` AGSL `RuntimeShader` → `SDK_INT >= 33` (Android 13).
Below those the source falls back to `BlurredBackgroundSourceColor` (solid) regardless of flags.

## Settings (`settings/Settings.java`)
- `getLiquidGlassTabs()` bool, **default true** — master toggle (gates both hooks).
- `getLiquidGlassOpacity()` int 0..100, **default 40** — tint alpha (higher = more opaque).
- `getLiquidGlassIntensity()` int 0..100, **default 20** — refraction strength.

## UI (`settings/newui/AdditionalFragment.java` = "General")
- `LIQUID_GLASS_TABS_ID` check row. Toggling it calls `refreshCheckBox(..., true)` (full reload)
  so the two sliders appear/disappear.
- When on: two `UItem.asSlideView` rows — Opacity and Strength — using 6 evenly-spaced labels
  (`0% 20% 40% 60% 80% 100%`, helpers `opacityToIndex`/`indexToPercent`). Each writes its setting
  in the `Utilities.Callback<Integer>`. Localized: `LIQUID_GLASS_OPACITY/INTENSITY/SLIDERS_ABOUT`.

## Notes / gotchas
- Effect is built at chat/activity `createView`, so a toggle change needs an app restart to fully
  re-render (a bulletin used to prompt this; the sliders themselves apply live per-frame).
- The bar can still look solid on a capable device if `canBlurChat()` is false (RAM/perf gate).
- Decompiled refs: `MainTabsActivity`, `MainTabsActivity$1`, `blur3/LiquidGlassEffect`,
  `blur3/drawable/color/impl/BlurredBackgroundProviderImpl.mainTabs`, `SharedConfig.chatBlurEnabled`,
  `LiteMode.FLAG_CHAT_BLUR` / `FLAG_LIQUID_GLASS`.
