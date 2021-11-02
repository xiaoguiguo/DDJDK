package java.lang.reflect;

/**
 * TODO
 */
public interface AnnotatedType extends AnnotatedElement {

    default AnnotatedType getAnnotatedOwnerType() {
        return null;
    }

    public Type getType();
}
