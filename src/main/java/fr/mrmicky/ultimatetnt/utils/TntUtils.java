package fr.mrmicky.ultimatetnt.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TntUtils {

    private static final boolean HAS_SET_SOURCE_METHOD = hasSetSourceMethod();

    private TntUtils() {
        throw new UnsupportedOperationException();
    }

    public static void setTntSource(TNTPrimed tnt, Entity source) throws ReflectiveOperationException {
        if (HAS_SET_SOURCE_METHOD) {
            tnt.setSource(source);
            return;
        }

        // Old Bukkit versions support
        Method tntGetHandle = tnt.getClass().getDeclaredMethod("getHandle");
        Method entityGetHandle = source.getClass().getDeclaredMethod("getHandle");

        Object craftTnt = tntGetHandle.invoke(tnt);
        Object craftEntity = entityGetHandle.invoke(source);

        Field sourceField = craftTnt.getClass().getDeclaredField("source");
        sourceField.setAccessible(true);

        sourceField.set(craftTnt, craftEntity);
    }

    private static boolean hasSetSourceMethod() {
        try {
            TNTPrimed.class.getMethod("setSource", Entity.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
