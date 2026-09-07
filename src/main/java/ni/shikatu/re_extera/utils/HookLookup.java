package ni.shikatu.re_extera.utils;

import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;

public final class HookLookup {
    private HookLookup() {
    }

    public static Method findByArgCount(Class<?> clazz, String methodName, int paramCount) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterTypes().length == paramCount) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    public static Method findByMinArgCount(Class<?> clazz, String methodName, int minParamCount) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterTypes().length >= minParamCount) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    public static Method findByAnyName(Class<?> clazz, int paramCount, String... candidateNames) {
        for (String name : candidateNames) {
            Method m = findByArgCount(clazz, name, paramCount);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    public static void logMiss(String context, Class<?> clazz, String methodName) {
        Main.log("HookLookup: no match for %s.%s (context: %s)", clazz.getName(), methodName, context);
    }
}
