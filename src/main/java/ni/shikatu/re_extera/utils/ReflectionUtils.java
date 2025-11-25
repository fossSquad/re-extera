package ni.shikatu.re_extera.utils;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;

public class ReflectionUtils {
    public static Object invoke(Method method, Object object, Object... args) {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return method.invoke(object, args);
        } catch (IllegalAccessException e) {
            Main.log("IllegalAccessException", e.getMessage());
            return null;
        } catch (InvocationTargetException e2) {
            Main.log("InvocationTargetException", e2.getMessage());
            return null;
        } catch (Exception e3) {
            Main.log("Exception", e3.getMessage());
            return null;
        }
    }

    public static Object get(Field field, Object object) {
        if (field == null) {
            return null;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            Main.log("IllegalAccessException", e.getMessage());
            return null;
        }
    }

    public static void set(Field field, Object object, Object value) {
        if (field == null) {
            return;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            Main.log("IllegalAccessException", e.getMessage());
        }
    }

    public static Object invokeOriginalMethod(Method method, Object object, Object[] args) {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return XposedBridge.invokeOriginalMethod(method, object, args);
        } catch (IllegalAccessException e) {
            Main.log("Exception", e.getMessage());
            return null;
        } catch (InvocationTargetException e2) {
            Main.log("InvocationTargetException", e2.getMessage());
            return null;
        }
    }
}
