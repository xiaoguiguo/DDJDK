package java.lang.reflect;

/**
 * Field 提供有关类或接口的单个字段的信息和动态访问。 反射字段可以是类（静态）字段或实例字段。
 */
public final class Field extends AccessibleObject implements Member {

    private Class<?>        clazz;
    private int             slot;

    private String          name;
}
