# Custom icons — PathIconDrawable

re:extera ships as a DEX (`assembleRelease` AAR → d8 → `classes.dex`); the AAR's
resources are dropped, so **you cannot add res/drawable vectors**. All icons must be
either host drawables (`org.telegram.messenger.R.drawable.*`, referenced by id) or
rendered from code.

`utils/PathIconDrawable.java` renders SVG path data at runtime using
`androidx.core.graphics.PathParser` (present in the stub jar):

```java
// fill icon
new PathIconDrawable(viewportW, viewportH, evenOdd, intrinsicDp, "M...Z", ...).withTint(color);
// stroke (outline) icon — strokeWidth in viewport units, round cap/join
new PathIconDrawable(24f, 24f, false, 20, 2f, "M...Z");
```
- Parses each path via `PathParser.createPathFromPathData`, `addPath` into one `Path`
  with `FillType` = EVEN_ODD or WINDING (match the SVG's `fill-rule`).
- `draw()` scales the path to the current bounds (min-fit) and centers it.
- `withTint(color)` sets the paint color; `setColorFilter` also works (used by the
  quick-button cloud, tinted with `key_chat_serviceText`).
- `getIntrinsicWidth/Height` = `dp(intrinsicDp)` (used by `UItem.asButton`).

## Where used
- **Ghost mode + Spy + General** settings buttons (`settings/newui/SettingsFragmentNew.java`):
  `GHOST_ICON_PATH` (viewBox 16, evenOdd — the eyes are holes),
  `SPY_ICON_PATH_1/2` (viewBox 507.965, winding), and `GENERAL_ICON_PATH_1/2/3`
  (viewBox 24, three rounded bars — the user-supplied `general.svg`), all tinted
  `key_windowBackgroundWhiteBlueIcon`.
- **Quick save button** (`hooks/chatmessagecell/QuickButtons.java`):
  `CLOUD_ICON_PATH` (viewBox 24, stroke width 2) — the user-supplied `cloud.svg`.

Note: a small hand-drawn `GlowUnderline` `View` (not a `PathIconDrawable`) draws the neon bar
under the Credits header — see settings-ui.md.

## Adding a new user icon
1. User supplies an SVG (fill or stroke).
2. Read its `viewBox` (→ viewport W/H), `fill-rule` (→ evenOdd), and for outline
   icons its `stroke-width` (→ the float overload).
3. Copy the `d="..."` path(s) into a Java string constant (chunk with `+` for
   readability) and construct a `PathIconDrawable`.

The user-supplied SVGs came from svgrepo; do NOT copy another app's brand assets
(e.g. Telegraph's `button_cloud.png`).
