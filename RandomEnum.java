package obfuscator.utils;

import java.util.concurrent.ThreadLocalRandom;

public class RandomEnum {
// Source - https://stackoverflow.com/q/1972392
// Posted by Nick Heiner, modified by community. See post 'Timeline' for change history
// Retrieved 2026-11-13, License - CC BY-SA 4.0

    public static <T> T randomValue(Class<T> input) {
        try {
            if (!input.isEnum()){
                throw new IllegalArgumentException("Not an enum : " + input.componentType());
            }
            Class<T> access$input = FuckJVMRestrictions.getTrustedLookup().accessClass(input);
            T[] values = (T[]) access$input.getDeclaredMethod("values").invoke(null);
            int pick = ThreadLocalRandom.current().nextInt(values.length);
            return values[pick];
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

}
