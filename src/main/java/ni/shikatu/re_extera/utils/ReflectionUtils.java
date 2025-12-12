package ni.shikatu.re_extera.utils;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import ni.shikatu.re_extera.Main;

public class ReflectionUtils {
    public static <T> T invoke(Method method, Object obj, Object... objArr) {
        if (method == null) {
            return null;
        }
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) method.invoke(obj, objArr);
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

    public static <T> T get(Field field, Object obj) {
        if (field == null) {
            return null;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            return (T) field.get(obj);
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

    public static <T> T invokeOriginalMethod(Method method, Object obj, Object[] objArr) {
        if (method == null) {
            return null;
        }
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) XposedBridge.invokeOriginalMethod(method, obj, objArr);
        } catch (IllegalAccessException e) {
            Main.log("Exception", e.getMessage());
            return null;
        } catch (InvocationTargetException e2) {
            Main.log("InvocationTargetException", e2.getMessage());
            return null;
        }
    }
}
