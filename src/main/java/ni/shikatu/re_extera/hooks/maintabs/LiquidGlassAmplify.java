package ni.shikatu.re_extera.hooks.maintabs;

import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.settings.Settings;

/**
 * Drives exteraGram's liquid-glass shader from the re:extera sliders so the effect is
 * tunable (it ships far too subtle by default).
 *
 * <p>{@code LiquidGlassEffect.update(FFFFFFFFFFFI)} feeds the AGSL shader. We rewrite three
 * args before the shader consumes them:
 * <ul>
 *   <li>{@code args[8]}  thickness (edge/bevel) — scaled up for a chunkier rim.</li>
 *   <li>{@code args[9]}  refract_intensity — set from the Strength slider (0..100 → 0..3).</li>
 *   <li>{@code args[10]} refract_index — nudged up with Strength.</li>
 *   <li>{@code args[11]} tint colour (ARGB) — its alpha is set from the Opacity slider so
 *       the glass is see-through enough to reveal the blur/refraction, but not invisible.</li>
 * </ul>
 * This shader is shared by every glass surface (bottom tab bar, the top pinned/translate
 * panel, the message field), so the sliders balance them all together.
 */
public class LiquidGlassAmplify extends XC_MethodHook {

    @Override
    public void beforeHookedMethod(MethodHookParam param) {
        if (!Settings.getLiquidGlassTabs()) {
            return;
        }
        Object[] a = param.args;
        if (a == null || a.length < 12) {
            return;
        }

        int opacity = clamp(Settings.getLiquidGlassOpacity(), 5, 100);   // % opaque
        int strength = clamp(Settings.getLiquidGlassIntensity(), 0, 100); // warp

        float s = strength / 100f;                 // 0..1
        float refract = s * 3.0f;                  // 0 .. 3.0
        float thicknessMul = 1.0f + s * 1.2f;      // 1.0 .. 2.2
        float index = 1.5f + s * 0.5f;             // 1.5 .. 2.0
        int alpha = Math.round(opacity / 100f * 255f); // 13 .. 255

        if (a[8] instanceof Float) {
            a[8] = ((Float) a[8]) * thicknessMul;
        }
        if (a[9] instanceof Float) {
            a[9] = refract;
        }
        if (a[10] instanceof Float) {
            a[10] = Math.max((Float) a[10], index);
        }
        if (a[11] instanceof Integer) {
            int c = (Integer) a[11];
            a[11] = (alpha << 24) | (c & 0x00FFFFFF);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
