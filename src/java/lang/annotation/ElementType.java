package java.lang.annotation;

public enum ElementType {
    /**
     * Class, Interface (including Annotation Type), or enum declaration
     * Annotation is a type of Interface.
     */
    TYPE,

    /** Field declaration (includes enum constants) */
    FIELD,

    /** Method declaration */
    METHOD,

    /** Formal parameter declaration */
    PARAMETER,

    /** Constructor declaration */
    CONSTRUCTOR,

    /** Local variable declaration */
    LOCAL_VARIABLE,

    /** Annotation type declaration */
    ANNOTATION_TYPE,

    /** Package declaration */
    PACKAGE,

    /** TYPE parameter declaration, @since 1.8 */
    TYPE_PARAMETER,

    /** Use of a type, @since 1.8 */
    TYPE_USE,

    /** Module declaration. @since 9 */
    MODULE
}
