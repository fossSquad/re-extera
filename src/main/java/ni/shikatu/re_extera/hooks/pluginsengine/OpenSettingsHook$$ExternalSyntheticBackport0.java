package ni.shikatu.re_extera.hooks.pluginsengine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* JADX INFO: compiled from: D8$$SyntheticClass */
public final /* synthetic */ class OpenSettingsHook$$ExternalSyntheticBackport0 {
    public static /* synthetic */ Set m(Object[] objArr) {
        HashSet hashSet = new HashSet(objArr.length);
        for (Object obj : objArr) {
            obj.getClass();
            if (!hashSet.add(obj)) {
                throw new IllegalArgumentException("duplicate element: " + obj);
            }
        }
        return Collections.unmodifiableSet(hashSet);
    }
}
