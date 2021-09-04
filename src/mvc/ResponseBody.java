package mvc;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这个注解定义在方法纸上，如果有这个注解
 * 表示方法返回值是数据String，不是代表资源名
 *
 * @author XJHDPL*/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseBody {
}
