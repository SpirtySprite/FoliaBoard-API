package net.foliaboard.internal.packet.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Reflect {
    private Reflect() {
    }

    public static Class<?> clazz(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("FoliaBoard: class not found: " + name
                    + " (unsupported server version?)", e);
        }
    }

    public static Class<?> firstClass(String... names) {
        RuntimeException last = null;
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                last = new IllegalStateException("not found: " + name, e);
            }
        }
        throw new IllegalStateException("FoliaBoard: none of the candidate classes were found: "
                + String.join(", ", names), last);
    }

    public static Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            Method m = owner.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("FoliaBoard: method not found: "
                    + owner.getName() + "#" + name, e);
        }
    }

    public static Method methodByName(Class<?> owner, String name, int paramCount) {
        for (Method m : owner.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new IllegalStateException("FoliaBoard: no method " + owner.getName() + "#" + name
                + " with " + paramCount + " params");
    }

    public static Method methodByNameDeep(Class<?> owner, String name, int paramCount) {
        Class<?> c = owner;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                    m.setAccessible(true);
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        throw new IllegalStateException("FoliaBoard: no method " + name + " with " + paramCount
                + " params in hierarchy of " + owner.getName());
    }

    public static Field fieldByTypeDeep(Class<?> owner, Class<?> type) {
        Class<?> c = owner;
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f;
                }
            }
            c = c.getSuperclass();
        }
        throw new IllegalStateException("FoliaBoard: no field of type " + type.getName()
                + " in hierarchy of " + owner.getName());
    }

    public static Constructor<?> constructor(Class<?> owner, Class<?>... params) {
        try {
            Constructor<?> c = owner.getDeclaredConstructor(params);
            c.setAccessible(true);
            return c;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("FoliaBoard: constructor not found on " + owner.getName(), e);
        }
    }

    public static Constructor<?> constructorByCount(Class<?> owner, int paramCount) {
        for (Constructor<?> c : owner.getDeclaredConstructors()) {
            if (c.getParameterCount() == paramCount) {
                c.setAccessible(true);
                return c;
            }
        }
        throw new IllegalStateException("FoliaBoard: no constructor on " + owner.getName()
                + " with " + paramCount + " params");
    }

    public static Field field(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("FoliaBoard: field not found: "
                    + owner.getName() + "#" + name, e);
        }
    }

    public static Field fieldByType(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new IllegalStateException("FoliaBoard: no field of type " + type.getName()
                + " in " + owner.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Field field, Object instance) {
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("FoliaBoard: cannot read field " + field, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Method method, Object instance, Object... args) {
        try {
            return (T) method.invoke(instance, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FoliaBoard: cannot invoke " + method, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T instantiate(Constructor<?> constructor, Object... args) {
        try {
            return (T) constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FoliaBoard: cannot construct " + constructor, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass, name);
    }
}
