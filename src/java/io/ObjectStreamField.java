package java.io;

import java.lang.reflect.Field;

/**
 * Java.io.ObjectStreamField类是Serializable类中对Serializable字段的描述。
 * ObjectStreamFields数组用于声明类的Serializable字段。
 */
public class ObjectStreamField implements Comparable<Object> {

    /** field name */
    private final String name;
    /** 字段类型的标准jvm签名 */
    private final String signature;
    /** field type */
    private final Class<?> type;

    private String typeSignature;

    private final boolean unshared;

    private final Field field;

    private int offset;

    public ObjectStreamField(String name, Class<?> type) {
        this(name, type, false);
    }

    public ObjectStreamField(String name, Class<?> type, boolean unshared) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.type = type;
        this.unshared = unshared;
        this.field = null;
        this.signature = null;
    }

    ObjectStreamField(String name, String signature, boolean unshared) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.signature = signature.intern();
        this.unshared = unshared;
        this.field = null;

        switch (signature.charAt(0)) {
            // TODO
//            case 'Z': type = Boolean.TYPE; break;
//            case 'B': type = Byte.TYPE; break;
//            case 'C': type = Character.TYPE; break;
//            case 'S': type = Short.TYPE; break;
//            case 'I': type = Integer.TYPE; break;
//            case 'J': type = Long.TYPE; break;
//            case 'F': type = Float.TYPE; break;
//            case 'D': type = Double.TYPE; break;
            case 'L':
            case '[': type = Object.class; break;
            default: throw new IllegalArgumentException("illegal signature");
        }
    }

    private static String getPrimitiveSignature(Class<?> cl) {
        if (cl == Integer.TYPE)
            return "I";
        else if (cl == Byte.TYPE)
            return "B";
        else if (cl == Long.TYPE)
            return "J";
        else if (cl == Float.TYPE)
            return "F";
        else if (cl == Double.TYPE)
            return "D";
        else if (cl == Short.TYPE)
            return "S";
        else if (cl == Character.TYPE)
            return "C";
        else if (cl == Boolean.TYPE)
            return "Z";
        else if (cl == Void.TYPE)
            return "V";
        else
            throw new InternalError();
    }

    static String getClassSignature(Class<?> cl) {
        if (cl.isPrimitive()) {
            return getPrimitiveSignature(cl);
        } else {
            return appendClassSignature(new StringBuilder(), cl).toString();
        }
    }

    public char getTypeCode() {
        return getSignature().charAt(0);
    }

    public int compareTo(Object obj) {
        ObjectStreamField other = (ObjectStreamField) obj;
        boolean isPrim = isPrimitive();
        if (isPrim != other.isPrimitive()) {
            return isPrim ? -1 : 1;
        }
        return name.compareTo(other.name);
    }

    /**
     * 如果此字段具有基本类型，则返回 true。
     * 基本类型有：boolean、char、byte、short、int、long、float、double
     */
    private boolean isPrimitive() {
        char tcode = getTypeCode();
        return ((tcode != 'L') && (tcode != '['));
    }

    /**
     * 返回字段的 JVM 类型签名（类似于 getTypeString，除了原始字段也返回签名字符串）。
     */
    String getSignature() {
        if (signature != null) {
            return signature;
        }
        String sig = typeSignature;
        /**
         * 这种惰性计算是安全的，因为如果使用公共构造函数之一，则签名可以为空，
         * 在这种情况下，类型始终初始化为我们希望签名表示的确切类型。
         */
        if (sig == null) {
            typeSignature = sig = getClassSignature(type).intern();
        }
        return sig;
    }
}
