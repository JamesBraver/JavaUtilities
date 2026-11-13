package obfuscator.utils;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

@UtilityClass
@SuppressWarnings("removal")// shut the fuck up
public final class FuckJVMRestrictions {

    @Getter
    private static Unsafe unsafe;

    @Getter
    private static MethodHandles.Lookup trustedLookup;


    public static <T> boolean setFieldValue(
            Object target,
                                            String fieldName,
                                            T value
    ) {
        FuckShit info;
        try {
            info = getFieldInfo(target, fieldName);
        } catch (NoSuchFieldException e) {
            return false;
        }
        unsafe.putObject(info.base(), info.offset(), value);
        return true;
    }

    @SneakyThrows
    public static <T> T getFieldValue(Object target, String fieldName) {
        FuckShit info = getFieldInfo(target, fieldName);
        //noinspection unchecked
        return (T) unsafe.getObject(info.base(), info.offset());
    }

    /**
     * Extracts the common logic for locating a field's memory address.
     */
    private static FuckShit getFieldInfo(Object target, String fieldName) throws NoSuchFieldException {
        boolean isStatic = target instanceof Class;
        Class<?> clazz = isStatic ? (Class<?>) target : target.getClass();
        Field field = clazz.getDeclaredField(fieldName);

        long offset = isStatic ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field);
        Object base = isStatic ? unsafe.staticFieldBase(field) : target;

        return new FuckShit(base, offset);
    }

    private record FuckShit(Object base, long offset) {
    }

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
            trustedLookup = getFieldValue(MethodHandles.Lookup.class, "IMPL_LOOKUP");
        } catch (ReflectiveOperationException e) {
            unsafe = null;
        }
    }
}