package obfuscator.utils;

import obfuscator.analysis.misc.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.Objects;
import java.util.Optional;
///todo more skid from jdk.internal.util.random.RandomSupport
public enum Algorithms implements Opcodes {
    XORSHIFT32(true),
    MURMUR32(false),
    JENKINS_INTEGER(false),
    WANG32(false),
    LOWBIAS32(false),
    TRIPLE32(false);

    public final boolean requiresSalt;

    Algorithms(boolean requiresSalt){
        this.requiresSalt = requiresSalt;
    }

    public static enum ResultType{
        REMAIN_ON_STACK, STORE_TO_LOCAL;
    }
    public int calculate(int input, int saltSeed) {
        int x = input;

        switch (this) {
            case XORSHIFT32 -> {
                x ^= x << 13;
                x ^= x >>> 17;
                x ^= x << 5;
                x ^= saltSeed >>> 7;
            }
            case MURMUR32 -> {
                x ^= x >>> 16;
                x *= 0x85ebca6b;
                x ^= x >>> 13;
                x *= 0xc2b2ae35;
                x ^= x >>> 16;
            }
            case JENKINS_INTEGER -> {
                x += x << 10;
                x ^= x >>> 6;
                x += x << 3;
                x ^= x >>> 11;
                x += x << 15;
            }
            case WANG32 -> {
                x = (x ^ (x >>> 16)) * 0x045d9f3b;
                x = (x ^ (x >>> 16)) * 0x045d9f3b;
                x ^= x >>> 16;
            }
            case LOWBIAS32 -> {
                x ^= x >>> 16;
                x *= 0x7feb352d;
                x ^= x >>> 15;
                x *= 0x846ca68b;
                x ^= x >>> 16;
            }
            case TRIPLE32 -> {
                x ^= x >>> 17;
                x *= 0xed5ad4bb;
                x ^= x >>> 11;
                x *= 0xac4c1b51;
                x ^= x >>> 15;
                x *= 0x31848bab;
                x ^= x >>> 14;
                return x;
            }
        }

        return x & 0xFF;
    }

    public int calculate(int input, Optional<Integer> saltSeed) {
        Objects.requireNonNull(saltSeed, "saltSeed");
        if (requiresSalt && saltSeed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Algorithm " + name() + " requires salt seed"
            );
        }
        return calculate(input, saltSeed.orElse(0));
    }

    public int calculate(int input) {
        return calculate(input, Optional.empty());
    }

    public void inject(InsnList instructions, AbstractInsnNode after, Local integer, int saltSeed, ResultType resultType) {
        InsnList list = new InsnList();
        switch (this) {
            case XORSHIFT32 -> {
                // x ^= (x << 13)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(13));
                list.add(new InsnNode(ISHL));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());

                // x ^= (x >>> 17)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(17));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());

                // x ^= (x << 5)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(5));
                list.add(new InsnNode(ISHL));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());

                // x ^= (seed >>> 7)
                list.add(integer.load());
                list.add(ASMUtils.pushInt(saltSeed));
                list.add(ASMUtils.pushInt(7));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
            }
            case MURMUR32 -> {
//                x = (x ^ x >>> 16) * -2048144789;
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(16));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(new LdcInsnNode(-2048144789));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
//               x = (x ^ x >>> 13) * -1028477387;
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(13));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(new LdcInsnNode(-1028477387));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
//                x ^ x >>> 16
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(16));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
            }
            case JENKINS_INTEGER -> {
                // x += (x << 10)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(10));
                list.add(new InsnNode(ISHL));
                list.add(new InsnNode(IADD));
                list.add(integer.store());
                // x ^= (x >>> 6)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(6));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                // x += (x << 3)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(3));
                list.add(new InsnNode(ISHL));
                list.add(new InsnNode(IADD));
                list.add(integer.store());
                // x ^= (x >>> 11)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(11));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                // x += (x << 15) — leave on stack
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(15));
                list.add(new InsnNode(ISHL));
                list.add(new InsnNode(IADD));
            }
            case WANG32 -> {
                // x = ((x >>> 16) ^ x) * 0x45d9f3b
                list.add(integer.load());
                list.add(ASMUtils.pushInt(16));
                list.add(new InsnNode(IUSHR));
                list.add(integer.load());
                list.add(new InsnNode(IXOR));
                list.add(new LdcInsnNode(0x45d9f3b));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
                // repeat
                list.add(integer.load());
                list.add(ASMUtils.pushInt(16));
                list.add(new InsnNode(IUSHR));
                list.add(integer.load());
                list.add(new InsnNode(IXOR));
                list.add(new LdcInsnNode(0x45d9f3b));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
                // x = (x >>> 16) ^ x — leave on stack
                list.add(integer.load());
                list.add(ASMUtils.pushInt(16));
                list.add(new InsnNode(IUSHR));
                list.add(integer.load());
                list.add(new InsnNode(IXOR));
            }
            case LOWBIAS32 -> {
                // x ^= (x >>> 16)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(16));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                // x *= 0x7feb352d
                list.add(integer.load());
                list.add(new LdcInsnNode(0x7feb352d));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
                // x ^= (x >>> 15)
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(15));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                // x *= 0x846ca68b
                list.add(integer.load());
                list.add(new LdcInsnNode(-2073254261));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
                // x ^= (x >>> 16) — leave on stack
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(16));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
            }
            case TRIPLE32 -> {
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(17));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(0xed5ad4bb));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(11));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(0xac4c1b51));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(15));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(0x31848bab));
                list.add(new InsnNode(IMUL));
                list.add(integer.store());
                list.add(integer.load());
                list.add(integer.load());
                list.add(ASMUtils.pushInt(14));
                list.add(new InsnNode(IUSHR));
                list.add(new InsnNode(IXOR));
                list.add(integer.store());
                list.add(integer.load());
            }
            default -> {
                throw new IllegalStateException("Unable to handle saltType " + this);
            }
        }
        switch (resultType){
            case STORE_TO_LOCAL ->  {
                list.add(integer.store());
            }
            case REMAIN_ON_STACK -> {
                // empty
            }
        }

        if (after == null) {
            instructions.insert(list);
        } else {
            instructions.insert(after, list);
        }
    }
}
