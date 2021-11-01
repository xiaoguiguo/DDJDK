package java.lang;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

/**
 * @className: SuppressWarnings
 * @author: doudou
 * @datetime: 2021/11/1
 * @description: 用于抑制编译器产生警告信息。
 *      抑制单类型的警告：@SuppressWarnings("unchecked")
 *      抑制多类型的警告：@SuppressWarnings(value={"unchecked", "rawtypes"})
 *      抑制所有类型的警告：@SuppressWarnings("all")
 */
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, MODULE})
@Retention(RetentionPolicy.SOURCE)
public @interface SuppressWarnings {
    String[] value();
}
