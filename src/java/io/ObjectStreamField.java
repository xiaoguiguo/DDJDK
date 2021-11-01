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


    @Override
    public int compareTo(Object obj) {
        ObjectStreamField other = (ObjectStreamField) obj;
        boolean isPrim = isPrimitive();
        if (isPrim != other.isPrimitive()) {
            return isPrim ? -1 : 1;
        }
        return name.compareTo(other.name);
    }

    private boolean isPrimitive() {
        // TODO
        return true;
    }
}
