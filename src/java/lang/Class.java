package java.lang;

import java.lang.annotation.Annotation;
import java.lang.module.ModuleReader;
import java.lang.ref.SoftReference;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.loader.BootLoader;
import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.module.Resources;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.ConstantPool;
import jdk.internal.reflect.Reflection;
import jdk.internal.reflect.ReflectionFactory;
import jdk.internal.vm.annotation.ForceInline;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.ClassRepository;
import sun.reflect.generics.repository.MethodRepository;
import sun.reflect.generics.repository.ConstructorRepository;
import sun.reflect.generics.scope.ClassScope;
import sun.security.util.SecurityConstants;
import sun.reflect.annotation.*;
import sun.reflect.misc.ReflectUtil;

/**
 * 除了int等基本类型外，Java的其他类型全部都是class（包括interface）
 * class是由JVM在执行过程中动态加载的。JVM在第一次读取到一种class类型时，将其加载进内存。
 * 每加载一种class，JVM就为其创建一个Class类型的实例，并关联起来。
 *
 * 以String类为例，当JVM加载String类时，它首先读取String.class文件到内存，然后，为String类创建一个Class实例并关联起来：
 * Class cls = new Class(String);
 * 这个Class实例是JVM内部创建的，如果我们查看JDK源码，可以发现Class类的构造方法是private，只有JVM能创建Class实例，
 * 我们自己的Java程序是无法创建Class实例的。
 *
 * 一个Class实例包含了该class的所有完整信息：name/package/super/interface/field/method，所以获取到Class实例，
 * 就可以通过反射（reflect）来获取对应类的所有信息。
 *
 * 所有的类都是在对其第一次使用时，动态加载到 JVM 中的（懒加载）。当程序创建第一个对类的静态成员的引用时，就会加载这个类。
 * 使用 new 创建类对象的时候也会被当作对类的静态成员的引用。因此 java 程序程序在它开始运行之前并非被完全加载，
 * 其各个类都是在必需时才加载的。
 *
 * 在类加载阶段，类加载器首先检查这个类的 Class 对象是否已经被加载。如果尚未加载，默认的类加载器就会根据类的全限定名查找 .class文件。
 * 在这个类的字节码被加载时，它们会接受验证，以确保其没有被破坏，并且不包含不良 java 代码。一旦某个类的 Class 对象被载入内存，
 * 我们就可以它来创建这个类的所有对象。
 */
public final class Class<T> implements java.io.Serializable,
                              GenericDeclaration,
                              Type,
                              AnnotatedElement {
    private static final int ANNOTATION= 0x00002000;
    private static final int ENUM      = 0x00004000;
    private static final int SYNTHETIC = 0x00001000;

    /**
     * 见Object
     */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    /*
     * 私有构造函数。 只有 Java 虚拟机创建 Class 对象。 不使用此构造函数并阻止生成默认构造函数。
     */
    private Class(ClassLoader loader, Class<?> arrayComponentType) {
        // Initialize final field for classLoader.  The initialization value of non-null
        // prevents future JIT optimizations from assuming this final field is null.
        classLoader = loader;
        componentType = arrayComponentType;
    }

    /**
     * 返回对象的字符串表示
     */
    public String toString() {
        return (isInterface() ? "interface " : (isPrimitive() ? "" : "class "))
            + getName();
    }

    /**
     * 返回一个描述此对象的字符串
     */
    public String toGenericString() {
        if (isPrimitive()) {
            return toString();
        } else {
            StringBuilder sb = new StringBuilder();
            Class<?> component = this;
            int arrayDepth = 0;

            if (isArray()) {
                do {
                    arrayDepth++;
                    component = component.getComponentType();
                } while (component.isArray());
                sb.append(component.getName());
            } else {
                // Class modifiers are a superset of interface modifiers
                int modifiers = getModifiers() & Modifier.classModifiers();
                if (modifiers != 0) {
                    sb.append(Modifier.toString(modifiers));
                    sb.append(' ');
                }

                if (isAnnotation()) {
                    sb.append('@');
                }
                if (isInterface()) { // Note: all annotation types are interfaces
                    sb.append("interface");
                } else {
                    if (isEnum())
                        sb.append("enum");
                    else
                        sb.append("class");
                }
                sb.append(' ');
                sb.append(getName());
            }

            TypeVariable<?>[] typeparms = component.getTypeParameters();
            if (typeparms.length > 0) {
                StringJoiner sj = new StringJoiner(",", "<", ">");
                for(TypeVariable<?> typeparm: typeparms) {
                    sj.add(typeparm.getTypeName());
                }
                sb.append(sj.toString());
            }

            for (int i = 0; i < arrayDepth; i++)
                sb.append("[]");

            return sb.toString();
        }
    }

    /**
     * 返回给定串名相应的Class对象。
     *
     * 1. 使用Class.forName()静态方法的目的是为了动态加载类。在加载完成后，一般还要调用Class下的newInstance()静态方法来实例化对象以便操作。
     * 因此，单单使用Class.forName( )是动态加载类是没有用的，其最终目的是为了实例化对象。
     *
     * 2. Class.forName()的作用是要求JVM查找并加载指定的类，首先要明白，java里面任何class都要装载在虚拟机上才能运行，
     * 而静态代码是和class绑定的，class装载成功就表示执行了你的静态代码了，而且以后不会再走这段静态代码了。
     */
    @CallerSensitive
    public static Class<?> forName(String className)
                throws ClassNotFoundException {
        Class<?> caller = Reflection.getCallerClass();
        return forName0(className, true, ClassLoader.getClassLoader(caller), caller);
    }


    /**
     * 使用给定的类加载器返回与具有给定字符串名称的类或接口关联的 {@code Class} 对象。
     * 给定类或接口的完全限定名称（与 {@code getName} 返回的格式相同），此方法尝试定位、加载和链接类或接口。
     * 指定的类加载器用于加载类或接口。 如果参数 {@code loader} 为 null，则通过引导类加载器加载该类。
     * 仅当 {@code initialize} 参数为 {@code true} 并且之前尚未初始化该类时，才会初始化该类。
     */
    @CallerSensitive
    public static Class<?> forName(String name, boolean initialize,
                                   ClassLoader loader)
        throws ClassNotFoundException
    {
        Class<?> caller = null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // Reflective call to get caller class is only needed if a security manager
            // is present.  Avoid the overhead of making this call otherwise.
            caller = Reflection.getCallerClass();
            if (loader == null) {
                ClassLoader ccl = ClassLoader.getClassLoader(caller);
                if (ccl != null) {
                    sm.checkPermission(
                        SecurityConstants.GET_CLASSLOADER_PERMISSION);
                }
            }
        }
        return forName0(name, initialize, loader, caller);
    }

    /** 在对系统加载程序访问检查进行安全检查后调用。 */
    private static native Class<?> forName0(String name, boolean initialize,
                                            ClassLoader loader,
                                            Class<?> caller)
        throws ClassNotFoundException;


    /**
     * 返回给定模块中具有给定 <a href="ClassLoader.html#name"> 二进制名称</a> 的 {@code Class}。
     * 此方法尝试定位、加载和链接类或接口。 它不运行类初始值设定项。 如果未找到该类，则此方法返回 {@code null}
     */
    @CallerSensitive
    public static Class<?> forName(Module module, String name) {
        Objects.requireNonNull(module);
        Objects.requireNonNull(name);

        ClassLoader cl;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Class<?> caller = Reflection.getCallerClass();
            if (caller != null && caller.getModule() != module) {
                // if caller is null, Class.forName is the last java frame on the stack.
                // java.base has all permissions
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
            PrivilegedAction<ClassLoader> pa = module::getClassLoader;
            cl = AccessController.doPrivileged(pa);
        } else {
            cl = module.getClassLoader();
        }

        if (cl != null) {
            return cl.loadClass(module, name);
        } else {
            return BootLoader.loadClass(module, name);
        }
    }

    /**
     * 创建对象的方法
     */
    @CallerSensitive
    @Deprecated(since="9")
    public T newInstance()
        throws InstantiationException, IllegalAccessException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), false);
        }

        // NOTE: the following code may not be strictly correct under
        // the current Java memory model.

        // Constructor lookup
        if (cachedConstructor == null) {
            if (this == Class.class) {
                throw new IllegalAccessException(
                    "Can not call newInstance() on the Class for java.lang.Class"
                );
            }
            try {
                Class<?>[] empty = {};
                final Constructor<T> c = getReflectionFactory().copyConstructor(
                    getConstructor0(empty, Member.DECLARED));
                // Disable accessibility checks on the constructor
                // since we have to do the security check here anyway
                // (the stack depth is wrong for the Constructor's
                // security check to work)
                AccessController.doPrivileged(
                    new PrivilegedAction<>() {
                        public Void run() {
                                c.setAccessible(true);
                                return null;
                            }
                        });
                cachedConstructor = c;
            } catch (NoSuchMethodException e) {
                throw (InstantiationException)
                    new InstantiationException(getName()).initCause(e);
            }
        }
        Constructor<T> tmpConstructor = cachedConstructor;
        // Security check (same as in java.lang.reflect.Constructor)
        Class<?> caller = Reflection.getCallerClass();
        if (newInstanceCallerCache != caller) {
            int modifiers = tmpConstructor.getModifiers();
            Reflection.ensureMemberAccess(caller, this, this, modifiers);
            newInstanceCallerCache = caller;
        }
        // Run constructor
        try {
            return tmpConstructor.newInstance((Object[])null);
        } catch (InvocationTargetException e) {
            Unsafe.getUnsafe().throwException(e.getTargetException());
            // Not reached
            return null;
        }
    }
    private transient volatile Constructor<T> cachedConstructor;
    private transient volatile Class<?>       newInstanceCallerCache;


    /**
     * 判断指定的 Object 是否 Class 的实例
     */
    @HotSpotIntrinsicCandidate
    public native boolean isInstance(Object obj);


    /**
     * Determines if the class or interface represented by this
     * {@code Class} object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * {@code Class} parameter. It returns {@code true} if so;
     * otherwise it returns {@code false}. If this {@code Class}
     * object represents a primitive type, this method returns
     * {@code true} if the specified {@code Class} parameter is
     * exactly this {@code Class} object; otherwise it returns
     * {@code false}.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param cls the {@code Class} object to be checked
     * @return the {@code boolean} value indicating whether objects of the
     * type {@code cls} can be assigned to objects of this class
     * @exception NullPointerException if the specified Class parameter is
     *            null.
     * @since 1.1
     */
    @HotSpotIntrinsicCandidate
    public native boolean isAssignableFrom(Class<?> cls);


    /**
     *  判断指定的 Class 对象是否表示一个接口类型
     */
    @HotSpotIntrinsicCandidate
    public native boolean isInterface();


    /**
     * 判断指定的 Class 对象是否标识一个数组类型
     */
    @HotSpotIntrinsicCandidate
    public native boolean isArray();


    /**
     * 判断指定的 Class 对象是否表示一个基本类型
     */
    @HotSpotIntrinsicCandidate
    public native boolean isPrimitive();

    /**
     * 判断当前Class对象是否是注解类型
     */
    public boolean isAnnotation() {
        return (getModifiers() & ANNOTATION) != 0;
    }

    /**
     * Returns {@code true} if this class is a synthetic class;
     * returns {@code false} otherwise.
     * @return {@code true} if and only if this class is a synthetic class as
     *         defined by the Java Language Specification.
     * @jls 13.1 The Form of a Binary
     * @since 1.5
     */
    public boolean isSynthetic() {
        return (getModifiers() & SYNTHETIC) != 0;
    }

    /**
     * 返回全限定名
     */
    public String getName() {
        String name = this.name;
        return name != null ? name : initClassName();
    }

    // Cache the name to reduce the number of calls into the VM.
    // This field would be set by VM itself during initClassName call.
    private transient String name;
    private native String initClassName();

    /**
     * 获取该类的类装载器。
     */
    @CallerSensitive
    @ForceInline // to ensure Reflection.getCallerClass optimization
    public ClassLoader getClassLoader() {
        ClassLoader cl = getClassLoader0();
        if (cl == null)
            return null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ClassLoader.checkClassLoaderPermission(cl, Reflection.getCallerClass());
        }
        return cl;
    }

    // Package-private to allow ClassLoader access
    ClassLoader getClassLoader0() { return classLoader; }

    /**
     * 返回此类或接口所属的模块。
     */
    public Module getModule() {
        return module;
    }

    // set by VM
    private transient Module module;

    // Initialized in JVM not by private constructor
    // This field is filtered from reflection access, i.e. getDeclaredField
    // will throw NoSuchFieldException
    private final ClassLoader classLoader;

    /**
     * Returns an array of {@code TypeVariable} objects that represent the
     * type variables declared by the generic declaration represented by this
     * {@code GenericDeclaration} object, in declaration order.  Returns an
     * array of length 0 if the underlying generic declaration declares no type
     * variables.
     *
     * @return an array of {@code TypeVariable} objects that represent
     *     the type variables declared by this generic declaration
     * @throws java.lang.reflect.GenericSignatureFormatError if the generic
     *     signature of this generic declaration does not conform to
     *     the format specified in
     *     <cite>The Java&trade; Virtual Machine Specification</cite>
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public TypeVariable<Class<T>>[] getTypeParameters() {
        ClassRepository info = getGenericInfo();
        if (info != null)
            return (TypeVariable<Class<T>>[])info.getTypeParameters();
        else
            return (TypeVariable<Class<T>>[])new TypeVariable<?>[0];
    }


    /**
     * Returns the {@code Class} representing the direct superclass of the
     * entity (class, interface, primitive type or void) represented by
     * this {@code Class}.  If this {@code Class} represents either the
     * {@code Object} class, an interface, a primitive type, or void, then
     * null is returned.  If this object represents an array class then the
     * {@code Class} object representing the {@code Object} class is
     * returned.
     *
     * @return the direct superclass of the class represented by this object
     */
    @HotSpotIntrinsicCandidate
    public native Class<? super T> getSuperclass();


    /**
     * Returns the {@code Type} representing the direct superclass of
     * the entity (class, interface, primitive type or void) represented by
     * this {@code Class}.
     *
     * <p>If the superclass is a parameterized type, the {@code Type}
     * object returned must accurately reflect the actual type
     * parameters used in the source code. The parameterized type
     * representing the superclass is created if it had not been
     * created before. See the declaration of {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * semantics of the creation process for parameterized types.  If
     * this {@code Class} represents either the {@code Object}
     * class, an interface, a primitive type, or void, then null is
     * returned.  If this object represents an array class then the
     * {@code Class} object representing the {@code Object} class is
     * returned.
     *
     * @throws java.lang.reflect.GenericSignatureFormatError if the generic
     *     class signature does not conform to the format specified in
     *     <cite>The Java&trade; Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if the generic superclass
     *     refers to a non-existent type declaration
     * @throws java.lang.reflect.MalformedParameterizedTypeException if the
     *     generic superclass refers to a parameterized type that cannot be
     *     instantiated  for any reason
     * @return the direct superclass of the class represented by this object
     * @since 1.5
     */
    public Type getGenericSuperclass() {
        ClassRepository info = getGenericInfo();
        if (info == null) {
            return getSuperclass();
        }

        // Historical irregularity:
        // Generic signature marks interfaces with superclass = Object
        // but this API returns null for interfaces
        if (isInterface()) {
            return null;
        }

        return info.getSuperclass();
    }

    /**
     * Gets the package of this class.
     *
     * <p>If this class represents an array type, a primitive type or void,
     * this method returns {@code null}.
     *
     * @return the package of this class.
     * @revised 9
     * @spec JPMS
     */
    public Package getPackage() {
        if (isPrimitive() || isArray()) {
            return null;
        }
        ClassLoader cl = getClassLoader0();
        return cl != null ? cl.definePackage(this)
                          : BootLoader.definePackage(this);
    }

    /**
     * Returns the fully qualified package name.
     *
     * <p> If this class is a top level class, then this method returns the fully
     * qualified name of the package that the class is a member of, or the
     * empty string if the class is in an unnamed package.
     *
     * <p> If this class is a member class, then this method is equivalent to
     * invoking {@code getPackageName()} on the {@linkplain #getEnclosingClass
     * enclosing class}.
     *
     * <p> If this class is a {@linkplain #isLocalClass local class} or an {@linkplain
     * #isAnonymousClass() anonymous class}, then this method is equivalent to
     * invoking {@code getPackageName()} on the {@linkplain #getDeclaringClass
     * declaring class} of the {@linkplain #getEnclosingMethod enclosing method} or
     * {@linkplain #getEnclosingConstructor enclosing constructor}.
     *
     * <p> If this class represents an array type then this method returns the
     * package name of the element type. If this class represents a primitive
     * type or void then the package name "{@code java.lang}" is returned.
     *
     * @return the fully qualified package name
     *
     * @since 9
     * @spec JPMS
     * @jls 6.7  Fully Qualified Names
     */
    public String getPackageName() {
        String pn = this.packageName;
        if (pn == null) {
            Class<?> c = this;
            while (c.isArray()) {
                c = c.getComponentType();
            }
            if (c.isPrimitive()) {
                pn = "java.lang";
            } else {
                String cn = c.getName();
                int dot = cn.lastIndexOf('.');
                pn = (dot != -1) ? cn.substring(0, dot).intern() : "";
            }
            this.packageName = pn;
        }
        return pn;
    }

    // cached package name
    private transient String packageName;

    /**
     * Returns the interfaces directly implemented by the class or interface
     * represented by this object.
     *
     * <p>If this object represents a class, the return value is an array
     * containing objects representing all interfaces directly implemented by
     * the class.  The order of the interface objects in the array corresponds
     * to the order of the interface names in the {@code implements} clause of
     * the declaration of the class represented by this object.  For example,
     * given the declaration:
     * <blockquote>
     * {@code class Shimmer implements FloorWax, DessertTopping { ... }}
     * </blockquote>
     * suppose the value of {@code s} is an instance of
     * {@code Shimmer}; the value of the expression:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[0]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code FloorWax}; and the value of:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[1]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code DessertTopping}.
     *
     * <p>If this object represents an interface, the array contains objects
     * representing all interfaces directly extended by the interface.  The
     * order of the interface objects in the array corresponds to the order of
     * the interface names in the {@code extends} clause of the declaration of
     * the interface represented by this object.
     *
     * <p>If this object represents a class or interface that implements no
     * interfaces, the method returns an array of length 0.
     *
     * <p>If this object represents a primitive type or void, the method
     * returns an array of length 0.
     *
     * <p>If this {@code Class} object represents an array type, the
     * interfaces {@code Cloneable} and {@code java.io.Serializable} are
     * returned in that order.
     *
     * @return an array of interfaces directly implemented by this class
     */
    public Class<?>[] getInterfaces() {
        // defensively copy before handing over to user code
        return getInterfaces(true);
    }

    private Class<?>[] getInterfaces(boolean cloneArray) {
        ReflectionData<T> rd = reflectionData();
        if (rd == null) {
            // no cloning required
            return getInterfaces0();
        } else {
            Class<?>[] interfaces = rd.interfaces;
            if (interfaces == null) {
                interfaces = getInterfaces0();
                rd.interfaces = interfaces;
            }
            // defensively copy if requested
            return cloneArray ? interfaces.clone() : interfaces;
        }
    }

    private native Class<?>[] getInterfaces0();

    /**
     * Returns the {@code Type}s representing the interfaces
     * directly implemented by the class or interface represented by
     * this object.
     *
     * <p>If a superinterface is a parameterized type, the
     * {@code Type} object returned for it must accurately reflect
     * the actual type parameters used in the source code. The
     * parameterized type representing each superinterface is created
     * if it had not been created before. See the declaration of
     * {@link java.lang.reflect.ParameterizedType ParameterizedType}
     * for the semantics of the creation process for parameterized
     * types.
     *
     * <p>If this object represents a class, the return value is an array
     * containing objects representing all interfaces directly implemented by
     * the class.  The order of the interface objects in the array corresponds
     * to the order of the interface names in the {@code implements} clause of
     * the declaration of the class represented by this object.
     *
     * <p>If this object represents an interface, the array contains objects
     * representing all interfaces directly extended by the interface.  The
     * order of the interface objects in the array corresponds to the order of
     * the interface names in the {@code extends} clause of the declaration of
     * the interface represented by this object.
     *
     * <p>If this object represents a class or interface that implements no
     * interfaces, the method returns an array of length 0.
     *
     * <p>If this object represents a primitive type or void, the method
     * returns an array of length 0.
     *
     * <p>If this {@code Class} object represents an array type, the
     * interfaces {@code Cloneable} and {@code java.io.Serializable} are
     * returned in that order.
     *
     * @throws java.lang.reflect.GenericSignatureFormatError
     *     if the generic class signature does not conform to the format
     *     specified in
     *     <cite>The Java&trade; Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if any of the generic
     *     superinterfaces refers to a non-existent type declaration
     * @throws java.lang.reflect.MalformedParameterizedTypeException
     *     if any of the generic superinterfaces refer to a parameterized
     *     type that cannot be instantiated for any reason
     * @return an array of interfaces directly implemented by this class
     * @since 1.5
     */
    public Type[] getGenericInterfaces() {
        ClassRepository info = getGenericInfo();
        return (info == null) ?  getInterfaces() : info.getSuperInterfaces();
    }


    /**
     * 如果当前类表示一个数组，则返回表示该数组组件的Class对象，否则返回null。
     */
    public Class<?> getComponentType() {
        // Only return for array types. Storage may be reused for Class for instance types.
        if (isArray()) {
            return componentType;
        } else {
            return null;
        }
    }

    private final Class<?> componentType;


    /**
     * Returns the Java language modifiers for this class or interface, encoded
     * in an integer. The modifiers consist of the Java Virtual Machine's
     * constants for {@code public}, {@code protected},
     * {@code private}, {@code final}, {@code static},
     * {@code abstract} and {@code interface}; they should be decoded
     * using the methods of class {@code Modifier}.
     *
     * <p> If the underlying class is an array class, then its
     * {@code public}, {@code private} and {@code protected}
     * modifiers are the same as those of its component type.  If this
     * {@code Class} represents a primitive type or void, its
     * {@code public} modifier is always {@code true}, and its
     * {@code protected} and {@code private} modifiers are always
     * {@code false}. If this object represents an array class, a
     * primitive type or void, then its {@code final} modifier is always
     * {@code true} and its interface modifier is always
     * {@code false}. The values of its other modifiers are not determined
     * by this specification.
     *
     * <p> The modifier encodings are defined in <em>The Java Virtual Machine
     * Specification</em>, table 4.1.
     *
     * @return the {@code int} representing the modifiers for this class
     * @see     Modifier
     * @since 1.1
     */
    @HotSpotIntrinsicCandidate
    public native int getModifiers();


    /**
     * Gets the signers of this class.
     *
     * @return  the signers of this class, or null if there are no signers.  In
     *          particular, this method returns null if this object represents
     *          a primitive type or void.
     * @since   1.1
     */
    public native Object[] getSigners();


    /**
     * Set the signers of this class.
     */
    native void setSigners(Object[] signers);


    /**
     * If this {@code Class} object represents a local or anonymous
     * class within a method, returns a {@link
     * Method Method} object representing the
     * immediately enclosing method of the underlying class. Returns
     * {@code null} otherwise.
     *
     * In particular, this method returns {@code null} if the underlying
     * class is a local or anonymous class immediately enclosed by a type
     * declaration, instance initializer or static initializer.
     *
     * @return the immediately enclosing method of the underlying class, if
     *     that class is a local or anonymous class; otherwise {@code null}.
     *
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and any of the
     *         following conditions is met:
     *
     *         <ul>
     *
     *         <li> the caller's class loader is not the same as the
     *         class loader of the enclosing class and invocation of
     *         {@link SecurityManager#checkPermission
     *         s.checkPermission} method with
     *         {@code RuntimePermission("accessDeclaredMembers")}
     *         denies access to the methods within the enclosing class
     *
     *         <li> the caller's class loader is not the same as or an
     *         ancestor of the class loader for the enclosing class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of the enclosing class
     *
     *         </ul>
     * @since 1.5
     */
    @CallerSensitive
    public Method getEnclosingMethod() throws SecurityException {
        EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();

        if (enclosingInfo == null)
            return null;
        else {
            if (!enclosingInfo.isMethod())
                return null;

            MethodRepository typeInfo = MethodRepository.make(enclosingInfo.getDescriptor(),
                                                              getFactory());
            Class<?>   returnType       = toClass(typeInfo.getReturnType());
            Type []    parameterTypes   = typeInfo.getParameterTypes();
            Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

            // Convert Types to Classes; returned types *should*
            // be class objects since the methodDescriptor's used
            // don't have generics information
            for(int i = 0; i < parameterClasses.length; i++)
                parameterClasses[i] = toClass(parameterTypes[i]);

            // Perform access check
            final Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkMemberAccess(sm, Member.DECLARED,
                                                     Reflection.getCallerClass(), true);
            }
            Method[] candidates = enclosingCandidate.privateGetDeclaredMethods(false);

            /*
             * Loop over all declared methods; match method name,
             * number of and type of parameters, *and* return
             * type.  Matching return type is also necessary
             * because of covariant returns, etc.
             */
            ReflectionFactory fact = getReflectionFactory();
            for (Method m : candidates) {
                if (m.getName().equals(enclosingInfo.getName()) &&
                    arrayContentsEq(parameterClasses,
                                    fact.getExecutableSharedParameterTypes(m))) {
                    // finally, check return type
                    if (m.getReturnType().equals(returnType)) {
                        return fact.copyMethod(m);
                    }
                }
            }

            throw new InternalError("Enclosing method not found");
        }
    }

    private native Object[] getEnclosingMethod0();

    private EnclosingMethodInfo getEnclosingMethodInfo() {
        Object[] enclosingInfo = getEnclosingMethod0();
        if (enclosingInfo == null)
            return null;
        else {
            return new EnclosingMethodInfo(enclosingInfo);
        }
    }

    private static final class EnclosingMethodInfo {
        private final Class<?> enclosingClass;
        private final String name;
        private final String descriptor;

        static void validate(Object[] enclosingInfo) {
            if (enclosingInfo.length != 3)
                throw new InternalError("Malformed enclosing method information");
            try {
                // The array is expected to have three elements:

                // the immediately enclosing class
                Class<?> enclosingClass = (Class<?>)enclosingInfo[0];
                assert(enclosingClass != null);

                // the immediately enclosing method or constructor's
                // name (can be null).
                String name = (String)enclosingInfo[1];

                // the immediately enclosing method or constructor's
                // descriptor (null iff name is).
                String descriptor = (String)enclosingInfo[2];
                assert((name != null && descriptor != null) || name == descriptor);
            } catch (ClassCastException cce) {
                throw new InternalError("Invalid type in enclosing method information", cce);
            }
        }

        EnclosingMethodInfo(Object[] enclosingInfo) {
            validate(enclosingInfo);
            this.enclosingClass = (Class<?>)enclosingInfo[0];
            this.name = (String)enclosingInfo[1];
            this.descriptor = (String)enclosingInfo[2];
        }

        boolean isPartial() {
            return enclosingClass == null || name == null || descriptor == null;
        }

        boolean isConstructor() { return !isPartial() && "<init>".equals(name); }

        boolean isMethod() { return !isPartial() && !isConstructor() && !"<clinit>".equals(name); }

        Class<?> getEnclosingClass() { return enclosingClass; }

        String getName() { return name; }

        String getDescriptor() { return descriptor; }

    }

    private static Class<?> toClass(Type o) {
        if (o instanceof GenericArrayType)
            return Array.newInstance(toClass(((GenericArrayType)o).getGenericComponentType()),
                                     0)
                .getClass();
        return (Class<?>)o;
     }

    /**
     * If this {@code Class} object represents a local or anonymous
     * class within a constructor, returns a {@link
     * Constructor Constructor} object representing
     * the immediately enclosing constructor of the underlying
     * class. Returns {@code null} otherwise.  In particular, this
     * method returns {@code null} if the underlying class is a local
     * or anonymous class immediately enclosed by a type declaration,
     * instance initializer or static initializer.
     *
     * @return the immediately enclosing constructor of the underlying class, if
     *     that class is a local or anonymous class; otherwise {@code null}.
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and any of the
     *         following conditions is met:
     *
     *         <ul>
     *
     *         <li> the caller's class loader is not the same as the
     *         class loader of the enclosing class and invocation of
     *         {@link SecurityManager#checkPermission
     *         s.checkPermission} method with
     *         {@code RuntimePermission("accessDeclaredMembers")}
     *         denies access to the constructors within the enclosing class
     *
     *         <li> the caller's class loader is not the same as or an
     *         ancestor of the class loader for the enclosing class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of the enclosing class
     *
     *         </ul>
     * @since 1.5
     */
    @CallerSensitive
    public Constructor<?> getEnclosingConstructor() throws SecurityException {
        EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();

        if (enclosingInfo == null)
            return null;
        else {
            if (!enclosingInfo.isConstructor())
                return null;

            ConstructorRepository typeInfo = ConstructorRepository.make(enclosingInfo.getDescriptor(),
                                                                        getFactory());
            Type []    parameterTypes   = typeInfo.getParameterTypes();
            Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

            // Convert Types to Classes; returned types *should*
            // be class objects since the methodDescriptor's used
            // don't have generics information
            for(int i = 0; i < parameterClasses.length; i++)
                parameterClasses[i] = toClass(parameterTypes[i]);

            // Perform access check
            final Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkMemberAccess(sm, Member.DECLARED,
                                                     Reflection.getCallerClass(), true);
            }

            Constructor<?>[] candidates = enclosingCandidate
                    .privateGetDeclaredConstructors(false);
            /*
             * Loop over all declared constructors; match number
             * of and type of parameters.
             */
            ReflectionFactory fact = getReflectionFactory();
            for (Constructor<?> c : candidates) {
                if (arrayContentsEq(parameterClasses,
                                    fact.getExecutableSharedParameterTypes(c))) {
                    return fact.copyConstructor(c);
                }
            }

            throw new InternalError("Enclosing constructor not found");
        }
    }


    /**
     * If the class or interface represented by this {@code Class} object
     * is a member of another class, returns the {@code Class} object
     * representing the class in which it was declared.  This method returns
     * null if this class or interface is not a member of any other class.  If
     * this {@code Class} object represents an array class, a primitive
     * type, or void,then this method returns null.
     *
     * @return the declaring class for this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and the caller's
     *         class loader is not the same as or an ancestor of the class
     *         loader for the declaring class and invocation of {@link
     *         SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *         denies access to the package of the declaring class
     * @since 1.1
     */
    @CallerSensitive
    public Class<?> getDeclaringClass() throws SecurityException {
        final Class<?> candidate = getDeclaringClass0();

        if (candidate != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                candidate.checkPackageAccess(sm,
                    ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return candidate;
    }

    private native Class<?> getDeclaringClass0();


    /**
     * Returns the immediately enclosing class of the underlying
     * class.  If the underlying class is a top level class this
     * method returns {@code null}.
     * @return the immediately enclosing class of the underlying class
     * @exception  SecurityException
     *             If a security manager, <i>s</i>, is present and the caller's
     *             class loader is not the same as or an ancestor of the class
     *             loader for the enclosing class and invocation of {@link
     *             SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *             denies access to the package of the enclosing class
     * @since 1.5
     */
    @CallerSensitive
    public Class<?> getEnclosingClass() throws SecurityException {
        // There are five kinds of classes (or interfaces):
        // a) Top level classes
        // b) Nested classes (static member classes)
        // c) Inner classes (non-static member classes)
        // d) Local classes (named classes declared within a method)
        // e) Anonymous classes


        // JVM Spec 4.7.7: A class must have an EnclosingMethod
        // attribute if and only if it is a local class or an
        // anonymous class.
        EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();
        Class<?> enclosingCandidate;

        if (enclosingInfo == null) {
            // This is a top level or a nested class or an inner class (a, b, or c)
            enclosingCandidate = getDeclaringClass0();
        } else {
            Class<?> enclosingClass = enclosingInfo.getEnclosingClass();
            // This is a local class or an anonymous class (d or e)
            if (enclosingClass == this || enclosingClass == null)
                throw new InternalError("Malformed enclosing method information");
            else
                enclosingCandidate = enclosingClass;
        }

        if (enclosingCandidate != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkPackageAccess(sm,
                    ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return enclosingCandidate;
    }

    /**
     * 返回源代码中给出的底层类的简称
     */
    public String getSimpleName() {
        ReflectionData<T> rd = reflectionData();
        String simpleName = rd.simpleName;
        if (simpleName == null) {
            rd.simpleName = simpleName = getSimpleName0();
        }
        return simpleName;
    }

    private String getSimpleName0() {
        if (isArray()) {
            return getComponentType().getSimpleName() + "[]";
        }
        String simpleName = getSimpleBinaryName();
        if (simpleName == null) { // top level class
            simpleName = getName();
            simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1); // strip the package name
        }
        return simpleName;
    }

    /**
     * Return an informative string for the name of this type.
     *
     * @return an informative string for the name of this type
     * @since 1.8
     */
    public String getTypeName() {
        if (isArray()) {
            try {
                Class<?> cl = this;
                int dimensions = 0;
                do {
                    dimensions++;
                    cl = cl.getComponentType();
                } while (cl.isArray());
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e) { /*FALLTHRU*/ }
        }
        return getName();
    }

    /**
     * 返回底层类的规范化名称
     */
    public String getCanonicalName() {
        ReflectionData<T> rd = reflectionData();
        String canonicalName = rd.canonicalName;
        if (canonicalName == null) {
            rd.canonicalName = canonicalName = getCanonicalName0();
        }
        return canonicalName == ReflectionData.NULL_SENTINEL? null : canonicalName;
    }

    private String getCanonicalName0() {
        if (isArray()) {
            String canonicalName = getComponentType().getCanonicalName();
            if (canonicalName != null)
                return canonicalName + "[]";
            else
                return ReflectionData.NULL_SENTINEL;
        }
        if (isLocalOrAnonymousClass())
            return ReflectionData.NULL_SENTINEL;
        Class<?> enclosingClass = getEnclosingClass();
        if (enclosingClass == null) { // top level class
            return getName();
        } else {
            String enclosingName = enclosingClass.getCanonicalName();
            if (enclosingName == null)
                return ReflectionData.NULL_SENTINEL;
            return enclosingName + "." + getSimpleName();
        }
    }

    /**
     * 判断当前类是不是匿名类，匿名类一般用于实例化接口
     */
    public boolean isAnonymousClass() {
        return !isArray() && isLocalOrAnonymousClass() &&
                getSimpleBinaryName0() == null;
    }

    /**
     * 判断是不是局部类，也就是方法里面的类
     */
    public boolean isLocalClass() {
        return isLocalOrAnonymousClass() &&
                (isArray() || getSimpleBinaryName0() != null);
    }

    /**
     * 判断是不是成员内部类，也就是一个类里面定义的类
     */
    public boolean isMemberClass() {
        return !isLocalOrAnonymousClass() && getDeclaringClass0() != null;
    }

    /**
     * Returns the "simple binary name" of the underlying class, i.e.,
     * the binary name without the leading enclosing class name.
     * Returns {@code null} if the underlying class is a top level
     * class.
     */
    private String getSimpleBinaryName() {
        if (isTopLevelClass())
            return null;
        String name = getSimpleBinaryName0();
        if (name == null) // anonymous class
            return "";
        return name;
    }

    private native String getSimpleBinaryName0();

    /**
     * Returns {@code true} if this is a top level class.  Returns {@code false}
     * otherwise.
     */
    private boolean isTopLevelClass() {
        return !isLocalOrAnonymousClass() && getDeclaringClass0() == null;
    }

    /**
     * Returns {@code true} if this is a local class or an anonymous
     * class.  Returns {@code false} otherwise.
     */
    private boolean isLocalOrAnonymousClass() {
        // JVM Spec 4.7.7: A class must have an EnclosingMethod
        // attribute if and only if it is a local class or an
        // anonymous class.
        return hasEnclosingMethodInfo();
    }

    private boolean hasEnclosingMethodInfo() {
        Object[] enclosingInfo = getEnclosingMethod0();
        if (enclosingInfo != null) {
            EnclosingMethodInfo.validate(enclosingInfo);
            return true;
        }
        return false;
    }

    /**
     * Returns an array containing {@code Class} objects representing all
     * the public classes and interfaces that are members of the class
     * represented by this {@code Class} object.  This includes public
     * class and interface members inherited from superclasses and public class
     * and interface members declared by the class.  This method returns an
     * array of length 0 if this {@code Class} object has no public member
     * classes or interfaces.  This method also returns an array of length 0 if
     * this {@code Class} object represents a primitive type, an array
     * class, or void.
     *
     * @return the array of {@code Class} objects representing the public
     *         members of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     */
    @CallerSensitive
    public Class<?>[] getClasses() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), false);
        }

        // Privileged so this implementation can look at DECLARED classes,
        // something the caller might not have privilege to do.  The code here
        // is allowed to look at DECLARED classes because (1) it does not hand
        // out anything other than public members and (2) public member access
        // has already been ok'd by the SecurityManager.

        return AccessController.doPrivileged(
            new PrivilegedAction<>() {
                public Class<?>[] run() {
                    List<Class<?>> list = new ArrayList<>();
                    Class<?> currentClass = Class.this;
                    while (currentClass != null) {
                        for (Class<?> m : currentClass.getDeclaredClasses()) {
                            if (Modifier.isPublic(m.getModifiers())) {
                                list.add(m);
                            }
                        }
                        currentClass = currentClass.getSuperclass();
                    }
                    return list.toArray(new Class<?>[0]);
                }
            });
    }


    /**
     * Returns an array containing {@code Field} objects reflecting all
     * the accessible public fields of the class or interface represented by
     * this {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface with
     * no accessible public fields, then this method returns an array of length
     * 0.
     *
     * <p> If this {@code Class} object represents a class, then this method
     * returns the public fields of the class and of all its superclasses and
     * superinterfaces.
     *
     * <p> If this {@code Class} object represents an interface, then this
     * method returns the fields of the interface and of all its
     * superinterfaces.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then this method returns an array of length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return the array of {@code Field} objects representing the
     *         public fields
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field[] getFields() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return copyFields(privateGetPublicFields());
    }


    /**
     * Returns an array containing {@code Method} objects reflecting all the
     * public methods of the class or interface represented by this {@code
     * Class} object, including those declared by the class or interface and
     * those inherited from superclasses and superinterfaces.
     *
     * <p> If this {@code Class} object represents an array type, then the
     * returned array has a {@code Method} object for each of the public
     * methods inherited by the array type from {@code Object}. It does not
     * contain a {@code Method} object for {@code clone()}.
     *
     * <p> If this {@code Class} object represents an interface then the
     * returned array does not contain any implicitly declared methods from
     * {@code Object}. Therefore, if no methods are explicitly declared in
     * this interface or any of its superinterfaces then the returned array
     * has length 0. (Note that a {@code Class} object which represents a class
     * always has public methods, inherited from {@code Object}.)
     *
     * <p> The returned array never contains methods with names "{@code <init>}"
     * or "{@code <clinit>}".
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * <p> Generally, the result is computed as with the following 4 step algorithm.
     * Let C be the class or interface represented by this {@code Class} object:
     * <ol>
     * <li> A union of methods is composed of:
     *   <ol type="a">
     *   <li> C's declared public instance and static methods as returned by
     *        {@link #getDeclaredMethods()} and filtered to include only public
     *        methods.</li>
     *   <li> If C is a class other than {@code Object}, then include the result
     *        of invoking this algorithm recursively on the superclass of C.</li>
     *   <li> Include the results of invoking this algorithm recursively on all
     *        direct superinterfaces of C, but include only instance methods.</li>
     *   </ol></li>
     * <li> Union from step 1 is partitioned into subsets of methods with same
     *      signature (name, parameter types) and return type.</li>
     * <li> Within each such subset only the most specific methods are selected.
     *      Let method M be a method from a set of methods with same signature
     *      and return type. M is most specific if there is no such method
     *      N != M from the same set, such that N is more specific than M.
     *      N is more specific than M if:
     *   <ol type="a">
     *   <li> N is declared by a class and M is declared by an interface; or</li>
     *   <li> N and M are both declared by classes or both by interfaces and
     *        N's declaring type is the same as or a subtype of M's declaring type
     *        (clearly, if M's and N's declaring types are the same type, then
     *        M and N are the same method).</li>
     *   </ol></li>
     * <li> The result of this algorithm is the union of all selected methods from
     *      step 3.</li>
     * </ol>
     *
     * @apiNote There may be more than one method with a particular name
     * and parameter types in a class because while the Java language forbids a
     * class to declare multiple methods with the same signature but different
     * return types, the Java virtual machine does not.  This
     * increased flexibility in the virtual machine can be used to
     * implement various language features.  For example, covariant
     * returns can be implemented with {@linkplain
     * Method#isBridge bridge methods}; the bridge
     * method and the overriding method would have the same
     * signature but different return types.
     *
     * @return the array of {@code Method} objects representing the
     *         public methods of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method[] getMethods() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return copyMethods(privateGetPublicMethods());
    }


    /**
     * 获取构造函数
     */
    @CallerSensitive
    public Constructor<?>[] getConstructors() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return copyConstructors(privateGetDeclaredConstructors(true));
    }


    /**
     * 获取指定的成员变量
     */
    @CallerSensitive
    public Field getField(String name)
        throws NoSuchFieldException, SecurityException {
        Objects.requireNonNull(name);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        Field field = getField0(name);
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        return getReflectionFactory().copyField(field);
    }


    /**
     * 获取指定的成员方法
     */
    @CallerSensitive
    public Method getMethod(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        Objects.requireNonNull(name);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        Method method = getMethod0(name, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(methodToString(name, parameterTypes));
        }
        return getReflectionFactory().copyMethod(method);
    }

    /**
     * 返回当前Class对象表示的类的指定的公有构造子对象。
     */
    @CallerSensitive
    public Constructor<T> getConstructor(Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return getReflectionFactory().copyConstructor(
            getConstructor0(parameterTypes, Member.PUBLIC));
    }


    /**
     * Returns an array of {@code Class} objects reflecting all the
     * classes and interfaces declared as members of the class represented by
     * this {@code Class} object. This includes public, protected, default
     * (package) access, and private classes and interfaces declared by the
     * class, but excludes inherited classes and interfaces.  This method
     * returns an array of length 0 if the class declares no classes or
     * interfaces as members, or if this {@code Class} object represents a
     * primitive type, an array class, or void.
     *
     * @return the array of {@code Class} objects representing all the
     *         declared members of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and any of the
     *         following conditions is met:
     *
     *         <ul>
     *
     *         <li> the caller's class loader is not the same as the
     *         class loader of this class and invocation of
     *         {@link SecurityManager#checkPermission
     *         s.checkPermission} method with
     *         {@code RuntimePermission("accessDeclaredMembers")}
     *         denies access to the declared classes within this class
     *
     *         <li> the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class
     *
     *         </ul>
     *
     * @since 1.1
     */
    @CallerSensitive
    public Class<?>[] getDeclaredClasses() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), false);
        }
        return getDeclaredClasses0();
    }


    /**
     * Returns an array of {@code Field} objects reflecting all the fields
     * declared by the class or interface represented by this
     * {@code Class} object. This includes public, protected, default
     * (package) access, and private fields, but excludes inherited fields.
     *
     * <p> If this {@code Class} object represents a class or interface with no
     * declared fields, then this method returns an array of length 0.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then this method returns an array of length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return  the array of {@code Field} objects representing all the
     *          declared fields of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared fields within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field[] getDeclaredFields() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        return copyFields(privateGetDeclaredFields(false));
    }


    /**
     * Returns an array containing {@code Method} objects reflecting all the
     * declared methods of the class or interface represented by this {@code
     * Class} object, including public, protected, default (package)
     * access, and private methods, but excluding inherited methods.
     *
     * <p> If this {@code Class} object represents a type that has multiple
     * declared methods with the same name and parameter types, but different
     * return types, then the returned array has a {@code Method} object for
     * each such method.
     *
     * <p> If this {@code Class} object represents a type that has a class
     * initialization method {@code <clinit>}, then the returned array does
     * <em>not</em> have a corresponding {@code Method} object.
     *
     * <p> If this {@code Class} object represents a class or interface with no
     * declared methods, then the returned array has length 0.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then the returned array has length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return  the array of {@code Method} objects representing all the
     *          declared methods of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared methods within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method[] getDeclaredMethods() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        return copyMethods(privateGetDeclaredMethods(false));
    }


    /**
     * 返回当前Class对象表示的类的所有已声明的构造子对象数组。
     */
    @CallerSensitive
    public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        return copyConstructors(privateGetDeclaredConstructors(false));
    }


    /**
     * Returns a {@code Field} object that reflects the specified declared
     * field of the class or interface represented by this {@code Class}
     * object. The {@code name} parameter is a {@code String} that specifies
     * the simple name of the desired field.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code length} field of the array type.
     *
     * @param name the name of the field
     * @return  the {@code Field} object for the specified field in this
     *          class
     * @throws  NoSuchFieldException if a field with the specified name is
     *          not found.
     * @throws  NullPointerException if {@code name} is {@code null}
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared field
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field getDeclaredField(String name)
        throws NoSuchFieldException, SecurityException {
        Objects.requireNonNull(name);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        Field field = searchFields(privateGetDeclaredFields(false), name);
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        return getReflectionFactory().copyField(field);
    }


    /**
     * Returns a {@code Method} object that reflects the specified
     * declared method of the class or interface represented by this
     * {@code Class} object. The {@code name} parameter is a
     * {@code String} that specifies the simple name of the desired
     * method, and the {@code parameterTypes} parameter is an array of
     * {@code Class} objects that identify the method's formal parameter
     * types, in declared order.  If more than one method with the same
     * parameter types is declared in a class, and one of these methods has a
     * return type that is more specific than any of the others, that method is
     * returned; otherwise one of the methods is chosen arbitrarily.  If the
     * name is "&lt;init&gt;"or "&lt;clinit&gt;" a {@code NoSuchMethodException}
     * is raised.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code clone()} method.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter array
     * @return  the {@code Method} object for the method of this class
     *          matching the specified name and parameters
     * @throws  NoSuchMethodException if a matching method is not found.
     * @throws  NullPointerException if {@code name} is {@code null}
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared method
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method getDeclaredMethod(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        Objects.requireNonNull(name);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        Method method = searchMethods(privateGetDeclaredMethods(false), name, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(methodToString(name, parameterTypes));
        }
        return getReflectionFactory().copyMethod(method);
    }

    /**
     * Returns the list of {@code Method} objects for the declared public
     * methods of this class or interface that have the specified method name
     * and parameter types.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter array
     * @return the list of {@code Method} objects for the public methods of
     *         this class matching the specified name and parameters
     */
    List<Method> getDeclaredPublicMethods(String name, Class<?>... parameterTypes) {
        Method[] methods = privateGetDeclaredMethods(/* publicOnly */ true);
        ReflectionFactory factory = getReflectionFactory();
        List<Method> result = new ArrayList<>();
        for (Method method : methods) {
            if (method.getName().equals(name)
                && Arrays.equals(
                    factory.getExecutableSharedParameterTypes(method),
                    parameterTypes)) {
                result.add(factory.copyMethod(method));
            }
        }
        return result;
    }

    /**
     * 返回当前Class对象表示的类的指定已声明的一个构造子对象。
     */
    @CallerSensitive
    public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }

        return getReflectionFactory().copyConstructor(
            getConstructor0(parameterTypes, Member.DECLARED));
    }

    /**
     * Finds a resource with a given name.
     *
     * <p> If this class is in a named {@link Module Module} then this method
     * will attempt to find the resource in the module. This is done by
     * delegating to the module's class loader {@link
     * ClassLoader#findResource(String,String) findResource(String,String)}
     * method, invoking it with the module name and the absolute name of the
     * resource. Resources in named modules are subject to the rules for
     * encapsulation specified in the {@code Module} {@link
     * Module#getResourceAsStream getResourceAsStream} method and so this
     * method returns {@code null} when the resource is a
     * non-"{@code .class}" resource in a package that is not open to the
     * caller's module.
     *
     * <p> Otherwise, if this class is not in a named module then the rules for
     * searching resources associated with a given class are implemented by the
     * defining {@linkplain ClassLoader class loader} of the class.  This method
     * delegates to this object's class loader.  If this object was loaded by
     * the bootstrap class loader, the method delegates to {@link
     * ClassLoader#getSystemResourceAsStream}.
     *
     * <p> Before delegation, an absolute resource name is constructed from the
     * given resource name using this algorithm:
     *
     * <ul>
     *
     * <li> If the {@code name} begins with a {@code '/'}
     * (<code>'&#92;u002f'</code>), then the absolute name of the resource is the
     * portion of the {@code name} following the {@code '/'}.
     *
     * <li> Otherwise, the absolute name is of the following form:
     *
     * <blockquote>
     *   {@code modified_package_name/name}
     * </blockquote>
     *
     * <p> Where the {@code modified_package_name} is the package name of this
     * object with {@code '/'} substituted for {@code '.'}
     * (<code>'&#92;u002e'</code>).
     *
     * </ul>
     *
     * @param  name name of the desired resource
     * @return  A {@link InputStream} object; {@code null} if no
     *          resource with this name is found, the resource is in a package
     *          that is not {@linkplain Module#isOpen(String, Module) open} to at
     *          least the caller module, or access to the resource is denied
     *          by the security manager.
     * @throws  NullPointerException If {@code name} is {@code null}
     *
     * @see Module#getResourceAsStream(String)
     * @since  1.1
     * @revised 9
     * @spec JPMS
     */
    @CallerSensitive
    public InputStream getResourceAsStream(String name) {
        name = resolveName(name);

        Module thisModule = getModule();
        if (thisModule.isNamed()) {
            // check if resource can be located by caller
            if (Resources.canEncapsulate(name)
                && !isOpenToCaller(name, Reflection.getCallerClass())) {
                return null;
            }

            // resource not encapsulated or in package open to caller
            String mn = thisModule.getName();
            ClassLoader cl = getClassLoader0();
            try {

                // special-case built-in class loaders to avoid the
                // need for a URL connection
                if (cl == null) {
                    return BootLoader.findResourceAsStream(mn, name);
                } else if (cl instanceof BuiltinClassLoader) {
                    return ((BuiltinClassLoader) cl).findResourceAsStream(mn, name);
                } else {
                    URL url = cl.findResource(mn, name);
                    return (url != null) ? url.openStream() : null;
                }

            } catch (IOException | SecurityException e) {
                return null;
            }
        }

        // unnamed module
        ClassLoader cl = getClassLoader0();
        if (cl == null) {
            return ClassLoader.getSystemResourceAsStream(name);
        } else {
            return cl.getResourceAsStream(name);
        }
    }

    /**
     * Finds a resource with a given name.
     *
     * <p> If this class is in a named {@link Module Module} then this method
     * will attempt to find the resource in the module. This is done by
     * delegating to the module's class loader {@link
     * ClassLoader#findResource(String,String) findResource(String,String)}
     * method, invoking it with the module name and the absolute name of the
     * resource. Resources in named modules are subject to the rules for
     * encapsulation specified in the {@code Module} {@link
     * Module#getResourceAsStream getResourceAsStream} method and so this
     * method returns {@code null} when the resource is a
     * non-"{@code .class}" resource in a package that is not open to the
     * caller's module.
     *
     * <p> Otherwise, if this class is not in a named module then the rules for
     * searching resources associated with a given class are implemented by the
     * defining {@linkplain ClassLoader class loader} of the class.  This method
     * delegates to this object's class loader. If this object was loaded by
     * the bootstrap class loader, the method delegates to {@link
     * ClassLoader#getSystemResource}.
     *
     * <p> Before delegation, an absolute resource name is constructed from the
     * given resource name using this algorithm:
     *
     * <ul>
     *
     * <li> If the {@code name} begins with a {@code '/'}
     * (<code>'&#92;u002f'</code>), then the absolute name of the resource is the
     * portion of the {@code name} following the {@code '/'}.
     *
     * <li> Otherwise, the absolute name is of the following form:
     *
     * <blockquote>
     *   {@code modified_package_name/name}
     * </blockquote>
     *
     * <p> Where the {@code modified_package_name} is the package name of this
     * object with {@code '/'} substituted for {@code '.'}
     * (<code>'&#92;u002e'</code>).
     *
     * </ul>
     *
     * @param  name name of the desired resource
     * @return A {@link URL} object; {@code null} if no resource with
     *         this name is found, the resource cannot be located by a URL, the
     *         resource is in a package that is not
     *         {@linkplain Module#isOpen(String, Module) open} to at least the caller
     *         module, or access to the resource is denied by the security
     *         manager.
     * @throws NullPointerException If {@code name} is {@code null}
     * @since  1.1
     * @revised 9
     * @spec JPMS
     */
    @CallerSensitive
    public URL getResource(String name) {
        name = resolveName(name);

        Module thisModule = getModule();
        if (thisModule.isNamed()) {
            // check if resource can be located by caller
            if (Resources.canEncapsulate(name)
                && !isOpenToCaller(name, Reflection.getCallerClass())) {
                return null;
            }

            // resource not encapsulated or in package open to caller
            String mn = thisModule.getName();
            ClassLoader cl = getClassLoader0();
            try {
                if (cl == null) {
                    return BootLoader.findResource(mn, name);
                } else {
                    return cl.findResource(mn, name);
                }
            } catch (IOException ioe) {
                return null;
            }
        }

        // unnamed module
        ClassLoader cl = getClassLoader0();
        if (cl == null) {
            return ClassLoader.getSystemResource(name);
        } else {
            return cl.getResource(name);
        }
    }

    /**
     * Returns true if a resource with the given name can be located by the
     * given caller. All resources in a module can be located by code in
     * the module. For other callers, then the package needs to be open to
     * the caller.
     */
    private boolean isOpenToCaller(String name, Class<?> caller) {
        // assert getModule().isNamed();
        Module thisModule = getModule();
        Module callerModule = (caller != null) ? caller.getModule() : null;
        if (callerModule != thisModule) {
            String pn = Resources.toPackageName(name);
            if (thisModule.getDescriptor().packages().contains(pn)) {
                if (callerModule == null && !thisModule.isOpen(pn)) {
                    // no caller, package not open
                    return false;
                }
                if (!thisModule.isOpen(pn, callerModule)) {
                    // package not open to caller
                    return false;
                }
            }
        }
        return true;
    }


    /** protection domain returned when the internal domain is null */
    private static java.security.ProtectionDomain allPermDomain;

    /**
     * Returns the {@code ProtectionDomain} of this class.  If there is a
     * security manager installed, this method first calls the security
     * manager's {@code checkPermission} method with a
     * {@code RuntimePermission("getProtectionDomain")} permission to
     * ensure it's ok to get the
     * {@code ProtectionDomain}.
     *
     * @return the ProtectionDomain of this class
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        getting the ProtectionDomain.
     *
     * @see java.security.ProtectionDomain
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @since 1.2
     */
    public java.security.ProtectionDomain getProtectionDomain() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.GET_PD_PERMISSION);
        }
        java.security.ProtectionDomain pd = getProtectionDomain0();
        if (pd == null) {
            if (allPermDomain == null) {
                java.security.Permissions perms =
                    new java.security.Permissions();
                perms.add(SecurityConstants.ALL_PERMISSION);
                allPermDomain =
                    new java.security.ProtectionDomain(null, perms);
            }
            pd = allPermDomain;
        }
        return pd;
    }


    /**
     * Returns the ProtectionDomain of this class.
     */
    private native java.security.ProtectionDomain getProtectionDomain0();

    /*
     * Return the Virtual Machine's Class object for the named
     * primitive type.
     */
    static native Class<?> getPrimitiveClass(String name);

    /*
     * Check if client is allowed to access members.  If access is denied,
     * throw a SecurityException.
     *
     * This method also enforces package access.
     *
     * <p> Default policy: allow all clients access with normal Java access
     * control.
     *
     * <p> NOTE: should only be called if a SecurityManager is installed
     */
    private void checkMemberAccess(SecurityManager sm, int which,
                                   Class<?> caller, boolean checkProxyInterfaces) {
        /* Default policy allows access to all {@link Member#PUBLIC} members,
         * as well as access to classes that have the same class loader as the caller.
         * In all other cases, it requires RuntimePermission("accessDeclaredMembers")
         * permission.
         */
        final ClassLoader ccl = ClassLoader.getClassLoader(caller);
        if (which != Member.PUBLIC) {
            final ClassLoader cl = getClassLoader0();
            if (ccl != cl) {
                sm.checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
            }
        }
        this.checkPackageAccess(sm, ccl, checkProxyInterfaces);
    }

    /*
     * Checks if a client loaded in ClassLoader ccl is allowed to access this
     * class under the current package access policy. If access is denied,
     * throw a SecurityException.
     *
     * NOTE: this method should only be called if a SecurityManager is active
     */
    private void checkPackageAccess(SecurityManager sm, final ClassLoader ccl,
                                    boolean checkProxyInterfaces) {
        final ClassLoader cl = getClassLoader0();

        if (ReflectUtil.needsPackageAccessCheck(ccl, cl)) {
            String pkg = this.getPackageName();
            if (pkg != null && !pkg.isEmpty()) {
                // skip the package access check on a proxy class in default proxy package
                if (!Proxy.isProxyClass(this) || ReflectUtil.isNonPublicProxyClass(this)) {
                    sm.checkPackageAccess(pkg);
                }
            }
        }
        // check package access on the proxy interfaces
        if (checkProxyInterfaces && Proxy.isProxyClass(this)) {
            ReflectUtil.checkProxyPackageAccess(ccl, this.getInterfaces());
        }
    }

    /**
     * Add a package name prefix if the name is not absolute Remove leading "/"
     * if name is absolute
     */
    private String resolveName(String name) {
        if (!name.startsWith("/")) {
            Class<?> c = this;
            while (c.isArray()) {
                c = c.getComponentType();
            }
            String baseName = c.getPackageName();
            if (baseName != null && !baseName.isEmpty()) {
                name = baseName.replace('.', '/') + "/" + name;
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }

    /**
     * Atomic operations support.
     */
    private static class Atomic {
        // initialize Unsafe machinery here, since we need to call Class.class instance method
        // and have to avoid calling it in the static initializer of the Class class...
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        // offset of Class.reflectionData instance field
        private static final long reflectionDataOffset
                = unsafe.objectFieldOffset(Class.class, "reflectionData");
        // offset of Class.annotationType instance field
        private static final long annotationTypeOffset
                = unsafe.objectFieldOffset(Class.class, "annotationType");
        // offset of Class.annotationData instance field
        private static final long annotationDataOffset
                = unsafe.objectFieldOffset(Class.class, "annotationData");

        static <T> boolean casReflectionData(Class<?> clazz,
                                             SoftReference<ReflectionData<T>> oldData,
                                             SoftReference<ReflectionData<T>> newData) {
            return unsafe.compareAndSetObject(clazz, reflectionDataOffset, oldData, newData);
        }

        static <T> boolean casAnnotationType(Class<?> clazz,
                                             AnnotationType oldType,
                                             AnnotationType newType) {
            return unsafe.compareAndSetObject(clazz, annotationTypeOffset, oldType, newType);
        }

        static <T> boolean casAnnotationData(Class<?> clazz,
                                             AnnotationData oldData,
                                             AnnotationData newData) {
            return unsafe.compareAndSetObject(clazz, annotationDataOffset, oldData, newData);
        }
    }

    /**
     * Reflection support.
     */

    // Reflection data caches various derived names and reflective members. Cached
    // values may be invalidated when JVM TI RedefineClasses() is called
    private static class ReflectionData<T> {
        volatile Field[] declaredFields;
        volatile Field[] publicFields;
        volatile Method[] declaredMethods;
        volatile Method[] publicMethods;
        volatile Constructor<T>[] declaredConstructors;
        volatile Constructor<T>[] publicConstructors;
        // Intermediate results for getFields and getMethods
        volatile Field[] declaredPublicFields;
        volatile Method[] declaredPublicMethods;
        volatile Class<?>[] interfaces;

        // Cached names
        String simpleName;
        String canonicalName;
        static final String NULL_SENTINEL = new String();

        // Value of classRedefinedCount when we created this ReflectionData instance
        final int redefinedCount;

        ReflectionData(int redefinedCount) {
            this.redefinedCount = redefinedCount;
        }
    }

    private transient volatile SoftReference<ReflectionData<T>> reflectionData;

    // Incremented by the VM on each call to JVM TI RedefineClasses()
    // that redefines this class or a superclass.
    private transient volatile int classRedefinedCount;

    // Lazily create and cache ReflectionData
    private ReflectionData<T> reflectionData() {
        SoftReference<ReflectionData<T>> reflectionData = this.reflectionData;
        int classRedefinedCount = this.classRedefinedCount;
        ReflectionData<T> rd;
        if (reflectionData != null &&
            (rd = reflectionData.get()) != null &&
            rd.redefinedCount == classRedefinedCount) {
            return rd;
        }
        // else no SoftReference or cleared SoftReference or stale ReflectionData
        // -> create and replace new instance
        return newReflectionData(reflectionData, classRedefinedCount);
    }

    private ReflectionData<T> newReflectionData(SoftReference<ReflectionData<T>> oldReflectionData,
                                                int classRedefinedCount) {
        while (true) {
            ReflectionData<T> rd = new ReflectionData<>(classRedefinedCount);
            // try to CAS it...
            if (Atomic.casReflectionData(this, oldReflectionData, new SoftReference<>(rd))) {
                return rd;
            }
            // else retry
            oldReflectionData = this.reflectionData;
            classRedefinedCount = this.classRedefinedCount;
            if (oldReflectionData != null &&
                (rd = oldReflectionData.get()) != null &&
                rd.redefinedCount == classRedefinedCount) {
                return rd;
            }
        }
    }

    // Generic signature handling
    private native String getGenericSignature0();

    // Generic info repository; lazily initialized
    private transient volatile ClassRepository genericInfo;

    // accessor for factory
    private GenericsFactory getFactory() {
        // create scope and factory
        return CoreReflectionFactory.make(this, ClassScope.make(this));
    }

    // accessor for generic info repository;
    // generic info is lazily initialized
    private ClassRepository getGenericInfo() {
        ClassRepository genericInfo = this.genericInfo;
        if (genericInfo == null) {
            String signature = getGenericSignature0();
            if (signature == null) {
                genericInfo = ClassRepository.NONE;
            } else {
                genericInfo = ClassRepository.make(signature, getFactory());
            }
            this.genericInfo = genericInfo;
        }
        return (genericInfo != ClassRepository.NONE) ? genericInfo : null;
    }

    // Annotations handling
    native byte[] getRawAnnotations();
    // Since 1.8
    native byte[] getRawTypeAnnotations();
    static byte[] getExecutableTypeAnnotationBytes(Executable ex) {
        return getReflectionFactory().getExecutableTypeAnnotationBytes(ex);
    }

    native ConstantPool getConstantPool();

    //
    //
    // java.lang.reflect.Field handling
    //
    //

    // Returns an array of "root" fields. These Field objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field[] privateGetDeclaredFields(boolean publicOnly) {
        Field[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.declaredPublicFields : rd.declaredFields;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        res = Reflection.filterFields(this, getDeclaredFields0(publicOnly));
        if (rd != null) {
            if (publicOnly) {
                rd.declaredPublicFields = res;
            } else {
                rd.declaredFields = res;
            }
        }
        return res;
    }

    // Returns an array of "root" fields. These Field objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field[] privateGetPublicFields() {
        Field[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = rd.publicFields;
            if (res != null) return res;
        }

        // Use a linked hash set to ensure order is preserved and
        // fields from common super interfaces are not duplicated
        LinkedHashSet<Field> fields = new LinkedHashSet<>();

        // Local fields
        addAll(fields, privateGetDeclaredFields(true));

        // Direct superinterfaces, recursively
        for (Class<?> si : getInterfaces()) {
            addAll(fields, si.privateGetPublicFields());
        }

        // Direct superclass, recursively
        Class<?> sc = getSuperclass();
        if (sc != null) {
            addAll(fields, sc.privateGetPublicFields());
        }

        res = fields.toArray(new Field[0]);
        if (rd != null) {
            rd.publicFields = res;
        }
        return res;
    }

    private static void addAll(Collection<Field> c, Field[] o) {
        for (Field f : o) {
            c.add(f);
        }
    }


    //
    //
    // java.lang.reflect.Constructor handling
    //
    //

    // Returns an array of "root" constructors. These Constructor
    // objects must NOT be propagated to the outside world, but must
    // instead be copied via ReflectionFactory.copyConstructor.
    private Constructor<T>[] privateGetDeclaredConstructors(boolean publicOnly) {
        Constructor<T>[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.publicConstructors : rd.declaredConstructors;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        if (isInterface()) {
            @SuppressWarnings("unchecked")
            Constructor<T>[] temporaryRes = (Constructor<T>[]) new Constructor<?>[0];
            res = temporaryRes;
        } else {
            res = getDeclaredConstructors0(publicOnly);
        }
        if (rd != null) {
            if (publicOnly) {
                rd.publicConstructors = res;
            } else {
                rd.declaredConstructors = res;
            }
        }
        return res;
    }

    //
    //
    // java.lang.reflect.Method handling
    //
    //

    // Returns an array of "root" methods. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method[] privateGetDeclaredMethods(boolean publicOnly) {
        Method[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.declaredPublicMethods : rd.declaredMethods;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        res = Reflection.filterMethods(this, getDeclaredMethods0(publicOnly));
        if (rd != null) {
            if (publicOnly) {
                rd.declaredPublicMethods = res;
            } else {
                rd.declaredMethods = res;
            }
        }
        return res;
    }

    // Returns an array of "root" methods. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method[] privateGetPublicMethods() {
        Method[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = rd.publicMethods;
            if (res != null) return res;
        }

        // No cached value available; compute value recursively.
        // Start by fetching public declared methods...
        PublicMethods pms = new PublicMethods();
        for (Method m : privateGetDeclaredMethods(/* publicOnly */ true)) {
            pms.merge(m);
        }
        // ...then recur over superclass methods...
        Class<?> sc = getSuperclass();
        if (sc != null) {
            for (Method m : sc.privateGetPublicMethods()) {
                pms.merge(m);
            }
        }
        // ...and finally over direct superinterfaces.
        for (Class<?> intf : getInterfaces(/* cloneArray */ false)) {
            for (Method m : intf.privateGetPublicMethods()) {
                // static interface methods are not inherited
                if (!Modifier.isStatic(m.getModifiers())) {
                    pms.merge(m);
                }
            }
        }

        res = pms.toArray();
        if (rd != null) {
            rd.publicMethods = res;
        }
        return res;
    }


    //
    // Helpers for fetchers of one field, method, or constructor
    //

    // This method does not copy the returned Field object!
    private static Field searchFields(Field[] fields, String name) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    // Returns a "root" Field object. This Field object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field getField0(String name) {
        // Note: the intent is that the search algorithm this routine
        // uses be equivalent to the ordering imposed by
        // privateGetPublicFields(). It fetches only the declared
        // public fields for each class, however, to reduce the number
        // of Field objects which have to be created for the common
        // case where the field being requested is declared in the
        // class which is being queried.
        Field res;
        // Search declared public fields
        if ((res = searchFields(privateGetDeclaredFields(true), name)) != null) {
            return res;
        }
        // Direct superinterfaces, recursively
        Class<?>[] interfaces = getInterfaces(/* cloneArray */ false);
        for (Class<?> c : interfaces) {
            if ((res = c.getField0(name)) != null) {
                return res;
            }
        }
        // Direct superclass, recursively
        if (!isInterface()) {
            Class<?> c = getSuperclass();
            if (c != null) {
                if ((res = c.getField0(name)) != null) {
                    return res;
                }
            }
        }
        return null;
    }

    // This method does not copy the returned Method object!
    private static Method searchMethods(Method[] methods,
                                        String name,
                                        Class<?>[] parameterTypes)
    {
        ReflectionFactory fact = getReflectionFactory();
        Method res = null;
        for (Method m : methods) {
            if (m.getName().equals(name)
                && arrayContentsEq(parameterTypes,
                                   fact.getExecutableSharedParameterTypes(m))
                && (res == null
                    || (res.getReturnType() != m.getReturnType()
                        && res.getReturnType().isAssignableFrom(m.getReturnType()))))
                res = m;
        }
        return res;
    }

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    // Returns a "root" Method object. This Method object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method getMethod0(String name, Class<?>[] parameterTypes) {
        PublicMethods.MethodList res = getMethodsRecursive(
            name,
            parameterTypes == null ? EMPTY_CLASS_ARRAY : parameterTypes,
            /* includeStatic */ true);
        return res == null ? null : res.getMostSpecific();
    }

    // Returns a list of "root" Method objects. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private PublicMethods.MethodList getMethodsRecursive(String name,
                                                         Class<?>[] parameterTypes,
                                                         boolean includeStatic) {
        // 1st check declared public methods
        Method[] methods = privateGetDeclaredMethods(/* publicOnly */ true);
        PublicMethods.MethodList res = PublicMethods.MethodList
            .filter(methods, name, parameterTypes, includeStatic);
        // if there is at least one match among declared methods, we need not
        // search any further as such match surely overrides matching methods
        // declared in superclass(es) or interface(s).
        if (res != null) {
            return res;
        }

        // if there was no match among declared methods,
        // we must consult the superclass (if any) recursively...
        Class<?> sc = getSuperclass();
        if (sc != null) {
            res = sc.getMethodsRecursive(name, parameterTypes, includeStatic);
        }

        // ...and coalesce the superclass methods with methods obtained
        // from directly implemented interfaces excluding static methods...
        for (Class<?> intf : getInterfaces(/* cloneArray */ false)) {
            res = PublicMethods.MethodList.merge(
                res, intf.getMethodsRecursive(name, parameterTypes,
                                              /* includeStatic */ false));
        }

        return res;
    }

    // Returns a "root" Constructor object. This Constructor object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyConstructor.
    private Constructor<T> getConstructor0(Class<?>[] parameterTypes,
                                        int which) throws NoSuchMethodException
    {
        ReflectionFactory fact = getReflectionFactory();
        Constructor<T>[] constructors = privateGetDeclaredConstructors((which == Member.PUBLIC));
        for (Constructor<T> constructor : constructors) {
            if (arrayContentsEq(parameterTypes,
                                fact.getExecutableSharedParameterTypes(constructor))) {
                return constructor;
            }
        }
        throw new NoSuchMethodException(methodToString("<init>", parameterTypes));
    }

    //
    // Other helpers and base implementation
    //

    private static boolean arrayContentsEq(Object[] a1, Object[] a2) {
        if (a1 == null) {
            return a2 == null || a2.length == 0;
        }

        if (a2 == null) {
            return a1.length == 0;
        }

        if (a1.length != a2.length) {
            return false;
        }

        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    private static Field[] copyFields(Field[] arg) {
        Field[] out = new Field[arg.length];
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < arg.length; i++) {
            out[i] = fact.copyField(arg[i]);
        }
        return out;
    }

    private static Method[] copyMethods(Method[] arg) {
        Method[] out = new Method[arg.length];
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < arg.length; i++) {
            out[i] = fact.copyMethod(arg[i]);
        }
        return out;
    }

    private static <U> Constructor<U>[] copyConstructors(Constructor<U>[] arg) {
        Constructor<U>[] out = arg.clone();
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < out.length; i++) {
            out[i] = fact.copyConstructor(out[i]);
        }
        return out;
    }

    private native Field[]       getDeclaredFields0(boolean publicOnly);
    private native Method[]      getDeclaredMethods0(boolean publicOnly);
    private native Constructor<T>[] getDeclaredConstructors0(boolean publicOnly);
    private native Class<?>[]   getDeclaredClasses0();

    /**
     * Helper method to get the method name from arguments.
     */
    private String methodToString(String name, Class<?>[] argTypes) {
        StringJoiner sj = new StringJoiner(", ", getName() + "." + name + "(", ")");
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                Class<?> c = argTypes[i];
                sj.add((c == null) ? "null" : c.getName());
            }
        }
        return sj.toString();
    }

    /** use serialVersionUID from JDK 1.1 for interoperability */
    private static final long serialVersionUID = 3206093459760846163L;


    /**
     * Class Class is special cased within the Serialization Stream Protocol.
     *
     * A Class instance is written initially into an ObjectOutputStream in the
     * following format:
     * <pre>
     *      {@code TC_CLASS} ClassDescriptor
     *      A ClassDescriptor is a special cased serialization of
     *      a {@code java.io.ObjectStreamClass} instance.
     * </pre>
     * A new handle is generated for the initial time the class descriptor
     * is written into the stream. Future references to the class descriptor
     * are written as references to the initial class descriptor instance.
     *
     * @see java.io.ObjectStreamClass
     */
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];


    /**
     * Returns the assertion status that would be assigned to this
     * class if it were to be initialized at the time this method is invoked.
     * If this class has had its assertion status set, the most recent
     * setting will be returned; otherwise, if any package default assertion
     * status pertains to this class, the most recent setting for the most
     * specific pertinent package default assertion status is returned;
     * otherwise, if this class is not a system class (i.e., it has a
     * class loader) its class loader's default assertion status is returned;
     * otherwise, the system class default assertion status is returned.
     * <p>
     * Few programmers will have any need for this method; it is provided
     * for the benefit of the JRE itself.  (It allows a class to determine at
     * the time that it is initialized whether assertions should be enabled.)
     * Note that this method is not guaranteed to return the actual
     * assertion status that was (or will be) associated with the specified
     * class when it was (or will be) initialized.
     *
     * @return the desired assertion status of the specified class.
     * @see    ClassLoader#setClassAssertionStatus
     * @see    ClassLoader#setPackageAssertionStatus
     * @see    ClassLoader#setDefaultAssertionStatus
     * @since  1.4
     */
    public boolean desiredAssertionStatus() {
        ClassLoader loader = getClassLoader0();
        // If the loader is null this is a system class, so ask the VM
        if (loader == null)
            return desiredAssertionStatus0(this);

        // If the classloader has been initialized with the assertion
        // directives, ask it. Otherwise, ask the VM.
        synchronized(loader.assertionLock) {
            if (loader.classAssertionStatus != null) {
                return loader.desiredAssertionStatus(getName());
            }
        }
        return desiredAssertionStatus0(this);
    }

    // Retrieves the desired assertion status of this class from the VM
    private static native boolean desiredAssertionStatus0(Class<?> clazz);

    /**
     * 判断该类是否是枚举类
     */
    public boolean isEnum() {
        // An enum must both directly extend java.lang.Enum and have
        // the ENUM bit set; classes for specialized enum constants
        // don't do the former.
        return (this.getModifiers() & ENUM) != 0 &&
        this.getSuperclass() == Enum.class;
    }

    // Fetches the factory for reflective objects
    private static ReflectionFactory getReflectionFactory() {
        if (reflectionFactory == null) {
            reflectionFactory =
                AccessController.doPrivileged
                    (new ReflectionFactory.GetReflectionFactoryAction());
        }
        return reflectionFactory;
    }
    private static ReflectionFactory reflectionFactory;

    /**
     * Returns the elements of this enum class or null if this
     * Class object does not represent an enum type.
     *
     * @return an array containing the values comprising the enum class
     *     represented by this Class object in the order they're
     *     declared, or null if this Class object does not
     *     represent an enum type
     * @since 1.5
     */
    public T[] getEnumConstants() {
        T[] values = getEnumConstantsShared();
        return (values != null) ? values.clone() : null;
    }

    /**
     * Returns the elements of this enum class or null if this
     * Class object does not represent an enum type;
     * identical to getEnumConstants except that the result is
     * uncloned, cached, and shared by all callers.
     */
    T[] getEnumConstantsShared() {
        T[] constants = enumConstants;
        if (constants == null) {
            if (!isEnum()) return null;
            try {
                final Method values = getMethod("values");
                AccessController.doPrivileged(
                    new PrivilegedAction<>() {
                        public Void run() {
                                values.setAccessible(true);
                                return null;
                            }
                        });
                @SuppressWarnings("unchecked")
                T[] temporaryConstants = (T[])values.invoke(null);
                enumConstants = constants = temporaryConstants;
            }
            // These can happen when users concoct enum-like classes
            // that don't comply with the enum spec.
            catch (InvocationTargetException | NoSuchMethodException |
                   IllegalAccessException ex) { return null; }
        }
        return constants;
    }
    private transient volatile T[] enumConstants;

    /**
     * Returns a map from simple name to enum constant.  This package-private
     * method is used internally by Enum to implement
     * {@code public static <T extends Enum<T>> T valueOf(Class<T>, String)}
     * efficiently.  Note that the map is returned by this method is
     * created lazily on first use.  Typically it won't ever get created.
     */
    Map<String, T> enumConstantDirectory() {
        Map<String, T> directory = enumConstantDirectory;
        if (directory == null) {
            T[] universe = getEnumConstantsShared();
            if (universe == null)
                throw new IllegalArgumentException(
                    getName() + " is not an enum type");
            directory = new HashMap<>((int)(universe.length / 0.75f) + 1);
            for (T constant : universe) {
                directory.put(((Enum<?>)constant).name(), constant);
            }
            enumConstantDirectory = directory;
        }
        return directory;
    }
    private transient volatile Map<String, T> enumConstantDirectory;

    /**
     * Casts an object to the class or interface represented
     * by this {@code Class} object.
     *
     * @param obj the object to be cast
     * @return the object after casting, or null if obj is null
     *
     * @throws ClassCastException if the object is not
     * null and is not assignable to the type T.
     *
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    @HotSpotIntrinsicCandidate
    public T cast(Object obj) {
        if (obj != null && !isInstance(obj))
            throw new ClassCastException(cannotCastMsg(obj));
        return (T) obj;
    }

    private String cannotCastMsg(Object obj) {
        return "Cannot cast " + obj.getClass().getName() + " to " + getName();
    }

    /**
     * Casts this {@code Class} object to represent a subclass of the class
     * represented by the specified class object.  Checks that the cast
     * is valid, and throws a {@code ClassCastException} if it is not.  If
     * this method succeeds, it always returns a reference to this class object.
     *
     * <p>This method is useful when a client needs to "narrow" the type of
     * a {@code Class} object to pass it to an API that restricts the
     * {@code Class} objects that it is willing to accept.  A cast would
     * generate a compile-time warning, as the correctness of the cast
     * could not be checked at runtime (because generic types are implemented
     * by erasure).
     *
     * @param <U> the type to cast this class object to
     * @param clazz the class of the type to cast this class object to
     * @return this {@code Class} object, cast to represent a subclass of
     *    the specified class object.
     * @throws ClassCastException if this {@code Class} object does not
     *    represent a subclass of the specified class (here "subclass" includes
     *    the class itself).
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public <U> Class<? extends U> asSubclass(Class<U> clazz) {
        if (clazz.isAssignableFrom(this))
            return (Class<? extends U>) this;
        else
            throw new ClassCastException(this.toString());
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        return (A) annotationData().annotations.get(annotationClass);
    }

    /**
     * 判断指定类型的注解是否存在
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return GenericDeclaration.super.isAnnotationPresent(annotationClass);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        AnnotationData annotationData = annotationData();
        return AnnotationSupport.getAssociatedAnnotations(annotationData.declaredAnnotations,
                                                          this,
                                                          annotationClass);
    }

    /**
     * @since 1.5
     */
    public Annotation[] getAnnotations() {
        return AnnotationParser.toArray(annotationData().annotations);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        return (A) annotationData().declaredAnnotations.get(annotationClass);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        return AnnotationSupport.getDirectlyAndIndirectlyPresent(annotationData().declaredAnnotations,
                                                                 annotationClass);
    }

    /**
     * @since 1.5
     */
    public Annotation[] getDeclaredAnnotations()  {
        return AnnotationParser.toArray(annotationData().declaredAnnotations);
    }

    // annotation data that might get invalidated when JVM TI RedefineClasses() is called
    private static class AnnotationData {
        final Map<Class<? extends Annotation>, Annotation> annotations;
        final Map<Class<? extends Annotation>, Annotation> declaredAnnotations;

        // Value of classRedefinedCount when we created this AnnotationData instance
        final int redefinedCount;

        AnnotationData(Map<Class<? extends Annotation>, Annotation> annotations,
                       Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
                       int redefinedCount) {
            this.annotations = annotations;
            this.declaredAnnotations = declaredAnnotations;
            this.redefinedCount = redefinedCount;
        }
    }

    // Annotations cache
    @SuppressWarnings("UnusedDeclaration")
    private transient volatile AnnotationData annotationData;

    private AnnotationData annotationData() {
        while (true) { // retry loop
            AnnotationData annotationData = this.annotationData;
            int classRedefinedCount = this.classRedefinedCount;
            if (annotationData != null &&
                annotationData.redefinedCount == classRedefinedCount) {
                return annotationData;
            }
            // null or stale annotationData -> optimistically create new instance
            AnnotationData newAnnotationData = createAnnotationData(classRedefinedCount);
            // try to install it
            if (Atomic.casAnnotationData(this, annotationData, newAnnotationData)) {
                // successfully installed new AnnotationData
                return newAnnotationData;
            }
        }
    }

    private AnnotationData createAnnotationData(int classRedefinedCount) {
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations =
            AnnotationParser.parseAnnotations(getRawAnnotations(), getConstantPool(), this);
        Class<?> superClass = getSuperclass();
        Map<Class<? extends Annotation>, Annotation> annotations = null;
        if (superClass != null) {
            Map<Class<? extends Annotation>, Annotation> superAnnotations =
                superClass.annotationData().annotations;
            for (Map.Entry<Class<? extends Annotation>, Annotation> e : superAnnotations.entrySet()) {
                Class<? extends Annotation> annotationClass = e.getKey();
                if (AnnotationType.getInstance(annotationClass).isInherited()) {
                    if (annotations == null) { // lazy construction
                        annotations = new LinkedHashMap<>((Math.max(
                                declaredAnnotations.size(),
                                Math.min(12, declaredAnnotations.size() + superAnnotations.size())
                            ) * 4 + 2) / 3
                        );
                    }
                    annotations.put(annotationClass, e.getValue());
                }
            }
        }
        if (annotations == null) {
            // no inherited annotations -> share the Map with declaredAnnotations
            annotations = declaredAnnotations;
        } else {
            // at least one inherited annotation -> declared may override inherited
            annotations.putAll(declaredAnnotations);
        }
        return new AnnotationData(annotations, declaredAnnotations, classRedefinedCount);
    }

    // Annotation types cache their internal (AnnotationType) form

    @SuppressWarnings("UnusedDeclaration")
    private transient volatile AnnotationType annotationType;

    boolean casAnnotationType(AnnotationType oldType, AnnotationType newType) {
        return Atomic.casAnnotationType(this, oldType, newType);
    }

    AnnotationType getAnnotationType() {
        return annotationType;
    }

    Map<Class<? extends Annotation>, Annotation> getDeclaredAnnotationMap() {
        return annotationData().declaredAnnotations;
    }

    /* Backing store of user-defined values pertaining to this class.
     * Maintained by the ClassValue class.
     */
    transient ClassValue.ClassValueMap classValueMap;

    /**
     * Returns an {@code AnnotatedType} object that represents the use of a
     * type to specify the superclass of the entity represented by this {@code
     * Class} object. (The <em>use</em> of type Foo to specify the superclass
     * in '...  extends Foo' is distinct from the <em>declaration</em> of type
     * Foo.)
     *
     * <p> If this {@code Class} object represents a type whose declaration
     * does not explicitly indicate an annotated superclass, then the return
     * value is an {@code AnnotatedType} object representing an element with no
     * annotations.
     *
     * <p> If this {@code Class} represents either the {@code Object} class, an
     * interface type, an array type, a primitive type, or void, the return
     * value is {@code null}.
     *
     * @return an object representing the superclass
     * @since 1.8
     */
    public AnnotatedType getAnnotatedSuperclass() {
        if (this == Object.class ||
                isInterface() ||
                isArray() ||
                isPrimitive() ||
                this == Void.TYPE) {
            return null;
        }

        return TypeAnnotationParser.buildAnnotatedSuperclass(getRawTypeAnnotations(), getConstantPool(), this);
    }

    /**
     * Returns an array of {@code AnnotatedType} objects that represent the use
     * of types to specify superinterfaces of the entity represented by this
     * {@code Class} object. (The <em>use</em> of type Foo to specify a
     * superinterface in '... implements Foo' is distinct from the
     * <em>declaration</em> of type Foo.)
     *
     * <p> If this {@code Class} object represents a class, the return value is
     * an array containing objects representing the uses of interface types to
     * specify interfaces implemented by the class. The order of the objects in
     * the array corresponds to the order of the interface types used in the
     * 'implements' clause of the declaration of this {@code Class} object.
     *
     * <p> If this {@code Class} object represents an interface, the return
     * value is an array containing objects representing the uses of interface
     * types to specify interfaces directly extended by the interface. The
     * order of the objects in the array corresponds to the order of the
     * interface types used in the 'extends' clause of the declaration of this
     * {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface whose
     * declaration does not explicitly indicate any annotated superinterfaces,
     * the return value is an array of length 0.
     *
     * <p> If this {@code Class} object represents either the {@code Object}
     * class, an array type, a primitive type, or void, the return value is an
     * array of length 0.
     *
     * @return an array representing the superinterfaces
     * @since 1.8
     */
    public AnnotatedType[] getAnnotatedInterfaces() {
         return TypeAnnotationParser.buildAnnotatedInterfaces(getRawTypeAnnotations(), getConstantPool(), this);
    }

    private native Class<?> getNestHost0();

    /**
     * Returns the nest host of the <a href=#nest>nest</a> to which the class
     * or interface represented by this {@code Class} object belongs.
     * Every class and interface is a member of exactly one nest.
     * A class or interface that is not recorded as belonging to a nest
     * belongs to the nest consisting only of itself, and is the nest
     * host.
     *
     * <p>Each of the {@code Class} objects representing array types,
     * primitive types, and {@code void} returns {@code this} to indicate
     * that the represented entity belongs to the nest consisting only of
     * itself, and is the nest host.
     *
     * <p>If there is a {@linkplain LinkageError linkage error} accessing
     * the nest host, or if this class or interface is not enumerated as
     * a member of the nest by the nest host, then it is considered to belong
     * to its own nest and {@code this} is returned as the host.
     *
     * @apiNote A {@code class} file of version 55.0 or greater may record the
     * host of the nest to which it belongs by using the {@code NestHost}
     * attribute (JVMS 4.7.28). Alternatively, a {@code class} file of
     * version 55.0 or greater may act as a nest host by enumerating the nest's
     * other members with the
     * {@code NestMembers} attribute (JVMS 4.7.29).
     * A {@code class} file of version 54.0 or lower does not use these
     * attributes.
     *
     * @return the nest host of this class or interface
     *
     * @throws SecurityException
     *         If the returned class is not the current class, and
     *         if a security manager, <i>s</i>, is present and the caller's
     *         class loader is not the same as or an ancestor of the class
     *         loader for the returned class and invocation of {@link
     *         SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *         denies access to the package of the returned class
     * @since 11
     * @jvms 4.7.28 and 4.7.29 NestHost and NestMembers attributes
     * @jvms 5.4.4 Access Control
     */
    @CallerSensitive
    public Class<?> getNestHost() {
        if (isPrimitive() || isArray()) {
            return this;
        }
        Class<?> host;
        try {
            host = getNestHost0();
        } catch (LinkageError e) {
            // if we couldn't load our nest-host then we
            // act as-if we have no nest-host attribute
            return this;
        }
        // if null then nest membership validation failed, so we
        // act as-if we have no nest-host attribute
        if (host == null || host == this) {
            return this;
        }
        // returning a different class requires a security check
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkPackageAccess(sm,
                               ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
        }
        return host;
    }

    /**
     * Determines if the given {@code Class} is a nestmate of the
     * class or interface represented by this {@code Class} object.
     * Two classes or interfaces are nestmates
     * if they have the same {@linkplain #getNestHost() nest host}.
     *
     * @param c the class to check
     * @return {@code true} if this class and {@code c} are members of
     * the same nest; and {@code false} otherwise.
     *
     * @since 11
     */
    public boolean isNestmateOf(Class<?> c) {
        if (this == c) {
            return true;
        }
        if (isPrimitive() || isArray() ||
            c.isPrimitive() || c.isArray()) {
            return false;
        }
        try {
            return getNestHost0() == c.getNestHost0();
        } catch (LinkageError e) {
            return false;
        }
    }

    private native Class<?>[] getNestMembers0();

    /**
     * Returns an array containing {@code Class} objects representing all the
     * classes and interfaces that are members of the nest to which the class
     * or interface represented by this {@code Class} object belongs.
     * The {@linkplain #getNestHost() nest host} of that nest is the zeroth
     * element of the array. Subsequent elements represent any classes or
     * interfaces that are recorded by the nest host as being members of
     * the nest; the order of such elements is unspecified. Duplicates are
     * permitted.
     * If the nest host of that nest does not enumerate any members, then the
     * array has a single element containing {@code this}.
     *
     * <p>Each of the {@code Class} objects representing array types,
     * primitive types, and {@code void} returns an array containing only
     * {@code this}.
     *
     * <p>This method validates that, for each class or interface which is
     * recorded as a member of the nest by the nest host, that class or
     * interface records itself as a member of that same nest. Any exceptions
     * that occur during this validation are rethrown by this method.
     *
     * @return an array of all classes and interfaces in the same nest as
     * this class
     *
     * @throws LinkageError
     *         If there is any problem loading or validating a nest member or
     *         its nest host
     * @throws SecurityException
     *         If any returned class is not the current class, and
     *         if a security manager, <i>s</i>, is present and the caller's
     *         class loader is not the same as or an ancestor of the class
     *         loader for that returned class and invocation of {@link
     *         SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *         denies access to the package of that returned class
     *
     * @since 11
     * @see #getNestHost()
     */
    @CallerSensitive
    public Class<?>[] getNestMembers() {
        if (isPrimitive() || isArray()) {
            return new Class<?>[] { this };
        }
        Class<?>[] members = getNestMembers0();
        // Can't actually enable this due to bootstrapping issues
        // assert(members.length != 1 || members[0] == this); // expected invariant from VM

        if (members.length > 1) {
            // If we return anything other than the current class we need
            // a security check
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkPackageAccess(sm,
                                   ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return members;
    }
}
