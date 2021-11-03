package java.lang.reflect;

import sun.reflect.generics.repository.FieldRepository;

/**
 * Field 提供有关类或接口的单个字段的信息和动态访问。 反射字段可以是类（静态）字段或实例字段。
 */
public final class Field extends AccessibleObject implements Member {

    private Class<?>            clazz;
    private int                 slot;

    private String              name;
    private Class<?>            type;
    private int                 modifiers;
    /** 泛型和注解支持 */
    private transient String    signature;

    private transient FieldRepository genericInfo;
    private byte[]              annotations;
}
