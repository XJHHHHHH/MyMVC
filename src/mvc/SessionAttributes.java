package mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这个注解放在控制类上，
 * 看是否需要向session作用域中存值
 *
 * @author XJHDPL*/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SessionAttributes {
    String[] value();
}
