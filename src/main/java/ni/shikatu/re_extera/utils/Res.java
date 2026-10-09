package ni.shikatu.re_extera.utils;

import android.content.res.Resources;

import org.telegram.messenger.ApplicationLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource id resolution that works on both exteraGram and Inugram.
 *
 * Resource ids are compiled into the DEX from exteragram.jar, and exteraGram's
 * resource table differs from Inugram's, so baked ids point to the wrong entries
 * (or to nothing) on other forks. On exteraGram the baked id is returned as-is;
 * on other forks the id is looked up by name in the running app's resource table.
 */
public final class Res {

    private static final Map<String, Integer> cache = new HashMap<>();

    private Res() {
    }

    public static int get(String type, String name, int fallback) {
        if (ForkCompat.isExteraGram()) {
            return fallback;
        }
        String key = type + '/' + name;
        synchronized (cache) {
            Integer cached = cache.get(key);
            if (cached != null) {
                return cached != 0 ? cached : fallback;
            }
        }
        int id = 0;
        try {
            Resources resources = ApplicationLoader.applicationContext.getResources();
            id = resources.getIdentifier(name, type, ApplicationLoader.applicationContext.getPackageName());
        } catch (Throwable ignored) {
        }
        synchronized (cache) {
            cache.put(key, id);
        }
        return id != 0 ? id : fallback;
    }

    public static int drawable(String name, int fallback) {
        return get("drawable", name, fallback);
    }

    public static int string(String name, int fallback) {
        return get("string", name, fallback);
    }

    public static int id(String name, int fallback) {
        return get("id", name, fallback);
    }

    public static int raw(String name, int fallback) {
        return get("raw", name, fallback);
    }

    public static int plurals(String name, int fallback) {
        return get("plurals", name, fallback);
    }

    public static int array(String name, int fallback) {
        return get("array", name, fallback);
    }

    public static int color(String name, int fallback) {
        return get("color", name, fallback);
    }

    public static int integer(String name, int fallback) {
        return get("integer", name, fallback);
    }

    public static int bool(String name, int fallback) {
        return get("bool", name, fallback);
    }
}
