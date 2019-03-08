package fr.mrmicky.ultimatetnt.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author MrMicky
 */
public final class TntUtils {

    private TntUtils() {
        throw new UnsupportedOperationException();
    }

    public static void setTntSource(TNTPrimed tnt, Entity source) throws ReflectiveOperationException {
        Method tntGetHandle = tnt.getClass().getDeclaredMethod("getHandle");
        Method entityGetHandle = source.getClass().getDeclaredMethod("getHandle");

        Object craftTnt = tntGetHandle.invoke(tnt);
        Object craftEntity = entityGetHandle.invoke(source);

        Field sourceField = craftTnt.getClass().getDeclaredField("source");
        sourceField.setAccessible(true);

        sourceField.set(craftTnt, craftEntity);
    }
}
